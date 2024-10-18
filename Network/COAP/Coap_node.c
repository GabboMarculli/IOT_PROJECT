#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "contiki.h"
#include "coap-engine.h"
#include "sys/log.h"
#include "os/dev/leds.h"

#define LOG_MODULE "App"
#define LOG_LEVEL LOG_LEVEL_APP

extern coap_resource_t res_actuator_temperature;
extern coap_resource_t res_actuator_humidity;

PROCESS(coap_process, "coap actuator process");
AUTOSTART_PROCESSES(&coap_process);

PROCESS_THREAD(coap_process, ev, data)
{
	PROCESS_BEGIN();
	PROCESS_PAUSE();

	coap_activate_resource(&res_actuator_temperature, "actuator_temperature");
	coap_activate_resource(&res_actuator_humidity, "actuator_humidity");

	leds_set(LEDS_RED);
    while(1) {
      PROCESS_WAIT_EVENT();
    }                             

    PROCESS_END();
}