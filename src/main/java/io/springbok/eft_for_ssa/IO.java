package io.springbok.eft_for_ssa;

import org.apache.flink.statefun.flink.io.datastream.SinkFunctionSpec;
import org.apache.flink.statefun.flink.io.datastream.SourceFunctionSpec;
import org.apache.flink.statefun.sdk.io.EgressIdentifier;
import org.apache.flink.statefun.sdk.io.EgressSpec;
import org.apache.flink.statefun.sdk.io.IngressIdentifier;
import org.apache.flink.statefun.sdk.io.IngressSpec;
import org.apache.flink.statefun.sdk.spi.StatefulFunctionModule;
import org.apache.flink.streaming.api.functions.sink.PrintSinkFunction;

import java.util.Map;

public class IO implements StatefulFunctionModule
{
	static final IngressIdentifier<Tracklet> INGRESS_ID = new IngressIdentifier<>(Tracklet.class, "eft", "tracklet");
	static final EgressIdentifier<KeyedOrbit> EGRESS_ID = new EgressIdentifier<>("eft", "KeyedOrbit", KeyedOrbit.class);

	@Override
	public void configure(Map<String, String> globalConfiguration, Binder binder) {

		// Ingress
		IngressSpec<Tracklet> ingressSpec = new SourceFunctionSpec<>(INGRESS_ID, new TrackletSource().buildAndGetFileMonitor());

		binder.bindIngress(ingressSpec);
		binder.bindIngressRouter(INGRESS_ID, new TrackletRouter());

		// Egress
		EgressSpec<KeyedOrbit> egressSpec = new SinkFunctionSpec<>(EGRESS_ID, new PrintSinkFunction<>());
		binder.bindEgress(egressSpec);
	}
}