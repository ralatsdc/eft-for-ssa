package io.springbok.statefun.examples.demonstration;

import org.orekit.orbits.Orbit;

import java.util.ArrayList;

public class KeyedOrbit {

  public Orbit orbit;
  public String orbitId;
  public ArrayList<String> trackIds;
  public ArrayList<Integer> objectIds;

  /** Creates an IDOrbit with the given parameters. */
  public KeyedOrbit(Orbit orbit, String orbitId, Track track) {
    this.orbit = orbit;
    this.orbitId = orbitId;
    this.trackIds = new ArrayList<>();
    trackIds.add(track.trackId);
    this.objectIds = new ArrayList<>();
    objectIds.add(track.objectId);
  }

  public KeyedOrbit(Orbit orbit, String orbitId, ArrayList<Track> tracks) {

    this.orbit = orbit;
    this.orbitId = orbitId;
    this.trackIds = new ArrayList<>();
    this.objectIds = new ArrayList<>();
    tracks.forEach(track -> {
      trackIds.add(track.trackId);
      objectIds.add(track.objectId);
    });
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof KeyedOrbit)) {
      return false;
    }
    if (this == obj) {
      return true;
    }
    KeyedOrbit other = (KeyedOrbit) obj;

    if (!other.objectIds.equals(this.objectIds)) {
      return false;
    }
    return true;
  }
}
