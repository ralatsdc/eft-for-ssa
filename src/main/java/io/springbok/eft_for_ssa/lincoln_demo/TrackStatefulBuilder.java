package io.springbok.eft_for_ssa.lincoln_demo;

import org.apache.flink.statefun.sdk.Context;
import org.apache.flink.statefun.sdk.FunctionType;
import org.apache.flink.statefun.sdk.StatefulFunction;

public class TrackStatefulBuilder implements StatefulFunction {

  public static final FunctionType TYPE = new FunctionType("springbok", "track-stateful-builder");

  @Override
  public void invoke(Context context, Object input) {

    String line = (String) input;

    // Create Track
    // TODO: throw IllegalArgumentException for bad inputs here
    Track track = Track.fromString(line);

    // Send to track stateful function to save and process
    //    context.send(TrackStatefulFunction.TYPE, String.valueOf(track.getId()), track);
    context.send(IO.DEFAULT_EGRESS_ID, track.toString());
  }
}
