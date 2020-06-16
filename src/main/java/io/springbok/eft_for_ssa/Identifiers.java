package io.springbok.eft_for_ssa;

import org.apache.flink.statefun.sdk.io.EgressIdentifier;
import org.apache.flink.statefun.sdk.io.IngressIdentifier;

public class Identifiers {
	public static final IngressIdentifier<Tracklet> INGRESS_ID = new IngressIdentifier<>(Tracklet.class, "eft", "in");
	public static final EgressIdentifier<KeyedOrbit> EGRESS_ID = new EgressIdentifier<>("eft", "keyed-orbit-out", KeyedOrbit.class);
	public static final EgressIdentifier<String> STRING_EGRESS_ID = new EgressIdentifier<>("eft", "string-out", String.class);
}
