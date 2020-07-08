package io.springbok.statefun.examples.demonstration;

import io.springbok.statefun.examples.demonstration.generated.TrackIn;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.flink.statefun.flink.harness.Harness;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.PrintStream;

/** Unit test for simple App. */
public class UnitTests {

  private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
  private final PrintStream systemOut = System.out;
  private SourceFunction<TrackIn> finiteTracksSource;

  TestConsumer testConsumer;
  TrackReader trackReader;
  MockConsumer<String, String> mockConsumer;

  @Before
  public void setUp() throws IOException {
    trackReader = new TrackReader();
    mockConsumer = new MockConsumer<String, String>(OffsetResetStrategy.EARLIEST);
  }

  @Test
  public void testTrackCreation() throws Exception {

    TestTracksSourceFunction finiteTracksSource = new TestTracksSourceFunction(trackReader.getXTracks(1));
    testConsumer = new TestConsumer();

    Harness harness =
            new Harness()
                    .withKryoMessageSerializer()
                    .withFlinkSourceFunction(DemonstrationIO.TRACKS_INGRESS_ID, finiteTracksSource)
                    .withConsumingEgress(DemonstrationIO.DEFAULT_EGRESS_ID, testConsumer);
    harness.start();

    Assert.assertTrue(testConsumer.messages.contains("Created trackId 0"));
    Assert.assertTrue(testConsumer.messages.contains("Created track for id 0"));
    Assert.assertTrue(testConsumer.messages.contains("Created orbitId 0"));
    Assert.assertTrue(testConsumer.messages.contains("Created orbit for id 0"));
    Assert.assertTrue(testConsumer.messages.contains("Added orbitId 0 to trackId 0"));
    Assert.assertTrue(testConsumer.messages.contains("Cleared orbit for id 0"));
    Assert.assertTrue(testConsumer.messages.contains("Cleared track for trackId 0"));
    Assert.assertTrue(testConsumer.messages.contains("Removed orbitId 0"));
  }
}
