package iot.unipi.it.DBMaintaince;

import java.sql.SQLException;

public class DBSingleton {
    private static DBDriver instance;

    private DBSingleton() {
        // Costruttore privato per evitare l'istanziazione diretta
    }

    public static DBDriver createDBManager(String URL, String user, String password) throws SQLException {
        if(instance == null)
            instance = new DBDriver(URL, user, password);
        return instance;
    }

    public static DBDriver getDbInstance(){
        return instance;
    }
}
