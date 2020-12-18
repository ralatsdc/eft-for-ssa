package io.springbok.statefun.examples.demonstration;

import io.springbok.statefun.examples.demonstration.generated.FireEventMessage;
import io.springbok.statefun.examples.demonstration.generated.NewEventMessage;
import io.springbok.statefun.examples.demonstration.generated.GetNextEventMessage;
import io.springbok.statefun.examples.demonstration.generated.NewEventSourceMessage;
import org.apache.flink.statefun.sdk.Context;
import org.apache.flink.statefun.sdk.FunctionType;
import org.apache.flink.statefun.sdk.StatefulFunction;
import org.apache.flink.statefun.sdk.annotations.Persisted;
import org.apache.flink.statefun.sdk.state.PersistedValue;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

import java.time.Duration;
import java.util.ArrayList;

public class EventManager implements StatefulFunction {

  public static final FunctionType TYPE = new FunctionType("springbok", "event-manager");

  // PersistedValues can be stored and recalled when this StatefulFunction is invoked
  @Persisted
  private final PersistedValue<ArrayList> eventsState =
      PersistedValue.of("events", ArrayList.class);

  @Persisted
  private PersistedValue<AbsoluteDate> lastEventTimeState =
      PersistedValue.of("current-time", AbsoluteDate.class);

  @Persisted
  private PersistedValue<Boolean> hasSentMessage =
      PersistedValue.of("has-sent-message", Boolean.class);

  @Override
  public void invoke(Context context, Object input) {

    if (input instanceof NewEventMessage) {
      NewEventMessage newEventMessage = (NewEventMessage) input;

      ArrayList<NewEventMessage> events =
          eventsState.getOrDefault(new ArrayList<NewEventMessage>());

      events.add(newEventMessage);
      // TODO: sort list by time
      if (hasSentMessage.getOrDefault(false) == false) {

        NewEventMessage nextEvent = events.get(0);
        events.remove(0);
        scheduleEvent(context, nextEvent);

        hasSentMessage.set(true);
      }
      eventsState.set(events);
    }

    // Update clock, fire off event, get next event (these will be the same message -
    // satellitestatefulfunction will both fire event and send a new one, sort list, and schedule
    // next event
    if (input instanceof FireEventMessage) {
      FireEventMessage fireEventMessage = (FireEventMessage) input;

      AbsoluteDate eventTime =
          new AbsoluteDate(fireEventMessage.getTime(), TimeScalesFactory.getUTC());
      AbsoluteDate lastEventTime = lastEventTimeState.get();

      int timePassed = eventTime.compareTo(lastEventTime);

      // event time is after the simulation time - update sim time to event time
      if (timePassed > 0) {
        lastEventTimeState.set(eventTime);
      }

      context.send(
          SatelliteStatefulFunction.TYPE, fireEventMessage.getObjectId(), fireEventMessage);

      ArrayList<NewEventMessage> events = eventsState.get();
      NewEventMessage nextEvent = events.get(0);
      events.remove(0);

      scheduleEvent(context, nextEvent);

      eventsState.set(events);
    }

    if (input instanceof NewEventSourceMessage) {
      NewEventSourceMessage newEventSourceMessage = (NewEventSourceMessage) input;

      GetNextEventMessage getNextEventMessage =
          GetNextEventMessage.newBuilder().setTime(lastEventTimeState.get().toString()).build();

      context.send(
          SatelliteStatefulFunction.TYPE, newEventSourceMessage.getId(), getNextEventMessage);
    }
  }

  // TODO: handle simulation timing as well as real life time
  // TODO: use a switch where, if simulation is flagged, checkTime() just adds to the current time
  // by some amount
  private void checkTime() {
    lastEventTimeState.set(new AbsoluteDate(new java.util.Date(), TimeScalesFactory.getUTC()));
  }

  private void updateTime(NewEventMessage newEventMessage) {}

  private void scheduleEvent(Context context, NewEventMessage nextEvent) {

    AbsoluteDate currentEventTime = lastEventTimeState.get();
    AbsoluteDate nextEventTime = new AbsoluteDate(nextEvent.getTime(), TimeScalesFactory.getUTC());

    FireEventMessage nextEventMessage =
        FireEventMessage.newBuilder()
            .setObjectId(nextEvent.getObjectId())
            .setTime(nextEvent.getTime())
            .build();

    int timeUntilEvent = nextEventTime.compareTo(currentEventTime);

    // next event time is before current event - event fires immediately
    if (timeUntilEvent <= 0) {
      context.send(context.self(), nextEventMessage);
    } else {
      // TODO: add time speed up factor here
      context.sendAfter(Duration.ofMillis(timeUntilEvent), context.self(), nextEventMessage);
    }
  }
}
