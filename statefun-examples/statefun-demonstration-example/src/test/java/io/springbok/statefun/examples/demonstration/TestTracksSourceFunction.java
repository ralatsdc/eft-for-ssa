package io.springbok.statefun.examples.demonstration;

import io.springbok.statefun.examples.demonstration.generated.TrackIn;
import org.apache.flink.streaming.api.functions.source.SourceFunction;

import java.util.ArrayList;

public final class TestTracksSourceFunction implements SourceFunction<TrackIn> {

	private final ArrayList<TrackIn> tracks;

	TestTracksSourceFunction(ArrayList<String> tracks) {
		this.tracks = new ArrayList<>();
		tracks.forEach(track ->{
			TrackIn trackIn = TrackIn.newBuilder().setTrack(track).build();
			this.tracks.add(trackIn);
		});
	}

	@Override
	public void run(SourceContext<TrackIn> sourceContext) {
		for (TrackIn track : tracks) {
			sourceContext.collect(track);
		}
	}

	@Override
	public void cancel() {}
}
