package org.edgecomputing.simulation;

import org.edgecomputing.drl.DRLAgent;
import org.edgecomputing.metrics.PerformanceMetrics;
import org.edgecomputing.model.*;
import org.edgecomputing.utils.ConfigUtils;
import org.edgecomputing.utils.VisualizationUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Controls the overall simulation process including setup, execution, and results collection.
 * Acts as a facade for the simulation subsystem.
 */
public class SimulationController {
    // Simulation components
    private final OffloadingSimulation simulation;
    private final PerformanceMetrics metrics;
    private final String outputDir;
    private final boolean enableVisualization;
    
    // Simulation parameters
    private final int trainingEpisodes;
    private final int evaluationRuns;
    private final boolean saveModel;
    private final String modelSavePath;
    
    /**
     * Constructor to initialize simulation controller with configuration
     * @param configPath Path to the configuration file
     */
    public SimulationController(String configPath) {
        // Load configuration
        ConfigUtils.loadConfig(configPath);
        
        // Create output directory
        this.outputDir = ConfigUtils.getProperty("output_dir", "results");
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        // Set simulation parameters
        this.trainingEpisodes = ConfigUtils.getIntProperty("training_episodes", 1000);
        this.evaluationRuns = ConfigUtils.getIntProperty("evaluation_runs", 1);
        this.saveModel = ConfigUtils.getBooleanProperty("save_model", true);
        this.modelSavePath = outputDir + "/drl_model.zip";
        this.enableVisualization = ConfigUtils.getBooleanProperty("enable_visualization", true);
        
        // Initialize simulation
        this.simulation = new OffloadingSimulation(ConfigUtils.loadConfig(configPath));
        this.metrics = new PerformanceMetrics();
        
        System.out.println("Simulation controller initialized with:");
        System.out.println("  - Training episodes: " + trainingEpisodes);
        System.out.println("  - Evaluation runs: " + evaluationRuns);
        System.out.println("  - Output directory: " + outputDir);
    }
    
    /**
     * Run the complete simulation workflow
     */
    public void runSimulation() {
        System.out.println("\n=== Starting Edge Computing Task Offloading Simulation ===\n");
        
        // Run training phase
        if (trainingEpisodes > 0) {
            runTrainingPhase();
        }
        
        // Run evaluation phase
        runEvaluationPhase();
        
        // Generate results
        generateResults();
        
        System.out.println("\n=== Simulation completed successfully ===");
    }
    
    /**
     * Run the training phase of the simulation
     */
    private void runTrainingPhase() {
        System.out.println("\n=== Training Phase ===");
        long startTime = System.currentTimeMillis();
        
        simulation.runTrainingPhase(trainingEpisodes);
        
        long endTime = System.currentTimeMillis();
        System.out.println("Training completed in " + ((endTime - startTime) / 1000) + " seconds");
        
        // Save model if configured
        if (saveModel) {
            simulation.saveModel(modelSavePath);
            System.out.println("DRL model saved to: " + modelSavePath);
        }
    }
    
    /**
     * Run the evaluation phase of the simulation
     */
    private void runEvaluationPhase() {
        System.out.println("\n=== Evaluation Phase ===");
        long startTime = System.currentTimeMillis();
        
        // Reset metrics
        metrics.reset();
        
        // Run multiple evaluation runs if configured
        for (int i = 0; i < evaluationRuns; i++) {
            System.out.println("Evaluation run " + (i + 1) + "/" + evaluationRuns);
            simulation.runEvaluationPhase();
            
            // Aggregate metrics from this run
            PerformanceMetrics runMetrics = simulation.getMetrics();
            
            // Display summary of this run
            System.out.println("  Tasks: " + runMetrics.getTotalTasks() + 
                               ", Success rate: " + String.format("%.2f%%", runMetrics.getSuccessRate() * 100) +
                               ", Avg latency: " + String.format("%.2f ms", runMetrics.getAverageLatency()));
        }
        
        long endTime = System.currentTimeMillis();
        System.out.println("Evaluation completed in " + ((endTime - startTime) / 1000) + " seconds");
    }
    
    /**
     * Generate simulation result outputs
     */
    private void generateResults() {
        System.out.println("\n=== Generating Results ===");
        
        // Get final metrics
        PerformanceMetrics finalMetrics = simulation.getMetrics();
        
        // Generate textual report
        String report = finalMetrics.generateSummaryReport();
        System.out.println("\n" + report);
        
        // Export to CSV
        VisualizationUtils.exportToCsv(finalMetrics, outputDir);
        
        // Generate visualizations if enabled
        if (enableVisualization) {
            VisualizationUtils.generateResultsOutput(finalMetrics, outputDir);
            System.out.println("Visualization charts generated in: " + outputDir);
        }
        
        System.out.println("All results saved to: " + outputDir);
    }
    
    /**
     * Run a comparison between DRL offloading and baseline approaches
     */
    public void runComparisonExperiment() {
        System.out.println("\n=== Running Comparison Experiment ===\n");
        
        // Run DRL approach (already done in the regular evaluation)
        runEvaluationPhase();
        PerformanceMetrics drlMetrics = simulation.getMetrics();
        
        // Run baseline approaches (local-only, edge-only, cloud-only)
        // These would be implemented in a full simulation
        System.out.println("DRL Approach Performance:");
        System.out.println("  - Average latency: " + String.format("%.2f ms", drlMetrics.getAverageLatency()));
        System.out.println("  - Average energy: " + String.format("%.5f J", drlMetrics.getAverageEnergy()));
        System.out.println("  - Success rate: " + String.format("%.2f%%", drlMetrics.getSuccessRate() * 100));
        
        // In a full implementation, we'd add code to run the simulation with fixed offloading policies
        // and collect their metrics for comparison
    }
}
