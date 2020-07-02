package io.springbok.statefun.examples.demonstration;

import org.apache.flink.statefun.sdk.Context;
import org.apache.flink.statefun.sdk.FunctionType;
import org.apache.flink.statefun.sdk.StatefulFunction;
import org.apache.flink.statefun.sdk.annotations.Persisted;
import org.apache.flink.statefun.sdk.state.PersistedValue;

import java.time.Duration;
import java.util.ArrayList;

public class OrbitStatefulFunction implements StatefulFunction {

  public static final FunctionType TYPE = new FunctionType("springbok", "orbit-stateful-function");

  @Persisted
  private final PersistedValue<KeyedOrbit> orbitState =
      PersistedValue.of("orbit", KeyedOrbit.class);

  @Override
  public void invoke(Context context, Object input) {

    if (input instanceof NewOrbitMessage) {

      NewOrbitMessage message = (NewOrbitMessage) input;
      KeyedOrbit orbit = message.getOrbit();

      // Persist orbit state
      orbitState.set(orbit);
      Utilities.sendToDefault(
          context, String.format("Set orbit for orbitId %d", orbit.getOrbitId()));

      // Send delete message
      context.sendAfter(Duration.ofSeconds(2), context.self(), new DelayedDeleteMessage());
    }

    if (input instanceof DelayedDeleteMessage) {
      KeyedOrbit orbit = orbitState.get();
      ArrayList<Long> ids = orbit.getTracksId();

      // Send message to manager
      context.send(
          OrbitIdStatefulFunction.TYPE, "id-manager", new RemoveOrbitMessage(orbit.getOrbitId()));

      // Send message to track(s)
      ids.forEach(
          id -> {
            context.send(
                TrackStatefulFunction.TYPE,
                String.valueOf(id),
                new RemoveOrbitMessage(orbit.getOrbitId()));
          });

      orbitState.clear();
      Utilities.sendToDefault(
          context, String.format("Removed orbit for orbitId %d", orbit.getOrbitId()));
    }

    //    // Message from manager
    //    if (input instanceof CompareOrbitsMessage) {
    //      CompareOrbitsMessage message = (CompareOrbitsMessage) input;
    //      KeyedOrbit recievedOrbit = message.getOrbit();
    //      KeyedOrbit orbit = orbitState.get();
    //
    //      if (CompareOrbits.compareAtRandom(recievedOrbit, orbit)) {
    //        // Get tracklets from current orbitState
    //        CollectedTrackletsMessage collectedTrackletsMessage =
    //            new CollectedTrackletsMessage(recievedOrbit, orbit);
    //        context.send(IO.STRING_EGRESS_ID, "Sending Orbit Comparison");
    //        context.send(
    //            TrackletStatefulFunction.TYPE,
    //            collectedTrackletsMessage.getRoute(),
    //            collectedTrackletsMessage);
    //      }
    //    }
    //
    //    // Orbit least squares refine
    //    if (input instanceof CollectedTrackletsMessage) {
    //      // call orbit builder
    //      CollectedTrackletsMessage message = (CollectedTrackletsMessage) input;
    //      KeyedOrbit orbit = orbitState.get();
    //      KeyedOrbit newOrbit = OrbitBuilder.refineOrbit(orbit, message.getTracklets());
    //
    //      context.send(
    //          OrbitStatefulFunction.TYPE,
    //          String.valueOf(newOrbit.getId()),
    //          new NewOrbitMessage(newOrbit));
    //    }
  }

  private class DelayedDeleteMessage {}
}
