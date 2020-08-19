package io.springbok.statefun.examples.demonstration;

import io.springbok.statefun.examples.demonstration.generated.*;
import org.apache.flink.statefun.sdk.Context;
import org.apache.flink.statefun.sdk.FunctionType;
import org.apache.flink.statefun.sdk.StatefulFunction;
import org.apache.flink.statefun.sdk.annotations.Persisted;
import org.apache.flink.statefun.sdk.state.PersistedValue;

import java.util.ArrayList;

/*
 TrackStatefulFunction stores instances of the Track class built from the data it receives from the TrackIdManager.
 There will be a new instance of this class created for each new id created by the TrackIdManager.
*/
public class TrackStatefulFunction implements StatefulFunction {

  // This FunctionType binding is used in the Demonstration module
  public static final FunctionType TYPE = new FunctionType("springbok", "track-stateful-function");

  // PersistedValues can be stored and recalled when this StatefulFunction is invoked
  @Persisted
  private final PersistedValue<Track> trackState = PersistedValue.of("track", Track.class);

  // Invoke is called once when another part of the application calls context.send to this address.
  // instanceof is used to specify what message is received
  @Override
  public void invoke(Context context, Object input) {

    // TrackIn is a message from the TrackStateful Function. This constructs a new Track from
    // incoming data
    if (input instanceof TrackIn) {
      TrackIn trackIn = (TrackIn) input;

      // OrbitFactory.init() ensures Orekit data is loaded into the current context
      OrbitFactory.init();

      // Create track from input
      Track track = Track.fromString(trackIn.getTrack(), context.self().id());

      NewTrackMessage newTrackMessage =
          NewTrackMessage.newBuilder()
              .setStringTrack(track.toString())
              .setId(track.trackId)
              .build();

      // Send message to the OrbitIdManager that a new track was created
      context.send(OrbitIdManager.TYPE, "orbit-id-manager", newTrackMessage);

      // Send message out that track was created
      Utilities.sendToDefault(context, String.format("Created track for id %s", track.trackId));

      // Set persisted state
      trackState.set(track);
    }

    // NewOrbitIdMessage is sent when a new orbit is created with this track id
    if (input instanceof NewOrbitIdMessage) {
      NewOrbitIdMessage newOrbitIdMessage = (NewOrbitIdMessage) input;

      // Get the trackState and update it with the new id
      Track track = trackState.get();
      track.addOrbitId(newOrbitIdMessage.getId());

      // Send message out that orbitId was added
      Utilities.sendToDefault(
          context,
          String.format(
              "Added orbitId %s to trackId %s", newOrbitIdMessage.getId(), track.trackId));

      // Set persisted state
      trackState.set(track);
    }

    // RemoveOrbitIdMessage is sent when an orbit expires
    if (input instanceof RemoveOrbitIdMessage) {
      RemoveOrbitIdMessage removeOrbitIdMessage = (RemoveOrbitIdMessage) input;

      String orbitId = removeOrbitIdMessage.getStringContent();

      // Get the trackState and remove the id from it
      Track track = trackState.get();
      track.removeOrbitId(orbitId);

      // TODO: potential problem here. If the manager sends a message to an almost expired
      //     orbit and that orbit successfully compares - we have instance of a track being
      // cleared before it can give itself to the refined orbit calculation

      // If the track still has orbits associated with it, save it otherwise delete this value
      if (track.getOrbitIds().size() == 0) {
        trackState.clear();
        Utilities.sendToDefault(
            context, String.format("Cleared track for trackId %s", track.trackId));
      } else {
        trackState.set(track);
        Utilities.sendToDefault(
            context, String.format("Removed orbitId %s from trackId %s", orbitId, track.trackId));
      }
    }

    // CollectedTracksMessage is sent after two orbits correlate. This message is requesting the
    // track information so a least squares estimation can be run
    if (input instanceof CollectedTracksMessage) {
      CollectedTracksMessage collectedTracksMessage = (CollectedTracksMessage) input;

      // Get the trackState and add it to the incoming message
      Track track = trackState.get();

      String collectedTracks;

      if (collectedTracksMessage.getCollectedTracks() == null) {
        collectedTracks = track.toString();
      } else {
        ArrayList<String> trackArray =
            Utilities.stringToArrayList(collectedTracksMessage.getCollectedTracks());
        trackArray.add(track.toString());
        collectedTracks = Utilities.arrayListToString(trackArray);
      }

      // Send message out
      Utilities.sendToDefault(
          context,
          String.format("Added track with id %s to collectedTracksMessage", track.trackId));

      ArrayList<String> remainingTracks =
          Utilities.stringToArrayList(collectedTracksMessage.getRemainingTracksToGather());

      // If the CollectedTracksMessage still needs to collect more tracks, forward it to the next
      // track, otherwise send it to get a new id

      if (remainingTracks.size() > 0) {
        // Send to next track on list

        String trackId = remainingTracks.remove(0);

        CollectedTracksMessage newCollectedTracksMessage =
            CollectedTracksMessage.newBuilder()
                .setKeyedOrbit1(collectedTracksMessage.getKeyedOrbit1())
                .setKeyedOrbit2(collectedTracksMessage.getKeyedOrbit2())
                .setRemainingTracksToGather(Utilities.arrayListToString(remainingTracks))
                .setCollectedTracks(collectedTracks)
                .build();

        context.send(TrackStatefulFunction.TYPE, trackId, newCollectedTracksMessage);
      } else {

        CollectedTracksMessage newCollectedTracksMessage =
            CollectedTracksMessage.newBuilder()
                .setKeyedOrbit1(collectedTracksMessage.getKeyedOrbit1())
                .setKeyedOrbit2(collectedTracksMessage.getKeyedOrbit2())
                .setCollectedTracks(collectedTracks)
                .build();
        // Route to orbitIdManager to get an ID for the new orbit
        context.send(OrbitIdManager.TYPE, "orbit-id-manager", newCollectedTracksMessage);
      }
    }
  }
}
