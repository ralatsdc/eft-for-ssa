package io.springbok.eft_for_ssa;

import org.apache.flink.statefun.sdk.Context;
import org.apache.flink.statefun.sdk.FunctionType;
import org.apache.flink.statefun.sdk.StatefulFunction;
import org.apache.flink.statefun.sdk.annotations.Persisted;
import org.apache.flink.statefun.sdk.state.PersistedValue;

import java.util.ArrayList;

public class OrbitIdStatefulFunction implements StatefulFunction {

	public static final FunctionType TYPE = new FunctionType("springbok", "tracklet");
	@Persisted
	private final PersistedValue<ArrayList> savedOrbitIdList = PersistedValue.of("idList", ArrayList.class);

	@Override
	public void invoke(Context context, Object input) {

		if (input instanceof AddOrbitMessage){
			ArrayList idList = savedOrbitIdList.getOrDefault(new ArrayList<Long>());
			AddOrbitMessage orbitMessage = (AddOrbitMessage) input;
			KeyedOrbit orbit = orbitMessage.getOrbit();

			// Send to all existing orbits to do calculation
			// TODO: deal with flink requiring string IDs and keyedorbits and tracklets incrementing long IDs
			idList.forEach(id ->{
				context.send(OrbitStatefulFunction.TYPE, id.toString(), new CompareOrbitsMessage(orbit));
			});

			idList.add(orbit.getId());
			savedOrbitIdList.set(idList);
		}
		if (input instanceof RemoveOrbitMessage) {
			ArrayList idList = savedOrbitIdList.get();
			RemoveOrbitMessage orbitMessage = (RemoveOrbitMessage) input;
			idList.remove(orbitMessage.getOrbitId());
			savedOrbitIdList.set(idList);
		}
	}
}
