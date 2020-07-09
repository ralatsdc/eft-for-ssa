package io.springbok.statefun.examples.demonstration;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.optim.nonlinear.vector.leastsquares.GaussNewtonOptimizer;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.estimation.iod.IodLambert;
import org.orekit.estimation.leastsquares.BatchLSEstimator;
import org.orekit.estimation.measurements.Position;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.conversion.EulerIntegratorBuilder;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.propagation.integration.AbstractIntegratedPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

import java.io.File;
import java.util.ArrayList;

public class OrbitFactory {

  // Gravitation coefficient
  static final double mu = Constants.IERS2010_EARTH_MU;
  // Inertial frame
  static final Frame inertialFrame = FramesFactory.getGCRF();

  // Configure Orekit
  static final File orekitData = new File("../../orekit-data");
  static DataProvidersManager manager = null;

  public static void init() {
    // Configure Orekit
    if (manager == null) {
      final DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
      manager.addProvider(new DirectoryCrawler(orekitData));
    }
  }

  // Modeling a static class
  private OrbitFactory() {}

  // Create an orbit with a single track of 2 or more positions
  public static KeyedOrbit createOrbit(Track track, String orbitId) {

    init();

    ArrayList<Position> positions = track.getPositions();

    Orbit orbit;
    Orbit orbitEstimation = iod(positions);

    if (positions.size() > 2) {
      orbit = leastSquaresRefine(orbitEstimation, positions);
    } else {
      orbit = orbitEstimation;
    }
    KeyedOrbit keyedOrbit = new KeyedOrbit(orbit, orbitId, track);
    return keyedOrbit;
  }

  // Create an orbit with an ArrayList<Track> of 2 or more positions
  public static KeyedOrbit createOrbit(ArrayList<Track> tracks, String orbitId) {

    init();

    ArrayList<Position> positions = new ArrayList<>();
    tracks.forEach(
        track -> {
          positions.addAll(track.getPositions());
        });

    Orbit orbit;
    Orbit orbitEstimation = iod(positions);

    if (positions.size() > 2) {
      orbit = leastSquaresRefine(orbitEstimation, positions);
    } else {
      orbit = orbitEstimation;
    }
    KeyedOrbit keyedOrbit = new KeyedOrbit(orbit, orbitId, tracks);
    return keyedOrbit;
  }

    public static KeyedOrbit refineOrbit(KeyedOrbit keyedOrbit1, ArrayList<Track> keyedOrbit2Tracks, String id) {

      init();

      ArrayList<Position> positions = new ArrayList<>();
      keyedOrbit2Tracks.forEach(
          track -> {
            positions.addAll(track.getPositions());
          });

      Orbit refinedOrbit = leastSquaresRefine(keyedOrbit1.orbit, positions);
      KeyedOrbit refinedKeyedOrbit = new KeyedOrbit(refinedOrbit, id, keyedOrbit2Tracks);

      return refinedKeyedOrbit;
    }

  private static Orbit iod(ArrayList<Position> positions) {

    init();

    // Orbit Determination
    final IodLambert lambert = new IodLambert(mu);
    // TODO: Posigrade and number of revolutions are set as guesses for now, but will need to be calculated later
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

  private static Orbit leastSquaresRefine(Orbit orbitEstimation, ArrayList<Position> positions) {

    init();

    // Least squares estimator setup
    final GaussNewtonOptimizer GNOptimizer = new GaussNewtonOptimizer();
    final EulerIntegratorBuilder eulerBuilder = new EulerIntegratorBuilder(60);
    final double positionScale = 1.;
    final NumericalPropagatorBuilder propBuilder =
        new NumericalPropagatorBuilder(
            orbitEstimation, eulerBuilder, PositionAngle.MEAN, positionScale);
    final BatchLSEstimator leastSquares = new BatchLSEstimator(GNOptimizer, propBuilder);
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
}
