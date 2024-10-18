#include "MQTT-sensor.h"
#include "contiki.h"
#include "net/routing/routing.h"
#include "mqtt.h"
#include "net/ipv6/uip.h"
#include "net/ipv6/uip-icmp6.h"
#include "net/ipv6/sicslowpan.h"
#include "sys/etimer.h"
#include "sys/ctimer.h"
#include "lib/sensors.h"
#include "dev/button-hal.h"
#include "dev/leds.h"
#include "os/sys/log.h"
#include "os/lib/json/jsonparse.c"
#include "node-id.h"
#include "random.h"
#include "os/dev/leds.h"
#include <time.h>
#include "sys/node-id.h"

#define LOG_MODULE "IOT_PROJECT"
#define LOG_LEVEL LOG_LEVEL_APP

#include <string.h>
#include <strings.h>

static struct mqtt_connection conn;

#define MQTT_CLIENT_BROKER_IP_ADDR "fd00::1"
static const char *broker_ip = MQTT_CLIENT_BROKER_IP_ADDR;

#define DEFAULT_BROKER_PORT 1883
#define DEFAULT_PUBLISH_INTERVAL (30 * CLOCK_SECOND)
#define PUBLISH_INTERVAL (15 * CLOCK_SECOND)

#define MAX_TCP_SEGMENT_SIZE 32
#define CONFIG_IP_ADDR_STR_LEN 64
static struct mqtt_message *msg_ptr = 0;

#define BUFFER_SIZE 64
static char client_id[BUFFER_SIZE];
static char pub_topic[BUFFER_SIZE];
static char sub_topic[BUFFER_SIZE];

#define APP_BUFFER_SIZE 512
static char app_buffer[APP_BUFFER_SIZE];

#define STATE_MACHINE_PERIODIC (CLOCK_SECOND >> 1)
static struct etimer periodic_timer;

static uint8_t state;
#define STATE_INIT 0 // Initial state
#define STATE_NET_OK 1 // Network is initialized
#define STATE_CONNECTING 2 // Connecting to MQTT broker
#define STATE_CONNECTED 3 // Connection successful
#define STATE_SUBSCRIBED 4 // Topics of interest subscribed
#define STATE_DISCONNECTED 5 // Disconnected from MQTT broker

char broker_address[CONFIG_IP_ADDR_STR_LEN];
static mqtt_status_t status;

static bool have_connectivity(void)
{
	if(uip_ds6_get_global(ADDR_PREFERRED) == NULL ||
	uip_ds6_defrt_choose() == NULL) {
		return false;
	}

	return true;
}

static bool change_temperature = false;
static bool change_humidity = false;

// questa variabile vale 1 durante la increase, 2 durante la decrease e 0 se la modalità NON E' user control
static int user_control = 0;

void extractJson(const char *json, const char *key, char *value) {
	const char *key_start = strstr(json, key);
	    if (key_start) {
	        key_start += strlen(key) + strlen("\":");
	        const char *value_end = strchr(key_start, '"');
	        if (value_end) {
	            strncpy(value, key_start, value_end - key_start);
	            value[value_end - key_start] = '\0';
	            //printf("%s: %s\n", key, value);
	        }
	    }
}

// viene chiamata quando viene ricevuto un messaggio da un topic al quale il nodo si è iscritto.
static void pub_handler(const char *topic, uint16_t topic_len, const uint8_t *chunk, uint16_t chunk_len)
{
  printf("Pub Handler: topic='%s' (len=%u), chunk='%.*s' (len=%u)\n", topic, topic_len, chunk_len, chunk, chunk_len);
  
  char sensor[20];  
  char payload[20];
  
  extractJson((char*)chunk, "\"Sensor\"", sensor);
  extractJson((char*)chunk, "\"payload\"", payload);
  
  if(strcmp(topic, "Actuator")==0){
	  if(strcmp(sensor, "temperature") == 0){
		   if(strcmp(payload, "ON") == 0) {
				change_temperature = true;
				user_control = 0;
				printf("Actuator temperature is on\n");
			} else if(strcmp(payload, "OFF") == 0){
				change_temperature = false;
				user_control = 0;
				printf("Actuator temperature is off\n");
			} else if(strcmp(payload, "increase") == 0){
				change_temperature = false;
				user_control = 1;
				printf("Actuator temperature is under control of the user, increase\n");
			} else if(strcmp(payload, "decrease") == 0){
				change_temperature = false;
				user_control = 2;
				printf("Actuator temperature is under control of the user, decrease\n");
			}
	  } else if(strcmp(sensor, "humidity") == 0){
	    if(strcmp(payload, "ON") == 0){
			change_humidity = true;
			printf("Actuator humidity is on \n");
		} else if(strcmp(payload, "OFF")==0){
			change_humidity = false;
			printf("Actuator humidity is off \n");
		}
	  }
  } else
	  printf("Invalid topic\n");
}

// id del nodo
static int nodeID = 0;

// Variabile per il valore da pubblicare periodicamente
static int temperature;
static int humidity;
static int initialize = 0;

static void initialize_parameter()
{
	srand(time(NULL));
    temperature = 10 + (rand() % 20);
    humidity = 10 + (rand() % 70);

	node_id_init();
	nodeID = node_id;
}

// tengo traccia del colore attuale del led. 
// - Se la temperatura si discosta di almeno 10 gradi diventa rossa (lettera r)
// - Se la temperatura si discosta di <= 5 gradi diventa gialla (lettera y)
// - Se la temperatura è uguale a quella ottimale, il led è verde (lettera g)
// Inizialmente il led è spento (lettera w)
char currentColor = 'w';

// funzione che cambia lo stato del led. Il parametro "ledStatus" rappresenta il colore che voglio che il led assuma
void changeLEDColor(char ledStatus) {
    if (ledStatus == 'r') {
        if (currentColor != 'r') {
            leds_off(LEDS_GREEN);
            leds_off(LEDS_YELLOW);
            leds_on(LEDS_RED);
            currentColor = 'r';
        }
    } else if (ledStatus == 'y') {
        if (currentColor != 'y') {
            leds_off(LEDS_RED);
            leds_off(LEDS_GREEN);
            leds_on(LEDS_YELLOW);
            currentColor = 'y';
        }
    } else if (ledStatus == 'g') {
        if (currentColor != 'g') {
            leds_off(LEDS_RED);
            leds_off(LEDS_YELLOW);
            leds_on(LEDS_GREEN);
            currentColor = 'g';
        }
    }
}

static void update_leds(){
		int temperatureDiff = abs(temperature - 20);
		int humidityDiff = abs(humidity - 45);

		if (temperatureDiff >= 10 || humidityDiff >= 20)
			changeLEDColor('r');
		else if (temperatureDiff >= 5 || humidityDiff >= 15)
			changeLEDColor('y');
		else if (temperatureDiff == 0)
			changeLEDColor('g');
}

static void actuator()
{
	// Se l'attuatore per la temperatura è acceso
	if(change_temperature){
		initialize = 1;
		if(temperature < 20)
			temperature++;
		else if(temperature > 20)
			temperature--;
	} else if(user_control==1 && temperature < 30){ // l'utente ha digitato "increase"
		temperature++;
	} else if(user_control==2 && temperature > 10){ // l'utente ha digitato "decrease"
		temperature--;
	} 
	
	// se l'attuatore per l'umidità è acceso
	if(change_humidity){
		initialize = 1;
		if(humidity < 30)
			humidity += (rand() % 5) + 1;
		else if(humidity > 60)
			humidity -= (rand() % 5) + 1;
	}

	// se gli attuatori sono entrambi spenti, dopo 45 secondi (initialize > 2) dall'ultima rilevazione avverà un 
	// incremento/decremento casuale delle misurazioni successive delle due grandezze, altrimenti temperatura e umidità rimarrebbero 
	// le stesse per sempre nella simulazione
	if(!change_temperature && !change_humidity){
		initialize++;
		
		if(initialize > 2){
			int sign = (rand() % 2 == 0) ? 1 : -1;
			temperature += sign * (rand() % 5) + 1;
			sign = (rand() % 2 == 0) ? 1 : -1;
			humidity += sign * (rand() % 10) + 1;
		}
	}
	
	update_leds();
}

PROCESS(mqtt_sensor_process, "MQTT Sensor");
AUTOSTART_PROCESSES(&mqtt_sensor_process);

static void mqtt_event(struct mqtt_connection *m, mqtt_event_t event, void *data)
{
	switch(event) {
		case MQTT_EVENT_CONNECTED: {
			printf("App has a MQTT connection\n");
		    	state = STATE_CONNECTED;
		    	break;
		}
		case MQTT_EVENT_DISCONNECTED: {
			printf("MQTT Disconnect. Reason %u\n",*((mqtt_event_t *)data));
		    	state = STATE_DISCONNECTED;
		    	process_poll(&mqtt_sensor_process);
		    	break;
		}
		case MQTT_EVENT_PUBLISH: {
			// Notification on a subscribed topic received 
 			msg_ptr = data;

			if (msg_ptr != NULL){
    			pub_handler(msg_ptr->topic, strlen(msg_ptr->topic), msg_ptr->payload_chunk, msg_ptr->payload_length);
			}    			
			break;
		}
		case MQTT_EVENT_SUBACK: {
			// Subscription successful
			#if MQTT_311
    				mqtt_suback_event_t *suback_event = (mqtt_suback_event_t *)data;

			    	if(suback_event->success) {
			      		printf("Application is subscribed to topic successfully\n");
			    	} else {
			      	printf("Application failed to subscribe to topic (ret code %x)\n", suback_event-	>return_code);
			    	}
			#else
		    		printf("Application is subscribed to topic successfully\n");
			#endif
		    	break;
		}
		case MQTT_EVENT_UNSUBACK: {
			printf("Application is unsubscribed to topic successfully\n");
    			break;
		}
		case MQTT_EVENT_PUBACK: {
			// Publishing completed
			printf("Publishing complete.\n");
    			break;
		}
		default:
		    printf("Unknown MQTT event: %i\n", event);
		    break;
	}
}

PROCESS_THREAD(mqtt_sensor_process, ev, data)
		{	
  			PROCESS_BEGIN();

			if(initialize == 0){
				initialize_parameter();
				printf("Valore della temperatura all'avvio: %d \n", temperature);
				printf("Valore dell'umidita' all'avvio: %d \n", humidity);
				initialize = 1;
			}

			snprintf(client_id, BUFFER_SIZE, "%02x%02x%02x%02x%02x%02x",
				linkaddr_node_addr.u8[0], linkaddr_node_addr.u8[1],
				linkaddr_node_addr.u8[2], linkaddr_node_addr.u8[5],
				linkaddr_node_addr.u8[6], linkaddr_node_addr.u8[7]);
			
			// Broker registration
			mqtt_register(&conn, &mqtt_sensor_process, client_id, mqtt_event, MAX_TCP_SEGMENT_SIZE);
			state=STATE_INIT;
			
			// Initialize periodic timer to check the status 
			etimer_set(&periodic_timer, PUBLISH_INTERVAL);

			while(1) {
				PROCESS_YIELD();

				if((ev == PROCESS_EVENT_TIMER && data == &periodic_timer) || ev == PROCESS_EVENT_POLL){
					// When the network is initialized the client moves from STATE_INIT to STATE_NET_OK
					if(state==STATE_INIT){
						if(have_connectivity()==true) 
							state = STATE_NET_OK;
					} else if(state == STATE_NET_OK){
						printf("Connecting!\n");

						memcpy(broker_address, broker_ip, strlen(broker_ip));
						mqtt_connect(&conn, broker_address,DEFAULT_BROKER_PORT,
							    (DEFAULT_PUBLISH_INTERVAL * 3) / CLOCK_SECOND, 
							     MQTT_CLEAN_SESSION_ON);

						state = STATE_CONNECTING;
						printf("Connected to MQTT server\n");
					}else if(state==STATE_CONNECTED){
						strcpy(sub_topic,"Actuator");
						status = mqtt_subscribe(&conn, NULL, sub_topic, MQTT_QOS_LEVEL_0);
						printf("Subscribing!\n");

						if(status == MQTT_STATUS_OUT_QUEUE_FULL) {
							LOG_ERR("Tried to subscribe but command queue was full!\n");
							PROCESS_EXIT();
						}

						state = STATE_SUBSCRIBED;
					}else if(state == STATE_SUBSCRIBED){
						actuator();

						sprintf(pub_topic, "%s", "Measurements");
						sprintf(app_buffer, "{\"SensorID\": %d, \"Temperature\": %d, \"Humidity\": %d}", nodeID,temperature,humidity);
												
						mqtt_publish(&conn, NULL, pub_topic,(uint8_t*) app_buffer,
						strlen(app_buffer), MQTT_QOS_LEVEL_0, MQTT_RETAIN_OFF);
						
						printf("Pubblicato: %s \n", app_buffer);
						printf("Sul topic: %s \n", pub_topic);
					} else if(state == STATE_DISCONNECTED) {
						printf("Disconnected from MQTT broker\n");
						state = STATE_INIT;	
					}

					etimer_set(&periodic_timer, PUBLISH_INTERVAL);
            } else if(ev == button_hal_press_event) {
				sprintf(pub_topic, "%s", "Measurements");
				sprintf(app_buffer, "{\"SensorID\": %d, \"Temperature\": %d, \"Humidity\": %d}", nodeID,temperature,humidity);
										
				mqtt_publish(&conn, NULL, pub_topic,(uint8_t*) app_buffer, strlen(app_buffer), MQTT_QOS_LEVEL_0, MQTT_RETAIN_OFF);
				printf("Pubblicato: %s , topic: %s \n", app_buffer, pub_topic);
            }
		}
		
	        PROCESS_END();
}
