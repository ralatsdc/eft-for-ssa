package io.springbok.statefun.examples.demonstration;

import org.apache.flink.statefun.sdk.Context;
import org.apache.flink.statefun.sdk.FunctionType;
import org.apache.flink.statefun.sdk.StatefulFunction;
import org.apache.flink.statefun.sdk.annotations.Persisted;
import org.apache.flink.statefun.sdk.state.PersistedValue;

import java.time.Duration;

public class OrbitStatefulFunction implements StatefulFunction {

  public static final FunctionType TYPE = new FunctionType("springbok", "orbit-stateful-function");

  @Persisted
  private final PersistedValue<KeyedOrbit> orbitState =
      PersistedValue.of("orbit", KeyedOrbit.class);

  @Override
  public void invoke(Context context, Object input) {

    if (input instanceof NewTrackMessage) {
      NewTrackMessage newTrackMessage = (NewTrackMessage) input;

      // Create Orbit
      KeyedOrbit keyedOrbit = OrbitFactory.createOrbit(newTrackMessage.track, context.self().id());

      // Send orbitId to TrackStatefulFunction
      context.send(
          TrackStatefulFunction.TYPE,
          newTrackMessage.track.trackId,
          new NewOrbitIdMessage(keyedOrbit.orbitId));

      // Send delete message
      context.sendAfter(Duration.ofSeconds(2), context.self(), new DelayedDeleteMessage());

      Utilities.sendToDefault(
          context, String.format("Created orbit for id %s", keyedOrbit.orbitId));

      orbitState.set(keyedOrbit);
    }

    if (input instanceof DelayedDeleteMessage) {
      KeyedOrbit keyedOrbit = orbitState.get();

      RemoveOrbitIdMessage removeOrbitIdMessage = new RemoveOrbitIdMessage(keyedOrbit.orbitId);

      // Send message to manager
      context.send(OrbitIdManager.TYPE, "orbit-id-manager", removeOrbitIdMessage);

      // Send message to track(s)
      keyedOrbit.trackIds.forEach(
          id -> {
            context.send(TrackStatefulFunction.TYPE, String.valueOf(id), removeOrbitIdMessage);
          });

      Utilities.sendToDefault(
          context, String.format("Cleared orbit for id %s", keyedOrbit.orbitId));

      orbitState.clear();
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
}
