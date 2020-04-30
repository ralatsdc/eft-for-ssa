package io.springbok.eft_for_ssa;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.DiagonalMatrix;
import org.hipparchus.ode.nonstiff.EulerIntegrator;
import org.hipparchus.optim.nonlinear.vector.leastsquares.GaussNewtonOptimizer;
import org.hipparchus.random.CorrelatedRandomVectorGenerator;
import org.hipparchus.random.GaussianRandomGenerator;
import org.hipparchus.random.ISAACRandom;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.NetworkCrawler;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.iod.IodLambert;
import org.orekit.estimation.leastsquares.BatchLSEstimator;
import org.orekit.estimation.measurements.AngularAzEl;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.Range;
import org.orekit.estimation.measurements.generation.AngularAzElBuilder;
import org.orekit.estimation.measurements.generation.RangeBuilder;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.frames.Transform;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.conversion.EulerIntegratorBuilder;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.propagation.integration.AbstractIntegratedPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.Constants;

/** Orekit tutorial for slave mode propagation.
 * <p>This tutorial shows a basic usage of the slave mode in which the user drives all propagation steps.<p>
 * @author Pascal Parraud
 */
public class TLENumericalPropagator {

    /** Private constructor for utility class. */
    private TLENumericalPropagator() {
        // empty
    }
            
	static Vector3D getPositionFromAzEl(AngularAzEl azel, Range range, Frame staF, Frame inertialFrame, AbsoluteDate date){
                	double az = azel.getObservedValue()[0];
                	double alpha = -az + (Math.PI / 2);
                	double delta = azel.getObservedValue()[1];
                	double ra = range.getObservedValue()[0];
                	
                	Vector3D eHat = new Vector3D(alpha, delta);
                	Vector3D pos = new Vector3D(ra, eHat);
                	
					Transform topographicToInertial = staF.getTransformTo(inertialFrame, date);
                	Vector3D inertialPos = topographicToInertial.transformPosition(pos);
                	return inertialPos;
	}

    /** Program entry point.
     * @param args program arguments (unused here)
     */
    public static void main(final String[] args) throws IOException {
        try {
        	
            // Configure Orekit
            final URL utcTaiData = new URL("https://hpiers.obspm.fr/eoppc/bul/bulc/UTC-TAI.history");
            final URL eopData = new URL("ftp://ftp.iers.org/products/eop/rapid/daily/finals.daily"); 
            final DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
            manager.addProvider(new NetworkCrawler(utcTaiData));
            manager.addProvider(new NetworkCrawler(eopData));

            // Read TLE file
            final File tleData = new File("04_07_2020.txt");            
            
            BufferedReader tleReader;
			try {
				tleReader = new BufferedReader(new FileReader(tleData));
				String line1 = tleReader.readLine();
				String line2 = tleReader.readLine();	
				System.out.println(line1);
				System.out.println(line2);	
				TLE tle = new TLE(line1, line2);
			 
            // Gravitation coefficient
            final double mu = Constants.IERS2010_EARTH_MU;

            // Initial orbit parameters
            final double a = Math.cbrt(mu / (Math.pow(tle.getMeanMotion(), 2)));
			final double e = tle.getE(); // eccentricity
            final double i = tle.getI(); // inclination
            final double omega = tle.getPerigeeArgument(); // perigee argument
            final double raan = tle.getRaan(); // right ascension of ascending node
            final double lM = tle.getMeanAnomaly(); // mean anomaly

            // Inertial frame
            final Frame inertialFrame = FramesFactory.getGCRF();

            // Initial date in UTC time scale
            final AbsoluteDate initialDate = tle.getDate();
             
            // Orbit construction as Keplerian
            final Orbit initialOrbit = new KeplerianOrbit(a, e, i, omega, raan, lM, PositionAngle.MEAN,
                                                         inertialFrame, initialDate, mu);
            
            // Overall duration in seconds for extrapolation
            final double duration = 600.;

            // Stop date
            final AbsoluteDate finalDate = initialDate.shiftedBy(duration);

            // Step duration in seconds
            final double stepT = 60.;

            // Establish topocentric frame
            double longitude = FastMath.toRadians(45.);
            double latitude  = FastMath.toRadians(25.);
            double altitude  = 0.;
            GeodeticPoint station = new GeodeticPoint(latitude, longitude, altitude);
            Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
            BodyShape earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
            										Constants.WGS84_EARTH_FLATTENING,
            										earthFrame);
            TopocentricFrame staF = new TopocentricFrame(earth, station, "station");

            // Initialize ground station
            final GroundStation groundStation = new GroundStation(staF);
            groundStation.getPrimeMeridianOffsetDriver().setReferenceDate(initialDate);
            groundStation.getPolarOffsetXDriver().setReferenceDate(initialDate);
            groundStation.getPolarOffsetYDriver().setReferenceDate(initialDate);

            // Set initial state
            final SpacecraftState initialState = new SpacecraftState(initialOrbit);
            SpacecraftState finalState = null;
            
            // Initialize orbit estimation variables
            Vector3D initialPosition = null;
            Vector3D finalPosition = null;

            // Set up propagator
            final EulerIntegrator euler = new EulerIntegrator(stepT);
            final KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit);

            // Set propagator index of satellite
            final ObservableSatellite satelliteIndex = new ObservableSatellite(0);
            
            // Random number generator set up
            final int small = 0;
            final ISAACRandom randomNumGenerator = new ISAACRandom();
            final GaussianRandomGenerator gaussianGenerator = new GaussianRandomGenerator(randomNumGenerator);

            // Set up Az/El builder
            // TODO: Understand why sigma doesn't change the value
            final double[] angularMatrix = new double[] {0.000001, 0.000001};
            final DiagonalMatrix angularCovarianceMatrix = new DiagonalMatrix(angularMatrix);
            final CorrelatedRandomVectorGenerator angularNoiseGenerator = new CorrelatedRandomVectorGenerator(angularCovarianceMatrix, small, gaussianGenerator);
            final double[] azElSigmas = new double[] {1, 1};
            final double[] azElBaseWeights = new double[] {1, 1};
            final AngularAzElBuilder azElBuilder = new AngularAzElBuilder(angularNoiseGenerator, groundStation, azElSigmas, azElBaseWeights, satelliteIndex);
            azElBuilder.init(initialDate, finalDate);
            // Container to hold Az/El measurements
            ArrayList<AngularAzEl> azElContainer = new ArrayList<AngularAzEl>();
            
            // Set up range builder
            final double[] rangeMatrix = new double[] {1, 1};
            final DiagonalMatrix rangeCovarianceMatrix = new DiagonalMatrix(rangeMatrix);
            final CorrelatedRandomVectorGenerator rangeNoiseGenerator = new CorrelatedRandomVectorGenerator(rangeCovarianceMatrix, small, gaussianGenerator);
            final double sigma = 1.;
            final double baseWeight = 1.;
            final RangeBuilder rangeBuilder = new RangeBuilder(rangeNoiseGenerator, groundStation, false, sigma, baseWeight, satelliteIndex);
            rangeBuilder.init(initialDate, finalDate);
            // Container to hold range measurements
            ArrayList<Range> rangeContainer = new ArrayList<Range>();
            
            // Extrapolation loop
            int cpt = 1;
            for (AbsoluteDate extrapDate = initialDate;
                 extrapDate.compareTo(finalDate) <= 0;
                 extrapDate = extrapDate.shiftedBy(stepT))  {

                final SpacecraftState currentState = propagator.propagate(extrapDate);
                
                // Add Az/El measurement to container
                SpacecraftState[] states = new SpacecraftState[] {currentState};
                AngularAzEl azel = azElBuilder.build(states);
                azElContainer.add(azel);
               
                // Add Range measurement to container
                Range range = rangeBuilder.build(states);
                rangeContainer.add(range);
              
                // Print Results
                System.out.format(Locale.US, "step %2d %s %s%n",
                        cpt++, currentState.getDate(), currentState.getOrbit());
                
                // Get first and last measurements for IOD
                if (extrapDate.compareTo(initialDate) == 0) {
                	initialPosition = getPositionFromAzEl(azel, range, staF, inertialFrame, extrapDate);
                }
                else if (extrapDate.compareTo(finalDate) == 0) {
                	finalPosition = getPositionFromAzEl(azel, range, staF, inertialFrame, extrapDate);
                }

            }
            
            // Initial Orbit Determination           
            final IodLambert lambert = new IodLambert(mu);
            // TODO: Posigrade and number of revolutions are set as guesses for now, but will need to be calculated later
            final boolean posigrade = true;
            final int nRev = 0;
            final KeplerianOrbit orbitEstimation = lambert.estimate(inertialFrame, posigrade, nRev, initialPosition, initialDate, finalPosition, finalDate);
            System.out.println("Lambert IOD Estimation: ");
            System.out.println(orbitEstimation.toString());
 
            // Least squares estimator setup
            final GaussNewtonOptimizer GNOptimizer = new GaussNewtonOptimizer();
            final EulerIntegratorBuilder eulerBuilder = new EulerIntegratorBuilder(stepT);
            final double positionScale = 1.;
            final NumericalPropagatorBuilder propBuilder = new NumericalPropagatorBuilder(orbitEstimation, eulerBuilder, PositionAngle.MEAN, positionScale);
            final BatchLSEstimator leastSquares = new BatchLSEstimator(GNOptimizer, propBuilder);            
            leastSquares.setMaxIterations(1000);
            leastSquares.setMaxEvaluations(1000);
            leastSquares.setParametersConvergenceThreshold(.001);
            // Add measurements
            azElContainer.forEach(measurement->leastSquares.addMeasurement(measurement));
            rangeContainer.forEach(measurement->leastSquares.addMeasurement(measurement));
            
            // Run least squares fit            
            AbstractIntegratedPropagator[] lSPropagators = leastSquares.estimate();
            System.out.println("Least Squares Estimation: ");
            System.out.println(lSPropagators[0].getInitialState()); 

			}catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        } catch (OrekitException oe) {
            System.err.println(oe.getLocalizedMessage());
        }
    }

}
