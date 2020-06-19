package io.springbok.eft_for_ssa;

import junit.framework.TestCase;
import junit.framework.TestResult;
import org.apache.flink.statefun.flink.harness.Harness;
import org.apache.flink.statefun.flink.harness.io.SerializableSupplier;
import org.junit.Test;

import java.io.*;

/**
 * Unit test for simple App.
 */
public class AppTest extends TestCase {

	private File file;

	@Override
	protected void setUp() throws Exception {
		String input = "output/2020-05-25_tracklet_messages.txt";
		File file = new File(input);    //creates a new file instance
	}

	@Test
	public TestResult run() {
		Harness harness =
				null;
		try {
			harness = new Harness()
					.withKryoMessageSerializer()
					.withSupplyingIngress(IO.INGRESS_ID, new TrackletStringGenerator(file))
					.withPrintingEgress(IO.STRING_EGRESS_ID);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		try {
			harness.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	protected void tearDown() throws Exception {
		file = null;
		assertNull(file);
	}


	private static final class TrackletStringGenerator
			implements SerializableSupplier<String> {

		private static final long serialVersionUID = 1;
		File file;
		FileReader fr;
		BufferedReader br;

		TrackletStringGenerator(File file) throws FileNotFoundException {
			this.file = file;
			FileReader fr = new FileReader(file);   //reads the file
			BufferedReader br = new BufferedReader(fr);  //creates a buffering character input stream


		}


		@Override
		public String get() {
			try {
				Thread.sleep(1_000);
			} catch (InterruptedException e) {
				throw new RuntimeException("Interrupted", e);
			}

			String response = "End of document";
			try {
				response = br.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return response;
		}
	}
}
