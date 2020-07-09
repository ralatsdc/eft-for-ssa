package io.springbok.statefun.examples.demonstration;

public class RefinedOrbitMessage {

	public KeyedOrbit keyedOrbit;
	public RefinedOrbitMessage(KeyedOrbit keyedOrbit) {
		this.keyedOrbit = keyedOrbit;
	}

	public KeyedOrbit getKeyedOrbit() {
		return keyedOrbit;
	}
}
