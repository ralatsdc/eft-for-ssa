package io.springbok.statefun.examples.prototype;

import org.apache.flink.statefun.sdk.Context;
import org.apache.flink.statefun.sdk.FunctionType;
import org.apache.flink.statefun.sdk.StatefulFunction;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;

import java.io.File;

public class TrackletStatefulBuilder implements StatefulFunction {

  public static final FunctionType TYPE =
      new FunctionType("springbok", "tracklet-stateful-builder");

  @Override
  public void invoke(Context context, Object input) {

    // Configure Orekit
    final File orekitData = new File("./orekit-data");
    final DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
    manager.addProvider(new DirectoryCrawler(orekitData));

    String line = (String) input;

    // Create Tracklet
    // TODO: throw IllegalArgumentException for bad inputs here
    Tracklet tracklet = Tracklet.fromString(line);

    // Send to tracklet stateful function to save and process
    context.send(TrackletStatefulFunction.TYPE, String.valueOf(tracklet.getId()), tracklet);
  }
}
