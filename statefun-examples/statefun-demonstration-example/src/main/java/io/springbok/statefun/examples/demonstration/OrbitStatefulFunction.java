package io.springbok.statefun.examples.demonstration;

import io.springbok.statefun.examples.demonstration.generated.*;
import org.apache.flink.statefun.sdk.Context;
import org.apache.flink.statefun.sdk.FunctionType;
import org.apache.flink.statefun.sdk.StatefulFunction;
import org.apache.flink.statefun.sdk.annotations.Persisted;
import org.apache.flink.statefun.sdk.state.PersistedValue;

import java.time.Duration;
import java.util.ArrayList;

/*
 TrackStatefulFunction stores instances of the KeyedOrbit class built from the data it receives from the OrbitIdManager.
 There will be a new instance of this class created for each new id created by the OrbitIdManager.
*/
public class OrbitStatefulFunction implements StatefulFunction {

  // This FunctionType binding is used in the Demonstration module
  public static final FunctionType TYPE = new FunctionType("springbok", "orbit-stateful-function");

  public static int deleteTimer = 320;

  // PersistedValues can be stored and recalled when this StatefulFunction is invoked
  @Persisted
  private final PersistedValue<KeyedOrbit> orbitState =
      PersistedValue.of("orbit", KeyedOrbit.class);

  // Invoke is called once when another part of the application calls context.send to this address.
  // instanceof is used to specify what message is received
  @Override
  public void invoke(Context context, Object input) {

    // TrackIn is a message from the TrackStatefulFunction. This constructs a new KeyedOrbit from
    // incoming data
    if (input instanceof NewTrackMessage) {
      NewTrackMessage newTrackMessage = (NewTrackMessage) input;

      Track track = Track.fromString(newTrackMessage.getStringTrack(), newTrackMessage.getId());
      try {
        // Create KeyedOrbit
        KeyedOrbit keyedOrbit = OrbitFactory.createOrbit(track, context.self().id());

        // Send new orbit id to TrackStatefulFunction
        context.send(
            TrackStatefulFunction.TYPE,
            track.trackId,
            NewOrbitIdMessage.newBuilder().setId(keyedOrbit.orbitId).build());

        // Send orbit to id manager for correlation and save
        context.send(
            OrbitIdManager.TYPE,
            "orbit-id-manager",
            CorrelateOrbitsMessage.newBuilder().setStringContent(keyedOrbit.toString()).build());

        // Send delete message to self after a certain amount of time
        sendSelfDeleteMessage(context);

        // Send message out that orbit was created
        Utilities.sendToDefault(
            context,
            String.format(
                "Created orbit for id %s: %s", keyedOrbit.orbitId, keyedOrbit.orbit.toString()));

        orbitState.set(keyedOrbit);
      } catch (Exception e) {
        // Send message out that orbit was created
        Utilities.sendToDefault(
            context,
            String.format(
                "Orbit with id %s failed to create. Discarding track with id %s",
                context.self().id(), track.trackId));
        context.send(
            TrackStatefulFunction.TYPE, track.trackId, DeleteTrackMessage.newBuilder().build());
      }
    }

    // This message is sent from this instance of the OrbitStatefulFunction after a period of time
    if (input instanceof DelayedDeleteMessage) {
      KeyedOrbit keyedOrbit = orbitState.get();

      RemoveOrbitIdMessage removeOrbitIdMessage =
          RemoveOrbitIdMessage.newBuilder().setStringContent(keyedOrbit.orbitId).build();

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
      try {
        CorrelateOrbitsMessage correlateOrbitsMessage = (CorrelateOrbitsMessage) input;

        KeyedOrbit recievedKeyedOrbit =
            KeyedOrbit.fromString(correlateOrbitsMessage.getStringContent());
        KeyedOrbit keyedOrbit = orbitState.get();

        // Attempt correlation
        if (OrbitCorrelator.correlate(recievedKeyedOrbit, keyedOrbit)) {

          // Send message out that correlation successful
          Utilities.sendToDefault(
              context,
              String.format(
                  "Correlated orbits with ids %s and %s",
                  recievedKeyedOrbit.orbitId,
                  keyedOrbit.orbitId,
                  recievedKeyedOrbit.objectIds.get(0),
                  recievedKeyedOrbit.objectIds.get(0)));

          // CollectedTracksMessage gathers all Tracks from one orbit, and keeps the orbit of the
          // other to refine with a least squares
          ArrayList<String> trackIds = new ArrayList<>(keyedOrbit.trackIds);
          String nextTrack = trackIds.get(0);

          CollectedTracksMessage collectedTracksMessage =
              CollectedTracksMessage.newBuilder()
                  .setKeyedOrbit1(recievedKeyedOrbit.toString())
                  .setKeyedOrbit2(keyedOrbit.toString())
                  .setTracksToGather(Utilities.arrayListToString(trackIds))
                  .setIterator(1)
                  .build();

          context.send(TrackStatefulFunction.TYPE, nextTrack, collectedTracksMessage);
        } else {
          // Send message out that correlation not successful
          Utilities.sendToDefault(
              context,
              String.format(
                  "Not correlated orbits with ids %s and %s",
                  recievedKeyedOrbit.orbitId, keyedOrbit.orbitId));
        }
      } catch (Exception e) {
        Utilities.sendToDefault(
            context,
            String.format(
                "Not correlated orbits - orbit with id %s has expired", context.self().id()));
      }
    }

    // CollectedTracksMessage is received in a new OrbitStatefulFunction created by the
    // OrbitIdManager once all tracks are collected
    if (input instanceof CollectedTracksMessage) {
      try {
        CollectedTracksMessage collectedTracksMessage = (CollectedTracksMessage) input;

        KeyedOrbit keyedOrbit1 = KeyedOrbit.fromString(collectedTracksMessage.getKeyedOrbit1());
        KeyedOrbit keyedOrbit2 = KeyedOrbit.fromString(collectedTracksMessage.getKeyedOrbit2());

        ArrayList<String> stringTracks =
            Utilities.stringToArrayList(collectedTracksMessage.getCollectedTracks());
        ArrayList<Track> collectedTracks = new ArrayList<>();

        for (int j = 0; j < stringTracks.size(); j++) {
          collectedTracks.add(
              Track.fromString(stringTracks.remove(j), keyedOrbit2.trackIds.get(j)));
        }

        // Create new KeyedOrbit by refining with new tracks
        KeyedOrbit newOrbit =
            OrbitFactory.refineOrbit(
                keyedOrbit1.orbit, keyedOrbit1.trackIds, collectedTracks, context.self().id());

        // Send message out that orbit was refined
        Utilities.sendToDefault(
            context,
            String.format(
                "Refined orbits with ids %s and %s to create orbit with id %s: %s",
                keyedOrbit1.orbitId,
                keyedOrbit2.orbitId,
                newOrbit.orbitId,
                newOrbit.orbit.toString()));

        NewRefinedOrbitIdMessage newRefinedOrbitIdMessage =
            NewRefinedOrbitIdMessage.newBuilder()
                .setNewOrbitId(newOrbit.orbitId)
                .setOldOrbitId1(keyedOrbit1.orbitId)
                .setOldOrbitId2(keyedOrbit2.orbitId)
                .build();

        // Send a message to the OrbitIdManager to save the new orbit id
        context.send(OrbitIdManager.TYPE, "orbit-id-manager", newRefinedOrbitIdMessage);

        // Send orbitId to each TrackStatefulFunction associated with this orbit to save it in each
        // one
        NewOrbitIdMessage newOrbitIdMessage =
            NewOrbitIdMessage.newBuilder().setId(newOrbit.orbitId).build();
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
      } catch (Exception e) {

        // Send message out that orbit refine failed
        Utilities.sendToDefault(
            context,
            String.format(
                "Orbit refine for orbit id %s failed with exception %s",
                context.self().id(), e.toString()));
      }
    }
  }

  // Sends a delete message after a certain amount of time
  private void sendSelfDeleteMessage(Context context) {
    context.sendAfter(
        Duration.ofSeconds(deleteTimer), context.self(), DelayedDeleteMessage.newBuilder().build());
  }
}
