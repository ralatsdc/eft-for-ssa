package io.springbok.statefun.examples.demonstration;

// Simple container message signaling that a new orbit id was created
public class NewOrbitIdMessage {

  public String id;

  public NewOrbitIdMessage(String id) {
    this.id = id;
  }
}
