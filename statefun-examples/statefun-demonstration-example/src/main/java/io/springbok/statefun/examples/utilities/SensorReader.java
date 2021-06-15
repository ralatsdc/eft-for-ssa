package io.springbok.statefun.examples.utilities;

import io.springbok.statefun.examples.demonstration.Utilities;
import io.springbok.statefun.examples.demonstration.generated.SensorInfoMessage;
import org.hipparchus.util.FastMath;

import java.util.ArrayList;

public class SensorReader {

  // TODO: class reads file with sensor information
  public static ArrayList<String> getTestSensorInfo() {

    ArrayList<String> sensors = new ArrayList<>();

    String stringSensor0 =
        createStringSensor("0", FastMath.toRadians(45.), FastMath.toRadians(25.), 0.);
    String stringSensor1 =
        createStringSensor("1", FastMath.toRadians(0.), FastMath.toRadians(0.), 0.);

    sensors.add(stringSensor0);
    sensors.add(stringSensor1);

    return sensors;
  }

  private static String createStringSensor(
      String id, double longitude, double latitude, double altitude) {

    SensorInfoMessage sensorInfoMessage =
        SensorInfoMessage.newBuilder()
            .setSensorId(id)
            .setLongitude(longitude)
            .setLatitude(latitude)
            .setAltitude(altitude)
            .build();

    String stringSensor = Utilities.SensorInfoMessageToString(sensorInfoMessage);

    return stringSensor;
  }
}
