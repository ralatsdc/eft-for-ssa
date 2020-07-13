package io.springbok.statefun.examples.demonstration;

import io.springbok.statefun.examples.demonstration.generated.DelayedDeleteMessage;
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

      // Send orbit to id manager for comparison
      context.send(OrbitIdManager.TYPE, "orbit-id-manager", new CorrelateOrbitsMessage(keyedOrbit));

      // Send delete message
      context.sendAfter(
          Duration.ofSeconds(2), context.self(), DelayedDeleteMessage.newBuilder().build());

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

    // Message from manager
    if (input instanceof CorrelateOrbitsMessage) {
      CorrelateOrbitsMessage correlateOrbitsMessage = (CorrelateOrbitsMessage) input;

      KeyedOrbit recievedKeyedOrbit = correlateOrbitsMessage.getKeyedOrbit();
      KeyedOrbit keyedOrbit = orbitState.get();

      if (OrbitCorrelator.correlate(recievedKeyedOrbit, keyedOrbit)) {

        Utilities.sendToDefault(
            context,
            String.format(
                "Correlated orbits with ids %s and %s",
                recievedKeyedOrbit.orbitId, keyedOrbit.orbitId));

        // Get tracks from current orbitState
        CollectedTracksMessage collectedTracksMessage =
            new CollectedTracksMessage(recievedKeyedOrbit, keyedOrbit);
        context.send(
            TrackStatefulFunction.TYPE,
            collectedTracksMessage.getNextTrackId(),
            collectedTracksMessage);
      } else {
        Utilities.sendToDefault(
            context,
            String.format(
                "Not correlated orbits with ids %s and %s",
                recievedKeyedOrbit.orbitId, keyedOrbit.orbitId));
      }
    }

    // Least squares refine creation of the state of THIS OrbitStatefulFunction instance
    if (input instanceof CollectedTracksMessage) {
      CollectedTracksMessage message = (CollectedTracksMessage) input;

      // Create new orbit by refining with new tracks
      System.out.println("Orbit: " + message.orbit1);
      System.out.println("Tracks: " + message.keyedOrbit2Tracks);
      System.out.println("ID: " + context.self().id());
      KeyedOrbit newOrbit =
          OrbitFactory.refineOrbit(message.orbit1, message.keyedOrbit2Tracks, context.self().id());
      Utilities.sendToDefault(
          context,
          String.format(
              "Refined orbits with ids %s and %s to create orbit with id %s",
              message.keyedOrbitId1, message.keyedOrbitId2, newOrbit.orbitId));

      // Save new orbit and send to idmanager
    }

    // Save orbit that has already been registered with the ID manager
    // TODO: reduce repetition here and with NewTracksMessage
    if (input instanceof RefinedOrbitMessage) {
      RefinedOrbitMessage refinedOrbitMessage = (RefinedOrbitMessage) input;

      KeyedOrbit keyedOrbit = refinedOrbitMessage.keyedOrbit;

      // Send orbitId to TrackStatefulFunction
      refinedOrbitMessage.keyedOrbit.trackIds.forEach(
          id -> {
            context.send(TrackStatefulFunction.TYPE, id, new NewOrbitIdMessage(keyedOrbit.orbitId));
          });

      // Send delete message
      context.sendAfter(
          Duration.ofSeconds(2), context.self(), DelayedDeleteMessage.newBuilder().build());

      Utilities.sendToDefault(
          context, String.format("Created refined orbit for id %s", keyedOrbit.orbitId));

      orbitState.set(keyedOrbit);
    }
  }
}
