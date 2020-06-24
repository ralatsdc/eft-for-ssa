package io.springbok.eft_for_ssa.examples;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class KafkaFileReaderProducer {

  static String inputPath = "output/2020-05-25_tracklet_messages.txt";

  public static void main(String[] args) {

    Properties props = new Properties();
    props.put("bootstrap.servers", "localhost:9092");
    props.put("acks", "all");
    props.put("retries", 0);
    props.put("batch.size", 16384);
    props.put("linger.ms", 1);
    props.put("buffer.memory", 33554432);
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

    Producer<String, String> producer = new KafkaProducer<>(props);

    try {
      File file = new File(inputPath); // creates a new file instance
      FileReader fr = new FileReader(file); // reads the file
      BufferedReader br = new BufferedReader(fr); // creates a buffering character input stream
      StringBuffer sb = new StringBuffer(); // constructs a string buffer with no characters
      String line;
      int iterator = 0;
      while ((line = br.readLine()) != null) {
        producer.send(
            new ProducerRecord<String, String>("tracklets", Integer.toString(iterator), line));
        iterator++;
      }
      fr.close(); // closes the stream and release the resources
    } catch (IOException e) {
      e.printStackTrace();
    }

    producer.close();
  }
}
