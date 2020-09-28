package io.springbok.statefun.examples.demonstration;

import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.time.TimeScalesFactory;

import java.io.FileInputStream;
import java.util.Properties;

// Class used to determine if two orbits correlate
public class OrbitCorrelator {

  public static boolean correlate(KeyedOrbit keyedOrbit1, KeyedOrbit keyedOrbit2) throws Exception {

    OrbitFactory.init();

    Properties defaultProps = new Properties();
    FileInputStream in = new FileInputStream(System.getProperty("default.properties"));
    defaultProps.load(in);
    in.close();
    Properties applicationProps = new Properties(defaultProps);

    KeplerianOrbit orbit1 = (KeplerianOrbit) keyedOrbit1.orbit;
    KeplerianOrbit orbit2 = (KeplerianOrbit) keyedOrbit2.orbit;

    double a1 = orbit1.getA();
    double a2 = orbit2.getA();

    double e1 = orbit1.getE();
    double e2 = orbit2.getE();

    double i1 = orbit1.getI();
    double i2 = orbit2.getI();

    double pa1 = orbit1.getPerigeeArgument();
    double pa2 = orbit2.getPerigeeArgument();
    if (pa1 < 0) {
      pa1 = pa1 + 2 * Math.PI;
    }
    if (pa2 < 0) {
      pa2 = pa2 + 2 * Math.PI;
    }

    double raan1 = orbit1.getRightAscensionOfAscendingNode();
    double raan2 = orbit2.getRightAscensionOfAscendingNode();
    if (raan1 < 0) {
      raan1 = raan1 + 2 * Math.PI;
    }
    if (raan2 < 0) {
      raan2 = raan2 + 2 * Math.PI;
    }

    // Calculate anomaly adjusted for time
    double anomaly1 = orbit1.getAnomaly(PositionAngle.MEAN);
    double anomaly2 = orbit2.getAnomaly(PositionAngle.MEAN);
    if (anomaly1 < 0) {
      anomaly1 = anomaly1 + 2 * Math.PI;
    }
    if (anomaly2 < 0) {
      anomaly2 = anomaly2 + 2 * Math.PI;
    }

    double offset =
        keyedOrbit1
            .orbit
            .getDate()
            .offsetFrom(keyedOrbit2.orbit.getDate(), TimeScalesFactory.getUTC());

    double meanMotion2 = orbit2.getKeplerianMeanMotion();

    double adjustedAnomaly2 = (anomaly2 + meanMotion2 * offset) % (2 * Math.PI);
    if (adjustedAnomaly2 < 0) {
      adjustedAnomaly2 = adjustedAnomaly2 + 2 * Math.PI;
    }

    double epsilon = Double.parseDouble(applicationProps.getProperty("epsilon"));
    double aEpsilon = Double.parseDouble(applicationProps.getProperty("aEpsilon"));

    boolean a = (Math.abs(a1 - a2) < aEpsilon);
    boolean e = (Math.abs(e1 - e2) < epsilon);
    boolean i = (Math.abs(i1 - i2) < epsilon);
    boolean pa = (Math.abs(pa1 - pa2) < epsilon);
    boolean raan = (Math.abs(raan1 - raan2) < epsilon);
    boolean anomaly = (Math.abs(anomaly1 - adjustedAnomaly2) < 0.1);

    return (a && e && i && pa && raan && anomaly);
  }
}
