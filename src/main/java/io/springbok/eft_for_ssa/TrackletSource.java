package io.springbok.eft_for_ssa;

import org.apache.flink.streaming.api.functions.source.ContinuousFileMonitoringFunction;
import org.apache.flink.streaming.api.functions.source.FileProcessingMode;

public class TrackletSource {

	static int parellelism = 4;
	// Not used in PROCESS_ONCE mode
	static long interval = 60;
	static String inputPath = "output/2020-05-25_tracklet_messages.txt";
	static TrackletFormatter formatter = new TrackletFormatter(inputPath);

	ContinuousFileMonitoringFunction buildAndGetFileMonitor(){

		ContinuousFileMonitoringFunction fileMonitior =
				new ContinuousFileMonitoringFunction(formatter, FileProcessingMode.PROCESS_ONCE, parellelism, interval);

		return fileMonitior;
	}

}
