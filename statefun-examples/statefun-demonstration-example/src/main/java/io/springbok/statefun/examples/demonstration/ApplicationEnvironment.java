package io.springbok.statefun.examples.utilities;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SetPaths {

  public static void setPaths() {
    setPath("OREKIT_PATH");
    setPath("TLE_PATH");
    setPath("PROPERTIES_PATH");

    if (System.getenv("OREKIT_PATH") == null) {
      String orekitPath = rootPath + "eft-for-ssa/orekit-data";
      System.setProperty("OREKIT_PATH", orekitPath);
    }

    if (System.getenv("TLE_PATH") == null) {
      String tlePath = rootPath + "eft-for-ssa/tle-data/globalstar_tles_05_18_2020.txt";
      System.setProperty("TLE_PATH", tlePath);
    }

    if (System.getenv("PROPERTIES_PATH") == null) {
      String propertiesPath =
              rootPath
                      + "eft-for-ssa/statefun-examples/statefun-demonstration-example/src/resources/test.properties";
      System.setProperty("PROPERTIES_PATH", propertiesPath);
    }
  }

  public static void setPath(String property) throws Exception {
    if (System.getenv(property) == null) {
      String rootPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
      rootPath = rootPath.substring(0, rootPath.indexOf("eft-for-ssa"));
      String path;
      switch (property) {
        case "OREKIT_PATH":
          path = rootPath + "eft-for-ssa/orekit-data";
          break;

        case "TLE_PATH":
          path = rootPath + "eft-for-ssa/tle-data/globalstar_tles_05_18_2020.txt";
          break;

        case "PROPERTIES_PATH":
          path = rootPath + "eft-for-ssa/statefun-examples/statefun-demonstration-example/src/resources/test.properties";

        default:
          throw new Exception("Unexpected property");
      }
      if (!Files.exists(Paths.get(path))) {
        throw new Exception(String.format("Path %s does not exist", path));
      }
      System.setProperty(property, path);
    }
  }
}
