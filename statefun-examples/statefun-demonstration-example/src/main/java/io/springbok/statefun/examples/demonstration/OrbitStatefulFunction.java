package io.springbok.statefun.examples.demonstration;

import io.springbok.statefun.examples.demonstration.generated.DefaultOut;
import org.apache.flink.statefun.sdk.Context;
import org.apache.flink.statefun.sdk.FunctionType;
import org.apache.flink.statefun.sdk.StatefulFunction;
import org.apache.flink.statefun.sdk.annotations.Persisted;
import org.apache.flink.statefun.sdk.state.PersistedValue;

public class OrbitStatefulFunction implements StatefulFunction {

  public static final FunctionType TYPE = new FunctionType("springbok", "orbit");

  @Persisted
  private final PersistedValue<KeyedOrbit> orbitState =
      PersistedValue.of("orbit", KeyedOrbit.class);

  @Override
  public void invoke(Context context, Object input) {

    if (input instanceof NewOrbitMessage) {

      NewOrbitMessage message = (NewOrbitMessage) input;
      KeyedOrbit orbit = message.getOrbit();

      // Persist orbit state and set delete message
      orbitState.set(orbit);
      //      context.sendAfter(Duration.ofDays(2), context.self(), new DelayedDeleteMessage());

      // Send orbit to manager
      //      context.send(OrbitIdStatefulFunction.TYPE, "manager", new AddOrbitMessage(orbit));

      KeyedOrbit response = orbit;
      context.send(
          DemonstrationIO.DEFAULT_EGRESS_ID,
          DefaultOut.newBuilder().setContent(orbit.getOrbit().toString()).build());
    }

    //    if (input instanceof DelayedDeleteMessage) {
    //      KeyedOrbit orbit = orbitState.get();
    //      ArrayList<Long> ids = orbit.getTrackletsId();
    //
    //      // Send message to manager
    //      context.send(OrbitIdStatefulFunction.TYPE, "manager", new
    // RemoveOrbitMessage(orbit.getId()));
    //
    //      // Send message to tracklet(s)
    //      ids.forEach(
    //          id -> {
    //            context.send(
    //                TrackletStatefulFunction.TYPE,
    //                String.valueOf(id),
    //                new RemoveOrbitMessage(orbit.getId()));
    //          });
    //
    //      orbitState.clear();
    //    }
    //
    //    // Message from manager
    //    if (input instanceof CompareOrbitsMessage) {
    //      CompareOrbitsMessage message = (CompareOrbitsMessage) input;
    //      KeyedOrbit recievedOrbit = message.getOrbit();
    //      KeyedOrbit orbit = orbitState.get();
    //
    //      if (CompareOrbits.compareAtRandom(recievedOrbit, orbit)) {
    //        // Get tracklets from current orbitState
    //        CollectedTrackletsMessage collectedTrackletsMessage =
    //            new CollectedTrackletsMessage(recievedOrbit, orbit);
    //        context.send(IO.STRING_EGRESS_ID, "Sending Orbit Comparison");
    //        context.send(
    //            TrackletStatefulFunction.TYPE,
    //            collectedTrackletsMessage.getRoute(),
    //            collectedTrackletsMessage);
    //      }
    //    }
    //
    //    // Orbit least squares refine
    //    if (input instanceof CollectedTrackletsMessage) {
    //      // call orbit builder
    //      CollectedTrackletsMessage message = (CollectedTrackletsMessage) input;
    //      KeyedOrbit orbit = orbitState.get();
    //      KeyedOrbit newOrbit = OrbitBuilder.refineOrbit(orbit, message.getTracklets());
    //
    //      context.send(
    //          OrbitStatefulFunction.TYPE,
    //          String.valueOf(newOrbit.getId()),
    //          new NewOrbitMessage(newOrbit));
    //    }
  }
  //
  //  private class DelayedDeleteMessage {}
}
