package org.edgecomputing.simulation;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;
import org.edgecomputing.drl.DRLAgent;
import org.edgecomputing.drl.OffloadingEnvironment;
import org.edgecomputing.model.CloudDatacenter;
import org.edgecomputing.model.EdgeServer;
import org.edgecomputing.model.IoTDevice;
import org.edgecomputing.model.IoTTask;
import org.edgecomputing.model.MobilityModel;
import org.edgecomputing.utils.ConfigUtils;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Custom broker that integrates DRL agent with CloudSim for task offloading decisions.
 * This broker handles task generation, scheduling, and execution using the DRL agent
 * to make intelligent offloading decisions.
 */
public class OffloadingDatacenterBroker extends org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple {
    
    // DRL components
    private final DRLAgent drlAgent;
    private final OffloadingEnvironment environment;
    
    // Simulation entities
    private final List<IoTDevice> devices;
    private final List<EdgeServer> edgeServers;
    private final CloudDatacenter cloudDatacenter;
    private final MobilityModel mobilityModel;
    
    // Task management
    private final Map<Integer, IoTTask> taskMap;
    private final Map<IoTDevice, Double> nextTaskTime;
    private final boolean isTrainingPhase;
    
    // Task generation parameters
    private final double taskArrivalRate;
    private final double simulationTime;
    
    /**
     * Constructor for the custom broker
     */
    public OffloadingDatacenterBroker(CloudSim simulation, DRLAgent drlAgent, 
                                     OffloadingEnvironment environment, List<IoTDevice> devices,
                                     List<EdgeServer> edgeServers, CloudDatacenter cloudDatacenter,
                                     MobilityModel mobilityModel, boolean isTrainingPhase) {
        super(simulation);
        
        this.drlAgent = drlAgent;
        this.environment = environment;
        this.devices = devices;
        this.edgeServers = edgeServers;
        this.cloudDatacenter = cloudDatacenter;
        this.mobilityModel = mobilityModel;
        this.isTrainingPhase = isTrainingPhase;
        
        // Initialize task management
        this.taskMap = new HashMap<>();
        this.nextTaskTime = new HashMap<>();
        
        // Get simulation parameters from config
        this.taskArrivalRate = Double.parseDouble(ConfigUtils.getProperty("task_arrival_rate", "2.0"));
        this.simulationTime = Double.parseDouble(ConfigUtils.getProperty("simulation_time", "300.0"));
        
        // Initialize next task times for each device
        for (IoTDevice device : devices) {
            nextTaskTime.put(device, getNextArrivalTime(taskArrivalRate));
        }
        
        System.out.println("[DEBUG-BROKER] Broker initialized with " + devices.size() + " devices");
    }
    
    /**
     * Schedule task generation for a specific device
     */
    public void scheduleTaskGeneration(IoTDevice device, double arrivalRate) {
        double nextTime = getNextArrivalTime(arrivalRate);
        nextTaskTime.put(device, nextTime);
        
        System.out.println("[DEBUG-BROKER] Scheduled task generation for device " + device.getId() + " at time " + nextTime);
    }
    
    /**
     * Process a task directly without CloudSim events
     * 
     * @param task The IoT task to process
     * @param device The IoT device that generated the task
     * @param action The offloading decision (0=local, 1...n=edge, n+1=cloud)
     */
    public void processTaskDirectly(IoTTask task, IoTDevice device, int action) {
        System.out.println("[DEBUG-BROKER] Processing task " + task.getId() + " directly with action " + action);
        
        // Store the task in the map
        taskMap.put(task.getId(), task);
        
        // Calculate execution time based on action
        double executionTime = 0.0;
        double energyConsumption = 0.0;
        
        switch (action) {
            case 0: // Local execution
                executionTime = task.getLength() / device.getMips();
                energyConsumption = executionTime * device.getComputingPower();
                task.setExecutionLocation(IoTTask.TaskLocation.LOCAL_DEVICE);
                break;
                
            case 1: // Edge server 1
            case 2: // Edge server 2
            case 3: // Edge server 3
                int edgeIndex = action - 1;
                if (edgeIndex < edgeServers.size()) {
                    EdgeServer edgeServer = edgeServers.get(edgeIndex);
                    executionTime = task.getLength() / edgeServer.getMips();
                    // Add network latency
                    double distance = calculateDistance(device, edgeServer);
                    double networkLatency = distance * 0.1; // 0.1 ms per unit distance
                    executionTime += networkLatency;
                    energyConsumption = task.getInputSize() * 0.001; // Transmission energy
                    task.setExecutionLocation(IoTTask.TaskLocation.EDGE_SERVER);
                } else {
                    // Fallback to local execution
                    executionTime = task.getLength() / device.getMips();
                    energyConsumption = executionTime * device.getComputingPower();
                    task.setExecutionLocation(IoTTask.TaskLocation.LOCAL_DEVICE);
                }
                break;
                
            default: // Cloud execution
                executionTime = task.getLength() / cloudDatacenter.getMipsCapacity();
                // Add network latency to cloud
                double cloudDistance = calculateDistanceToCloud(device);
                double cloudNetworkLatency = cloudDistance * 0.05; // 0.05 ms per unit distance
                executionTime += cloudNetworkLatency;
                energyConsumption = task.getInputSize() * 0.002; // Higher transmission energy for cloud
                task.setExecutionLocation(IoTTask.TaskLocation.CLOUD);
                break;
        }
        
        // Set task completion time and energy
        task.setFinishTime(task.getStartTime() + executionTime);
        task.setExecutionTime(executionTime);
        task.setTotalEnergy(energyConsumption);
        
        // Record the task execution in the device
        device.recordTaskExecution(task, energyConsumption);
        
        // Calculate and set the reward for this action
        double reward = environment.executeAction(device, task, action);
        task.setReward(reward);
        System.out.println("[DEBUG-BROKER] Set reward to " + reward + " for task " + task.getId());
        
        // Calculate latency for metrics
        double latency = task.getFinishTime() - task.getStartTime();
        System.out.println("[DEBUG-BROKER] Task " + task.getId() + " completed with latency " + latency + " ms");
    }
    
    /**
     * Calculate distance between device and edge server
     */
    private double calculateDistance(IoTDevice device, EdgeServer edgeServer) {
        double dx = device.getXPos() - edgeServer.getXPos();
        double dy = device.getYPos() - edgeServer.getYPos();
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Calculate distance to cloud (simplified)
     */
    private double calculateDistanceToCloud(IoTDevice device) {
        // Simplified cloud distance calculation
        return 100.0 + Math.random() * 50.0; // Base distance + random variation
    }
    
    /**
     * Get next task arrival time based on exponential distribution
     */
    private double getNextArrivalTime(double rate) {
        return -Math.log(1 - ThreadLocalRandom.current().nextDouble()) / rate;
    }
    
    /**
     * Get the map of tasks being managed.
     * 
     * @return The task map
     */
    public Map<Integer, IoTTask> getTaskMap() {
        return taskMap;
    }
    
    /**
     * Get the list of IoT devices
     */
    public List<IoTDevice> getDevices() {
        return devices;
    }
    
    /**
     * Get the list of edge servers
     */
    public List<EdgeServer> getEdgeServers() {
        return edgeServers;
    }
    
    /**
     * Get the cloud datacenter
     */
    public CloudDatacenter getCloudDatacenter() {
        return cloudDatacenter;
    }
    
    /**
     * Get the DRL agent
     */
    public DRLAgent getDRLAgent() {
        return drlAgent;
    }
    
    /**
     * Get the environment
     */
    public OffloadingEnvironment getEnvironment() {
        return environment;
    }
}