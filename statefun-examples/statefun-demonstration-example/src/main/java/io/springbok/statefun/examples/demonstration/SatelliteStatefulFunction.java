package io.springbok.statefun.examples.demonstration;

import io.springbok.statefun.examples.demonstration.generated.SingleLineTLE;
import io.springbok.statefun.examples.utilities.TLEReader;
import org.apache.flink.statefun.sdk.Context;
import org.apache.flink.statefun.sdk.FunctionType;
import org.apache.flink.statefun.sdk.StatefulFunction;
import org.apache.flink.statefun.sdk.annotations.Persisted;
import org.apache.flink.statefun.sdk.state.PersistedValue;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.analytical.tle.TLE;

public class SatelliteStatefulFunction implements StatefulFunction {
  // This FunctionType binding is used in the Demonstration module
  public static final FunctionType TYPE =
      new FunctionType("springbok", "satellite-stateful-function");

  // PersistedValues can be stored and recalled when this StatefulFunction is invoked
  @Persisted
  private final PersistedValue<Orbit> orbitState = PersistedValue.of("orbit", Orbit.class);

  @Override
  public void invoke(Context context, Object input) {

    // OrbitFactory.init() ensures Orekit data is loaded into the current context
    // Not in try block since orekit must be loaded for project to work
    OrbitFactory.init();

    // TLE is from reading the source file
    if (input instanceof SingleLineTLE) {

      SingleLineTLE singleLineTLE = (SingleLineTLE) input;
      TLE tle = TLEReader.fromSingleLineTLE(singleLineTLE);
      Orbit orbit = OrbitFactory.createOrbit(tle);

      orbitState.set(orbit);

      Utilities.log(
          context, String.format("Saved orbit with satellite ID: %s", context.self().id()), 1);
    }
  }
}
