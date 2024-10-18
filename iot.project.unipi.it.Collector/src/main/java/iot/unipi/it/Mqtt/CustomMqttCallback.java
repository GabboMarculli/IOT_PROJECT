package iot.unipi.it.Mqtt;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.concurrent.TimeUnit;

public class CustomMqttCallback implements MqttCallback {
    private MessageHandler messageHandler;
    private String topic;
    private static final int MAX_RECONNECT_ATTEMPTS = 3;
    private static final long RECONNECT_DELAY = 5000;
    
    public CustomMqttCallback(MessageHandler messageHandler, String topic) {
        this.messageHandler = messageHandler;
        this.topic = topic;
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.out.println("Lost MQTT connection: " + cause.getMessage());

        int reconnectAttempts = 0;

        // Nel momento in cui viene persa la connessione si prova, per 3 volte e ad intervalli di 5 secondi uno dall'altro, a riconnettersi
        while (reconnectAttempts < MAX_RECONNECT_ATTEMPTS && !MqttHandler.isConnected()) {
            try {
                TimeUnit.SECONDS.sleep(RECONNECT_DELAY);
                MqttHandler.connect(topic);
                System.out.println("Connection reestablished!");
                break; 
            } catch (InterruptedException e) {
                System.out.println("Error during sleep");
            } catch (Exception e) {
                System.out.println("Error during connection: " + e.getMessage());
                reconnectAttempts++;
            }
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        if (messageHandler != null) {
            messageHandler.handleMessage(topic, message);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        System.out.println("\u001B[34m" +"Messaggio MQTT consegnato con successo" + "\u001B[0m");
        System.out.println("Enter a command: ");
    }
}

