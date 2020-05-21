package io.springbok.eft_for_ssa;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public class TrackletReader {

	static String inputPath = "output/2020-05-21_tracklet_messages.txt";
	static TLEInputFormat formatter = new TLEInputFormat(inputPath);
	
	public static void main(final String[] args) throws Exception {

	final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
	
	DataStream<String> input = env.readTextFile(inputPath);
	
	input.print();
	
	env.execute();
		
	}
	
//	private static DataStream<Integer> toInt(DataStream<String> input) {
//		DataStream<Integer> num = input.map(new MapFunction<String, Integer>() {
//			
//			public Integer map(String value) {
//				return Integer.parseInt(value);
//			}
//		});	
//	}

}
