package org.edgecomputing.drl;

import org.edgecomputing.model.IoTDevice;
import org.edgecomputing.model.IoTTask;
import org.edgecomputing.model.EdgeServer;
import org.edgecomputing.model.CloudDatacenter;

import java.util.List;

/**
 * Represents the environment for the DRL agent. This environment provides
 * the current state observation, processes actions, and calculates rewards.
 */
public class OffloadingEnvironment {
    /**
     * Constructor with simplified parameters for testing
     */
    public OffloadingEnvironment(List<IoTDevice> devices, List<EdgeServer> edgeServers, 
                               CloudDatacenter cloudDatacenter) {
        this(devices, edgeServers, cloudDatacenter, 
             100.0, 1000.0, 10000.0,  // Mobile, edge, cloud bandwidth (Mbps)
             10.0, 50.0,              // Mobile-edge, edge-cloud latency (ms)
             0.4, 0.4, 0.2);         // Weights: latency, energy, load balance
    }
    private List<IoTDevice> devices;
    private List<EdgeServer> edgeServers;
    private CloudDatacenter cloudDatacenter;
    
    // Network parameters
    private final double mobileBandwidth; // in Mbps
    private final double edgeBandwidth; // in Mbps
    private final double cloudBandwidth; // in Mbps
    private final double mobileToEdgeLatency; // in ms
    private final double edgeToCloudLatency; // in ms
    
    // Weights for reward calculation
    private final double latencyWeight;
    private final double energyWeight;
    private final double loadBalanceWeight;
    
    public OffloadingEnvironment(List<IoTDevice> devices, List<EdgeServer> edgeServers, 
                               CloudDatacenter cloudDatacenter,
                               double mobileBandwidth, double edgeBandwidth, double cloudBandwidth,
                               double mobileToEdgeLatency, double edgeToCloudLatency,
                               double latencyWeight, double energyWeight, double loadBalanceWeight) {
        this.devices = devices;
        this.edgeServers = edgeServers;
        this.cloudDatacenter = cloudDatacenter;
        this.mobileBandwidth = mobileBandwidth;
        this.edgeBandwidth = edgeBandwidth;
        this.cloudBandwidth = cloudBandwidth;
        this.mobileToEdgeLatency = mobileToEdgeLatency;
        this.edgeToCloudLatency = edgeToCloudLatency;
        this.latencyWeight = latencyWeight;
        this.energyWeight = energyWeight;
        this.loadBalanceWeight = loadBalanceWeight;
    }
    
    /**
     * Get the state representation for a specific IoT device and task
     * @param device The IoT device
     * @param task The task to be offloaded
     * @return State vector as double array
     */
    public double[] getState(IoTDevice device, IoTTask task) {
        // The state includes:
        // 1. Task attributes (length, input size, output size)
        // 2. Device attributes (MIPS, remaining energy)
        // 3. Edge servers attributes (load, distance)
        // 4. Cloud datacenter attributes
        
        int stateSize = 3 + 2 + (3 * edgeServers.size()) + 1;
        double[] state = new double[stateSize];
        int idx = 0;
        
        // 1. Task attributes
        state[idx++] = normalize(task.getTaskLength(), 0, 10000);  // Normalized task length
        state[idx++] = normalize(task.getInputDataSize(), 0, 1000);  // Normalized input data size
        state[idx++] = normalize(task.getOutputDataSize(), 0, 200);  // Normalized output data size
        
        // 2. Device attributes
        state[idx++] = normalize(device.getMips(), 0, 5000);  // Normalized device MIPS
        state[idx++] = normalize(1.0 - (device.getTotalEnergyConsumed() / 100.0), 0, 1);  // Normalized remaining energy
        
        // 3. Edge servers attributes
        for (EdgeServer server : edgeServers) {
            state[idx++] = server.getCurrentLoad();  // Load (already between 0 and 1)
            state[idx++] = normalize(server.calculateDistance(device), 0, 1000);  // Normalized distance
            state[idx++] = normalize(server.getMips() * server.getNumOfPes(), 0, 50000);  // Normalized processing capacity
        }
        
        // 4. Cloud datacenter attribute
        state[idx++] = normalize(cloudDatacenter.getAverageUtilization(), 0, 1);  // Cloud utilization
        
        return state;
    }
    
    /**
     * Execute an offloading action and calculate the reward
     * @param device The IoT device
     * @param task The task to be offloaded
     * @param action The action index (0 = local, 1...n = edge servers, n+1 = cloud)
     * @return Reward value
     */
    public double executeAction(IoTDevice device, IoTTask task, int action) {
        double latency = 0.0;
        double energy = 0.0;
        double loadBalance = 0.0;
        boolean validAction = true;
        
        if (action == 0) {
            // Execute locally
            latency = (double) task.getTaskLength() / device.getMips();
            energy = device.calculateLocalExecutionEnergy(task);
            task.setExecutionLocation(IoTTask.TaskLocation.LOCAL_DEVICE);
            device.recordTaskExecution(task, energy);
            
        } else if (action <= edgeServers.size()) {
            // Execute on edge server
            EdgeServer server = edgeServers.get(action - 1);
            
            // Check if device is in coverage of the edge server
            if (!server.isDeviceInCoverage(device)) {
                validAction = false;
                latency = task.getDeadline() * 2; // Penalty for invalid action
                energy = 0.0;
            } else {
                // Calculate transmission latency
                double transmissionLatency = (task.getInputDataSize() * 8) / (mobileBandwidth * 1000);
                double receptionLatency = (task.getOutputDataSize() * 8) / (mobileBandwidth * 1000);
                
                // Calculate processing latency
                double processingLatency = server.calculateTaskExecutionTime(task);
                
                // Total latency including network delay
                latency = transmissionLatency + processingLatency + receptionLatency + (2 * mobileToEdgeLatency / 1000.0);
                
                // Calculate energy consumption
                energy = device.calculateOffloadingEnergy(task, server.getBandwidth());
                
                // Update task and device info
                task.setExecutionLocation(IoTTask.TaskLocation.EDGE_SERVER);
                device.recordTaskExecution(task, energy);
                
                // Update edge server info
                server.receiveTask();
                server.completeTask(task, processingLatency, server.calculateExecutionEnergy(task));
                
                // Calculate load balance factor (standard deviation of loads across edge servers)
                loadBalance = calculateLoadBalanceFactor();
            }
            
        } else {
            // Execute on cloud
            // Calculate transmission latency (device to edge to cloud)
            double upTransmissionLatency = (task.getInputDataSize() * 8) / (mobileBandwidth * 1000);
            double upBackboneLatency = (task.getInputDataSize() * 8) / (edgeBandwidth * 1000);
            
            // Calculate processing latency
            double processingLatency = cloudDatacenter.calculateTaskExecutionTime(task);
            
            // Calculate reception latency (cloud to edge to device)
            double downBackboneLatency = (task.getOutputDataSize() * 8) / (edgeBandwidth * 1000);
            double downTransmissionLatency = (task.getOutputDataSize() * 8) / (mobileBandwidth * 1000);
            
            // Total latency including network delays
            latency = upTransmissionLatency + upBackboneLatency + processingLatency +
                     downBackboneLatency + downTransmissionLatency +
                     (2 * mobileToEdgeLatency / 1000.0) + (2 * edgeToCloudLatency / 1000.0);
            
            // Calculate energy consumption
            energy = device.calculateOffloadingEnergy(task, (int)mobileBandwidth);
            
            // Update task and device info
            task.setExecutionLocation(IoTTask.TaskLocation.CLOUD);
            device.recordTaskExecution(task, energy);
            
            // Update cloud info
            cloudDatacenter.receiveTask();
            cloudDatacenter.completeTask(task, processingLatency);
        }
        
        // Update task timing information
        task.setFinishTime(task.getStartTime() + latency);
        
        // Calculate reward based on deadline compliance and performance
        double reward = 0.0;
        
        if (validAction) {
            // Check if task meets deadline
            boolean meetsDeadline = latency <= task.getDeadline();
            
            // Base reward for successful completion
            if (meetsDeadline) {
                reward += 10.0; // Bonus for meeting deadline
                
                // Additional reward based on performance
                double normalizedLatency = Math.min(1.0, latency / task.getDeadline());
                double normalizedEnergy = Math.min(1.0, energy / 5.0);
                
                // Reward decreases with higher latency and energy
                reward += (1.0 - normalizedLatency) * 5.0; // Up to 5 points for low latency
                reward += (1.0 - normalizedEnergy) * 3.0;  // Up to 3 points for low energy
                
                // Load balance reward
                reward += (1.0 - loadBalance) * 2.0; // Up to 2 points for good load balance
                
            } else {
                // Penalty for missing deadline
                reward -= 5.0;
                
                // Additional penalty based on how much deadline was missed
                double deadlineMissRatio = (latency - task.getDeadline()) / task.getDeadline();
                reward -= deadlineMissRatio * 10.0;
            }
        } else {
            // Large penalty for invalid action
            reward -= 20.0;
        }
        
        return reward;
    }
    
    /**
     * Calculate the load balance factor across edge servers
     * @return Load balance factor [0,1], 0 = perfectly balanced
     */
    private double calculateLoadBalanceFactor() {
        if (edgeServers.size() <= 1) {
            return 0.0;
        }
        
        // Calculate average load
        double totalLoad = 0.0;
        for (EdgeServer server : edgeServers) {
            totalLoad += server.getCurrentLoad();
        }
        double avgLoad = totalLoad / edgeServers.size();
        
        // Calculate standard deviation
        double sumSquaredDiff = 0.0;
        for (EdgeServer server : edgeServers) {
            double diff = server.getCurrentLoad() - avgLoad;
            sumSquaredDiff += diff * diff;
        }
        double stdDev = Math.sqrt(sumSquaredDiff / edgeServers.size());
        
        // Return normalized standard deviation
        return Math.min(1.0, stdDev);
    }
    
    /**
     * Simple normalization function to scale values to [0,1]
     */
    private double normalize(double value, double min, double max) {
        return Math.min(1.0, Math.max(0.0, (value - min) / (max - min)));
    }
    
    // Getters
    public List<IoTDevice> getDevices() {
        return devices;
    }
    
    public List<EdgeServer> getEdgeServers() {
        return edgeServers;
    }
    
    public CloudDatacenter getCloudDatacenter() {
        return cloudDatacenter;
    }
    
    public int getActionSpace() {
        // Actions: local execution, each edge server, cloud
        return 1 + edgeServers.size() + 1;
    }
    
    public int getStateSpace() {
        // State space size
        return 3 + 2 + (3 * edgeServers.size()) + 1;
    }
    
    /**
     * Calculate reward based on execution time, energy consumption and deadline
     * 
     * @param executionTime The total execution time of the task
     * @param energy The energy consumed during task execution
     * @param deadline The deadline for task completion
     * @return The calculated reward value
     */
    public double calculateReward(double executionTime, double energy, double deadline) {
        // Normalize metrics
        double normalizedTime = Math.min(1.0, executionTime / 10.0); // Assume max execution time is 10s
        double normalizedEnergy = Math.min(1.0, energy / 5.0);       // Assume max energy is 5J
        
        // Calculate load balance factor
        double loadBalance = calculateLoadBalanceFactor();
        
        // Calculate reward (negative since we want to minimize these values)
        double reward = -(latencyWeight * normalizedTime + 
                        energyWeight * normalizedEnergy + 
                        loadBalanceWeight * loadBalance);
        
        // Add bonus if completed within deadline
        if (executionTime <= deadline) {
            reward += 1.0;
        } else {
            reward -= 0.5;
        }
        
        return reward;
    }
}
