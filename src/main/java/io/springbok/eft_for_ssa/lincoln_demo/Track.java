package io.springbok.eft_for_ssa.lincoln_demo;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.Position;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;

import java.util.ArrayList;

public class Track {

  private static long increment = 0;
  private long trackId;
  private AbsoluteDate msgTime;
  private int sensorId;
  private int objectId;
  private ArrayList<Position> positions;
  private ArrayList<Double> rcsArr;
  private ArrayList<Long> orbitsIds;

  private static TimeScale utc = TimeScalesFactory.getUTC();

  /** Creates a Track with the given parameters. */
  public Track(
      AbsoluteDate msgTime,
      int sensorId,
      int objectId,
      ArrayList<Position> positions,
      ArrayList<Double> rcsArr) {

    this.msgTime = msgTime;
    this.sensorId = sensorId;
    this.objectId = objectId;
    this.positions = positions;
    this.rcsArr = rcsArr;
    this.trackId = increment++;
    this.orbitsIds = new ArrayList<>();
  }

  private Track() {}

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

  /** Parse a Tracklet from a CSV representation. */
  public static Track fromString(String line) {

    String[] tokens = line.split(",");
    if (tokens.length % 5 != 3) {
      throw new RuntimeException("Invalid record: " + line);
    }

    Track track = new Track();

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
      track.trackId = increment++;
      track.orbitsIds = new ArrayList<>();

    } catch (NumberFormatException nfe) {
      throw new RuntimeException("Invalid record: " + line, nfe);
    }

    return track;
  }

  public void addOrbit(long orbitId) {
    this.orbitsIds.add(orbitId);
  }

  public void removeOrbit(long orbitId) {
    this.orbitsIds.remove(orbitId);
  }

  public AbsoluteDate getMsgTime() {
    return msgTime;
  }

  public int getSensorId() {
    return sensorId;
  }

  public ArrayList<Position> getPositions() {
    return positions;
  }

  public Long getId() {
    return trackId;
  }

  public ArrayList<Long> getOrbitIds() {
    return orbitsIds;
  }
}
