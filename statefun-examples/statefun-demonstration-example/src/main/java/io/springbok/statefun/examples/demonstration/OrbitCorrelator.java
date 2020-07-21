package io.springbok.statefun.examples.demonstration;

// Class used to determine if two orbits correlate
public class OrbitCorrelator {

  public static boolean correlate(KeyedOrbit orbit1, KeyedOrbit orbit2) {
    return orbit1.equals(orbit2);
  }
}
