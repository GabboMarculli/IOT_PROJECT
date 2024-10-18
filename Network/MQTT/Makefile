TIKI_PROJECT = LAB-mqtt
all: $(CONTIKI_PROJECT)

CFLAGS = -Wall -I../../network/mqtt
CONTIKI = ../../..

include $(CONTIKI)/Makefile.dir-variables
MODULES += $(CONTIKI_NG_APP_LAYER_DIR)/mqtt

-include $(CONTIKI)/Makefile.identify-target

MODULES_REL += arch/platform/$(TARGET)

include $(CONTIKI)/Makefile.include

