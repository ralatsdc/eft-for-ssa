package io.springbok.statefun.examples.utilities;

import io.springbok.statefun.examples.demonstration.generated.SingleLineTLE;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.orekit.propagation.analytical.tle.TLE;

import java.util.ArrayList;
import java.util.Iterator;

public class MockTLESourceFunction implements SourceFunction<SingleLineTLE> {

  private ArrayList<SingleLineTLE> singleLineTLES;
  private volatile boolean isRunning = true;
  public int runTimeMS = 8000;

  public MockTLESourceFunction(ArrayList<TLE> tles) {
    this.singleLineTLES = new ArrayList<>();
    tles.forEach(
        tle -> {
          SingleLineTLE singleLineTLE = TLEReader.toSingleLineTLE(tle);
          this.singleLineTLES.add(singleLineTLE);
        });
  }

  @Override
  public void run(SourceContext<SingleLineTLE> sourceContext) throws InterruptedException {

    Iterator<SingleLineTLE> tracksIterator = singleLineTLES.iterator();

    while (isRunning && tracksIterator.hasNext())
      for (SingleLineTLE singleLineTLE : singleLineTLES) {
        sourceContext.collect(tracksIterator.next());
      }
    sourceContext.close();
    Thread.sleep(runTimeMS);
  }

  @Override
  public void cancel() {
    isRunning = false;
  }
}
