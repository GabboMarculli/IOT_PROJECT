package iot.unipi.it.DTO;

public class MeasurementsDTO {
    private int sensorID;
    private java.sql.Timestamp timestamp;
    private int Temperature;
    private int Humidity;

    public MeasurementsDTO(int sensorID, java.sql.Timestamp timestamp, int Temperature, int Humidity) {
        this.sensorID = sensorID;
        this.timestamp = timestamp;
        this.Temperature = Temperature;
        this.Humidity = Humidity;
    }

    public int getSensorID() {
        return sensorID;
    }

    public java.sql.Timestamp getTimestamp() {
        return timestamp;
    }

    public int getTemperature() {
        return Temperature;
    }
    
    public int getHumidity() {
    	return Humidity;
    }
    
    @Override
    public String toString() {
        return "MeasurementsDTO{" +
        	"sensorID='" + sensorID + '\'' +
                ", timestamp=" + timestamp +
                ", Temperature=" + Temperature +
                ", Humidity=" + Humidity +
                '}';
    }
}
