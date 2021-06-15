package io.springbok.statefun.examples.demonstration;

import io.springbok.statefun.examples.demonstration.generated.NewSatelliteMessage;
import io.springbok.statefun.examples.demonstration.generated.SensorIn;
import io.springbok.statefun.examples.demonstration.generated.SensorInfoMessage;
import org.apache.flink.statefun.sdk.Context;
import org.apache.flink.statefun.sdk.FunctionType;
import org.apache.flink.statefun.sdk.StatefulFunction;
import org.apache.flink.statefun.sdk.annotations.Persisted;
import org.apache.flink.statefun.sdk.state.PersistedValue;

import java.util.ArrayList;

public class SensorIdManager implements StatefulFunction {

  // This FunctionType binding is used in the Demonstration module
  public static final FunctionType TYPE =
      new FunctionType("springbok", "sensor-id-stateful-function");

  // PersistedValues can be stored and recalled when this StatefulFunction is invoked
  @Persisted
  private final PersistedValue<ArrayList> satelliteIdsState =
      PersistedValue.of("satellite-ids", ArrayList.class);

  @Persisted
  private final PersistedValue<ArrayList> leoSensorIdsState =
      PersistedValue.of("leo-sensors", ArrayList.class);

  @Persisted
  private final PersistedValue<ArrayList> meoSensorIdsState =
      PersistedValue.of("meo-sensors", ArrayList.class);

  @Persisted
  private final PersistedValue<ArrayList> geoSensorIdsState =
      PersistedValue.of("geo-sensors", ArrayList.class);

  @Override
  public void invoke(Context context, Object input) {

    // Register new sensor with manager and forward to SensorStatefulFunction
    if (input instanceof SensorIn) {
      SensorIn sensorIn = (SensorIn) input;

      SensorInfoMessage sensorInfoMessage =
          Utilities.StringToSensorInfoMessage(sensorIn.getSensor());

      // TODO: determine if sensor is leo meo geo and add switch
      ArrayList<String> sensors = geoSensorIdsState.getOrDefault(ArrayList::new);
      sensors.add(sensorInfoMessage.getSensorId());

      // TODO: add check (in SatelliteStatefulFunction) to ensure sensor is right type for that
      // satellite
      sendToSatellites(context, satelliteIdsState.getOrDefault(ArrayList::new), sensorInfoMessage);

      context.send(SensorStatefulFunction.TYPE, sensorInfoMessage.getSensorId(), sensorInfoMessage);
      Utilities.log(
          context,
          String.format(
              "Added Sensor with ID %s to SensorIdManager", sensorInfoMessage.getSensorId()),
          1);

      geoSensorIdsState.set(sensors);
    }
    // Message that comes from the SatelliteStatefulFunction that a new Satellite was created with
    // information about sensor type
    if (input instanceof NewSatelliteMessage) {
      // Sends message to appropriate satellite groups
      NewSatelliteMessage newSatelliteMessage = (NewSatelliteMessage) input;

      if (newSatelliteMessage.getLeo()) {
        // Send to leo sensors
        ArrayList<String> leoSensors = leoSensorIdsState.getOrDefault(new ArrayList());

        sendToSensors(context, leoSensors, newSatelliteMessage);
      }
      if (newSatelliteMessage.getMeo()) {
        // Send to meo sensors
        ArrayList<String> meoSensors = meoSensorIdsState.getOrDefault(new ArrayList());

        sendToSensors(context, meoSensors, newSatelliteMessage);
      }
      if (newSatelliteMessage.getGeo()) {
        // Send to geo sensors
        ArrayList<String> geoSensors = geoSensorIdsState.getOrDefault(new ArrayList());

        sendToSensors(context, geoSensors, newSatelliteMessage);
      }
    }
  }

  private void sendToSensors(
      Context context, ArrayList<String> sensorIds, NewSatelliteMessage newSatelliteMessage) {

    // Send new satellite id to all existing sensors to save
    sensorIds.forEach(
        id -> {
          context.send(SensorStatefulFunction.TYPE, id, newSatelliteMessage);
        });
  }

  private void sendToSatellites(
      Context context, ArrayList<String> satelliteIds, SensorInfoMessage sensorInfoMessage) {

    // Send new satellite id to all existing sensors to save
    satelliteIds.forEach(
        id -> {
          context.send(SatelliteStatefulFunction.TYPE, id, sensorInfoMessage);
        });
  }
}
