package io.springbok.eft_for_ssa;

import org.apache.flink.statefun.flink.harness.Harness;
import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
    @Test
    public void run() throws Exception {
        Harness harness =
                new Harness()
                        .withFlinkSourceFunction(Identifiers.INGRESS_ID, new TrackletSource().getSource())
                        .withPrintingEgress(Identifiers.EGRESS_ID);
        harness.start();
    }
}
