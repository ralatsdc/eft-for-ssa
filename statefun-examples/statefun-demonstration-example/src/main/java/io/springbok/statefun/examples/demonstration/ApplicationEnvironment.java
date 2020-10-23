package io.springbok.statefun.examples.demonstration;

import java.nio.file.Files;
import java.nio.file.Paths;

public class ApplicationEnvironment {

  public static void setPathProperties() throws Exception {
    setPathProperty("OREKIT_PATH");
    setPathProperty("TLE_PATH");
    setPathProperty("PROPERTIES_PATH");
  }

  public static void setPathProperty(String property) throws Exception {
    String path = null;
    if (System.getenv(property) != null) {
      path = System.getenv(property);
    } else {
      String rootPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
      rootPath = rootPath.substring(0, rootPath.indexOf("eft-for-ssa"));
      switch (property) {
        case "OREKIT_PATH":
          path = rootPath + "eft-for-ssa/orekit-data";
          break;

        case "TLE_PATH":
          path = rootPath + "eft-for-ssa/tle-data/globalstar_tles_05_18_2020.txt";
          break;

        case "PROPERTIES_PATH":
          path = rootPath + "eft-for-ssa/statefun-examples/statefun-demonstration-example/src/resources/test.properties";
          break;

        default:
          throw new Exception("Unexpected property");
      }
    }
    if (!Files.exists(Paths.get(path))) {
      throw new Exception(String.format("Path %s does not exist", path));
    }
    System.setProperty(property, path);
  }
}
