package io.springbok.eft_for_ssa.examples;

import org.apache.flink.statefun.sdk.io.EgressIdentifier;
import org.apache.flink.statefun.sdk.io.IngressIdentifier;
import org.apache.flink.statefun.sdk.io.IngressSpec;
import org.apache.flink.statefun.sdk.kafka.KafkaIngressBuilder;
import org.apache.flink.statefun.sdk.kafka.KafkaIngressDeserializer;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class IO {
  public static final IngressIdentifier<String> INGRESS_ID =
      new IngressIdentifier<>(String.class, "eft", "string-in");
  public static final IngressIdentifier<Tracklet> TRACKLET_INGRESS_ID =
      new IngressIdentifier<>(Tracklet.class, "eft", "string-in");
  public static final EgressIdentifier<KeyedOrbit> EGRESS_ID =
      new EgressIdentifier<>("eft", "keyed-orbit-out", KeyedOrbit.class);
  public static final EgressIdentifier<String> STRING_EGRESS_ID =
      new EgressIdentifier<>("eft", "string-out", String.class);
  private final String kafkaAddress;

  public IO(String kafkaAddress) {
    this.kafkaAddress = (String) Objects.requireNonNull(kafkaAddress);
  }

  public IngressSpec<String> getIngressSpec() {
    return KafkaIngressBuilder.forIdentifier(INGRESS_ID)
        .withKafkaAddress(this.kafkaAddress)
        .withTopic("tracklets")
        .withDeserializer(IO.KafkaDeserializer.class)
        .withProperty("group.id", "eft-for-ssa")
        .build();
  }

  private static final class KafkaDeserializer implements KafkaIngressDeserializer<String> {
    private static final long serialVersionUID = 1L;

    private KafkaDeserializer() {}

    public String deserialize(ConsumerRecord<byte[], byte[]> input) {
      String string = new String((byte[]) input.value(), StandardCharsets.UTF_8);
      return string;
    }
  }
}
