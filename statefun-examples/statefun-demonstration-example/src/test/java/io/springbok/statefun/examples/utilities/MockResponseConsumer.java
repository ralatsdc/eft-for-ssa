package io.springbok.statefun.examples.utilities;

import io.springbok.statefun.examples.demonstration.generated.Command;
import org.apache.flink.statefun.flink.harness.io.SerializableConsumer;

import java.util.ArrayList;

public class MockResponseConsumer implements SerializableConsumer<Command> {

  // Must be static to correctly write all messages to same place
  public static ArrayList<String> messages = new ArrayList<>();

  @Override
  public synchronized void accept(Command message) {
    System.out.println(message.getCommand());
    messages.add(message.getCommand());
  }

  public void clearMessages() {
    messages = new ArrayList<>();
  }
}
