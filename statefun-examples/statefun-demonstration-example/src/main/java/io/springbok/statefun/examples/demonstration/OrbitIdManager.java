package io.springbok.statefun.examples.demonstration;

import io.springbok.statefun.examples.demonstration.generated.NewTrackMessage;
import org.apache.flink.statefun.sdk.Context;
import org.apache.flink.statefun.sdk.FunctionType;
import org.apache.flink.statefun.sdk.StatefulFunction;
import org.apache.flink.statefun.sdk.annotations.Persisted;
import org.apache.flink.statefun.sdk.state.PersistedValue;

import java.util.ArrayList;

/*
 The OrbitIdManager is responsible for creating new ids for orbits, as well as keeping track of those ids to check for possible correlations between orbits
*/
public class OrbitIdManager implements StatefulFunction {

  // This FunctionType binding is used in the Demonstration module
  public static final FunctionType TYPE =
      new FunctionType("springbok", "orbit-id-stateful-function");

  // PersistedValues can be stored and recalled when this StatefulFunction is invoked
  @Persisted
  private final PersistedValue<ArrayList> orbitIds =
      PersistedValue.of("orbit-ids", ArrayList.class);

  @Persisted
  private final PersistedValue<Long> lastOrbitId = PersistedValue.of("last-orbit-id", Long.class);

  // Invoke is called once when another part of the application calls context.send to this address.
  // instanceof is used to specify what message is received
  @Override
  public void invoke(Context context, Object input) {

    // This is a message from a TrackStatefulFunction. This creates a new orbit id and forwards the
    // Track to the appropriate OrbitStatefulFunction
    if (input instanceof NewTrackMessage) {
      NewTrackMessage newTrackMessage = (NewTrackMessage) input;

      Long id = createNewId();

      // Send the incoming track to save and process at the OrbitStatefulFunction that corresponds
      // to
      // the just created id
      context.send(OrbitStatefulFunction.TYPE, String.valueOf(id), newTrackMessage);

      // Message out that orbit id was created
      Utilities.sendToDefault(context, String.format("Created orbitId %s", id));

      // Set persisted state
      lastOrbitId.set(id);
    }

    // This is the final stop for the CollectedTracksMessage before it gets forwarded to its new
    // OrbitStatefulFunction. Here it collects an id and is sent to the appropriate
    // OrbitStatefulFunction
    if (input instanceof CollectedTracksMessage) {
      CollectedTracksMessage collectedTracksMessage = (CollectedTracksMessage) input;

      Long id = createNewId();

      // Send the incoming track to save and process at the OrbitStatefulFunction that corresponds
      // to
      // the just created id
      context.send(OrbitStatefulFunction.TYPE, String.valueOf(id), collectedTracksMessage);

      // Message out that orbit id was created
      Utilities.sendToDefault(
          context,
          String.format(
              "Created orbitId %s refined from orbits with ids %s and %s",
              id, collectedTracksMessage.keyedOrbitId1, collectedTracksMessage.keyedOrbitId2));

      // Set persisted state
      lastOrbitId.set(id);
    }

    // This message is received from an OrbitStatefulFunction when a new refined orbit (multiple
    // tracks combined) is successfully created.
    // It saves the new orbit id in its list and deletes the old ones.
    if (input instanceof NewRefinedOrbitIdMessage) {
      NewRefinedOrbitIdMessage newRefinedOrbitIdMessage = (NewRefinedOrbitIdMessage) input;

      ArrayList<String> orbitIdList = orbitIds.getOrDefault(new ArrayList<String>());

      // Message out that orbit id was saved
      Utilities.sendToDefault(
          context, String.format("Saved orbitId %s", newRefinedOrbitIdMessage.newOrbitId));

      // Update orbitIdList with the new orbit
      orbitIdList.add(newRefinedOrbitIdMessage.newOrbitId);
      orbitIdList.remove(newRefinedOrbitIdMessage.oldOrbitId1);
      orbitIdList.remove(newRefinedOrbitIdMessage.oldOrbitId2);
      Utilities.sendToDefault(context, orbitIdList.toString());
      orbitIds.set(orbitIdList);
    }

    // This message is received from an OrbitStatefulFunction when a new orbit is successfully
    // created.
    // It saves the new orbit id in its list and sends out a message to all other orbits in its list
    // to check for a correlation
    if (input instanceof CorrelateOrbitsMessage) {
      CorrelateOrbitsMessage correlateOrbitsMessage = (CorrelateOrbitsMessage) input;

      ArrayList<String> orbitIdList = orbitIds.getOrDefault(new ArrayList<String>());

      // Send new id to all existing orbits to do calculation
      orbitIdList.forEach(
          orbitId -> {
            context.send(OrbitStatefulFunction.TYPE, orbitId, correlateOrbitsMessage);
          });

      // Message out that orbit id was saved
      Utilities.sendToDefault(
          context, String.format("Saved orbitId %s", correlateOrbitsMessage.keyedOrbit.orbitId));

      // Update orbitIdList with the new orbit
      orbitIdList.add(correlateOrbitsMessage.keyedOrbit.orbitId);
      orbitIds.set(orbitIdList);
    }

    // This message is sent from an OrbitStatefulFunction when that orbit expires. This removes that
    // orbit id from the orbit id list
    if (input instanceof RemoveOrbitIdMessage) {
      RemoveOrbitIdMessage orbitMessage = (RemoveOrbitIdMessage) input;

      ArrayList ids = orbitIds.get();
      ids.remove(orbitMessage.orbitId);

      // Message out that orbit id was removed
      Utilities.sendToDefault(context, String.format("Removed orbitId %s", orbitMessage.orbitId));

      orbitIds.set(ids);
    }
  }

  private Long createNewId() {
    Long id = lastOrbitId.getOrDefault(-1L);
    id++;
    return id;
  }
}
