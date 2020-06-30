package io.springbok.statefun.examples.demonstration;

import org.orekit.orbits.Orbit;

import java.util.ArrayList;

public class KeyedOrbit {

  private Orbit orbit;
  private static long increment = 0;
  private long orbitId;
  private ArrayList<Long> trackIds;

  /** Creates an IDOrbit with the given parameters. */
  public KeyedOrbit(Orbit orbit, Track track) {

    this.trackIds = new ArrayList<>();
    this.trackIds.add(track.getId());
    this.orbit = orbit;
    this.orbitId = increment++;
  }

  public KeyedOrbit(Orbit orbit, ArrayList<Track> tracks) {

    this.trackIds = new ArrayList<>();
    tracks.forEach(track -> this.trackIds.add(track.getId()));
    this.orbit = orbit;
    this.orbitId = increment++;
  }

  public ArrayList<Long> getTracksId() {
    return trackIds;
  }

  public Orbit getOrbit() {
    return orbit;
  }

  public String getStringId() {
    return String.valueOf(orbitId);
  }

  public long getId() {
    return orbitId;
  }
}
