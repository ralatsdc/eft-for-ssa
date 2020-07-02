package io.springbok.statefun.examples.demonstration;

import io.springbok.statefun.examples.demonstration.generated.TrackIn;
import org.apache.flink.statefun.flink.harness.Harness;
import org.apache.flink.statefun.flink.harness.io.SerializableSupplier;
import org.junit.Test;

/** Unit test for simple App. */
public class RunnerTest {

  @Test
  public void run() throws Exception {
    Harness harness =
        new Harness()
            .withKryoMessageSerializer()
            .withSupplyingIngress(DemonstrationIO.TRACKS_INGRESS_ID, new TracksGenerator())
            .withPrintingEgress(DemonstrationIO.DEFAULT_EGRESS_ID);

    harness.start();
  }

  private static final class TracksGenerator implements SerializableSupplier<TrackIn> {

    private static final long serialVersionUID = 1;

    @Override
    public TrackIn get() {
      try {
        Thread.sleep(1_000);
      } catch (InterruptedException e) {
        throw new RuntimeException("Interrupted", e);
      }

      String track =
          "2020-05-17T11:04:01.286,0,25772,2020-05-17T10:54:01.286,-1989133.3430834783,-8130217.779334966,9271.59691128247,6.4386047071908585,2020-05-17T10:55:01.286,-1738970.3523443781,-8180988.596336187,335525.95140157803,9.480538857259878,2020-05-17T10:56:01.286,-1484552.2841660646,-8211744.582400013,660960.2815084265,5.152920379124324,2020-05-17T10:57:01.286,-1226502.2337608847,-8222413.925267441,984778.6375833277,5.8293295865332375,2020-05-17T10:58:01.286,-965451.9889628496,-8212974.117142757,1306189.3128123847,1.8989353201602777,2020-05-17T10:59:01.286,-702040.4653171457,-8183451.97508489,1624406.7934519465,5.7498216886647,2020-05-17T11:00:01.286,-436912.12565480906,-8133923.538346406,1938653.6907981252,0.10900306126677473,2020-05-17T11:01:01.286,-170715.3882237681,-8064513.843279387,2248162.6500216275,0.2739524331416332,2020-05-17T11:02:01.286,95898.97254546685,-7975396.576748958,2552178.2310886616,2.8464553358531077,2020-05-17T11:03:01.286,362279.42856605735,-7866793.609310688,2849958.757090927,0.7778908036341792";
      TrackIn response = TrackIn.newBuilder().setTrack(track).build();

      return response;
    }
  }
}
