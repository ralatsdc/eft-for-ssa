package io.springbok.statefun.examples.utilities;

public class SetTestPaths {
  public static void init() {
    String rootPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
    rootPath = rootPath.substring(0, rootPath.indexOf("eft-for-ssa"));
    String orekitPath = rootPath + "eft-for-ssa/orekit-data";
    System.setProperty("OREKIT_PATH", orekitPath);

    String tlePath = rootPath + "eft-for-ssa/tle-data/globalstar_tles_05_18_2020.txt";
    System.setProperty("TLE_PATH", tlePath);

    String propertiesPath =
        rootPath
            + "eft-for-ssa/statefun-examples/statefun-demonstration-example/src/resources/test.properties";
    System.setProperty("PROPERTIES_PATH", propertiesPath);
  }
}
