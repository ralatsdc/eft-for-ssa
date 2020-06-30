package io.springbok.statefun.examples.demonstration;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.DiagonalMatrix;
import org.hipparchus.ode.nonstiff.EulerIntegrator;
import org.hipparchus.random.CorrelatedRandomVectorGenerator;
import org.hipparchus.random.GaussianRandomGenerator;
import org.hipparchus.random.ISAACRandom;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;

public class KafkaTrackProducer {

  static String inputPath = "tles/globalstar_tles_05_18_2020.txt";

  public static void main(String[] args) throws IOException {

    // Configure Kafka
    Properties props = new Properties();
    props.put("bootstrap.servers", "localhost:9092");
    props.put("acks", "all");
    props.put("retries", 0);
    props.put("batch.size", 16384);
    props.put("linger.ms", 1);
    props.put("buffer.memory", 33554432);
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

    // Configure Orekit
    final File orekitData = new File("./orekit-data");
    final DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
    manager.addProvider(new DirectoryCrawler(orekitData));

    // Add tles to list
    final File tleData = new File(inputPath);
    ArrayList<TLE> tles = convertTLES(tleData);

    // Gravitation coefficient
    final double mu = Constants.IERS2010_EARTH_MU;

    // Inertial frame
    final Frame inertialFrame = FramesFactory.getGCRF();

    // Overall duration in seconds for extrapolation - 1 week
    final double duration = 60 * 60 * 24 * 7;

    // Step duration in seconds - 12 hours
    final double largeStep = 60 * 60 * 12;

    // Step duration in seconds
    final double smallStep = 60;

    // Set up propagator
    final EulerIntegrator euler = new EulerIntegrator(largeStep);
    final NumericalPropagator nPropagator = new NumericalPropagator(euler);

    // Random number generator set up
    final int small = 0;
    final ISAACRandom randomNumGenerator = new ISAACRandom();
    final GaussianRandomGenerator gaussianGenerator =
        new GaussianRandomGenerator(randomNumGenerator);

    // Set propagator index of satellite
    final ObservableSatellite satelliteIndex = new ObservableSatellite(0);

    // Set up for PV builder
    final double[] PVMatrix = new double[] {1, 1};
    final DiagonalMatrix PVCovarianceMatrix = new DiagonalMatrix(PVMatrix);
    final CorrelatedRandomVectorGenerator PVNoiseGenerator =
        new CorrelatedRandomVectorGenerator(PVCovarianceMatrix, small, gaussianGenerator);
    final double sigmaP = 1.;
    final double sigmaV = 1.;
    final double baseWeight = 1.;
    final PVBuilder pvBuilder = new PVBuilder(null, sigmaP, sigmaV, baseWeight, satelliteIndex);

    ArrayList<String> messageContainer = new ArrayList<String>();

    // Start propagating each TLE
    tles.forEach(
        (tle) -> {

          // Keplerian Initial orbit parameters
          final double a = Math.cbrt(mu / (Math.pow(tle.getMeanMotion(), 2)));
          final double e = tle.getE(); // eccentricity
          final double i = tle.getI(); // inclination
          final double omega = tle.getPerigeeArgument(); // perigee argument
          final double raan = tle.getRaan(); // right ascension of ascending node
          final double lM = tle.getMeanAnomaly(); // mean anomaly

          // Initial date in UTC time scale
          final AbsoluteDate initialDate = tle.getDate();

          // Orbit construction as Keplerian
          final Orbit initialOrbit =
              new KeplerianOrbit(
                  a, e, i, omega, raan, lM, PositionAngle.MEAN, inertialFrame, initialDate, mu);

          // Set initial state
          final SpacecraftState initialState = new SpacecraftState(initialOrbit);
          nPropagator.setInitialState(initialState);

          // Stop date
          final AbsoluteDate finalDate = initialDate.shiftedBy(duration);

          // 12 hour steps, plus or minus ~ 5 minutes
          double randomizedStep = largeStep + (gaussianGenerator.nextNormalizedDouble() * 150);

          // Extrapolation loop - 12 hour increments
          for (AbsoluteDate extrapDate = initialDate;
              extrapDate.compareTo(finalDate) <= 0;
              extrapDate = extrapDate.shiftedBy(randomizedStep)) {

            String message = createMessage(extrapDate, smallStep, nPropagator, pvBuilder, tle);
            messageContainer.add(message);
          }
        });

    Collections.sort(messageContainer);

    Producer<String, String> producer = new KafkaProducer<>(props);
    // TODO: load a catalog and compute tracks
    for (int i = 0; i < messageContainer.size(); i++) {
      producer.send(
          new ProducerRecord<String, String>(
              "tracks", Integer.toString(i), messageContainer.get(i)));
    }

    producer.close();
  }

  static String createMessage(
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

    int positionReadingNum = (int) (Math.random() * 10) + 1;

    // 3 readings separated by 1 minute
    for (int i = 0; i < positionReadingNum; i++) {
      AbsoluteDate currentDate = extrapDate.shiftedBy(smallStep * i);
      final SpacecraftState currentState = nPropagator.propagate(currentDate);
      // Add Az/El measurement to container
      SpacecraftState[] states = new SpacecraftState[] {currentState};
      PV pv = pvBuilder.build(states);
      Vector3D position = pv.getPosition();

      double rcs = Math.random() * 10;

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

  static ArrayList<TLE> convertTLES(File tleData) throws IOException {

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
}