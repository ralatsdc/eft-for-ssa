package io.springbok.statefun.examples.demonstration;

public class NewOrbitMessage {

  KeyedOrbit orbit;

  NewOrbitMessage(KeyedOrbit orbit) {
    this.orbit = orbit;
  }

  public KeyedOrbit getOrbit() {
    return orbit;
  }
}
