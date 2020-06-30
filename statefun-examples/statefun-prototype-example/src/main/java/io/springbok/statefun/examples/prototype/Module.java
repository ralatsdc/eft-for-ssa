package io.springbok.statefun.examples.prototype;

import com.google.auto.service.AutoService;
import org.apache.flink.statefun.flink.io.datastream.SinkFunctionSpec;
import org.apache.flink.statefun.sdk.io.EgressSpec;
import org.apache.flink.statefun.sdk.spi.StatefulFunctionModule;
import org.apache.flink.streaming.api.functions.sink.PrintSinkFunction;

import java.util.Map;

@AutoService(StatefulFunctionModule.class)
public class Module implements StatefulFunctionModule {

  @Override
  public void configure(Map<String, String> globalConfiguration, Binder binder) {

    // Declare functions
    binder.bindFunctionProvider(OrbitStatefulFunction.TYPE, unused -> new OrbitStatefulFunction());
    binder.bindFunctionProvider(
        TrackletStatefulFunction.TYPE, unused -> new TrackletStatefulFunction());
    binder.bindFunctionProvider(
        OrbitIdStatefulFunction.TYPE, unused -> new OrbitIdStatefulFunction());
    binder.bindFunctionProvider(
        TrackletStatefulBuilder.TYPE, unused -> new TrackletStatefulBuilder());

    // Ingress
    String kafkaAddress =
        (String) globalConfiguration.getOrDefault("kafka-address", "kafka-broker:9092");
    IO ioModule = new IO(kafkaAddress);
    binder.bindIngress(ioModule.getIngressSpec());

    binder.bindIngressRouter(IO.INGRESS_ID, new LineRouter());

    // Egress
    EgressSpec<KeyedOrbit> egressSpec =
        new SinkFunctionSpec<>(IO.EGRESS_ID, new PrintSinkFunction<>());
    binder.bindEgress(egressSpec);
  }
}
