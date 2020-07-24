package io.springbok.statefun.examples.demonstration;

import io.springbok.statefun.examples.demonstration.generated.DefaultOut;
import org.apache.flink.statefun.flink.harness.io.SerializableConsumer;

import java.util.ArrayList;

public class TestConsumer implements SerializableConsumer<DefaultOut> {

  // Must be static to correctly write all messages to same place
  public static ArrayList<String> messages = new ArrayList<>();

  @Override
  public synchronized void accept(DefaultOut message) {
    System.out.println(message.getContent());
    messages.add(message.getContent());
  }

  public void clearMessages() {
    messages = new ArrayList<>();
  }
}
