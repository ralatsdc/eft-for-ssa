package io.springbok.statefun.examples.demonstration;

public class CorrelateOrbitsMessage {
	KeyedOrbit keyedOrbit;
	public CorrelateOrbitsMessage(KeyedOrbit keyedOrbit) {
		this.keyedOrbit = keyedOrbit;
	}

	public KeyedOrbit getKeyedOrbit() {
		return keyedOrbit;
	}
}
