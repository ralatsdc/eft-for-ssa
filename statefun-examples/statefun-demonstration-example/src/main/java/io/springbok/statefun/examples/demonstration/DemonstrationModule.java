package io.springbok.statefun.examples.demonstration;

import com.google.auto.service.AutoService;
import org.apache.flink.statefun.sdk.spi.StatefulFunctionModule;

import java.util.Map;

/*
  The Stateful Function Module is the entry point for the application.
  Here the ingress and egress are bound (detailed in the DemonstrationIO class),
  the router is bound, and the StatefulFunctions are bound to the application.
*/
@AutoService(StatefulFunctionModule.class)
public class DemonstrationModule implements StatefulFunctionModule {

  private static final String KAFKA_KEY = "kafka-address";

  private static final String DEFAULT_KAFKA_ADDRESS = "kafka-broker:9093";

  private static String TLEPATH =
      "/Users/williamspear/projects/orbits/eft-for-ssa/tle-data/globalstar_tles_05_18_2020.txt";

  @Override
  public void configure(Map<String, String> globalConfiguration, Binder binder) {

    //  IO created with Kafka Keys. These ports are opened in the docker-compose.yml
    String kafkaAddress =
        (String) globalConfiguration.getOrDefault(KAFKA_KEY, DEFAULT_KAFKA_ADDRESS);
    DemonstrationIO ioModule = new DemonstrationIO(kafkaAddress);

    // Bind the track ingress and router
    binder.bindIngress(ioModule.getIngressSpec());
    binder.bindIngressRouter(DemonstrationIO.TRACKS_INGRESS_ID, new TrackRouter());

    // Bind the tle ingress
    binder.bindIngress(ioModule.getTLEIngressSpec(TLEPATH));
    binder.bindIngressRouter(DemonstrationIO.TLE_INGRESS_ID, new TLERouter());

    // Bind application egress
    binder.bindEgress(ioModule.getEgressSpec());

    // Bind functions to the application
    binder.bindFunctionProvider(TrackIdManager.TYPE, unused -> new TrackIdManager());
    binder.bindFunctionProvider(OrbitStatefulFunction.TYPE, unused -> new OrbitStatefulFunction());
    binder.bindFunctionProvider(TrackStatefulFunction.TYPE, unused -> new TrackStatefulFunction());
    binder.bindFunctionProvider(OrbitIdManager.TYPE, unused -> new OrbitIdManager());
    binder.bindFunctionProvider(
        SatelliteStatefulFunction.TYPE, unused -> new SatelliteStatefulFunction());
  }
}
