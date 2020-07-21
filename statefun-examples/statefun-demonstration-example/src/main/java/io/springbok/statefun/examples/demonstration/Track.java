package io.springbok.statefun.examples.demonstration;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.Position;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;

import java.util.ArrayList;

// Track stores information from a TrackIn string in a more usable interface
public class Track {

  private AbsoluteDate msgTime;
  public int sensorId;
  public int objectId;
  private ArrayList<Position> positions;
  private ArrayList<Double> rcsArr;

  public String trackId;
  private ArrayList<String> orbitIds;

  private static TimeScale utc = TimeScalesFactory.getUTC();

  @Override
  public String toString() {
    return msgTime
        + ","
        + sensorId
        + ","
        + objectId
        + ","
        + String.join(", ", positions.toString());
  }

  // Parse a Track from a CSV representation.
  public static Track fromString(String line, String id) {

    // Ensure Orekit is configured
    OrbitFactory.init();

    String[] tokens = line.split(",");
    if (tokens.length % 5 != 3) {
      throw new RuntimeException("Invalid record: " + line);
    }

    Track track = new Track();

    // Create track from string
    try {

      track.msgTime = new AbsoluteDate(tokens[0], utc);
      track.sensorId = Integer.parseInt(tokens[1]);
      track.objectId = Integer.parseInt(tokens[2]);

      ArrayList<Position> positions = new ArrayList<Position>();
      ArrayList<Double> rcsArr = new ArrayList<Double>();

      ObservableSatellite satelliteIndex = new ObservableSatellite(0);

      for (int i = 3; i < tokens.length; i = i + 5) {

        AbsoluteDate date = new AbsoluteDate(tokens[i], utc);
        double x = Double.parseDouble(tokens[(i + 1)]);
        double y = Double.parseDouble(tokens[i + 2]);
        double z = Double.parseDouble(tokens[i + 3]);
        Vector3D vector = new Vector3D(x, y, z);
        Position position = new Position(date, vector, 1, 1, satelliteIndex);

        rcsArr.add(Double.parseDouble(tokens[i + 4]));

        positions.add(position);
      }

      track.positions = positions;
      track.rcsArr = rcsArr;

      track.trackId = id;
      track.orbitIds = new ArrayList<>();

    } catch (NumberFormatException nfe) {
      throw new RuntimeException("Invalid record: " + line, nfe);
    }

    return track;
  }

  public void addOrbitId(String orbitId) {
    orbitIds.add(orbitId);
  }

  public void removeOrbitId(String orbitId) {
    orbitIds.remove(orbitId);
  }

  public AbsoluteDate getMsgTime() {
    return msgTime;
  }

  public void setMsgTime(AbsoluteDate msgTime) {
    this.msgTime = msgTime;
  }

  public void setPositions(ArrayList<Position> positions) {
    this.positions = positions;
  }

  public ArrayList<Position> getPositions() {
    return positions;
  }

  public void setRcsArr(ArrayList<Double> rcsArr) {
    this.rcsArr = rcsArr;
  }

  public ArrayList<Double> getRcsArr() {
    return rcsArr;
  }

  public void setOrbitIds(ArrayList<String> orbitIds) {
    this.orbitIds = orbitIds;
  }

  public ArrayList<String> getOrbitIds() {
    return orbitIds;
  }
}
