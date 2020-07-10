package io.springbok.statefun.examples.demonstration;

public class RefinedOrbitMessage {

	public KeyedOrbit keyedOrbit;
	public RefinedOrbitMessage(KeyedOrbit newOrbitkeyedOrbit) {
		this.keyedOrbit = keyedOrbit;
	}

	public KeyedOrbit getKeyedOrbit() {
		return keyedOrbit;
	}
}
