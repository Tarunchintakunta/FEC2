package org.edgecomputing.model;

import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Cloud Datacenter with significantly more computing resources
 * than edge servers, but with higher network latency from IoT devices.
 */
public class CloudDatacenter {
    private final int id;
    private final String name;
    private final int numHosts;
    private final int mipsPerHost;
    private final int ramPerHost; // RAM in MB
    private final int storagePerHost; // Storage in MB
    private int bandwidthPerHost; // Network bandwidth in Mbps
    private final int pesPerHost; // Number of processing elements per host
    private final Datacenter datacenter; // CloudSim representation
    private final List<Host> hostList;
    
    // Stats
    private int tasksReceived;
    private int tasksCompleted;
    private int tasksFailed;
    private double totalProcessingTime;
    private double averageUtilization;
    
    public CloudDatacenter(int id, String name, int numHosts, int mipsPerHost, int ramPerHost,
                          int storagePerHost, int bandwidthPerHost, int pesPerHost) {
        this.id = id;
        this.name = name;
        this.numHosts = numHosts;
        this.mipsPerHost = mipsPerHost;
        this.ramPerHost = ramPerHost;
        this.storagePerHost = storagePerHost;
        this.bandwidthPerHost = bandwidthPerHost;
        this.pesPerHost = pesPerHost;
        
        this.tasksReceived = 0;
        this.tasksCompleted = 0;
        this.tasksFailed = 0;
        this.totalProcessingTime = 0.0;
        this.averageUtilization = 0.0;
        
        // Create hosts for this datacenter
        this.hostList = createHosts();
        
        // Create CloudSim datacenter
        this.datacenter = createDatacenter();
    }
    
    private List<Host> createHosts() {
        List<Host> hosts = new ArrayList<>();
        
        // Limit the number of hosts and PEs to avoid OutOfMemoryError
        int safeNumHosts = Math.min(numHosts, 2); // Cap at 2 hosts for testing
        int safePesPerHost = Math.min(pesPerHost, 2); // Cap at 2 PEs per host for testing
        
        for (int i = 0; i < safeNumHosts; i++) {
            // Create PEs (Processing Elements)
            List<Pe> peList = new ArrayList<>();
            for (int j = 0; j < safePesPerHost; j++) {
                peList.add(new PeSimple(mipsPerHost));
            }
            
            // Create a host using CloudSim Plus 5.0.0 API
            Host host = new HostSimple(ramPerHost, bandwidthPerHost, storagePerHost, peList);
            hosts.add(host);
        }
        
        return hosts;
    }
    
    private Datacenter createDatacenter() {
        // In CloudSim Plus 5.0.0, DatacenterSimple requires a Simulation instance
        // We'll create a temporary CloudSim instance for now
        // In a real implementation, this should be passed from the simulation controller
        CloudSim simulation = new CloudSim();
        return new DatacenterSimple(simulation, hostList, new VmAllocationPolicySimple());
    }
    
    // Method to calculate task execution time on the cloud
    public double calculateTaskExecutionTime(IoTTask task) {
        // In a cloud, we assume abundant resources, so the execution time is mainly
        // determined by the task length and cloud processing capacity
        double totalCloudMips = numHosts * pesPerHost * mipsPerHost;
        return (double) task.getTaskLength() / totalCloudMips;
    }
    
    // Method to calculate total task latency including network delay
    public double calculateTotalTaskLatency(IoTTask task, double networkLatency) {
        return calculateTaskExecutionTime(task) + networkLatency;
    }
    
    // Record task being received
    public void receiveTask() {
        tasksReceived++;
    }
    
    // Record task completion
    public void completeTask(IoTTask task, double executionTime) {
        tasksCompleted++;
        totalProcessingTime += executionTime;
        
        // Update average utilization (simple moving average)
        double taskUtilization = task.getTaskLength() / (double)(numHosts * pesPerHost * mipsPerHost * executionTime);
        averageUtilization = ((averageUtilization * (tasksCompleted - 1)) + taskUtilization) / tasksCompleted;
    }
    
    // Record task failure
    public void failTask() {
        tasksFailed++;
    }
    
    // Getters
    public int getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public int getNumHosts() {
        return numHosts;
    }
    
    public int getMipsPerHost() {
        return mipsPerHost;
    }
    
    public int getTotalMips() {
        return numHosts * pesPerHost * mipsPerHost;
    }
    
    public Datacenter getDatacenter() {
        return datacenter;
    }
    
    public List<Host> getHostList() {
        return hostList;
    }
    
    public int getTasksReceived() {
        return tasksReceived;
    }
    
    public int getTasksCompleted() {
        return tasksCompleted;
    }
    
    public int getTasksFailed() {
        return tasksFailed;
    }
    
    public double getAverageProcessingTime() {
        return tasksCompleted > 0 ? totalProcessingTime / tasksCompleted : 0.0;
    }
    
    public double getAverageUtilization() {
        return averageUtilization;
    }
    
    /**
     * Get the total MIPS capacity of this datacenter
     * @return Total MIPS capacity
     */
    public int getMipsCapacity() {
        return getTotalMips();
    }
    
    /**
     * Get the bandwidth capacity of this datacenter
     * @return Bandwidth in Mbps
     */
    public int getBandwidth() {
        return bandwidthPerHost * numHosts;
    }
    
    /**
     * Set the bandwidth per host
     * @param bandwidth Bandwidth in Mbps
     */
    public void setBandwidth(double bandwidthPerHost) {
        this.bandwidthPerHost = (int)bandwidthPerHost;
    }
    
    /**
     * Reset datacenter stats for a new simulation run
     */
    public void reset() {
        this.tasksReceived = 0;
        this.tasksCompleted = 0;
        this.tasksFailed = 0;
    }
    
    @Override
    public String toString() {
        return "CloudDatacenter{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", hosts=" + numHosts +
                ", totalMips=" + getTotalMips() +
                '}';
    }
}
