package io.springbok.statefun.examples.demonstration;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.optim.nonlinear.vector.leastsquares.LevenbergMarquardtOptimizer;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.estimation.iod.IodLambert;
import org.orekit.estimation.leastsquares.BatchLSEstimator;
import org.orekit.estimation.measurements.Position;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.conversion.DormandPrince54IntegratorBuilder;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.propagation.integration.AbstractIntegratedPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;

import java.io.File;
import java.util.ArrayList;

/*
 OrbitFactory is used to create or refine KeyedOrbits using Orekit
 OrbitFactory is intended to be used statically
*/
public class OrbitFactory {

  // Gravitation coefficient
  public static final double mu = Constants.IERS2010_EARTH_MU;

  // Inertial frame
  static final Frame inertialFrame = FramesFactory.getGCRF();

  // Configure Orekit
  static String inputPath = "../../orekit-data";
  static final File orekitData = new File(inputPath);
  static DataProvidersManager manager = null;

  public static void init() {
    // Ensure Orekit is configured
    if (manager == null) {
      manager = DataContext.getDefault().getDataProvidersManager();
      manager.addProvider(new DirectoryCrawler(orekitData));
    }
  }

  // Create an orbit with a single Track of 2 or more positions
  public static KeyedOrbit createOrbit(Track track, String orbitId) {

    // Ensure Orekit is configured
    init();

    ArrayList<Position> positions = track.getPositions();

    Orbit orbit;
    Orbit orbitEstimation = iod(positions);

    // If there are only two positions, only the initial estimation is possible, else refine the
    // orbits
    if (positions.size() > 2) {
      orbit = leastSquaresRefine(orbitEstimation, positions);
    } else {
      orbit = orbitEstimation;
    }
    KeyedOrbit keyedOrbit = new KeyedOrbit(orbit, orbitId, track);
    return keyedOrbit;
  }

  // Refine an orbit with an ArrayList of Tracks of 2 or more positions
  public static KeyedOrbit refineOrbit(
      Orbit orbit1,
      ArrayList<String> keyedOrbit1TrackIds,
      ArrayList<Track> keyedOrbit2Tracks,
      String newOrbitId) {

    // Ensure Orekit is configured
    init();

    // New orbit instance must be created to set the inertialFrame correctly in the current
    // StatefulFunction context
    // This is a copy of the given orbit
    Orbit orbit =
        new KeplerianOrbit(orbit1.getPVCoordinates(), inertialFrame, orbit1.getDate(), mu);

    ArrayList<Position> positions = new ArrayList<>();
    keyedOrbit2Tracks.forEach(
        track -> {
          positions.addAll(track.getPositions());
        });

    // Refine the orbit with positions taken from the Track(s)
    Orbit refinedOrbit = leastSquaresRefine(orbit, positions);
    KeyedOrbit refinedKeyedOrbit =
        new KeyedOrbit(refinedOrbit, newOrbitId, keyedOrbit2Tracks, keyedOrbit1TrackIds);

    return refinedKeyedOrbit;
  }

  private static Orbit iod(ArrayList<Position> positions) {

    // Ensure Orekit is configured
    init();

    // Orbit Determination from first and last position
    final IodLambert lambert = new IodLambert(mu);
    // TODO: Posigrade and number of revolutions are set as guesses for now, but will need to be
    // calculated later
    final boolean posigrade = true;
    final int nRev = 0;
    final Vector3D initialPosition = positions.get(0).getPosition();
    final AbsoluteDate initialDate = positions.get(0).getDate();
    final Vector3D finalPosition = positions.get(positions.size() - 1).getPosition();
    final AbsoluteDate finalDate = positions.get(positions.size() - 1).getDate();
    final Orbit orbitEstimation =
        lambert.estimate(
            inertialFrame, posigrade, nRev, initialPosition, initialDate, finalPosition, finalDate);
    return orbitEstimation;
  }

  // Refine the orbit with additional positions taken from the Track(s)
  private static Orbit leastSquaresRefine(Orbit orbitEstimation, ArrayList<Position> positions) {

    // Ensure Orekit is configured
    init();

    // Least squares refine setup
    final LevenbergMarquardtOptimizer levenbergMarquardtOptimizer =
        new LevenbergMarquardtOptimizer();
    final DormandPrince54IntegratorBuilder dormandPrince54IntegratorBuilder =
        new DormandPrince54IntegratorBuilder(360, 2 * 24 * 3600, 10);
    final double positionScale = 1.;
    final NumericalPropagatorBuilder propBuilder =
        new NumericalPropagatorBuilder(
            orbitEstimation, dormandPrince54IntegratorBuilder, PositionAngle.MEAN, positionScale);
    final BatchLSEstimator leastSquares =
        new BatchLSEstimator(levenbergMarquardtOptimizer, propBuilder);
    leastSquares.setMaxIterations(1000);
    leastSquares.setMaxEvaluations(1000);
    leastSquares.setParametersConvergenceThreshold(.001);

    // Add measurements
    positions.forEach(leastSquares::addMeasurement);

    // Run least squares fit
    AbstractIntegratedPropagator[] lsPropagators = leastSquares.estimate();
    Orbit orbit = lsPropagators[0].getInitialState().getOrbit();
    return orbit;
  }

  public static Orbit fromTokens(String[] tokens) {

    init();

    double a = Double.parseDouble(tokens[0]);
    double e = Double.parseDouble(tokens[1]);
    double i = Double.parseDouble(tokens[2]);
    double pa = Double.parseDouble(tokens[3]);
    double raan = Double.parseDouble(tokens[4]);
    double v = Double.parseDouble(tokens[5]);
    AbsoluteDate date = new AbsoluteDate(tokens[6], TimeScalesFactory.getUTC());

    Orbit orbit =
        new KeplerianOrbit(a, e, i, pa, raan, v, PositionAngle.MEAN, inertialFrame, date, mu);
    return orbit;
  }
}
