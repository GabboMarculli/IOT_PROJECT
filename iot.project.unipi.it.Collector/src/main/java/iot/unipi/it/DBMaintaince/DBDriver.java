package iot.unipi.it.DBMaintaince;
//import iot.unipi.it.Model.Actuator;
//import iot.unipi.it.Model.Sensor;

import java.sql.*;

public class DBDriver {
    private static Connection connection;
    private static String DB_URL;
    private static String user;
    private static String password;

    public DBDriver(String URL, String user, String password) throws SQLException {
        DB_URL = URL;
        this.user = user;
        this.password = password;
        
        try {
        	connection = DriverManager.getConnection(URL, user, password);
        } catch(SQLException e){
            e.printStackTrace();
        }
    }
    
    public static Connection getConnection() {
    	return connection;
    }

    public void closeConnection(){
        try {
        	if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing database connection: " + e.getMessage());
        }
    }

    public PreparedStatement prepareStatement(String query) throws SQLException {
        return connection.prepareStatement(query);
    }

    public int executeUpdate(PreparedStatement statement) throws SQLException {
        return statement.executeUpdate();
    }

    public ResultSet executeQuery(PreparedStatement statement) throws SQLException {
        return statement.executeQuery();
    }

    public void handleSQLException(SQLException e) {
        e.printStackTrace();
    }
}


    public void handleSQLException(SQLException e) {
        e.printStackTrace();
    }
}
