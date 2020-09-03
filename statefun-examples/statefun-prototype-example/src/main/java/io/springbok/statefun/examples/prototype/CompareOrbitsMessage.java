package io.springbok.statefun.examples.prototype;

public class CompareOrbitsMessage {

  KeyedOrbit orbit;

  CompareOrbitsMessage(KeyedOrbit orbit) {
    this.orbit = orbit;
  }

  public KeyedOrbit getOrbit() {
    return orbit;
  }
}
