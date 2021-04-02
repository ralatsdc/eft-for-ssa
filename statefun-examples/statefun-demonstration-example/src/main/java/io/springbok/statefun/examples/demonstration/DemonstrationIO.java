package io.springbok.statefun.examples.demonstration;

import io.springbok.statefun.examples.demonstration.generated.DefaultOut;
import io.springbok.statefun.examples.demonstration.generated.SensorIn;
import io.springbok.statefun.examples.demonstration.generated.SingleLineTLE;
import io.springbok.statefun.examples.demonstration.generated.TrackIn;
import org.apache.flink.statefun.sdk.io.EgressIdentifier;
import org.apache.flink.statefun.sdk.io.EgressSpec;
import org.apache.flink.statefun.sdk.io.IngressIdentifier;
import org.apache.flink.statefun.sdk.io.IngressSpec;
import org.apache.flink.statefun.sdk.kafka.*;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;

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
  public static final IngressIdentifier<SingleLineTLE> TLE_INGRESS_ID =
      new IngressIdentifier<>(SingleLineTLE.class, "eft-for-ssa", "tle-in");

  // Setting Sensor ingress identifier
  public static final IngressIdentifier<SensorIn> SENSOR_INGRESS_ID =
      new IngressIdentifier<>(SensorIn.class, "eft-for-ssa", "sensor-in");

  // Setting egress identifier
  public static final EgressIdentifier<DefaultOut> DEFAULT_EGRESS_ID =
      new EgressIdentifier<>("eft-for-ssa", "default-out", DefaultOut.class);

  // Simple constructor
  public DemonstrationIO(String kafkaAddress) {
    this.kafkaAddress = Objects.requireNonNull(kafkaAddress);
  }

  // Build and return tracks ingress spec
  public IngressSpec<TrackIn> getIngressSpec() {
    return KafkaIngressBuilder.forIdentifier(TRACKS_INGRESS_ID)
        .withKafkaAddress(this.kafkaAddress)
        .withTopic("tracks")
        .withDeserializer(KafkaTracksDeserializer.class)
        .withProperty(ConsumerConfig.GROUP_ID_CONFIG, "eft-for-ssa")
        .build();
  }

  // Build and return TLE ingress spec
  public KafkaIngressSpec<SingleLineTLE> getTLEIngressSpec() {
    return KafkaIngressBuilder.forIdentifier(TLE_INGRESS_ID)
        .withKafkaAddress(this.kafkaAddress)
        .withTopic("tles")
        .withDeserializer(KafkaTLEDeserializer.class)
        .withProperty(ConsumerConfig.GROUP_ID_CONFIG, "eft-for-ssa")
        .build();
  }

  // Build and return TLE ingress spec
  public KafkaIngressSpec<SensorIn> getSensorIngressSpec() {
    return KafkaIngressBuilder.forIdentifier(SENSOR_INGRESS_ID)
        .withKafkaAddress(this.kafkaAddress)
        .withTopic("sensors")
        .withDeserializer(KafkaSensorDeserializer.class)
        .withProperty(ConsumerConfig.GROUP_ID_CONFIG, "eft-for-ssa")
        .build();
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

  // Simple byte deserializer for the ingress
  private static final class KafkaTLEDeserializer
      implements KafkaIngressDeserializer<SingleLineTLE> {

    @Override
    public SingleLineTLE deserialize(ConsumerRecord<byte[], byte[]> input) {
      String lines = new String((byte[]) input.value(), StandardCharsets.UTF_8);

      return SingleLineTLE.newBuilder().setLines(lines).build();
    }
  }

  // Simple byte deserializer for the ingress
  private static final class KafkaSensorDeserializer implements KafkaIngressDeserializer<SensorIn> {

    @Override
    public SensorIn deserialize(ConsumerRecord<byte[], byte[]> input) {
      String sensor = new String((byte[]) input.value(), StandardCharsets.UTF_8);

      return SensorIn.newBuilder().setSensor(sensor).build();
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
