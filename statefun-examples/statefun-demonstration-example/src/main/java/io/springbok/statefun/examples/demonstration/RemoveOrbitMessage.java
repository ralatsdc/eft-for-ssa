package io.springbok.statefun.examples.demonstration;

public class RemoveOrbitMessage {

  private final Long orbitId;

  public RemoveOrbitMessage(Long orbitId) {
    this.orbitId = orbitId;
  }

  public Long getOrbitId() {
    return orbitId;
  }
}
