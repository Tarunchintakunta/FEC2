package org.edgecomputing.simulation;

import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.events.SimEvent;
import org.cloudbus.cloudsim.vms.Vm;
import org.edgecomputing.drl.DRLAgent;
import org.edgecomputing.drl.OffloadingEnvironment;
import org.edgecomputing.model.CloudDatacenter;
import org.edgecomputing.model.EdgeServer;
import org.edgecomputing.model.IoTDevice;
import org.edgecomputing.model.IoTTask;
import org.edgecomputing.model.MobilityModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Custom datacenter broker that uses DRL agent to make offloading decisions.
 * This broker extends CloudSim's DatacenterBrokerSimple to integrate with the 
 * CloudSim Plus event-driven simulation framework.
 */
public class OffloadingDatacenterBroker extends DatacenterBrokerSimple {

    private static final int OFFLOAD_DECISION_EVENT = 897;
    private static final int MOBILITY_UPDATE_EVENT = 898;
    private static final int TASK_GENERATION_EVENT = 899;
    private static final int TASK_COMPLETION_EVENT = 900;
    
    // Store reference to the CloudSim simulation
    private final CloudSim simulation;
    private final DRLAgent drlAgent;
    private final OffloadingEnvironment environment;
    private final List<IoTDevice> devices;
    private final List<EdgeServer> edgeServers;
    private final CloudDatacenter cloudDatacenter;
    private final MobilityModel mobilityModel;
    
    // Map to track IoT tasks and their Cloudlet counterparts
    private final Map<Integer, IoTTask> taskMap;
    
    // Simulation parameters
    private final boolean isTrainingPhase;
    
    /**
     * Creates a OffloadingDatacenterBroker with the given CloudSim instance.
     * 
     * @param simulation The CloudSim instance
     * @param drlAgent The DRL agent for making offloading decisions
     * @param environment The environment for the DRL agent
     * @param devices List of IoT devices
     * @param edgeServers List of edge servers
     * @param cloudDatacenter The cloud datacenter
     * @param isTrainingPhase Whether this is a training phase
     */
    public OffloadingDatacenterBroker(
            CloudSim simulation,
            DRLAgent drlAgent,
            OffloadingEnvironment environment,
            List<IoTDevice> devices,
            List<EdgeServer> edgeServers,
            CloudDatacenter cloudDatacenter,
            MobilityModel mobilityModel,
            boolean isTrainingPhase) {
        super(simulation);
        this.simulation = simulation;
        this.drlAgent = drlAgent;
        this.environment = environment;
        this.devices = devices;
        this.edgeServers = edgeServers;
        this.cloudDatacenter = cloudDatacenter;
        this.mobilityModel = mobilityModel;
        this.isTrainingPhase = isTrainingPhase;
        this.taskMap = new HashMap<>();
        
        // Schedule first mobility update
        if (mobilityModel != null && mobilityModel.getPattern() != MobilityModel.MobilityPattern.STATIC) {
            scheduleMobilityUpdate();
        }
    }
    
    /**
     * Schedule a task offloading decision for a specific IoT task.
     * 
     * @param task The IoT task to make offloading decision for
     * @param device The IoT device that generated the task
     * @param delay The delay before making the decision
     */
    /**
     * Get the broker's ID in the simulation
     * 
     * @return The broker's entity ID in the simulation
     */
    public long getId() {
        // Use the parent class's getId method
        return super.getId();
    }
    
    /**
     * Get the simulation instance
     * @return The CloudSim simulation instance
     */
    public CloudSim getSimulation() {
        return simulation;
    }
    
    public void scheduleOffloadingDecision(IoTTask task, IoTDevice device, double delay) {
        // Store the task in the map
        taskMap.put(task.getId(), task);
        
        // Create an object array to pass both task and device as data
        Object[] data = new Object[]{task, device};
        
        // Schedule the offloading decision event using CloudSim Plus 5.0.0 API
        schedule(delay, OFFLOAD_DECISION_EVENT, data);
    }
    
    /**
     * Process events other than default CloudSim Plus events.
     * 
     * @param ev The simulation event
     */
    protected void processOtherEvent(SimEvent ev) {
        System.out.println("[DEBUG] OffloadingDatacenterBroker processing event with tag: " + ev.getTag() + 
            " at time " + simulation.clock());
            
        switch (ev.getTag()) {
            case OFFLOAD_DECISION_EVENT:
                System.out.println("[DEBUG] Processing OFFLOAD_DECISION_EVENT");
                processOffloadDecisionEvent(ev);
                break;
            case MOBILITY_UPDATE_EVENT:
                System.out.println("[DEBUG] Processing MOBILITY_UPDATE_EVENT");
                processMobilityUpdate();
                break;
            case TASK_GENERATION_EVENT:
                System.out.println("[DEBUG] Processing TASK_GENERATION_EVENT");
                processTaskGenerationEvent(ev);
                break;
            case TASK_COMPLETION_EVENT:
                System.out.println("[DEBUG] Processing TASK_COMPLETION_EVENT");
                processTaskCompletion(ev);
                break;
            default:
                System.out.println("[DEBUG] Unknown event tag: " + ev.getTag() + ", ignoring");
                // Parent class might not have processOtherEvent so handle silently
        }
    }
    
    /**
     * Process an offloading decision event.
     * 
     * @param ev The simulation event
     */
    private void processOffloadDecisionEvent(SimEvent ev) {
        System.out.println("[DEBUG-BROKER] Processing offload decision event at time " + simulation.clock());
        
        Object[] data = (Object[]) ev.getData();
        IoTTask task = (IoTTask) data[0];
        IoTDevice device = (IoTDevice) data[1];
        
        System.out.println("[DEBUG-BROKER] Offloading decision for task " + task.getId() + 
            " from device " + device.getId());
        
        // Get the current state from the environment
        double[] state = environment.getState(device, task);
        System.out.println("[DEBUG-BROKER] Got environment state for task " + task.getId());
        
        // Use DRL agent to choose action (offloading decision)
        int action = drlAgent.chooseAction(state);
        System.out.println("[DEBUG-BROKER] DRL agent chose action " + action + 
            " for task " + task.getId() + 
            " (0=local, 1...n=edge, n+1=cloud)");
        
        // Execute action by submitting the task to the appropriate location
        convertTaskToCloudletAndSubmit(task, device, action);
        System.out.println("[DEBUG-BROKER] Converted task to cloudlet and submitted");
        
        // Set the execution location in the task based on the action
        String location = "unknown";
        if (action == 0) {
            task.setExecutionLocation(IoTTask.TaskLocation.LOCAL_DEVICE);
            location = "LOCAL_DEVICE";
        } else if (action <= edgeServers.size()) {
            task.setExecutionLocation(IoTTask.TaskLocation.EDGE_SERVER);
            location = "EDGE_SERVER";
        } else {
            task.setExecutionLocation(IoTTask.TaskLocation.CLOUD);
            location = "CLOUD";
        }
        System.out.println("[DEBUG-BROKER] Set execution location to " + location);
        
        // Calculate execution time and energy consumption
        double executionTime = calculateExecutionTime(device, task, action);
        double energy = calculateEnergyConsumption(device, task, action);
        
        System.out.println("[DEBUG-BROKER] Calculated execution time: " + executionTime + 
            ", energy: " + energy + " for task " + task.getId());
        
        // Update task with execution details
        task.setStartTime(simulation.clock());
        task.setFinishTime(task.getStartTime() + executionTime);
        task.setTotalEnergy(energy);
        task.setCompleted(true);
        
        System.out.println("[DEBUG-BROKER] Updated task with execution details: start=" + 
            task.getStartTime() + ", finish=" + task.getFinishTime() + ", completed=true");
        
        // Record task execution in the device
        device.recordTaskExecution(task, energy);
        System.out.println("[DEBUG-BROKER] Recorded task execution in device " + device.getId());
        
        // Calculate and set the reward for this action
        double reward = environment.executeAction(device, task, action);
        task.setReward(reward);
        System.out.println("[DEBUG-BROKER] Set reward to " + reward + " for task " + task.getId());
        
        // If in training phase, store experience for learning
        if (isTrainingPhase) {
            double[] nextState = environment.getState(device, task);
            drlAgent.storeExperience(state, action, reward, nextState, false);
            
            // Train the network periodically
            if (taskMap.size() % 10 == 0) {
                drlAgent.trainFromExperience();
                System.out.println("[DEBUG-BROKER] Training DRL agent from experience");
            }
        }
        
        // CloudSim Plus 5.0.0: get simulation time
        double currentTime = simulation.clock();
        // Schedule task completion event
        schedule(executionTime, TASK_COMPLETION_EVENT, task.getId());
        System.out.println("[DEBUG-BROKER] Scheduled task completion event for task " + 
            task.getId() + " at time " + (currentTime + executionTime));
    }
    
    /**
     * Process a task completion event.
     * 
     * @param ev The simulation event
     */
    private void processCloudletReturn(SimEvent ev) {
        int taskId = (int) ev.getData();
        IoTTask task = taskMap.remove(taskId);
        
        if (task != null) {
            // Record any final metrics or statistics here
            task.setCompleted(true);
            
            // Notify any listeners about task completion
            // This could be used for metrics collection
        }
    }
    
    /**
     * Convert an IoT task to a CloudSim Cloudlet and submit it based on the offloading decision.
     * 
     * @param task The IoT task
     * @param device The IoT device
     * @param action The offloading decision (0=local, 1...n=edge, n+1=cloud)
     */
    private void convertTaskToCloudletAndSubmit(IoTTask task, IoTDevice device, int action) {
        System.out.println("[DEBUG-BROKER] Converting task " + task.getId() + " to CloudSim Cloudlet");
        
        // Set the start time of the task to the current simulation time
        task.setStartTime(simulation.clock());
        System.out.println("[DEBUG-BROKER] Set task " + task.getId() + " start time to " + task.getStartTime());
        
        try {
            System.out.println("[DEBUG-BROKER] Task will be processed based on action=" + action + 
                " (0=local, 1...n=edge, n+1=cloud)");
                
            // Set the execution location based on the offloading decision
            if (action == 0) {
                // Local execution
                task.setExecutionLocation(IoTTask.TaskLocation.LOCAL_DEVICE);
                System.out.println("[DEBUG-BROKER] Task " + task.getId() + " set for LOCAL execution");
            } else if (action > 0 && action <= edgeServers.size()) {
                // Edge server execution
                task.setExecutionLocation(IoTTask.TaskLocation.EDGE_SERVER);
                task.setEdgeServerId(action - 1); // Action 1 corresponds to edge server 0
                System.out.println("[DEBUG-BROKER] Task " + task.getId() + " set for EDGE execution on server " + (action - 1));
            } else {
                // Cloud execution
                task.setExecutionLocation(IoTTask.TaskLocation.CLOUD);
                System.out.println("[DEBUG-BROKER] Task " + task.getId() + " set for CLOUD execution");
            }
        } catch (Exception e) {
            System.out.println("[ERROR-BROKER] Error in convertTaskToCloudletAndSubmit: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Calculate the execution time for a task based on the offloading decision.
     * 
     * @param device The source IoT device
     * @param task The task to execute
     * @param action The offloading action (0=local, 1...n=edge, n+1=cloud)
     * @return Execution time in seconds
     */
    private double calculateExecutionTime(IoTDevice device, IoTTask task, int action) {
        double processingTime;
        double transferTime = 0;
        double bandwidth;
        
        if (action == 0) {
            // Local execution
            processingTime = task.getTaskLength() / (double) device.getMips();
        } else if (action <= edgeServers.size()) {
            // Edge server execution
            EdgeServer server = edgeServers.get(action - 1);
            processingTime = task.getTaskLength() / (double) server.getMips();
            
            // Calculate transfer time based on distance and bandwidth
            double distance = calculateDistance(device, server);
            
            // Adjust bandwidth based on distance (further = weaker signal)
            double maxDistance = server.getCoverageRadius();
            double distanceRatio = Math.min(distance / maxDistance, 1.0);
            // Get bandwidth information
            int serverBandwidth = server.getBandwidth();
            double adjustedBandwidth = serverBandwidth * (1.0 - distanceRatio * 0.8); // 20% minimum bandwidth at edge
            
            transferTime = (task.getInputDataSize() + task.getOutputDataSize()) / adjustedBandwidth;
        } else {
            // Cloud execution
            processingTime = task.getTaskLength() / (double) cloudDatacenter.getMipsCapacity();
            bandwidth = cloudDatacenter.getBandwidth();
            transferTime = (task.getInputDataSize() + task.getOutputDataSize()) / bandwidth * 1.5; // Additional WAN latency factor
        }
        
        return processingTime + transferTime;
    }
    
    /**
     * Calculate the energy consumption for a task based on the offloading decision.
     * 
     * @param device The source IoT device
     * @param task The task to execute
     * @param action The offloading action (0=local, 1...n=edge, n+1=cloud)
     * @return Energy consumption in Joules
     */
    private double calculateEnergyConsumption(IoTDevice device, IoTTask task, int action) {
        if (action == 0) {
            // Local execution - full energy cost
            return device.getEnergyPerMI() * task.getTaskLength();
        } else {
            // Offloaded - only transmission energy
            double transmissionEnergy = device.getTransmissionEnergyPerKb() * task.getInputDataSize();
            double receptionEnergy = device.getReceptionEnergyPerKb() * task.getOutputDataSize();
            return transmissionEnergy + receptionEnergy;
        }
    }
    
    /**
     * Calculate the Euclidean distance between a device and an edge server.
     * 
     * @param device The IoT device
     * @param server The edge server
     * @return The distance between them
     */
    private double calculateDistance(IoTDevice device, EdgeServer server) {
        double dx = device.getXPos() - server.getXPos();
        double dy = device.getYPos() - server.getYPos();
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Generate a task with random parameters for the specified device.
     * 
     * @param device The IoT device to generate a task for
     * @return The generated task
     */
    private IoTTask generateTask(IoTDevice device) {
        // Get task configuration parameters from simulation
        int taskId = taskMap.size(); // Use size as unique ID
        
        // Get parameters from simulation properties
        OffloadingCloudSim sim = (OffloadingCloudSim)getSimulation();
        long minTaskLength = 50; // Default value if property is not found
        long maxTaskLength = 500;
        double minInputSize = 10;
        double maxInputSize = 1000;
        double minOutputSize = 1;
        double maxOutputSize = 100;
        
        // Try to get values from properties if available
        try {
            minTaskLength = sim.getIntProperty("task_min_length", 50);
            maxTaskLength = sim.getIntProperty("task_max_length", 500);
            minInputSize = sim.getDoubleProperty("task_min_input_size", 10.0);
            maxInputSize = sim.getDoubleProperty("task_max_input_size", 1000.0);
            minOutputSize = sim.getDoubleProperty("task_min_output_size", 1.0);
            maxOutputSize = sim.getDoubleProperty("task_max_output_size", 100.0);
        } catch (Exception e) {
            System.out.println("Warning: Using default task parameters due to configuration error.");
        }
        
        // Generate random task parameters
        long taskLength = minTaskLength + (long)(Math.random() * (maxTaskLength - minTaskLength));
        double inputSize = minInputSize + Math.random() * (maxInputSize - minInputSize);
        double outputSize = minOutputSize + Math.random() * (maxOutputSize - minOutputSize);
        
        // Calculate a reasonable deadline based on the task length
        double maxExecutionTime = (taskLength / (double) device.getMips()) * 2.0; // 2x the local execution time
        
        // Create and return the task
        IoTTask task = device.generateTask(taskId, taskLength, inputSize, outputSize, maxExecutionTime);
        task.setStartTime(getSimulation().clock());
        
        return task;
    }
    
    /**
     * Get the environment being used by this broker.
     * 
     * @return The environment
     */
    public OffloadingEnvironment getEnvironment() {
        return environment;
    }
    
    /**
     * Get the mobility model being used by this broker.
     * 
     * @return The mobility model
     */
    public MobilityModel getMobilityModel() {
        return mobilityModel;
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
     * Schedule mobility update events.
     */
    private void scheduleMobilityUpdate() {
        double updateInterval = mobilityModel.getUpdateInterval();
        // Schedule CloudSim events for our simulation components
        schedule(updateInterval, MOBILITY_UPDATE_EVENT, null);
    }
    
    /**
     * Process a mobility update event.
     */
    private void processMobilityUpdate() {
        // Update device positions based on mobility model
        mobilityModel.updateDevicePositions(devices, mobilityModel.getUpdateInterval());
        
        // Schedule next mobility update
        scheduleMobilityUpdate();
    }
    
    /**
     * Schedule task generation for a specific device.
     * 
     * @param device The IoT device to generate a task for
     * @param arrivalRate Task arrival rate in tasks per second
     */
    public void scheduleTaskGeneration(IoTDevice device, double arrivalRate) {
        // Calculate next task generation time (exponential distribution for Poisson process)
        double nextArrivalTime = -Math.log(1.0 - new Random().nextDouble()) / arrivalRate;
        System.out.println("[DEBUG-BROKER] Scheduling task generation for device " + 
            device.getId() + " with arrival rate " + arrivalRate + 
            " tasks/second, next arrival in " + nextArrivalTime + " seconds");
        schedule(nextArrivalTime, TASK_GENERATION_EVENT, device);
    }
    
    /**
     * Process a task generation event.
     * 
     * @param ev The simulation event
     */
    private void processTaskGenerationEvent(SimEvent ev) {
        System.out.println("[DEBUG-BROKER] Processing task generation event at time " + simulation.clock());
        
        // Extract the device from the event
        IoTDevice device = (IoTDevice) ev.getData();
        System.out.println("[DEBUG-BROKER] Generating task for device " + device.getId());
        
        // Get device properties
        double taskArrivalRate = device.getTaskArrivalRate();
        System.out.println("[DEBUG-BROKER] Device task arrival rate: " + taskArrivalRate + " tasks per second");
        
        try {
            // Generate a new task for this device
            IoTTask task = generateTask(device);
            System.out.println("[DEBUG-BROKER] Generated task " + task.getId() + 
                " with length " + task.getLength() + 
                ", input size " + task.getInputSize() + 
                ", output size " + task.getOutputSize());
            
            // Set the task start time to current simulation time
            task.setStartTime(simulation.clock());
            System.out.println("[DEBUG-BROKER] Task " + task.getId() + " start time set to " + task.getStartTime());
            
            // Schedule the offloading decision for this task
            // The decision is made immediately (0 delay)
            System.out.println("[DEBUG-BROKER] Scheduling offloading decision for task " + task.getId());
            scheduleOffloadingDecision(task, device, 0);
            
            // Schedule the next task generation for this device
            scheduleTaskGeneration(device, taskArrivalRate);
            
        } catch (Exception e) {
            System.out.println("[ERROR-BROKER] Error in processTaskGenerationEvent: " + e.getMessage());
            e.printStackTrace();
            
            // Even if there was an error, try to schedule the next task generation
            scheduleTaskGeneration(device, taskArrivalRate);
        }
    }
    
        /**
     * Process a task completion event.
     * 
     * @param ev The simulation event
     */
    private void processTaskCompletion(SimEvent ev) {
        int taskId = (int) ev.getData();
        System.out.println("[DEBUG-BROKER] Processing task completion event for task " + 
            taskId + " at time " + simulation.clock());
        
        // Get the task from the map
        IoTTask task = taskMap.remove(taskId);
        
        if (task != null) {
            System.out.println("[DEBUG-BROKER] Task " + taskId + " completion processing");
            
            // Set end time
            task.setEndTime(simulation.clock());
            
            // Calculate execution time
            double executionTime = task.getEndTime() - task.getStartTime();
            task.setExecutionTime(executionTime);
                
            // Calculate energy based on execution location
            double energy = 0;
            IoTDevice device = task.getDevice();
                
            if (device != null) {
                // Record the task completion in the device
                device.recordTaskExecution(task, task.getTotalEnergy());
                System.out.println("[DEBUG-BROKER] Recorded task " + task.getId() + 
                    " completion in device " + device.getId());
                
                // Store experience if in training phase
                if (isTrainingPhase) {
                    // Use the previously calculated execution time
                    
                    // Convert TaskLocation enum to integer action
                    int action;
                    switch (task.getExecutionLocation()) {
                        case LOCAL_DEVICE:
                            action = 0;
                            break;
                        case EDGE_SERVER:
                            action = 1; // This is approximate; ideally we'd track which edge server was used
                            break;
                        case CLOUD:
                            action = edgeServers.size() + 1;
                            break;
                        default:
                            action = 0;
                    }
                    
                    // Use the previously calculated energy
                    double reward = environment.calculateReward(executionTime, energy, task.getMaxExecutionTime());
                    
                    // Set the reward for the task
                    task.setReward(reward);
                    
                    // Get the state and action
                    double[] state = environment.getState(device, task);
                    
                    // Get next state (could be the state of the next task or a dummy final state)
                    double[] nextState = new double[state.length];
                    
                    // Store the experience in the agent's replay memory
                    drlAgent.storeExperience(state, action, reward, nextState, true);
                    
                    // Periodically train the agent
                    if (taskMap.size() % 10 == 0) {
                        drlAgent.trainFromExperience();
                    }
                }
            }
        }
    }
}
