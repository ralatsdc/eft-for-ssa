package io.springbok.statefun.examples.demonstration;

import io.springbok.statefun.examples.demonstration.generated.DefaultOut;
import io.springbok.statefun.examples.demonstration.generated.SensorInfoMessage;
import org.apache.flink.statefun.sdk.Context;
import org.hipparchus.geometry.euclidean.threed.Vector3D;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;

// Containing Utilities used for convenience in the application
public class Utilities {

  static Integer logLevel = 0;

  // Sending string messages to the default out
  public static void log(Context context, String content, Integer messageLevel) {

    try {
      logLevel = ApplicationProperties.getLogLevel();
    } catch (Exception e) {
      content = "WARNING! COULD NOT READ CONFIG FILE. CHECK PATH. " + content;
      context.send(
          DemonstrationIO.DEFAULT_EGRESS_ID, DefaultOut.newBuilder().setContent(content).build());
    }

    if (logLevel >= messageLevel) {
      Timestamp timestamp = new Timestamp(System.currentTimeMillis());
      content = "[" + timestamp + "] " + content;
      context.send(
          DemonstrationIO.DEFAULT_EGRESS_ID, DefaultOut.newBuilder().setContent(content).build());
    }
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

  public static String SensorInfoMessageToString(SensorInfoMessage sensorInfoMessage) {

    String sensorId = sensorInfoMessage.getSensorId();
    String latitude = String.valueOf(sensorInfoMessage.getLatitude());
    String longitude = String.valueOf(sensorInfoMessage.getLongitude());
    String altitude = String.valueOf(sensorInfoMessage.getAltitude());

    return sensorId + "," + latitude + "," + longitude + "," + altitude;
  }

  public static SensorInfoMessage StringToSensorInfoMessage(String string) {
    String[] values = string.split(",");
    String sensorId = values[0];
    double latitude = Double.parseDouble(values[1]);
    double longitude = Double.parseDouble(values[2]);
    double altitude = Double.parseDouble(values[3]);

    SensorInfoMessage sensorInfoMessage =
        SensorInfoMessage.newBuilder()
            .setSensorId(sensorId)
            .setLatitude(latitude)
            .setLongitude(longitude)
            .setAltitude(altitude)
            .build();

    return sensorInfoMessage;
  }

  // Convert vector3D to x y z representation
  public static String vector3DToString(Vector3D vector3D) {
    String x = String.valueOf(vector3D.getX());
    String y = String.valueOf(vector3D.getY());
    String z = String.valueOf(vector3D.getZ());

    String string = x + "," + y + "," + z;

    return string;
  }

  // Converts string to vector 3d. Assumes csv with 3 doubles.
  public static Vector3D stringToVector3D(String string) {
    String[] values = string.split(",");
    double x = Double.parseDouble(values[0]);
    double y = Double.parseDouble(values[1]);
    double z = Double.parseDouble(values[2]);

    Vector3D vector3D = new Vector3D(x, y, z);

    return vector3D;
  }
}
