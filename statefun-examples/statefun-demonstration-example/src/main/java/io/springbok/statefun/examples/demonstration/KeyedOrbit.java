package io.springbok.statefun.examples.demonstration;

import org.orekit.orbits.Orbit;

import java.util.ArrayList;

/*
 KeyedOrbit adds a key to the Orekit Orbit class. It also contains what trackIds and objectIds were used to create the Orbit.
*/
public class KeyedOrbit {

  public Orbit orbit;
  public String orbitId;
  public ArrayList<String> trackIds;
  public ArrayList<Integer> objectIds;

  // Construct a KeyedOrbit from a single track
  public KeyedOrbit(Orbit orbit, String orbitId, Track track) {
    this.orbit = orbit;
    this.orbitId = orbitId;
    this.trackIds = new ArrayList<>();
    trackIds.add(track.trackId);
    this.objectIds = new ArrayList<>();
    objectIds.add(track.objectId);
  }

  // Construct a KeyedOrbit from a list of tracks
  public KeyedOrbit(Orbit orbit, String orbitId, ArrayList<Track> tracks) {
    this.orbit = orbit;
    this.orbitId = orbitId;
    this.trackIds = new ArrayList<>();
    this.objectIds = new ArrayList<>();
    tracks.forEach(
        track -> {
          trackIds.add(track.trackId);
          objectIds.add(track.objectId);
        });
  }

  // Construct a KeyedOrbit with additional trackIds
  public KeyedOrbit(
      Orbit orbit, String orbitId, ArrayList<Track> tracks, ArrayList<String> trackIds) {

    this.orbit = orbit;
    this.orbitId = orbitId;
    this.objectIds = new ArrayList<>();
    this.trackIds = trackIds;
    tracks.forEach(
        track -> {
          trackIds.add(track.trackId);
          objectIds.add(track.objectId);
        });
  }

  // Compares the objectIds
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
