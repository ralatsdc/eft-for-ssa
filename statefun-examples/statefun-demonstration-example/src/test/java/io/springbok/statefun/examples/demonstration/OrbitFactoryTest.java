package io.springbok.statefun.examples.demonstration;

import io.springbok.statefun.examples.utilities.SetTestPaths;
import io.springbok.statefun.examples.utilities.TrackGenerator;

import java.util.ArrayList;

public class OrbitFactoryTest {

  public static void main(String[] args) throws Exception {

    SetTestPaths.init();
    TrackGenerator trackGenerator = new TrackGenerator();
    trackGenerator.init();
    trackGenerator.finitePropagation();

    ArrayList<String> singleIdMessages = trackGenerator.getMessagesById(25875);

    Track track0 = Track.fromString(singleIdMessages.get(0), "0");
    Track track1 = Track.fromString(singleIdMessages.get(1), "1");

    ArrayList<Track> trackArrayList0 = new ArrayList<>();
    trackArrayList0.add(track0);
    ArrayList<Track> trackArrayList1 = new ArrayList<>();
    trackArrayList1.add(track1);

    KeyedOrbit keyedOrbit0 = OrbitFactory.createOrbit(track0, "0");
    KeyedOrbit keyedOrbit1 = OrbitFactory.createOrbit(track1, "1");

    System.out.println(keyedOrbit0.orbit);
    System.out.println(keyedOrbit1.orbit);

    KeyedOrbit keyedOrbit2 =
        OrbitFactory.refineOrbit(keyedOrbit0.orbit, keyedOrbit0.trackIds, trackArrayList1, "2");
    KeyedOrbit keyedOrbit3 =
        OrbitFactory.refineOrbit(keyedOrbit1.orbit, keyedOrbit1.trackIds, trackArrayList0, "3");

    System.out.println(keyedOrbit2.orbit);
    System.out.println(keyedOrbit3.orbit);

    System.out.println(singleIdMessages);
    orbitCorrelation(trackGenerator);
  }

  private static void orbitCorrelation(TrackGenerator trackGenerator) throws Exception {
    ArrayList<String> singleObjectMessages = trackGenerator.getXSingleObjectMessages(2);

    Track track0 = Track.fromString(singleObjectMessages.get(0), "0");
    Track track1 = Track.fromString(singleObjectMessages.get(1), "1");

    ArrayList<Track> trackArrayList0 = new ArrayList<>();
    trackArrayList0.add(track0);
    ArrayList<Track> trackArrayList1 = new ArrayList<>();
    trackArrayList1.add(track1);

    KeyedOrbit keyedOrbit0 = OrbitFactory.createOrbit(track0, "0");
    KeyedOrbit keyedOrbit1 = OrbitFactory.createOrbit(track1, "1");

    if (OrbitCorrelator.correlate(keyedOrbit0, keyedOrbit1)) {
      KeyedOrbit keyedOrbit2 =
          OrbitFactory.refineOrbit(keyedOrbit0.orbit, keyedOrbit0.trackIds, trackArrayList1, "2");
      KeyedOrbit keyedOrbit3 =
          OrbitFactory.refineOrbit(keyedOrbit1.orbit, keyedOrbit1.trackIds, trackArrayList0, "3");

      System.out.println(keyedOrbit2.orbit);
      System.out.println(keyedOrbit3.orbit);
    }
  }
}
