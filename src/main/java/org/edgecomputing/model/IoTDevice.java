package org.edgecomputing.model;

import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents an IoT device capable of executing tasks locally
 * or offloading them to edge servers or cloud.
 */
public class IoTDevice {
    private final int id;
    private final String name;
    private final int mips; // Processing capacity in Million Instructions Per Second
    private final int ram; // RAM in MB
    private final int storage; // Storage in MB
    private final int bandwidth; // Network bandwidth in Mbps
    private final Host host; // CloudSim representation
    
    // Energy model parameters
    private final double idlePower; // in Watts
    private final double computingPower; // in Watts
    private final double transmissionPower; // in Watts
    private final double receptionPower; // in Watts
    
    // Device coordinates and mobility parameters
    private double xPos; // X position in meters
    private double yPos; // Y position in meters
    private double speed;          // Current speed in m/s
    private double direction;      // Current direction in radians
    private double targetX;        // Target X position for waypoint mobility
    private double targetY;        // Target Y position for waypoint mobility
    
    // Task generation parameters
    private double taskArrivalRate = 1.0; // Default tasks per second
    
    // List of completed tasks for metrics collection
    private final List<IoTTask> completedTasks;
    
    // Stats
    private int tasksGenerated;
    private int tasksExecutedLocally;
    private int tasksOffloadedToEdge;
    private int tasksOffloadedToCloud;
    private double totalEnergyConsumed;
    
    public IoTDevice(int id, String name, int mips, int ram, int storage, int bandwidth,
                    double idlePower, double computingPower, double transmissionPower, double receptionPower) {
        this.id = id;
        this.name = name;
        this.mips = mips;
        this.ram = ram;
        this.storage = storage;
        this.bandwidth = bandwidth;
        this.idlePower = idlePower;
        this.computingPower = computingPower;
        this.transmissionPower = transmissionPower;
        this.receptionPower = receptionPower;
        this.xPos = 0.0;
        this.yPos = 0.0;
        this.speed = 0.0;
        this.direction = 0.0;
        this.targetX = 0.0;
        this.targetY = 0.0;
        
        this.tasksGenerated = 0;
        this.tasksExecutedLocally = 0;
        this.tasksOffloadedToEdge = 0;
        this.tasksOffloadedToCloud = 0;
        this.totalEnergyConsumed = 0.0;
        this.completedTasks = new ArrayList<>();
        
        // Create CloudSim host representation
        this.host = createHost();
    }
    
    private Host createHost() {
        // Create PEs (Processing Elements)
        List<Pe> peList = new ArrayList<>();
        peList.add(new PeSimple(mips));
        
        // Create a simple host using CloudSim Plus 5.0.0 API
        return new HostSimple(ram, bandwidth, storage, peList);
    }
    
    // Method to generate a new task
    public IoTTask generateTask(int taskId, long taskLength, double inputSize, double outputSize, double maxExecutionTime) {
        this.tasksGenerated++;
        return new IoTTask(taskId, this, taskLength, inputSize, outputSize, maxExecutionTime);
    }
    
    // Method to calculate energy consumption for local execution
    public double calculateLocalExecutionEnergy(IoTTask task) {
        // Time (in seconds) to execute the task locally
        double executionTime = (double) task.getTaskLength() / mips;
        
        // Energy consumed = Power * Time
        return computingPower * executionTime;
    }
    
    /**
     * Calculate energy for local execution of a task
     * @param task The task to calculate local execution energy for
     * @return Energy consumption in Joules for local execution
     */
    public double calculateLocalEnergy(IoTTask task) {
        return calculateLocalExecutionEnergy(task);
    }
    
    // Method to calculate energy consumption for offloading
    public double calculateOffloadingEnergy(IoTTask task, int targetBandwidth) {
        // Time to transmit input data
        double transmissionTime = (task.getInputDataSize() * 8) / (targetBandwidth * 1000); // Convert KB to bits and Mbps to bps
        
        // Time to receive output data
        double receptionTime = (task.getOutputDataSize() * 8) / (targetBandwidth * 1000);
        
        // Energy = Transmission power * transmission time + Reception power * reception time
        return (transmissionPower * transmissionTime) + (receptionPower * receptionTime);
    }
    
    // Record task execution
    public void recordTaskExecution(IoTTask task, double energy) {
        switch (task.getExecutionLocation()) {
            case LOCAL_DEVICE:
                tasksExecutedLocally++;
                break;
            case EDGE_SERVER:
                tasksOffloadedToEdge++;
                break;
            case CLOUD:
                tasksOffloadedToCloud++;
                break;
        }
        
        totalEnergyConsumed += energy;
        
        // Add to completed tasks for metrics collection
        task.setCompleted(true);
        completedTasks.add(task);
    }
    
    // Getters and setters
    public int getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public int getMips() {
        return mips;
    }
    
    public int getRam() {
        return ram;
    }
    
    public int getStorage() {
        return storage;
    }
    
    public int getBandwidth() {
        return bandwidth;
    }
    
    public Host getHost() {
        return host;
    }
    
    public double getIdlePower() {
        return idlePower;
    }
    
    public double getComputingPower() {
        return computingPower;
    }
    
    public double getTransmissionPower() {
        return transmissionPower;
    }
    
    public double getReceptionPower() {
        return receptionPower;
    }
    
    public double getXPos() {
        return xPos;
    }
    
    public void setXPos(double xPos) {
        this.xPos = xPos;
    }
    
    public double getYPos() {
        return yPos;
    }
    
    public void setYPos(double yPos) {
        this.yPos = yPos;
    }
    
    /**
     * Get the task arrival rate (tasks per second)
     * @return Task arrival rate
     */
    public double getTaskArrivalRate() {
        return taskArrivalRate;
    }

    /**
     * Set the task arrival rate (tasks per second)
     * @param taskArrivalRate Tasks per second
     */
    public void setTaskArrivalRate(double taskArrivalRate) {
        this.taskArrivalRate = taskArrivalRate;
    }
    
    public double distanceTo(EdgeServer server) {
        double dx = xPos - server.getXPos();
        double dy = yPos - server.getYPos();
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    public int getTasksGenerated() {
        return tasksGenerated;
    }
    
    public int getTasksExecutedLocally() {
        return tasksExecutedLocally;
    }
    
    public int getTasksOffloadedToEdge() {
        return tasksOffloadedToEdge;
    }
    
    public int getTasksOffloadedToCloud() {
        return tasksOffloadedToCloud;
    }
    
    public double getTotalEnergyConsumed() {
        return totalEnergyConsumed;
    }
    
    /**
     * Set both X and Y position at once
     */
    public void setPosition(double x, double y) {
        this.xPos = x;
        this.yPos = y;
    }
    
    /**
     * Set target position for waypoint mobility
     */
    public void setTargetPosition(double x, double y) {
        this.targetX = x;
        this.targetY = y;
    }
    
    /**
     * Get target X position
     */
    public double getTargetX() {
        return targetX;
    }
    
    /**
     * Get target Y position
     */
    public double getTargetY() {
        return targetY;
    }
    
    /**
     * Set speed in m/s
     */
    public void setSpeed(double speed) {
        this.speed = speed;
    }
    
    /**
     * Get current speed in m/s
     */
    public double getSpeed() {
        return speed;
    }
    
    /**
     * Set movement direction in radians
     */
    public void setDirection(double direction) {
        this.direction = direction;
    }
    
    /**
     * Get movement direction in radians
     */
    public double getDirection() {
        return direction;
    }
    
    /**
     * Get list of completed tasks
     */
    public List<IoTTask> getCompletedTasks() {
        return completedTasks;
    }
    
    /**
     * Get IDs of all tasks generated by this device
     * 
     * @return List of task IDs
     */
    public List<Integer> getTaskIds() {
        List<Integer> taskIds = new ArrayList<>();
        for (IoTTask task : completedTasks) {
            taskIds.add(task.getId());
        }
        return taskIds;
    }
    
    /**
     * Get total reward from all tasks
     */
    public double getTotalReward() {
        double totalReward = 0.0;
        for (IoTTask task : completedTasks) {
            totalReward += task.getReward();
        }
        return totalReward;
    }
    
    /**
     * Calculate the energy consumption per million instructions (MI)
     * @return Energy consumption in Joules per MI
     */
    public double getEnergyPerMI() {
        // Convert computingPower (Watts) to Joules per second, then per MI
        return computingPower / mips;
    }
    
    /**
     * Calculate transmission energy consumption per Kb
     * @return Transmission energy consumption in Joules per Kb
     */
    public double getTransmissionEnergyPerKb() {
        // Convert transmissionPower (Watts) to Joules per second, then calculate per Kb
        return transmissionPower / (bandwidth * 0.125); // bandwidth in Mbps to KB/s
    }
    
    /**
     * Calculate reception energy consumption per Kb
     * @return Reception energy consumption in Joules per Kb
     */
    public double getReceptionEnergyPerKb() {
        // Convert receptionPower (Watts) to Joules per second, then calculate per Kb
        return receptionPower / (bandwidth * 0.125); // bandwidth in Mbps to KB/s
    }
    
    /**
     * Calculate the execution time of a task on this device
     * @param task Task to calculate execution time for
     * @return Execution time in seconds
     */
    public double calculateTaskExecutionTime(IoTTask task) {
        return (double) task.getTaskLength() / mips;
    }
    
    /**
     * Calculate energy consumption for transmitting data
     * 
     * @param dataSize Data size in bytes
     * @param distance Distance in meters
     * @return Energy consumption in Joules
     */
    public double calculateTransmissionEnergy(double dataSize, double distance) {
        // Simple model: Energy = dataSize * distance factor * base transmission power
        // Actual implementation would be more complex based on wireless models
        return dataSize * 0.000001 * distance * transmissionPower;
    }
    
    /**
     * Calculate energy consumption for receiving data
     * 
     * @param dataSize Data size in bytes
     * @return Energy consumption in Joules
     */
    public double calculateReceptionEnergy(double dataSize) {
        // Simple model: Energy = dataSize * base reception power
        return dataSize * 0.0000005 * receptionPower;
    }
    
    /**
     * Set energy parameters for this device
     * 
     * @param idlePower Idle power consumption in Watts
     * @param computingPower Computing power consumption in Watts
     * @param transmissionPower Transmission power consumption in Watts
     * @param receptionPower Reception power consumption in Watts
     */
    public void setEnergyParameters(double idlePower, double computingPower, 
                                 double transmissionPower, double receptionPower) {
        // Since these are final fields, we can't actually set them, but in a real
        // implementation we would update the energy parameters here
        // This is only needed for backwards compatibility with existing code
    }
    
    /**
     * Calculate energy consumption for a task execution
     * This is a convenience method for backward compatibility
     * 
     * @param task The task to be executed
     * @return Energy consumption in Joules
     */
    public double calculateEnergyConsumption(IoTTask task) {
        return calculateLocalExecutionEnergy(task);
    }
    
    @Override
    public String toString() {
        return "IoTDevice{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", mips=" + mips +
                ", position=(" + xPos + "," + yPos + ")" +
                ", speed=" + speed +
                ", tasks=" + tasksGenerated +
                ", energy=" + String.format("%.2f", totalEnergyConsumed) + "J" +
                '}';
    }
}
