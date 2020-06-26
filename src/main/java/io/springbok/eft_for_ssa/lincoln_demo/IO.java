package io.springbok.eft_for_ssa.lincoln_demo;

import org.apache.flink.statefun.sdk.io.EgressIdentifier;
import org.apache.flink.statefun.sdk.io.EgressSpec;
import org.apache.flink.statefun.sdk.io.IngressIdentifier;
import org.apache.flink.statefun.sdk.io.IngressSpec;
import org.apache.flink.statefun.sdk.kafka.KafkaEgressBuilder;
import org.apache.flink.statefun.sdk.kafka.KafkaEgressSerializer;
import org.apache.flink.statefun.sdk.kafka.KafkaIngressBuilder;
import org.apache.flink.statefun.sdk.kafka.KafkaIngressDeserializer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class IO {

  public static final IngressIdentifier<String> TRACKS_INGRESS_ID =
      new IngressIdentifier<>(String.class, "eft-for-ssa", "tracks-in");
  //	public static final EgressIdentifier<KeyedOrbit> EGRESS_ID = new
  // EgressIdentifier<>("eft-for-ssa",
  // "keyed-orbit-out", KeyedOrbit.class);
  public static final EgressIdentifier<String> DEFAULT_EGRESS_ID =
      new EgressIdentifier<>("eft-for-ssa", "default-out", String.class);
  public static final EgressIdentifier<String> PRINT_EGRESS_ID =
      new EgressIdentifier<>("eft-for-ssa", "print-out", String.class);
  private final String kafkaAddress;

  public IO(String kafkaAddress) {
    this.kafkaAddress = (String) Objects.requireNonNull(kafkaAddress);
  }

  public IngressSpec<String> getIngressSpec() {
    return KafkaIngressBuilder.forIdentifier(TRACKS_INGRESS_ID)
        .withKafkaAddress(this.kafkaAddress)
        .withTopic("tracks")
        .withDeserializer(KafkaTracksDeserializer.class)
        .withProperty("group.id", "eft-for-ssa")
        .build();
  }

  EgressSpec<String> getEgressSpec() {
    return KafkaEgressBuilder.forIdentifier(DEFAULT_EGRESS_ID)
        .withKafkaAddress(this.kafkaAddress)
        .withSerializer(KafkaTracksSerializer.class)
        .withProperty("group.id", "eft-for-ssa")
        .build();
  }

  private static final class KafkaTracksDeserializer implements KafkaIngressDeserializer<String> {
    //    private static final long serialVersionUID = 1L;

    private KafkaTracksDeserializer() {}

    public String deserialize(ConsumerRecord<byte[], byte[]> input) {
      String string = new String((byte[]) input.value(), StandardCharsets.UTF_8);
      return string;
    }
  }

  private static final class KafkaTracksSerializer implements KafkaEgressSerializer<String> {

    private static final long serialVersionUID = 1L;

    @Override
    public ProducerRecord<byte[], byte[]> serialize(String response) {
      // TODO: Serialize to real keys
      byte[] key = response.getBytes(StandardCharsets.UTF_8);
      byte[] value = response.getBytes(StandardCharsets.UTF_8);

      return new ProducerRecord<>("default", key, value);
    }
  }
}
