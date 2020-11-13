package io.springbok.statefun.examples.demonstration;

import org.apache.flink.statefun.sdk.io.Router;
import org.orekit.propagation.analytical.tle.TLE;

public class TLERouter implements Router<TLE> {

  // route to SatelliteStatefulFunctions based on the Satellite ID
  @Override
  public void route(TLE tle, Downstream<TLE> downstream) {
    downstream.forward(
        SatelliteStatefulFunction.TYPE, String.valueOf(tle.getSatelliteNumber()), tle);
  }
}
