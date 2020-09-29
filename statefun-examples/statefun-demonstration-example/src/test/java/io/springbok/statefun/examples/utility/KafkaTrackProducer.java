package io.springbok.statefun.examples.utility;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.ArrayList;
import java.util.Properties;

public class KafkaTrackProducer {

  public static void main(String[] args) throws Exception {

    Properties props = new Properties();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

    SetTestPaths.init();

    TrackGenerator trackGenerator = new TrackGenerator();
    trackGenerator.init();
    ArrayList<String> trackMessages = trackGenerator.finitePropagation();

    Producer producer = new KafkaProducer(props);

    trackMessages.forEach(
        message -> {
          producer.send(new ProducerRecord<>("tracks", message));
        });
    producer.flush();
    producer.close();
  }
}
