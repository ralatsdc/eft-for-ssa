package io.springbok.eft_for_ssa;

import java.net.URL;
import java.util.ArrayList;

import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.metrics.util.SystemResourcesCounter;
import org.apache.flink.streaming.api.TimerService;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.util.Collector;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.optim.nonlinear.vector.leastsquares.GaussNewtonOptimizer;
import org.orekit.data.NetworkCrawler;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.estimation.iod.IodLambert;
import org.orekit.estimation.leastsquares.BatchLSEstimator;
import org.orekit.estimation.measurements.Position;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.EulerIntegratorBuilder;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.propagation.integration.AbstractIntegratedPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;

public class TrackletReader {


	static String inputPath = "output/2020-05-25_tracklet_messages.txt";
	static TrackletFormatter formatter = new TrackletFormatter(inputPath);
	
	// Gravitation coefficient
	final static double mu = Constants.IERS2010_EARTH_MU;
	
	// Time Scale
	final static TimeScale utc = TimeScalesFactory.getUTC();

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
	
	DataStream<Tracklet> tracklets = env.readFile(formatter, inputPath)
			.map(new Counter());
	
	DataStream<Tracklet> filteredTracklets = tracklets
			.filter(new LowPositionFilter());
	
	DataStream<Orbit> orbits = filteredTracklets
			.map(new CreateOrbit())
			.keyBy(orbit -> orbit.getDate())
			.process(new OrbitTimeout());
	
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
			
			Orbit orbit;
			
            // Orbit Determination           
            final IodLambert lambert = new IodLambert(mu);
            // TODO: Posigrade and number of revolutions are set as guesses for now, but will need to be calculated later
            final boolean posigrade = true;
            final int nRev = 0;
            final Vector3D initialPosition = positions.get(0).getPosition();
            final AbsoluteDate initialDate = positions.get(0).getDate();
            final Vector3D finalPosition = positions.get(positions.size() - 1).getPosition();
            final AbsoluteDate finalDate = positions.get(positions.size() - 1).getDate();
            final Orbit orbitEstimation = lambert.estimate(inertialFrame, posigrade, nRev, initialPosition, initialDate, finalPosition, finalDate);
            
            if (positions.size() > 2) {
            	
				// Least squares estimator setup
				final GaussNewtonOptimizer GNOptimizer = new GaussNewtonOptimizer();
				final EulerIntegratorBuilder eulerBuilder = new EulerIntegratorBuilder(60);
				final double positionScale = 1.;
				final NumericalPropagatorBuilder propBuilder = new NumericalPropagatorBuilder(orbitEstimation, eulerBuilder, PositionAngle.MEAN, positionScale);
				final BatchLSEstimator leastSquares = new BatchLSEstimator(GNOptimizer, propBuilder);            
				leastSquares.setMaxIterations(1000);
				leastSquares.setMaxEvaluations(1000);
				leastSquares.setParametersConvergenceThreshold(.001);
				// Add measurements
				positions.forEach(measurement->leastSquares.addMeasurement(measurement));
				
				// Run least squares fit            
				AbstractIntegratedPropagator[] lsPropagators = leastSquares.estimate();
				orbit = lsPropagators[0].getInitialState().getOrbit();

            } else {
            	orbit = orbitEstimation;
            }
			
			return orbit;
		}
	}

	public static class OrbitTimeout extends KeyedProcessFunction<AbsoluteDate, Orbit, Orbit> {
		
		private ValueState<Orbit> orbitState;

		@Override
		public void open(Configuration config) {
			ValueStateDescriptor<Orbit> orbitDescriptor = 
					new ValueStateDescriptor<>("saved orbit", Orbit.class);
			orbitState = getRuntimeContext().getState(orbitDescriptor);
		}

		@Override
		public void processElement(Orbit orbit, Context context, Collector<Orbit> out) throws Exception {
			TimerService timerService = context.timerService();
			
			orbitState.update(orbit);
			
			// State (Orbit) timeout in milliseconds
			double timeout = (120 * 60 * 1000);
			timerService.registerEventTimeTimer(orbit.getDate().shiftedBy(timeout).toDate(utc).getTime());
		}

		@Override
		public void onTimer(long timestamp, OnTimerContext context, Collector<Orbit> out) throws Exception {
			orbitState.clear();
		}
	}

	private static class Counter<T> extends RichMapFunction<T, T> {
		private transient org.apache.flink.metrics.Counter counter;
		
		
		public void open(Configuration config) {
			this.counter = getRuntimeContext()
					.getMetricGroup()
					.counter("trackletCounter");
		}

		@Override
		public T map(T value) throws Exception {
			this.counter.inc();
//			System.out.println(this.counter.getCount());
			return value;
		}
	}
}
