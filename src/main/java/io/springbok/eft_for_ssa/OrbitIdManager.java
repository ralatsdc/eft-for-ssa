package io.springbok.eft_for_ssa;

import org.apache.flink.statefun.sdk.Context;
import org.apache.flink.statefun.sdk.FunctionType;
import org.apache.flink.statefun.sdk.StatefulFunction;
import org.apache.flink.statefun.sdk.annotations.Persisted;
import org.apache.flink.statefun.sdk.state.PersistedValue;

import java.util.ArrayList;

public class OrbitIdManager implements StatefulFunction {

	public static final FunctionType TYPE = new FunctionType("springbok", "tracklet");
	@Persisted
	private final PersistedValue<ArrayList> savedIdList = PersistedValue.of("idList", ArrayList.class);

	@Override
	public void invoke(Context context, Object input) {

		// This is the bottleneck, so it should be as fast as possible
		// TODO: optimize this
		if (input instanceof AddOrbitMessage){
			ArrayList idList = savedIdList.get();
			AddOrbitMessage orbitMessage = (AddOrbitMessage) input;
			KeyedOrbit orbit = orbitMessage.getOrbit();

			// Send to all existing orbits to do calculation
			idList.forEach(id ->{
				context.send(OrbitState.TYPE, id.toString(), orbit);
			});

			idList.add(orbit.getId());
			savedIdList.set(idList);
		}
		if (input instanceof RemoveOrbitMessage) {
			ArrayList idList = savedIdList.get();
			RemoveOrbitMessage orbitMessage = (RemoveOrbitMessage) input;
			idList.remove(orbitMessage.getOrbitId());
			savedIdList.set(idList);
		}
	}
}
