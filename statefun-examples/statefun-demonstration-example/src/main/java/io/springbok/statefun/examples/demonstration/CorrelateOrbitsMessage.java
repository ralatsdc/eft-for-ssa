package io.springbok.statefun.examples.demonstration;

// Simple container message signaling to compare this orbit with another
public class CorrelateOrbitsMessage {
  KeyedOrbit keyedOrbit;

  public CorrelateOrbitsMessage(KeyedOrbit keyedOrbit) {
    this.keyedOrbit = keyedOrbit;
  }

  public KeyedOrbit getKeyedOrbit() {
    return keyedOrbit;
  }
}
