package io.springbok.statefun.examples.utility;

import io.springbok.statefun.examples.demonstration.generated.TrackIn;
import org.apache.flink.streaming.api.functions.source.SourceFunction;

import java.util.ArrayList;
import java.util.Iterator;

public final class MockTracksSourceFunction implements SourceFunction<TrackIn> {

  private ArrayList<TrackIn> tracks;
  private volatile boolean isRunning = true;
  public int runTimeMS = 8000;

  public MockTracksSourceFunction(ArrayList<String> tracks) {
    this.tracks = new ArrayList<>();
    tracks.forEach(
        track -> {
          TrackIn trackIn = TrackIn.newBuilder().setTrack(track).build();
          this.tracks.add(trackIn);
        });
  }

  @Override
  public void run(SourceContext<TrackIn> sourceContext) throws InterruptedException {

    Iterator<TrackIn> tracksIterator = tracks.iterator();

    while (isRunning && tracksIterator.hasNext())
      for (TrackIn track : tracks) {
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
