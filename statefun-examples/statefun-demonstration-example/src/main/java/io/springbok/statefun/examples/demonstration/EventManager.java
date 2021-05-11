package io.springbok.statefun.examples.demonstration;

import io.springbok.statefun.examples.demonstration.generated.*;
import org.apache.flink.statefun.sdk.Context;
import org.apache.flink.statefun.sdk.FunctionType;
import org.apache.flink.statefun.sdk.StatefulFunction;
import org.apache.flink.statefun.sdk.annotations.Persisted;
import org.apache.flink.statefun.sdk.state.PersistedValue;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;

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

  @Persisted
  private PersistedValue<Integer> eventsHandledState =
      PersistedValue.of("events-handled", Integer.class);

  @Override
  public void invoke(Context context, Object input) {

    if (input instanceof NewEventMessage) {
      NewEventMessage newEventMessage = (NewEventMessage) input;

      try {
        ArrayList<NewEventMessage> events =
            eventsState.getOrDefault(new ArrayList<NewEventMessage>());

        Utilities.log(
            context,
            String.format(
                "Recieved Event: %s, id %s, date %s",
                newEventMessage.getEventType(),
                newEventMessage.getObjectId(),
                newEventMessage.getTime()),
            3);

        // Handle saving event by event type
        if (newEventMessage.getEventType().equals("satellite-visible")) {
          events.add(newEventMessage);
        } else if (newEventMessage.getEventType().equals("delete-orbit")) {

          // Delete timer in days
          long deleteTimer = ApplicationProperties.getDeleteTimer();
          // Current orbit time shifted by seconds
          AbsoluteDate deleteDate =
              new AbsoluteDate(newEventMessage.getTime(), TimeScalesFactory.getUTC())
                  .shiftedBy(deleteTimer * 86400);

          NewEventMessage newEventMessage1 =
              NewEventMessage.newBuilder()
                  .setEventType(newEventMessage.getEventType())
                  .setObjectId(newEventMessage.getObjectId())
                  .setTime(deleteDate.toString())
                  .build();

          events.add(newEventMessage1);
        }

        // TODO: sort list by time

        Collections.sort(
            events,
            (message1, message2) -> {
              AbsoluteDate time1 = new AbsoluteDate(message1.getTime(), TimeScalesFactory.getUTC());
              AbsoluteDate time2 = new AbsoluteDate(message2.getTime(), TimeScalesFactory.getUTC());
              return time1.compareTo(time2);
            });

        Utilities.log(context, String.format("All current events: %s", events), 3);
        Utilities.log(
            context,
            String.format(
                "Current Time: %s", lastEventTimeState.getOrDefault(AbsoluteDate.FUTURE_INFINITY)),
            3);

        // if this is the first event received, fire it off
        if (hasSentMessage.getOrDefault(false) == false) {

          // if lastEventTimeState has not been set, set it as equal to incoming event message
          lastEventTimeState.set(
              lastEventTimeState.getOrDefault(
                  new AbsoluteDate(newEventMessage.getTime(), TimeScalesFactory.getUTC())));

          NewEventMessage nextEvent = events.get(0);
          events.remove(0);
          scheduleEvent(context, nextEvent);

          hasSentMessage.set(true);
        }
        eventsState.set(events);

      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    // Update clock, fire off event, get next event
    // these will be the same message - SatelliteStatefulFunction will both fire event and send a
    // new one, sort list, and schedule next event
    if (input instanceof FireEventMessage) {
      FireEventMessage fireEventMessage = (FireEventMessage) input;

      Utilities.log(
          context,
          String.format(
              "Triggering Event: %s, id %s, date %s",
              fireEventMessage.getEventType(),
              fireEventMessage.getObjectId(),
              fireEventMessage.getTime()),
          3);

      AbsoluteDate eventTime =
          new AbsoluteDate(fireEventMessage.getTime(), TimeScalesFactory.getUTC());
      AbsoluteDate lastEventTime = lastEventTimeState.get();

      int timePassed = eventTime.compareTo(lastEventTime);

      // event time is after the simulation time - update sim time to event time
      if (timePassed > 0) {
        lastEventTimeState.set(eventTime);
      }

      if (fireEventMessage.getEventType().equals("satellite-visible")) {
        context.send(
            SatelliteStatefulFunction.TYPE, fireEventMessage.getObjectId(), fireEventMessage);
      } else if (fireEventMessage.getEventType().equals("delete-orbit")) {
        DeleteMessage deleteMessage = DeleteMessage.newBuilder().build();
        context.send(OrbitStatefulFunction.TYPE, fireEventMessage.getObjectId(), deleteMessage);
      }

      ArrayList<NewEventMessage> events = eventsState.get();

      // if there are no more events, wait for next event to come in
      if (events.size() < 1) {
        hasSentMessage.set(false);
      } else {
        NewEventMessage nextEvent = events.get(0);
        events.remove(0);

        scheduleEvent(context, nextEvent);

        eventsState.set(events);
      }
    }

    if (input instanceof NewEventSourceMessage) {
      NewEventSourceMessage newEventSourceMessage = (NewEventSourceMessage) input;

      GetNextEventMessage getNextEventMessage;

      AbsoluteDate lastEventTime = lastEventTimeState.get();
      if (lastEventTime == null) {
        getNextEventMessage = GetNextEventMessage.newBuilder().buildPartial();
      } else {
        getNextEventMessage =
            GetNextEventMessage.newBuilder().setTime(lastEventTimeState.get().toString()).build();
      }

      context.send(
          SatelliteStatefulFunction.TYPE, newEventSourceMessage.getId(), getNextEventMessage);
    }
  }

  private void scheduleEvent(Context context, NewEventMessage nextEvent) {

    try {
      if (ApplicationProperties.getIsTest()) {
        Integer eventsHandled = eventsHandledState.getOrDefault(0);
        Integer testEventNumber = ApplicationProperties.getTestEventNumber();
        if (eventsHandled == 0) {
          // 0 can be set in properties to have default behavior during docker tests && infinite
          // events
        } else if (eventsHandled.equals(testEventNumber)) {
          Utilities.log(context, String.format("All events handled. Exiting."), 1);
          return;
        }
        eventsHandledState.set(eventsHandled + 1);
      }
    } catch (Exception e) {
      Utilities.log(context, e.toString(), 1);
    }

    AbsoluteDate currentEventTime = lastEventTimeState.get();
    AbsoluteDate nextEventTime = new AbsoluteDate(nextEvent.getTime(), TimeScalesFactory.getUTC());

    FireEventMessage fireEventMessage =
        FireEventMessage.newBuilder()
            .setEventType(nextEvent.getEventType())
            .setObjectId(nextEvent.getObjectId())
            .setTime(nextEvent.getTime())
            .build();

    double timeUntilEvent = nextEventTime.durationFrom(currentEventTime);

    // next event time is before current event - event fires immediately
    if (timeUntilEvent <= 0) {
      context.send(context.self(), fireEventMessage);
      Utilities.log(context, String.format("Next event sent immediately"), 1);
    } else {
      // TODO: make speedup settable
      // a factor of 43200 makes 1 day pass every 2 seconds
      try {
        double speedUpFactor = ApplicationProperties.getSpeedUpFactor();
        double adjustedTime = timeUntilEvent / speedUpFactor;
        Utilities.log(context, String.format("Time until event: %s", timeUntilEvent), 3);
        Utilities.log(context, String.format("Adjusted Time %s", adjustedTime), 3);

        context.sendAfter(
            Duration.ofSeconds((long) adjustedTime), context.self(), fireEventMessage);
        Utilities.log(
            context, String.format("Next event scheduled for %s", nextEventTime.toString()), 1);
      } catch (Exception e) {
        Utilities.log(
            context,
            String.format(
                "Event scheduler cannot schedule next event - check properties. \n %s", e),
            1);
      }
    }
  }
}
