package org.edgecomputing.strategy;

import org.edgecomputing.model.*;

import java.util.List;
import java.util.Random;

/**
 * Implements baseline task offloading strategies for comparison with DRL.
 * Includes strategies like local-only, edge-only, cloud-only, random, and greedy.
 */
public class BaselineStrategy {
    
    /**
     * Enum defining the available baseline offloading strategies
     */
    public enum StrategyType {
        LOCAL_ONLY,     // Execute all tasks locally
        EDGE_ONLY,      // Execute all tasks on edge servers
        CLOUD_ONLY,     // Execute all tasks on cloud
        RANDOM,         // Random offloading decisions
        GREEDY_LATENCY, // Greedy strategy optimizing for latency
        GREEDY_ENERGY   // Greedy strategy optimizing for energy
    }
    
    private final Random random;
    private final StrategyType strategyType;
    private final NetworkModel networkModel;
    
    /**
     * Constructor for baseline strategy
     * @param strategyType Type of strategy to use
     * @param networkModel Network model to calculate latencies
     */
    public BaselineStrategy(StrategyType strategyType, NetworkModel networkModel) {
        this.strategyType = strategyType;
        this.networkModel = networkModel;
        this.random = new Random(42); // Fixed seed for reproducibility
    }
    
    /**
     * Make offloading decision based on the selected strategy
     * @param task Task to be offloaded
     * @param device Source IoT device
     * @param edgeServers Available edge servers
     * @param cloud Cloud datacenter
     * @return Offloading location (0 for local, 1..N for edge servers, N+1 for cloud)
     */
    public int makeOffloadingDecision(IoTTask task, IoTDevice device, 
                                     List<EdgeServer> edgeServers, CloudDatacenter cloud) {
        switch (strategyType) {
            case LOCAL_ONLY:
                return 0; // Always execute locally
                
            case EDGE_ONLY:
                // Find nearest edge server (with capacity)
                return findBestEdgeServer(task, device, edgeServers, false);
                
            case CLOUD_ONLY:
                return edgeServers.size() + 1; // Cloud is always last index
                
            case RANDOM:
                // Random choice between local, edge servers, and cloud
                return random.nextInt(edgeServers.size() + 2);
                
            case GREEDY_LATENCY:
                return greedyLatencyStrategy(task, device, edgeServers, cloud);
                
            case GREEDY_ENERGY:
                return greedyEnergyStrategy(task, device, edgeServers, cloud);
                
            default:
                return 0; // Default to local execution
        }
    }
    
    /**
     * Find the best edge server based on distance and load
     * @param task Task to be offloaded
     * @param device Source device
     * @param edgeServers Available edge servers
     * @param considerLoad Whether to consider server load in decision
     * @return Index of the best edge server (1-based) or 0 if none available
     */
    private int findBestEdgeServer(IoTTask task, IoTDevice device, 
                                  List<EdgeServer> edgeServers, boolean considerLoad) {
        double bestMetric = Double.MAX_VALUE;
        int bestServerIndex = 0; // Default to no server (local execution)
        
        for (int i = 0; i < edgeServers.size(); i++) {
            EdgeServer server = edgeServers.get(i);
            
            // Calculate distance to server
            double distance = server.calculateDistance(device);
            
            // Skip if device is outside coverage
            if (distance > server.getCoverageRadius()) {
                continue;
            }
            
            // Calculate metric (distance or combined with load)
            double metric = distance;
            if (considerLoad) {
                // Factor in current load
                double loadFactor = server.getCurrentLoad() / server.getCapacity();
                metric = distance * (1 + loadFactor);
            }
            
            // Update best server if this one is better
            if (metric < bestMetric) {
                bestMetric = metric;
                bestServerIndex = i + 1; // +1 because index 0 is reserved for local execution
            }
        }
        
        return bestServerIndex;
    }
    
    /**
     * Greedy strategy to minimize task execution latency
     * @param task Task to be offloaded
     * @param device Source device
     * @param edgeServers Available edge servers
     * @param cloud Cloud datacenter
     * @return Offloading location (0 for local, 1..N for edge servers, N+1 for cloud)
     */
    private int greedyLatencyStrategy(IoTTask task, IoTDevice device, 
                                    List<EdgeServer> edgeServers, CloudDatacenter cloud) {
        double bestLatency = Double.MAX_VALUE;
        int bestLocation = 0; // Default to local execution
        
        // Calculate local execution latency
        double localLatency = device.calculateTaskExecutionTime(task);
        if (localLatency < bestLatency) {
            bestLatency = localLatency;
            bestLocation = 0;
        }
        
        // Check each edge server
        for (int i = 0; i < edgeServers.size(); i++) {
            EdgeServer server = edgeServers.get(i);
            
            // Calculate distance to server
            double distance = server.calculateDistance(device);
            
            // Skip if device is outside coverage
            if (distance > server.getCoverageRadius()) {
                continue;
            }
            
            // Calculate total latency for edge offloading
            double edgeLatency = networkModel.calculateEdgeOffloadingLatency(task, device, server);
            
            if (edgeLatency < bestLatency) {
                bestLatency = edgeLatency;
                bestLocation = i + 1;
            }
        }
        
        // Check cloud execution
        // Assume edge server 0 as relay if we have any edge servers
        if (!edgeServers.isEmpty()) {
            EdgeServer relay = edgeServers.get(0);
            double cloudLatency = networkModel.calculateCloudOffloadingLatency(task, device, relay, cloud);
            
            if (cloudLatency < bestLatency) {
                bestLatency = cloudLatency;
                bestLocation = edgeServers.size() + 1;
            }
        }
        
        return bestLocation;
    }
    
    /**
     * Greedy strategy to minimize energy consumption
     * @param task Task to be offloaded
     * @param device Source device
     * @param edgeServers Available edge servers
     * @param cloud Cloud datacenter
     * @return Offloading location (0 for local, 1..N for edge servers, N+1 for cloud)
     */
    private int greedyEnergyStrategy(IoTTask task, IoTDevice device, 
                                   List<EdgeServer> edgeServers, CloudDatacenter cloud) {
        double bestEnergy = Double.MAX_VALUE;
        int bestLocation = 0; // Default to local execution
        
        // Calculate local execution energy
        double localEnergy = device.calculateEnergyConsumption(task);
        if (localEnergy < bestEnergy) {
            bestEnergy = localEnergy;
            bestLocation = 0;
        }
        
        // Check each edge server
        for (int i = 0; i < edgeServers.size(); i++) {
            EdgeServer server = edgeServers.get(i);
            
            // Calculate distance to server
            double distance = server.calculateDistance(device);
            
            // Skip if device is outside coverage
            if (distance > server.getCoverageRadius()) {
                continue;
            }
            
            // Calculate transmission energy (depends on distance and data size)
            double transmitEnergy = device.calculateTransmissionEnergy(task.getInputDataSize(), distance);
            
            // Receive result energy (usually smaller)
            double receiveEnergy = device.calculateReceptionEnergy(task.getOutputDataSize());
            
            // Total energy for offloading
            double offloadEnergy = transmitEnergy + receiveEnergy;
            
            if (offloadEnergy < bestEnergy) {
                bestEnergy = offloadEnergy;
                bestLocation = i + 1;
            }
        }
        
        // Check cloud execution (if any edge servers are available as relay)
        if (!edgeServers.isEmpty()) {
            EdgeServer relay = edgeServers.get(0);
            double distance = relay.calculateDistance(device);
            
            // Only consider cloud if device is within coverage of relay
            if (distance <= relay.getCoverageRadius()) {
                // Calculate transmission energy to edge
                double transmitEnergy = device.calculateTransmissionEnergy(task.getInputDataSize(), distance);
                
                // Receive result energy
                double receiveEnergy = device.calculateReceptionEnergy(task.getOutputDataSize());
                
                // Total energy for cloud offloading
                double cloudEnergy = transmitEnergy + receiveEnergy;
                
                if (cloudEnergy < bestEnergy) {
                    bestEnergy = cloudEnergy;
                    bestLocation = edgeServers.size() + 1;
                }
            }
        }
        
        return bestLocation;
    }
    
    /**
     * Get the strategy type
     * @return Current strategy type
     */
    public StrategyType getStrategyType() {
        return strategyType;
    }
}
