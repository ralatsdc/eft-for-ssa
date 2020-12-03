package io.springbok.statefun.examples.demonstration;

import io.springbok.statefun.examples.demonstration.generated.SensorIn;
import org.apache.flink.statefun.sdk.io.Router;

public class SensorRouter implements Router<SensorIn> {

  // There is only one instance of the SensorIdManager class, so all incoming data will be forwarded
  // to the same function
  @Override
  public void route(SensorIn message, Downstream<SensorIn> downstream) {
    downstream.forward(SensorIdManager.TYPE, "sensor-id-manager", message);
  }
}
