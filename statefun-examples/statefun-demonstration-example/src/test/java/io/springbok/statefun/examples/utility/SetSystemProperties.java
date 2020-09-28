package io.springbok.statefun.examples.utility;

public class SetSystemProperties {
  public static void init() {
    String rootPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
    rootPath = rootPath.substring(0, rootPath.indexOf("eft-for-ssa"));
    String orekitPath = rootPath + "eft-for-ssa/orekit-data";
    System.setProperty("orekit-path", orekitPath);

    String tlePath = rootPath + "eft-for-ssa/tle-data/globalstar_tles_05_18_2020.txt";
    System.setProperty("tle-path", tlePath);

    String propertiesPath =
        rootPath
            + "eft-for-ssa/statefun-examples/statefun-demonstration-example/src/resources/default.properties";
    System.setProperty("default.properties", propertiesPath);
  }
}
