package io.springbok.eft_for_ssa;

import java.net.URL;
import java.util.ArrayList;

import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.data.NetworkCrawler;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.estimation.iod.IodLambert;
import org.orekit.estimation.measurements.Position;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.Orbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

public class TrackletReader {


	static String inputPath = "output/2020-05-22_tracklet_messages.txt";
	static TrackletFormatter formatter = new TrackletFormatter(inputPath);
	
	// Gravitation coefficient
	final static double mu = Constants.IERS2010_EARTH_MU;

	// Inertial frame
	final static Frame inertialFrame = FramesFactory.getGCRF();
	
	public static void main(final String[] args) throws Exception {

	// Configure Orekit
	final URL utcTaiData = new URL("https://hpiers.obspm.fr/eoppc/bul/bulc/UTC-TAI.history");
	final URL eopData = new URL("ftp://ftp.iers.org/products/eop/rapid/daily/finals.daily"); 
	final DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
	manager.addProvider(new NetworkCrawler(utcTaiData));
	manager.addProvider(new NetworkCrawler(eopData));

	final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
	
	DataStream<Tracklet> tracklets = env.readFile(formatter, inputPath);
	
	DataStream<Tracklet> filteredTracklets = tracklets.filter(new LowPositionFilter());
	
	DataStream<Orbit> orbits = filteredTracklets.map(new CreateOrbit());
	
	orbits.print();
	
	env.execute();
		
	}

	private static class LowPositionFilter implements FilterFunction<Tracklet> {

		@Override
		public boolean filter(Tracklet tracklet) throws Exception {
			
			ArrayList<Position> positions = tracklet.getPositions();
			
			if (positions.size() > 1) {
				return true;
			}
			else {return false;}
		}
	}

	private static class CreateOrbit implements MapFunction<Tracklet, Orbit> {

		public Orbit map(Tracklet tracklet) throws Exception {
			
			ArrayList<Position> positions = tracklet.getPositions();
			
            // Orbit Determination           
            final IodLambert lambert = new IodLambert(mu);
            // TODO: Posigrade and number of revolutions are set as guesses for now, but will need to be calculated later
            final boolean posigrade = true;
            final int nRev = 0;
            final Vector3D initialPosition = positions.get(0).getPosition();
            final AbsoluteDate initialDate = positions.get(0).getDate();
            final Vector3D finalPosition = positions.get(positions.size() - 1).getPosition();
            final AbsoluteDate finalDate = positions.get(positions.size() - 1).getDate();
            final Orbit orbit = lambert.estimate(inertialFrame, posigrade, nRev, initialPosition, initialDate, finalPosition, finalDate);
			
			return orbit;
		}
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
