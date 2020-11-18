package io.springbok.statefun.examples.utilities;

import io.springbok.statefun.examples.demonstration.generated.SingleLineTLE;
import org.orekit.propagation.analytical.tle.TLE;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class TLEReader {

  public static ArrayList<TLE> readTLEs(File tleData) throws IOException {

    ArrayList<TLE> tles = new ArrayList<TLE>();
    BufferedReader tleReader;
    try {
      tleReader = new BufferedReader(new FileReader(tleData));
      String line = tleReader.readLine();

      // Loop until file end
      while (line != null) {

        TLE tle = new TLE(line, tleReader.readLine());
        tles.add(tle);

        line = tleReader.readLine();
      }

    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    // Sort TLEs by TLE epoch
    Collections.sort(
        tles,
        new Comparator<TLE>() {
          @Override
          public int compare(TLE tleOne, TLE tleTwo) {
            return tleOne.getDate().compareTo(tleTwo.getDate());
          }
        });
    return tles;
  }

  public static SingleLineTLE toSingleLineTLE(TLE tle) {
    String lines = tle.getLine1() + "&&" + tle.getLine2();

    return SingleLineTLE.newBuilder()
        .setSatelliteNumber(String.valueOf(tle.getSatelliteNumber()))
        .setLines(lines)
        .build();
  }

  public static TLE fromSingleLineTLE(SingleLineTLE singleLineTLE) {
    String[] lines = singleLineTLE.getLines().split("&&");
    TLE tle = new TLE(lines[0], lines[1]);

    return tle;
  }
}
