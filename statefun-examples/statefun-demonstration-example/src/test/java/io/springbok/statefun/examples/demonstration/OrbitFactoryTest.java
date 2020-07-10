package io.springbok.statefun.examples.demonstration;

import java.util.ArrayList;

public class OrbitFactoryTest {

	public static void main(String[] args) throws Exception {

		System.out.println("Working Directory = " + System.getProperty("user.dir"));

		TrackGenerator trackGenerator = new TrackGenerator("../../tle-data/globalstar_tles_05_18_2020.txt");
		trackGenerator.init();

		ArrayList<String> singleObjectMessages = trackGenerator.getXSingleObjectMessages(2);

		Track track0 = Track.fromString(singleObjectMessages.get(0), "0");
		Track track1 = Track.fromString(singleObjectMessages.get(1), "1");

		ArrayList<Track> trackArrayList1 = new ArrayList<>();
		trackArrayList1.add(track1);

		KeyedOrbit keyedOrbit0 = OrbitFactory.createOrbit(track0,"0");

		KeyedOrbit newOrbit = OrbitFactory.refineOrbit(keyedOrbit0.orbit, trackArrayList1, "2");

		System.out.println(newOrbit);
	}
}
