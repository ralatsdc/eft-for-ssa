package io.springbok.eft_for_ssa;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.Random;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.AbstractRealMatrix;
import org.hipparchus.linear.BlockRealMatrix;
import org.hipparchus.linear.DiagonalMatrix;
import org.hipparchus.ode.AbstractIntegrator;
import org.hipparchus.ode.nonstiff.EulerIntegrator;
import org.hipparchus.ode.nonstiff.GillIntegrator;
import org.hipparchus.ode.nonstiff.LutherIntegrator;
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
import org.orekit.data.DirectoryCrawler;
import org.orekit.data.LazyLoadedDataContext;
import org.orekit.data.NetworkCrawler;
import org.orekit.data.ZipJarCrawler;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.iod.IodGooding;
import org.orekit.estimation.iod.IodLambert;
import org.orekit.estimation.leastsquares.BatchLSEstimator;
import org.orekit.estimation.measurements.AngularAzEl;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.PV;
import org.orekit.estimation.measurements.Position;
import org.orekit.estimation.measurements.Range;
import org.orekit.estimation.measurements.generation.AngularAzElBuilder;
import org.orekit.estimation.measurements.generation.PVBuilder;
import org.orekit.estimation.measurements.generation.RangeBuilder;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.frames.Transform;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.conversion.EulerIntegratorBuilder;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.propagation.integration.AbstractIntegratedPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;
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

    /** Program entry point.
     * @param args program arguments (unused here)
     */
    public static void main(final String[] args) throws IOException {
        try {
        	
            // configure Orekit
            final URL utcTaiData = new URL("https://hpiers.obspm.fr/eoppc/bul/bulc/UTC-TAI.history");
            final URL eopData = new URL("ftp://ftp.iers.org/products/eop/rapid/daily/finals.daily"); 
            final DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
            manager.addProvider(new NetworkCrawler(utcTaiData));
            //this one isn't needed for this to work
            manager.addProvider(new NetworkCrawler(eopData));
            
            System.out.println(manager.getProviders());

            //read tle file
            final File tleData = new File("04_07_2020.txt");            
            
            BufferedReader tleReader;
			try {
				tleReader = new BufferedReader(new FileReader(tleData));
				String line1 = tleReader.readLine();
				String line2 = tleReader.readLine();	
				System.out.println(line1);
				System.out.println(line2);	
				TLE tle = new TLE(line1, line2);
			 
            // gravitation coefficient
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
            
            //can convert existing orbits, or the propagator will do it automatically
            //final Orbit initialOrbit = new EquinoctialOrbit(initialKeplarOrbit);

            // Overall duration in seconds for extrapolation
            final double duration = 600.;

            // Stop date
            final AbsoluteDate finalDate = initialDate.shiftedBy(duration);

            // Step duration in seconds
            final double stepT = 60.;

            //establishing frame
            double longitude = FastMath.toRadians(45.);
            double latitude  = FastMath.toRadians(25.);
            double altitude  = 0.;
            GeodeticPoint station = new GeodeticPoint(latitude, longitude, altitude);
            Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
            BodyShape earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
            										Constants.WGS84_EARTH_FLATTENING,
            										earthFrame);
            TopocentricFrame staF = new TopocentricFrame(earth, station, "station");
             
            //set initial state
            final SpacecraftState initialState = new SpacecraftState(initialOrbit);
            SpacecraftState finalState = null;
            
            //initialize orbit estimation variables
            Vector3D initialPosition = null;
            Vector3D finalPosition = null;

            // Setting up propagator
            final EulerIntegrator euler = new EulerIntegrator(stepT);
            //final NumericalPropagator propagator = new NumericalPropagator(euler);
            final KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit);
            //propagator.setInitialState(initialState);   

            //set propagator index of satellite
            final ObservableSatellite sat0 = new ObservableSatellite(0);
            
            // Setting up noise
            final Random random = new Random();
 
            //build the least squares estimator
            final GaussNewtonOptimizer GNOptimizer = new GaussNewtonOptimizer();
            final EulerIntegratorBuilder eulerBuilder = new EulerIntegratorBuilder(stepT);
            //Set to 10x more accurate than you expect measurement to be - (https://forum.orekit.org/t/normalized-parameters-question/645/2)
            final double positionScale = 1.;
            final NumericalPropagatorBuilder propBuilder = new NumericalPropagatorBuilder(initialOrbit, eulerBuilder, PositionAngle.MEAN, positionScale);
            final BatchLSEstimator leastSquares = new BatchLSEstimator(GNOptimizer, propBuilder);            
            leastSquares.setMaxIterations(100);
            leastSquares.setMaxEvaluations(100);
            leastSquares.setParametersConvergenceThreshold(.001);
            
            //initialize ground station using topographic frame
            final GroundStation groundStation = new GroundStation(staF);
            groundStation.getPrimeMeridianOffsetDriver().setReferenceDate(initialDate);
            groundStation.getPolarOffsetXDriver().setReferenceDate(initialDate);
            groundStation.getPolarOffsetYDriver().setReferenceDate(initialDate);

            //Setting up random number generator
            final int small = 0;
            final ISAACRandom randomNumGenerator = new ISAACRandom();
            final GaussianRandomGenerator gaussianGenerator = new GaussianRandomGenerator(randomNumGenerator);

            //setting up azel builder
            final double[] angularMatrix = new double[] {1, 1};
            final DiagonalMatrix angularCovarianceMatrix = new DiagonalMatrix(angularMatrix);
            final CorrelatedRandomVectorGenerator angularNoiseGenerator = new CorrelatedRandomVectorGenerator(angularCovarianceMatrix, small, gaussianGenerator);
            final double[] azElSigmas = new double[] {0.001, 0.001};
            final double[] azElBaseWeights = new double[] {1., 1.};
            final AngularAzElBuilder azElBuilder = new AngularAzElBuilder(angularNoiseGenerator, groundStation, azElSigmas, azElBaseWeights, sat0);
            azElBuilder.init(initialDate, finalDate);
            
            //setting up range builder
            final double[] rangeMatrix = new double[] {1, 1};
            final DiagonalMatrix rangeCovarianceMatrix = new DiagonalMatrix(rangeMatrix);
            final CorrelatedRandomVectorGenerator rangeNoiseGenerator = new CorrelatedRandomVectorGenerator(rangeCovarianceMatrix, small, gaussianGenerator);
            final double sigma = 1.;
            final double baseWeight = 1.;
            final RangeBuilder rangeBuilder = new RangeBuilder(rangeNoiseGenerator, groundStation, false, sigma, baseWeight, sat0);
            rangeBuilder.init(initialDate, finalDate);
            
            //setting up pv builder
            final double[] posMatrix = new double[] {1, 1, 1, 1, 1, 1};
            final DiagonalMatrix posCovarianceMatrix = new DiagonalMatrix(posMatrix);
            final CorrelatedRandomVectorGenerator posNoiseGenerator = new CorrelatedRandomVectorGenerator(posCovarianceMatrix, small, gaussianGenerator);
            final PVBuilder pvBuilder = new PVBuilder(posNoiseGenerator, sigma, sigma, baseWeight, sat0);

            // Extrapolation loop
            int cpt = 1;
            for (AbsoluteDate extrapDate = initialDate;
                 extrapDate.compareTo(finalDate) <= 0;
                 extrapDate = extrapDate.shiftedBy(stepT))  {

                final SpacecraftState currentState = propagator.propagate(extrapDate);
                
                Transform transform = currentState.getFrame().getTransformTo(staF, currentState.getDate());
                PVCoordinates pv0 = transform.transformPVCoordinates(currentState.getPVCoordinates()); 
                final Vector3D inertialPosition0 = currentState.getPVCoordinates().getPosition(); 
                
                //get az/el
                Vector3D p = pv0.getPosition();
                double azimuth = p.getAlpha();
                double elevation = p.getDelta();
                double range0 = p.getNorm();
                
                //add noise
                double noise = random.nextGaussian() * 0.;
                azimuth = azimuth + noise;
                noise = random.nextGaussian() * 0.;
                elevation = elevation + noise;
                
                //back to inertial frame
                Vector3D noisyAngle = new Vector3D(azimuth, elevation);
                Vector3D noisyPosition = new Vector3D(range0, noisyAngle);
                Transform spaceTransform = staF.getTransformTo(currentState.getFrame(), extrapDate);
                Transform spaceTransform0 = groundStation.getOffsetToInertial(inertialFrame, extrapDate);
                Vector3D inertialPosition = spaceTransform.transformPosition(noisyPosition);
                 
                //add measurements
                //What value do we need for these?
//                double sigmaPosition = 1.;
//                double baseWeight = 1.;
//                Position position = new Position(extrapDate, inertialPosition, sigmaPosition, baseWeight, sat0);
//                leastSquares.addMeasurement(position);
                
                
             
                
                
                //Az/El measurement
                SpacecraftState[] states = new SpacecraftState[] {currentState};
                AngularAzEl azel = azElBuilder.build(states);
//                leastSquares.addMeasurement(azel);
//                
//                //Range measurement
                Range range = rangeBuilder.build(states);
//                leastSquares.addMeasurement(range);
                
                PV pv = pvBuilder.build(states);
                leastSquares.addMeasurement(pv);
              
                System.out.format(Locale.US, "step %2d %s %s%n",
                        cpt++, currentState.getDate(), currentState.getOrbit());
                
                if (extrapDate.compareTo(initialDate) == 0) {
                	
                	double[] azelMeasurement = azel.getObservedValue();
                	double az = azel.getObservedValue()[0];
                	double az0 = -az + (Math.PI / 2);
                	double el = azel.getObservedValue()[1];
                	double ra = range.getObservedValue()[0];
                	Vector3D angle = new Vector3D(az, el);
                	Vector3D pos = new Vector3D(ra, angle);
                	Vector3D angle0 = new Vector3D(az0, el);
                	Vector3D pos0 = new Vector3D(ra, angle0);
                	pos = transform.transformPosition(pos);
                	Vector3D inertialPos = spaceTransform.transformPosition(pos);
                	Vector3D inertialPos0 = spaceTransform.transformPosition(pos0);
                	initialPosition = inertialPos0;
                	
                	//initialPosition = pvBuilder.build(states).getPosition();
                }
                else if (extrapDate.compareTo(finalDate) == 0) {
                	double[] azelMeasurement = azel.getObservedValue();
                	double az = azel.getObservedValue()[0];
                	double az0 = -az + (Math.PI / 2);
                	double el = azel.getObservedValue()[1];
                	double ra = range.getObservedValue()[0];
                	Vector3D angle = new Vector3D(az, el);
                	Vector3D pos = new Vector3D(ra, angle);
                	Vector3D angle0 = new Vector3D(az0, el);
                	Vector3D pos0 = new Vector3D(ra, angle0);
                	pos = transform.transformPosition(pos);
                	Vector3D inertialPos = spaceTransform.transformPosition(pos);
                	Vector3D inertialPos0 = spaceTransform.transformPosition(pos0);
                	finalPosition = inertialPos0;

                	//finalPosition = pvBuilder.build(states).getPosition();
                }

            }
            
            //construct orbit            
            final IodLambert lambert = new IodLambert(mu);
            //set as guesses for now, but will need to calculate later
            final boolean posigrade = true;
            final int nRev = 0;
            final KeplerianOrbit orbitEstimation = lambert.estimate(inertialFrame, posigrade, nRev, initialPosition, initialDate, finalPosition, finalDate);
            System.out.println("Lambert IOD Estimation: ");
            System.out.println(orbitEstimation.toString());
            
            //least squares fit            
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
