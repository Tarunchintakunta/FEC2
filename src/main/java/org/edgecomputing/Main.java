package org.edgecomputing;

import org.edgecomputing.simulation.OffloadingSimulation;
import org.edgecomputing.utils.ConfigUtils;
import org.edgecomputing.metrics.PerformanceMetrics;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

/**
 * Main entry point for the Edge Computing Task Offloading simulation.
 * This simulation implements a proof-of-concept for the paper:
 * "Deep Reinforcement Learning for Task Offloading in Mobile Edge Computing" 
 * by Songtao Wang et al., IEEE INFOCOM 2020.
 */
public class Main {
    
    public static void main(String[] args) {
        System.out.println("Edge Computing Task Offloading Simulation");
        System.out.println("=========================================");
        
        // Load configuration
        Properties config;
        if (args.length > 0) {
            config = ConfigUtils.loadConfig(args[0]);
        } else {
            // Load from default location in resources
            try {
                config = ConfigUtils.loadConfig("config.properties");
            } catch (Exception e) {
                config = new Properties();
                System.out.println("Using default configuration");
            }
        }
        
        // Create results directory
        File resultsDir = new File("results");
        if (!resultsDir.exists()) {
            resultsDir.mkdir();
        }
        
        // Initialize simulation
        OffloadingSimulation simulation = new OffloadingSimulation(config);
        
        // Run training phase
        int trainingEpisodes = Integer.parseInt(
            config.getProperty("training_episodes", "1000")
        );
        simulation.runTrainingPhase(trainingEpisodes);
        
        // Save the trained model
        simulation.saveModel("results/drl_model.zip");
        
        // Run evaluation phase
        simulation.runEvaluationPhase();
        
        // Get metrics and generate reports
        PerformanceMetrics metrics = simulation.getMetrics();
        String report = metrics.generateSummaryReport();
        
        // Save report to file
        try (FileWriter writer = new FileWriter("results/simulation_report.txt")) {
            writer.write(report);
            System.out.println("\nSimulation report saved to results/simulation_report.txt");
        } catch (IOException e) {
            System.err.println("Error writing report: " + e.getMessage());
        }
        
        // Generate visualization charts
        generateCharts(metrics);
        
        System.out.println("\nSimulation completed successfully.");
    }
    
    /**
     * Generate visualization charts from the simulation metrics
     */
    private static void generateCharts(PerformanceMetrics metrics) {
        try {
            // Create task distribution chart
            DefaultCategoryDataset taskDistDataset = new DefaultCategoryDataset();
            taskDistDataset.addValue(metrics.getLocalExecutionCount(), "Tasks", "Local");
            taskDistDataset.addValue(metrics.getEdgeExecutionCount(), "Tasks", "Edge");
            taskDistDataset.addValue(metrics.getCloudExecutionCount(), "Tasks", "Cloud");
            
            JFreeChart taskDistChart = ChartFactory.createBarChart(
                "Task Execution Distribution", 
                "Execution Location", 
                "Number of Tasks", 
                taskDistDataset,
                PlotOrientation.VERTICAL,
                true, true, false
            );
            
            ChartUtils.saveChartAsPNG(
                new File("results/task_distribution.png"), 
                taskDistChart, 
                800, 600
            );
            System.out.println("Task distribution chart saved to results/task_distribution.png");
            
            // Create performance comparison chart (latency)
            DefaultCategoryDataset perfDataset = new DefaultCategoryDataset();
            perfDataset.addValue(metrics.getAverageLatency(), "Avg Latency (ms)", "DRL");
            perfDataset.addValue(metrics.getLatencyPercentile(95), "95th Percentile Latency (ms)", "DRL");
            
            JFreeChart perfChart = ChartFactory.createBarChart(
                "Task Completion Latency", 
                "Metric", 
                "Latency (ms)", 
                perfDataset,
                PlotOrientation.VERTICAL,
                true, true, false
            );
            
            ChartUtils.saveChartAsPNG(
                new File("results/latency_metrics.png"), 
                perfChart, 
                800, 600
            );
            System.out.println("Latency metrics chart saved to results/latency_metrics.png");
            
            // Create energy consumption chart
            DefaultCategoryDataset energyDataset = new DefaultCategoryDataset();
            energyDataset.addValue(metrics.getTotalEnergy(), "Total Energy (J)", "Simulation");
            energyDataset.addValue(metrics.getAverageEnergy() * metrics.getTotalTasks(), "Estimated Energy without Offloading", "Simulation");
            
            JFreeChart energyChart = ChartFactory.createBarChart(
                "Energy Consumption Comparison", 
                "", 
                "Energy (Joules)", 
                energyDataset,
                PlotOrientation.VERTICAL,
                true, true, false
            );
            
            ChartUtils.saveChartAsPNG(
                new File("results/energy_consumption.png"), 
                energyChart, 
                800, 600
            );
            System.out.println("Energy consumption chart saved to results/energy_consumption.png");
            
        } catch (IOException e) {
            System.err.println("Error generating charts: " + e.getMessage());
        }
    }
}
