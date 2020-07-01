package io.springbok.statefun.examples.demonstration;

import io.springbok.statefun.examples.demonstration.generated.DefaultOut;
import io.springbok.statefun.examples.demonstration.generated.TrackIn;
import org.apache.flink.statefun.sdk.Context;
import org.apache.flink.statefun.sdk.FunctionType;
import org.apache.flink.statefun.sdk.StatefulFunction;

public class TrackStatefulBuilder implements StatefulFunction {

  public static final FunctionType TYPE = new FunctionType("springbok", "track-stateful-builder");

  @Override
  public void invoke(Context context, Object input) {

    TrackIn message = (TrackIn) input;

    String line = message.getTrack();

    // Create Track
    // TODO: throw IllegalArgumentException for bad inputs here
    Track track = Track.fromString(line);

    // Send to track stateful function to save and process
    //    context.send(TrackStatefulFunction.TYPE, String.valueOf(track.getId()), track);
    context.send(
        DemonstrationIO.DEFAULT_EGRESS_ID,
        DefaultOut.newBuilder().setTrack(track.toString()).build());
  }
}
