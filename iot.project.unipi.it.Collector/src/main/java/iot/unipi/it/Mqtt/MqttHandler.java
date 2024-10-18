package iot.unipi.it.Mqtt;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import iot.unipi.it.Coap.MyCoapClient;
import org.eclipse.californium.core.CoapClient;

public class MqttHandler {
        private static MqttClient mqttClient;
        private static String brokerUrl;
        private static String clientId;
        private static MessageHandler messageHandler;

        public MqttHandler(String brokerUrl, String clientId, String topic, MyCoapClient temp, MyCoapClient hum) throws MqttException {
            MqttHandler.brokerUrl = brokerUrl;
            MqttHandler.clientId = clientId;
            this.setMessageHandler(new MessageHandler(temp, hum));

            connect(topic);
        }

        public static void connect(String topic) throws MqttException {
                try {
                        mqttClient = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
                        mqttClient.setCallback(new CustomMqttCallback(messageHandler, topic));

                        mqttClient.connect();
                        mqttClient.subscribe(topic);
                } catch (MqttException e) {
                        e.printStackTrace();
                }
        }

        public void setMessageHandler(MessageHandler handler) {
                messageHandler = handler;
        }

        public void disconnect() {
                try {
                	if(MqttHandler.isConnected())
                        mqttClient.disconnect();
                } catch (MqttException e) {
                    System.err.println("Error disconnecting from MQTT broker: " + e.getMessage());
                }
        }

        public static boolean isConnected()
        {
                return mqttClient.isConnected();
        }

        public static void publishMessage(String topic, String message) throws MqttException{
                try {
                        MqttMessage mqttMessage = new MqttMessage(message.getBytes());
                        mqttClient.publish(topic, mqttMessage);
                        
                        System.out.println("\u001B[34m" + "Sul topic " + topic + " ho pubblicato " + message + "\u001B[0m");
                } catch (MqttException e) {
                        e.printStackTrace();
                }
        }
        
        public void set_modality(boolean state) {
        	messageHandler.setModality(state);
        }
}
