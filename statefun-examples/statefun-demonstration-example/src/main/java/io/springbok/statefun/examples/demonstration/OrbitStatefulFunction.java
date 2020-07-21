package io.springbok.statefun.examples.demonstration;

import io.springbok.statefun.examples.demonstration.generated.DelayedDeleteMessage;
import org.apache.flink.statefun.sdk.Context;
import org.apache.flink.statefun.sdk.FunctionType;
import org.apache.flink.statefun.sdk.StatefulFunction;
import org.apache.flink.statefun.sdk.annotations.Persisted;
import org.apache.flink.statefun.sdk.state.PersistedValue;

import java.time.Duration;

/*
 TrackStatefulFunction stores instances of the KeyedOrbit class built from the data it receives from the OrbitIdManager.
 There will be a new instance of this class created for each new id created by the OrbitIdManager.
*/
public class OrbitStatefulFunction implements StatefulFunction {

  // This FunctionType binding is used in the Demonstration module
  public static final FunctionType TYPE = new FunctionType("springbok", "orbit-stateful-function");

  // PersistedValues can be stored and recalled when this StatefulFunction is invoked
  @Persisted
  private final PersistedValue<KeyedOrbit> orbitState =
      PersistedValue.of("orbit", KeyedOrbit.class);

  // Invoke is called once when another part of the application calls context.send to this address.
  // instanceof is used to specify what message is received
  @Override
  public void invoke(Context context, Object input) {

    // TrackIn is a message from the TrackStateful Function. This constructs a new KeyedOrbit from
    // incoming data
    if (input instanceof NewTrackMessage) {
      NewTrackMessage newTrackMessage = (NewTrackMessage) input;

      // Create KeyedOrbit
      KeyedOrbit keyedOrbit = OrbitFactory.createOrbit(newTrackMessage.track, context.self().id());

      // Send new orbit id to TrackStatefulFunction
      context.send(
          TrackStatefulFunction.TYPE,
          newTrackMessage.track.trackId,
          new NewOrbitIdMessage(keyedOrbit.orbitId));

      // Send orbit to id manager for correlation and save
      context.send(OrbitIdManager.TYPE, "orbit-id-manager", new CorrelateOrbitsMessage(keyedOrbit));

      // Send delete message to self after a certain amount of time
      sendSelfDeleteMessage(context);

      // Send message out that orbit was created
      Utilities.sendToDefault(
          context, String.format("Created orbit for id %s", keyedOrbit.orbitId));

      orbitState.set(keyedOrbit);
    }

    // This message is sent from this instance of the OrbitStatefulFunction after a period of time
    if (input instanceof DelayedDeleteMessage) {
      KeyedOrbit keyedOrbit = orbitState.get();

      RemoveOrbitIdMessage removeOrbitIdMessage = new RemoveOrbitIdMessage(keyedOrbit.orbitId);

      // Send message to manager to remove this orbit from list
      context.send(OrbitIdManager.TYPE, "orbit-id-manager", removeOrbitIdMessage);

      // Send message to track(s) to remove this orbit from their lists
      keyedOrbit.trackIds.forEach(
          id -> {
            context.send(TrackStatefulFunction.TYPE, String.valueOf(id), removeOrbitIdMessage);
          });

      // Send message out that this orbit was destroyed
      Utilities.sendToDefault(
          context, String.format("Cleared orbit for id %s", keyedOrbit.orbitId));

      orbitState.clear();
    }

    // Message from manager to try to correlate this orbit with incoming orbit
    if (input instanceof CorrelateOrbitsMessage) {
      CorrelateOrbitsMessage correlateOrbitsMessage = (CorrelateOrbitsMessage) input;

      KeyedOrbit recievedKeyedOrbit = correlateOrbitsMessage.getKeyedOrbit();
      KeyedOrbit keyedOrbit = orbitState.get();

      // Attempt correlation
      if (OrbitCorrelator.correlate(recievedKeyedOrbit, keyedOrbit)) {

        // Send message out that correlation successful
        Utilities.sendToDefault(
            context,
            String.format(
                "Correlated orbits with ids %s and %s, and objectIds %s and %s",
                recievedKeyedOrbit.orbitId,
                keyedOrbit.orbitId,
                recievedKeyedOrbit.objectIds.get(0),
                recievedKeyedOrbit.objectIds.get(0)));

        // CollectedTracksMessage gathers all Tracks from one orbit, and keeps the orbit of the
        // other to refine with a least squares
        CollectedTracksMessage collectedTracksMessage =
            new CollectedTracksMessage(recievedKeyedOrbit, keyedOrbit);
        context.send(
            TrackStatefulFunction.TYPE,
            collectedTracksMessage.getNextTrackId(),
            collectedTracksMessage);
      } else {
        // Send message out that correlation not successful
        Utilities.sendToDefault(
            context,
            String.format(
                "Not correlated orbits with ids %s and %s",
                recievedKeyedOrbit.orbitId, keyedOrbit.orbitId));
      }
    }

    // CollectedTracksMessage is received in a new OrbitStatefulFunction created by the
    // OrbitIdManager once all tracks are collected
    if (input instanceof CollectedTracksMessage) {
      CollectedTracksMessage message = (CollectedTracksMessage) input;

      // Create new KeyedOrbit by refining with new tracks
      KeyedOrbit newOrbit =
          OrbitFactory.refineOrbit(
              message.orbit1,
              message.keyedOrbit1TrackIds,
              message.keyedOrbit2Tracks,
              context.self().id());

      // Send message out that orbit was refined
      Utilities.sendToDefault(
          context,
          String.format(
              "Refined orbits with ids %s and %s to create orbit with id %s",
              message.keyedOrbitId1, message.keyedOrbitId2, newOrbit.orbitId));

      NewOrbitIdMessage newOrbitIdMessage = new NewOrbitIdMessage(newOrbit.orbitId);

      // Send a message to the OrbitIdManager to save the new orbit id
      context.send(OrbitIdManager.TYPE, "orbit-id-manager", newOrbitIdMessage);

      // Send orbitId to each TrackStatefulFunction associated with this orbit to save it in each
      // one
      newOrbit.trackIds.forEach(
          id -> {
            context.send(TrackStatefulFunction.TYPE, id, newOrbitIdMessage);
          });

      // Send delete message to self after a certain amount of time
      sendSelfDeleteMessage(context);

      // Send message out that orbit was created and saved
      Utilities.sendToDefault(
          context, String.format("Created refined orbit for id %s", newOrbit.orbitId));

      orbitState.set(newOrbit);
    }
  }

  // Sends a delete message after a certain amount of time
  private void sendSelfDeleteMessage(Context context) {
    context.sendAfter(
        Duration.ofSeconds(320), context.self(), DelayedDeleteMessage.newBuilder().build());
  }
}
