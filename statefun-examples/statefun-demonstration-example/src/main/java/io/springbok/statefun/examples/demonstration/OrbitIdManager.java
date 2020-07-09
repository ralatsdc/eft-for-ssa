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

      Long id = createNewId();

      context.send(OrbitStatefulFunction.TYPE, String.valueOf(id), newTrackMessage);

      Utilities.sendToDefault(context, String.format("Created orbitId %s", id));

      lastOrbitId.set(id);
    }

    if (input instanceof CollectedTracksMessage) {
      CollectedTracksMessage collectedTracksMessage = (CollectedTracksMessage) input;

      Long id = createNewId();

      context.send(OrbitStatefulFunction.TYPE, String.valueOf(id), collectedTracksMessage);

      Utilities.sendToDefault(context, String.format("Created orbitId %s refined from orbits with ids %s and %s", id, collectedTracksMessage.keyedOrbitId1, collectedTracksMessage.keyedOrbitId2));

      ArrayList<String> orbitIdList = orbitIds.get();
      orbitIdList.add(String.valueOf(id));

      orbitIds.set(orbitIdList);
      lastOrbitId.set(id);
    }

    if (input instanceof CorrelateOrbitsMessage) {
      CorrelateOrbitsMessage correlateOrbitsMessage = (CorrelateOrbitsMessage) input;

      ArrayList<String> orbitIdList = orbitIds.getOrDefault(new ArrayList<String>());

      // Send to all existing orbits to do calculation
      orbitIdList.forEach(orbitId -> {
        // Do not send to the new orbit
          context.send(OrbitStatefulFunction.TYPE, orbitId, correlateOrbitsMessage);
      });

      // Update orbitIdList with the new orbit
      orbitIdList.add(correlateOrbitsMessage.keyedOrbit.orbitId);
      orbitIds.set(orbitIdList);
    }

    if (input instanceof RemoveOrbitIdMessage) {
      RemoveOrbitIdMessage orbitMessage = (RemoveOrbitIdMessage) input;
      ArrayList ids = orbitIds.get();
      ids.remove(orbitMessage.orbitId);
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
