package io.springbok.statefun.examples.demonstration;

import java.util.ArrayList;

public class OrbitFactoryTest {

  public static void main(String[] args) throws Exception {

    TrackGenerator trackGenerator =
        new TrackGenerator("../../tle-data/globalstar_tles_05_18_2020.txt");
    trackGenerator.init();

    ArrayList<String> singleObjectMessages = trackGenerator.getXSingleObjectMessages(2);

    Track track0 = Track.fromString(singleObjectMessages.get(0), "0");
    Track track1 = Track.fromString(singleObjectMessages.get(1), "1");

    ArrayList<Track> trackArrayList0 = new ArrayList<>();
    trackArrayList0.add(track0);
    ArrayList<Track> trackArrayList1 = new ArrayList<>();
    trackArrayList1.add(track1);

    KeyedOrbit keyedOrbit0 = OrbitFactory.createOrbit(track0, "0");
    KeyedOrbit keyedOrbit1 = OrbitFactory.createOrbit(track1, "1");

    KeyedOrbit keyedOrbit2 =
        OrbitFactory.refineOrbit(keyedOrbit0.orbit, keyedOrbit0.trackIds, trackArrayList1, "2");
    KeyedOrbit keyedOrbit3 =
        OrbitFactory.refineOrbit(keyedOrbit1.orbit, keyedOrbit1.trackIds, trackArrayList0, "3");

    System.out.println(keyedOrbit2.orbit);
    System.out.println(keyedOrbit3.orbit);
  }
}