package io.springbok.statefun.examples.demonstration;

import io.springbok.statefun.examples.demonstration.generated.DelayedWakeUpMessage;
import io.springbok.statefun.examples.demonstration.generated.NewSatelliteMessage;
import io.springbok.statefun.examples.demonstration.generated.SensorInfoMessage;
import io.springbok.statefun.examples.demonstration.generated.SingleLineTLE;
import io.springbok.statefun.examples.utilities.TLEReader;
import org.apache.flink.statefun.sdk.Context;
import org.apache.flink.statefun.sdk.FunctionType;
import org.apache.flink.statefun.sdk.StatefulFunction;
import org.apache.flink.statefun.sdk.annotations.Persisted;
import org.apache.flink.statefun.sdk.state.PersistedValue;
import org.hipparchus.ode.events.Action;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.events.ElevationDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

import java.time.Duration;
import java.util.ArrayList;

public class SatelliteStatefulFunction implements StatefulFunction {
  // This FunctionType binding is used in the Demonstration module
  public static final FunctionType TYPE =
      new FunctionType("springbok", "satellite-stateful-function");

  // PersistedValues can be stored and recalled when this StatefulFunction is invoked
  @Persisted
  private final PersistedValue<Orbit> orbitState = PersistedValue.of("orbit", Orbit.class);
  // Store list of sensor information
  @Persisted
  private final PersistedValue<ArrayList> sensorVisibilityStates =
      PersistedValue.of("sensor-list", ArrayList.class);

  @Override
  public void invoke(Context context, Object input) {

    // OrbitFactory.init() ensures Orekit data is loaded into the current context
    // Not in try block since orekit must be loaded for project to work
    OrbitFactory.init();

    // TLE is from reading the source file
    if (input instanceof SingleLineTLE) {

      try {
        // Save new satellite information
        SingleLineTLE singleLineTLE = (SingleLineTLE) input;
        TLE tle = TLEReader.fromSingleLineTLE(singleLineTLE);
        Orbit orbit = OrbitFactory.createOrbit(tle);

        orbitState.set(orbit);
        sendWakeUpMessage(context);

        // TODO: Calculate whether satellite can be seen by leo, meo, geo sensors
        // Send message to SensorIdManager that new SatelliteStatefulFunction was created

        NewSatelliteMessage newSatelliteMessage =
            NewSatelliteMessage.newBuilder()
                .setId(context.self().id())
                .setLeo(true)
                .setMeo(true)
                .setGeo(true)
                .build();

        // Send a message to the SensorIdManager to forward to appropriate sensors
        context.send(SensorIdManager.TYPE, "sensor-id-manager", newSatelliteMessage);

        Utilities.log(
            context, String.format("Saved orbit with satellite ID: %s", context.self().id()), 1);
      } catch (Exception e) {
        Utilities.log(
            context,
            String.format(
                "Failed to save orbit with satellite ID: %s. Exception: %s",
                context.self().id(), e),
            1);
      }
    }

    // TODO: check sensor to see if it's seeable - send messages to SensorStatefulFunction - will
    // send back message if it's seeable
    // Not sure what makes the most sense to distribute this - don't want to bog down the sensor
    // statefulfunctions every time a satellite wakes up

    // Maybe there's a calculated idea of when it'll be in view that the satellite can just know -
    // and it'll only have to send a message initially to get that object and maybe periodically to
    // make sure it's in sync
    // TODO: save current state
    if (input instanceof DelayedWakeUpMessage) {

      try {
        sendWakeUpMessage(context);

        Utilities.log(
            context,
            String.format("Satellite with ID %s is checking sensors", context.self().id()),
            1);

        ArrayList<EventDetector> sensorVisibilities =
            sensorVisibilityStates.getOrDefault(new ArrayList<EventDetector>());

        Propagator kepler = new KeplerianPropagator(orbitState.get());

        sensorVisibilities.forEach(
            sensorVisibility -> {
              kepler.addEventDetector(sensorVisibility);
            });

        // Propagate to trigger event only
        // Duration in seconds - 24 hour periods. Propagation will automatically stop once
        // visibility is determined, and will not continue for the entire duration
        SpacecraftState finalState =
            kepler.propagate(new AbsoluteDate(orbitState.get().getDate(), 86400.));

        Utilities.log(
            context,
            String.format(
                "Satellite State %s: %s\n\t\t\t\tTime: %s",
                context.self().id(), orbitState.get(), orbitState.get().getDate()),
            1);
      } catch (Exception e) {
        Utilities.log(
            context,
            String.format(
                "Satellite with ID %s failed to wake up: Exception: %s", context.self().id(), e),
            1);
      }
    }

    // Adding sensor information to Satellite
    if (input instanceof SensorInfoMessage) {
      SensorInfoMessage sensorInfoMessage = (SensorInfoMessage) input;

      // TODO: add simple check to see if this sensor is the right type

      GeodeticPoint sensor =
          new GeodeticPoint(
              sensorInfoMessage.getLatitude(),
              sensorInfoMessage.getLongitude(),
              sensorInfoMessage.getAltitude());

      Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
      BodyShape earth =
          new OneAxisEllipsoid(
              Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
              Constants.WGS84_EARTH_FLATTENING,
              earthFrame);
      TopocentricFrame sensorFrame =
          new TopocentricFrame(earth, sensor, sensorInfoMessage.getSensorId());

      // TODO: determine use of these values
      // Maximum checking interval (seconds)
      double maxcheck = 60.0;
      // Maximum divergence threshold (seconds)
      double threshold = 0.001;
      // Min elevation for detection (rad)
      double elevation = FastMath.toRadians(5.);
      // TODO: handle logic of sending out tracks in this lambda function
      EventDetector sensorVisibility =
          new ElevationDetector(maxcheck, threshold, sensorFrame)
              .withConstantElevation(elevation)
              .withHandler(
                  (s, detector, increasing) -> {
                    System.out.println(
                        " Visibility on "
                            + detector.getTopocentricFrame().getName()
                            + (increasing ? " begins at " : " ends at ")
                            + s.getDate());
                    System.out.println(detector.getTopocentricFrame().getZenith());
                    System.out.println(s.getPVCoordinates().toString());
                    return increasing ? Action.CONTINUE : Action.STOP;
                  });

      ArrayList<EventDetector> sensorVisibilities =
          sensorVisibilityStates.getOrDefault(new ArrayList<EventDetector>());

      sensorVisibilities.add(sensorVisibility);

      sensorVisibilityStates.set(sensorVisibilities);
    }
  }

  // Sends a delete message after a certain amount of time
  private void sendWakeUpMessage(Context context) throws Exception {

    long wakeupInterval = ApplicationProperties.getWakeupInterval();

    context.sendAfter(
        Duration.ofSeconds(wakeupInterval),
        context.self(),
        DelayedWakeUpMessage.newBuilder().build());
  }
}
