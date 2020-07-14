package io.springbok.statefun.examples.demonstration;

import io.springbok.statefun.examples.demonstration.generated.TrackIn;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.flink.statefun.flink.harness.Harness;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.PrintStream;
import java.util.ArrayList;

/** Unit test for simple App. */
public class UnitTests {

  private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
  private final PrintStream systemOut = System.out;
  private SourceFunction<TrackIn> finiteTracksSource;

  TestConsumer testConsumer;
  TrackGenerator trackGenerator;

  @Before
  public void setUp() throws Exception {
    trackGenerator = new TrackGenerator("../../tle-data/globalstar_tles_05_18_2020.txt");
    trackGenerator.init();
  }

  @Test
  public void testTrackCreation() throws Exception {

    TestTracksSourceFunction finiteTracksSource =
        new TestTracksSourceFunction(trackGenerator.getXMessages(1));
    testConsumer = new TestConsumer();

    Harness harness =
        new Harness()
            .withKryoMessageSerializer()
            .withFlinkSourceFunction(DemonstrationIO.TRACKS_INGRESS_ID, finiteTracksSource)
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
    testConsumer = new TestConsumer();

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
        testConsumer.messages.contains(
                "Added track with id 1 to collectedTracksMessage with orbit ids 0 and 1")
            || testConsumer.messages.contains(
                "Added track with id 1 to collectedTracksMessage with orbit ids 1 and 0")
            || testConsumer.messages.contains(
                "Added track with id 0 to collectedTracksMessage with orbit ids 0 and 1")
            || testConsumer.messages.contains(
                "Added track with id 0 to collectedTracksMessage with orbit ids 1 and 0"));

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
}
