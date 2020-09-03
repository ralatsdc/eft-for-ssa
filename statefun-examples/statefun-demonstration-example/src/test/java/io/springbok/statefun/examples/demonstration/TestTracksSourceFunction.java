package io.springbok.statefun.examples.demonstration;

import io.springbok.statefun.examples.demonstration.generated.TrackIn;
import org.apache.flink.streaming.api.functions.source.SourceFunction;

import java.util.ArrayList;
import java.util.Iterator;

public final class TestTracksSourceFunction implements SourceFunction<TrackIn> {

  private final ArrayList<TrackIn> tracks;
  private volatile boolean isRunning = true;

  TestTracksSourceFunction(ArrayList<String> tracks) {
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
    // Todo: figure out how to ensure all processes are complete instead of dumb timing them
    // Timer is important because once the flinksource run shuts down, stateful functions CLOSE
    // mailboxes, so all of the processes can't finish and it throws an error
    Thread.sleep(8000);
  }

  @Override
  public void cancel() {
    isRunning = false;
  }
}
