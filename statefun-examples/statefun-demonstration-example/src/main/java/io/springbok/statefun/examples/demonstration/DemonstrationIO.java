package io.springbok.statefun.examples.demonstration;

import io.springbok.statefun.examples.demonstration.generated.DefaultOut;
import io.springbok.statefun.examples.demonstration.generated.TrackIn;
import org.apache.flink.statefun.flink.io.datastream.SourceFunctionSpec;
import org.apache.flink.statefun.sdk.io.EgressIdentifier;
import org.apache.flink.statefun.sdk.io.EgressSpec;
import org.apache.flink.statefun.sdk.io.IngressIdentifier;
import org.apache.flink.statefun.sdk.io.IngressSpec;
import org.apache.flink.statefun.sdk.kafka.KafkaEgressBuilder;
import org.apache.flink.statefun.sdk.kafka.KafkaEgressSerializer;
import org.apache.flink.statefun.sdk.kafka.KafkaIngressBuilder;
import org.apache.flink.statefun.sdk.kafka.KafkaIngressDeserializer;
import org.apache.flink.streaming.api.functions.source.ContinuousFileMonitoringFunction;
import org.apache.flink.streaming.api.functions.source.FileProcessingMode;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.orekit.propagation.analytical.tle.TLE;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/*
   DemonstrationIO defines the application ingress and egress, and binds them to Kafka topics.
This class is used as part of the Demonstration Module
*/
public class DemonstrationIO {

  private final String kafkaAddress;

  // Setting track ingress identifier
  public static final IngressIdentifier<TrackIn> TRACKS_INGRESS_ID =
      new IngressIdentifier<>(TrackIn.class, "eft-for-ssa", "tracks-in");

  // Setting TLE ingress identifier
  public static final IngressIdentifier<TLE> TLE_INGRESS_ID =
      new IngressIdentifier<>(TLE.class, "eft-for-ssa", "tle-in");

  // Setting egress identifier
  public static final EgressIdentifier<DefaultOut> DEFAULT_EGRESS_ID =
      new EgressIdentifier<>("eft-for-ssa", "default-out", DefaultOut.class);

  // Simple constructor
  public DemonstrationIO(String kafkaAddress) {
    this.kafkaAddress = Objects.requireNonNull(kafkaAddress);
  }

  // Build and return ingress spec
  public IngressSpec<TrackIn> getIngressSpec() {
    return KafkaIngressBuilder.forIdentifier(TRACKS_INGRESS_ID)
        .withKafkaAddress(this.kafkaAddress)
        .withTopic("tracks")
        .withDeserializer(KafkaTracksDeserializer.class)
        .withProperty(ConsumerConfig.GROUP_ID_CONFIG, "eft-for-ssa")
        .build();
  }

  // Build and return file ingress
  public IngressSpec<TLE> getTLEIngressSpec(String TLEPath) {
    TLEFormatter formatter = new TLEFormatter(TLEPath);
    ContinuousFileMonitoringFunction fileMonitor =
        new ContinuousFileMonitoringFunction<TLE>(formatter, FileProcessingMode.PROCESS_ONCE, 1, 1);
    IngressSpec<TLE> TLESpec = new SourceFunctionSpec<TLE>(TLE_INGRESS_ID, fileMonitor);
    return TLESpec;
  }

  // Build and return egress spec
  EgressSpec<DefaultOut> getEgressSpec() {
    return KafkaEgressBuilder.forIdentifier(DEFAULT_EGRESS_ID)
        .withKafkaAddress(this.kafkaAddress)
        .withSerializer(KafkaTracksSerializer.class)
        .build();
  }

  // Simple byte deserializer for the ingress
  private static final class KafkaTracksDeserializer implements KafkaIngressDeserializer<TrackIn> {

    @Override
    public TrackIn deserialize(ConsumerRecord<byte[], byte[]> input) {
      String track = new String((byte[]) input.value(), StandardCharsets.UTF_8);

      return TrackIn.newBuilder().setTrack(track).build();
    }
  }

  // Simple byte serializer for the egress
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
