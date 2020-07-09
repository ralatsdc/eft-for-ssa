package io.springbok.statefun.examples.demonstration;

import io.springbok.statefun.examples.demonstration.generated.TrackIn;
import org.apache.flink.statefun.sdk.Context;
import org.apache.flink.statefun.sdk.FunctionType;
import org.apache.flink.statefun.sdk.StatefulFunction;
import org.apache.flink.statefun.sdk.annotations.Persisted;
import org.apache.flink.statefun.sdk.state.PersistedValue;

public class TrackStatefulFunction implements StatefulFunction {

  public static final FunctionType TYPE = new FunctionType("springbok", "track-stateful-function");

  @Persisted
  private final PersistedValue<Track> trackState = PersistedValue.of("track", Track.class);

  @Override
  public void invoke(Context context, Object input) {
    if (input instanceof TrackIn) {
      TrackIn trackIn = (TrackIn) input;

      OrbitFactory.init();
      Track track = Track.fromString(trackIn.getTrack(), context.self().id());

      context.send(OrbitIdManager.TYPE, "orbit-id-manager", new NewTrackMessage(track));

      Utilities.sendToDefault(context, String.format("Created track for id %s", track.trackId));

      trackState.set(track);
    }

    if (input instanceof NewOrbitIdMessage) {
      NewOrbitIdMessage newOrbitIdMessage = (NewOrbitIdMessage) input;

      Track track = trackState.get();
      track.addOrbitId(newOrbitIdMessage.id);

      Utilities.sendToDefault(
          context,
          String.format("Added orbitId %s to trackId %s", newOrbitIdMessage.id, track.trackId));

      trackState.set(track);
    }

    //    if (input instanceof AddOrbitMessage) {
    //      Tracklet tracklet = trackState.get();
    //      AddOrbitMessage orbitMessage = (AddOrbitMessage) input;
    //      Long orbitId = orbitMessage.getOrbitId();
    //      tracklet.addOrbit(orbitId);
    //      trackState.set(tracklet);
    //    }

    if (input instanceof RemoveOrbitIdMessage) {
      RemoveOrbitIdMessage removeOrbitIdMessage = (RemoveOrbitIdMessage) input;
      Track track = trackState.get();
      track.removeOrbitId(removeOrbitIdMessage.orbitId);

      // TODO: potential problem here. If the manager sends a message to an almost expired
      //     orbit and that orbit successfully compares - we have instance of a tracklet being
      // cleared before it can give itself to the refined orbit calculation

      if (track.getOrbitIds().size() == 0) {
        trackState.clear();
        Utilities.sendToDefault(
            context, String.format("Cleared track for trackId %s", track.trackId));
      } else {
        trackState.set(track);
        Utilities.sendToDefault(
            context,
            String.format(
                "Removed orbitId %s from trackId %s", removeOrbitIdMessage.orbitId, track.trackId));
      }
    }
        if (input instanceof CollectedTracksMessage) {
          CollectedTracksMessage collectedTracksMessage = (CollectedTracksMessage) input;

          Track track = trackState.get();
          collectedTracksMessage.addTrack(track);
          Utilities.sendToDefault(context, String.format("Added track with id %s to collectedTracksMessage with orbit ids %s and %s", track.trackId, collectedTracksMessage.keyedOrbitId1, collectedTracksMessage.keyedOrbitId2));

          if (collectedTracksMessage.hasNextTrackId()) {
            // Send to next track on list
            context.send(
                    TrackStatefulFunction.TYPE,
                    collectedTracksMessage.getNextTrackId(),
                    collectedTracksMessage);
          } else {
            // Route back to orbit to do calculation
            context.send(
                    OrbitStatefulFunction.TYPE,
                    collectedTracksMessage.keyedOrbitId1,
                    collectedTracksMessage);
          }
        }
  }
}
