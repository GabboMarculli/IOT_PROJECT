package iot.unipi.it.Coap;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;

import java.io.IOException;

public class MyCoapClient {
    private CoapClient coapClient;
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_YELLOW = "\u001B[33m";

    public MyCoapClient(String uri) {
        coapClient = new CoapClient(uri);
    }
    
    public boolean performPostRequest(String payload) {        
        try {
            CoapResponse response = coapClient.post(payload, MediaTypeRegistry.TEXT_PLAIN);
            if (response != null && response.isSuccess()) {
                String responseText = response.getResponseText();
                System.out.println(ANSI_YELLOW + "Risposta dalla richiesta POST: " + responseText + ANSI_RESET);
                return true;
            } else {
            	System.out.println("La richiesta POST non è riuscita o ha restituito una risposta non valida");
            	return false;
            }
        } catch (Exception e) {
            if (e.getMessage().contains("Network is unreachable")) {
                System.out.println(ANSI_YELLOW + "I sensori sono attualmente spenti o non raggiungibili" + ANSI_RESET);
            } else {
                System.out.println("Si è verificato un errore durante la richiesta POST: " + e.getMessage());
            }
            
            return false;
        }
    }

    public boolean performGetRequest(String sensor) {
        try {
            CoapResponse response = coapClient.get();
            if (response != null && response.isSuccess()) {
                String responseText = response.getResponseText();
                
                if(sensor.equals("Temperature"))
                	System.out.println(ANSI_YELLOW +"Temperature actuator is " + responseText + ANSI_RESET);
                else if(sensor.equals("Humidity"))
                	System.out.println(ANSI_YELLOW +"Humidity actuator is " + response.getResponseText() + ANSI_RESET);
                
                return true;
            } else {
                System.out.println("La richiesta GET non è riuscita o ha restituito una risposta non valida");
                return false;
            }
        } catch (Exception e) {
            if (e.getMessage().contains("Network is unreachable")) {
                System.out.println(ANSI_YELLOW + "Il sensore " + sensor + " è attualmente spento o non raggiungibile" + ANSI_RESET);
            } else {
                System.out.println("Si è verificato un errore durante la richiesta GET: " + e.getMessage());
            }
            
            return false;
        }
    }
}






