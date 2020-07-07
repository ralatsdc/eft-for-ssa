package io.springbok.statefun.examples.demonstration;

import io.springbok.statefun.examples.demonstration.generated.TrackIn;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.flink.statefun.flink.harness.Harness;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.PrintStream;

/** Unit test for simple App. */
public class UnitTests {

  private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
  private final PrintStream systemOut = System.out;
  private SourceFunction<TrackIn> finiteTracksSource;

  KafkaTestConsumer testConsumer;
  TrackReader trackReader;

  @Before
  public void setUp() throws IOException {
    testConsumer = new KafkaTestConsumer();
    trackReader = new TrackReader();
  }

  @Test
  public void testTrackCreation() throws Exception {

    TestTracksSourceFunction finiteTracksSource = new TestTracksSourceFunction(trackReader.getXTracks(1));

    Harness harness =
            new Harness()
                    .withKryoMessageSerializer()
                    .withFlinkSourceFunction(DemonstrationIO.TRACKS_INGRESS_ID, finiteTracksSource)
                    .withConsumingEgress(DemonstrationIO.DEFAULT_EGRESS_ID, testConsumer);
    harness.start();

    // Test, Somehow
    // testConsumer contains what we expect
    // Would it be useful to create a testing egress? Or are the logs sufficient?
    //Assert.that();
  }

}
