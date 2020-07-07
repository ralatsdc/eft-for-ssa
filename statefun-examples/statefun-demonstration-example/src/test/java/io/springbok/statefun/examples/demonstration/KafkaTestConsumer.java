package io.springbok.statefun.examples.demonstration;

import io.springbok.statefun.examples.demonstration.generated.DefaultOut;
import org.apache.flink.statefun.flink.harness.io.SerializableConsumer;

import java.util.ArrayList;

public class KafkaTestConsumer implements SerializableConsumer<DefaultOut> {

	ArrayList<String> messages = new ArrayList<String>();

	@Override
	public void accept(DefaultOut message) {
		messages.add(message.getContent());
	}
}
