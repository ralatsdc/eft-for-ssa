package io.springbok.eft_for_ssa.lincoln_demo;

import org.apache.flink.statefun.sdk.io.Router;

public class TrackRouter implements Router<String> {

  @Override
  public void route(String message, Downstream<String> downstream) {
    downstream.forward(TrackStatefulBuilder.TYPE, "builder", message);
  }
}
