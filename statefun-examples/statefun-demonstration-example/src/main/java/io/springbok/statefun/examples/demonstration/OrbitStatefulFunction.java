package io.springbok.statefun.examples.demonstration;

import io.springbok.statefun.examples.demonstration.generated.*;
import org.apache.flink.statefun.sdk.Context;
import org.apache.flink.statefun.sdk.FunctionType;
import org.apache.flink.statefun.sdk.StatefulFunction;
import org.apache.flink.statefun.sdk.annotations.Persisted;
import org.apache.flink.statefun.sdk.state.PersistedValue;
import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

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

    // OrbitFactory.init() ensures Orekit data is loaded into the current context
    // Not in try block since orekit must be loaded for project to work
    OrbitFactory.init();

    // TrackIn is a message from the TrackStatefulFunction. This constructs a new KeyedOrbit from
    // incoming data
    if (input instanceof NewTrackMessage) {
      NewTrackMessage newTrackMessage = (NewTrackMessage) input;

      Track track = Track.fromString(newTrackMessage.getStringTrack(), newTrackMessage.getId());
      try {
        // Create KeyedOrbit
        KeyedOrbit keyedOrbit = OrbitFactory.createKeyedOrbit(track, context.self().id());

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
        sendSelfDelayedDeleteMessage(context, keyedOrbit.orbit.getDate());

        // Send message out that orbit was created
        Utilities.log(
            context,
            String.format(
                "Created orbitId %s from trackId %s: %s",
                keyedOrbit.orbitId, track.trackId, keyedOrbit.orbit.toString()),
            1);

        orbitState.set(keyedOrbit);
      } catch (Exception e) {
        // Send message out that orbit creation failed
        Utilities.log(
            context,
            String.format(
                "OrbitId %s failed to create. Discarding trackId %s: %s",
                context.self().id(), track.trackId, e),
            1);
        context.send(
            TrackStatefulFunction.TYPE, track.trackId, DeleteTrackMessage.newBuilder().build());
      }
    }

    // This message is sent to delete this state
    if (input instanceof DeleteMessage) {
      try {
        KeyedOrbit keyedOrbit = orbitState.get();

        removeOrbitFromOtherStates(context, keyedOrbit.orbitId, keyedOrbit.trackIds);

        orbitState.clear();
      } catch (NullPointerException e) {
        Utilities.log(
            context, String.format("OrbitId %s already deleted: %s", context.self().id(), e), 2);
      }
    }

    // Message from manager to try to correlate this orbit with incoming orbit
    if (input instanceof CorrelateOrbitsMessage) {
      try {
        CorrelateOrbitsMessage correlateOrbitsMessage = (CorrelateOrbitsMessage) input;

        KeyedOrbit recievedKeyedOrbit =
            KeyedOrbit.fromString(correlateOrbitsMessage.getStringContent());
        KeyedOrbit keyedOrbit = orbitState.get();

        // Check if correlation should be attempted. Correlation should not be attempted if either
        // representation of an orbit is redundant, i.e., all of the tracks contained in one of the
        // orbits are contained in another. If a redundant orbit is found, it will be deleted if it
        // has a number of tracks greater than the track cutoff value found in the properties file
        int redundancySwitch = KeyedOrbit.checkRedundancy(keyedOrbit, recievedKeyedOrbit);

        int trackCutoff = ApplicationProperties.getTrackCutoff();

        switch (redundancySwitch) {
          case 1:

            // keep if smaller or equal to trackcutoff
            if (keyedOrbit.trackIds.size() <= trackCutoff) {
              Utilities.log(
                  context,
                  String.format(
                      "OrbitId %s is redundant with orbitId %s. Orbit is smaller than track cutoff. Not deleting",
                      context.self().id(), recievedKeyedOrbit.orbitId),
                  2);
            } else {
              // delete this orbit
              Utilities.log(
                  context,
                  String.format(
                      "OrbitId %s is redundant with orbitId %s. Sending delete message",
                      context.self().id(), recievedKeyedOrbit.orbitId),
                  2);
              context.send(context.self(), DeleteMessage.newBuilder().build());
            }
            break;

          case 2:

            // keep if smaller or equal to trackcutoff
            if (recievedKeyedOrbit.trackIds.size() <= trackCutoff) {
              Utilities.log(
                  context,
                  String.format(
                      "OrbitId %s is redundant with orbitId %s. Orbit is smaller than track cutoff. Not deleting",
                      recievedKeyedOrbit.orbitId, context.self().id()),
                  2);
            } else {
              // delete recieved orbit
              Utilities.log(
                  context,
                  String.format(
                      "OrbitId %s is redundant with orbitId %s. Sending delete message",
                      recievedKeyedOrbit.orbitId, context.self().id()),
                  2);
              context.send(
                  OrbitStatefulFunction.TYPE,
                  recievedKeyedOrbit.orbitId,
                  DeleteMessage.newBuilder().build());
            }
            break;

          default:

            // Attempt correlation
            if (OrbitCorrelator.correlate(recievedKeyedOrbit, keyedOrbit)) {

              // Send message out that correlation successful
              Utilities.log(
                  context,
                  String.format(
                      "Correlated orbitIds %s and %s",
                      recievedKeyedOrbit.orbitId,
                      keyedOrbit.orbitId,
                      recievedKeyedOrbit.objectIds.get(0),
                      recievedKeyedOrbit.objectIds.get(0)),
                  1);

              Boolean deleteKeyedOrbit1 = false;
              Boolean deleteKeyedOrbit2 = false;

              // Add Orbits larger than trackcutoff to list of tracks to be deleted on successful
              // correlation
              if (keyedOrbit.trackIds.size() > trackCutoff) {
                Utilities.log(
                    context,
                    String.format(
                        "OrbitId %s has more tracks than trackCutoff value %s. Scheduling orbit for deletion.",
                        context.self().id(), trackCutoff),
                    2);
                deleteKeyedOrbit1 = true;
              }
              if (recievedKeyedOrbit.trackIds.size() > trackCutoff) {
                Utilities.log(
                    context,
                    String.format(
                        "OrbitId %s has more tracks than trackCutoff value %s. Scheduling orbit for deletion.",
                        recievedKeyedOrbit.orbitId, trackCutoff),
                    2);
                deleteKeyedOrbit2 = true;
              }

              // CollectedTracksMessage gathers all Tracks from one orbit, and keeps the orbit of
              // the
              // other to refine with a least squares
              Set<String> trackIdsSet = new LinkedHashSet<>(keyedOrbit.trackIds);
              trackIdsSet.addAll(recievedKeyedOrbit.trackIds);
              ArrayList<String> trackIds = new ArrayList<>(trackIdsSet);

              // Build collected tracks message
              CollectedTracksMessage collectedTracksMessage =
                  CollectedTracksMessage.newBuilder()
                      .setKeyedOrbit1(recievedKeyedOrbit.toString())
                      .setKeyedOrbit2(keyedOrbit.toString())
                      .setTracksToGather(Utilities.arrayListToString(trackIds))
                      .setIterator(1)
                      .setDeleteKeyedOrbit1(deleteKeyedOrbit1)
                      .setDeleteKeyedOrbit2(deleteKeyedOrbit2)
                      .build();

              context.send(OrbitIdManager.TYPE, "orbit-id-manager", collectedTracksMessage);
            } else {
              // Send message out that correlation not successful
              Utilities.log(
                  context,
                  String.format(
                      "Not correlated orbitIds %s and %s",
                      recievedKeyedOrbit.orbitId, keyedOrbit.orbitId),
                  3);
            }
        }
      } catch (Exception e) {
        Utilities.log(context, String.format("Not correlated orbits: %s", e), 2);
      }
    }

    // CollectedTracksMessage is received in a new OrbitStatefulFunction created
    // once all tracks are collected
    if (input instanceof CollectedTracksMessage) {
      CollectedTracksMessage collectedTracksMessage = (CollectedTracksMessage) input;
      try {
        KeyedOrbit keyedOrbit1 = KeyedOrbit.fromString(collectedTracksMessage.getKeyedOrbit1());
        KeyedOrbit keyedOrbit2 = KeyedOrbit.fromString(collectedTracksMessage.getKeyedOrbit2());

        ArrayList<String> stringTracks =
            Utilities.stringToArrayList(collectedTracksMessage.getCollectedTracks());
        ArrayList<Track> collectedTracks = new ArrayList<>();

        for (int j = 0; j < stringTracks.size(); j++) {
          collectedTracks.add(
              Track.fromString(stringTracks.remove(j), keyedOrbit2.trackIds.get(j)));
        }

        ArrayList<String> newOrbitTrackIds = new ArrayList<>(keyedOrbit1.trackIds);
        keyedOrbit2.trackIds.forEach(
            track -> {
              if (!newOrbitTrackIds.contains(track)) {
                newOrbitTrackIds.add(track);
              }
            });

        // Create new KeyedOrbit by refining with new tracks
        KeyedOrbit newOrbit =
            OrbitFactory.refineOrbit(
                keyedOrbit1.orbit, newOrbitTrackIds, collectedTracks, context.self().id());

        // Send message out that orbit was refined
        Utilities.log(
            context,
            String.format(
                "Refined orbitIds %s and %s to create orbitId %s: %s",
                keyedOrbit1.orbitId,
                keyedOrbit2.orbitId,
                newOrbit.orbitId,
                newOrbit.orbit.toString()),
            1);

        // Delete old orbits that were flagged
        if (collectedTracksMessage.getDeleteKeyedOrbit1()) {
          context.send(
              OrbitStatefulFunction.TYPE, keyedOrbit1.orbitId, DeleteMessage.newBuilder().build());
        }
        if (collectedTracksMessage.getDeleteKeyedOrbit2()) {
          context.send(
              OrbitStatefulFunction.TYPE, keyedOrbit2.orbitId, DeleteMessage.newBuilder().build());
        }

        NewRefinedOrbitIdMessage newRefinedOrbitIdMessage =
            NewRefinedOrbitIdMessage.newBuilder()
                .setNewOrbitId(newOrbit.orbitId)
                .setOldOrbitId1(keyedOrbit1.orbitId)
                .setOldOrbitId2(keyedOrbit2.orbitId)
                .setOldOrbit1TracksNumber(keyedOrbit1.trackIds.size())
                .setOldOrbit2TracksNumber(keyedOrbit2.trackIds.size())
                .setNewOrbit(newOrbit.toString())
                .build();

        // Send a message to the OrbitIdManager to save the new orbit id
        context.send(OrbitIdManager.TYPE, "orbit-id-manager", newRefinedOrbitIdMessage);

        // Send delete message to self after a certain amount of time
        sendSelfDelayedDeleteMessage(context, newOrbit.orbit.getDate());

        // Send message out that orbit was created and saved
        Utilities.log(context, String.format("Created refined orbitId %s", newOrbit.orbitId), 2);

        orbitState.set(newOrbit);
      } catch (NullPointerException e) {
        Utilities.log(
            context,
            String.format(
                "Orbit refine for orbitId %s failed with exception %s - did you forget to set properties?",
                context.self().id(), e),
            1);

        // Delete this orbit from Tracks' and OrbitIdManager orbit list
        ArrayList<String> trackIds =
            Utilities.stringToArrayList(collectedTracksMessage.getTracksToGather());
        removeOrbitFromOtherStates(context, context.self().id(), trackIds);

      } catch (Exception e) {
        // Send message out that orbit refine failed
        Utilities.log(
            context,
            String.format("Orbit refine for orbitId %s failed: %s", context.self().id(), e),
            1);

        // Delete this orbit from Tracks' and OrbitIdManager orbit list
        ArrayList<String> trackIds =
            Utilities.stringToArrayList(collectedTracksMessage.getTracksToGather());
        removeOrbitFromOtherStates(context, context.self().id(), trackIds);
      }

      // Delete this orbit from Tracks' orbit list
    }
  }

  // Sends a delete message after a certain amount of time
  private void sendSelfDelayedDeleteMessage(Context context, AbsoluteDate orbitDate)
      throws Exception {

    NewEventMessage newEventMessage =
        NewEventMessage.newBuilder()
            .setEventType("delete-orbit")
            .setObjectId(context.self().id())
            .setTime(orbitDate.toString())
            .build();

    // Send to event manager to schedule orbit delete
    context.send(EventManager.TYPE, "event-manager", newEventMessage);
  }

  // Delete this orbit from Tracks' and OrbitIdManager orbit list
  private void removeOrbitFromOtherStates(Context context, String orbitId, ArrayList trackIds) {
    RemoveOrbitIdMessage removeOrbitIdMessage =
        RemoveOrbitIdMessage.newBuilder().setStringContent(orbitId).build();

    // Send message to manager to remove this orbit from list
    context.send(OrbitIdManager.TYPE, "orbit-id-manager", removeOrbitIdMessage);

    // Send message to track(s) to remove this orbit from their lists
    trackIds.forEach(
        id -> {
          context.send(TrackStatefulFunction.TYPE, String.valueOf(id), removeOrbitIdMessage);
        });

    // Send message out that this orbit was destroyed
    Utilities.log(context, String.format("Cleared orbitId %s", orbitId), 1);
  }
}
