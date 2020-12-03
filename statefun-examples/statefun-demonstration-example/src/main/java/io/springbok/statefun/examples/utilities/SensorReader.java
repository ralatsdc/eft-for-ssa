package io.springbok.statefun.examples.utilities;

import io.springbok.statefun.examples.demonstration.Utilities;
import io.springbok.statefun.examples.demonstration.generated.SensorInfoMessage;
import org.hipparchus.util.FastMath;

import java.util.ArrayList;

public class SensorReader {

  // TODO: class reads file with sensor information
  public static ArrayList<String> getTestSensorInfo() {

    ArrayList<String> sensors = new ArrayList<>();

    String id = "0";
    double longitude = FastMath.toRadians(45.);
    double latitude = FastMath.toRadians(25.);
    double altitude = 0.;

    SensorInfoMessage sensorInfoMessage =
        SensorInfoMessage.newBuilder()
            .setSensorId(id)
            .setLongitude(longitude)
            .setLatitude(latitude)
            .setAltitude(altitude)
            .build();
    String stringSensor = Utilities.SensorInfoMessageToString(sensorInfoMessage);

    sensors.add(stringSensor);

    return sensors;
  }
}
