package io.springbok.statefun.examples.demonstration;

import java.io.FileInputStream;
import java.util.Properties;

public class ApplicationProperties {

  private static Properties properties = null;
  private static Double epsilon = null;
  private static Double axisEpsilon = null;
  private static Long deleteTimer = null;
  private static Long wakeupInterval = null;
  private static Integer trackCutoff = null;
  private static Integer logLevel = null;
  private static Boolean correlateByObject = null;

  public static Properties getProperties() throws Exception {
    if (properties == null) {
      Properties defaultProps = new Properties();
      FileInputStream in =
          new FileInputStream(
              System.getProperty("PROPERTIES_PATH", System.getenv("PROPERTIES_PATH")));
      defaultProps.load(in);
      in.close();
      properties = new Properties(defaultProps);
    }
    return properties;
  }

  public static double getEpsilon() throws Exception {
    if (epsilon == null) {
      epsilon = Double.parseDouble(getProperties().getProperty("epsilon"));
    }
    return epsilon;
  }

  public static double getAxisEpsilon() throws Exception {
    if (axisEpsilon == null) {
      axisEpsilon = Double.parseDouble(getProperties().getProperty("axisEpsilon"));
    }
    return axisEpsilon;
  }

  public static int getTrackCutoff() throws Exception {
    if (trackCutoff == null) {
      trackCutoff = Integer.parseInt(getProperties().getProperty("trackCutoff"));
    }
    return trackCutoff;
  }

  public static long getDeleteTimer() throws Exception {
    if (deleteTimer == null) {
      deleteTimer = Long.parseLong(getProperties().getProperty("deleteTimer"));
    }
    return deleteTimer;
  }

  public static long getWakeupInterval() throws Exception {
    if (wakeupInterval == null) {
      wakeupInterval = Long.parseLong(getProperties().getProperty("wakeupInterval"));
    }
    return wakeupInterval;
  }

  public static Integer getLogLevel() throws Exception {
    if (logLevel == null) {
      logLevel = Integer.parseInt(getProperties().getProperty("logLevel"));
    }
    return logLevel;
  }

  public static Boolean getCorrelateByObject() throws Exception {
    if (correlateByObject == null) {
      correlateByObject = Boolean.parseBoolean(getProperties().getProperty("correlateByObject"));
    }
    return correlateByObject;
  }
}
