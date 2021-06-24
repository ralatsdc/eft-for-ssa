package io.springbok.statefun.examples.utilities;

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

  static String tlePath;
  private String orekitPath;

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

  public TrackGenerator() throws Exception {

    // TODO: verify the input is a TLE
    orekitPath = System.getProperty("OREKIT_PATH");
    messages = new ArrayList<>();
    mappedMessages = new HashMap<>();
  }

  public TrackGenerator(String tleLocation) throws Exception {

    // TODO: verify the input is a TLE
    tlePath = tleLocation;
    orekitPath = System.getProperty("OREKIT_PATH");
    messages = new ArrayList<>();
    mappedMessages = new HashMap<>();
  }

  public TrackGenerator(String tleLocation, String orekitPath) throws Exception {

    // TODO: verify the input is a TLE
    tlePath = tleLocation;
    this.orekitPath = orekitPath;
    messages = new ArrayList<>();
    mappedMessages = new HashMap<>();
  }

  public void init() throws IOException {

    // Configure Orekit
    final File orekitData = new File(orekitPath);
    final DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
    manager.addProvider(new DirectoryCrawler(orekitData));

    if (tlePath != null) {
      // Add tles to list
      final File tleData = new File(tlePath);
      tles = TLEReader.readTLEs(tleData);
    }

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

  // Time in weeks
  public ArrayList<String> finitePropagation(double weeks) {

    // Overall duration in seconds for extrapolation - 1 week
    final double duration = 60 * 60 * 24 * 7 * weeks;

    // Default universe value given no parameters
    final String universe = "0";

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
                createMessage(universe, extrapDate, smallStep, numericalPropagator, pvBuilder, tle);
            messages.add(message);

            ArrayList<String> sortedMessages =
                mappedMessages.getOrDefault(tle.getSatelliteNumber(), new ArrayList<>());
            sortedMessages.add(message);
            mappedMessages.put(tle.getSatelliteNumber(), sortedMessages);
          }
        });

    // Sort messages by track start time
    Collections.sort(
        messages,
        new Comparator<String>() {
          @Override
          public int compare(String msgOne, String msgTwo) {
            String[] fldsOne = msgOne.split(",");
            String[] fldsTwo = msgTwo.split(",");
            // Sort lexicographically since times are in YYYY-MM-DDTHH:MM:SS.SSS format
            return fldsOne[4].compareTo(fldsTwo[4]);
          }
        });
    return messages;
  }

  // Time in weeks
  public ArrayList<String> finitePropagation(double weeks, String universe) {

    // Overall duration in seconds for extrapolation - 1 week
    final double duration = 60 * 60 * 24 * 7 * weeks;

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
                createMessage(universe, extrapDate, smallStep, numericalPropagator, pvBuilder, tle);
            messages.add(message);

            ArrayList<String> sortedMessages =
                mappedMessages.getOrDefault(tle.getSatelliteNumber(), new ArrayList<>());
            sortedMessages.add(message);
            mappedMessages.put(tle.getSatelliteNumber(), sortedMessages);
          }
        });

    // Sort messages by track start time
    Collections.sort(
        messages,
        new Comparator<String>() {
          @Override
          public int compare(String msgOne, String msgTwo) {
            String[] fldsOne = msgOne.split(",");
            String[] fldsTwo = msgTwo.split(",");
            // Sort lexicographically since times are in YYYY-MM-DDTHH:MM:SS.SSS format
            return fldsOne[4].compareTo(fldsTwo[4]);
          }
        });
    return messages;
  }

  public String propagate(TLE tle, Double timePassed) {

    // Get orbit from TLE
    Orbit initialOrbit = createOrbit(tle);

    // Set initial state
    final SpacecraftState initialState = new SpacecraftState(initialOrbit);
    numericalPropagator.setInitialState(initialState);

    // Get working date
    AbsoluteDate extrapDate = tle.getDate().shiftedBy(timePassed);

    // Give default universe value
    String universe = "0";

    String message =
        createMessage(universe, extrapDate, smallStep, numericalPropagator, pvBuilder, tle);

    return message;
  }

  public String propagate(TLE tle, Double timePassed, String universe) {

    // Get orbit from TLE
    Orbit initialOrbit = createOrbit(tle);

    // Set initial state
    final SpacecraftState initialState = new SpacecraftState(initialOrbit);
    numericalPropagator.setInitialState(initialState);

    // Get working date
    AbsoluteDate extrapDate = tle.getDate().shiftedBy(timePassed);

    String message =
        createMessage(universe, extrapDate, smallStep, numericalPropagator, pvBuilder, tle);

    return message;
  }

  public String produceTrackAtTime(TLE tle, AbsoluteDate endDate, String sensorId) {

    // Get orbit from TLE
    Orbit initialOrbit = createOrbit(tle);

    // Set initial state
    final SpacecraftState initialState = new SpacecraftState(initialOrbit);
    numericalPropagator.setInitialState(initialState);

    String universe = "0";

    String message =
        createMessage(universe, endDate, smallStep, numericalPropagator, pvBuilder, tle, sensorId);

    return message;
  }

  public String produceTrackAtTime(
      TLE tle, AbsoluteDate endDate, String sensorId, String universe) {

    // Get orbit from TLE
    Orbit initialOrbit = createOrbit(tle);

    // Set initial state
    final SpacecraftState initialState = new SpacecraftState(initialOrbit);
    numericalPropagator.setInitialState(initialState);

    String message =
        createMessage(universe, endDate, smallStep, numericalPropagator, pvBuilder, tle, sensorId);

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
      String universe,
      AbsoluteDate extrapDate,
      double smallStep,
      NumericalPropagator nPropagator,
      PVBuilder pvBuilder,
      TLE tle) {

    // Message format:
    // msgTime, sensorId, objectId, universes, obsTime1, x1, y1, z1, rcs1, obsTime2, x2, y2, z2,
    // rcs2,
    // obsTime3, x3, y3, z3, rcs3
    String message;

    UUID uuid = UUID.randomUUID();

    // Message set to always come in ten minutes after first observation
    AbsoluteDate msgTime = extrapDate.shiftedBy(600);
    // Set sensor ID
    int sensorId = 0;
    // Set object ID
    int objectId = tle.getSatelliteNumber();

    message =
        uuid.toString()
            + ","
            + msgTime.toString()
            + ","
            + sensorId
            + ","
            + objectId
            + ","
            + universe;

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

  // Overloaded method adds sensorId specification
  public static String createMessage(
      String universe,
      AbsoluteDate extrapDate,
      double smallStep,
      NumericalPropagator nPropagator,
      PVBuilder pvBuilder,
      TLE tle,
      String sensorId) {

    // Message format:
    // msgTime, sensorId, objectId, obsTime1, x1, y1, z1, rcs1, obsTime2, x2, y2, z2, rcs2,
    // obsTime3, x3, y3, z3, rcs3
    String message;

    UUID uuid = UUID.randomUUID();

    // Message set to always come in ten minutes after first observation
    AbsoluteDate msgTime = extrapDate.shiftedBy(600);
    // Set object ID
    int objectId = tle.getSatelliteNumber();

    message =
        uuid.toString()
            + ","
            + msgTime.toString()
            + ","
            + sensorId
            + ","
            + objectId
            + ","
            + universe;

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
