package io.springbok.statefun.examples.demonstration;

import io.springbok.statefun.examples.demonstration.generated.TrackIn;
import org.apache.flink.statefun.sdk.io.Router;

/*
The router is the starting point of all data into the application.
The Kafka stream specified in the Demonstration Module is forwarded through the router to the TrackIdManager
*/
public class TrackRouter implements Router<TrackIn> {

  // There is only one instance of the TrackIdManager class, so all incoming data will be forwarded
  // to the same function
  @Override
  public void route(TrackIn message, Downstream<TrackIn> downstream) {
    downstream.forward(TrackIdManager.TYPE, "track-id-manager", message);
  }
}
