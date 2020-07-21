package io.springbok.statefun.examples.demonstration;

import io.springbok.statefun.examples.demonstration.generated.DefaultOut;
import org.apache.flink.statefun.sdk.Context;

// Containing Utilities used for convenience in the application
public class Utilities {

  // Sending string messages to the default out
  public static void sendToDefault(Context context, String content) {
    context.send(
        DemonstrationIO.DEFAULT_EGRESS_ID, DefaultOut.newBuilder().setContent(content).build());
  }
}
