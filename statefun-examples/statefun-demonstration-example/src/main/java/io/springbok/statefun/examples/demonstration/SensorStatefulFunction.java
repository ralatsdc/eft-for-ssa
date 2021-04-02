package io.springbok.statefun.examples.demonstration;

import io.springbok.statefun.examples.demonstration.generated.NewSatelliteMessage;
import io.springbok.statefun.examples.demonstration.generated.SensorInfoMessage;
import org.apache.flink.statefun.sdk.Context;
import org.apache.flink.statefun.sdk.FunctionType;
import org.apache.flink.statefun.sdk.StatefulFunction;
import org.apache.flink.statefun.sdk.annotations.Persisted;
import org.apache.flink.statefun.sdk.state.PersistedValue;

import java.util.ArrayList;

public class SensorStatefulFunction implements StatefulFunction {
  // This FunctionType binding is used in the Demonstration module
  public static final FunctionType TYPE = new FunctionType("springbok", "sensor-stateful-function");

  // PersistedValues can be stored and recalled when this StatefulFunction is invoked
  @Persisted
  private final PersistedValue<SensorInfoMessage> sensorInfoState =
      PersistedValue.of("fov-message", SensorInfoMessage.class);

  @Persisted
  private final PersistedValue<ArrayList> satelliteIds =
      PersistedValue.of("satellite-ids", ArrayList.class);

  @Override
  public void invoke(Context context, Object input) {

    // Message that indicates sensor creation
    // Create sensor and set values
    if (input instanceof SensorInfoMessage) {
      SensorInfoMessage sensorInfoMessage = (SensorInfoMessage) input;

      Utilities.log(
          context, String.format("Saved sensor with ID: %s", sensorInfoMessage.getSensorId()), 1);

      sensorInfoState.set(sensorInfoMessage);
    }

    // Message that comes from the SensorIdManager that a new satellite was created
    // Gives own information to requesting satellite and records satellite id
    if (input instanceof NewSatelliteMessage) {
      NewSatelliteMessage newSatelliteMessage = (NewSatelliteMessage) input;

      // Add new satellite to saved id list
      ArrayList<String> ids = satelliteIds.getOrDefault(new ArrayList<String>());
      ids.add(newSatelliteMessage.getId());
      satelliteIds.set(ids);
      Utilities.log(
          context,
          String.format(
              "Saved satellite with ID %s to Sensor with ID %s",
              context.self().id(), newSatelliteMessage.getId()),
          1);

      // Send sensor info to new satellite
      context.send(
          SatelliteStatefulFunction.TYPE, newSatelliteMessage.getId(), sensorInfoState.get());
    }
  }
}
