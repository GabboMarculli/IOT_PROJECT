#include "contiki.h"
#include "coap-engine.h"
#include <stdio.h>
#include "os/dev/leds.h"
#include <string.h>

static void res_post_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
static void res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);

RESOURCE(res_actuator_temperature, 
	    "",
		res_get_handler, // handler per GET
	    res_post_handler, // Handler per POST
	    NULL, // Nessun handler per PUT
	    NULL); // Nessun handler per DELETE

static int on = 0; 

static void res_post_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset)
{
    const uint8_t *payload = NULL;
    int len = coap_get_payload(request, &payload);

    if (len > 0) {
	  char payload_str[len + 1];
	  memcpy(payload_str, payload, len);
	  payload_str[len] = '\0';

	  // il led è rosso all'inizio e quando termina la modalità manuale, è verde quando invece l'attuatore è in modalità manuale
	  printf("Arrive on temperature resource: %s\n", payload_str);
        if (strncmp((char *)payload, "Turn on actuator", len) == 0) {	
            on = 1;
            leds_set(LEDS_RED);
        } else if (strncmp((char *)payload, "Turn off actuator", len) == 0) {
            on = 0;
            leds_set(LEDS_GREEN);
        } 
  }
  coap_set_status_code(response, CONTENT_2_05);
}

static void res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
    char *message = (on == 1) ? "on." : "off."; 
    
    printf("Mi appresto ad inviare: %s \n", message);
    	
    int length = strlen(message);
    memcpy(buffer, message, length);

    // Prepare the response
    coap_set_header_content_format(response, TEXT_PLAIN); 
    coap_set_header_etag(response, (uint8_t *)&length, 1);
    coap_set_payload(response, buffer, length);
}
