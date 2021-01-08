package io.springbok.statefun.examples.demonstration;

import io.springbok.statefun.examples.demonstration.generated.*;
import io.springbok.statefun.examples.utilities.TLEReader;
import io.springbok.statefun.examples.utilities.TrackGenerator;
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
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

import java.util.ArrayList;

public class SatelliteStatefulFunction implements StatefulFunction {
  // This FunctionType binding is used in the Demonstration module
  public static final FunctionType TYPE =
      new FunctionType("springbok", "satellite-stateful-function");

  // PersistedValues can be stored and recalled when this StatefulFunction is invoked
  @Persisted private PersistedValue<TLE> tleState = PersistedValue.of("tle", TLE.class);
  // PersistedValues can be stored and recalled when this StatefulFunction is invoked
  @Persisted private PersistedValue<Orbit> orbitState = PersistedValue.of("orbit", Orbit.class);
  // Store list of sensor information
  @Persisted
  private final PersistedValue<ArrayList> sensorVisibilityStates =
      PersistedValue.of("sensor-list", ArrayList.class);

  @Persisted
  private final PersistedValue<AbsoluteDate> nextEvent =
      PersistedValue.of("next-event", AbsoluteDate.class);

  @Persisted
  private final PersistedValue<Boolean> hasDecreased =
      PersistedValue.of("has-decreased", Boolean.class);

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

        tleState.set(tle);

        Orbit orbit = OrbitFactory.createOrbit(tle);

        orbitState.set(orbit);

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
            context, String.format("Saved orbit with satellite ID %s", context.self().id()), 1);
      } catch (Exception e) {
        Utilities.log(
            context,
            String.format(
                "Failed to save orbit with satellite ID %s. Exception: %s", context.self().id(), e),
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
    if (input instanceof GetNextEventMessage) {

      GetNextEventMessage getNextEventMessage = (GetNextEventMessage) input;

      AbsoluteDate startDate;

      if (getNextEventMessage.getTime().isEmpty()) {
        startDate = orbitState.get().getDate();
      } else {
        startDate = new AbsoluteDate(getNextEventMessage.getTime(), TimeScalesFactory.getUTC());
      }
      getNextEvent(context, startDate);
    }

    // create track from this Satellite
    if (input instanceof FireEventMessage) {
      try {
        FireEventMessage fireEventMessage = (FireEventMessage) input;

        AbsoluteDate eventTime =
            new AbsoluteDate(fireEventMessage.getTime(), TimeScalesFactory.getUTC());

        getNextEvent(context, eventTime);

        Utilities.log(
            context, String.format("Satellite with ID %s created track", context.self().id()), 1);

        TrackGenerator trackGenerator = new TrackGenerator();
        trackGenerator.init();

        // TODO: get correct sensorId
        String track = trackGenerator.produceTrackAtTime(tleState.get(), eventTime, "1");

        if (ApplicationProperties.getIsTest()) {
          TrackIn trackIn = TrackIn.newBuilder().setTrack(track).build();
          context.send(TrackIdManager.TYPE, "track-id-manager", trackIn);
        } else {
          Utilities.sendKafkaMessage("tracks", track);
        }
      } catch (Exception e) {

        Utilities.log(
            context,
            String.format("Satellite with id %s cannot form track: \n %s", context.self().id(), e),
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
      double maxcheck = 60.0;
      double threshold = 0.001;
      double elevation = FastMath.toRadians(5.);
      // TODO: handle logic of sending out tracks in this lambda function
      EventDetector sensorVisibility =
          new ElevationDetector(maxcheck, threshold, sensorFrame)
              .withConstantElevation(elevation)
              .withHandler(
                  (s, detector, increasing) -> {
                    // TODO: Handle instances where the orbit is currently visible
                    // Currently, if it's visible the first time
                    // TODO: handle correctly getting next event for multiple satellites + getting
                    // sensor ID
                    if (increasing) {
                      nextEvent.set(s.getDate());
                    }
                    // Stops the simulation once it finds first visibility after current visibility
                    if (!increasing) {
                      hasDecreased.set(true);
                    }
                    if (hasDecreased.getOrDefault(false)) {
                      return increasing ? Action.STOP : Action.CONTINUE;
                    } else {
                      return Action.CONTINUE;
                    }
                  });

      ArrayList<EventDetector> sensorVisibilities =
          sensorVisibilityStates.getOrDefault(new ArrayList<EventDetector>());

      sensorVisibilities.add(sensorVisibility);

      // Send a message to the event manager that a new object has been created - this is handled
      // when the first sensor is registered, as that is the first time the
      // SatelliteStatefulFunction is capable of generating events.
      if (sensorVisibilities.size() == 1) {
        NewEventSourceMessage newEventSourceMessage =
            NewEventSourceMessage.newBuilder().setId(context.self().id()).build();

        context.send(EventManager.TYPE, "event-manager", newEventSourceMessage);
      }

      Utilities.log(
          context,
          String.format(
              "Added sensor with ID %s to Satellite with ID %s",
              sensorInfoMessage.getSensorId(), context.self().id()),
          1);

      sensorVisibilityStates.set(sensorVisibilities);
    }
  }

  private void getNextEvent(Context context, AbsoluteDate startDate) {

    try {

      Utilities.log(
          context,
          String.format("Satellite with ID %s is checking sensors", context.self().id()),
          1);

      ArrayList<EventDetector> sensorVisibilities =
          sensorVisibilityStates.getOrDefault(new ArrayList<EventDetector>());

      // log current time
      Utilities.log(context, String.format("Current time: %s", startDate), 1);

      hasDecreased.set(false);

      // Advance satellite to current time - this will be saved to avoid extra propagation in the
      // future
      Propagator initialKeplar = new KeplerianPropagator(orbitState.get());
      SpacecraftState currentState = initialKeplar.propagate(startDate);

      Propagator kepler = new KeplerianPropagator(currentState.getOrbit());

      sensorVisibilities.forEach(
          sensorVisibility -> {
            kepler.addEventDetector(sensorVisibility);
          });

      SpacecraftState finalState =
          kepler.propagate(new AbsoluteDate(currentState.getDate(), 86400.));

      Utilities.log(context, String.format("Next Visibility: %s", nextEvent.get()), 1);

      // Send next event back to event handler, and handle the event
      NewEventMessage newEventMessage =
          NewEventMessage.newBuilder()
              .setObjectId(context.self().id())
              .setTime(nextEvent.get().toString())
              .build();

      context.send(EventManager.TYPE, "event-manager", newEventMessage);

      orbitState.set(currentState.getOrbit());
    } catch (Exception e) {
      Utilities.log(
          context,
          String.format(
              "Satellite with ID %s failed to wake up. Exception: %s", context.self().id(), e),
          1);
    }
  }
}
