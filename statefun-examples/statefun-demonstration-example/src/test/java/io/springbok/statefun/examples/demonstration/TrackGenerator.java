package io.springbok.statefun.examples.demonstration;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.GillIntegrator;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.PV;
import org.orekit.estimation.measurements.generation.PVBuilder;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

import java.io.*;
import java.time.LocalDate;
import java.util.*;

// This class will always produce exactly the same track messages given a specific TLE
// Examples of randomized tracks are shown below but not utilized
public class TrackGenerator {

  static String tlePath = "./tle-data/globalstar_tles_05_18_2020.txt";
  private String orekitPath = "../../orekit-data";

  ArrayList<String> messages;
  Map<Integer, ArrayList<String>> mappedMessages;

  // Constants
  final double mu = Constants.IERS2010_EARTH_MU;
  final Frame inertialFrame = FramesFactory.getGCRF();

  // Steps - Duration in seconds
  public final double largeStep = 60 * 60 * 12;
  final double smallStep = 600;

  // Propagator
  NumericalPropagator numericalPropagator;
  PVBuilder pvBuilder;
  public ArrayList<TLE> tles;

  // infinite generator
  Iterator<TLE> iterator;

  public TrackGenerator(String tlePath) throws Exception {

    // TODO: verify the input is a TLE
    this.tlePath = tlePath;
    this.orekitPath = orekitPath;
    messages = new ArrayList<>();
    mappedMessages = new HashMap<>();
  }

  public TrackGenerator(String tlePath, String orekitPath) throws Exception {

    // TODO: verify the input is a TLE
    this.tlePath = tlePath;
    this.orekitPath = orekitPath;
    messages = new ArrayList<>();
    mappedMessages = new HashMap<>();
  }

  public void init() throws IOException {

    // Configure Orekit
    final File orekitData = new File(orekitPath);
    final DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
    manager.addProvider(new DirectoryCrawler(orekitData));

    // Add tles to list
    final File tleData = new File(tlePath);
    tles = convertTLES(tleData);

    // Set up propagator
    final GillIntegrator gillIntegrator = new GillIntegrator(largeStep);
    numericalPropagator = new NumericalPropagator(gillIntegrator);

    // Set propagator index of satellite
    final ObservableSatellite satelliteIndex = new ObservableSatellite(0);

    // Set up for PV builder
    final double sigmaP = 1.;
    final double sigmaV = 1.;
    final double baseWeight = 1.;
    // Null here signals no random variance.
    pvBuilder = new PVBuilder(null, sigmaP, sigmaV, baseWeight, satelliteIndex);
  }

  public ArrayList<String> finitePropagation() {

    // Overall duration in seconds for extrapolation - 1 week
    final double duration = 60 * 60 * 24 * 7;

    tles.forEach(
        (tle) -> {

          // Initial date in UTC time scale
          final AbsoluteDate initialDate = tle.getDate();

          // Get orbit from TLE
          Orbit initialOrbit = createOrbit(tle);

          // Set initial state
          final SpacecraftState initialState = new SpacecraftState(initialOrbit);
          numericalPropagator.setInitialState(initialState);

          // Stop date
          final AbsoluteDate finalDate = initialDate.shiftedBy(duration);

          // Extrapolation loop - 12 hour increments
          for (AbsoluteDate extrapDate = initialDate;
              extrapDate.compareTo(finalDate) <= 0;
              extrapDate = extrapDate.shiftedBy(largeStep)) {

            String message =
                createMessage(extrapDate, smallStep, numericalPropagator, pvBuilder, tle);
            messages.add(message);

            ArrayList<String> sortedMessages =
                mappedMessages.getOrDefault(tle.getSatelliteNumber(), new ArrayList<>());
            sortedMessages.add(message);
            mappedMessages.put(tle.getSatelliteNumber(), sortedMessages);
          }
        });

    Collections.sort(messages);
    return messages;
  }

  public String propagate(TLE tle, Double timePassedMS) {

    // Get orbit from TLE
    Orbit initialOrbit = createOrbit(tle);

    // Set initial state
    final SpacecraftState initialState = new SpacecraftState(initialOrbit);
    numericalPropagator.setInitialState(initialState);

    // Get working date
    AbsoluteDate extrapDate = tle.getDate().shiftedBy(timePassedMS);

    String message = createMessage(extrapDate, smallStep, numericalPropagator, pvBuilder, tle);

    return message;
  }

  public Orbit createOrbit(TLE tle) {

    // Keplerian Initial orbit parameters
    final double a = Math.cbrt(mu / (Math.pow(tle.getMeanMotion(), 2)));
    final double e = tle.getE(); // eccentricity
    final double i = tle.getI(); // inclination
    final double omega = tle.getPerigeeArgument(); // perigee argument
    final double raan = tle.getRaan(); // right ascension of ascending node
    final double lM = tle.getMeanAnomaly(); // mean anomaly

    // Orbit construction as Keplerian
    final Orbit initialOrbit =
        new KeplerianOrbit(
            a, e, i, omega, raan, lM, PositionAngle.MEAN, inertialFrame, tle.getDate(), mu);

    return initialOrbit;
  }

  static void createFile(ArrayList<String> messageContainer) {
    LocalDate date = LocalDate.now();
    String name = "output/test-track-messages.txt";
    try {
      FileWriter writer = new FileWriter(name);
      System.out.println(name + " created");
      BufferedWriter bufferedwriter = new BufferedWriter(writer);

      messageContainer.forEach(
          message -> {
            try {
              bufferedwriter.write(message);
              bufferedwriter.newLine();
            } catch (IOException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }
          });

      bufferedwriter.close();

    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  String createMessage(
      AbsoluteDate extrapDate,
      double smallStep,
      NumericalPropagator nPropagator,
      PVBuilder pvBuilder,
      TLE tle) {

    // Message format:
    // msgTime, sensorId, objectId, obsTime1, x1, y1, z1, rcs1, obsTime2, x2, y2, z2, rcs2,
    // obsTime3, x3, y3, z3, rcs3
    String message;

    // Message set to always come in ten minutes after first observation
    AbsoluteDate msgTime = extrapDate.shiftedBy(600);
    // Set sensor ID
    int sensorId = 0;
    // Set object ID
    int objectId = tle.getSatelliteNumber();

    message = msgTime.toString() + "," + sensorId + "," + objectId;

    // TODO: future random number of readings option
    //    int positionReadingNum = (int) (Math.random() * 10);
    int positionReadingNum = 3;

    // 3 readings separated by smallStep
    for (int i = 0; i <= positionReadingNum; i++) {
      AbsoluteDate currentDate = extrapDate.shiftedBy(smallStep * i);
      final SpacecraftState currentState = nPropagator.propagate(currentDate);
      // Add Az/El measurement to container
      SpacecraftState[] states = new SpacecraftState[] {currentState};
      PV pv = pvBuilder.build(states);
      Vector3D position = pv.getPosition();

      // TODO: generate RCS in a more specified way.
      double rcs = 5;

      String obs =
          currentDate.toString()
              + ","
              + position.getX()
              + ","
              + position.getY()
              + ","
              + position.getZ()
              + ","
              + rcs;

      message = message + "," + obs;
    }
    return message;
  }

  public static ArrayList<TLE> convertTLES(File tleData) throws IOException {

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
    return tles;
  }

  public ArrayList<String> getMessages() {
    return messages;
  }

  // TODO: add error handling for x being larger than file length
  public ArrayList<String> getXMessages(int x) {
    ArrayList<String> slicedMessages = new ArrayList<String>();
    for (int i = 0; i < x; i++) {
      slicedMessages.add(messages.get(i));
    }
    return slicedMessages;
  }

  public ArrayList<String> getSingleObjectMessages() {
    Set<Integer> keySet = mappedMessages.keySet();
    ArrayList<String> singleObjectMessages = mappedMessages.get(keySet.iterator().next());
    return singleObjectMessages;
  }

  // TODO: add error handling for x being larger than file length
  public ArrayList<String> getXSingleObjectMessages(int x) {
    Set<Integer> keySet = mappedMessages.keySet();
    ArrayList<String> singleObjectMessages = mappedMessages.get(keySet.iterator().next());

    ArrayList<String> slicedSingleObjectMessages = new ArrayList<String>();
    for (int i = 0; i < x; i++) {
      slicedSingleObjectMessages.add(singleObjectMessages.get(i));
    }
    return slicedSingleObjectMessages;
  }

  public ArrayList<String> getMessagesById(int id) {
    ArrayList<String> messagesById = mappedMessages.get(id);
    return messagesById;
  }
}
