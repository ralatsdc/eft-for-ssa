package io.springbok.statefun.examples.demonstration;

// Simple container message signaling to remove an orbit id
public class RemoveOrbitIdMessage {

  public final String orbitId;

  public RemoveOrbitIdMessage(String orbitId) {
    this.orbitId = orbitId;
  }
}
