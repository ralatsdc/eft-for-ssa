package io.springbok.statefun.examples.demonstration;

public class NewRefinedOrbitIdMessage {

  public String newOrbitId;
  public String oldOrbitId1;
  public String oldOrbitId2;

  public NewRefinedOrbitIdMessage(String newOrbitId, String oldOrbitId1, String oldOrbitId2) {
    this.newOrbitId = newOrbitId;
    this.oldOrbitId1 = oldOrbitId1;
    this.oldOrbitId2 = oldOrbitId2;
  }
}
