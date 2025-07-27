package org.edgecomputing.simulation;

import org.edgecomputing.model.IoTDevice;
import org.edgecomputing.model.IoTTask;
import org.edgecomputing.utils.ConfigUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates IoT tasks for the simulation with realistic workload patterns.
 * Supports different task arrival patterns and distributions.
 */
public class TaskGenerator {
    // Task ID generator
    private static final AtomicInteger taskIdGenerator = new AtomicInteger(0);
    
    // Task generation parameters
    private final double minTaskLength;      // in MI (million instructions)
    private final double maxTaskLength;      // in MI
    private final double minInputSize;       // in KB
    private final double maxInputSize;       // in KB
    private final double minOutputSize;      // in KB
    private final double maxOutputSize;      // in KB
    private final double minDeadline;        // in seconds
    private final double maxDeadline;        // in seconds
    private final double priorityHigh;       // probability of high priority tasks
    private final double priorityMedium;     // probability of medium priority tasks
    
    // Task arrival patterns
    private enum ArrivalPattern {
        UNIFORM,        // Uniform arrival rate
        POISSON,        // Poisson process
        BURSTY,         // Bursty arrivals
        DIURNAL         // Time-of-day dependent
    }
    
    private final ArrivalPattern arrivalPattern;
    private final double meanArrivalRate;    // tasks per second
    private final Random random;
    
    // For diurnal pattern
    private double currentSimTime = 0.0;
    private static final double SECONDS_PER_DAY = 86400.0;
    
    /**
     * Constructor with configuration parameters
     */
    public TaskGenerator() {
        // Load task parameters from configuration
        this.minTaskLength = ConfigUtils.getDoubleProperty("task_min_length", 10.0);
        this.maxTaskLength = ConfigUtils.getDoubleProperty("task_max_length", 500.0);
        this.minInputSize = ConfigUtils.getDoubleProperty("task_min_input_size", 10.0);
        this.maxInputSize = ConfigUtils.getDoubleProperty("task_max_input_size", 1000.0);
        this.minOutputSize = ConfigUtils.getDoubleProperty("task_min_output_size", 1.0);
        this.maxOutputSize = ConfigUtils.getDoubleProperty("task_max_output_size", 100.0);
        this.minDeadline = ConfigUtils.getDoubleProperty("task_min_deadline", 2.0);
        this.maxDeadline = ConfigUtils.getDoubleProperty("task_max_deadline", 20.0);
        this.priorityHigh = ConfigUtils.getDoubleProperty("task_priority_high_prob", 0.2);
        this.priorityMedium = ConfigUtils.getDoubleProperty("task_priority_medium_prob", 0.5);
        
        // Set arrival pattern
        String patternStr = ConfigUtils.getProperty("task_arrival_pattern", "POISSON").toUpperCase();
        ArrivalPattern pattern;
        try {
            pattern = ArrivalPattern.valueOf(patternStr);
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid arrival pattern: " + patternStr + ". Using POISSON.");
            pattern = ArrivalPattern.POISSON;
        }
        this.arrivalPattern = pattern;
        
        this.meanArrivalRate = ConfigUtils.getDoubleProperty("task_arrival_rate", 5.0);
        
        // Initialize random number generator
        long seed = ConfigUtils.getIntProperty("random_seed", 42);
        this.random = new Random(seed);
    }
    
    /**
     * Generate a new IoT task with random characteristics
     * @param sourceDevice The device that generates the task
     * @param submissionTime The time when the task is submitted
     * @return A new IoT task
     */
    public IoTTask generateTask(IoTDevice sourceDevice, double submissionTime) {
        // Generate task characteristics
        int taskId = taskIdGenerator.incrementAndGet();
        String taskName = "Task_" + taskId;
        
        // Random task length (computational requirement)
        double taskLength = minTaskLength + random.nextDouble() * (maxTaskLength - minTaskLength);
        
        // Random data sizes
        double inputSize = minInputSize + random.nextDouble() * (maxInputSize - minInputSize);
        double outputSize = minOutputSize + random.nextDouble() * (maxOutputSize - minOutputSize);
        
        // Random deadline
        double deadline = submissionTime + minDeadline + random.nextDouble() * (maxDeadline - minDeadline);
        
        // Random priority (1-high, 2-medium, 3-low)
        int priority;
        double priorityRand = random.nextDouble();
        if (priorityRand < priorityHigh) {
            priority = 1; // High
        } else if (priorityRand < priorityHigh + priorityMedium) {
            priority = 2; // Medium
        } else {
            priority = 3; // Low
        }
        
        // Create task - using new constructor with IoTDevice directly
        // Parameters: id, sourceDevice, taskLength, inputSize, outputSize, deadline
        IoTTask task = new IoTTask(taskId, sourceDevice, (long)taskLength, inputSize, outputSize, deadline);
        // Priority is not directly set in constructor, would need to be added to IoTTask class if required
        
        return task;
    }
    
    /**
     * Calculate the next task arrival time based on the configured arrival pattern
     * @param currentTime Current simulation time
     * @return Time until the next task arrival in seconds
     */
    public double getNextArrivalTime(double currentTime) {
        this.currentSimTime = currentTime;
        
        switch (arrivalPattern) {
            case UNIFORM:
                // Uniform arrival with fixed rate
                return 1.0 / meanArrivalRate;
                
            case POISSON:
                // Poisson process (exponential inter-arrival times)
                return -Math.log(1.0 - random.nextDouble()) / meanArrivalRate;
                
            case BURSTY:
                // Bursty traffic model with 20% chance of burst
                if (random.nextDouble() < 0.2) {
                    // During burst, 5x the normal rate
                    return -Math.log(1.0 - random.nextDouble()) / (meanArrivalRate * 5);
                } else {
                    // Normal rate
                    return -Math.log(1.0 - random.nextDouble()) / meanArrivalRate;
                }
                
            case DIURNAL:
                // Time-of-day dependent arrival rate
                // Map current time to a position in a day (0-24h)
                double timeOfDay = (currentTime % SECONDS_PER_DAY) / 3600.0; // Hour of the day
                
                // Calculate rate multiplier based on time of day
                // Peak during working hours, low at night
                double rateMultiplier = getDiurnalRateMultiplier(timeOfDay);
                
                // Calculate arrival time with adjusted rate
                return -Math.log(1.0 - random.nextDouble()) / (meanArrivalRate * rateMultiplier);
                
            default:
                // Default to Poisson
                return -Math.log(1.0 - random.nextDouble()) / meanArrivalRate;
        }
    }
    
    /**
     * Calculate the rate multiplier for diurnal (time-of-day) pattern
     * @param hourOfDay Hour of the day (0-24)
     * @return Rate multiplier (0.0-2.0)
     */
    private double getDiurnalRateMultiplier(double hourOfDay) {
        // Night (0-6h): low activity
        if (hourOfDay < 6.0) {
            return 0.2;
        }
        // Morning ramp-up (6-9h)
        else if (hourOfDay < 9.0) {
            return 0.2 + (hourOfDay - 6.0) * 0.6; // Linear increase from 0.2 to 2.0
        }
        // Work day peak (9-17h)
        else if (hourOfDay < 17.0) {
            return 2.0;
        }
        // Evening ramp-down (17-22h)
        else if (hourOfDay < 22.0) {
            return 2.0 - (hourOfDay - 17.0) * 0.36; // Linear decrease from 2.0 to 0.2
        }
        // Late night (22-24h)
        else {
            return 0.2;
        }
    }
    
    /**
     * Generate a batch of tasks for initial testing
     * @param devices List of IoT devices
     * @param startTime Start time for task generation
     * @param count Number of tasks to generate
     * @return List of generated tasks
     */
    public List<IoTTask> generateInitialTaskBatch(List<IoTDevice> devices, double startTime, int count) {
        List<IoTTask> tasks = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            // Choose a random device as the source
            IoTDevice device = devices.get(random.nextInt(devices.size()));
            
            // Generate task with slight time offset
            double submissionTime = startTime + i * 0.01;
            tasks.add(generateTask(device, submissionTime));
        }
        
        return tasks;
    }
}
