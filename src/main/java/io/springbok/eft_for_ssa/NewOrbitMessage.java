package io.springbok.eft_for_ssa;

public class NewOrbitMessage {

	KeyedOrbit orbit;

	NewOrbitMessage(KeyedOrbit orbit){
		this.orbit = orbit;
	}

	public KeyedOrbit getOrbit() {
		return orbit;
	}
}
