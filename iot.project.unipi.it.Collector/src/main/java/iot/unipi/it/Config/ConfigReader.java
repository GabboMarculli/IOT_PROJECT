package iot.unipi.it.Config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigReader {
	private Properties properties;

    public ConfigReader(String configFile) {
        properties = new Properties();
        load(configFile);
    }
    
    public void load(String configFile) {
        try (FileInputStream inputStream = new FileInputStream(configFile)) {
            properties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getString(String key) {
        return properties.getProperty(key);
    }

    public int getInt(String key) {
        return Integer.parseInt(properties.getProperty(key));
    }

    public void setString(String key, String value) {
        properties.setProperty(key, value);
    }

    public void setInt(String key, int value) {
        properties.setProperty(key, String.valueOf(value));
    }

    
}
