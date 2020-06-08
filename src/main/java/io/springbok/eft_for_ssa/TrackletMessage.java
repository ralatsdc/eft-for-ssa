package io.springbok.eft_for_ssa;

public class TrackletMessage {

	KeyedOrbit orbit;

	TrackletMessage(KeyedOrbit orbit){
		this.orbit = orbit;
	}

	public KeyedOrbit getOrbit() {
		return orbit;
	}
}
