package io.springbok.statefun.examples.demonstration;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

public class KafkaTrackProducer {

  public static void main(String[] args) throws Exception {

    // Configure Kafka
    //    Properties props = new Properties();
    //    props.put("bootstrap.servers", "127.0.0.1:9092");
    //    props.put("acks", "all");
    //    props.put("retries", 0);
    //    props.put("batch.size", 16384);
    //    props.put("linger.ms", 1);
    //    props.put("buffer.memory", 33554432);
    //    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    //    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

    Properties props = new Properties();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

    //    TrackGenerator trackGenerator =
    //        new TrackGenerator("tle-data/globalstar_tles_05_18_2020.txt", "orekit-data");
    //    trackGenerator.init();
    //    ArrayList<String> trackMessages = trackGenerator.finitePropagation();

    Producer producer = new KafkaProducer(props);

    String message = "Hi";
    ProducerRecord record = new ProducerRecord("tracks", message);
    producer.send(record);
    producer.flush();

    //    trackMessages.forEach(
    //        message -> {
    //          producer.send(new ProducerRecord<>("tracks", message));
    //        });
    producer.close();
  }
}
