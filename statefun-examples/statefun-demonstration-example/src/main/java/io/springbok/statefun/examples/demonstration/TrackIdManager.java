package io.springbok.statefun.examples.demonstration;

import io.springbok.statefun.examples.demonstration.generated.TrackIn;
import org.apache.flink.statefun.sdk.Context;
import org.apache.flink.statefun.sdk.FunctionType;
import org.apache.flink.statefun.sdk.StatefulFunction;
import org.apache.flink.statefun.sdk.annotations.Persisted;
import org.apache.flink.statefun.sdk.state.PersistedValue;

import java.util.ArrayList;

/*
 The TrackIdManager is responsible for giving incoming data new id numbers that are saved for reference within the application
*/
public class TrackIdManager implements StatefulFunction {

  // This FunctionType binding is used in the Demonstration module
  public static final FunctionType TYPE = new FunctionType("springbok", "track-stateful-builder");

  // PersistedValues can be stored and recalled when this StatefulFunction is invoked
  @Persisted
  private final PersistedValue<Long> lastTrackId = PersistedValue.of("last-track-id", Long.class);

  @Persisted
  private final PersistedValue<ArrayList> universes =
      PersistedValue.of("universes", ArrayList.class);

  @Override
  public void invoke(Context context, Object input) {

    // TrackIn is a protobuf message that is a simple container for a string
    TrackIn trackIn = (TrackIn) input;

    // Check validity of track
    try {
      // Create track from input
      Track track = Track.fromString(trackIn.getTrack(), context.self().id());

      // Give the incoming track a new ID
      Long id = lastTrackId.getOrDefault(-1L);
      id++;
      String trackId = track.universe + id.toString();

      // Send the incoming track to save and process at the TrackStatefulFunction that correspond to
      // the just created id
      context.send(TrackStatefulFunction.TYPE, trackId, trackIn);

      // Send a message out that the id creation was successful
      Utilities.log(context, String.format("Created trackId %s", trackId), 2);

      // Update the persisted value so the next created id is unique
      lastTrackId.set(id);
    } catch (Exception e) {
      Utilities.log(
          context,
          String.format(
              "track given id %s not valid. Discarding message '%s' \n Error: %s",
              context.self().id(), trackIn.getTrack(), e),
          1);
    }
  }
}
