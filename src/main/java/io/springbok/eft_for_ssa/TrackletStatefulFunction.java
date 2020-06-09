package io.springbok.eft_for_ssa;

import org.apache.flink.statefun.sdk.Context;
import org.apache.flink.statefun.sdk.FunctionType;
import org.apache.flink.statefun.sdk.StatefulFunction;
import org.apache.flink.statefun.sdk.annotations.Persisted;
import org.apache.flink.statefun.sdk.state.PersistedValue;

public class TrackletStatefulFunction implements StatefulFunction {

	public static final FunctionType TYPE = new FunctionType("springbok", "tracklet");
	@Persisted
	private final PersistedValue<Tracklet> trackletState = PersistedValue.of("tracklet", Tracklet.class);

	@Override
	public void invoke(Context context, Object input) {
		if (input instanceof Tracklet) {
			Tracklet tracklet = (Tracklet) input;
			// Create Orbit
			KeyedOrbit orbit = OrbitBuilder.createOrbit(tracklet);
			// Add id to tracklet
			tracklet.addOrbit(orbit.getId());
			trackletState.set(tracklet);
			// Send to orbit state for save
			context.send(OrbitStatefulFunction.TYPE, String.valueOf(orbit.getId()), new NewOrbitMessage(orbit));
		}
		if (input instanceof AddOrbitMessage){
			Tracklet tracklet = trackletState.get();
			AddOrbitMessage orbitMessage = (AddOrbitMessage) input;
			Long orbitId = orbitMessage.getOrbitId();
			tracklet.addOrbit(orbitId);
			trackletState.set(tracklet);
		}
		if (input instanceof RemoveOrbitMessage){
			Tracklet tracklet = trackletState.get();
			RemoveOrbitMessage orbitMessage = (RemoveOrbitMessage) input;
			Long orbitId = orbitMessage.getOrbitId();
			tracklet.removeOrbit(orbitId);

			//TODO: potential problem here. If the manager sends a message to an almost expired orbit, and that orbit successfully compares - we have instance of a tracklet being cleared before it can give itself to the refined orbit calculation
			if (tracklet.getOrbitIds().size() == 0){
				trackletState.clear();
			} else {
				trackletState.set(tracklet);
			}
		}
		if (input instanceof CollectedTrackletsMessage){
			Tracklet tracklet = trackletState.get();
			CollectedTrackletsMessage collectedTrackletsMessage = (CollectedTrackletsMessage) input;
			collectedTrackletsMessage.addTracklet(tracklet);
			collectedTrackletsMessage.removeTrackletId(tracklet.getId());
			if (collectedTrackletsMessage.emptyIdList()){
				// Route back to orbit to do calculation
				context.send(OrbitStatefulFunction.TYPE, String.valueOf(collectedTrackletsMessage.getOrbitId()), collectedTrackletsMessage);
			} else {
				// Send to next tracklet on list
				context.send(TrackletStatefulFunction.TYPE, collectedTrackletsMessage.getRoute(), collectedTrackletsMessage);
			}
		}
	}
}
