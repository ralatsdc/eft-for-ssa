package io.springbok.statefun.examples.demonstration;

import io.springbok.statefun.examples.demonstration.generated.DefaultOut;
import io.springbok.statefun.examples.demonstration.generated.TrackIn;
import org.apache.flink.statefun.sdk.io.EgressIdentifier;
import org.apache.flink.statefun.sdk.io.EgressSpec;
import org.apache.flink.statefun.sdk.io.IngressIdentifier;
import org.apache.flink.statefun.sdk.io.IngressSpec;
import org.apache.flink.statefun.sdk.kafka.KafkaEgressBuilder;
import org.apache.flink.statefun.sdk.kafka.KafkaEgressSerializer;
import org.apache.flink.statefun.sdk.kafka.KafkaIngressBuilder;
import org.apache.flink.statefun.sdk.kafka.KafkaIngressDeserializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class DemonstrationIO {

  public static final IngressIdentifier<TrackIn> TRACKS_INGRESS_ID =
      new IngressIdentifier<>(TrackIn.class, "eft-for-ssa", "tracks-in");
  //	public static final EgressIdentifier<KeyedOrbit> EGRESS_ID = new
  // EgressIdentifier<>("eft-for-ssa",
  // "keyed-orbit-out", KeyedOrbit.class);
  public static final EgressIdentifier<DefaultOut> DEFAULT_EGRESS_ID =
      new EgressIdentifier<>("eft-for-ssa", "default-out", DefaultOut.class);
  public static final EgressIdentifier<String> PRINT_EGRESS_ID =
      new EgressIdentifier<>("eft-for-ssa", "print-out", String.class);

  private final String kafkaAddress;

  public DemonstrationIO(String kafkaAddress) {
    this.kafkaAddress = Objects.requireNonNull(kafkaAddress);
  }

  public IngressSpec<TrackIn> getIngressSpec() {
    return KafkaIngressBuilder.forIdentifier(TRACKS_INGRESS_ID)
        .withKafkaAddress(this.kafkaAddress)
        .withTopic("tracks")
        .withDeserializer(KafkaTracksDeserializer.class)
        .withProperty(ConsumerConfig.GROUP_ID_CONFIG, "eft-for-ssa")
        .build();
  }

  EgressSpec<DefaultOut> getEgressSpec() {
    return KafkaEgressBuilder.forIdentifier(DEFAULT_EGRESS_ID)
        .withKafkaAddress(this.kafkaAddress)
        .withSerializer(KafkaTracksSerializer.class)
        .build();
  }

  private static final class KafkaTracksDeserializer implements KafkaIngressDeserializer<TrackIn> {
    //    private static final long serialVersionUID = 1L;

    @Override
    public TrackIn deserialize(ConsumerRecord<byte[], byte[]> input) {
      String track = new String((byte[]) input.value(), StandardCharsets.UTF_8);

      return TrackIn.newBuilder().setTrack(track).build();
    }
  }

  private static final class KafkaTracksSerializer implements KafkaEgressSerializer<DefaultOut> {

    private static final long serialVersionUID = 1L;

    @Override
    public ProducerRecord<byte[], byte[]> serialize(DefaultOut response) {
      // TODO: Serialize to real keys
      byte[] value = response.getContent().getBytes(StandardCharsets.UTF_8);

      return new ProducerRecord<>("default", value);
    }
  }
}
