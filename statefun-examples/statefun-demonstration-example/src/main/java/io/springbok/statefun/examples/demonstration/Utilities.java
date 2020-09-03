package io.springbok.statefun.examples.demonstration;

import io.springbok.statefun.examples.demonstration.generated.DefaultOut;
import org.apache.flink.statefun.sdk.Context;

import java.util.ArrayList;
import java.util.Arrays;

// Containing Utilities used for convenience in the application
public class Utilities {

  // Sending string messages to the default out
  public static void sendToDefault(Context context, String content) {
    context.send(
        DemonstrationIO.DEFAULT_EGRESS_ID, DefaultOut.newBuilder().setContent(content).build());
  }

  public static String arrayListToString(ArrayList<String> arrayList) {

    StringBuilder stringBuilder = new StringBuilder();
    for (String s : arrayList) {
      stringBuilder.append(s);
      stringBuilder.append(";");
    }
    String str = stringBuilder.toString();
    if (str != null && str.length() > 0) {
      str = str.substring(0, str.length() - 1);
    }
    return str;
  }

  public static ArrayList<String> stringToArrayList(String string) {

    if (string == "") {
      return new ArrayList<>();
    } else {
      ArrayList<String> arrayList = new ArrayList<>(Arrays.asList(string.split(";")));

      return arrayList;
    }
  }
}
