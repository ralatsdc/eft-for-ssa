package io.springbok.statefun.examples.utilities;

import io.springbok.statefun.examples.demonstration.generated.SensorIn;
import org.apache.flink.streaming.api.functions.source.SourceFunction;

import java.util.ArrayList;
import java.util.Iterator;

public class MockSensorSourceFunction implements SourceFunction<SensorIn> {

  private ArrayList<SensorIn> sensorIns;
  private volatile boolean isRunning = true;
  public int runTimeMS = 8000;

  public MockSensorSourceFunction() {}

  public MockSensorSourceFunction(ArrayList<String> sensors) {
    this.sensorIns = new ArrayList<>();
    sensors.forEach(
        sensor -> {
          SensorIn sensorIn = SensorIn.newBuilder().setSensor(sensor).build();
          this.sensorIns.add(sensorIn);
        });
  }

  @Override
  public void run(SourceContext<SensorIn> sourceContext) throws InterruptedException {

    if (!(sensorIns == null)) {
      Iterator<SensorIn> sensorInIterator = sensorIns.iterator();

      while (isRunning && sensorInIterator.hasNext())
        for (SensorIn sensorIn : sensorIns) {
          sourceContext.collect(sensorInIterator.next());
        }
      sourceContext.close();
      Thread.sleep(runTimeMS);
    }
  }

  @Override
  public void cancel() {
    isRunning = false;
  }
}
