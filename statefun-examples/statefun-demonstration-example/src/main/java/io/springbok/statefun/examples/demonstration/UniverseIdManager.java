package io.springbok.statefun.examples.demonstration;

import io.springbok.statefun.examples.demonstration.generated.Command;
import io.springbok.statefun.examples.demonstration.generated.TrackIn;
import org.apache.flink.statefun.sdk.Context;
import org.apache.flink.statefun.sdk.FunctionType;
import org.apache.flink.statefun.sdk.StatefulFunction;
import org.apache.flink.statefun.sdk.annotations.Persisted;
import org.apache.flink.statefun.sdk.state.PersistedValue;

import java.util.ArrayList;

public class UniverseIdManager implements StatefulFunction {

  // This FunctionType binding is used in the Demonstration module
  public static final FunctionType TYPE = new FunctionType("springbok", "universe-manager");

  // PersistedValues can be stored and recalled when this StatefulFunction is invoked
  @Persisted
  private final PersistedValue<ArrayList> universes =
      PersistedValue.of("universes", ArrayList.class);

  @Override
  public void invoke(Context context, Object input) {

    // TrackIn contains universe information; route to correct TrackIdManager
    if (input instanceof TrackIn) {
      TrackIn trackIn = (TrackIn) input;
      try {
        // Create track from input
        Track track = Track.fromString(trackIn.getTrack(), context.self().id());

        // Send the new track to the TrackStatefulFunction in the correct universe
        context.send(TrackIdManager.TYPE, track.universe, trackIn);

        updateUniverseList(track.universe);
      } catch (Exception e) {
        Utilities.log(
            context,
            String.format(
                "track given id %s not valid. Discarding message '%s' \n Error: %s",
                context.self().id(), trackIn.getTrack(), e),
            1);
      }
    }
    if (input instanceof Command) {
      String command = ((Command) input).getCommand();

      if (command.toLowerCase().indexOf("create") != -1) {
        Utilities.respond(context, String.format("%s %s", context.self().id(), command));
      } else if (command.toLowerCase().indexOf("read") != -1) {
        Utilities.respond(context, String.format("%s %s", context.self().id(), command));
      } else if (command.toLowerCase().indexOf("update") != -1) {
        Utilities.respond(context, String.format("%s %s", context.self().id(), command));
      } else if (command.toLowerCase().indexOf("delete") != -1) {
        Utilities.respond(context, String.format("%s %s", context.self().id(), command));
      }
    }
  }

  private void updateUniverseList(String universe_id) {
    ArrayList universe_list = universes.getOrDefault(new ArrayList<String>());

    if (!universe_list.contains(universe_id)) {
      universe_list.add(universe_id);
      universes.set(universe_list);
    }
  }
}
