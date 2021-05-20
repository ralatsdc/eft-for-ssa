package io.springbok.statefun.examples.demonstration;

import io.springbok.statefun.examples.demonstration.generated.DeleteMessage;
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

import java.time.Duration;

public class EventManager implements StatefulFunction {

  public static final FunctionType TYPE = new FunctionType("springbok", "event-manager");

  // PersistedValues can be stored and recalled when this StatefulFunction is invoked
  @Persisted
  private PersistedValue<Long> lastAwakeTimeState =
      PersistedValue.of("last-awake-time", Long.class);

  @Persisted
  private PersistedValue<AbsoluteDate> eventTimeState =
      PersistedValue.of("event-time", AbsoluteDate.class);

  @Persisted
  private PersistedValue<Integer> eventsHandledState =
      PersistedValue.of("events-handled", Integer.class);

  @Persisted
  private PersistedValue<Integer> speedUpFactorState = PersistedValue.of("speed-up", Integer.class);

  @Override
  public void invoke(Context context, Object input) {

    // EventManager schedules event message and fires it off with a timer delay. The primary purpose
    // of this class is clock synchronization across application
    if (input instanceof NewEventMessage) {
      NewEventMessage newEventMessage = (NewEventMessage) input;

      updateClock(context, newEventMessage);

      try {

        Utilities.log(
            context,
            String.format(
                "Recieved Event: %s, id %s, date %s",
                newEventMessage.getEventType(),
                newEventMessage.getObjectId(),
                newEventMessage.getTime()),
            3);

        scheduleEvent(context, newEventMessage);

      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    if (input instanceof NewEventSourceMessage) {
      NewEventSourceMessage newEventSourceMessage = (NewEventSourceMessage) input;

      GetNextEventMessage getNextEventMessage;

      AbsoluteDate eventTime = eventTimeState.get();
      if (eventTime == null) {
        getNextEventMessage = GetNextEventMessage.newBuilder().buildPartial();
      } else {
        getNextEventMessage =
            GetNextEventMessage.newBuilder().setTime(eventTimeState.get().toString()).build();
      }

      context.send(
          SatelliteStatefulFunction.TYPE, newEventSourceMessage.getId(), getNextEventMessage);
    }
  }

  private void scheduleEvent(Context context, NewEventMessage newEventMessage) throws Exception {

    // Limiter for # of events handled. Good for contained tests
    try {
      if (ApplicationProperties.getIsTest()) {
        Integer eventsHandled = eventsHandledState.getOrDefault(0);
        Integer testEventNumber = ApplicationProperties.getTestEventNumber();
        if (eventsHandled == 0) {
          // 0 can be set in properties to have default behavior during docker tests && infinite
          // events
          // TODO: Fix this logic
        } else if (eventsHandled.equals(testEventNumber)) {
          Utilities.log(context, String.format("All events handled. Exiting."), 1);
          return;
        }
        eventsHandledState.set(eventsHandled + 1);
      }
    } catch (Exception e) {
      Utilities.log(context, e.toString(), 1);
    }

    AbsoluteDate nextEventTime = null;
    long timeUntilEvent = 0;
    double adjustedTime = 0;
    int speedUpFactor = speedUpFactorState.getOrDefault(ApplicationProperties.getSpeedUpFactor());
    AbsoluteDate eventTime = eventTimeState.get();

    try {
      // Route events correctly
      if (newEventMessage.getEventType().equals("satellite-visible")) {

        // TODO: implement

      } else if (newEventMessage.getEventType().equals("delete-orbit")) {

        // Get delete timer in days and convert to seconds
        timeUntilEvent = ApplicationProperties.getDeleteTimer() * 86400;
        // Current event time shifted by delete timer
        nextEventTime = eventTime.shiftedBy(timeUntilEvent);
        adjustedTime = timeUntilEvent / speedUpFactor;

        DeleteMessage deleteMessage = DeleteMessage.newBuilder().build();
        context.sendAfter(
            Duration.ofSeconds((long) adjustedTime),
            OrbitStatefulFunction.TYPE,
            newEventMessage.getObjectId(),
            deleteMessage);
      }

      Utilities.log(
          context,
          String.format(
              "Next event scheduled for %s \n" + " Time until event: %s. Adjusted Time: %s",
              nextEventTime.toString(), timeUntilEvent, adjustedTime),
          3);

    } catch (Exception e) {
      Utilities.log(
          context,
          String.format("Event scheduler cannot schedule next event - check properties. \n %s", e),
          1);
    }
  }

  private void updateClock(Context context, NewEventMessage newEventMessage) {
    try {
      Long currentTime = System.nanoTime();
      Long lastAwakeTime = lastAwakeTimeState.getOrDefault(currentTime);

      // Time passed in system time in nanoseconds since last awake
      Long timePassed = currentTime - lastAwakeTime;

      // TODO: initialize eventTimeState
      AbsoluteDate eventTime =
          eventTimeState.getOrDefault(
              new AbsoluteDate(newEventMessage.getTime(), TimeScalesFactory.getUTC()));

      int speedUpFactor = speedUpFactorState.getOrDefault(ApplicationProperties.getSpeedUpFactor());
      AbsoluteDate newEventTime =
          eventTime.shiftedBy((double) (timePassed * speedUpFactor) / 1000000000.);

      lastAwakeTimeState.set(currentTime);
      eventTimeState.set(newEventTime);

      Utilities.log(context, String.format("Current event time: %s", newEventTime), 1);
    } catch (Exception e) {
      Utilities.log(
          context,
          String.format("Event scheduler cannot update clock - check properties. \n %s", e),
          1);
    }
  }
}
