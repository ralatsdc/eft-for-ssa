package io.springbok.statefun.examples.demonstration;

import org.apache.flink.statefun.sdk.Context;
import org.apache.flink.statefun.sdk.FunctionType;
import org.apache.flink.statefun.sdk.StatefulFunction;
import org.apache.flink.statefun.sdk.annotations.Persisted;
import org.apache.flink.statefun.sdk.state.PersistedValue;
import org.orekit.orbits.Orbit;

public class SatelliteStatefulFunction implements StatefulFunction {
  // This FunctionType binding is used in the Demonstration module
  public static final FunctionType TYPE =
      new FunctionType("springbok", "satellite-stateful-function");

  // PersistedValues can be stored and recalled when this StatefulFunction is invoked
  @Persisted
  private final PersistedValue<Orbit> orbitState = PersistedValue.of("orbit", Orbit.class);

  @Override
  public void invoke(Context context, Object o) {}
}
