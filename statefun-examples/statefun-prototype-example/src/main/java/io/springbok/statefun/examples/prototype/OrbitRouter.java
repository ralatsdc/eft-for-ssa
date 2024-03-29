package io.springbok.statefun.examples.prototype;

import org.apache.flink.statefun.sdk.io.Router;

public class OrbitRouter implements Router<KeyedOrbit> {

  @Override
  public void route(KeyedOrbit message, Downstream<KeyedOrbit> downstream) {
    downstream.forward(OrbitStatefulFunction.TYPE, message.getStringId(), message);
  }
}
