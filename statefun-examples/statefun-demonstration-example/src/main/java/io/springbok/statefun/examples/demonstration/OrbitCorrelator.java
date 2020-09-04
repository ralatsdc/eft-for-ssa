package io.springbok.statefun.examples.demonstration;

import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngle;

// Class used to determine if two orbits correlate
public class OrbitCorrelator {

  public static boolean correlate(KeyedOrbit keyedOrbit1, KeyedOrbit keyedOrbit2) {
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

    double anomaly1 = orbit1.getAnomaly(PositionAngle.MEAN);
    double anomaly2 = orbit2.getAnomaly(PositionAngle.MEAN);
    //    if (anomaly1 < 0) {
    //      anomaly1 = anomaly1 + 2 * Math.PI;
    //    }
    //    if (anomaly2 < 0) {
    //      anomaly2 = anomaly2 + 2 * Math.PI;
    //    }

    System.out.println(a1);
    System.out.println(a2);
    System.out.println(e1);
    System.out.println(e2);
    System.out.println(i1);
    System.out.println(i2);
    System.out.println(pa1);
    System.out.println(pa2);
    System.out.println(raan1);
    System.out.println(raan2);
    System.out.println(anomaly1);
    System.out.println(anomaly2);
    System.out.println(orbit1.getDate());
    System.out.println(orbit2.getDate());

    double epsilon = 0.0001;

    boolean a = (Math.abs(a1 - a2) < 10);
    boolean e = (Math.abs(e1 - e2) < epsilon);
    boolean i = (Math.abs(i1 - i2) < epsilon);
    boolean pa = (Math.abs(pa1 - pa2) < epsilon);
    boolean raan = (Math.abs(raan1 - raan2) < epsilon);
    boolean anomaly = (Math.abs(anomaly1 - anomaly2) < epsilon);

    System.out.println(a);
    System.out.println(e);
    System.out.println(i);
    System.out.println(pa);
    System.out.println(raan);
    System.out.println(anomaly);

    return (a && e && i && pa && raan && anomaly);
  }
}
