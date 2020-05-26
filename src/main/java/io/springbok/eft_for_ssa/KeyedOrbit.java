package io.springbok.eft_for_ssa;

import org.orekit.orbits.Orbit;

public class KeyedOrbit {
	
	private Orbit orbit;
	private static long increment = 0;
	private long orbitId;

	/**
	 * Creates an IDOrbit with the given parameters.
	 */
	public KeyedOrbit(Orbit orbit) {	
		
		this.orbit = orbit;
		this.orbitId = increment++;
			
	}
	
	public Orbit getOrbit() {
		return orbit;
	}
	
	public long getId() {
		return orbitId;
	}
	
}
