package io.springbok.eft_for_ssa;

import org.apache.flink.statefun.sdk.Context;
import org.apache.flink.statefun.sdk.FunctionType;
import org.apache.flink.statefun.sdk.StatefulFunction;
import org.apache.flink.statefun.sdk.annotations.Persisted;
import org.apache.flink.statefun.sdk.state.PersistedValue;

import java.time.Duration;
import java.util.ArrayList;

public class OrbitState implements StatefulFunction {

	public static final FunctionType TYPE = new FunctionType("springbok", "orbit");

	@Persisted
	private final PersistedValue<KeyedOrbit> savedOrbit = PersistedValue.of("orbit", KeyedOrbit.class);

	@Override
	public void invoke(Context context, Object input) {

		if (input instanceof TrackletMessage) {

			TrackletMessage message = (TrackletMessage) input;
			KeyedOrbit orbit = message.getOrbit();

			// Persist orbit state and set delete message
			savedOrbit.set(orbit);
			context.sendAfter(Duration.ofDays(2), context.self(), new DelayedDeleteMessage());

			// Send orbit ID to manager
			context.send(OrbitIdManager.TYPE, "manager", new AddOrbitMessage(orbit));

			KeyedOrbit response = orbit;
			context.send(IO.EGRESS_ID, response);
		}

		if (input instanceof DelayedDeleteMessage){
			KeyedOrbit orbit = savedOrbit.get();
			ArrayList<Long> ids = orbit.getTrackletsId();

			// Send message to manager
			context.send(OrbitIdManager.TYPE, "manager", new RemoveOrbitMessage(orbit.getId()));

			// Send message to tracklet(s)
			ids.forEach(id ->{
				context.send(TrackletState.TYPE, String.valueOf(id), new RemoveOrbitMessage(orbit.getId()));
			});

			savedOrbit.clear();
		}

		// Message from manager
		if (input instanceof KeyedOrbit){
			KeyedOrbit inputOrbit = (KeyedOrbit) input;
			KeyedOrbit orbitState = savedOrbit.get();

			// Compare these two orbits;
		}
	}

	private class DelayedDeleteMessage {
	}
}
