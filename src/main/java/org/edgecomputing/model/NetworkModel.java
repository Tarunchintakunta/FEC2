package org.edgecomputing.model;

import org.edgecomputing.utils.ConfigUtils;

import java.util.List;

/**
 * Models the network connectivity between IoT devices, edge servers, and cloud.
 * Calculates network delays and bandwidth limitations.
 */
public class NetworkModel {
    // Network parameters
    private final double mobileToEdgeBandwidth;  // in Mbps
    private final double edgeToCloudBandwidth;   // in Mbps
    private final double mobileToEdgeLatency;    // in ms
    private final double edgeToCloudLatency;     // in ms
    
    // Network condition parameters (for dynamic conditions)
    private double mobileNetworkQuality;  // 0.0 to 1.0 (poor to excellent)
    private double backboneNetworkQuality; // 0.0 to 1.0 (poor to excellent)

    public NetworkModel(double mobileToEdgeBandwidth, double edgeToCloudBandwidth, 
                       double mobileToEdgeLatency, double edgeToCloudLatency) {
        this.mobileToEdgeBandwidth = mobileToEdgeBandwidth;
        this.edgeToCloudBandwidth = edgeToCloudBandwidth;
        this.mobileToEdgeLatency = mobileToEdgeLatency;
        this.edgeToCloudLatency = edgeToCloudLatency;
        
        // Initialize with good network quality
        this.mobileNetworkQuality = 0.9;
        this.backboneNetworkQuality = 0.95;
    }
    
    /**
     * Calculate data transfer time from device to edge server
     * @param dataSize Size of data to transfer in KB
     * @return Transfer time in seconds
     */
    public double calculateDeviceToEdgeTransferTime(double dataSize, IoTDevice device, EdgeServer server) {
        // Convert KB to bits
        double dataBits = dataSize * 8 * 1024;
        
        // Get effective bandwidth based on device location and network quality
        double distance = server.calculateDistance(device);
        double distanceFactor = calculateDistanceFactor(distance, server.getCoverageRadius());
        double effectiveBandwidth = mobileToEdgeBandwidth * mobileNetworkQuality * distanceFactor;
        
        // Time = data size / bandwidth
        return dataBits / (effectiveBandwidth * 1000000); // Convert Mbps to bps
    }
    
    /**
     * Calculate data transfer time from edge server to cloud
     * @param dataSize Size of data to transfer in KB
     * @return Transfer time in seconds
     */
    public double calculateEdgeToCloudTransferTime(double dataSize) {
        // Convert KB to bits
        double dataBits = dataSize * 8 * 1024;
        
        // Get effective bandwidth based on backbone network quality
        double effectiveBandwidth = edgeToCloudBandwidth * backboneNetworkQuality;
        
        // Time = data size / bandwidth
        return dataBits / (effectiveBandwidth * 1000000); // Convert Mbps to bps
    }
    
    /**
     * Calculate data transfer time from cloud to edge server
     * @param dataSize Size of data to transfer in KB
     * @return Transfer time in seconds
     */
    public double calculateCloudToEdgeTransferTime(double dataSize) {
        // Similar to edge to cloud, potentially with different parameters
        return calculateEdgeToCloudTransferTime(dataSize);
    }
    
    /**
     * Calculate data transfer time from edge server to device
     * @param dataSize Size of data to transfer in KB
     * @return Transfer time in seconds
     */
    public double calculateEdgeToDeviceTransferTime(double dataSize, IoTDevice device, EdgeServer server) {
        // Similar to device to edge, potentially with different parameters
        return calculateDeviceToEdgeTransferTime(dataSize, device, server);
    }
    
    /**
     * Calculate the total latency for offloading a task to an edge server
     * @param task Task to be offloaded
     * @param device Source device
     * @param server Target edge server
     * @return Total latency in seconds (transfer + processing + return)
     */
    public double calculateEdgeOffloadingLatency(IoTTask task, IoTDevice device, EdgeServer server) {
        // Calculate data transfer time to edge
        double uploadTime = calculateDeviceToEdgeTransferTime(task.getInputDataSize(), device, server);
        
        // Calculate processing time on edge
        double processingTime = server.calculateTaskExecutionTime(task);
        
        // Calculate data transfer time back to device
        double downloadTime = calculateEdgeToDeviceTransferTime(task.getOutputDataSize(), device, server);
        
        // Add network latency (round trip)
        double networkLatency = 2 * mobileToEdgeLatency / 1000.0; // Convert ms to seconds
        
        // Total latency
        return uploadTime + processingTime + downloadTime + networkLatency;
    }
    
    /**
     * Calculate the total latency for offloading a task to the cloud
     * @param task Task to be offloaded
     * @param device Source device
     * @param server Edge server that relays to cloud
     * @param cloud Cloud datacenter
     * @return Total latency in seconds
     */
    public double calculateCloudOffloadingLatency(IoTTask task, IoTDevice device, EdgeServer server, CloudDatacenter cloud) {
        // Calculate data transfer time to edge
        double deviceToEdgeTime = calculateDeviceToEdgeTransferTime(task.getInputDataSize(), device, server);
        
        // Calculate data transfer time to cloud
        double edgeToCloudTime = calculateEdgeToCloudTransferTime(task.getInputDataSize());
        
        // Calculate processing time on cloud
        double processingTime = cloud.calculateTaskExecutionTime(task);
        
        // Calculate data transfer time from cloud to edge
        double cloudToEdgeTime = calculateCloudToEdgeTransferTime(task.getOutputDataSize());
        
        // Calculate data transfer time from edge to device
        double edgeToDeviceTime = calculateEdgeToDeviceTransferTime(task.getOutputDataSize(), device, server);
        
        // Add network latency (round trip for both segments)
        double edgeLatency = 2 * mobileToEdgeLatency / 1000.0; // Convert ms to seconds
        double cloudLatency = 2 * edgeToCloudLatency / 1000.0; // Convert ms to seconds
        
        // Total latency
        return deviceToEdgeTime + edgeToCloudTime + processingTime + 
               cloudToEdgeTime + edgeToDeviceTime + edgeLatency + cloudLatency;
    }
    
    /**
     * Update network conditions based on a quality factor
     * This can be used to simulate network congestion or interference
     * @param mobileQualityFactor Quality factor for mobile network (0.0-1.0)
     * @param backboneQualityFactor Quality factor for backbone network (0.0-1.0)
     */
    public void updateNetworkConditions(double mobileQualityFactor, double backboneQualityFactor) {
        this.mobileNetworkQuality = Math.max(0.1, Math.min(1.0, mobileQualityFactor));
        this.backboneNetworkQuality = Math.max(0.1, Math.min(1.0, backboneQualityFactor));
    }
    
    /**
     * Calculate a factor based on the distance between device and server
     * The factor decreases as distance increases, with a sharp drop-off near the edge of coverage
     * @param distance The distance between device and server
     * @param coverageRadius The maximum coverage radius of the server
     * @return A factor between 0.0 and 1.0
     */
    private double calculateDistanceFactor(double distance, double coverageRadius) {
        if (distance >= coverageRadius) {
            return 0.0;
        }
        
        // Linear decrease with exponential drop-off near the edge
        double normalizedDistance = distance / coverageRadius;
        return Math.pow(1.0 - normalizedDistance, 2);
    }
    
    // Getters and setters
    public double getMobileToEdgeBandwidth() {
        return mobileToEdgeBandwidth;
    }
    
    public double getEdgeToCloudBandwidth() {
        return edgeToCloudBandwidth;
    }
    
    public double getMobileToEdgeLatency() {
        return mobileToEdgeLatency;
    }
    
    public double getEdgeToCloudLatency() {
        return edgeToCloudLatency;
    }
    
    public double getMobileNetworkQuality() {
        return mobileNetworkQuality;
    }
    
    public double getBackboneNetworkQuality() {
        return backboneNetworkQuality;
    }
    
    /**
     * Find the edge server closest to the given IoT device
     * @param device The IoT device
     * @param servers List of available edge servers
     * @return The closest edge server, or null if no server is available
     */
    public EdgeServer findNearestEdgeServer(IoTDevice device, List<EdgeServer> servers) {
        if (servers == null || servers.isEmpty()) {
            return null;
        }
        
        EdgeServer nearest = null;
        double minDistance = Double.MAX_VALUE;
        
        for (EdgeServer server : servers) {
            double distance = server.calculateDistance(device);
            if (distance < minDistance) {
                minDistance = distance;
                nearest = server;
            }
        }
        
        return nearest;
    }
}
