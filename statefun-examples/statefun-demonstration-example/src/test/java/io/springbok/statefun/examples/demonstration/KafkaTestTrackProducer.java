package io.springbok.statefun.examples.demonstration;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.ArrayList;
import java.util.Properties;

public class KafkaTestTrackProducer {

  ArrayList<String> tracks;
  Properties props;

  public KafkaTestTrackProducer(ArrayList<String> tracks) {

    this.tracks = tracks;

    // Configure Kafka
    this.props = new Properties();
    props.put("bootstrap.servers", "localhost:9092");
    props.put("acks", "all");
    props.put("retries", 0);
    props.put("batch.size", 16384);
    props.put("linger.ms", 1);
    props.put("buffer.memory", 33554432);
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

  }

public void sendTracks(){
  Producer<String, String> producer = new KafkaProducer<>(props);
  // TODO: load a catalog and compute tracks
  for (int i = 0; i < tracks.size(); i++) {
    producer.send(
            new ProducerRecord<String, String>(
                    "tracks", tracks.get(i)));
  }

  producer.close();
}
}
