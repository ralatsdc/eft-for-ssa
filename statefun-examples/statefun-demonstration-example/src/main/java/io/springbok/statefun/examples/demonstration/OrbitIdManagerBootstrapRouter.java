package io.springbok.statefun.examples.demonstration;

import io.springbok.statefun.examples.demonstration.generated.BootstrapOrbitIdManager;
import org.apache.flink.statefun.sdk.io.Router;

public class OrbitIdManagerBootstrapRouter implements Router<BootstrapOrbitIdManager> {
  @Override
  public void route(
      BootstrapOrbitIdManager message, Downstream<BootstrapOrbitIdManager> downstream) {
    downstream.forward(OrbitIdManagerBootstrap.TYPE, "orbit-id-manager", message);
  }
}
