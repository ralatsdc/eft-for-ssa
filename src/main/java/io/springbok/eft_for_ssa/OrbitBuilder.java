package io.springbok.eft_for_ssa;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.optim.nonlinear.vector.leastsquares.GaussNewtonOptimizer;
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

import java.util.ArrayList;

public class OrbitBuilder {

	// Gravitation coefficient
	final static double mu = Constants.IERS2010_EARTH_MU;
	// Inertial frame
	final static Frame inertialFrame = FramesFactory.getGCRF();

	// Modeling a static class
	private OrbitBuilder(){}

	// Create an orbit with a single tracklet of 2 or more positions
	public static KeyedOrbit createOrbit(Tracklet tracklet) {

		ArrayList<Position> positions = tracklet.getPositions();

		Orbit orbit;
		Orbit orbitEstimation = iod(positions);

		if (positions.size() > 2) {
			orbit = leastSquaresRefine(orbitEstimation, positions);
		} else {
			orbit = orbitEstimation;
		}
		KeyedOrbit keyedOrbit = new KeyedOrbit(orbit, tracklet);
		return keyedOrbit;
	}


	// Create an orbit with an ArrayList<Tracklet> of 2 or more positions
	public static KeyedOrbit createOrbit(ArrayList<Tracklet> tracklets) {

		ArrayList<Position> positions = new ArrayList<>();
		tracklets.forEach(tracklet -> {positions.addAll(tracklet.getPositions());});

		Orbit orbit;
		Orbit orbitEstimation = iod(positions);

		if (positions.size() > 2) {
			orbit = leastSquaresRefine(orbitEstimation, positions);
		} else {
			orbit = orbitEstimation;
		}
		KeyedOrbit keyedOrbit = new KeyedOrbit(orbit, tracklets);
		return keyedOrbit;
	}

	public static KeyedOrbit refineOrbit(KeyedOrbit orbit, ArrayList<Tracklet> tracklets){

		ArrayList<Position> positions = new ArrayList<>();
		tracklets.forEach(tracklet -> {positions.addAll(tracklet.getPositions());});

		Orbit refinedOrbit = leastSquaresRefine(orbit.getOrbit(), positions);
		KeyedOrbit keyedOrbit = new KeyedOrbit(refinedOrbit, tracklets);

		return keyedOrbit;
	}

	private static Orbit iod(ArrayList<Position> positions){
		// Orbit Determination
		final IodLambert lambert = new IodLambert(mu);
		// TODO: Posigrade and number of revolutions are set as guesses for now, but will need to be calculated later
		final boolean posigrade = true;
		final int nRev = 0;
		final Vector3D initialPosition = positions.get(0).getPosition();
		final AbsoluteDate initialDate = positions.get(0).getDate();
		final Vector3D finalPosition = positions.get(positions.size() - 1).getPosition();
		final AbsoluteDate finalDate = positions.get(positions.size() - 1).getDate();
		final Orbit orbitEstimation = lambert.estimate(inertialFrame, posigrade, nRev, initialPosition, initialDate, finalPosition, finalDate);
		return orbitEstimation;
	}

	private static Orbit leastSquaresRefine(Orbit orbitEstimation, ArrayList<Position> positions) {

		// Least squares estimator setup
		final GaussNewtonOptimizer GNOptimizer = new GaussNewtonOptimizer();
		final EulerIntegratorBuilder eulerBuilder = new EulerIntegratorBuilder(60);
		final double positionScale = 1.;
		final NumericalPropagatorBuilder propBuilder = new NumericalPropagatorBuilder(orbitEstimation, eulerBuilder, PositionAngle.MEAN, positionScale);
		final BatchLSEstimator leastSquares = new BatchLSEstimator(GNOptimizer, propBuilder);
		leastSquares.setMaxIterations(1000);
		leastSquares.setMaxEvaluations(1000);
		leastSquares.setParametersConvergenceThreshold(.001);

		// Add measurements
		positions.forEach(measurement->leastSquares.addMeasurement(measurement));

		// Run least squares fit
		AbstractIntegratedPropagator[] lsPropagators = leastSquares.estimate();
		Orbit orbit = lsPropagators[0].getInitialState().getOrbit();
		return orbit;
	}
}
