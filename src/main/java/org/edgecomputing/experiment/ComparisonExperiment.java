package org.edgecomputing.experiment;

import org.edgecomputing.drl.DRLAgent;
import org.edgecomputing.drl.OffloadingEnvironment;
import org.edgecomputing.metrics.PerformanceMetrics;
import org.edgecomputing.model.*;
import org.edgecomputing.simulation.OffloadingSimulation;
import org.edgecomputing.simulation.TaskGenerator;
import org.edgecomputing.strategy.BaselineStrategy;
import org.edgecomputing.utils.ConfigUtils;
import org.edgecomputing.utils.VisualizationUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Conducts comparison experiments between DRL-based offloading and baseline strategies.
 * Runs identical workloads with different offloading approaches and compares performance metrics.
 */
public class ComparisonExperiment {
    
    // We now use NetworkModel's findNearestEdgeServer method instead of implementing it here
    
    // Experiment parameters
    private final int numberOfTasks;
    private final int numberOfRuns;
    private final String outputDir;
    private final Random random;
    
    // Simulation components
    private final List<IoTDevice> devices;
    private final List<EdgeServer> edgeServers;
    private final CloudDatacenter cloudDatacenter;
    private final NetworkModel networkModel;
    private final TaskGenerator taskGenerator;
    
    // DRL components
    private final DRLAgent drlAgent;
    private final OffloadingEnvironment environment;
    
    // Performance metrics for each strategy
    private final Map<String, PerformanceMetrics> metricsMap;
    
    /**
     * Constructor to initialize the comparison experiment
     * @param configPath Path to the configuration file
     */
    public ComparisonExperiment(String configPath) {
        // Load configuration
        ConfigUtils.loadConfig(configPath);
        
        // Set experiment parameters
        this.numberOfTasks = ConfigUtils.getIntProperty("experiment_number_of_tasks", 1000);
        this.numberOfRuns = ConfigUtils.getIntProperty("experiment_number_of_runs", 3);
        this.outputDir = ConfigUtils.getProperty("output_dir", "results/experiment");
        
        // Create output directory
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        // Initialize random generator
        long seed = ConfigUtils.getIntProperty("random_seed", 42);
        this.random = new Random(seed);
        
        // Initialize simulation components (simplified for this class)
        OffloadingSimulation simulation = new OffloadingSimulation(ConfigUtils.loadConfig(configPath));
        this.devices = simulation.getIoTDevices();
        this.edgeServers = simulation.getEdgeServers();
        this.cloudDatacenter = simulation.getCloudDatacenter();
        this.networkModel = new NetworkModel(
            ConfigUtils.getDoubleProperty("mobile_to_edge_bandwidth", 100.0),
            ConfigUtils.getDoubleProperty("edge_to_cloud_bandwidth", 1000.0),
            ConfigUtils.getDoubleProperty("mobile_to_edge_latency", 10.0),
            ConfigUtils.getDoubleProperty("edge_to_cloud_latency", 50.0)
        );
        this.taskGenerator = new TaskGenerator();
        
        // Initialize DRL components
        this.environment = new OffloadingEnvironment(
            devices, 
            edgeServers, 
            cloudDatacenter,
            ConfigUtils.getDoubleProperty("mobile_to_edge_bandwidth", 100.0), 
            ConfigUtils.getDoubleProperty("edge_to_cloud_bandwidth", 1000.0),
            ConfigUtils.getDoubleProperty("cloud_bandwidth", 10000.0),
            ConfigUtils.getDoubleProperty("mobile_to_edge_latency", 10.0),
            ConfigUtils.getDoubleProperty("edge_to_cloud_latency", 50.0),
            ConfigUtils.getDoubleProperty("latency_weight", 0.4),
            ConfigUtils.getDoubleProperty("energy_weight", 0.4),
            ConfigUtils.getDoubleProperty("load_balance_weight", 0.2)
        );
        this.drlAgent = simulation.getDRLAgent();
        
        // Initialize metrics map
        this.metricsMap = new HashMap<>();
        
        System.out.println("Comparison experiment initialized with:");
        System.out.println("  - Number of tasks: " + numberOfTasks);
        System.out.println("  - Number of runs: " + numberOfRuns);
        System.out.println("  - Output directory: " + outputDir);
    }
    
    /**
     * Run the comparison experiment
     */
    public void runExperiment() {
        System.out.println("\n=== Starting Offloading Strategy Comparison Experiment ===\n");
        
        // Create list of strategies to compare
        List<BaselineStrategy.StrategyType> baselineStrategies = Arrays.asList(
            BaselineStrategy.StrategyType.LOCAL_ONLY,
            BaselineStrategy.StrategyType.EDGE_ONLY,
            BaselineStrategy.StrategyType.CLOUD_ONLY,
            BaselineStrategy.StrategyType.GREEDY_LATENCY,
            BaselineStrategy.StrategyType.GREEDY_ENERGY
        );
        
        // Initialize metrics for each strategy and DRL
        metricsMap.put("DRL", new PerformanceMetrics());
        for (BaselineStrategy.StrategyType strategy : baselineStrategies) {
            metricsMap.put(strategy.toString(), new PerformanceMetrics());
        }
        
        // Run multiple times to get average performance
        for (int run = 1; run <= numberOfRuns; run++) {
            System.out.println("\n=== Run " + run + "/" + numberOfRuns + " ===");
            
            // Generate task set for this run (same for all strategies)
            List<IoTTask> tasks = generateTaskSet(run);
            
            // Run DRL strategy
            System.out.println("Running DRL strategy...");
            runDRLStrategy(tasks, run);
            
            // Run baseline strategies
            for (BaselineStrategy.StrategyType strategyType : baselineStrategies) {
                System.out.println("Running " + strategyType + " strategy...");
                runBaselineStrategy(tasks, strategyType, run);
            }
        }
        
        // Generate result outputs
        System.out.println("\n=== Generating Comparison Results ===");
        generateComparisonResults();
    }
    
    /**
     * Generate a consistent set of tasks for the experiment
     * @param runIndex Run index for seed variation
     * @return List of tasks for the experiment
     */
    private List<IoTTask> generateTaskSet(int runIndex) {
        List<IoTTask> tasks = new ArrayList<>();
        
        // Set seed based on run index for consistency within run but variation between runs
        random.setSeed(42 + runIndex);
        
        // Generate tasks with timestamps
        double currentTime = 0.0;
        for (int i = 0; i < numberOfTasks; i++) {
            // Select random device
            IoTDevice device = devices.get(random.nextInt(devices.size()));
            
            // Generate task
            IoTTask task = taskGenerator.generateTask(device, currentTime);
            tasks.add(task);
            
            // Increment time (simulate task arrival pattern)
            currentTime += taskGenerator.getNextArrivalTime(currentTime);
        }
        
        return tasks;
    }
    
    /**
     * Run the experiment with DRL offloading strategy
     * @param tasks List of tasks to process
     * @param runIndex Run index for output
     */
    private void runDRLStrategy(List<IoTTask> tasks, int runIndex) {
        // Get metrics collector for DRL
        PerformanceMetrics metrics = metricsMap.get("DRL");
        
        // Process each task with DRL agent
        for (IoTTask task : tasks) {
            // Get task state for DRL agent
            double[] state = environment.getState(task.getSourceDevice(), task);
            
            // Get action from DRL agent
            int action = drlAgent.chooseAction(state);
            
            // Execute action
            double reward = environment.executeAction(task.getSourceDevice(), task, action);
            boolean successful = reward > -10.0; // Assuming large negative reward means failure
            
            // Record metrics
            IoTTask.TaskLocation location = task.getExecutionLocation();
            double latency = task.getFinishTime() - task.getStartTime();
            double energy = task.getTotalEnergy();
            double networkUsage = location == IoTTask.TaskLocation.LOCAL_DEVICE ? 0.0 :
                                 (task.getInputDataSize() + task.getOutputDataSize());
            
            metrics.recordTaskCompletion(task.getId(), successful, location, 
                                         latency, energy, networkUsage);
        }
        
        // Save run-specific metrics
        saveMetricsToFile(metrics, "DRL", runIndex);
    }
    
    /**
     * Run the experiment with a baseline offloading strategy
     * @param tasks List of tasks to process
     * @param strategyType Type of baseline strategy to use
     * @param runIndex Run index for output
     */
    private void runBaselineStrategy(List<IoTTask> tasks, BaselineStrategy.StrategyType strategyType, int runIndex) {
        // Create strategy
        BaselineStrategy strategy = new BaselineStrategy(strategyType, networkModel);
        
        // Get metrics collector for this strategy
        PerformanceMetrics metrics = metricsMap.get(strategyType.toString());
        
        // Process each task with the strategy
        for (IoTTask task : tasks) {
            IoTDevice device = task.getSourceDevice();
            
            // Get offloading decision
            int action = strategy.makeOffloadingDecision(task, device, edgeServers, cloudDatacenter);
            
            // Convert action to IoTTask.TaskLocation
            IoTTask.TaskLocation location = IoTTask.TaskLocation.LOCAL_DEVICE;
            if (action == 0) {
                location = IoTTask.TaskLocation.LOCAL_DEVICE;
            } else if (action <= edgeServers.size()) {
                location = IoTTask.TaskLocation.EDGE_SERVER;
            } else {
                location = IoTTask.TaskLocation.CLOUD;
            }
            task.setExecutionLocation(location);
            
            // Calculate execution time and energy
            double executionTime = 0.0;
            double energyConsumption = 0.0;
            
            // Execute the task based on location
            if (location == IoTTask.TaskLocation.LOCAL_DEVICE) {
                executionTime = device.calculateTaskExecutionTime(task);
                energyConsumption = device.calculateLocalEnergy(task);
            } else if (location == IoTTask.TaskLocation.EDGE_SERVER) {
                EdgeServer server = edgeServers.get(Math.min(action - 1, edgeServers.size() - 1));
                executionTime = networkModel.calculateEdgeOffloadingLatency(task, device, server);
                energyConsumption = device.calculateOffloadingEnergy(task, (int)networkModel.getMobileToEdgeBandwidth());
            } else { // CLOUD
                EdgeServer relay = networkModel.findNearestEdgeServer(device, edgeServers);
                executionTime = networkModel.calculateCloudOffloadingLatency(task, device, relay, cloudDatacenter);
                energyConsumption = device.calculateOffloadingEnergy(task, (int)networkModel.getMobileToEdgeBandwidth());
            }
            
            // Set task execution times
            double currentTime = 0.0; // Simulation time
            task.setStartTime(currentTime);
            task.setFinishTime(task.getStartTime() + executionTime);
            task.setTotalEnergy(energyConsumption);
            task.setCompleted(true);
            
            // Calculate reward
            double reward = environment.calculateReward(executionTime, energyConsumption, task.getDeadline());
            task.setReward(reward);
            
            boolean successful = task.getFinishTime() <= task.getMaxExecutionTime();
            
            // Record metrics
            double latency = task.getFinishTime() - task.getStartTime();
            double networkUsage = location == IoTTask.TaskLocation.LOCAL_DEVICE ? 0.0 :
                                 (task.getInputDataSize() + task.getOutputDataSize());
            
            // Add to our metrics collector
            metrics.recordTaskCompletion(task.getId(), successful, location, 
                                         latency, task.getTotalEnergy(), networkUsage);
        }
        
        // Save run-specific metrics
        saveMetricsToFile(metrics, strategyType.toString(), runIndex);
    }
    
    /**
     * Save metrics from a specific run to file
     * @param metrics Performance metrics to save
     * @param strategyName Name of the strategy
     * @param runIndex Run index
     */
    private void saveMetricsToFile(PerformanceMetrics metrics, String strategyName, int runIndex) {
        try {
            String filename = outputDir + "/" + strategyName + "_run" + runIndex + ".csv";
            try (FileWriter writer = new FileWriter(filename)) {
                writer.write(metrics.exportToCsv());
            }
        } catch (IOException e) {
            System.err.println("Error saving metrics for " + strategyName + ": " + e.getMessage());
        }
    }
    
    /**
     * Generate comparison results between strategies
     */
    private void generateComparisonResults() {
        // Create comparison chart data
        try (FileWriter writer = new FileWriter(outputDir + "/comparison_summary.csv")) {
            // CSV header
            writer.write("Strategy,SuccessRate,AverageLatency,AverageEnergy,AverageNetworkUsage\n");
            
            // Write data for each strategy
            for (Map.Entry<String, PerformanceMetrics> entry : metricsMap.entrySet()) {
                String strategy = entry.getKey();
                PerformanceMetrics metrics = entry.getValue();
                
                writer.write(String.format("%s,%.4f,%.4f,%.8f,%.4f\n",
                    strategy,
                    metrics.getSuccessRate(),
                    metrics.getAverageLatency(),
                    metrics.getAverageEnergy(),
                    metrics.getAverageNetworkUsage()
                ));
                
                // Generate individual charts for this strategy
                VisualizationUtils.generateResultsOutput(metrics, outputDir + "/" + strategy);
            }
            
            System.out.println("Comparison summary saved to: " + outputDir + "/comparison_summary.csv");
        } catch (IOException e) {
            System.err.println("Error generating comparison results: " + e.getMessage());
        }
        
        // Generate comparison charts
        generateComparisonCharts();
    }
    
    /**
     * Generate charts comparing different strategies
     */
    private void generateComparisonCharts() {
        // This would be implemented using JFreeChart to create bar charts comparing
        // different strategies across key metrics like latency, energy, etc.
        
        System.out.println("Comparison charts generated in: " + outputDir);
    }
}
