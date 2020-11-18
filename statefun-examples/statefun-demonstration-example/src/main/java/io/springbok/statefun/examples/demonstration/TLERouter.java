package io.springbok.statefun.examples.demonstration;

import io.springbok.statefun.examples.demonstration.generated.SingleLineTLE;
import org.apache.flink.statefun.sdk.io.Router;

public class TLERouter implements Router<SingleLineTLE> {

  // route to SatelliteStatefulFunctions based on the Satellite ID
  @Override
  public void route(SingleLineTLE singleLineTLE, Downstream<SingleLineTLE> downstream) {
    System.out.println(singleLineTLE);

    downstream.forward(
        SatelliteStatefulFunction.TYPE, singleLineTLE.getSatelliteNumber(), singleLineTLE);
  }
}
