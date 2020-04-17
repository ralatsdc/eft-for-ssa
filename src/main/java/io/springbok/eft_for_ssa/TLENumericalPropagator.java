package io.springbok.eft_for_ssa;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.Locale;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.AbstractIntegrator;
import org.hipparchus.ode.nonstiff.EulerIntegrator;
import org.hipparchus.ode.nonstiff.GillIntegrator;
import org.hipparchus.ode.nonstiff.LutherIntegrator;
import org.hipparchus.optim.nonlinear.vector.leastsquares.GaussNewtonOptimizer;
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
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.conversion.EulerIntegratorBuilder;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
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

            final EulerIntegrator euler = new EulerIntegrator(stepT);

            // extrapolation with numerical propagator
            final NumericalPropagator numerical = new NumericalPropagator(euler);
             
            //set initial state
            final SpacecraftState initialState = new SpacecraftState(initialOrbit);
            SpacecraftState finalState = null;
            numerical.setInitialState(initialState);

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

            // Extrapolation loop
            int cpt = 1;
            for (AbsoluteDate extrapDate = initialDate;
                 extrapDate.compareTo(finalDate) <= 0;
                 extrapDate = extrapDate.shiftedBy(stepT))  {

                final SpacecraftState currentState = numerical.propagate(extrapDate);
                System.out.format(Locale.US, "step %2d %s %s%n",
                        cpt++, currentState.getDate(), currentState.getOrbit());
                
                if (extrapDate.compareTo(finalDate) == 0) {
                	finalState = currentState;
                }

            }

            //build the least squares estimator
            final GaussNewtonOptimizer GNOptimizer = new GaussNewtonOptimizer();
            final EulerIntegratorBuilder eulerBuilder = new EulerIntegratorBuilder(stepT);
            //Set to 10x more accurate than you expect measurement to be - (https://forum.orekit.org/t/normalized-parameters-question/645/2)
            final double positionScale = 0.001;
            final NumericalPropagatorBuilder propBuilder = new NumericalPropagatorBuilder(initialOrbit, eulerBuilder, PositionAngle.MEAN, positionScale);
            final BatchLSEstimator leastSquares = new BatchLSEstimator(GNOptimizer, propBuilder);            
            leastSquares.setMaxIterations(10);
            leastSquares.setMaxEvaluations(10);
            
            final IodLambert lambert = new IodLambert(mu);
            final Vector3D initialPosition =  initialState.getPVCoordinates(inertialFrame).getPosition();
            final Vector3D initialVelocity =  initialState.getPVCoordinates(inertialFrame).getVelocity();
            final double initialSigmaPosition = 0.1;
            final double initialSigmaVelocity = 0.1;
            final double baseWeight = 1;
            
            final Vector3D finalPosition = finalState.getPVCoordinates(inertialFrame).getPosition();
            final Vector3D finalVelocity =  finalState.getPVCoordinates(inertialFrame).getVelocity(); 
            final double finalSigmaPosition = 0.1;
            final double finalSigmaVelocity = 0.1;

            //set propagator index of satellite
            final ObservableSatellite sat0 = new ObservableSatellite(0);
            
            final PV initialPV = new PV(initialDate, initialPosition, initialVelocity, initialSigmaPosition, initialSigmaVelocity, baseWeight, sat0);
            final PV finalPV = new PV(finalDate, finalPosition, finalVelocity, finalSigmaPosition, finalSigmaVelocity, baseWeight, sat0);
            
            //add measurements to least squares estimator
            leastSquares.addMeasurement(initialPV);
            leastSquares.addMeasurement(finalPV);
            System.out.println(leastSquares.getPropagatorParametersDrivers(true).getDrivers());
            System.out.println(leastSquares.getPropagatorParametersDrivers(true).getDrivers());
            System.out.println(leastSquares.getMeasurementsParametersDrivers(true).getDrivers());
            System.out.println("Least Squares Estimation: ");
            System.out.println(leastSquares.estimate());
            
            //final AngularAzEl azEl = new AngularAzEl(station, initialDate, double[] angular, double[] sigma, double[] baseWeight, ObservableSatellite satellite);
            
            //set as default for now, but will need to calculate later
            //final double angularSeparation = initialPosition.angle(initialPosition, finalPosition); 
            //System.out.println(angularSeparation);
            final boolean posigrade = true;
            final int nRev = 0;
        
            final KeplerianOrbit orbitEstimation = lambert.estimate(inertialFrame, posigrade, nRev, initialPosition, initialDate, finalPosition, finalDate);
            
            System.out.println(orbitEstimation.toString());
            
			}catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        } catch (OrekitException oe) {
            System.err.println(oe.getLocalizedMessage());
        }
    }

}
