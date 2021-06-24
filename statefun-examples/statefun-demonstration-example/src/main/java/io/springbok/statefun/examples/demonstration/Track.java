package io.springbok.statefun.examples.demonstration;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.Position;
import org.orekit.orbits.Orbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;

import java.util.ArrayList;
import java.util.UUID;

// Track stores information from a TrackIn string in a more usable interface
public class Track {

  private AbsoluteDate msgTime;
  public int sensorId;
  public int objectId;
  private ArrayList<Position> positions;
  private ArrayList<Double> rcsArr;

  public UUID messageUUID;
  public String trackId;
  private ArrayList<String> orbitIds;
  public String universe;

  private static TimeScale utc = TimeScalesFactory.getUTC();

  // TODO: add sensor id, object id
  public Track(Orbit orbit, int satelliteId) {
    msgTime = orbit.getDate();
    sensorId = 0;
    objectId = satelliteId;

    // TODO: put sigma and baseweight in properties file

    final double sigmaP = 1.;
    final double baseWeight = 1.;
    // This value is never used
    final ObservableSatellite satelliteIndex = new ObservableSatellite(satelliteId);

    Position position =
        new Position(
            orbit.getDate(),
            orbit.getPVCoordinates().getPosition(),
            sigmaP,
            baseWeight,
            satelliteIndex);

    positions.add(position);

    // TODO: generate RCS in a more specified way.
    double rcs = 5;
    rcsArr.add(rcs);

    messageUUID = UUID.randomUUID();
    trackId = null;
    orbitIds = new ArrayList<>();
  }

  public Track() {}

  @Override
  public String toString() {

    String msg = messageUUID + "," + msgTime + "," + sensorId + "," + objectId + "," + universe;

    for (int i = 0; i < positions.size(); i++) {
      Position position = positions.get(i);
      String positionString =
          position.getDate()
              + ","
              + position.getPosition().getX()
              + ","
              + position.getPosition().getY()
              + ","
              + position.getPosition().getZ()
              + ","
              + rcsArr.get(i);
      msg = msg + "," + positionString;
    }
    return msg;
  }

  // Parse a Track from a CSV representation.
  public static Track fromString(String line, String id) {

    // Ensure Orekit is configured
    OrbitFactory.init();

    String[] tokens = line.split(",");

    if (tokens.length % 5 != 0) {
      throw new RuntimeException("Invalid string to form Track: " + line);
    }

    Track track = new Track();

    // Create track from string
    track.messageUUID = UUID.fromString(tokens[0]);
    track.msgTime = new AbsoluteDate(tokens[1], utc);
    track.sensorId = Integer.parseInt(tokens[2]);
    track.objectId = Integer.parseInt(tokens[3]);
    track.universe = tokens[4];

    ArrayList<Position> positions = new ArrayList<Position>();
    ArrayList<Double> rcsArr = new ArrayList<Double>();

    ObservableSatellite satelliteIndex = new ObservableSatellite(0);

    for (int i = 5; i < tokens.length; i = i + 5) {

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
