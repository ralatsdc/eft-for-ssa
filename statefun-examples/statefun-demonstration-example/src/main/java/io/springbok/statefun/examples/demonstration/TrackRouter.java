package io.springbok.statefun.examples.demonstration;

import io.springbok.statefun.examples.lincoln_demo.generated.TrackIn;
import org.apache.flink.statefun.sdk.io.Router;

public class TrackRouter implements Router<TrackIn> {

  @Override
  public void route(TrackIn message, Downstream<TrackIn> downstream) {
    downstream.forward(TrackStatefulBuilder.TYPE, "builder", message);
  }
}
