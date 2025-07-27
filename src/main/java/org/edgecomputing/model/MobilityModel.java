package org.edgecomputing.model;

import org.edgecomputing.utils.ConfigUtils;

import java.util.List;
import java.util.Random;

/**
 * Implements mobility patterns for IoT devices in the simulation.
 * Supports different mobility models including random walk, random waypoint,
 * and group mobility patterns.
 */
public class MobilityModel {
    // Mobility types
    public enum MobilityPattern {
        STATIC,          // No movement
        RANDOM_WALK,     // Random direction and speed
        RANDOM_WAYPOINT, // Move to random waypoints
        GROUP_MOBILITY   // Group-based movement
    }
    
    // Mobility parameters
    private double areaWidth;               // Width of simulation area in meters
    private double areaHeight;              // Height of simulation area in meters
    private double minSpeed;                 // Minimum speed in m/s
    private double maxSpeed;                 // Maximum speed in m/s
    private double updateInterval;           // How often to update positions in seconds
    private MobilityPattern pattern;         // Mobility pattern to use
    
    private final Random random;              // Random number generator
    
    /**
     * Constructor with configuration parameters
     */
    public MobilityModel() {
        // Load mobility parameters from configuration
        this.areaWidth = ConfigUtils.getDoubleProperty("mobility_area_width", 1000.0);
        this.areaHeight = ConfigUtils.getDoubleProperty("mobility_area_height", 1000.0);
        this.minSpeed = ConfigUtils.getDoubleProperty("mobility_min_speed", 0.5);  // 0.5 m/s ~ walking speed
        this.maxSpeed = ConfigUtils.getDoubleProperty("mobility_max_speed", 2.0);  // 2.0 m/s ~ fast walking
        this.updateInterval = ConfigUtils.getDoubleProperty("mobility_update_interval", 1.0);
        
        // Set mobility pattern
        String patternStr = ConfigUtils.getProperty("mobility_pattern", "RANDOM_WALK").toUpperCase();
        try {
            this.pattern = MobilityPattern.valueOf(patternStr);
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid mobility pattern: " + patternStr + ". Using RANDOM_WALK.");
            this.pattern = MobilityPattern.RANDOM_WALK;
        }
        
        // Initialize random number generator
        long seed = ConfigUtils.getIntProperty("random_seed", 42);
        this.random = new Random(seed);
    }
    
    /**
     * Initialize device positions within the simulation area
     * @param devices List of IoT devices to initialize positions for
     */
    public void initializeDevicePositions(List<IoTDevice> devices) {
        for (IoTDevice device : devices) {
            // Random position within area
            double x = random.nextDouble() * areaWidth;
            double y = random.nextDouble() * areaHeight;
            device.setPosition(x, y);
            
            // Initialize movement parameters based on pattern
            if (pattern == MobilityPattern.RANDOM_WAYPOINT) {
                // Set random target waypoint
                double targetX = random.nextDouble() * areaWidth;
                double targetY = random.nextDouble() * areaHeight;
                device.setTargetPosition(targetX, targetY);
                
                // Random speed
                double speed = minSpeed + random.nextDouble() * (maxSpeed - minSpeed);
                device.setSpeed(speed);
            } else if (pattern == MobilityPattern.RANDOM_WALK) {
                // Random direction (angle in radians)
                double direction = random.nextDouble() * 2 * Math.PI;
                device.setDirection(direction);
                
                // Random speed
                double speed = minSpeed + random.nextDouble() * (maxSpeed - minSpeed);
                device.setSpeed(speed);
            }
        }
    }
    
    /**
     * Update device positions based on the mobility model
     * @param devices List of IoT devices to update
     * @param elapsedTime Time elapsed since last update in seconds
     */
    public void updateDevicePositions(List<IoTDevice> devices, double elapsedTime) {
        // Skip if static pattern or no time elapsed
        if (pattern == MobilityPattern.STATIC || elapsedTime <= 0) {
            return;
        }
        
        for (IoTDevice device : devices) {
            switch (pattern) {
                case RANDOM_WALK:
                    updateRandomWalkPosition(device, elapsedTime);
                    break;
                case RANDOM_WAYPOINT:
                    updateRandomWaypointPosition(device, elapsedTime);
                    break;
                case GROUP_MOBILITY:
                    // Group mobility is not implemented in this basic version
                    // but could be added for more complex scenarios
                    updateRandomWalkPosition(device, elapsedTime);
                    break;
                default:
                    // Do nothing for static devices
                    break;
            }
        }
    }
    
    /**
     * Update device position based on Random Walk mobility model
     * @param device IoT device to update
     * @param elapsedTime Time elapsed since last update in seconds
     */
    private void updateRandomWalkPosition(IoTDevice device, double elapsedTime) {
        // Get current position and movement parameters
        double x = device.getXPos();
        double y = device.getYPos();
        double speed = device.getSpeed();
        double direction = device.getDirection();
        
        // Calculate new position based on speed, direction and elapsed time
        double distance = speed * elapsedTime;
        double newX = x + distance * Math.cos(direction);
        double newY = y + distance * Math.sin(direction);
        
        // Check if device hits boundary and bounce
        boolean changedDirection = false;
        if (newX < 0) {
            newX = -newX;
            direction = Math.PI - direction;
            changedDirection = true;
        } else if (newX > areaWidth) {
            newX = 2 * areaWidth - newX;
            direction = Math.PI - direction;
            changedDirection = true;
        }
        
        if (newY < 0) {
            newY = -newY;
            direction = 2 * Math.PI - direction;
            changedDirection = true;
        } else if (newY > areaHeight) {
            newY = 2 * areaHeight - newY;
            direction = 2 * Math.PI - direction;
            changedDirection = true;
        }
        
        // Occasionally change direction randomly
        if (!changedDirection && random.nextDouble() < 0.1) {
            // Change direction by up to ±45 degrees
            direction += (random.nextDouble() - 0.5) * Math.PI / 2;
            // Normalize direction to [0, 2*PI]
            direction = (direction + 2 * Math.PI) % (2 * Math.PI);
        }
        
        // Update device position and direction
        device.setPosition(newX, newY);
        device.setDirection(direction);
    }
    
    /**
     * Update device position based on Random Waypoint mobility model
     * @param device IoT device to update
     * @param elapsedTime Time elapsed since last update in seconds
     */
    private void updateRandomWaypointPosition(IoTDevice device, double elapsedTime) {
        // Get current position, target and speed
        double x = device.getXPos();
        double y = device.getYPos();
        double targetX = device.getTargetX();
        double targetY = device.getTargetY();
        double speed = device.getSpeed();
        
        // Calculate distance to target
        double deltaX = targetX - x;
        double deltaY = targetY - y;
        double distanceToTarget = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        
        // If close to target, choose a new waypoint
        if (distanceToTarget < speed * elapsedTime) {
            // Arrived at waypoint, set new target
            targetX = random.nextDouble() * areaWidth;
            targetY = random.nextDouble() * areaHeight;
            device.setTargetPosition(targetX, targetY);
            
            // Pause for a bit (simulated by reducing effective speed)
            if (random.nextDouble() < 0.3) { // 30% chance to pause
                speed = speed * 0.1; // Slow down to simulate pause
            } else {
                // Set new random speed
                speed = minSpeed + random.nextDouble() * (maxSpeed - minSpeed);
            }
            device.setSpeed(speed);
            
            // Recalculate delta and distance
            deltaX = targetX - x;
            deltaY = targetY - y;
            distanceToTarget = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        }
        
        // Calculate movement direction
        double direction = Math.atan2(deltaY, deltaX);
        
        // Calculate new position
        double distance = Math.min(speed * elapsedTime, distanceToTarget);
        double newX = x + distance * Math.cos(direction);
        double newY = y + distance * Math.sin(direction);
        
        // Update device position
        device.setPosition(newX, newY);
    }
    
    /**
     * Get the mobility update interval
     * @return Update interval in seconds
     */
    public double getUpdateInterval() {
        return updateInterval;
    }
    
    /**
     * Get current mobility pattern
     */
    public MobilityPattern getPattern() {
        return pattern;
    }
    
    /**
     * Set mobility pattern
     * @param pattern Mobility pattern to use
     */
    public void setPattern(MobilityPattern pattern) {
        this.pattern = pattern;
    }
    
    /**
     * Set mobility update interval
     * @param updateInterval Update interval in seconds
     */
    public void setUpdateInterval(double updateInterval) {
        this.updateInterval = updateInterval;
    }
    
    /**
     * Get minimum speed
     * @return Minimum speed in m/s
     */
    public double getMinSpeed() {
        return minSpeed;
    }
    
    /**
     * Set minimum speed
     * @param minSpeed Minimum speed in m/s
     */
    public void setMinSpeed(double minSpeed) {
        this.minSpeed = minSpeed;
    }
    
    /**
     * Get maximum speed
     * @return Maximum speed in m/s
     */
    public double getMaxSpeed() {
        return maxSpeed;
    }
    
    /**
     * Set maximum speed
     * @param maxSpeed Maximum speed in m/s
     */
    public void setMaxSpeed(double maxSpeed) {
        this.maxSpeed = maxSpeed;
    }
    
    /**
     * Set the simulation area size
     * @param width Width of the area in meters
     * @param height Height of the area in meters
     */
    public void setAreaSize(int width, int height) {
        this.areaWidth = width;
        this.areaHeight = height;
    }
}
