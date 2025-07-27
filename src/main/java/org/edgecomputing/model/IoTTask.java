package org.edgecomputing.model;

import java.util.UUID;

// CloudSim Plus class imports
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic;

/**
 * Represents an IoT task that can be executed locally on an IoT device,
 * on an edge server, or in the cloud.
 */
public class IoTTask {
    private final int id;
    private final long taskLength; // in Million Instructions (MI)
    private final double inputDataSize; // in KB
    private final double outputDataSize; // in KB
    private final double maxExecutionTime; // in seconds, deadline
    private Cloudlet cloudlet; // CloudSim representation
    private final IoTDevice sourceDevice; // Device that generated this task
    
    private TaskLocation executionLocation;
    private double startTime;
    private double finishTime;
    private double totalEnergy; // in Joules
    private double reward; // Reward from DRL agent
    private boolean completed; // Whether the task has been completed
    private int edgeServerId = -1; // ID of the edge server processing this task (if applicable)
    
    public enum TaskLocation {
        LOCAL_DEVICE, 
        EDGE_SERVER, 
        CLOUD
    }
    
    public IoTTask(int id, IoTDevice sourceDevice, long taskLength, double inputDataSize, 
                  double outputDataSize, double maxExecutionTime) {
        this.id = id;
        this.sourceDevice = sourceDevice;
        this.taskLength = taskLength;
        this.inputDataSize = inputDataSize;
        this.outputDataSize = outputDataSize;
        this.maxExecutionTime = maxExecutionTime;
        this.executionLocation = TaskLocation.LOCAL_DEVICE; // Default location
        this.reward = 0.0;
        this.completed = false;
        
        // Stub implementation for CloudSim Plus integration
        this.cloudlet = null;
    }
    
    private Cloudlet createCloudlet() {
        // Create a utilization model for the cloudlet
        UtilizationModel utilizationModel = new UtilizationModelFull();
        
        // Create a CloudSim Plus 5.0.0 cloudlet
        CloudletSimple cloudlet = new CloudletSimple(taskLength, 1);
        cloudlet.setFileSize((long)(inputDataSize * 1024)); // Convert KB to bytes
        cloudlet.setOutputSize((long)(outputDataSize * 1024)); // Convert KB to bytes
        cloudlet.setUtilizationModelCpu(utilizationModel);
        cloudlet.setUtilizationModelRam(utilizationModel);
        cloudlet.setUtilizationModelBw(utilizationModel);
        
        return cloudlet;
    }
    
    // Getters and setters
    public int getId() {
        return id;
    }
    
    public long getTaskLength() {
        return taskLength;
    }
    
    /**
     * Wrapper for getTaskLength() for compatibility
     */
    public long getLength() {
        return getTaskLength();
    }
    
    public double getInputDataSize() {
        return inputDataSize;
    }
    
    /**
     * Wrapper for getInputDataSize() for compatibility
     */
    public double getInputSize() {
        return getInputDataSize();
    }
    
    public double getOutputDataSize() {
        return outputDataSize;
    }
    
    /**
     * Wrapper for getOutputDataSize() for compatibility
     */
    public double getOutputSize() {
        return getOutputDataSize();
    }
    
    public double getMaxExecutionTime() {
        return maxExecutionTime;
    }
    
    /**
     * Gets the task deadline, which is the same as the max execution time.
     * 
     * @return The deadline in seconds
     */
    public double getDeadline() {
        return maxExecutionTime;
    }
    
    public Cloudlet getCloudlet() {
        return cloudlet;
    }
    
    public IoTDevice getSourceDevice() {
        return sourceDevice;
    }
    
    public TaskLocation getExecutionLocation() {
        return executionLocation;
    }
    
    public void setExecutionLocation(TaskLocation executionLocation) {
        this.executionLocation = executionLocation;
    }
    
    public double getStartTime() {
        return startTime;
    }
    
    public void setStartTime(double startTime) {
        this.startTime = startTime;
    }
    
    public double getFinishTime() {
        return finishTime;
    }
    
    public void setFinishTime(double finishTime) {
        this.finishTime = finishTime;
    }
    
    public double getTotalEnergy() {
        return totalEnergy;
    }
    
    public void setTotalEnergy(double totalEnergy) {
        this.totalEnergy = totalEnergy;
    }
    
    public double getExecutionTime() {
        return finishTime - startTime;
    }
    
    /**
     * Set the execution time directly (for compatibility)
     * 
     * @param executionTime The execution time in seconds
     */
    public void setExecutionTime(double executionTime) {
        // If start time is set, calculate finish time based on execution time
        if (startTime > 0) {
            this.finishTime = this.startTime + executionTime;
        }
    }
    
    /**
     * Get the end time, which is the same as finish time
     * 
     * @return The end/finish time
     */
    public double getEndTime() {
        return finishTime;
    }
    
    /**
     * Set the end time, which is the same as finish time
     * 
     * @param endTime The end/finish time
     */
    public void setEndTime(double endTime) {
        this.finishTime = endTime;
    }
    
    /**
     * Get the edge server ID this task is assigned to
     * 
     * @return The edge server ID, or -1 if not assigned to an edge server
     */
    public int getEdgeServerId() {
        return edgeServerId;
    }
    
    /**
     * Set the edge server ID this task is assigned to
     * 
     * @param edgeServerId The edge server ID
     */
    public void setEdgeServerId(int edgeServerId) {
        this.edgeServerId = edgeServerId;
    }
    
    /**
     * Get the device that generated this task (alias for getSourceDevice)
     * 
     * @return The source IoT device
     */
    public IoTDevice getDevice() {
        return sourceDevice;
    }
    
    public boolean isCompletedOnTime() {
        return getExecutionTime() <= maxExecutionTime;
    }
    
    public double getReward() {
        return reward;
    }
    
    public void setReward(double reward) {
        this.reward = reward;
    }
    
    public boolean isCompleted() {
        return completed;
    }
    
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
    
    @Override
    public String toString() {
        return "IoTTask{" +
                "id=" + id +
                ", length=" + taskLength +
                ", inputSize=" + inputDataSize +
                ", outputSize=" + outputDataSize +
                ", location=" + executionLocation +
                ", executionTime=" + getExecutionTime() +
                ", completed=" + completed +
                (completed ? ", reward=" + String.format("%.3f", reward) : "") +
                ", onTime=" + isCompletedOnTime() +
                '}';
    }
}
