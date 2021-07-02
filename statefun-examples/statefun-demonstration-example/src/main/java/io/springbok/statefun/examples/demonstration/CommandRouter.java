package io.springbok.statefun.examples.demonstration;

import io.springbok.statefun.examples.demonstration.generated.Command;
import org.apache.flink.statefun.sdk.io.Router;

public class CommandRouter implements Router<Command> {

  // There is only one instance of the SensorIdManager class, so all incoming data will be forwarded
  // to the same function
  @Override
  public void route(Command message, Downstream<Command> downstream) {

    String command = message.getCommand();

    if (command.toLowerCase().indexOf("universe") != -1) {
      downstream.forward(UniverseIdManager.TYPE, "universe-id-manager", message);
    }
  }
}
