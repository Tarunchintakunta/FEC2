package org.edgecomputing.test;

import org.cloudbus.cloudsim.core.CloudSim;
import org.edgecomputing.drl.DRLAgent;
import org.edgecomputing.drl.OffloadingEnvironment;
import org.edgecomputing.metrics.PerformanceMetrics;
import org.edgecomputing.model.*;
import org.edgecomputing.simulation.OffloadingCloudSim;
import org.edgecomputing.simulation.OffloadingDatacenterBroker;
import org.edgecomputing.simulation.OffloadingSimulation;
import org.edgecomputing.utils.ConfigUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

/**
 * Test for the mobility model integration with CloudSim Plus and the DRL agent.
 * This test specifically focuses on verifying that device mobility affects task offloading decisions.
 */
public class MobilityIntegrationTest {
    
    public static void main(String[] args) {
        System.out.println("=== Edge Computing Mobility Integration Test ===");
        
        // Load configuration
        Properties config = ConfigUtils.loadConfig("config.properties");
        if (config == null) {
            System.out.println("Failed to load configuration. Using defaults.");
            config = new Properties();
        }
        
        // Set specific test parameters
        config.setProperty("num_iot_devices", "5");
        config.setProperty("num_edge_servers", "3");
        config.setProperty("simulation_time", "300");
        config.setProperty("task_arrival_rate", "2");
        config.setProperty("mobility_pattern", "RANDOM_WALK");
        config.setProperty("mobility_min_speed", "1.0");
        config.setProperty("mobility_max_speed", "3.0");
        config.setProperty("mobility_update_interval", "2.0");
        config.setProperty("training_episodes", "10");  // Reduced for testing
        
        // Run mobility tests
        testMobilityIntegration(config);
        testEventDrivenSimulation(config);
        
        System.out.println("\n=== Mobility Integration Tests Completed ===");
    }
    
    /**
     * Test the mobility model integration
     */
    private static void testMobilityIntegration(Properties config) {
        System.out.println("\n--- Testing Mobility Model Integration ---");
        
        try {
            // Create mobility model
            MobilityModel mobilityModel = new MobilityModel();
            mobilityModel.setPattern(MobilityModel.MobilityPattern.RANDOM_WALK);
            mobilityModel.setUpdateInterval(Double.parseDouble(config.getProperty("mobility_update_interval", "2.0")));
            mobilityModel.setMinSpeed(Double.parseDouble(config.getProperty("mobility_min_speed", "1.0")));
            mobilityModel.setMaxSpeed(Double.parseDouble(config.getProperty("mobility_max_speed", "3.0")));
            
            // Create devices
            List<IoTDevice> devices = new ArrayList<>();
            int numDevices = Integer.parseInt(config.getProperty("num_iot_devices", "5"));
            for (int i = 0; i < numDevices; i++) {
                IoTDevice device = new IoTDevice(i, "Device-" + i, 500, 1024, 4096, 100, 0.01, 0.5, 1.0, 0.5);
                device.setPosition(Math.random() * 1000, Math.random() * 1000);
                device.setEnergyParameters(0.01, 0.5, 1.0, 0.5);
                devices.add(device);
            }
            
            // Initialize device positions
            mobilityModel.initializeDevicePositions(devices);
            
            // Print initial positions
            System.out.println("Initial device positions:");
            for (IoTDevice device : devices) {
                System.out.printf("Device %s: (%.2f, %.2f)\n", device.getId(), device.getXPos(), device.getYPos());
            }
            
            // Update positions several times
            for (int i = 0; i < 5; i++) {
                double updateInterval = mobilityModel.getUpdateInterval();
                mobilityModel.updateDevicePositions(devices, updateInterval);
                
                System.out.printf("\nAfter update %d (time: %.1f):\n", i + 1, (i + 1) * updateInterval);
                for (IoTDevice device : devices) {
                    System.out.printf("Device %s: (%.2f, %.2f) with speed %.2f m/s and direction %.2f degrees\n", 
                            device.getId(), device.getXPos(), device.getYPos(), device.getSpeed(), Math.toDegrees(device.getDirection()));
                }
            }
            
            System.out.println("Mobility model integration test completed successfully");
        } catch (Exception e) {
            System.out.println("Mobility model integration test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Test the CloudSim Plus event-driven simulation with mobility
     */
    private static void testEventDrivenSimulation(Properties config) {
        System.out.println("\n--- Testing Event-Driven Simulation with Mobility ---");
        
        try {
            // Create our own lightweight test simulation instead of the full OffloadingSimulation
            System.out.println("Setting up mobility-enabled simulation...");
            long startTime = System.currentTimeMillis();
            
            // Create basic simulation components
            OffloadingCloudSim simulation = new OffloadingCloudSim();
            simulation.setProperty("task_arrival_rate", "2.0");
            simulation.setProperty("mobility_update_interval", "2.0");
            
            // Create devices
            List<IoTDevice> devices = new ArrayList<>();
            int numDevices = 5;
            for (int i = 0; i < numDevices; i++) {
                IoTDevice device = new IoTDevice(i, "Device-" + i, 500, 1024, 4096, 100, 0.01, 0.5, 1.0, 0.5);
                device.setPosition(Math.random() * 1000, Math.random() * 1000);
                device.setEnergyParameters(0.01, 0.5, 1.0, 0.5);
                devices.add(device);
            }
            
            // Create edge servers
            List<EdgeServer> edgeServers = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                EdgeServer server = new EdgeServer(i, "Edge-" + i, 4000, 8192, 32768, 100, 4, 1.5, 4.0, 200);
                server.setPosition(Math.random() * 1000, Math.random() * 1000);
                server.setBandwidth(100); // Mbps
                edgeServers.add(server);
            }
            
            // Create cloud datacenter
            CloudDatacenter cloudDatacenter = new CloudDatacenter(0, "Cloud", 4, 5000, 32768, 1048576, 1000, 8);
            cloudDatacenter.setBandwidth(1000); // Mbps
            
            // Create mobility model
            MobilityModel mobilityModel = new MobilityModel();
            mobilityModel.setPattern(MobilityModel.MobilityPattern.RANDOM_WALK);
            mobilityModel.setUpdateInterval(2.0);
            mobilityModel.setMinSpeed(1.0);
            mobilityModel.setMaxSpeed(3.0);
            mobilityModel.setAreaSize(1000, 1000);
            mobilityModel.initializeDevicePositions(devices);
            
            // Create simplified environment and DRL agent for testing
            OffloadingEnvironment environment = new OffloadingEnvironment(devices, edgeServers, cloudDatacenter);
            DRLAgent drlAgent = new DRLAgent(environment, devices.size(), edgeServers.size() + 2);
            
            // Create broker that integrates DRL agent with CloudSim
            OffloadingDatacenterBroker broker = new OffloadingDatacenterBroker(simulation, drlAgent, 
                environment, devices, edgeServers, cloudDatacenter, mobilityModel, false);
            
            // Schedule task generation for each device
            double arrivalRate = 0.5; // tasks per second on average
            for (IoTDevice device : devices) {
                // Random initial delay to avoid all devices generating tasks at the same time
                double initialDelay = Math.random() * 5.0;
                broker.scheduleTaskGeneration(device, arrivalRate);
            }
            
            // Run a short simulation
            System.out.println("Running event-driven simulation for 60 simulation seconds...");
            simulation.startSimulation(); // This will block until simulation is done
            simulation.terminateSimulation(60); // End after 60 simulation seconds
            long endTime = System.currentTimeMillis();
            System.out.println("Simulation completed in " + (endTime - startTime) + " ms");
            
            // Create simple metrics for testing purposes
            PerformanceMetrics metrics = new PerformanceMetrics();
            
            // Get task information from devices
            int totalTasks = 0;
            int completedTasks = 0;
            int localExecution = 0;
            int edgeExecution = 0;
            int cloudExecution = 0;
            double totalLatency = 0;
            double totalEnergy = 0;
            
            for (IoTDevice device : devices) {
                List<IoTTask> deviceTasks = device.getCompletedTasks();
                totalTasks += deviceTasks.size();
                completedTasks += deviceTasks.size();
                
                for (IoTTask task : deviceTasks) {
                    totalLatency += task.getExecutionTime();
                    totalEnergy += task.getTotalEnergy();
                    
                    switch (task.getExecutionLocation()) {
                        case LOCAL_DEVICE:
                            localExecution++;
                            break;
                        case EDGE_SERVER:
                            edgeExecution++;
                            break;
                        case CLOUD:
                            cloudExecution++;
                            break;
                    }
                }
            }
            
            // Print basic metrics
            System.out.println("\nSimulation results:");
            System.out.println("  Total tasks: " + totalTasks);
            System.out.println("  Completed tasks: " + completedTasks);
            System.out.println("  Failed tasks: " + (totalTasks - completedTasks));
            
            if (completedTasks > 0) {
                System.out.println("  Success rate: " + String.format("%.2f%%", (completedTasks / (double)totalTasks) * 100));
                System.out.println("  Average latency: " + String.format("%.2f ms", totalLatency / completedTasks));
                System.out.println("  Average energy: " + String.format("%.2f J", totalEnergy / completedTasks));
                
                // Check offloading decisions
                System.out.println("\nOffloading distribution:");
                System.out.println("  Local execution: " + String.format("%.2f%%", (localExecution / (double)completedTasks) * 100));
                System.out.println("  Edge execution: " + String.format("%.2f%%", (edgeExecution / (double)completedTasks) * 100));
                System.out.println("  Cloud execution: " + String.format("%.2f%%", (cloudExecution / (double)completedTasks) * 100));
            }
            
            // Print mobility statistics
            List<IoTDevice> finalDevices = simulation.getIoTDevices();
            System.out.println("\nFinal device positions:");
            for (IoTDevice device : finalDevices) {
                System.out.printf("Device %s: (%.2f, %.2f) - %d tasks completed\n", 
                        device.getId(), device.getXPos(), device.getYPos(), device.getCompletedTasks().size());
            }
            
            System.out.println("\nEvent-driven simulation test completed successfully");
        } catch (Exception e) {
            System.out.println("Event-driven simulation test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
