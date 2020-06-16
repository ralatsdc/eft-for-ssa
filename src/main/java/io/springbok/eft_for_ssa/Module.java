package io.springbok.eft_for_ssa;

import com.google.auto.service.AutoService;
import org.apache.flink.statefun.flink.io.datastream.SinkFunctionSpec;
import org.apache.flink.statefun.flink.io.datastream.SourceFunctionSpec;
import org.apache.flink.statefun.sdk.io.EgressSpec;
import org.apache.flink.statefun.sdk.io.IngressSpec;
import org.apache.flink.statefun.sdk.spi.StatefulFunctionModule;
import org.apache.flink.streaming.api.functions.sink.PrintSinkFunction;

import java.util.Map;

@AutoService(StatefulFunctionModule.class)
public class Module implements StatefulFunctionModule
{

	@Override
	public void configure(Map<String, String> globalConfiguration, Binder binder) {

		// Declare functions
		binder.bindFunctionProvider(OrbitStatefulFunction.TYPE, unused -> new OrbitStatefulFunction());
		binder.bindFunctionProvider(TrackletStatefulFunction.TYPE, unused -> new TrackletStatefulFunction());
		binder.bindFunctionProvider(OrbitIdStatefulFunction.TYPE, unused -> new OrbitIdStatefulFunction());

		// Ingress
		IngressSpec<Tracklet> ingressSpec = new SourceFunctionSpec<>(Identifiers.INGRESS_ID, TrackletSource.getSource());
		binder.bindIngress(ingressSpec);
		binder.bindIngressRouter(Identifiers.INGRESS_ID, new TrackletRouter());

		// Egress
		EgressSpec<KeyedOrbit> egressSpec = new SinkFunctionSpec<>(Identifiers.EGRESS_ID, new PrintSinkFunction<>());
		binder.bindEgress(egressSpec);
	}
}