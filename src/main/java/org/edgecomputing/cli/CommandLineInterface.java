package org.edgecomputing.cli;

import org.edgecomputing.experiment.ComparisonExperiment;
import org.edgecomputing.simulation.SimulationController;
import org.edgecomputing.test.TestRunner;

/**
 * Command-line interface for the Edge Computing Task Offloading simulation.
 * Provides easy access to different execution modes.
 */
public class CommandLineInterface {
    
    /**
     * Main entry point for the command-line interface
     * @param args Command-line arguments
     */
    public static void main(String[] args) {
        System.out.println("Edge Computing Task Offloading Simulation");
        System.out.println("=========================================");
        
        // Default config path
        String configPath = "src/main/resources/config.properties";
        
        // Parse arguments
        String mode = "simulation";
        if (args.length > 0) {
            mode = args[0].toLowerCase();
            
            if (args.length > 1) {
                configPath = args[1];
            }
        }
        
        // Execute requested mode
        switch (mode) {
            case "simulation":
                runSimulation(configPath);
                break;
                
            case "experiment":
                runExperiment(configPath);
                break;
                
            case "test":
                runTests(configPath);
                break;
                
            case "help":
                showHelp();
                break;
                
            default:
                System.out.println("Unknown mode: " + mode);
                showHelp();
                break;
        }
    }
    
    /**
     * Run the main simulation
     * @param configPath Path to the configuration file
     */
    private static void runSimulation(String configPath) {
        System.out.println("\nRunning simulation with config: " + configPath);
        
        try {
            SimulationController controller = new SimulationController(configPath);
            controller.runSimulation();
        } catch (Exception e) {
            System.err.println("Error running simulation: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Run the comparison experiment
     * @param configPath Path to the configuration file
     */
    private static void runExperiment(String configPath) {
        System.out.println("\nRunning comparison experiment with config: " + configPath);
        
        try {
            ComparisonExperiment experiment = new ComparisonExperiment(configPath);
            experiment.runExperiment();
        } catch (Exception e) {
            System.err.println("Error running experiment: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Run test suite
     * @param configPath Path to the configuration file
     */
    private static void runTests(String configPath) {
        System.out.println("\nRunning tests with config: " + configPath);
        
        try {
            String[] testArgs = {configPath};
            TestRunner.main(testArgs);
        } catch (Exception e) {
            System.err.println("Error running tests: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Show help information
     */
    private static void showHelp() {
        System.out.println("\nEdge Computing Task Offloading Simulation - Help");
        System.out.println("================================================");
        System.out.println("Usage: java -jar edge-offloading.jar [MODE] [CONFIG_PATH]");
        System.out.println("\nAvailable modes:");
        System.out.println("  simulation  - Run the main simulation (default)");
        System.out.println("  experiment  - Run comparison experiments between DRL and baseline strategies");
        System.out.println("  test        - Run the test suite");
        System.out.println("  help        - Show this help information");
        System.out.println("\nExamples:");
        System.out.println("  java -jar edge-offloading.jar");
        System.out.println("  java -jar edge-offloading.jar simulation src/main/resources/config.properties");
        System.out.println("  java -jar edge-offloading.jar experiment custom-config.properties");
    }
}
