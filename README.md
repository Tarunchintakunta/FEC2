# Edge Computing Task Offloading POC

This project implements a proof-of-concept prototype for task offloading in Fog/Edge computing environments, focusing on IoT devices. It demonstrates the application of Deep Reinforcement Learning (DRL) to make intelligent offloading decisions that optimize latency, energy consumption, and resource utilization.

## Research Background

This implementation is based on the paper "Deep Reinforcement Learning for Task Offloading in Mobile Edge Computing" (IEEE INFOCOM 2020) by Wang et al. The paper proposes a DRL approach for optimizing task offloading decisions in mobile edge computing environments, considering multiple factors including latency, energy consumption, and computing resource allocation.

## System Architecture

The system architecture consists of three layers:

1. **IoT Device Layer**: Mobile or IoT devices with limited computing capacity, energy constraints, and wireless connectivity. These devices generate computational tasks that can either be executed locally or offloaded.

2. **Edge Server Layer**: Edge servers positioned at the network edge (e.g., base stations, access points) that provide computing resources with lower latency than cloud servers. They have limited coverage areas and resource capacity.

3. **Cloud Layer**: Cloud datacenter with abundant computing resources but higher access latency than edge servers. Provides a fallback for computationally intensive tasks or when edge resources are overloaded.

## Key Components

### Core Model Components
- `IoTDevice`: Represents devices with local computing capabilities, energy models, and mobility
- `EdgeServer`: Models edge servers with computing capacity, coverage areas, and dynamic workload
- `CloudDatacenter`: Simulates cloud resources with high computing power
- `IoTTask`: Represents computational tasks with attributes like input/output size and deadlines
- `NetworkModel`: Models network connectivity, bandwidth, and latency between layers
- `MobilityModel`: Implements device mobility patterns (random walk, waypoint, etc.)

### DRL Components
- `OffloadingEnvironment`: Defines the environment state, actions, and rewards for the DRL agent
- `DRLAgent`: Implements Deep Q-Network with experience replay for task offloading decisions
- `BaselineStrategy`: Implements alternative strategies for comparison (local-only, edge-only, etc.)

### Simulation Components
- `TaskGenerator`: Creates realistic workload patterns for the simulation
- `OffloadingSimulation`: Orchestrates the simulation with CloudSim Plus integration
- `SimulationController`: Controls simulation execution and parameter settings
- `PerformanceMetrics`: Collects and analyzes metrics like latency, energy, and success rate
- `VisualizationUtils`: Generates visual representations of simulation results

## Requirements

- Java 11 or higher
- Maven 3.6 or higher
- Minimum 4GB RAM recommended for running the simulation

## Libraries Used

- CloudSim Plus 7.3.0: For cloud/edge computing simulation
- Deeplearning4j 1.0.0-M2.1: For neural network implementation
- ND4J: For n-dimensional arrays and mathematical operations
- SLF4J & Logback: For logging
- JFreeChart: For visualization of results

## Build and Run

### Building the Project

```bash
mvn clean package
```

### Running the Simulation

```bash
java -jar target/edge-computing-task-offloading-poc-1.0-SNAPSHOT.jar
```

### Command-line Options

The simulation supports different execution modes:

```bash
java -jar target/edge-computing-task-offloading-poc-1.0-SNAPSHOT.jar [MODE] [CONFIG_PATH]
```

Available modes:
- `simulation` - Run the main simulation (default)
- `experiment` - Run comparison experiments between DRL and baseline strategies
- `test` - Run the test suite
- `help` - Show help information

Examples:
```bash
# Run with default configuration
java -jar target/edge-computing-task-offloading-poc-1.0-SNAPSHOT.jar

# Run experiment mode with custom configuration
java -jar target/edge-computing-task-offloading-poc-1.0-SNAPSHOT.jar experiment custom-config.properties

# Run tests
java -jar target/edge-computing-task-offloading-poc-1.0-SNAPSHOT.jar test
```

## Configuration

The simulation parameters can be customized in the `config.properties` file. Key parameters include:

- Number of IoT devices, edge servers, and cloud datacenters
- Computing capacities (MIPS), memory, and storage
- Network bandwidths and latencies
- Task characteristics and generation patterns
- Energy consumption models
- DRL hyperparameters
- Simulation time and random seed

## Output and Results

Simulation results are saved to the `results` directory by default, including:
- Performance metrics in CSV format
- Visualization charts for latency, energy consumption, and task distribution
- Trained DRL model (can be used for subsequent simulations)
- Comparison reports when running in experiment mode

## Future Enhancements

- Heterogeneous device and server specifications
- More complex mobility models
- Dynamic network conditions
- Real-time visualization of simulation progress
- Integration with real-world IoT testbeds

## Project Structure
- `src/main/java/org/edgecomputing/`: Source code directory
  - `model/`: Contains entity models (IoT devices, tasks, edge nodes)
  - `drl/`: Deep reinforcement learning implementation
  - `simulation/`: CloudSim Plus based simulation environment
  - `metrics/`: Performance metric collection and analysis
  - `utils/`: Utility classes
  - `Main.java`: Entry point for the simulation

## System Architecture
The system consists of three layers:
1. **IoT Device Layer**: Mobile devices generating computational tasks
2. **Edge Layer**: Edge servers that can process offloaded tasks
3. **Cloud Layer**: Cloud data centers for processing tasks that exceed edge capabilities

Tasks can be executed locally on the device, offloaded to nearby edge servers, or sent to the cloud. The DRL algorithm makes intelligent offloading decisions based on:
- Current network conditions
- Edge server load
- Task computational requirements
- Energy constraints

## Requirements
- Java 11+
- Maven 3.6+

## Building and Running the Simulation
1. Clone this repository
2. Build the project: `mvn clean package`
3. Run the simulation: `java -jar target/edge-computing-task-offloading-poc-1.0-SNAPSHOT-jar-with-dependencies.jar`

## Simulation Parameters
You can modify the simulation parameters in the `config.properties` file:
- Number of IoT devices
- Number of edge servers
- Network conditions
- Task generation rates
- DRL hyperparameters

## Results Analysis
Simulation results will be generated in the `results/` directory, including:
- CSV files with raw performance data
- Generated charts for visualization
- Summary report of key metrics
# Edge-computing_task_offloading
