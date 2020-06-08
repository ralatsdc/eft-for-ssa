package io.springbok.eft_for_ssa;

import org.apache.flink.statefun.sdk.io.Router;

public class OrbitRouter implements Router<KeyedOrbit> {

	@Override
	public void route(KeyedOrbit message, Downstream<KeyedOrbit> downstream) {
		downstream.forward(OrbitState.TYPE, message.getStringId(), message);
	}
}
