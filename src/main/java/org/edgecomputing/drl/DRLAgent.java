package org.edgecomputing.drl;

import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.util.*;

/**
 * Deep Reinforcement Learning Agent for task offloading decisions.
 * Implements DQN (Deep Q-Network) algorithm.
 */
public class DRLAgent {
    // Constructor with simplified parameters for quick initialization
    public DRLAgent(OffloadingEnvironment environment, int numDevices, int actionSpace) {
        this(environment, 0.001, 0.95, 0.1, 32, 1000, 100);
    }
    private final OffloadingEnvironment environment;
    private final MultiLayerNetwork qNetwork;      // Main Q-network
    private final MultiLayerNetwork targetNetwork; // Target Q-network for stable learning
    
    private final double learningRate;
    private final double discountFactor;
    private final double explorationRate;
    private final int batchSize;
    
    private final int stateSize;
    private final int actionSize;
    
    // Experience replay buffer
    private final List<Experience> replayBuffer;
    private final int replayBufferSize;
    private final Random random;
    
    // Training statistics
    private double totalReward;
    private int totalSteps;
    private int updateCounter;
    private final int targetUpdateFrequency;
    
    public DRLAgent(OffloadingEnvironment environment, double learningRate, double discountFactor, 
                   double explorationRate, int batchSize, int replayBufferSize, int targetUpdateFrequency) {
        this.environment = environment;
        this.learningRate = learningRate;
        this.discountFactor = discountFactor;
        this.explorationRate = explorationRate;
        this.batchSize = batchSize;
        this.replayBufferSize = replayBufferSize;
        this.targetUpdateFrequency = targetUpdateFrequency;
        
        this.stateSize = environment.getStateSpace();
        this.actionSize = environment.getActionSpace();
        this.replayBuffer = new ArrayList<>(replayBufferSize);
        this.random = new Random(42); // Fixed seed for reproducibility
        
        this.totalReward = 0.0;
        this.totalSteps = 0;
        this.updateCounter = 0;
        
        // Initialize Q-network and target network
        this.qNetwork = buildNetwork();
        this.targetNetwork = buildNetwork();
        
        // Initialize target network with same parameters
        this.targetNetwork.setParameters(this.qNetwork.params());
    }
    
    /**
     * Build the neural network architecture for the Q-network
     */
    private MultiLayerNetwork buildNetwork() {
        MultiLayerConfiguration config = new NeuralNetConfiguration.Builder()
            .seed(123)
            .weightInit(WeightInit.XAVIER)
            .updater(new Adam(learningRate))
            .l2(0.001)
            .list()
            .layer(0, new DenseLayer.Builder()
                .nIn(stateSize)
                .nOut(64)
                .activation(Activation.RELU)
                .build())
            .layer(1, new DenseLayer.Builder()
                .nIn(64)
                .nOut(32)
                .activation(Activation.RELU)
                .build())
            .layer(2, new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                .nIn(32)
                .nOut(actionSize)
                .activation(Activation.IDENTITY)
                .build())
            .build();
        
        MultiLayerNetwork network = new MultiLayerNetwork(config);
        network.init();
        network.setListeners(new ScoreIterationListener(100));
        
        return network;
    }
    
    /**
     * Choose an action using epsilon-greedy policy
     */
    public int chooseAction(double[] state) {
        // Epsilon-greedy exploration
        if (random.nextDouble() < explorationRate) {
            return random.nextInt(actionSize);
        } else {
            return getBestAction(state);
        }
    }
    
    /**
     * Get the best action for a given state
     */
    public int getBestAction(double[] state) {
        // Convert state to ND4J array
        INDArray stateArray = Nd4j.create(state).reshape(1, stateSize);
        
        // Get Q-values for all actions
        INDArray qValues = qNetwork.output(stateArray);
        
        // Return the action with maximum Q-value
        return qValues.argMax(1).getInt(0);
    }
    
    /**
     * Store experience in replay buffer
     */
    public void storeExperience(double[] state, int action, double reward, double[] nextState, boolean done) {
        Experience experience = new Experience(state, action, reward, nextState, done);
        
        if (replayBuffer.size() >= replayBufferSize) {
            replayBuffer.remove(0); // Remove oldest experience if buffer full
        }
        
        replayBuffer.add(experience);
        
        // Update statistics
        totalReward += reward;
        totalSteps++;
    }
    
    /**
     * Train the Q-network using experience replay
     */
    public void trainNetwork() {
        // Check if we have enough experiences to sample a batch
        if (replayBuffer.size() < batchSize) {
            return;
        }
        
        // Sample random batch from replay buffer
        List<Experience> batch = sampleBatch();
        
        // Create input and target arrays
        INDArray inputStates = Nd4j.create(batchSize, stateSize);
        INDArray actions = Nd4j.create(batchSize);
        INDArray rewards = Nd4j.create(batchSize);
        INDArray nextStates = Nd4j.create(batchSize, stateSize);
        
        // Fill arrays with batch data
        for (int i = 0; i < batchSize; i++) {
            Experience exp = batch.get(i);
            inputStates.putRow(i, Nd4j.create(exp.getState()));
            actions.putScalar(i, exp.getAction());
            rewards.putScalar(i, exp.getReward());
            nextStates.putRow(i, Nd4j.create(exp.getNextState()));
        }
        
        // Get current Q-values for all actions
        INDArray currentQValues = qNetwork.output(inputStates);
        
        // Get next Q-values from target network
        INDArray nextQValues = targetNetwork.output(nextStates);
        
        // Max Q-value for next state
        INDArray maxNextQ = nextQValues.max(1);
        
        // Create target Q-values (y_j = r_j + gamma * max_a' Q(s',a'))
        INDArray targetQValues = currentQValues.dup();
        for (int i = 0; i < batchSize; i++) {
            int action = actions.getInt(i);
            double targetValue = rewards.getDouble(i) + discountFactor * maxNextQ.getDouble(i);
            targetQValues.putScalar(i, action, targetValue);
        }
        
        // Train the network
        qNetwork.fit(inputStates, targetQValues);
        
        // Update target network periodically
        updateCounter++;
        if (updateCounter % targetUpdateFrequency == 0) {
            targetNetwork.setParameters(qNetwork.params());
            System.out.println("Target network updated at step " + totalSteps);
        }
    }
    
    /**
     * Sample a batch of experiences from the replay buffer
     */
    private List<Experience> sampleBatch() {
        List<Experience> batch = new ArrayList<>(batchSize);
        int bufferSize = replayBuffer.size();
        
        // Sample random indices without replacement
        Set<Integer> indices = new HashSet<>();
        while (indices.size() < batchSize) {
            indices.add(random.nextInt(bufferSize));
        }
        
        // Get the corresponding experiences
        for (int idx : indices) {
            batch.add(replayBuffer.get(idx));
        }
        
        return batch;
    }
    
    /**
     * Train the network using stored experiences
     */
    public void trainFromExperience() {
        // Only train if we have enough experiences in the buffer
        if (replayBuffer.size() >= batchSize) {
            trainNetwork();
        }
    }
    
    /**
     * Save the trained model
     */
    public void saveModel(String filePath) {
        try {
            java.io.File file = new java.io.File(filePath);
            qNetwork.save(file);
            System.out.println("Model saved to " + filePath);
        } catch (Exception e) {
            System.err.println("Error saving model: " + e.getMessage());
        }
    }
    
    /**
     * Load a pre-trained model
     */
    public void loadModel(String filePath) {
        try {
            java.io.File file = new java.io.File(filePath);
            MultiLayerNetwork loadedModel = MultiLayerNetwork.load(file, true);
            qNetwork.setParameters(loadedModel.params());
            targetNetwork.setParameters(loadedModel.params());
            System.out.println("Model loaded from " + filePath);
        } catch (Exception e) {
            System.err.println("Error loading model: " + e.getMessage());
        }
    }
    
    // Inner class to represent a single experience tuple (s,a,r,s',done)
    private static class Experience {
        private final double[] state;
        private final int action;
        private final double reward;
        private final double[] nextState;
        private final boolean done;
        
        public Experience(double[] state, int action, double reward, double[] nextState, boolean done) {
            this.state = state;
            this.action = action;
            this.reward = reward;
            this.nextState = nextState;
            this.done = done;
        }
        
        public double[] getState() {
            return state;
        }
        
        public int getAction() {
            return action;
        }
        
        public double getReward() {
            return reward;
        }
        
        public double[] getNextState() {
            return nextState;
        }
        
        public boolean isDone() {
            return done;
        }
    }
    
    // Getters for statistics
    public double getTotalReward() {
        return totalReward;
    }
    
    public int getTotalSteps() {
        return totalSteps;
    }
    
    public double getAverageReward() {
        return totalSteps > 0 ? totalReward / totalSteps : 0;
    }
}
