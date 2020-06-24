package io.springbok.eft_for_ssa.examples;

import org.apache.flink.statefun.sdk.io.Router;

public class LineRouter implements Router<String> {

  @Override
  public void route(String message, Downstream<String> downstream) {
    downstream.forward(TrackletStatefulBuilder.TYPE, "builder", message);
  }
}
