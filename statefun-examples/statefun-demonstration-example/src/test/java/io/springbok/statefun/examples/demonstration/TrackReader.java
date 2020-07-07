package io.springbok.statefun.examples.demonstration;

import java.io.*;
import java.util.ArrayList;

// This class reads data created from TrackGenerator
// Run that class before using this one
public class TrackReader {

	ArrayList<String> tracks;

	TrackReader() throws IOException{

		String path = "../../output/test-track-messages.txt";
		File file = new File(path);
		tracks = new ArrayList<String>();
		BufferedReader tracksReader;
		try {
			tracksReader = new BufferedReader(new FileReader(file));
			String line = tracksReader.readLine();

			// Loop until file end
			while (line != null) {
				String track = line;
				tracks.add(track);

				line = tracksReader.readLine();
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public ArrayList<String> getTracks() {
		return tracks;
	}

	//TODO: add error handling for x being larger than file length
	public ArrayList<String> getXTracks(int x){
		ArrayList<String> slicedTracks = new ArrayList<String>();
		for(int i=0; i<x; i++){
			slicedTracks.add(tracks.get(i));
		}
		return slicedTracks;
	}
}
