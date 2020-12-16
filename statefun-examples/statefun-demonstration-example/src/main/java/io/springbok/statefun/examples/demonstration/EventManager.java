package io.springbok.statefun.examples.demonstration;

import io.springbok.statefun.examples.demonstration.generated.GetNextEventMessage;
import io.springbok.statefun.examples.demonstration.generated.NewEventMessage;
import io.springbok.statefun.examples.demonstration.generated.NewEventSourceMessage;
import org.apache.flink.statefun.sdk.Context;
import org.apache.flink.statefun.sdk.FunctionType;
import org.apache.flink.statefun.sdk.StatefulFunction;
import org.apache.flink.statefun.sdk.annotations.Persisted;
import org.apache.flink.statefun.sdk.state.PersistedValue;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

import java.util.ArrayList;

public class EventManager implements StatefulFunction {

  public static final FunctionType TYPE = new FunctionType("springbok", "event-manager");

  // PersistedValues can be stored and recalled when this StatefulFunction is invoked
  @Persisted
  private final PersistedValue<ArrayList> events = PersistedValue.of("events", ArrayList.class);

  @Persisted
  private PersistedValue<AbsoluteDate> currentTime =
      PersistedValue.of("current-time", AbsoluteDate.class);

  @Override
  public void invoke(Context context, Object input) {

    checkTime();

    if (input instanceof NewEventMessage) {}

    if (input instanceof NewEventSourceMessage) {
      NewEventSourceMessage newEventSourceMessage = (NewEventSourceMessage) input;

      GetNextEventMessage getNextEventMessage =
          GetNextEventMessage.newBuilder().setTime(currentTime.get().toString()).build();

      context.send(
          SatelliteStatefulFunction.TYPE, newEventSourceMessage.getId(), getNextEventMessage);
    }
  }

  // TODO: handle simulation timing as well as real life time
  // TODO: use a switch where, if simulation is flagged, checkTime() just adds to the current time
  // by some amount
  private void checkTime() {
    currentTime.set(new AbsoluteDate(new java.util.Date(), TimeScalesFactory.getUTC()));
  }
}
