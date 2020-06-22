package io.springbok.eft_for_ssa.lincoln_demo;

import org.apache.flink.statefun.sdk.Context;
import org.apache.flink.statefun.sdk.FunctionType;
import org.apache.flink.statefun.sdk.StatefulFunction;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;

import java.io.File;

public class TrackStatefulBuilder implements StatefulFunction {

  public static final FunctionType TYPE =
      new FunctionType("springbok", "tracklet-stateful-builder");

  @Override
  public void invoke(Context context, Object input) {

    // Configure Orekit
    final File orekitData = new File("./orekit-data");
    final DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
    manager.addProvider(new DirectoryCrawler(orekitData));

    String line = (String) input;

    // Create Track
    // TODO: throw IllegalArgumentException for bad inputs here
    Track track = Track.fromString(line);

    // Send to track stateful function to save and process
    //    context.send(TrackStatefulFunction.TYPE, String.valueOf(track.getId()), track);
    context.send(IO.DEFAULT_EGRESS_ID, track.toString());
  }
}
