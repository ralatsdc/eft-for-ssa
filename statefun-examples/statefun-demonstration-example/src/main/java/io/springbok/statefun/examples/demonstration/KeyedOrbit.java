package io.springbok.statefun.examples.demonstration;

import org.orekit.orbits.Orbit;

import java.util.ArrayList;

public class KeyedOrbit {

  public Orbit orbit;
  public String orbitId;
  public ArrayList<String> trackIds;

  /** Creates an IDOrbit with the given parameters. */
  public KeyedOrbit(Orbit orbit, String orbitId, String trackId) {

    this.orbit = orbit;
    this.orbitId = orbitId;
    this.trackIds = new ArrayList<>();
    this.trackIds.add(trackId);
  }

  public KeyedOrbit(Orbit orbit, String orbitId, ArrayList<Track> tracks) {

    this.orbit = orbit;
    this.orbitId = orbitId;
    this.trackIds = new ArrayList<>();
    tracks.forEach(track -> this.trackIds.add(track.trackId));
  }
}
