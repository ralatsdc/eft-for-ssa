package io.springbok.statefun.examples.utilities;

import io.springbok.statefun.examples.demonstration.ApplicationEnvironment;
import io.springbok.statefun.examples.demonstration.OrbitFactory;
import io.springbok.statefun.examples.demonstration.generated.SingleLineTLE;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.orekit.propagation.analytical.tle.TLE;

import java.io.File;
import java.util.ArrayList;
import java.util.Properties;

public class KafkaTLEProducer {

  public static void main(String[] args) throws Exception {

    OrbitFactory.init();

    Properties props = new Properties();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

    ApplicationEnvironment.setPathProperties();

    String tlePath = System.getProperty("TLE_PATH");

    // Add tles to list
    final File tleData = new File(tlePath);
    ArrayList<TLE> tles = TLEReader.readTLEs(tleData);

    Producer producer = new KafkaProducer(props);

    tles.forEach(
        tle -> {
          SingleLineTLE singleLineTLE = TLEReader.toSingleLineTLE(tle);
          producer.send(new ProducerRecord<>("tles", singleLineTLE));
        });
    producer.flush();
    producer.close();
  }
}
