package io.springbok.statefun.examples.demonstration;

// Simple container message signaling that a new Track was created
public class NewTrackMessage {

  public Track track;

  public NewTrackMessage(Track track) {
    this.track = track;
  }
}
