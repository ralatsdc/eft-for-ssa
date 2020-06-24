package io.springbok.eft_for_ssa.examples;

public class CompareOrbitsMessage {

  KeyedOrbit orbit;

  CompareOrbitsMessage(KeyedOrbit orbit) {
    this.orbit = orbit;
  }

  public KeyedOrbit getOrbit() {
    return orbit;
  }
}
