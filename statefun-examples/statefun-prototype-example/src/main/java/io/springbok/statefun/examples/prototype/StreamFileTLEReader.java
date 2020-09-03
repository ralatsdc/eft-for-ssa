package io.springbok.statefun.examples.prototype;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.NetworkCrawler;
import org.orekit.propagation.analytical.tle.TLE;

import java.net.URL;

public class StreamFileTLEReader {

  static String inputPath = "tles/globalstar_tles_05_18_2020.txt";
  static TLEInputFormat formatter = new TLEInputFormat(inputPath);

  public static void main(final String[] args) throws Exception {

    // Configure Orekit
    final URL utcTaiData = new URL("https://hpiers.obspm.fr/eoppc/bul/bulc/UTC-TAI.history");
    final URL eopData = new URL("ftp://ftp.iers.org/products/eop/rapid/daily/finals.daily");
    final DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
    manager.addProvider(new NetworkCrawler(utcTaiData));
    manager.addProvider(new NetworkCrawler(eopData));

    final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

    DataStream<TLE> input = env.readFile(formatter, inputPath);

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
