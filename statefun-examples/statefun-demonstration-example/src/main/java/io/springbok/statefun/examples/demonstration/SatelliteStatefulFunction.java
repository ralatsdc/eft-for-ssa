package io.springbok.statefun.examples.demonstration;

import io.springbok.statefun.examples.demonstration.generated.DelayedWakeUpMessage;
import io.springbok.statefun.examples.demonstration.generated.SingleLineTLE;
import io.springbok.statefun.examples.utilities.TLEReader;
import org.apache.flink.statefun.sdk.Context;
import org.apache.flink.statefun.sdk.FunctionType;
import org.apache.flink.statefun.sdk.StatefulFunction;
import org.apache.flink.statefun.sdk.annotations.Persisted;
import org.apache.flink.statefun.sdk.state.PersistedValue;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.analytical.tle.TLE;

import java.time.Duration;

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

      try {
        SingleLineTLE singleLineTLE = (SingleLineTLE) input;
        TLE tle = TLEReader.fromSingleLineTLE(singleLineTLE);
        Orbit orbit = OrbitFactory.createOrbit(tle);

        orbitState.set(orbit);
        sendWakeUpMessage(context);
        Utilities.log(
            context, String.format("Saved orbit with satellite ID: %s", context.self().id()), 1);
      } catch (Exception e) {
        Utilities.log(
            context,
            String.format(
                "Failed to save orbit with satellite ID: %s. Exception: %s",
                context.self().id(), e),
            1);
      }
    }

    // TODO: check sensor to see if it's seeable - send messages to SensorStatefulFunction - will
    // send back message if it's seeable
    // Not sure what makes the most sense to distribute this - don't want to bog down the sensor
    // statefulfunctions every time a satellite wakes up

    // Maybe there's a calculated idea of when it'll be in view that the satellite can just know -
    // and it'll only have to send a message initially to get that object and maybe periodically to
    // make sure it's in sync
    // TODO: save current state
    if (input instanceof DelayedWakeUpMessage) {

      try {
        sendWakeUpMessage(context);
        Utilities.log(
            context,
            String.format("Satellite with ID %s is checking sensors", context.self().id()),
            1);
      } catch (Exception e) {
        Utilities.log(
            context,
            String.format(
                "Satellite with ID failed to wake up: %s. Exception: %s", context.self().id(), e),
            1);
      }
    }
  }

  // Sends a delete message after a certain amount of time
  private void sendWakeUpMessage(Context context) throws Exception {

    long wakeupInterval = ApplicationProperties.getWakeupInterval();

    context.sendAfter(
        Duration.ofSeconds(wakeupInterval),
        context.self(),
        DelayedWakeUpMessage.newBuilder().build());
  }
}
