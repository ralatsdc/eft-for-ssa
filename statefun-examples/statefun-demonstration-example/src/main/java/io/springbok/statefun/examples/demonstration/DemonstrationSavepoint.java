package io.springbok.statefun.examples.demonstration;

import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.statefun.flink.state.processor.StatefulFunctionsSavepointCreator;
import org.apache.flink.statefun.sdk.FunctionType;

public class DemonstrationSavepoint {

  private void configureSavepoint() throws Exception {

    int maxParallelism = 128;
    StatefulFunctionsSavepointCreator newSavepoint =
        new StatefulFunctionsSavepointCreator(maxParallelism);
    // Read data from a file, database, or other location
    final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

    // Register the dataset with a router
    newSavepoint.withBootstrapData(userSeenCounts, MyStateBootstrapFunctionRouter::new);

    // Register a bootstrap function to process the records
    newSavepoint.withStateBootstrapFunctionProvider(
        new FunctionType("eft-for-ssa", "orbit-id-manager"),
        ignored -> new OrbitIdManagerBootstrap());

    newSavepoint.write("file:///savepoint/path/");

    env.execute();
  }
}
