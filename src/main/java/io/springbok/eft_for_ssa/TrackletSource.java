package io.springbok.eft_for_ssa;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.api.functions.source.InputFormatSourceFunction;

public class TrackletSource {

	static int parellelism = 4;
	// Not used in PROCESS_ONCE mode
	static long interval = 60;
	static String inputPath = "output/2020-05-25_tracklet_messages.txt";
	static TrackletFormatter formatter = new TrackletFormatter(inputPath);

	public static InputFormatSourceFunction getSource(){

		formatter.setFilePath(inputPath);

		InputFormatSourceFunction source = new InputFormatSourceFunction(formatter, TypeInformation.of(Tracklet.class));

		DataGeneratorSource datasource = new DataGeneratorSource();

		return source;
	}

}
