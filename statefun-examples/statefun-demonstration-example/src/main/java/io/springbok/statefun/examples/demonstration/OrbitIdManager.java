package io.springbok.statefun.examples.demonstration;

import io.springbok.statefun.examples.demonstration.generated.*;
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
  private final PersistedValue<ArrayList> minFormedOrbitIds =
      PersistedValue.of("min-orbit-ids", ArrayList.class);

  @Persisted
  private final PersistedValue<ArrayList> maxFormedOrbitIds =
      PersistedValue.of("max-orbit-ids", ArrayList.class);

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
      Utilities.log(context, String.format("Created orbitId %s", id), 2);

      // Set persisted state
      lastOrbitId.set(id);
    }

    // This is the final stop for the CollectedTracksMessage before it gets forwarded to its new
    // OrbitStatefulFunction. Here it collects an id and is sent to the appropriate
    // OrbitStatefulFunction
    if (input instanceof CollectedTracksMessage) {
      CollectedTracksMessage collectedTracksMessage = (CollectedTracksMessage) input;

      Long id = createNewId();

      // Send the incoming track to collect all the tracks associated with the new ID
      ArrayList<String> tracksToGather =
          Utilities.stringToArrayList(collectedTracksMessage.getTracksToGather());

      CollectedTracksMessage newCollectedTracksMessage =
          CollectedTracksMessage.newBuilder()
              .setKeyedOrbit1(collectedTracksMessage.getKeyedOrbit1())
              .setKeyedOrbit2(collectedTracksMessage.getKeyedOrbit2())
              .setTracksToGather(collectedTracksMessage.getTracksToGather())
              .setIterator(1)
              .setOrbitId(id.toString())
              .setDeleteKeyedOrbit1(collectedTracksMessage.getDeleteKeyedOrbit1())
              .setDeleteKeyedOrbit2(collectedTracksMessage.getDeleteKeyedOrbit2())
              .build();

      String nextTrack = tracksToGather.get(0);

      context.send(TrackStatefulFunction.TYPE, nextTrack, newCollectedTracksMessage);

      // Message out that orbit id was created
      Utilities.log(context, String.format("Created orbitId %s", id), 2);

      // Set persisted state
      lastOrbitId.set(id);
    }

    // This message is received from an OrbitStatefulFunction when a new refined orbit (multiple
    // tracks combined) is successfully created.
    // It saves the new orbit id in its list and deletes the old ones.
    if (input instanceof NewRefinedOrbitIdMessage) {
      NewRefinedOrbitIdMessage newRefinedOrbitIdMessage = (NewRefinedOrbitIdMessage) input;

      // Message out that orbit id was saved

      // Update orbitIdList with the new orbit

      try {

        Integer trackCutoff = ApplicationProperties.getTrackCutoff();

        if (newRefinedOrbitIdMessage.getOldOrbit1TracksNumber()
                + newRefinedOrbitIdMessage.getOldOrbit2TracksNumber()
            <= trackCutoff) {

          ArrayList<String> minFormedOrbitIdList =
              minFormedOrbitIds.getOrDefault(new ArrayList<String>());
          minFormedOrbitIdList.add(newRefinedOrbitIdMessage.getNewOrbitId());
          minFormedOrbitIds.set(minFormedOrbitIdList);

          Utilities.log(
              context,
              String.format("Saved orbitId %s", newRefinedOrbitIdMessage.getNewOrbitId()),
              2);

        } else {

          ArrayList<String> maxFormedOrbitIdList =
              maxFormedOrbitIds.getOrDefault(new ArrayList<String>());

          Utilities.log(
              context,
              String.format("Saved orbitId %s", newRefinedOrbitIdMessage.getNewOrbitId()),
              2);

          try {
            if (newRefinedOrbitIdMessage.getOldOrbit1TracksNumber() > trackCutoff) {
              maxFormedOrbitIdList.remove(newRefinedOrbitIdMessage.getOldOrbitId1());
              Utilities.log(
                  context,
                  String.format("Removed orbitId: %s", newRefinedOrbitIdMessage.getOldOrbitId1()),
                  3);
            }
          } catch (Exception e) {
            Utilities.log(
                context,
                String.format(
                    "OrbitId %s is not registered with OrbitIdManager - delete canceled: %s",
                    newRefinedOrbitIdMessage.getOldOrbitId1(), e),
                1);
          }
          try {
            if (newRefinedOrbitIdMessage.getOldOrbit2TracksNumber() > trackCutoff) {
              maxFormedOrbitIdList.remove(newRefinedOrbitIdMessage.getOldOrbitId2());
              Utilities.log(
                  context,
                  String.format("Removed orbitId: %s", newRefinedOrbitIdMessage.getOldOrbitId1()),
                  3);
            }
          } catch (Exception e) {
            Utilities.log(
                context,
                String.format(
                    "OrbitId %s is not registered with OrbitIdManager - delete canceled: %s",
                    newRefinedOrbitIdMessage.getOldOrbitId2(), e),
                1);
          }
          try {
            Utilities.log(context, "maxFormedOrbitIdList: " + maxFormedOrbitIdList.toString(), 3);
          } catch (Exception e) {
            Utilities.log(
                context,
                String.format(
                    "OrbitIds %s and %s deletion failed: %s",
                    newRefinedOrbitIdMessage.getOldOrbitId1(),
                    newRefinedOrbitIdMessage.getOldOrbitId2(),
                    e),
                1);
          }

          // Send new refined orbit out to only existing refined orbits to see if they can further
          // combine
          Utilities.log(
              context,
              String.format(
                  "Sending Refined OrbitId %s to orbits on maxFormedIdList: %s",
                  newRefinedOrbitIdMessage.getNewOrbitId(), maxFormedOrbitIdList),
              3);
          maxFormedOrbitIdList.forEach(
              orbitId -> {
                context.send(
                    OrbitStatefulFunction.TYPE,
                    orbitId,
                    CorrelateOrbitsMessage.newBuilder()
                        .setStringContent(newRefinedOrbitIdMessage.getNewOrbit())
                        .build());
              });

          // Only add maxed formed orbit if the list is empty; we retroactively add if there's no
          // correlation, or if we find redundancy
          if (maxFormedOrbitIdList.size() == 0) {
            maxFormedOrbitIdList.add(newRefinedOrbitIdMessage.getNewOrbitId());
            maxFormedOrbitIds.set(maxFormedOrbitIdList);
          }
        }
      } catch (Exception e) {
        Utilities.log(context, String.format("OrbitIdManager cannot find properties file."), 1);
      }
    }

    // This message is received from an OrbitStatefulFunction when a new orbit is successfully
    // created.
    // It saves the new orbit id in its list and sends out a message to all other orbits in its list
    // to check for a correlation
    if (input instanceof CorrelateOrbitsMessage) {
      CorrelateOrbitsMessage correlateOrbitsMessage = (CorrelateOrbitsMessage) input;

      ArrayList<String> minFormedOrbitIdList =
          minFormedOrbitIds.getOrDefault(new ArrayList<String>());
      ArrayList<String> maxFormedOrbitIdList =
          maxFormedOrbitIds.getOrDefault(new ArrayList<String>());
      ArrayList<String> combinedOrbitIdList = new ArrayList<>();

      combinedOrbitIdList.addAll(minFormedOrbitIdList);
      combinedOrbitIdList.addAll(maxFormedOrbitIdList);

      // Send new id to all existing orbits to do calculation
      combinedOrbitIdList.forEach(
          orbitId -> {
            context.send(OrbitStatefulFunction.TYPE, orbitId, correlateOrbitsMessage);
          });

      KeyedOrbit keyedOrbit = KeyedOrbit.fromString(correlateOrbitsMessage.getStringContent());

      try {
        Integer trackCutoff = ApplicationProperties.getTrackCutoff();

        // Update minFormedOrbitIdList with the new orbit
        // TODO: figure out the interplay between saving here and when a refined orbit is created
        if (keyedOrbit.trackIds.size() <= trackCutoff) {
          minFormedOrbitIdList.add(keyedOrbit.orbitId);
          minFormedOrbitIds.set(minFormedOrbitIdList);
          // Message out that orbit id was saved
          Utilities.log(context, String.format("Saved orbitId %s", keyedOrbit.orbitId), 2);
        } else {
          // Only add maxed formed orbit if the list is empty; we retroactively add if there's no
          // correlation, or if we find redundancy
          if (maxFormedOrbitIdList.size() == 0) {
            maxFormedOrbitIdList.add(keyedOrbit.orbitId);
            maxFormedOrbitIds.set(maxFormedOrbitIdList);
            // Message out that orbit id was saved
            Utilities.log(context, String.format("Saved orbitId %s", keyedOrbit.orbitId), 2);
          }
        }
      } catch (Exception e) {
        Utilities.log(context, String.format("OrbitIdManager cannot find properties file."), 1);
      }
    }
    if (input instanceof AddMaxFormedOrbit) {
      AddMaxFormedOrbit addMaxFormedOrbit = (AddMaxFormedOrbit) input;

      ArrayList maxFormedOrbitIdList = maxFormedOrbitIds.getOrDefault(new ArrayList<String>());
      if (!maxFormedOrbitIdList.contains(addMaxFormedOrbit.getId())) {
        maxFormedOrbitIdList.add(addMaxFormedOrbit.getId());
        maxFormedOrbitIds.set(maxFormedOrbitIdList);
        Utilities.log(context, String.format("Saved orbitId %s", addMaxFormedOrbit.getId()), 2);
      }
    }

    // This message is sent from an OrbitStatefulFunction when that orbit expires. This removes that
    // orbit id from the orbit id list
    if (input instanceof RemoveOrbitIdMessage) {
      RemoveOrbitIdMessage removeOrbitIdMessage = (RemoveOrbitIdMessage) input;

      try {

        String orbitId = removeOrbitIdMessage.getStringContent();
        ArrayList minIds = minFormedOrbitIds.get();
        ArrayList maxIds = maxFormedOrbitIds.get();

        minIds.remove(orbitId);
        maxIds.remove(orbitId);

        // TODO: check if orbit already exists, and suppress message: currently log removes id
        // several times if it's correlated in more than one place
        // Message out that orbit id was removed
        Utilities.log(context, String.format("Removed orbitId %s from ID manager", orbitId), 1);
        minFormedOrbitIds.set(minIds);
        maxFormedOrbitIds.set(maxIds);

      } catch (Exception e) {

        // This exception is expected behavior for orbits that were deleted because they were over
        // the track limit
        Utilities.log(
            context,
            String.format(
                "OrbitId %s is not registered with OrbitIdManager - delete canceled: %s",
                removeOrbitIdMessage.getStringContent(), e),
            4);
      }
    }
  }

  private Long createNewId() {
    Long id = lastOrbitId.getOrDefault(-1L);
    id++;
    return id;
  }
}
