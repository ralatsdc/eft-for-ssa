package io.springbok.eft_for_ssa;

import org.apache.flink.statefun.sdk.io.Router;

public class TrackletRouter implements Router<Tracklet> {

	@Override
	public void route(Tracklet message, Downstream<Tracklet> downstream) {
		downstream.forward(TrackletStatefulFunction.TYPE, String.valueOf(message.getId()), message);
	}
}
