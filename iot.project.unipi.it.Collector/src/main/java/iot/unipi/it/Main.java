package iot.unipi.it;

import iot.unipi.it.DTO.MeasurementsDTO;
import iot.unipi.it.DAO.MeasurementsDAO;
import iot.unipi.it.Utils.Constant;
import iot.unipi.it.Coap.MyCoapClient;
import iot.unipi.it.DBMaintaince.DBDriver;
import iot.unipi.it.DBMaintaince.DBSingleton;
import iot.unipi.it.Mqtt.MqttHandler;
import iot.unipi.it.Config.ConfigReader;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import iot.unipi.it.Utils.Constant;
import java.sql.Timestamp;
import java.util.Date;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.CoapClient;

import java.sql.SQLException;
import java.util.Scanner;

public class Main {
    private static DBDriver database = null;
    private static Constant constant;
    private static MqttHandler mqttHandler;
    private static MyCoapClient coapClientTemp;
    private static MyCoapClient coapClientHum;

    private static String DB_URL;
    private static String DB_user;
    private static String DB_password;
    private static String topic;
    
    private static String MqttBrokerUrl;
    private static String MqttClientId;
    private static String coapURLTemp;
    private static String coapURLHum;
    
    private static int maxTemperature;
    private static int minTemperature;
    private static int maxHumidity;
    private static int minHumidity;

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m"; 
    public static final String ANSI_GREEN = "\u001B[32m";
    
    public static void main(String[] args) throws SQLException {
    	loadConfiguration();
    	
        database = DBSingleton.createDBManager(DB_URL, DB_user, DB_password);
        constant = new Constant();       
        
        run();
        
        printCommand(); 
        chooseCommand();
    }
    
    public static void run(){	
        coapClientTemp = new MyCoapClient(coapURLTemp);
        coapClientHum = new MyCoapClient(coapURLHum);
        
    	try {
        	mqttHandler = new MqttHandler(MqttBrokerUrl,MqttClientId, topic, coapClientTemp, coapClientHum);
        } catch (MqttException e) {
        	throw new RuntimeException(e);
        }
    }
    
    public static void loadConfiguration() {
    	ConfigReader configReader = new ConfigReader("config.properties");

        DB_URL = configReader.getString("db.url");
        DB_user = configReader.getString("db.user");
        DB_password = configReader.getString("db.password");
        topic = configReader.getString("mqtt.topic");
        MqttBrokerUrl = configReader.getString("mqtt.brokerUrl");
        MqttClientId = configReader.getString("mqtt.clientId");
        coapURLTemp = configReader.getString("coap.url.temp");
        coapURLHum = configReader.getString("coap.url.hum");
        
        constant.setTARGET_TEMPERATURE(configReader.getInt("ideal_temperature"));
        maxTemperature = configReader.getInt("max_temperature");
        minTemperature = configReader.getInt("min_temperature");
        maxHumidity = configReader.getInt("max_humidity");
        minHumidity = configReader.getInt("min_humidity");
   }
	

    public static void printCommand()
    {
        System.out.println("##########################################");
        System.out.println("WELCOME");
        System.out.println("##########################################");
        System.out.println("\n");
        
        System.out.println("List of the commands:");
        System.out.println("'SHOWAVG'      -> Show average temperature");
        System.out.println("'SHOWMAX'      -> Show maximum temperatures");
        System.out.println("'SHOWMIN'      -> Show minimum temperatures");
        System.out.println("'SHOWIDEAL'    -> Show ideal desidered temperatures");
        System.out.println("'SHOWLAST'     -> Show last registered temperatures");
        System.out.println("'SETMAX'       -> Set max threshold temperature");
        System.out.println("'SETMIN'       -> Set min threshold temperature");
        System.out.println("'DECREASE'     -> Turn on air conditioning system");
        System.out.println("'INCREASE'     -> Turn on heating system");
        System.out.println("'STOPCONTROL'  -> The control return to the actuator");
        System.out.println("'ACTUATORSTATE'-> Show the state of the actuator");
        System.out.println("'HELP'	       -> Show the list of commands");
        System.out.println("'OUT'          -> Exit from the program");
        
        System.out.println("\n");
    }
    
    public static void sendMessageToActuator(String coapMessage, String mqttMessage) {   	  	
    	if(coapMessage.equals("Show actuator state")) {
    		if(coapClientTemp.performGetRequest("Temperature") && coapClientHum.performGetRequest("Humidity"))
    			publicMessageToActuator(mqttMessage);
    	} else {
	    	if(coapClientTemp.performPostRequest(coapMessage))
	    		publicMessageToActuator(mqttMessage);
    	}
    }

    public static void publicMessageToActuator(String mqttMessage)
    {
    	try {
    		if(!mqttMessage.trim().isEmpty()) {
    			if(mqttMessage.equals("increase") || mqttMessage.equals("decrease"))
    				mqttHandler.publishMessage("Actuator", "{\"Sensor\":\"temperature\", \"payload\":\"" + mqttMessage + "\"}");
    			else	
    				mqttHandler.publishMessage("Actuator", mqttMessage);
    		}
		} catch (MqttException e) {
	        System.out.println(ANSI_RED + "Errore durante l'invio del messaggio MQTT: " + e.getMessage() + ANSI_RESET);
	    }
    }
    
    public static void chooseCommand()
    {
        Scanner scanner = new Scanner(System.in);
        String input;

        do {
            System.out.println("Enter a command: ");
            input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                System.out.println(ANSI_RED + "Input is empty. Please try again." + ANSI_RESET);
                continue;
            }
            
            int maxLength = 15;
            if (input.length() > maxLength) {
            	System.out.println(ANSI_RED + "The maximum allowed length is " + maxLength + " characters." + ANSI_RESET);
                input = input.substring(0, maxLength);
                continue;
            }

            // Il comando potrebbe essere composto da una sola parte, per esempio "SHOWMAX", oppure da due parti distinte, che
            // indicano il comando e il parametro da usare, per esempio "SETIDEAL 28". Per questo bisogna splittare la stringa inserita
            // e controllare che sia del formato giusto
            String[] tokens = input.split("\\s+", 2);  // Dividi l'input in due parti separate da uno o più spazi

            String command = tokens[0].toUpperCase();  
            
            if(input.equalsIgnoreCase("OUT")) {
            	System.out.println("Goodbye and thank you for using the collector!\n");
            	database.closeConnection();
            	mqttHandler.disconnect();
            	scanner.close();
            	System.exit(0);
            }
            
            // Caso in cui c'è un parametro
            if (tokens.length > 1) {
                String parameter = tokens[1]; 
		
                if (parameter.contains(" ")) {
                    System.out.println(ANSI_RED + "Invalid command: there are too much parameter. Please try again." + ANSI_RESET);
                    continue;
                }
		
                // recupero il valore del parametro
                int Value = 0;
                try {
                    Value = Integer.parseInt(parameter);
                } catch (NumberFormatException intException) {
                    System.err.println(ANSI_RED + "Errore: Il parametro non è un valore intero valido" + ANSI_RESET);
                    intException.printStackTrace();
                }
                
				if(Value < minTemperature || Value > maxTemperature)
				{
				    System.out.println(ANSI_RED + "Invalid parameter. Please try again." + ANSI_RESET);
		                    continue;
				}
				
                switch (command) {
                    case "SETMAX":
                    	if(Value <= minTemperature)
                    		System.out.println(ANSI_RED + "Invalid parameter. " + ANSI_RESET);
                    	else {
	                        System.out.println(ANSI_GREEN + "Setting max temperature to: " + parameter + ANSI_RESET);
	                        maxTemperature = Value;
                    	}
                        break;
                    case "SETMIN":
                    	if(Value >= maxTemperature)
                    		System.out.println(ANSI_RED + "Invalid parameter. " + ANSI_RESET);
                    	else {
	                        System.out.println(ANSI_GREEN + "Setting min temperature to: " + parameter + ANSI_RESET);
	                        minTemperature = Value;
                    	}
                        break;
                    default: 
                        System.out.println(ANSI_RED + "Invalid command. Please try again." + command + ANSI_RESET);
                }
            } else {
            	MeasurementsDAO measurementsDAO = new MeasurementsDAO(database.getConnection());
                switch (command) {
	                case "SHOWAVG":
	                    double[] avg = measurementsDAO.getStatistics("AVG");
	                    System.out.println(ANSI_GREEN + "Average temperature: " + avg[0] + ANSI_RESET);
	                    System.out.println(ANSI_GREEN + "Average humidity: " + avg[1] + ANSI_RESET);
	                    break;
	                case "SHOWMAX":
	                    double[] max = measurementsDAO.getStatistics("MAX");
	                    System.out.println(ANSI_GREEN + "Maximum temperature: " + max[0] + ANSI_RESET);
	                    System.out.println(ANSI_GREEN + "Maximum humidity: " + max[1] + ANSI_RESET);
	                    break;
	                case "SHOWMIN":
	                    double[] min = measurementsDAO.getStatistics("MIN");
	                    System.out.println(ANSI_GREEN + "Minimum temperature: " + min[0] + ANSI_RESET);
	                    System.out.println(ANSI_GREEN + "Minimum humidity: " + min[1] + ANSI_RESET);
	                    break;
	                case "SHOWIDEAL":
	                    System.out.println(ANSI_GREEN + "Showing ideal temperature..." + ANSI_RESET);
	                    System.out.println(ANSI_GREEN + constant.getTARGET_TEMPERATURE() + ANSI_RESET);
	                    break;
	                case "SHOWLAST":
	                    System.out.println(ANSI_GREEN + "Showing last registered measurement..." + ANSI_RESET);
	                    
	                    constant.setLAST_MEASUREMENT(measurementsDAO.findLastMeasurements()); // update variable with last registered value
	                    
	                    MeasurementsDTO temp = constant.getLAST_MEASUREMENT();
	                    
	                    System.out.println(ANSI_GREEN + "Node ID: " + temp.getSensorID() + ANSI_RESET);
	                    System.out.println(ANSI_GREEN + "Timestamp: " + temp.getTimestamp() + ANSI_RESET);
	                    System.out.println(ANSI_GREEN + "Temperature: " + temp.getTemperature() + ANSI_RESET);
	                    System.out.println(ANSI_GREEN + "Humidity: " + temp.getHumidity() + ANSI_RESET);
	                    
	                    break;
                    case "INCREASE":
                    	mqttHandler.set_modality(true);
                    	sendMessageToActuator("Turn off actuator","increase");
                    	break;
                    case "DECREASE":
                    	mqttHandler.set_modality(true);
                    	sendMessageToActuator("Turn off actuator","decrease");
                    	break;
                    case "STOPCONTROL":
                    	mqttHandler.set_modality(false);
                    	sendMessageToActuator("Turn on actuator","ON");
                    	break;
                    case "ACTUATORSTATE":
                    	sendMessageToActuator("Show actuator state"," ");
                    	break;
                    case "HELP":
                    	printCommand();
                    	break;                    	
                    default:
                        System.out.println(ANSI_RED + "Invalid command: " + command +  ". Please try again." + ANSI_RESET);
                }
            }
        } while (true);
    }
}


_RESET);
                }
            }
        } while (true);
    }
}


