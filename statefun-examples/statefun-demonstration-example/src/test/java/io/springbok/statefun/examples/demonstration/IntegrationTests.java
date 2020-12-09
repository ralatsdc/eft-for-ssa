package io.springbok.statefun.examples.demonstration;

import io.springbok.statefun.examples.utilities.*;
import org.apache.flink.statefun.flink.harness.Harness;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.analytical.tle.TLE;

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Pattern;

/*
 NOTE: Tests assume that the delayed delete message in the OrbitStatefulFunction will be send after a 4 second delay
*/
public class IntegrationTests {

  static TrackGenerator trackGenerator;
  static ArrayList<TLE> tles;
  static ArrayList<String> sensors;

  @BeforeClass
  public static void setUp() throws Exception {
    ApplicationEnvironment.setPathProperties();
    String tlePath = System.getProperty("TLE_PATH");

    trackGenerator = new TrackGenerator();
    trackGenerator.init();
    trackGenerator.finitePropagation();
    final File tleData = new File(tlePath);
    tles = TLEReader.readTLEs(tleData);
    sensors = SensorReader.getTestSensorInfo();
  }

  @Ignore("Only one Harness can be run at a time")
  @Test
  public void testSatelliteCreation() throws Exception {

    // All ingress must be bound for harness to work; creating empty source
    MockTracksSourceFunction singleTracksSource = new MockTracksSourceFunction();
    MockSensorSourceFunction sensorSourceFunction = new MockSensorSourceFunction();

    // Specific source function for testing
    ArrayList<TLE> testTLES = new ArrayList<>();
    testTLES.add(tles.get(0));
    MockTLESourceFunction TLESource = new MockTLESourceFunction(testTLES);
    MockConsumer testConsumer = new MockConsumer();
    TLESource.runTimeMS = 5000;
    OrbitStatefulFunction.deleteTimer = 1;

    Harness harness =
        new Harness()
            .withKryoMessageSerializer()
            .withFlinkSourceFunction(DemonstrationIO.TLE_INGRESS_ID, TLESource)
            .withFlinkSourceFunction(DemonstrationIO.SENSOR_INGRESS_ID, sensorSourceFunction)
            .withFlinkSourceFunction(DemonstrationIO.TRACKS_INGRESS_ID, singleTracksSource)
            .withConsumingEgress(DemonstrationIO.DEFAULT_EGRESS_ID, testConsumer);
    harness.start();

    Assert.assertTrue(testConsumer.messages.get(0).contains("Saved orbit with satellite ID:"));
  }

  @Ignore("Only one Harness can be run at a time")
  @Test
  public void testSensorReading() throws Exception {

    // All ingress must be bound for harness to work; creating empty source
    MockTracksSourceFunction singleTracksSource = new MockTracksSourceFunction();

    // Specific source function for testing
    ArrayList<TLE> testTLES = new ArrayList<>();
    testTLES.add(tles.get(0));
    MockTLESourceFunction TLESource = new MockTLESourceFunction(testTLES);

    ArrayList<String> testSensors = new ArrayList<>();
    testSensors.add(sensors.get(0));
    MockSensorSourceFunction sensorSourceFunction = new MockSensorSourceFunction(testSensors);

    MockConsumer testConsumer = new MockConsumer();
    TLESource.runTimeMS = 5000;
    OrbitStatefulFunction.deleteTimer = 1;

    Harness harness =
        new Harness()
            .withKryoMessageSerializer()
            .withFlinkSourceFunction(DemonstrationIO.SENSOR_INGRESS_ID, sensorSourceFunction)
            .withFlinkSourceFunction(DemonstrationIO.TLE_INGRESS_ID, TLESource)
            .withFlinkSourceFunction(DemonstrationIO.TRACKS_INGRESS_ID, singleTracksSource)
            .withConsumingEgress(DemonstrationIO.DEFAULT_EGRESS_ID, testConsumer);
    harness.start();

    Assert.assertTrue(
        arrayListContainsInclusive(testConsumer.messages, "Saved orbit with satellite ID:"));
    Assert.assertTrue(arrayListContainsInclusive(testConsumer.messages, "Saved sensor with ID: 0"));
    Assert.assertTrue(
        arrayListContainsInclusive(
            testConsumer.messages, "Saved satellite with ID 0 to Sensor with ID"));
  }

  @Ignore("Only one Harness can be run at a time")
  @Test
  public void testMultipleSensors() throws Exception {

    // All ingress must be bound for harness to work; creating empty source
    MockTracksSourceFunction singleTracksSource = new MockTracksSourceFunction();

    // Specific source function for testing
    ArrayList<TLE> testTLES = new ArrayList<>();
    testTLES.add(tles.get(0));
    MockTLESourceFunction TLESource = new MockTLESourceFunction(testTLES);

    ArrayList<String> testSensors = new ArrayList<>();
    testSensors.add(sensors.get(0));
    testSensors.add(sensors.get(1));
    MockSensorSourceFunction sensorSourceFunction = new MockSensorSourceFunction(testSensors);

    MockConsumer testConsumer = new MockConsumer();
    TLESource.runTimeMS = 5000;
    OrbitStatefulFunction.deleteTimer = 1;

    Harness harness =
        new Harness()
            .withKryoMessageSerializer()
            .withFlinkSourceFunction(DemonstrationIO.SENSOR_INGRESS_ID, sensorSourceFunction)
            .withFlinkSourceFunction(DemonstrationIO.TLE_INGRESS_ID, TLESource)
            .withFlinkSourceFunction(DemonstrationIO.TRACKS_INGRESS_ID, singleTracksSource)
            .withConsumingEgress(DemonstrationIO.DEFAULT_EGRESS_ID, testConsumer);
    harness.start();

    Assert.assertTrue(
        arrayListContainsInclusive(testConsumer.messages, "Saved orbit with satellite ID:"));
    Assert.assertTrue(arrayListContainsInclusive(testConsumer.messages, "Saved sensor with ID: 0"));
    Assert.assertTrue(
        arrayListContainsInclusive(
            testConsumer.messages, "Saved satellite with ID 0 to Sensor with ID"));
  }

  @Ignore("Only one Harness can be run at a time")
  @Test
  public void testTrackCreation() throws Exception {

    // All ingress must be bound for harness to work; creating empty source
    MockTLESourceFunction TLESource = new MockTLESourceFunction();
    MockSensorSourceFunction sensorSourceFunction = new MockSensorSourceFunction();

    // Specific source function for testing
    MockTracksSourceFunction singleTracksSource = new MockTracksSourceFunction();
    new MockTracksSourceFunction(trackGenerator.getXMessages(1));
    MockConsumer testConsumer = new MockConsumer();
    singleTracksSource.runTimeMS = 5000;
    OrbitStatefulFunction.deleteTimer = 1;

    Harness harness =
        new Harness()
            .withKryoMessageSerializer()
            .withFlinkSourceFunction(DemonstrationIO.TRACKS_INGRESS_ID, singleTracksSource)
            .withFlinkSourceFunction(DemonstrationIO.SENSOR_INGRESS_ID, sensorSourceFunction)
            .withFlinkSourceFunction(DemonstrationIO.TLE_INGRESS_ID, TLESource)
            .withConsumingEgress(DemonstrationIO.DEFAULT_EGRESS_ID, testConsumer);
    harness.start();

    Assert.assertTrue(testConsumer.messages.get(0).contains("Created trackId 0"));
    Assert.assertTrue(testConsumer.messages.get(1).contains("Created track for id 0"));
    Assert.assertTrue(testConsumer.messages.get(2).contains("Created orbitId 0"));
    Assert.assertTrue(testConsumer.messages.get(3).contains("Created orbit for id 0"));
    Assert.assertTrue(testConsumer.messages.get(4).contains("Added orbitId 0 to trackId 0"));
    Assert.assertTrue(testConsumer.messages.get(5).contains("Saved orbitId 0"));
    Assert.assertTrue(testConsumer.messages.get(6).contains("Cleared orbit for id 0"));
    Assert.assertTrue(testConsumer.messages.get(7).contains("Cleared track for trackId 0"));
    Assert.assertTrue(testConsumer.messages.get(8).contains("Removed orbitId 0"));
  }

  @Test
  public void testTracksGeneratorProvidesSingleObject() {
    ArrayList<String> singleObjectMessages = trackGenerator.getSingleObjectMessages();
    ArrayList<Track> tracks = new ArrayList<>();

    singleObjectMessages.forEach(
        message -> {
          // Track ID won't be used
          tracks.add(Track.fromString(message, "Unused"));
        });
    int trackObjectID = tracks.get(0).objectId;

    // Test that each track is from the same object
    tracks.forEach(
        track -> {
          Assert.assertTrue(track.objectId == trackObjectID);
        });
  }

  @Test
  public void testOrbitCorrelation() throws Exception {

    // All ingress must be bound for harness to work; creating empty source
    MockTLESourceFunction TLESource = new MockTLESourceFunction();
    MockSensorSourceFunction sensorSourceFunction = new MockSensorSourceFunction();

    // Specific source function for testing
    MockTracksSourceFunction finiteTracksSource =
        new MockTracksSourceFunction(trackGenerator.getXSingleObjectMessages(2));
    MockConsumer testConsumer = new MockConsumer();
    finiteTracksSource.runTimeMS = 8000;
    OrbitStatefulFunction.deleteTimer = 4;

    Harness harness =
        new Harness()
            .withKryoMessageSerializer()
            .withFlinkSourceFunction(DemonstrationIO.TRACKS_INGRESS_ID, finiteTracksSource)
            .withFlinkSourceFunction(DemonstrationIO.SENSOR_INGRESS_ID, sensorSourceFunction)
            .withFlinkSourceFunction(DemonstrationIO.TLE_INGRESS_ID, TLESource)
            .withConsumingEgress(DemonstrationIO.DEFAULT_EGRESS_ID, testConsumer);
    harness.start();

    // Test correlation
    Assert.assertTrue(
        IntegrationTests.arrayListContainsInclusive(
                testConsumer.messages, "Correlated orbits with ids 1 and 0")
            || IntegrationTests.arrayListContainsInclusive(
                testConsumer.messages, "Correlated orbits with ids 0 and 1"));
    Assert.assertFalse(
        IntegrationTests.arrayListContainsInclusive(
            testConsumer.messages, "Correlated orbits with ids 0 and 0"));
    Assert.assertFalse(
        IntegrationTests.arrayListContainsInclusive(
            testConsumer.messages, "Correlated orbits with ids 1 and 1"));

    // Test track collection
    Assert.assertTrue(
        IntegrationTests.arrayListContainsInclusive(
                testConsumer.messages, "Added track with id 1 to collectedTracksMessage")
            || IntegrationTests.arrayListContainsInclusive(
                testConsumer.messages, "Added track with id 0 to collectedTracksMessage"));

    // Test new orbit creation flow
    Assert.assertTrue(
        IntegrationTests.arrayListContainsInclusive(
            testConsumer.messages, "Refined orbits with ids"));

    Assert.assertTrue(
        IntegrationTests.arrayListContainsInclusive(
            testConsumer.messages, "Added orbitId 2 to trackId 0"));
    Assert.assertTrue(
        IntegrationTests.arrayListContainsInclusive(
            testConsumer.messages, "Added orbitId 2 to trackId 1"));

    Assert.assertTrue(
        IntegrationTests.arrayListContainsInclusive(
            testConsumer.messages, "Cleared orbit for id 2"));
    Assert.assertTrue(
        IntegrationTests.arrayListContainsInclusive(testConsumer.messages, "Removed orbitId 2"));

    Assert.assertTrue(
        IntegrationTests.arrayListContainsInclusive(
            testConsumer.messages, "Cleared track for trackId 0"));
    Assert.assertTrue(
        IntegrationTests.arrayListContainsInclusive(
            testConsumer.messages, "Cleared track for trackId 1"));
  }

  @Test
  public void testTrackMessages() throws Exception {

    final File tleData = new File(System.getProperty("TLE_PATH"));
    ArrayList<TLE> tles = TLEReader.readTLEs(tleData);
    tles.forEach(
        tle -> {
          int satelliteNumber = tle.getSatelliteNumber();
          String trackObject = trackGenerator.getMessagesById(satelliteNumber).get(0);
          Track track = Track.fromString(trackObject, "0");

          KeyedOrbit keyedOrbit = OrbitFactory.createKeyedOrbit(track, "0");
          KeplerianOrbit orbit = (KeplerianOrbit) keyedOrbit.orbit;

          double a = orbit.getA();
          double e = orbit.getE();
          double i = orbit.getI();
          double orbitPerigee = orbit.getPerigeeArgument();
          if (orbitPerigee < 0) {
            orbitPerigee = orbitPerigee + 2 * Math.PI;
          }
          double raan = orbit.getRightAscensionOfAscendingNode();
          if (raan < 0) {
            raan = raan + 2 * Math.PI;
          }
          double anomaly = orbit.getAnomaly(PositionAngle.MEAN);
          if (anomaly < 0) {
            anomaly = anomaly + 2 * Math.PI;
          }

          final double tleA =
              (Math.cbrt(OrbitFactory.mu))
                  / (Math.cbrt(Math.pow(tle.getMeanMotion(), 2))); // semi major axis in M

          // Assert true within epsilon
          Assert.assertEquals(tleA, a, 10);

          Assert.assertEquals(tle.getE(), e, 0.0001);

          Assert.assertEquals(tle.getI(), i, 0.0001);

          Assert.assertEquals(tle.getPerigeeArgument(), orbitPerigee, 0.0001);

          Assert.assertEquals(tle.getRaan(), raan, 0.0001);

          Assert.assertEquals(tle.getMeanAnomaly(), anomaly, 0.0001);
        });
  }

  static boolean arrayListContainsInclusive(ArrayList<String> list, String string) {

    String regex = "(.*)" + string + "(.*)";
    Pattern p = Pattern.compile(regex);

    for (String s : list) {
      if (p.matcher(s).matches()) {
        return true;
      }
    }
    return false;
  }
}
