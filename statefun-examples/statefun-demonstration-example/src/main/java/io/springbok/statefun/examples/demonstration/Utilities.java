package io.springbok.statefun.examples.demonstration;

import io.springbok.statefun.examples.demonstration.generated.DefaultOut;
import org.apache.flink.statefun.sdk.Context;

public class Utilities {

  public static void sendToDefault(Context context, String content) {
    context.send(
        DemonstrationIO.DEFAULT_EGRESS_ID, DefaultOut.newBuilder().setContent(content).build());
  }
}
