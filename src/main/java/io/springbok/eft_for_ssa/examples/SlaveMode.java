package io.springbok.eft_for_ssa.examples;

import org.hipparchus.util.FastMath;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;

import java.io.File;
import java.util.Locale;

/**
 * Orekit tutorial for slave mode propagation.
 *
 * <p>This tutorial shows a basic usage of the slave mode in which the user drives all propagation
 * steps.
 *
 * <p>
 *
 * @author Pascal Parraud
 */
public class SlaveMode {

  /** Private constructor for utility class. */
  private SlaveMode() {
    // empty
  }

  /**
   * Program entry point.
   *
   * @param args program arguments (unused here)
   */
  public static void main(final String[] args) {
    try {

      // configure Orekit
      final File orekitData = new File("orekit-data");

      final DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
      manager.addProvider(new DirectoryCrawler(orekitData));

      // Initial orbit parameters
      final double a = 24396159; // semi major axis in meters
      final double e = 0.72831215; // eccentricity
      final double i = FastMath.toRadians(7); // inclination
      final double omega = FastMath.toRadians(180); // perigee argument
      final double raan = FastMath.toRadians(261); // right ascension of ascending node
      final double lM = 0; // mean anomaly

      // Inertial frame
      final Frame inertialFrame = FramesFactory.getEME2000();

      // Initial date in UTC time scale
      final TimeScale utc = TimeScalesFactory.getUTC();
      final AbsoluteDate initialDate = new AbsoluteDate(2004, 01, 01, 23, 30, 00.000, utc);

      // gravitation coefficient
      final double mu = 3.986004415e+14;

      // Orbit construction as Keplerian
      final Orbit initialOrbit =
          new KeplerianOrbit(
              a, e, i, omega, raan, lM, PositionAngle.MEAN, inertialFrame, initialDate, mu);

      // Simple extrapolation with Keplerian motion
      final KeplerianPropagator kepler = new KeplerianPropagator(initialOrbit);

      // Set the propagator to slave mode (could be omitted as it is the default mode)
      kepler.setSlaveMode();

      // Overall duration in seconds for extrapolation
      final double duration = 600.;

      // Stop date
      final AbsoluteDate finalDate = initialDate.shiftedBy(duration);

      // Step duration in seconds
      final double stepT = 60.;

      // Extrapolation loop
      int cpt = 1;
      for (AbsoluteDate extrapDate = initialDate;
          extrapDate.compareTo(finalDate) <= 0;
          extrapDate = extrapDate.shiftedBy(stepT)) {

        final SpacecraftState currentState = kepler.propagate(extrapDate);
        System.out.format(
            Locale.US, "step %2d %s %s%n", cpt++, currentState.getDate(), currentState.getOrbit());
      }

    } catch (OrekitException oe) {
      System.err.println(oe.getLocalizedMessage());
    }
  }
}
