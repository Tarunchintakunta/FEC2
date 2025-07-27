package org.edgecomputing.model;

import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents an Edge Server that can execute offloaded tasks from IoT devices.
 * The edge server has greater computing capability than IoT devices but less
 * than cloud data centers.
 */
public class EdgeServer {
    private final int id;
    private final String name;
    private final int mips; // Processing capacity in Million Instructions Per Second
    private final int ram; // RAM in MB
    private final int storage; // Storage in MB
    private int bandwidth; // Network bandwidth in Mbps
    private final int numOfPes; // Number of processing elements
    private final Host host; // CloudSim representation
    
    // Energy model parameters
    private final double idlePower; // in Watts
    private final double computingPower; // in Watts
    
    // Server coordinates for proximity calculations
    private double xPos;
    private double yPos;
    
    // Coverage radius in meters
    private double coverageRadius;
    
    // Stats
    private int tasksReceived;
    private int tasksCompleted;
    private int tasksFailed;
    private double totalEnergyConsumed;
    private double totalProcessingTime;
    private double averageUtilization;
    
    // Current load (0.0 to 1.0)
    private double currentLoad;
    
    public EdgeServer(int id, String name, int mips, int ram, int storage, int bandwidth, int numOfPes,
                     double idlePower, double computingPower, double coverageRadius) {
        this.id = id;
        this.name = name;
        this.mips = mips;
        this.ram = ram;
        this.storage = storage;
        this.bandwidth = bandwidth;
        this.numOfPes = numOfPes;
        this.idlePower = idlePower;
        this.computingPower = computingPower;
        this.coverageRadius = coverageRadius;
        this.xPos = 0.0;
        this.yPos = 0.0;
        
        this.tasksReceived = 0;
        this.tasksCompleted = 0;
        this.tasksFailed = 0;
        this.totalEnergyConsumed = 0.0;
        this.totalProcessingTime = 0.0;
        this.averageUtilization = 0.0;
        this.currentLoad = 0.0;
        
        // Create CloudSim host representation
        this.host = createHost();
    }
    
    private Host createHost() {
        // Create PEs (Processing Elements)
        List<Pe> peList = new ArrayList<>();
        for (int i = 0; i < numOfPes; i++) {
            peList.add(new PeSimple(mips));
        }
        
        // Create a host using CloudSim Plus 5.0.0 API
        return new HostSimple(ram, bandwidth, storage, peList);
    }
    
    // Method to calculate task execution time on this edge server
    public double calculateTaskExecutionTime(IoTTask task) {
        // Basic calculation: task length / (MIPS * number of PEs * (1 - current load))
        // This accounts for server's current load and multiple PEs
        double effectiveMips = mips * numOfPes * (1.0 - currentLoad);
        return (double) task.getTaskLength() / effectiveMips;
    }
    
    // Method to calculate energy consumption for executing a task
    public double calculateExecutionEnergy(IoTTask task) {
        double executionTime = calculateTaskExecutionTime(task);
        
        // Energy = Power * Time
        // We use a linear model where power consumption is proportional to load
        double loadIncrease = task.getTaskLength() / (double)(mips * numOfPes);
        double averagePower = idlePower + (computingPower - idlePower) * (currentLoad + loadIncrease/2);
        
        return averagePower * executionTime;
    }
    
    // Method to check if an IoT device is within coverage
    public boolean isDeviceInCoverage(IoTDevice device) {
        double distance = calculateDistance(device);
        return distance <= coverageRadius;
    }
    
    // Calculate distance to an IoT device
    public double calculateDistance(IoTDevice device) {
        double dx = xPos - device.getXPos();
        double dy = yPos - device.getYPos();
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    // Record a task being received
    public void receiveTask() {
        tasksReceived++;
    }
    
    // Record task completion
    public void completeTask(IoTTask task, double executionTime, double energy) {
        tasksCompleted++;
        totalProcessingTime += executionTime;
        totalEnergyConsumed += energy;
        
        // Update average utilization (simple moving average)
        double taskUtilization = task.getTaskLength() / (double)(mips * numOfPes * executionTime);
        averageUtilization = ((averageUtilization * (tasksCompleted - 1)) + taskUtilization) / tasksCompleted;
    }
    
    // Record task failure
    public void failTask() {
        tasksFailed++;
    }
    
    // Update server load
    public void updateLoad(double newLoad) {
        this.currentLoad = Math.min(1.0, Math.max(0.0, newLoad)); // Keep load between 0 and 1
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
    
    /**
     * Set the network bandwidth
     * @param bandwidth Bandwidth in Mbps
     */
    public void setBandwidth(int bandwidth) {
        this.bandwidth = bandwidth;
    }
    
    public int getNumOfPes() {
        return numOfPes;
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
     * Set both X and Y positions at once
     * @param x X position in meters
     * @param y Y position in meters
     */
    public void setPosition(double x, double y) {
        this.xPos = x;
        this.yPos = y;
    }
    
    public double getCoverageRadius() {
        return coverageRadius;
    }
    
    public int getTasksCompleted() {
        return tasksCompleted;
    }
    
    /**
     * Reset the current load on the server
     */
    public void resetLoad() {
        this.currentLoad = 0;
    }
    
    /**
     * Reset the tasks completed counter
     */
    public void resetTasksCompleted() {
        this.tasksCompleted = 0;
    }
    
    public int getTasksFailed() {
        return tasksFailed;
    }
    
    public double getTotalEnergyConsumed() {
        return totalEnergyConsumed;
    }
    
    public double getAverageProcessingTime() {
        return tasksCompleted > 0 ? totalProcessingTime / tasksCompleted : 0.0;
    }
    
    public double getAverageUtilization() {
        return averageUtilization;
    }
    
    public double getCurrentLoad() {
        return currentLoad;
    }
    
    /**
     * Get the total computational capacity of this edge server
     * @return Total MIPS capacity (MIPS * number of PEs)
     */
    public int getCapacity() {
        return mips * numOfPes;
    }
    
    @Override
    public String toString() {
        return "EdgeServer{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", mips=" + mips +
                ", numOfPes=" + numOfPes +
                ", position=(" + xPos + "," + yPos + ")" +
                ", currentLoad=" + String.format("%.2f", currentLoad) +
                '}';
    }
}
