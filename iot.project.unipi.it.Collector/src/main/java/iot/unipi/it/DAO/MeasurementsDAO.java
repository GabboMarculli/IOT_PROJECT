package iot.unipi.it.DAO;
import iot.unipi.it.DTO.MeasurementsDTO;
import java.sql.Timestamp;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MeasurementsDAO {
    private Connection connection;
    private PreparedStatement pstRetrieveMeasurements;
    private static PreparedStatement pstInsertMeasurements;
    private PreparedStatement pstDeleteMeasurements;
    private static PreparedStatement pstLastMeasurements;

    public MeasurementsDAO(Connection connection) {
        this.connection = connection;

        try {
            pstRetrieveMeasurements = connection.prepareStatement("SELECT * FROM Measurements WHERE SensorID = ? AND Timestamp = ?");
            pstInsertMeasurements = connection.prepareStatement("INSERT INTO Measurements (SensorID, Timestamp, Temperature, Humidity) VALUES (?, ?, ?, ?)");
            pstDeleteMeasurements = connection.prepareStatement("DELETE FROM Measurements WHERE SensorID = ? AND Timestamp = ?");
            pstLastMeasurements = connection.prepareStatement("SELECT SensorID, Timestamp, Temperature, Humidity FROM Measurements ORDER BY Timestamp DESC LIMIT 1");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public MeasurementsDTO retrieveMeasurements(int id, Timestamp time) {
        MeasurementsDTO Measurements = null;

        try {
            pstRetrieveMeasurements.setInt(1, id);
            pstRetrieveMeasurements.setTimestamp(2,time);
            ResultSet rs = pstRetrieveMeasurements.executeQuery();

            if (rs.next()) {
            	int temp = rs.getInt("Temperature");
            	int hum = rs.getInt("Humidity");

                Measurements = new MeasurementsDTO(id, time, temp,hum);
                System.out.println(Measurements.toString());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return Measurements;
    }

    public static void insertMeasurements(MeasurementsDTO Measurements) {
        try {
            pstInsertMeasurements.setInt(1, Measurements.getSensorID());
            pstInsertMeasurements.setTimestamp(2, Measurements.getTimestamp());
            pstInsertMeasurements.setInt(3, Measurements.getTemperature());
            pstInsertMeasurements.setInt(4, Measurements.getHumidity());

            pstInsertMeasurements.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public static MeasurementsDTO findLastMeasurements()
    {
    	MeasurementsDTO Measurements = null;

        try {
            ResultSet rs = pstLastMeasurements.executeQuery();

            if (rs.next()) {
                int id = rs.getInt("SensorID");
                Timestamp time = rs.getTimestamp("Timestamp");
                int temperature = rs.getInt("Temperature");
                int humidity = rs.getInt("Humidity");
                    
                Measurements = new MeasurementsDTO(id, time, temperature, humidity);
            } else {
            	Measurements = new MeasurementsDTO(0,null,0, 0);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return Measurements;
    }
    
    
    public double[] getStatistics(String statistic) {
    	if (!statistic.equals("AVG") && !statistic.equals("MIN") && !statistic.equals("MAX")) 
    	    return null;

    	String query = "Select " + statistic + "(Temperature) AS val1, "
				    			 + statistic + "(Humidity) AS val2 "
				    			 + "FROM Measurements";
    	  	
    	double[] ris = new double[2];
    	try(PreparedStatement pstStatMeasurements = connection.prepareStatement(query)){
	    	try (ResultSet resultSet = pstStatMeasurements.executeQuery()) {
	    		if (resultSet.next())
	            	ris[0] = resultSet.getDouble("val1");
                	ris[1] = resultSet.getDouble("val2");
	        	}
    	}catch (SQLException e) {
            e.printStackTrace();
        }
            
        return ris; 
    }

    public void deleteMeasurements(int id, Timestamp timestamp) {
        try {
            pstDeleteMeasurements.setInt(1, id);
            pstDeleteMeasurements.setTimestamp(2,timestamp);
            pstDeleteMeasurements.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
