package io.springbok.statefun.examples.prototype;

import java.util.Random;

public class CompareOrbits {

  private CompareOrbits() {}

  public static boolean compareAtRandom(KeyedOrbit orbit1, KeyedOrbit orbit2) {
    Random random = new Random();
    return random.nextBoolean();
  }
}
