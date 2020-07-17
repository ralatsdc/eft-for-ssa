package io.springbok.statefun.examples.demonstration;

import com.google.auto.service.AutoService;
import org.apache.flink.statefun.sdk.spi.StatefulFunctionModule;

import java.util.Map;

@AutoService(StatefulFunctionModule.class)
public class DemonstrationModule implements StatefulFunctionModule {

  private static final String KAFKA_KEY = "kafka-address";

  private static final String DEFAULT_KAFKA_ADDRESS = "kbroker:9093";

  @Override
  public void configure(Map<String, String> globalConfiguration, Binder binder) {

    // Ingress
    String kafkaAddress =
        (String) globalConfiguration.getOrDefault(KAFKA_KEY, DEFAULT_KAFKA_ADDRESS);
    DemonstrationIO ioModule = new DemonstrationIO(kafkaAddress);

    // bind ingress and router
    binder.bindIngress(ioModule.getIngressSpec());
    binder.bindIngressRouter(DemonstrationIO.TRACKS_INGRESS_ID, new TrackRouter());

    // Egress
    //    EgressSpec<String> printEgressSpec =
    //        new SinkFunctionSpec<>(IO.DEFAULT_EGRESS_ID, new PrintSinkFunction<>());
    //    binder.bindEgress(printEgressSpec);
    binder.bindEgress(ioModule.getEgressSpec());

    // Functions
    binder.bindFunctionProvider(TrackIdManager.TYPE, unused -> new TrackIdManager());
    binder.bindFunctionProvider(OrbitStatefulFunction.TYPE, unused -> new OrbitStatefulFunction());
    binder.bindFunctionProvider(TrackStatefulFunction.TYPE, unused -> new TrackStatefulFunction());
    binder.bindFunctionProvider(OrbitIdManager.TYPE, unused -> new OrbitIdManager());
  }
}
