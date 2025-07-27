package org.edgecomputing.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Utility class for loading and managing configuration properties.
 */
public class ConfigUtils {
    private static Properties properties = null;
    
    /**
     * Load configuration from a file
     * @param configFile Path to the configuration file
     * @return Properties object with the loaded configuration
     */
    public static Properties loadConfig(String configFile) {
        properties = new Properties();
        
        try (InputStream input = new FileInputStream(configFile)) {
            properties.load(input);
            System.out.println("Configuration loaded from " + configFile);
        } catch (IOException e) {
            System.err.println("Error loading configuration from " + configFile + ": " + e.getMessage());
            
            // Load from classpath as fallback
            try (InputStream input = ConfigUtils.class.getClassLoader().getResourceAsStream("config.properties")) {
                if (input != null) {
                    properties.load(input);
                    System.out.println("Configuration loaded from classpath");
                } else {
                    System.err.println("Could not find default configuration in classpath");
                }
            } catch (IOException ex) {
                System.err.println("Error loading default configuration: " + ex.getMessage());
            }
        }
        
        return properties;
    }
    
    /**
     * Get a property value with a default fallback
     * @param key Property key
     * @param defaultValue Default value if property is not found
     * @return Property value or default if not found
     */
    public static String getProperty(String key, String defaultValue) {
        if (properties == null) {
            try (InputStream input = ConfigUtils.class.getClassLoader().getResourceAsStream("config.properties")) {
                properties = new Properties();
                if (input != null) {
                    properties.load(input);
                }
            } catch (IOException e) {
                System.err.println("Error loading default configuration: " + e.getMessage());
                properties = new Properties();
            }
        }
        
        return properties.getProperty(key, defaultValue);
    }
    
    /**
     * Get an integer property value with a default fallback
     * @param key Property key
     * @param defaultValue Default value if property is not found
     * @return Property value as integer or default if not found
     */
    public static int getIntProperty(String key, int defaultValue) {
        String value = getProperty(key, String.valueOf(defaultValue));
        try {
            // Remove comments (anything after #) and trim whitespace
            String cleanValue = value;
            if (cleanValue.contains("#")) {
                cleanValue = cleanValue.substring(0, cleanValue.indexOf('#'));
            }
            cleanValue = cleanValue.trim();
            
            return Integer.parseInt(cleanValue);
        } catch (NumberFormatException e) {
            System.err.println("Invalid integer value for property " + key + ": " + value);
            return defaultValue;
        }
    }
    
    /**
     * Get a double property value with a default fallback
     * @param key Property key
     * @param defaultValue Default value if property is not found
     * @return Property value as double or default if not found
     */
    public static double getDoubleProperty(String key, double defaultValue) {
        String value = getProperty(key, String.valueOf(defaultValue));
        try {
            // Remove comments (anything after #) and trim whitespace
            String cleanValue = value;
            if (cleanValue.contains("#")) {
                cleanValue = cleanValue.substring(0, cleanValue.indexOf('#'));
            }
            cleanValue = cleanValue.trim();
            
            return Double.parseDouble(cleanValue);
        } catch (NumberFormatException e) {
            System.err.println("Invalid double value for property " + key + ": " + value);
            return defaultValue;
        }
    }
    
    /**
     * Get a boolean property value with a default fallback
     * @param key Property key
     * @param defaultValue Default value if property is not found
     * @return Property value as boolean or default if not found
     */
    public static boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = getProperty(key, String.valueOf(defaultValue));
        return Boolean.parseBoolean(value);
    }
}
