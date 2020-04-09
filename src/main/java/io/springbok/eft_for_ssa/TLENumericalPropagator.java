package io.springbok.eft_for_ssa;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Locale;

import org.hipparchus.ode.AbstractIntegrator;
import org.hipparchus.ode.nonstiff.EulerIntegrator;
import org.hipparchus.ode.nonstiff.GillIntegrator;
import org.hipparchus.ode.nonstiff.LutherIntegrator;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;

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
            final File orekitData = new File("orekit-data");          
            final DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
            manager.addProvider(new DirectoryCrawler(orekitData));


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
            final double mu =  3.986004415e+14;

            // Initial orbit parameters
            final double a = (Math.cbrt(mu)) / (Math.cbrt(Math.pow((tle.getMeanMotion() * Math.PI / 86400), 2))); // semi major axis in meters 
			final double e = tle.getE(); // eccentricity
            final double i = tle.getI(); // inclination
            final double omega = tle.getPerigeeArgument(); // perigee argument
            final double raan = tle.getRaan(); // right ascension of ascending node
            final double lM = tle.getMeanAnomaly(); // mean anomaly

            // Inertial frame
            final Frame inertialFrame = FramesFactory.getEME2000();

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
            final GillIntegrator gill = new GillIntegrator(stepT);
            final LutherIntegrator luther = new LutherIntegrator(stepT); 

            // extrapolation with numerical propagator
            final NumericalPropagator numerical = new NumericalPropagator(euler);
                        
            //set initial state
            final SpacecraftState initialState = new SpacecraftState(initialOrbit);
            numerical.setInitialState(initialState);
            System.out.println(numerical.getMu());

            // Extrapolation loop
            int cpt = 1;
            for (AbsoluteDate extrapDate = initialDate;
                 extrapDate.compareTo(finalDate) <= 0;
                 extrapDate = extrapDate.shiftedBy(stepT))  {

                final SpacecraftState currentState = numerical.propagate(extrapDate);
                System.out.format(Locale.US, "step %2d %s %s%n",
                                  cpt++, currentState.getDate(), currentState.getOrbit());

            }
			}catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        } catch (OrekitException oe) {
            System.err.println(oe.getLocalizedMessage());
        }
    }

}
