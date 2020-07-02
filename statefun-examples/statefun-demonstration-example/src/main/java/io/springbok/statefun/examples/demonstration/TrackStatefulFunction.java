package io.springbok.statefun.examples.demonstration;

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
    if (input instanceof Track) {
      Track track = (Track) input;
      // Create Orbit
      KeyedOrbit orbit = OrbitBuilder.createOrbit(track);

      // Add orbit id to track
      track.addOrbitId(orbit.getOrbitId());
      trackState.set(track);
      Utilities.sendToDefault(
          context,
          String.format("Added orbitId %d to trackId %d", orbit.getOrbitId(), track.getTrackId()));

      // Send to orbit state for save
      context.send(
          OrbitStatefulFunction.TYPE,
          String.valueOf(orbit.getOrbitId()),
          new NewOrbitMessage(orbit));

      // Send orbitid to manager
      context.send(OrbitIdStatefulFunction.TYPE, "id-manager", new AddOrbitMessage(orbit));
    }
    //    if (input instanceof AddOrbitMessage) {
    //      Tracklet tracklet = trackState.get();
    //      AddOrbitMessage orbitMessage = (AddOrbitMessage) input;
    //      Long orbitId = orbitMessage.getOrbitId();
    //      tracklet.addOrbit(orbitId);
    //      trackState.set(tracklet);
    //    }
    if (input instanceof RemoveOrbitMessage) {
      Track track = trackState.get();
      RemoveOrbitMessage orbitMessage = (RemoveOrbitMessage) input;
      Long orbitId = orbitMessage.getOrbitId();
      track.removeOrbitId(orbitId);
      Utilities.sendToDefault(
          context,
          String.format(
              "Removed orbitId %d from trackId %d", orbitMessage.getOrbitId(), track.getTrackId()));

      // TODO: potential problem here. If the manager sends a message to an almost expired
      //     orbit and that orbit successfully compares - we have instance of a tracklet being
      // cleared before it can give itself to the refined orbit calculation

      if (track.getOrbitIds().size() == 0) {
        trackState.clear();
        Utilities.sendToDefault(
            context, String.format("Cleared track for trackId %d", track.getTrackId()));
      } else {
        trackState.set(track);
      }
    }
    //    if (input instanceof CollectedTrackletsMessage) {
    //      Tracklet tracklet = trackState.get();
    //      CollectedTrackletsMessage collectedTrackletsMessage = (CollectedTrackletsMessage) input;
    //      collectedTrackletsMessage.addTracklet(tracklet);
    //      collectedTrackletsMessage.removeTrackletId(tracklet.getId());
    //      if (collectedTrackletsMessage.emptyIdList()) {
    //        // Route back to orbit to do calculation
    //        context.send(
    //            OrbitStatefulFunction.TYPE,
    //            String.valueOf(collectedTrackletsMessage.getOrbitId()),
    //            collectedTrackletsMessage);
    //      } else {
    //        // Send to next tracklet on list
    //        context.send(
    //            TrackStatefulFunction.TYPE,
    //            collectedTrackletsMessage.getRoute(),
    //            collectedTrackletsMessage);
    //      }
    //    }
  }
}
