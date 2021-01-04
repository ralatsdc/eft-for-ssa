package io.springbok.statefun.examples.utilities;

import io.springbok.statefun.examples.demonstration.ApplicationEnvironment;
import org.apache.commons.cli.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.orekit.propagation.analytical.tle.TLE;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class KafkaTrackProducer {

  private static TrackGenerator trackGenerator = null;
  private static Iterator<TLE> tleIterator = null;
  private static double stepSize = 0.0;
  private static double timePassed = 0.0;
  private static long tracksPerSecond = 3;

  public static void main(String[] args) {

    // Define command line options, then parse command line
    Options options = new Options();
    options.addOption("i", "infinite-producer", false, "use infinite track producer");
    options.addOption("t", "tracks-per-second", true, "number of tracks per second");
    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = null;
    try {
      cmd = parser.parse(options, args);
    } catch (ParseException e) {
      System.out.println("Cannot parse command line: " + e);
      System.exit(1);
    }
    if (cmd.hasOption("t")) {
      tracksPerSecond = Long.parseLong(cmd.getOptionValue("t"));
    }

    // Set Orekit, TLE, and properties path
    try {
      ApplicationEnvironment.setPathProperties();
    } catch (Exception e) {
      System.out.println("Cannot set paths: " + e);
      System.exit(2);
    }

    // Initialize track generator
    try {
      String tlePath = System.getProperty("TLE_PATH");
      trackGenerator = new TrackGenerator(tlePath);
      trackGenerator.init();
    } catch (Exception e) {
      System.out.println("Cannot initialize track generator: " + e);
      System.exit(3);
    }

    // Produce messages, then clean up
    Properties props = new Properties();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    try (Producer producer = new KafkaProducer(props)) {
      if (cmd.hasOption("i")) {
        // Produce an infinite track message stream
        tleIterator = trackGenerator.tles.iterator();
        stepSize = trackGenerator.largeStep; // seconds
        Runnable sendMessage =
            new Runnable() {
              @Override
              public void run() {
                if (!tleIterator.hasNext()) {
                  // Start from beginning
                  tleIterator = trackGenerator.tles.iterator();
                  timePassed = timePassed + stepSize; // seconds
                }
                TLE tle = tleIterator.next();
                String message = trackGenerator.propagate(tle, timePassed);
                producer.send(new ProducerRecord<>("tracks", message));
                producer.flush();
                System.out.println("Sent Message: " + message);
              }
            };
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(sendMessage, 0, 1000 / tracksPerSecond, TimeUnit.MILLISECONDS);

      } else {
        // Produce a finite track message stream
        ArrayList<String> trackMessages = trackGenerator.finitePropagation();
        for (String message : trackMessages) {
          producer.send(new ProducerRecord<>("tracks", message));
          producer.flush();
          System.out.println("Sent Message: " + message);
          Thread.sleep(1000 / tracksPerSecond);
        }
      }
    } catch (Exception e) {
      System.out.println("Cannot send messages: " + e);
      System.exit(4);
    }
  }
}
