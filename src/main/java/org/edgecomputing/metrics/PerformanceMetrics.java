package org.edgecomputing.metrics;

import org.edgecomputing.model.IoTTask;
import java.util.*;

/**
 * Collects and analyzes performance metrics for the task offloading system.
 */
public class PerformanceMetrics {
    // Task statistics
    private int totalTasks;
    private int successfulTasks;
    private int failedTasks;
    
    // Execution location statistics
    private int localExecutionCount;
    private int edgeExecutionCount;
    private int cloudExecutionCount;
    
    // Performance metrics
    private double totalLatency; // in milliseconds
    private double totalEnergy;  // in Joules
    private double totalNetworkUsage; // in KB
    
    // Detailed metrics
    private final List<Double> latencyValues;
    private final List<Double> energyValues;
    private final Map<String, List<Double>> serverLatencyMap;
    
    public PerformanceMetrics() {
        latencyValues = new ArrayList<>();
        energyValues = new ArrayList<>();
        serverLatencyMap = new HashMap<>();
        reset();
    }
    
    /**
     * Reset all metrics
     */
    public void reset() {
        totalTasks = 0;
        successfulTasks = 0;
        failedTasks = 0;
        localExecutionCount = 0;
        edgeExecutionCount = 0;
        cloudExecutionCount = 0;
        totalLatency = 0.0;
        totalEnergy = 0.0;
        totalNetworkUsage = 0.0;
        
        latencyValues.clear();
        energyValues.clear();
        serverLatencyMap.clear();
    }
    
    /**
     * Record metrics for a completed task
     * @param task The completed task
     * @param latency The task completion latency in ms
     */
    public void recordTask(IoTTask task, double latency) {
        totalTasks++;
        
        // Record latency and energy
        totalLatency += latency;
        totalEnergy += task.getTotalEnergy();
        latencyValues.add(latency);
        energyValues.add(task.getTotalEnergy());
        
        // Record execution location
        switch (task.getExecutionLocation()) {
            case LOCAL_DEVICE:
                localExecutionCount++;
                break;
                
            case EDGE_SERVER:
                edgeExecutionCount++;
                // Record per-server latency if needed
                if (task.getCloudlet() != null && task.getCloudlet().getVm() != null) {
                    String serverName = "Edge" + (task.getCloudlet().getVm().getHost().getId() + 1);
                    serverLatencyMap.computeIfAbsent(serverName, k -> new ArrayList<>()).add(latency);
                } else {
                    // Fallback for tasks processed directly without CloudSim cloudlets
                    String serverName = "EdgeServer";
                    serverLatencyMap.computeIfAbsent(serverName, k -> new ArrayList<>()).add(latency);
                }
                break;
                
            case CLOUD:
                cloudExecutionCount++;
                break;
        }
        
        // Record success or failure
        if (task.isCompletedOnTime()) {
            successfulTasks++;
        } else {
            failedTasks++;
        }
        
        // Record network usage for offloaded tasks
        if (task.getExecutionLocation() != IoTTask.TaskLocation.LOCAL_DEVICE) {
            totalNetworkUsage += task.getInputDataSize() + task.getOutputDataSize();
        }
    }
    
    /**
     * Get the average latency across all tasks
     */
    public double getAverageLatency() {
        return totalTasks > 0 ? totalLatency / totalTasks : 0.0;
    }
    
    /**
     * Get the average energy consumption across all tasks
     */
    public double getAverageEnergy() {
        return totalTasks > 0 ? totalEnergy / totalTasks : 0.0;
    }
    
    /**
     * Get the task success rate
     */
    public double getSuccessRate() {
        return totalTasks > 0 ? (double) successfulTasks / totalTasks : 0.0;
    }
    
    /**
     * Get the percentage of tasks executed locally
     */
    public double getLocalExecutionPercentage() {
        return totalTasks > 0 ? (double) localExecutionCount / totalTasks : 0.0;
    }
    
    /**
     * Get the percentage of tasks executed on edge servers
     */
    public double getEdgeExecutionPercentage() {
        return totalTasks > 0 ? (double) edgeExecutionCount / totalTasks : 0.0;
    }
    
    /**
     * Get the percentage of tasks executed on cloud
     */
    public double getCloudExecutionPercentage() {
        return totalTasks > 0 ? (double) cloudExecutionCount / totalTasks : 0.0;
    }
    
    /**
     * Get the total network usage in KB
     */
    public double getTotalNetworkUsage() {
        return totalNetworkUsage;
    }
    
    /**
     * Get the average network usage per task in KB
     */
    public double getAverageNetworkUsage() {
        int offloadedTasks = edgeExecutionCount + cloudExecutionCount;
        return offloadedTasks > 0 ? totalNetworkUsage / offloadedTasks : 0.0;
    }
    
    /**
     * Get the percentile latency
     * @param percentile The percentile to calculate (e.g., 95 for 95th percentile)
     */
    public double getLatencyPercentile(double percentile) {
        if (latencyValues.isEmpty()) {
            return 0.0;
        }
        
        List<Double> sortedLatencies = new ArrayList<>(latencyValues);
        Collections.sort(sortedLatencies);
        
        int index = (int) Math.ceil(percentile / 100.0 * sortedLatencies.size()) - 1;
        index = Math.max(0, Math.min(sortedLatencies.size() - 1, index));
        
        return sortedLatencies.get(index);
    }
    
    /**
     * Get latency statistics for a specific server
     * @param serverName The name of the server
     */
    public Map<String, Double> getServerLatencyStats(String serverName) {
        List<Double> latencies = serverLatencyMap.get(serverName);
        if (latencies == null || latencies.isEmpty()) {
            return Collections.emptyMap();
        }
        
        Map<String, Double> stats = new HashMap<>();
        stats.put("min", Collections.min(latencies));
        stats.put("max", Collections.max(latencies));
        stats.put("avg", latencies.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
        stats.put("count", (double) latencies.size());
        
        return stats;
    }
    
    // Getters for all metrics
    public int getTotalTasks() {
        return totalTasks;
    }
    
    public int getSuccessfulTasks() {
        return successfulTasks;
    }
    
    public int getFailedTasks() {
        return failedTasks;
    }
    
    public int getLocalExecutionCount() {
        return localExecutionCount;
    }
    
    public int getEdgeExecutionCount() {
        return edgeExecutionCount;
    }
    
    public int getCloudExecutionCount() {
        return cloudExecutionCount;
    }
    
    public double getTotalLatency() {
        return totalLatency;
    }
    
    public double getTotalEnergy() {
        return totalEnergy;
    }
    
    /**
     * Record metrics for a completed task with explicit values
     * @param taskId The ID of the completed task
     * @param successful Whether the task was successful
     * @param location The execution location of the task
     * @param latency The task completion latency in ms
     * @param energy The energy consumption in Joules
     * @param networkUsage The network usage in KB
     */
    public void recordTaskCompletion(int taskId, boolean successful, IoTTask.TaskLocation location, 
                                    double latency, double energy, double networkUsage) {
        totalTasks++;
        
        // Record latency and energy
        totalLatency += latency;
        totalEnergy += energy;
        latencyValues.add(latency);
        energyValues.add(energy);
        
        // Record execution location
        switch (location) {
            case LOCAL_DEVICE:
                localExecutionCount++;
                break;
                
            case EDGE_SERVER:
                edgeExecutionCount++;
                // Use a generic server name since we don't have the specific server info
                String serverName = "Edge" + (taskId % 3 + 1); // Simple mapping to distribute tasks
                serverLatencyMap.computeIfAbsent(serverName, k -> new ArrayList<>()).add(latency);
                break;
                
            case CLOUD:
                cloudExecutionCount++;
                break;
        }
        
        // Record success or failure
        if (successful) {
            successfulTasks++;
        } else {
            failedTasks++;
        }
        
        // Record network usage for offloaded tasks
        if (location != IoTTask.TaskLocation.LOCAL_DEVICE) {
            totalNetworkUsage += networkUsage;
        }
    }
    
    /**
     * Export metrics to CSV format
     * @return CSV string with metrics data
     */
    public String exportToCsv() {
        StringBuilder sb = new StringBuilder();
        
        // CSV header
        sb.append("metric,value\n");
        
        // Task statistics
        sb.append("totalTasks,").append(totalTasks).append("\n");
        sb.append("successfulTasks,").append(successfulTasks).append("\n");
        sb.append("failedTasks,").append(failedTasks).append("\n");
        sb.append("successRate,").append(getSuccessRate()).append("\n");
        
        // Execution location distribution
        sb.append("localExecutionCount,").append(localExecutionCount).append("\n");
        sb.append("edgeExecutionCount,").append(edgeExecutionCount).append("\n");
        sb.append("cloudExecutionCount,").append(cloudExecutionCount).append("\n");
        sb.append("localExecutionPercentage,").append(getLocalExecutionPercentage()).append("\n");
        sb.append("edgeExecutionPercentage,").append(getEdgeExecutionPercentage()).append("\n");
        sb.append("cloudExecutionPercentage,").append(getCloudExecutionPercentage()).append("\n");
        
        // Performance metrics
        sb.append("averageLatency,").append(getAverageLatency()).append("\n");
        sb.append("95thPercentileLatency,").append(getLatencyPercentile(95)).append("\n");
        sb.append("averageEnergy,").append(getAverageEnergy()).append("\n");
        sb.append("totalNetworkUsage,").append(getTotalNetworkUsage()).append("\n");
        sb.append("averageNetworkUsage,").append(getAverageNetworkUsage()).append("\n");
        
        return sb.toString();
    }
    
    /**
     * Generate a summary report of all metrics
     */
    public String generateSummaryReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("==== Task Offloading Performance Metrics Summary ====\n\n");
        
        sb.append("Task Statistics:\n");
        sb.append("  Total Tasks: ").append(totalTasks).append("\n");
        sb.append("  Successful Tasks: ").append(successfulTasks).append(" (")
          .append(String.format("%.2f", getSuccessRate() * 100)).append("%)\n");
        sb.append("  Failed Tasks: ").append(failedTasks).append(" (")
          .append(String.format("%.2f", (1 - getSuccessRate()) * 100)).append("%)\n\n");
        
        sb.append("Execution Location Distribution:\n");
        sb.append("  Local Execution: ").append(localExecutionCount).append(" (")
          .append(String.format("%.2f", getLocalExecutionPercentage() * 100)).append("%)\n");
        sb.append("  Edge Execution: ").append(edgeExecutionCount).append(" (")
          .append(String.format("%.2f", getEdgeExecutionPercentage() * 100)).append("%)\n");
        sb.append("  Cloud Execution: ").append(cloudExecutionCount).append(" (")
          .append(String.format("%.2f", getCloudExecutionPercentage() * 100)).append("%)\n\n");
        
        sb.append("Performance Metrics:\n");
        sb.append("  Average Latency: ").append(String.format("%.2f", getAverageLatency())).append(" ms\n");
        sb.append("  95th Percentile Latency: ").append(String.format("%.2f", getLatencyPercentile(95))).append(" ms\n");
        sb.append("  Average Energy Consumption: ").append(String.format("%.4f", getAverageEnergy())).append(" J\n");
        sb.append("  Average Network Usage (offloaded tasks): ").append(String.format("%.2f", getAverageNetworkUsage())).append(" KB\n");
        sb.append("  Total Network Usage: ").append(String.format("%.2f", getTotalNetworkUsage())).append(" KB\n");
        
        return sb.toString();
    }
}