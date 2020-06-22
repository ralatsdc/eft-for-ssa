package io.springbok.eft_for_ssa;

import java.util.ArrayList;

public class CollectedTrackletsMessage {

	ArrayList<Long> trackletIds;
	ArrayList<Tracklet> tracklets;
	Long keyedOrbitId;

	CollectedTrackletsMessage(KeyedOrbit orbit1, KeyedOrbit orbit2){
		this.keyedOrbitId = orbit1.getId();
		this.trackletIds = orbit2.getTrackletsId();
	}

	public void addTracklet(Tracklet tracklet){
		this.tracklets.add(tracklet);
	}

	public void removeTrackletId(Long id){
		this.trackletIds.remove(id);
	}

	public String getRoute(){
		return String.valueOf(trackletIds.get(0));
	}

	public ArrayList<Tracklet> getTracklets(){
		return this.tracklets;
	}

	public Long getOrbitId() {
		return this.keyedOrbitId;
	}

	public boolean emptyIdList() {
		if (trackletIds.size() == 0){
			return true;
		} else {
			return false;
		}
	}
}
