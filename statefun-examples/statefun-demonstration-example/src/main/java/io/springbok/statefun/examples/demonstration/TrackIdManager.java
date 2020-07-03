package io.springbok.statefun.examples.demonstration;

import io.springbok.statefun.examples.demonstration.generated.TrackIn;
import org.apache.flink.statefun.sdk.Context;
import org.apache.flink.statefun.sdk.FunctionType;
import org.apache.flink.statefun.sdk.StatefulFunction;
import org.apache.flink.statefun.sdk.annotations.Persisted;
import org.apache.flink.statefun.sdk.state.PersistedValue;

public class TrackIdManager implements StatefulFunction {

  public static final FunctionType TYPE = new FunctionType("springbok", "track-stateful-builder");

  @Persisted
  private final PersistedValue<Long> lastTrackId = PersistedValue.of("last-track-id", Long.class);

  @Override
  public void invoke(Context context, Object input) {

    TrackIn trackIn = (TrackIn) input;

    Long id = lastTrackId.getOrDefault(-1L);
    id++;

    // Send to track stateful function to save and process
    context.send(TrackStatefulFunction.TYPE, String.valueOf(id), trackIn);

    Utilities.sendToDefault(context, String.format("Created trackId %d", id));

    lastTrackId.set(id);
  }
}
