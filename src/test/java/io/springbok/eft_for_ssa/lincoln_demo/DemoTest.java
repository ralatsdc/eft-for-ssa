package io.springbok.eft_for_ssa.lincoln_demo;

import org.apache.flink.statefun.flink.harness.Harness;
import org.apache.flink.statefun.flink.harness.io.SerializableSupplier;
import org.junit.Test;

import java.io.*;

/** Unit test for simple App. */
public class DemoTest {

  private File file;

  @Test
  public void run() throws Exception {

    String path = "output/2020-05-25_tracklet_messages.txt";
    File file = new File(path); // creates a new file instance
    FileReader fr = new FileReader(file); // reads the file
    BufferedReader br = new BufferedReader(fr); // creates a buffering character input stream

    Harness harness =
        new Harness()
            .withKryoMessageSerializer()
            .withSupplyingIngress(IO.TRACKS_INGRESS_ID, new TrackStringGenerator(br))
            .withPrintingEgress(IO.PRINT_EGRESS_ID);

    harness.start();
  }

  private static final class TrackStringGenerator implements SerializableSupplier<String> {

    private static final long serialVersionUID = 1;
    private transient BufferedReader br;

    TrackStringGenerator(BufferedReader br) throws FileNotFoundException {
      this.br = br;
    }

    @Override
    public String get() {
      try {
        Thread.sleep(1_000);
      } catch (InterruptedException e) {
        throw new RuntimeException("Interrupted", e);
      }

      String response = "End of document";
      try {
        response = br.readLine();
      } catch (IOException e) {
        e.printStackTrace();
      }
      return response;
    }
  }
}
