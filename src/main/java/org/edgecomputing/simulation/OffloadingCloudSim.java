package org.edgecomputing.simulation;

import org.cloudbus.cloudsim.core.CloudSim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.edgecomputing.model.IoTDevice;

/**
 * Custom extension of CloudSim to support additional properties needed by our offloading simulation.
 * This class enables the simulation to share configuration parameters with the OffloadingDatacenterBroker.
 */
public class OffloadingCloudSim extends CloudSim {
    
    private boolean terminated = false;
    
    private final Map<String, Object> properties;
    
    /**
     * Creates a new CloudSim instance with support for additional properties.
     */
    public OffloadingCloudSim() {
        super();
        properties = new HashMap<>();
    }
    
    /**
     * Add a property to the simulation.
     * 
     * @param key Property key
     * @param value Property value
     */
    public void addProperty(String key, Object value) {
        properties.put(key, value);
    }
    
    /**
     * Set a property in the simulation.
     * Alias for addProperty for backward compatibility.
     * 
     * @param key Property key
     * @param value Property value as string
     */
    public void setProperty(String key, String value) {
        properties.put(key, value);
    }
    
    /**
     * Get a property from the simulation.
     * 
     * @param key Property key
     * @return Property value, or null if not found
     */
    public Object getProperty(String key) {
        return properties.get(key);
    }
    
    /**
     * Get a double property from the simulation.
     * 
     * @param key Property key
     * @param defaultValue Default value if property not found or not a double
     * @return Property value as double
     */
    public double getDoubleProperty(String key, double defaultValue) {
        Object value = getProperty(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }
    
    /**
     * Get an integer property from the simulation.
     * 
     * @param key Property key
     * @param defaultValue Default value if property not found or not an int
     * @return Property value as int
     */
    public int getIntProperty(String key, int defaultValue) {
        Object value = getProperty(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
    
    /**
     * Get a task arrival rate property.
     * 
     * @return Task arrival rate, or 1.0 if not defined
     */
    public double getTaskArrivalRate() {
        double rate = 1.0; // Default rate: 1 task per second
        try {
            Object value = getProperty("task_arrival_rate");
            if (value != null && value instanceof String) {
                rate = Double.parseDouble((String)value);
            } else if (value != null && value instanceof Number) {
                rate = ((Number)value).doubleValue();
            }
        } catch (Exception e) {
            System.out.println("Warning: Error parsing task arrival rate. Using default.");
        }
        return rate;
    }
    
    /**
     * Run the simulation for a given time
     * @param duration Duration to run the simulation
     * @return The clock value after the simulation completes
     */
    public double runFor(double duration) {
        // Run the simulation for a specific duration
        return super.runFor(duration);
    }
    
    /**
     * Start the simulation
     * 
     * @return The simulation clock value after finishing
     */
    public double startSimulation() {
        // CloudSim Plus may expect this implementation
        System.out.println("Starting OffloadingCloudSim simulation...");
        
        try {
            // Call the CloudSim Plus 5.0.0 start() method
            return super.start();
        } catch (Exception e) {
            System.err.println("Error starting simulation: " + e.getMessage());
            e.printStackTrace();
            return 0.0;
        }
    }
    
    /**
     * Terminate the simulation
     * 
     * @param exitCode Exit code for the simulation
     */
    public void terminateSimulation(int exitCode) {
        System.out.println("Terminating simulation with exit code: " + exitCode);
        // CloudSim Plus 5.0.0 uses terminate()
        super.terminate();
        this.terminated = true;
    }
    
    /**
     * Get the list of IoT devices in the simulation
     * Stub for compatibility with test code
     * 
     * @return Empty list as this is a stub implementation
     */
    public List<IoTDevice> getIoTDevices() {
        // This is a stub method - in a real implementation, this would return
        // actual IoT devices registered with the simulation
        return new ArrayList<>();
    }
}
