package io.springbok.statefun.examples.demonstration;

import org.orekit.orbits.Orbit;

import java.util.ArrayList;
import java.util.Iterator;

public class CollectedTracksMessage {

  public String keyedOrbitId1;
  public String keyedOrbitId2;
  public Orbit orbit1;
  public ArrayList<String> keyedOrbit1TrackIds;
  public ArrayList<String> keyedOrbit2TrackIds;
  public ArrayList<Track> keyedOrbit2Tracks;
  Iterator<String> trackIdIterator;

  CollectedTracksMessage(KeyedOrbit keyedOrbit1, KeyedOrbit keyedOrbit2) {
    this.keyedOrbitId1 = keyedOrbit1.orbitId;
    this.keyedOrbitId2 = keyedOrbit2.orbitId;
    this.orbit1 = keyedOrbit1.orbit;
    this.keyedOrbit1TrackIds = keyedOrbit1.trackIds;
    this.keyedOrbit2TrackIds = keyedOrbit2.trackIds;
    this.trackIdIterator = keyedOrbit2TrackIds.iterator();
    this.keyedOrbit2Tracks = new ArrayList<>();
  }

  public void addTrack(Track track) {
    keyedOrbit2Tracks.add(track);
  }

  public String getNextTrackId() {
    return trackIdIterator.next();
  }

  public boolean hasNextTrackId() {
    return trackIdIterator.hasNext();
  }
}
