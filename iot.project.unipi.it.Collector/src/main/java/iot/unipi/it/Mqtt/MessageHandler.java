package iot.unipi.it.Mqtt;

import iot.unipi.it.DAO.MeasurementsDAO;
import iot.unipi.it.Coap.MyCoapClient;
import iot.unipi.it.DBMaintaince.DBDriver;
import iot.unipi.it.DBMaintaince.DBSingleton;
import iot.unipi.it.DTO.MeasurementsDTO;
import iot.unipi.it.Utils.Constant;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.CoapClient;

import java.sql.Timestamp;
import java.sql.Connection;

public class MessageHandler {
	private MyCoapClient coapClientTemp;
	private MyCoapClient coapClientHum;
	private static boolean user_modality = false;
	
	public MessageHandler(MyCoapClient temp, MyCoapClient hum) {
        this.coapClientTemp = temp;
        this.coapClientHum  = hum;
    }
	
    public void handleMessage(String topic, MqttMessage message){
        System.out.println("\u001B[34m" + "Messaggio ricevuto sul topic: " + topic + "\u001B[0m");

        JSONObject JsonObject = processMessage(message);

        try {
        	if(topic.equals("Measurements")) {
	            int sensorID = JsonObject.getInt("SensorID");
	            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
	            int temperature = JsonObject.getInt("Temperature");
	            int humidity = JsonObject.getInt("Humidity");
	
	            // inserisco nel database i dati appena estratti dal messaggio
	            MeasurementsDTO measurementsDTO = new MeasurementsDTO(sensorID, timestamp,temperature, humidity);
	            MeasurementsDAO measurementsDAO = new MeasurementsDAO(DBDriver.getConnection());
	            Constant.setLAST_MEASUREMENT(measurementsDTO);
	            measurementsDAO.insertMeasurements(measurementsDTO);
	            
	            // Comunico ai vari attuatori se devono attivarsi oppure no
	            sendMessages("temperature", temperature != Constant.getTARGET_TEMPERATURE(), coapClientTemp);
	            sendMessages("humidity", humidity < 30 || humidity > 60, coapClientHum);                   
        	}
        }catch (JSONException e) {
        	 System.err.println("Errore durante il parsing JSON del messaggio: " + e.getMessage());
        	 return;
        }
    }

    private void sendMessages(String sensor, boolean condition, MyCoapClient coapClient) {
    	String payload = "";
        String coapMessage = "";
        if(user_modality == false) {
        	payload = "{\"Sensor\":\"" + sensor + "\", \"payload\":\"";
        	if(condition) {
                payload += "ON\"}";
                coapMessage = "Turn on actuator";
            } else {
                payload += "OFF\"}";
                coapMessage = "Turn off actuator";
            }
            
            try {
            	MqttHandler.publishMessage("Actuator", payload);
	    		coapClient.performPostRequest(coapMessage);
	    	}  catch (Exception e) {
	    	    System.out.println("Errore durante l'invio della richiesta CoAP: " + e.getMessage());
	    	    e.printStackTrace();
	    	    return;
	    	}   	
        }
    }
    
    private JSONObject processMessage(MqttMessage message) {
        String payload = new String(message.getPayload());
        System.out.println("\u001B[34m" + "Arrivato: " + payload + "\u001B[0m");
        JSONObject jsonobj = null;

        try {
            jsonobj = new JSONObject(payload);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        
        return jsonobj;
    }
    
    public void setModality(boolean state)
    {
    	user_modality = state;
    }
}
 public void setModality(boolean state)
    {
    	user_modality = state;
    }
}
