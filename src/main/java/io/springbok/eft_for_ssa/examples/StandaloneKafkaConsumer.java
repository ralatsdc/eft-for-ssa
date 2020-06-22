package io.springbok.eft_for_ssa;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

public class StandaloneKafkaConsumer {

	public static void main(String[] args) {

		Properties consumerProps = new Properties();
		consumerProps.setProperty("bootstrap.servers", "localhost:9092");
		consumerProps.setProperty("group.id", "test");
		consumerProps.setProperty("enable.auto.commit", "true");
		consumerProps.setProperty("auto.commit.interval.ms", "1000");
		consumerProps.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		consumerProps.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps);
		consumer.subscribe(Arrays.asList("my-topic"));
		while (true) {
			ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
			for (ConsumerRecord<String, String> record : records)
				System.out.printf("offset = %d, key = %s, value = %s%n", record.offset(), record.key(), record.value());
		}
	}

}
