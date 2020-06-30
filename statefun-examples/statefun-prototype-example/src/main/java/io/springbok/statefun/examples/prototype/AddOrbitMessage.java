package io.springbok.statefun.examples.prototype;

public class AddOrbitMessage {

  private final KeyedOrbit orbit;

  public AddOrbitMessage(KeyedOrbit orbit) {
    this.orbit = orbit;
  }

  public KeyedOrbit getOrbit() {
    return orbit;
  }

  public Long getOrbitId() {
    return orbit.getId();
  }
}