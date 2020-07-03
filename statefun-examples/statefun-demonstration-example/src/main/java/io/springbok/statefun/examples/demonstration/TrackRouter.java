package io.springbok.statefun.examples.demonstration;

import io.springbok.statefun.examples.demonstration.generated.TrackIn;
import org.apache.flink.statefun.sdk.io.Router;

public class TrackRouter implements Router<TrackIn> {

  @Override
  public void route(TrackIn message, Downstream<TrackIn> downstream) {
    downstream.forward(TrackIdManager.TYPE, "track-id-manager", message);
  }
}
