package org.edgecomputing.simulation;

import org.cloudbus.cloudsim.core.CloudSim;
import org.edgecomputing.drl.DRLAgent;
import org.edgecomputing.drl.OffloadingEnvironment;
import org.edgecomputing.metrics.PerformanceMetrics;
import org.edgecomputing.model.CloudDatacenter;
import org.edgecomputing.model.EdgeServer;
import org.edgecomputing.model.IoTDevice;
import org.edgecomputing.model.IoTTask;
import org.edgecomputing.model.MobilityModel;
import org.edgecomputing.utils.ConfigUtils;
import org.edgecomputing.simulation.OffloadingCloudSim;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Arrays;
import java.util.ArrayList;

/**
 * Main simulation class that orchestrates the task offloading simulation
 * using the DRL agent for decision making.
 */
public class OffloadingSimulation {
    // Simulation entities
    private final List<IoTDevice> devices;
    private final List<EdgeServer> edgeServers;
    private final CloudDatacenter cloudDatacenter;
    private final OffloadingEnvironment environment;
    private final DRLAgent drlAgent;
    private final MobilityModel mobilityModel;
    
    // Simulation parameters
    private final int numDevices;
    private final int numEdgeServers;
    private final double simulationTime;
    private final double taskArrivalRate;
    private final Random random;
    
    // Metrics collection
    private final PerformanceMetrics metrics;
    
    // Simulation state
    private double currentTime;
    private int totalTasks;
    private boolean isTrainingPhase;
    
    /**
     * Constructor to initialize the simulation environment
     */
    public OffloadingSimulation(Properties config) {
        // Load simulation parameters
        this.numDevices = Integer.parseInt(config.getProperty("num_mobile_devices", "10"));
        this.numEdgeServers = Integer.parseInt(config.getProperty("num_edge_servers", "3"));
        this.simulationTime = Double.parseDouble(config.getProperty("simulation_time", "3600"));
        this.taskArrivalRate = Double.parseDouble(config.getProperty("task_arrival_rate", "5"));
        this.random = new Random(Long.parseLong(config.getProperty("seed", "42")));
        
        // Initialize simulation entities
        this.devices = createDevices(config);
        this.edgeServers = createEdgeServers(config);
        this.cloudDatacenter = createCloudDatacenter(config);
        
        // Initialize mobility model
        this.mobilityModel = new MobilityModel();
        
        // Initialize device positions based on mobility model
        mobilityModel.initializeDevicePositions(devices);
        
        // Create environment for the DRL agent
        double mobileBandwidth = Double.parseDouble(config.getProperty("bandwidth_mobile_to_edge", "10"));
        double edgeBandwidth = Double.parseDouble(config.getProperty("bandwidth_edge_to_cloud", "100"));
        double cloudBandwidth = edgeBandwidth; // Same as edge-to-cloud
        double mobileEdgeLatency = Double.parseDouble(config.getProperty("latency_mobile_to_edge", "10"));
        double edgeCloudLatency = Double.parseDouble(config.getProperty("latency_edge_to_cloud", "50"));
        
        this.environment = new OffloadingEnvironment(
            devices, edgeServers, cloudDatacenter,
            mobileBandwidth, edgeBandwidth, cloudBandwidth,
            mobileEdgeLatency, edgeCloudLatency,
            0.6, 0.3, 0.1 // Weights for latency, energy, and load balance
        );
        
        // Create DRL agent
        double learningRate = Double.parseDouble(config.getProperty("learning_rate", "0.001"));
        double discountFactor = Double.parseDouble(config.getProperty("discount_factor", "0.95"));
        double explorationRate = Double.parseDouble(config.getProperty("exploration_rate", "0.1"));
        int batchSize = Integer.parseInt(config.getProperty("batch_size", "32"));
        int replayMemorySize = Integer.parseInt(config.getProperty("replay_memory_size", "10000"));
        int targetUpdateFreq = Integer.parseInt(config.getProperty("target_update_frequency", "100"));
        
        this.drlAgent = new DRLAgent(
            environment, learningRate, discountFactor, explorationRate,
            batchSize, replayMemorySize, targetUpdateFreq
        );
        
        // Initialize metrics collection
        this.metrics = new PerformanceMetrics();
        
        // Initialize simulation state
        this.currentTime = 0.0;
        this.totalTasks = 0;
        this.isTrainingPhase = true;
        
        System.out.println("Simulation initialized with:");
        System.out.println("  - " + numDevices + " IoT devices");
        System.out.println("  - " + numEdgeServers + " Edge servers");
        System.out.println("  - 1 Cloud datacenter");
        System.out.println("  - Simulation time: " + simulationTime + " seconds");
    }
    
    /**
     * Create IoT devices with parameters from configuration
     */
    private List<IoTDevice> createDevices(Properties config) {
        List<IoTDevice> deviceList = new ArrayList<>();
        
        int mips = Integer.parseInt(config.getProperty("mobile_device_mips", "2000"));
        int ram = 1024; // MB
        int storage = 16000; // MB
        int bandwidth = Integer.parseInt(config.getProperty("bandwidth_mobile_to_edge", "10")); // Mbps
        
        double idlePower = Double.parseDouble(config.getProperty("idle_power_mobile", "0.01"));
        double computingPower = Double.parseDouble(config.getProperty("computing_power_mobile", "0.9"));
        double transmissionPower = Double.parseDouble(config.getProperty("transmission_power_mobile", "1.3"));
        double receptionPower = Double.parseDouble(config.getProperty("reception_power_mobile", "1.0"));
        
        // Create devices with random positions in a 1000x1000 area
        for (int i = 0; i < numDevices; i++) {
            IoTDevice device = new IoTDevice(
                i, "MobileDevice_" + i, 
                mips, ram, storage, bandwidth,
                idlePower, computingPower, transmissionPower, receptionPower
            );
            
            // Set random position
            device.setXPos(random.nextDouble() * 1000);
            device.setYPos(random.nextDouble() * 1000);
            
            deviceList.add(device);
        }
        
        return deviceList;
    }
    
    /**
     * Create edge servers with parameters from configuration
     */
    private List<EdgeServer> createEdgeServers(Properties config) {
        List<EdgeServer> serverList = new ArrayList<>();
        
        int mips = Integer.parseInt(config.getProperty("edge_server_mips", "10000"));
        int ram = 8192; // MB
        int storage = 500000; // MB
        int bandwidth = Integer.parseInt(config.getProperty("bandwidth_mobile_to_edge", "10")) * 10; // Mbps
        int numPes = 4; // Number of processing elements
        
        double idlePower = Double.parseDouble(config.getProperty("idle_power_edge", "10.0"));
        double computingPower = Double.parseDouble(config.getProperty("computing_power_edge", "20.0"));
        double coverageRadius = 300.0; // meters
        
        // Create edge servers with evenly distributed positions
        for (int i = 0; i < numEdgeServers; i++) {
            EdgeServer server = new EdgeServer(
                i, "EdgeServer_" + i,
                mips, ram, storage, bandwidth, numPes,
                idlePower, computingPower, coverageRadius
            );
            
            // Position edge servers in a grid pattern
            double x = 250 + 500 * (i % 3);
            double y = 250 + 500 * (i / 3);
            server.setXPos(x);
            server.setYPos(y);
            
            serverList.add(server);
        }
        
        return serverList;
    }
    
    /**
     * Create cloud datacenter with parameters from configuration
     */
    private CloudDatacenter createCloudDatacenter(Properties config) {
        int numHosts = Integer.parseInt(config.getProperty("num_cloud_datacenters", "1"));
        int mipsPerHost = Integer.parseInt(config.getProperty("cloud_datacenter_mips", "50000"));
        int ramPerHost = 32768; // MB
        int storagePerHost = 1000000; // MB
        int bandwidthPerHost = Integer.parseInt(config.getProperty("bandwidth_edge_to_cloud", "100")) * 10; // Mbps
        int pesPerHost = 16; // Number of processing elements per host
        
        return new CloudDatacenter(
            0, "CloudDatacenter",
            numHosts, mipsPerHost, ramPerHost, storagePerHost,
            bandwidthPerHost, pesPerHost
        );
    }
    
    /**
     * Run the training phase of the simulation
     */
    public void runTrainingPhase(int trainingEpisodes) {
        System.out.println("\nStarting training phase...");
        this.isTrainingPhase = true;
        
        for (int episode = 0; episode < trainingEpisodes; episode++) {
            // Reset simulation state for the episode
            resetSimulation();
            
            // Run a full episode
            double episodeReward = runEpisode();
            
            // Train the DRL network
            drlAgent.trainNetwork();
            
            // Log progress
            if (episode % 10 == 0) {
                System.out.printf("Episode %d/%d - Reward: %.2f - Avg Reward: %.4f\n",
                    episode, trainingEpisodes, episodeReward, drlAgent.getAverageReward());
            }
        }
        
        System.out.println("Training completed!");
    }
    
    /**
     * Run the evaluation phase of the simulation
     */
    public void runEvaluationPhase() {
        System.out.println("\nStarting evaluation phase...");
        this.isTrainingPhase = false;
        
        // Reset metrics and simulation state
        metrics.reset();
        resetSimulation();
        
        // Run the simulation
        runEpisode();
        
        // Report results
        System.out.println("\nSimulation Results:");
        System.out.println("Total tasks processed: " + totalTasks);
        System.out.println("Average latency: " + metrics.getAverageLatency() + " ms");
        System.out.println("Average energy consumption: " + metrics.getAverageEnergy() + " J");
        System.out.println("Task success rate: " + (metrics.getSuccessRate() * 100) + "%");
        
        System.out.println("\nTask distribution:");
        System.out.println("  - Local execution: " + metrics.getLocalExecutionCount() + 
            " (" + (metrics.getLocalExecutionPercentage() * 100) + "%)");
        System.out.println("  - Edge execution: " + metrics.getEdgeExecutionCount() +
            " (" + (metrics.getEdgeExecutionPercentage() * 100) + "%)");
        System.out.println("  - Cloud execution: " + metrics.getCloudExecutionCount() +
            " (" + (metrics.getCloudExecutionPercentage() * 100) + "%)");
        
        System.out.println("\nEdge server utilization:");
        for (EdgeServer server : edgeServers) {
            System.out.println("  - " + server.getName() + ": " + 
                (server.getAverageUtilization() * 100) + "% average utilization, " +
                server.getTasksCompleted() + " tasks completed");
        }
    }
    
    /**
     * Reset the simulation state for a new episode
     */
    private void resetSimulation() {
        this.currentTime = 0.0;
        this.totalTasks = 0;
        
        // Reset device positions (optional for mobility simulation)
        for (IoTDevice device : devices) {
            device.setXPos(random.nextDouble() * 1000);
            device.setYPos(random.nextDouble() * 1000);
        }
        
        // Reset server loads
        for (EdgeServer server : edgeServers) {
            server.resetLoad();
            server.resetTasksCompleted();
        }
        
        // Reset cloud datacenter
        cloudDatacenter.reset();
    }
    
    /**
     * Run a single simulation episode using direct task processing
     * @return Total reward accumulated in the episode
     */
    private double runEpisode() {
        double totalReward = 0.0;
        
        try {
            // Create custom CloudSim instance
            OffloadingCloudSim simulation = new OffloadingCloudSim();
            
            // Create the broker using our custom implementation
            OffloadingDatacenterBroker broker = new OffloadingDatacenterBroker(
                simulation, drlAgent, environment, devices, edgeServers, cloudDatacenter, mobilityModel, isTrainingPhase);
                
            // Set custom simulation properties to make them available to the broker
            simulation.addProperty("task_arrival_rate", taskArrivalRate);
            simulation.addProperty("simulation_time", simulationTime);
            simulation.addProperty("training_phase", isTrainingPhase);
            simulation.addProperty("task_min_length", ConfigUtils.getProperty("task_min_length", "50"));
            simulation.addProperty("task_max_length", ConfigUtils.getProperty("task_max_length", "500"));
            simulation.addProperty("task_min_input_size", ConfigUtils.getProperty("task_min_input_size", "10"));
            simulation.addProperty("task_max_input_size", ConfigUtils.getProperty("task_max_input_size", "1000"));
            simulation.addProperty("task_min_output_size", ConfigUtils.getProperty("task_min_output_size", "1"));
            simulation.addProperty("task_max_output_size", ConfigUtils.getProperty("task_max_output_size", "100"));
            
            // Calculate expected number of tasks based on simulation time and arrival rate
            int expectedTasks = (int)(simulationTime * taskArrivalRate * devices.size());
            System.out.println("Expected tasks for this episode: " + expectedTasks);
            
            // Process tasks directly without CloudSim events
            int tasksProcessed = 0;
            double currentTime = 0.0;
            
            while (currentTime < simulationTime && tasksProcessed < expectedTasks) {
                // Generate tasks for each device
                for (IoTDevice device : devices) {
                    // Check if it's time to generate a task for this device
                    double nextTaskTime = getNextArrivalTime(taskArrivalRate);
                    if (currentTime >= nextTaskTime) {
                        // Generate a task for this device
                        IoTTask task = generateTask(device);
                        task.setStartTime(currentTime);
                        
                        // Process the task immediately
                        processTaskDirectly(broker, task, device);
                        
                        tasksProcessed++;
                        totalTasks++; // Increment the total tasks counter
                        totalReward += task.getReward();
                        
                        if (tasksProcessed >= expectedTasks) {
                            break;
                        }
                    }
                }
                
                // Advance simulation time
                currentTime += 0.1; // Small time step
            }
            
            System.out.println("Processed " + tasksProcessed + " tasks in episode");
            
            // Collect metrics and rewards after simulation completes
            for (IoTDevice device : devices) {
                // Collect per-device metrics
                if (!isTrainingPhase) {
                    for (IoTTask task : device.getCompletedTasks()) {
                        collectMetrics(task);
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error during simulation: " + e.getMessage());
            e.printStackTrace();
        }
        
        return totalReward;
    }
    
    /**
     * Process a task directly without CloudSim events
     */
    private void processTaskDirectly(OffloadingDatacenterBroker broker, IoTTask task, IoTDevice device) {
        try {
            // Get the current state from the environment
            double[] state = environment.getState(device, task);
            
            // Use DRL agent to choose action (offloading decision)
            int action = drlAgent.chooseAction(state);
            
            // Execute action by processing the task
            broker.processTaskDirectly(task, device, action);
            
            // If in training phase, store experience for learning
            if (isTrainingPhase) {
                double[] nextState = environment.getState(device, task);
                drlAgent.storeExperience(state, action, task.getReward(), nextState, false);
            }
            
        } catch (Exception e) {
            System.err.println("Error processing task: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Generate the next task arrival time based on Poisson distribution
     * @param rate Task arrival rate (lambda)
     * @return Time until next arrival
     */
    private double getNextArrivalTime(double rate) {
        // For Poisson process, inter-arrival times follow exponential distribution
        return -Math.log(1.0 - random.nextDouble()) / rate;
    }
    
    /**
     * Generate a task with random parameters
     */
    private IoTTask generateTask(IoTDevice device) {
        // Get task configuration parameters
        int taskId = totalTasks;
        long minTaskLength = Long.parseLong(ConfigUtils.getProperty("task_min_length", "50"));
        long maxTaskLength = Long.parseLong(ConfigUtils.getProperty("task_max_length", "500"));
        double minInputSize = Double.parseDouble(ConfigUtils.getProperty("task_min_input_size", "10"));
        double maxInputSize = Double.parseDouble(ConfigUtils.getProperty("task_max_input_size", "1000"));
        double minOutputSize = Double.parseDouble(ConfigUtils.getProperty("task_min_output_size", "1"));
        double maxOutputSize = Double.parseDouble(ConfigUtils.getProperty("task_max_output_size", "100"));
        
        // Generate random task parameters
        long taskLength = ThreadLocalRandom.current().nextLong(minTaskLength, maxTaskLength + 1);
        double inputSize = ThreadLocalRandom.current().nextDouble(minInputSize, maxInputSize);
        double outputSize = ThreadLocalRandom.current().nextDouble(minOutputSize, maxOutputSize);
        
        // Calculate a reasonable deadline based on the task length
        double maxExecutionTime = (taskLength / (double) device.getMips()) * 2.0; // 2x the local execution time
        
        // Create and return the task
        IoTTask task = device.generateTask(taskId, taskLength, inputSize, outputSize, maxExecutionTime);
        task.setStartTime(currentTime);
        
        return task;
    }
    
    /**
     * Collect performance metrics for the current task
     */
    private void collectMetrics(IoTTask task) {
        // Calculate latency
        double latency = task.getFinishTime() - task.getStartTime();
        
        // Determine execution location
        IoTTask.TaskLocation location = task.getExecutionLocation();
        
        // Update metrics
        metrics.recordTask(task, latency * 1000.0); // Convert to milliseconds
    }
    
    /**
     * Save the trained DRL model
     */
    public void saveModel(String filePath) {
        drlAgent.saveModel(filePath);
    }
    
    /**
     * Load a pre-trained DRL model
     */
    public void loadModel(String filePath) {
        drlAgent.loadModel(filePath);
    }
    
    /**
     * Get the collected performance metrics
     */
    public PerformanceMetrics getMetrics() {
        return metrics;
    }
    
    /**
     * Get the DRL agent used in this simulation
     */
    public DRLAgent getDRLAgent() {
        return drlAgent;
    }
    
    /**
     * Get the cloud datacenter used in this simulation
     */
    public CloudDatacenter getCloudDatacenter() {
        return cloudDatacenter;
    }
    
    /**
     * Get the list of IoT devices in this simulation
     */
    public List<IoTDevice> getIoTDevices() {
        return devices;
    }
    
    /**
     * Get the list of edge servers in this simulation
     */
    public List<EdgeServer> getEdgeServers() {
        return edgeServers;
    }
}