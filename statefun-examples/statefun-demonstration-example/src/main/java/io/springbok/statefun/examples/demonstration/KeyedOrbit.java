package io.springbok.statefun.examples.demonstration;

import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;

import java.util.ArrayList;
import java.util.Arrays;

/*
 KeyedOrbit adds a key to the Orekit Orbit class. It also contains what trackIds and objectIds were used to create the Orbit.
*/
public class KeyedOrbit {

  public Orbit orbit;
  public String orbitId;
  public ArrayList<String> trackIds;
  public String universe;
  public ArrayList<Integer> objectIds;

  // Construct a KeyedOrbit from a single track
  public KeyedOrbit(Orbit orbit, String orbitId, Track track) {
    this.orbit = orbit;
    this.orbitId = orbitId;
    this.trackIds = new ArrayList<>();
    trackIds.add(track.trackId);
    this.objectIds = new ArrayList<>();
    objectIds.add(track.objectId);
    this.universe = track.universe;
  }

  // Construct a KeyedOrbit from a list of tracks
  public KeyedOrbit(Orbit orbit, String orbitId, ArrayList<Track> tracks) {
    this.orbit = orbit;
    this.orbitId = orbitId;
    this.trackIds = new ArrayList<>();
    this.objectIds = new ArrayList<>();
    // All track universes should be the same
    this.universe = tracks.get(0).universe;
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
    // All track universes should be the same
    this.universe = tracks.get(0).universe;
    tracks.forEach(
        track -> {
          if (!trackIds.contains(track.trackId)) {
            trackIds.add(track.trackId);
          }
          if (!objectIds.contains(track.objectId)) {
            objectIds.add(track.objectId);
          }
        });
  }

  private KeyedOrbit() {}

  @Override
  public String toString() {
    KeplerianOrbit keplerianOrbit = new KeplerianOrbit(orbit);
    return orbit.getA()
        + ";"
        + orbit.getE()
        + ";"
        + orbit.getI()
        + ";"
        + keplerianOrbit.getPerigeeArgument()
        + ";"
        + keplerianOrbit.getRightAscensionOfAscendingNode()
        + ";"
        + keplerianOrbit.getTrueAnomaly()
        + ";"
        + keplerianOrbit.getDate()
        + ";"
        + orbitId
        + ";"
        + trackIds
        + ";"
        + objectIds
        + ";"
        + universe;
  }

  public static KeyedOrbit fromString(String line) {

    String[] tokens = line.split(";");
    if (tokens.length != 11) {
      throw new RuntimeException("Invalid string to form KeyedOrbit: " + line);
    }

    KeyedOrbit keyedOrbit = new KeyedOrbit();

    // Create KeyedOrbit from string
    try {

      Orbit orbit = OrbitFactory.fromTokens(Arrays.copyOfRange(tokens, 0, 7));
      keyedOrbit.orbit = orbit;

      keyedOrbit.orbitId = tokens[7];

      ArrayList<String> trackIds =
          new ArrayList<>(Arrays.asList(tokens[8].replaceAll("[\\[\\]]", "").split(", ")));
      keyedOrbit.trackIds = trackIds;

      String[] objectIdStrings = tokens[9].replaceAll("[\\[\\]]", "").split(", ");
      ArrayList<Integer> objectIds = new ArrayList<>();
      for (int i = 0; i < objectIdStrings.length; i++) {
        objectIds.add(Integer.parseInt(objectIdStrings[i]));
      }
      keyedOrbit.objectIds = objectIds;

      keyedOrbit.universe = tokens[10];

    } catch (NumberFormatException nfe) {
      throw new RuntimeException("Invalid record: " + line, nfe);
    }

    return keyedOrbit;
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

  // Redundancy is defined here as an orbit being comprised entirely with tracks contained in
  // another orbit
  // A return value of 0 - neither orbit is redundant
  // A return value of 1 - orbit 1 is redundant OR both orbits are the same (does not complete the
  // check)
  // A return value of 2 - orbit 2 is redundant
  public static int checkRedundancy(KeyedOrbit keyedOrbit1, KeyedOrbit keyedOrbit2)
      throws Exception {

    ArrayList trackIds1 = new ArrayList(keyedOrbit1.trackIds);
    ArrayList trackIds2 = new ArrayList(keyedOrbit2.trackIds);

    trackIds1.removeAll(trackIds2);
    trackIds2.removeAll(trackIds1);

    if (trackIds1.size() == 0) {
      return 1;
    } else if (trackIds2.size() == 0) {
      return 2;
    } else {
      return 0;
    }
  }
}
