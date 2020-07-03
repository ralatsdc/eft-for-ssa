package io.springbok.statefun.examples.demonstration;

import org.apache.flink.statefun.sdk.Context;
import org.apache.flink.statefun.sdk.FunctionType;
import org.apache.flink.statefun.sdk.StatefulFunction;
import org.apache.flink.statefun.sdk.annotations.Persisted;
import org.apache.flink.statefun.sdk.state.PersistedValue;

import java.util.ArrayList;

public class OrbitIdManager implements StatefulFunction {

  public static final FunctionType TYPE =
      new FunctionType("springbok", "orbit-id-stateful-function");

  @Persisted
  private final PersistedValue<ArrayList> orbitIds =
      PersistedValue.of("orbit-ids", ArrayList.class);

  @Persisted
  private final PersistedValue<Long> lastOrbitId = PersistedValue.of("last-orbit-id", Long.class);

  @Override
  public void invoke(Context context, Object input) {

    if (input instanceof NewTrackMessage) {
      NewTrackMessage newTrackMessage = (NewTrackMessage) input;

      ArrayList<String> ids = orbitIds.getOrDefault(new ArrayList());
      Long id = lastOrbitId.getOrDefault(-1L);
      id++;
      ids.add(String.valueOf(id));

      context.send(OrbitStatefulFunction.TYPE, String.valueOf(id), newTrackMessage);

      Utilities.sendToDefault(context, String.format("Created orbitId %s", id));

      orbitIds.set(ids);
      lastOrbitId.set(id);
    }

    //    if (input instanceof AddOrbitMessage) {
    //      ArrayList idList = orbitIds.getOrDefault(new ArrayList<Long>());
    //      AddOrbitMessage orbitMessage = (AddOrbitMessage) input;
    //      KeyedOrbit orbit = orbitMessage.getOrbit();
    //
    //      // Send to all existing orbits to do calculation
    //      // TODO: deal with flink requiring string IDs and keyedorbits and tracklets incrementing
    // long
    //      // IDs
    //      //      idList.forEach(
    //      //          id -> {
    //      //            context.send(
    //      //                OrbitStatefulFunction.TYPE, id.toString(), new
    // CompareOrbitsMessage(orbit));
    //      //          });
    //      //
    //      idList.add(orbit.getOrbitId());
    //      orbitIds.set(idList);
    //      Utilities.sendToDefault(
    //          context, String.format("Added orbitId %d to id-manager", orbit.getOrbitId()));
    //    }

    if (input instanceof RemoveOrbitIdMessage) {
      RemoveOrbitIdMessage orbitMessage = (RemoveOrbitIdMessage) input;
      ArrayList ids = orbitIds.get();
      ids.remove(orbitMessage.orbitId);
      Utilities.sendToDefault(context, String.format("Removed orbitId %s", orbitMessage.orbitId));

      orbitIds.set(ids);
    }
  }
}
