package io.springbok.statefun.examples.utility;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.orekit.propagation.analytical.tle.TLE;

import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class KafkaInfiniteTrackProducer {

  private static Iterator<TLE> tleIterator;
  private static Double timePassed = 0.;
  private static Double stepSize;

  public static void main(String[] args) throws Exception {

    Properties props = new Properties();
    //    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "192.168.64.37:30092");
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

    SetSystemProperties.init();

    TrackGenerator trackGenerator = new TrackGenerator();
    trackGenerator.init();

    tleIterator = trackGenerator.tles.iterator();

    Producer producer = new KafkaProducer(props);

    stepSize = trackGenerator.largeStep;

    Runnable sendMessage =
        new Runnable() {
          @Override
          public void run() {

            if (!tleIterator.hasNext()) {
              // Start from beginning
              tleIterator = trackGenerator.tles.iterator();
              timePassed = timePassed + stepSize;
            }
            TLE tle = tleIterator.next();

            String message = trackGenerator.propagate(tle, timePassed);
            producer.send(new ProducerRecord<>("tracks", message));
            System.out.println("Sent Message: " + message);
          }
        };

    ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    executor.scheduleAtFixedRate(sendMessage, 0, 1000, TimeUnit.MILLISECONDS);

    //    producer.flush();
  }
}
