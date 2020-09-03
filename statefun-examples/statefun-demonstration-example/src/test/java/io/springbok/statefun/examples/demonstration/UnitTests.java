package io.springbok.statefun.examples.demonstration;

import org.apache.flink.statefun.flink.harness.Harness;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.analytical.tle.TLE;

import java.io.File;
import java.util.ArrayList;

/*
 NOTE: Tests assume that the delayed delete message in the OrbitStatefulFunction will be send after a 4 second delay
*/
public class UnitTests {

  static TrackGenerator trackGenerator;
  static String tlePath = "../../tle-data/globalstar_tles_05_18_2020.txt";

  @BeforeClass
  public static void setUp() throws Exception {
    trackGenerator = new TrackGenerator(tlePath);
    trackGenerator.init();
    trackGenerator.finitePropagation();
  }

  @Test
  public void testTrackCreation() throws Exception {

    TestTracksSourceFunction singleTracksSource =
        new TestTracksSourceFunction(trackGenerator.getXMessages(1));
    TestConsumer testConsumer = new TestConsumer();
    singleTracksSource.runTimeMS = 2000;
    OrbitStatefulFunction.deleteTimer = 1;

    Harness harness =
        new Harness()
            .withKryoMessageSerializer()
            .withFlinkSourceFunction(DemonstrationIO.TRACKS_INGRESS_ID, singleTracksSource)
            .withConsumingEgress(DemonstrationIO.DEFAULT_EGRESS_ID, testConsumer);
    harness.start();

    Assert.assertTrue(testConsumer.messages.get(0).equals("Created trackId 0"));
    Assert.assertTrue(testConsumer.messages.get(1).equals("Created track for id 0"));
    Assert.assertTrue(testConsumer.messages.get(2).equals("Created orbitId 0"));
    Assert.assertTrue(testConsumer.messages.get(3).equals("Created orbit for id 0"));
    Assert.assertTrue(testConsumer.messages.get(4).equals("Added orbitId 0 to trackId 0"));
    Assert.assertTrue(testConsumer.messages.get(5).equals("Saved orbitId 0"));
    Assert.assertTrue(testConsumer.messages.get(6).equals("Cleared orbit for id 0"));
    Assert.assertTrue(testConsumer.messages.get(7).equals("Cleared track for trackId 0"));
    Assert.assertTrue(testConsumer.messages.get(8).equals("Removed orbitId 0"));
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

    TestTracksSourceFunction finiteTracksSource =
        new TestTracksSourceFunction(trackGenerator.getXSingleObjectMessages(2));
    TestConsumer testConsumer = new TestConsumer();
    finiteTracksSource.runTimeMS = 8000;
    OrbitStatefulFunction.deleteTimer = 4;

    Harness harness =
        new Harness()
            .withKryoMessageSerializer()
            .withFlinkSourceFunction(DemonstrationIO.TRACKS_INGRESS_ID, finiteTracksSource)
            .withConsumingEgress(DemonstrationIO.DEFAULT_EGRESS_ID, testConsumer);
    harness.start();

    // Test correlation
    Assert.assertTrue(
        testConsumer.messages.contains("Correlated orbits with ids 1 and 0")
            || testConsumer.messages.contains("Correlated orbits with ids 0 and 1"));
    Assert.assertFalse(testConsumer.messages.contains("Correlated orbits with ids 0 and 0"));
    Assert.assertFalse(testConsumer.messages.contains("Correlated orbits with ids 1 and 1"));

    // Test track collection
    Assert.assertTrue(
        testConsumer.messages.contains("Added track with id 1 to collectedTracksMessage")
            || testConsumer.messages.contains("Added track with id 0 to collectedTracksMessage"));

    // Test new orbit creation flow
    Assert.assertTrue(
        testConsumer.messages.contains("Refined orbits with ids 1 and 0 to create orbit with id 2")
            || testConsumer.messages.contains(
                "Refined orbits with ids 0 and 1 to create orbit with id 2"));
    Assert.assertTrue(testConsumer.messages.contains("Added orbitId 2 to trackId 0"));
    Assert.assertTrue(testConsumer.messages.contains("Added orbitId 2 to trackId 1"));

    Assert.assertTrue(testConsumer.messages.contains("Cleared orbit for id 2"));
    Assert.assertTrue(testConsumer.messages.contains("Removed orbitId 2"));

    Assert.assertTrue(testConsumer.messages.contains("Cleared track for trackId 0"));
    Assert.assertTrue(testConsumer.messages.contains("Cleared track for trackId 1"));
  }

  @Test
  public void testTrackMessages() throws Exception {

    final File tleData = new File(tlePath);
    ArrayList<TLE> tles = TrackGenerator.convertTLES(tleData);
    tles.forEach(
        tle -> {
          int satelliteNumber = tle.getSatelliteNumber();
          String trackObject = trackGenerator.getMessagesById(satelliteNumber).get(0);
          Track track = Track.fromString(trackObject, "0");

          KeyedOrbit keyedOrbit = OrbitFactory.createOrbit(track, "0");
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

          // Assert true within 1000th of the value
          Assert.assertTrue("Semimajor Axis", tleA - tleA / 1000 < a && a < tleA + tleA / 1000);

          Assert.assertTrue(
              "Eccentricity",
              tle.getE() - tle.getE() / 1000 < e && e < tle.getE() + tle.getE() / 1000);

          Assert.assertTrue(
              "Inclination",
              tle.getI() - tle.getI() / 1000 < i && i < tle.getI() + tle.getI() / 1000);

          Assert.assertEquals(tle.getPerigeeArgument(), orbitPerigee, 0.0001);

          Assert.assertTrue(
              "RAAN",
              tle.getRaan() - tle.getRaan() / 1000 < raan
                  && raan < tle.getRaan() + tle.getRaan() / 1000);

          Assert.assertTrue(
              "Anomaly",
              tle.getMeanAnomaly() - tle.getMeanAnomaly() / 1000 < anomaly
                  && anomaly < tle.getMeanAnomaly() + tle.getMeanAnomaly() / 1000);
        });
  }
}
