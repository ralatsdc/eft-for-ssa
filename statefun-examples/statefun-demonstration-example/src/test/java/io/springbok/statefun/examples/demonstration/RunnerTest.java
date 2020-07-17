package io.springbok.statefun.examples.demonstration;

import io.springbok.statefun.examples.demonstration.generated.TrackIn;
import org.apache.flink.statefun.flink.harness.Harness;
import org.apache.flink.statefun.flink.harness.io.SerializableSupplier;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.orekit.propagation.analytical.tle.TLE;

import java.util.Iterator;

/** Unit test for simple App. */
public class RunnerTest {

  static TrackGenerator trackGenerator;
  private static Iterator<TLE> tleIterator;
  private static Double timePassed = 0.;
  private static Double stepSize;

  @Before
  public void setUp() throws Exception {
    trackGenerator = new TrackGenerator("../../tle-data/globalstar_tles_05_18_2020.txt");
    trackGenerator.init();
    tleIterator = trackGenerator.tles.iterator();
    stepSize = trackGenerator.largeStep;
  }

  @Test
  @Ignore
  public void run() throws Exception {
    Harness harness =
        new Harness()
            .withKryoMessageSerializer()
            .withSupplyingIngress(DemonstrationIO.TRACKS_INGRESS_ID, new InfiniteTracksSource())
            .withPrintingEgress(DemonstrationIO.DEFAULT_EGRESS_ID);

    harness.start();
  }

  private static final class InfiniteTracksSource implements SerializableSupplier<TrackIn> {

    private static final long serialVersionUID = 1;

    @Override
    public TrackIn get() {
      try {
        Thread.sleep(3000);
      } catch (InterruptedException e) {
        throw new RuntimeException("Interrupted", e);
      }

      if (!tleIterator.hasNext()) {
        // Start from beginning
        tleIterator = trackGenerator.tles.iterator();
        timePassed = timePassed + stepSize;
      }
      TLE tle = tleIterator.next();

      String message = trackGenerator.propagate(tle, timePassed);

      System.out.println("Sent Message: " + message);
      TrackIn trackIn = TrackIn.newBuilder().setTrack(message).build();
      return trackIn;
    }
  }
}
