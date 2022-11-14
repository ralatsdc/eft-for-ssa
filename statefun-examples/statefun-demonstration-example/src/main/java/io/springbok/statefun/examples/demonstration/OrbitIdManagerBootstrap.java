package io.springbok.statefun.examples.demonstration;

import org.apache.flink.statefun.flink.state.processor.Context;
import org.apache.flink.statefun.flink.state.processor.StateBootstrapFunction;
import org.apache.flink.statefun.sdk.FunctionType;
import org.apache.flink.statefun.sdk.annotations.Persisted;
import org.apache.flink.statefun.sdk.state.PersistedValue;

import java.util.ArrayList;

// Each bootstrap functions instance directly corresponds to a StatefulFunction type. Likewise, each
// instance is uniquely identified by an address, represented by the type and id of the function
// being bootstrapped. Any state that is persisted by a bootstrap functions instance will be
// available to the corresponding live StatefulFunction instance having the same address.
public class OrbitIdManagerBootstrap implements StateBootstrapFunction {

  public static final FunctionType TYPE =
      new FunctionType("springbok", "orbit-id-manager-bootstrap");
  // PersistedValues are registered when this function is invoked
  @Persisted
  private final PersistedValue<ArrayList> minFormedOrbitIds =
      PersistedValue.of("min-orbit-ids", ArrayList.class);

  @Persisted
  private final PersistedValue<ArrayList> maxFormedOrbitIds =
      PersistedValue.of("max-orbit-ids", ArrayList.class);

  @Persisted
  private final PersistedValue<Long> lastOrbitId = PersistedValue.of("last-orbit-id", Long.class);

  @Override
  public void bootstrap(Context context, Object input) {
    minFormedOrbitIds.set(extractStateFromInput(input));
    maxFormedOrbitIds.set(extractStateFromInput(input));
    lastOrbitId.set(extractStateFromInput(input));
  }
}
