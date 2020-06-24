package io.springbok.eft_for_ssa.examples;

import org.orekit.orbits.Orbit;
import java.util.ArrayList;

public class KeyedOrbit {

  private Orbit orbit;
  private static long increment = 0;
  private long orbitId;
  private ArrayList<Long> trackletIds;

  /** Creates an IDOrbit with the given parameters. */
  public KeyedOrbit(Orbit orbit, Tracklet tracklet) {

    this.trackletIds = new ArrayList<>();
    this.trackletIds.add(tracklet.getId());
    this.orbit = orbit;
    this.orbitId = increment++;
  }

  public KeyedOrbit(Orbit orbit, ArrayList<Tracklet> tracklets) {

    this.trackletIds = new ArrayList<>();
    tracklets.forEach(tracklet -> this.trackletIds.add(tracklet.getId()));
    this.orbit = orbit;
    this.orbitId = increment++;
  }

  public ArrayList<Long> getTrackletsId() {
    return trackletIds;
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
