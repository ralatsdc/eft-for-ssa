package io.springbok.eft_for_ssa;

import org.apache.flink.statefun.sdk.Context;
import org.apache.flink.statefun.sdk.FunctionType;
import org.apache.flink.statefun.sdk.StatefulFunction;
import org.apache.flink.statefun.sdk.annotations.Persisted;
import org.apache.flink.statefun.sdk.state.PersistedValue;

public class TrackletState implements StatefulFunction {

	public static final FunctionType TYPE = new FunctionType("springbok", "tracklet");
	@Persisted
	private final PersistedValue<Tracklet> savedTracklet = PersistedValue.of("tracklet", Tracklet.class);

	@Override
	public void invoke(Context context, Object input) {
		if (input instanceof Tracklet) {
			Tracklet tracklet = (Tracklet) input;
			savedTracklet.set(tracklet);
			// Create Orbit
			KeyedOrbit orbit = OrbitBuilder.createOrbit(tracklet);
			// Add id to tracklet
			tracklet.addOrbit(orbit.getId());
			// Send to orbit state for save
			context.send(OrbitState.TYPE, String.valueOf(orbit.getId()), new TrackletMessage(orbit));
		}
		if (input instanceof AddOrbitMessage){
			Tracklet tracklet = savedTracklet.get();
			AddOrbitMessage orbitMessage = (AddOrbitMessage) input;
			Long orbitId = orbitMessage.getOrbitId();
			tracklet.addOrbit(orbitId);
			savedTracklet.set(tracklet);
		}
		if (input instanceof RemoveOrbitMessage) {
			Tracklet tracklet = savedTracklet.get();
			RemoveOrbitMessage orbitMessage = (RemoveOrbitMessage) input;
			Long orbitId = orbitMessage.getOrbitId();
			tracklet.removeOrbit(orbitId);
			savedTracklet.set(tracklet);
		}
	}
}
