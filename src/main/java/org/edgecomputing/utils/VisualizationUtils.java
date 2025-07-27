package org.edgecomputing.utils;

import org.edgecomputing.metrics.PerformanceMetrics;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Utility class for visualizing simulation results and metrics.
 */
public class VisualizationUtils {

    /**
     * Generate all charts and reports from the simulation results
     * @param metrics The collected performance metrics
     * @param outputDir Directory to save the output files
     */
    public static void generateResultsOutput(PerformanceMetrics metrics, String outputDir) {
        // Create output directory if it doesn't exist
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        // Generate textual report
        generateReport(metrics, outputDir + "/simulation_report.txt");
        
        // Generate charts
        generateTaskDistributionChart(metrics, outputDir + "/task_distribution.png");
        generateLatencyChart(metrics, outputDir + "/latency_metrics.png");
        generateEnergyChart(metrics, outputDir + "/energy_consumption.png");
        generateComparisonChart(metrics, outputDir + "/performance_comparison.png");
    }
    
    /**
     * Generate a text report with the simulation results
     */
    private static void generateReport(PerformanceMetrics metrics, String filePath) {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(metrics.generateSummaryReport());
            System.out.println("Report saved to " + filePath);
        } catch (IOException e) {
            System.err.println("Error writing report: " + e.getMessage());
        }
    }
    
    /**
     * Generate a bar chart showing the task execution distribution
     */
    private static void generateTaskDistributionChart(PerformanceMetrics metrics, String filePath) {
        try {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            
            // Add data points
            dataset.addValue(metrics.getLocalExecutionCount(), "Task Count", "Local Execution");
            dataset.addValue(metrics.getEdgeExecutionCount(), "Task Count", "Edge Execution");
            dataset.addValue(metrics.getCloudExecutionCount(), "Task Count", "Cloud Execution");
            
            // Create chart
            JFreeChart chart = ChartFactory.createBarChart(
                "Task Offloading Distribution",
                "Execution Location",
                "Number of Tasks",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false
            );
            
            // Save chart to file
            ChartUtils.saveChartAsPNG(new File(filePath), chart, 800, 600);
            System.out.println("Task distribution chart saved to " + filePath);
        } catch (IOException e) {
            System.err.println("Error generating task distribution chart: " + e.getMessage());
        }
    }
    
    /**
     * Generate a bar chart showing latency metrics
     */
    private static void generateLatencyChart(PerformanceMetrics metrics, String filePath) {
        try {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            
            // Add data points
            dataset.addValue(metrics.getAverageLatency(), "Latency (ms)", "Average");
            dataset.addValue(metrics.getLatencyPercentile(95), "Latency (ms)", "95th Percentile");
            dataset.addValue(metrics.getLatencyPercentile(99), "Latency (ms)", "99th Percentile");
            
            // Create chart
            JFreeChart chart = ChartFactory.createBarChart(
                "Task Completion Latency",
                "Metric",
                "Latency (ms)",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false
            );
            
            // Save chart to file
            ChartUtils.saveChartAsPNG(new File(filePath), chart, 800, 600);
            System.out.println("Latency chart saved to " + filePath);
        } catch (IOException e) {
            System.err.println("Error generating latency chart: " + e.getMessage());
        }
    }
    
    /**
     * Generate a bar chart showing energy consumption metrics
     */
    private static void generateEnergyChart(PerformanceMetrics metrics, String filePath) {
        try {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            
            // Add data points
            dataset.addValue(metrics.getAverageEnergy(), "Energy (J)", "Average per Task");
            
            // Estimate energy if all tasks were executed locally (assuming 2x energy)
            double estimatedLocalEnergy = metrics.getAverageEnergy() * 2.0;
            dataset.addValue(estimatedLocalEnergy, "Energy (J)", "Est. Local Execution");
            
            // Calculate total energy saved
            double totalEnergySaved = (estimatedLocalEnergy - metrics.getAverageEnergy()) * metrics.getTotalTasks();
            dataset.addValue(totalEnergySaved, "Energy (J)", "Total Energy Saved");
            
            // Create chart
            JFreeChart chart = ChartFactory.createBarChart(
                "Energy Consumption Analysis",
                "Metric",
                "Energy (Joules)",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false
            );
            
            // Save chart to file
            ChartUtils.saveChartAsPNG(new File(filePath), chart, 800, 600);
            System.out.println("Energy chart saved to " + filePath);
        } catch (IOException e) {
            System.err.println("Error generating energy chart: " + e.getMessage());
        }
    }
    
    /**
     * Generate a comparison chart of different metrics
     */
    private static void generateComparisonChart(PerformanceMetrics metrics, String filePath) {
        try {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            
            // Add performance indicators (normalized for comparison)
            dataset.addValue(metrics.getSuccessRate() * 100, "Success Rate (%)", "DRL Approach");
            dataset.addValue(metrics.getEdgeExecutionPercentage() * 100, "Edge Utilization (%)", "DRL Approach");
            dataset.addValue((metrics.getAverageLatency() / 1000), "Avg. Latency (s)", "DRL Approach");
            dataset.addValue(metrics.getAverageNetworkUsage() / 100, "Avg. Network Usage (100 KB)", "DRL Approach");
            
            // Create chart
            JFreeChart chart = ChartFactory.createBarChart(
                "Performance Metrics Comparison",
                "Metric",
                "Value",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false
            );
            
            // Save chart to file
            ChartUtils.saveChartAsPNG(new File(filePath), chart, 800, 600);
            System.out.println("Performance comparison chart saved to " + filePath);
        } catch (IOException e) {
            System.err.println("Error generating comparison chart: " + e.getMessage());
        }
    }
    
    /**
     * Generate strategy comparison visualizations between different offloading strategies
     * @param strategies Map of strategy names to their performance metrics
     * @param outputDir Directory to save visualization files
     */
    public static void generateStrategyVisualizations(Map<String, PerformanceMetrics> strategies, String outputDir) {
        try {
            // Create output directory if it doesn't exist
            File dir = new File(outputDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            // Generate success rate comparison
            generateSuccessRateComparison(strategies, outputDir + "/strategy_success_rate.png");
            
            // Generate latency comparison
            generateLatencyComparison(strategies, outputDir + "/strategy_latency.png");
            
            // Generate energy comparison
            generateEnergyComparison(strategies, outputDir + "/strategy_energy.png");
            
            // Generate execution distribution comparison
            generateLocationDistributionComparison(strategies, outputDir + "/strategy_distribution.png");
            
            // Generate CSV comparison
            generateComparisonCsv(strategies, outputDir + "/strategy_comparison.csv");
            
            System.out.println("Strategy comparison visualizations saved to " + outputDir);
        } catch (Exception e) {
            System.err.println("Error generating strategy visualizations: " + e.getMessage());
        }
    }
    
    /**
     * Generate success rate comparison chart for different strategies
     */
    private static void generateSuccessRateComparison(Map<String, PerformanceMetrics> strategies, String filePath) {
        try {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            
            // Add data points for each strategy
            for (Map.Entry<String, PerformanceMetrics> entry : strategies.entrySet()) {
                String strategy = entry.getKey();
                PerformanceMetrics metrics = entry.getValue();
                dataset.addValue(metrics.getSuccessRate() * 100, "Success Rate (%)", strategy);
            }
            
            // Create chart
            JFreeChart chart = ChartFactory.createBarChart(
                "Task Completion Success Rate by Strategy",
                "Strategy",
                "Success Rate (%)",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false
            );
            
            // Save chart to file
            ChartUtils.saveChartAsPNG(new File(filePath), chart, 800, 600);
        } catch (IOException e) {
            System.err.println("Error generating success rate comparison chart: " + e.getMessage());
        }
    }
    
    /**
     * Generate latency comparison chart for different strategies
     */
    private static void generateLatencyComparison(Map<String, PerformanceMetrics> strategies, String filePath) {
        try {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            
            // Add data points for each strategy
            for (Map.Entry<String, PerformanceMetrics> entry : strategies.entrySet()) {
                String strategy = entry.getKey();
                PerformanceMetrics metrics = entry.getValue();
                dataset.addValue(metrics.getAverageLatency(), "Average Latency (ms)", strategy);
                dataset.addValue(metrics.getLatencyPercentile(95), "95th Percentile Latency (ms)", strategy);
            }
            
            // Create chart
            JFreeChart chart = ChartFactory.createBarChart(
                "Task Completion Latency by Strategy",
                "Strategy",
                "Latency (ms)",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false
            );
            
            // Save chart to file
            ChartUtils.saveChartAsPNG(new File(filePath), chart, 800, 600);
        } catch (IOException e) {
            System.err.println("Error generating latency comparison chart: " + e.getMessage());
        }
    }
    
    /**
     * Generate energy consumption comparison chart for different strategies
     */
    private static void generateEnergyComparison(Map<String, PerformanceMetrics> strategies, String filePath) {
        try {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            
            // Add data points for each strategy
            for (Map.Entry<String, PerformanceMetrics> entry : strategies.entrySet()) {
                String strategy = entry.getKey();
                PerformanceMetrics metrics = entry.getValue();
                dataset.addValue(metrics.getAverageEnergy(), "Average Energy (J)", strategy);
                dataset.addValue(metrics.getTotalEnergy(), "Total Energy (J)", strategy);
            }
            
            // Create chart
            JFreeChart chart = ChartFactory.createBarChart(
                "Energy Consumption by Strategy",
                "Strategy",
                "Energy (Joules)",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false
            );
            
            // Save chart to file
            ChartUtils.saveChartAsPNG(new File(filePath), chart, 800, 600);
        } catch (IOException e) {
            System.err.println("Error generating energy comparison chart: " + e.getMessage());
        }
    }
    
    /**
     * Generate execution location distribution comparison for different strategies
     */
    private static void generateLocationDistributionComparison(Map<String, PerformanceMetrics> strategies, String filePath) {
        try {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            
            // Add data points for each strategy
            for (Map.Entry<String, PerformanceMetrics> entry : strategies.entrySet()) {
                String strategy = entry.getKey();
                PerformanceMetrics metrics = entry.getValue();
                dataset.addValue(metrics.getLocalExecutionPercentage() * 100, "Local (%)", strategy);
                dataset.addValue(metrics.getEdgeExecutionPercentage() * 100, "Edge (%)", strategy);
                dataset.addValue(metrics.getCloudExecutionPercentage() * 100, "Cloud (%)", strategy);
            }
            
            // Create chart
            JFreeChart chart = ChartFactory.createBarChart(
                "Execution Location Distribution by Strategy",
                "Strategy",
                "Percentage (%)",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false
            );
            
            // Save chart to file
            ChartUtils.saveChartAsPNG(new File(filePath), chart, 800, 600);
        } catch (IOException e) {
            System.err.println("Error generating location distribution comparison chart: " + e.getMessage());
        }
    }
    
    /**
     * Generate CSV comparison for different strategies
     */
    private static void generateComparisonCsv(Map<String, PerformanceMetrics> strategies, String filePath) {
        try (FileWriter writer = new FileWriter(filePath)) {
            // Write CSV header with metrics and strategy names
            writer.write("Metric");
            for (String strategy : strategies.keySet()) {
                writer.write("," + strategy);
            }
            writer.write("\n");
            
            // Define metrics to compare
            String[] metricNames = {
                "Task Count", "Success Rate (%)", "Average Latency (ms)", "95th Percentile Latency (ms)",
                "Average Energy (J)", "Local Execution (%)", "Edge Execution (%)", "Cloud Execution (%)",
                "Network Usage (KB)"
            };
            
            // Write each metric row
            for (String metric : metricNames) {
                writer.write(metric);
                
                for (PerformanceMetrics metrics : strategies.values()) {
                    double value = 0;
                    switch (metric) {
                        case "Task Count":
                            value = metrics.getTotalTasks();
                            break;
                        case "Success Rate (%)":
                            value = metrics.getSuccessRate() * 100;
                            break;
                        case "Average Latency (ms)":
                            value = metrics.getAverageLatency();
                            break;
                        case "95th Percentile Latency (ms)":
                            value = metrics.getLatencyPercentile(95);
                            break;
                        case "Average Energy (J)":
                            value = metrics.getAverageEnergy();
                            break;
                        case "Local Execution (%)":
                            value = metrics.getLocalExecutionPercentage() * 100;
                            break;
                        case "Edge Execution (%)":
                            value = metrics.getEdgeExecutionPercentage() * 100;
                            break;
                        case "Cloud Execution (%)":
                            value = metrics.getCloudExecutionPercentage() * 100;
                            break;
                        case "Network Usage (KB)":
                            value = metrics.getTotalNetworkUsage();
                            break;
                    }
                    writer.write("," + value);
                }
                writer.write("\n");
            }
        } catch (IOException e) {
            System.err.println("Error generating comparison CSV: " + e.getMessage());
        }
    }
    
    /**
     * Generate CSV output for the metrics
     */
    public static void exportToCsv(PerformanceMetrics metrics, String outputDir) {
        try (FileWriter writer = new FileWriter(outputDir + "/metrics_summary.csv")) {
            // CSV header
            writer.write("Metric,Value\n");
            
            // Write metrics
            writer.write("Total Tasks," + metrics.getTotalTasks() + "\n");
            writer.write("Successful Tasks," + metrics.getSuccessfulTasks() + "\n");
            writer.write("Failed Tasks," + metrics.getFailedTasks() + "\n");
            writer.write("Success Rate (%)," + (metrics.getSuccessRate() * 100) + "\n");
            writer.write("Local Execution Count," + metrics.getLocalExecutionCount() + "\n");
            writer.write("Edge Execution Count," + metrics.getEdgeExecutionCount() + "\n");
            writer.write("Cloud Execution Count," + metrics.getCloudExecutionCount() + "\n");
            writer.write("Local Execution (%)," + (metrics.getLocalExecutionPercentage() * 100) + "\n");
            writer.write("Edge Execution (%)," + (metrics.getEdgeExecutionPercentage() * 100) + "\n");
            writer.write("Cloud Execution (%)," + (metrics.getCloudExecutionPercentage() * 100) + "\n");
            writer.write("Average Latency (ms)," + metrics.getAverageLatency() + "\n");
            writer.write("95th Percentile Latency (ms)," + metrics.getLatencyPercentile(95) + "\n");
            writer.write("Average Energy (J)," + metrics.getAverageEnergy() + "\n");
            writer.write("Total Network Usage (KB)," + metrics.getTotalNetworkUsage() + "\n");
            writer.write("Average Network Usage per Offloaded Task (KB)," + metrics.getAverageNetworkUsage() + "\n");
            
            System.out.println("Metrics exported to CSV: " + outputDir + "/metrics_summary.csv");
        } catch (IOException e) {
            System.err.println("Error exporting metrics to CSV: " + e.getMessage());
        }
    }
}
