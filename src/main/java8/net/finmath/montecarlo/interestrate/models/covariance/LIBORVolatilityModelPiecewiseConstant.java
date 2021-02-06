/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 08.08.2005
 */
package net.finmath.montecarlo.interestrate.models.covariance;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFromArrayFactory;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
import net.finmath.time.TimeDiscretization;

/**
 * @author Christian Fries
 * @version 1.0
 */
public class LIBORVolatilityModelPiecewiseConstant extends LIBORVolatilityModel {

	private static final long serialVersionUID = 3258093488453501312L;

	private final RandomVariableFactory	randomVariableFactory;

	private final TimeDiscretization	simulationTimeDiscretization;
	private final TimeDiscretization	timeToMaturityDiscretization;

	private final Map<Integer, Map<Integer, Integer>> 	indexMap = new ConcurrentHashMap<>();
	private RandomVariable[] volatility;
	private final	boolean		isCalibrateable;

	public LIBORVolatilityModelPiecewiseConstant(final TimeDiscretization timeDiscretization, final TimeDiscretization liborPeriodDiscretization, final TimeDiscretization simulationTimeDiscretization, final TimeDiscretization timeToMaturityDiscretization, final RandomVariable[] volatility, final boolean isCalibrateable) {
		super(timeDiscretization, liborPeriodDiscretization);

		randomVariableFactory = new RandomVariableFromArrayFactory();

		/*
		 * Build index map
		 */
		final double maxMaturity = liborPeriodDiscretization.getTime(liborPeriodDiscretization.getNumberOfTimes()-1);
		int volatilityIndex = 0;
		for(int simulationTime=0; simulationTime<simulationTimeDiscretization.getNumberOfTimes(); simulationTime++) {
			final HashMap<Integer, Integer> timeToMaturityIndexing = new HashMap<>();
			for(int timeToMaturity=0; timeToMaturity<timeToMaturityDiscretization.getNumberOfTimes(); timeToMaturity++) {
				if(simulationTimeDiscretization.getTime(simulationTime)+timeToMaturityDiscretization.getTime(timeToMaturity) > maxMaturity) {
					continue;
				}

				timeToMaturityIndexing.put(timeToMaturity,volatilityIndex++);
			}
			indexMap.put(simulationTime, timeToMaturityIndexing);
		}

		if(volatility.length == 1) {
			this.volatility = new RandomVariable[volatilityIndex];
			Arrays.fill(this.volatility, volatility[0]);
		}
		else if(volatility.length == volatilityIndex) {
			this.volatility = volatility.clone();
		}
		else {
			throw new IllegalArgumentException("Volatility length does not match number of free parameters.");
		}

		if(volatilityIndex != this.volatility.length) {
			throw new IllegalArgumentException("volatility.length should equal simulationTimeDiscretization.getNumberOfTimes()*timeToMaturityDiscretization.getNumberOfTimes().");
		}
		this.simulationTimeDiscretization = simulationTimeDiscretization;
		this.timeToMaturityDiscretization = timeToMaturityDiscretization;
		this.isCalibrateable = isCalibrateable;

	}

	public LIBORVolatilityModelPiecewiseConstant(final RandomVariableFactory randomVariableFactory, final TimeDiscretization timeDiscretization, final TimeDiscretization liborPeriodDiscretization, final TimeDiscretization simulationTimeDiscretization, final TimeDiscretization timeToMaturityDiscretization, final double[][] volatility, final boolean isCalibrateable) {
		super(timeDiscretization, liborPeriodDiscretization);

		this.randomVariableFactory = randomVariableFactory;

		/*
		 * Build index map
		 */
		final double maxMaturity = liborPeriodDiscretization.getTime(liborPeriodDiscretization.getNumberOfTimes()-1);
		int volatilityIndex = 0;
		for(int simulationTime=0; simulationTime<simulationTimeDiscretization.getNumberOfTimes(); simulationTime++) {
			final Map<Integer, Integer> timeToMaturityIndexing = new ConcurrentHashMap<>();
			for(int timeToMaturity=0; timeToMaturity<timeToMaturityDiscretization.getNumberOfTimes(); timeToMaturity++) {
				if(simulationTimeDiscretization.getTime(simulationTime)+timeToMaturityDiscretization.getTime(timeToMaturity) > maxMaturity) {
					continue;
				}

				timeToMaturityIndexing.put(timeToMaturity,volatilityIndex++);
			}
			indexMap.put(simulationTime, timeToMaturityIndexing);
		}

		// Flatten parameter matrix
		this.volatility = new RandomVariable[volatilityIndex];
		for(final Integer simulationTime : indexMap.keySet()) {
			for(final Integer timeToMaturity : indexMap.get(simulationTime).keySet()) {
				this.volatility[indexMap.get(simulationTime).get(timeToMaturity)] = randomVariableFactory.createRandomVariable(volatility[simulationTime][timeToMaturity]);
			}
		}

		this.simulationTimeDiscretization = simulationTimeDiscretization;
		this.timeToMaturityDiscretization = timeToMaturityDiscretization;
		this.isCalibrateable = isCalibrateable;
	}

	public LIBORVolatilityModelPiecewiseConstant(final RandomVariableFactory randomVariableFactory, final TimeDiscretization timeDiscretization, final TimeDiscretization liborPeriodDiscretization, final TimeDiscretization simulationTimeDiscretization, final TimeDiscretization timeToMaturityDiscretization, final double[] volatility, final boolean isCalibrateable) {
		super(timeDiscretization, liborPeriodDiscretization);

		this.randomVariableFactory = randomVariableFactory;

		/*
		 * Build index map
		 */
		final double maxMaturity = liborPeriodDiscretization.getTime(liborPeriodDiscretization.getNumberOfTimes()-1);
		int volatilityIndex = 0;
		for(int simulationTime=0; simulationTime<simulationTimeDiscretization.getNumberOfTimes(); simulationTime++) {
			final HashMap<Integer, Integer> timeToMaturityIndexing = new HashMap<>();
			for(int timeToMaturity=0; timeToMaturity<timeToMaturityDiscretization.getNumberOfTimes(); timeToMaturity++) {
				if(simulationTimeDiscretization.getTime(simulationTime)+timeToMaturityDiscretization.getTime(timeToMaturity) > maxMaturity) {
					continue;
				}

				timeToMaturityIndexing.put(timeToMaturity,volatilityIndex++);
			}
			indexMap.put(simulationTime, timeToMaturityIndexing);
		}

		if(volatility.length == 1) {
			this.volatility = new RandomVariable[volatilityIndex];
			Arrays.fill(this.volatility, randomVariableFactory.createRandomVariable(volatility[0]));
		}
		else if(volatility.length == volatilityIndex) {
			this.volatility = new RandomVariable[volatilityIndex];
			for(int i=0; i<volatility.length; i++) {
				this.volatility[i] = randomVariableFactory.createRandomVariable(volatility[i]);
			}
		}
		else {
			throw new IllegalArgumentException("Volatility length does not match number of free parameters.");
		}

		if(volatilityIndex != this.volatility.length) {
			throw new IllegalArgumentException("volatility.length should equal simulationTimeDiscretization.getNumberOfTimes()*timeToMaturityDiscretization.getNumberOfTimes().");
		}
		this.simulationTimeDiscretization = simulationTimeDiscretization;
		this.timeToMaturityDiscretization = timeToMaturityDiscretization;
		this.isCalibrateable = isCalibrateable;
	}

	public LIBORVolatilityModelPiecewiseConstant(final TimeDiscretization timeDiscretization, final TimeDiscretization liborPeriodDiscretization, final TimeDiscretization simulationTimeDiscretization, final TimeDiscretization timeToMaturityDiscretization, final double[] volatility, final boolean isCalibrateable) {
		this(new RandomVariableFromArrayFactory(), timeDiscretization, liborPeriodDiscretization, simulationTimeDiscretization, timeToMaturityDiscretization, volatility, isCalibrateable);
	}

	public LIBORVolatilityModelPiecewiseConstant(final TimeDiscretization timeDiscretization, final TimeDiscretization liborPeriodDiscretization, final TimeDiscretization simulationTimeDiscretization, final TimeDiscretization timeToMaturityDiscretization, final double volatility, final boolean isCalibrateable) {
		this(timeDiscretization, liborPeriodDiscretization, simulationTimeDiscretization, timeToMaturityDiscretization, new double[] { volatility }, isCalibrateable);
	}

	public LIBORVolatilityModelPiecewiseConstant(final TimeDiscretization timeDiscretization, final TimeDiscretization liborPeriodDiscretization, final TimeDiscretization simulationTimeDiscretization, final TimeDiscretization timeToMaturityDiscretization, final double[] volatility) {
		this(timeDiscretization, liborPeriodDiscretization, simulationTimeDiscretization, timeToMaturityDiscretization, volatility, true);
	}

	public LIBORVolatilityModelPiecewiseConstant(final TimeDiscretization timeDiscretization, final TimeDiscretization liborPeriodDiscretization, final TimeDiscretization simulationTimeDiscretization, final TimeDiscretization timeToMaturityDiscretization, final double volatility) {
		this(timeDiscretization, liborPeriodDiscretization, simulationTimeDiscretization, timeToMaturityDiscretization, new double[] { volatility });
	}

	@Override
	public RandomVariable[] getParameter() {
		if(isCalibrateable) {
			return volatility;
		} else {
			return null;
		}
	}

	@Override
	public LIBORVolatilityModel getCloneWithModifiedParameter(final RandomVariable[] parameter) {
		return new LIBORVolatilityModelPiecewiseConstant(
				super.getTimeDiscretization(),
				super.getLiborPeriodDiscretization(),
				simulationTimeDiscretization,
				timeToMaturityDiscretization,
				parameter,
				isCalibrateable
				);
	}

	@Override
	public RandomVariable getVolatility(final int timeIndex, final int liborIndex) {
		// Create a very simple volatility model here
		final double time             = getTimeDiscretization().getTime(timeIndex);
		final double maturity         = getLiborPeriodDiscretization().getTime(liborIndex);
		final double timeToMaturity   = maturity-time;

		double volatilityInstanteaneous;
		if(timeToMaturity <= 0)
		{
			volatilityInstanteaneous = 0.0;   // This forward rate is already fixed, no volatility

			return randomVariableFactory == null ? new Scalar(0.0) : randomVariableFactory.createRandomVariable(time, volatilityInstanteaneous);
		}
		else
		{
			int timeIndexSimulationTime = simulationTimeDiscretization.getTimeIndexNearestLessOrEqual(time);
			if(timeIndexSimulationTime < 0) {
				timeIndexSimulationTime = 0;
			}
			if(timeIndexSimulationTime >= simulationTimeDiscretization.getNumberOfTimes()) {
				timeIndexSimulationTime--;
			}

			int timeIndexTimeToMaturity = timeToMaturityDiscretization.getTimeIndexNearestLessOrEqual(timeToMaturity);
			if(timeIndexTimeToMaturity < 0) {
				timeIndexTimeToMaturity = 0;
			}
			if(timeIndexTimeToMaturity >= timeToMaturityDiscretization.getNumberOfTimes()) {
				timeIndexTimeToMaturity--;
			}

			final int parameterIndex = indexMap.get(timeIndexSimulationTime).get(timeIndexTimeToMaturity);
			return volatility[parameterIndex];
		}
	}

	@Override
	public Object clone() {
		return new LIBORVolatilityModelPiecewiseConstant(
				super.getTimeDiscretization(),
				super.getLiborPeriodDiscretization(),
				simulationTimeDiscretization,
				timeToMaturityDiscretization,
				volatility.clone(),
				isCalibrateable
				);
	}


	/**
	 * @return the simulationTimeDiscretization
	 */
	public TimeDiscretization getSimulationTimeDiscretization() {
		return simulationTimeDiscretization;
	}

	/**
	 * @return the timeToMaturityDiscretization
	 */
	public TimeDiscretization getTimeToMaturityDiscretization() {
		return timeToMaturityDiscretization;
	}

	@Override
	public LIBORVolatilityModel getCloneWithModifiedData(final Map<String, Object> dataModified) {
		RandomVariableFactory randomVariableFactory = this.randomVariableFactory;
		TimeDiscretization timeDiscretization = this.getTimeDiscretization();
		TimeDiscretization liborPeriodDiscretization = this.getLiborPeriodDiscretization();
		TimeDiscretization simulationTimeDiscretization = this.getSimulationTimeDiscretization();
		TimeDiscretization timeToMaturityDiscretization = this.getTimeToMaturityDiscretization();
		double[][] volatility = new double[simulationTimeDiscretization.getNumberOfTimes()][timeToMaturityDiscretization.getNumberOfTimes()];
		for(int i = 0;i<volatility.length;i++) {
			for(int j = 0;j<volatility[i].length;j++) {
				volatility[i][j] = this.volatility[indexMap.get(i).get(j)].doubleValue();
			}
		}

		if(dataModified != null) {
			// Explicitly passed covarianceModel has priority
			randomVariableFactory = (RandomVariableFactory)dataModified.getOrDefault("randomVariableFactory", randomVariableFactory);
			timeDiscretization = (TimeDiscretization)dataModified.getOrDefault("timeDiscretization", timeDiscretization);
			liborPeriodDiscretization = (TimeDiscretization)dataModified.getOrDefault("liborPeriodDiscretization", liborPeriodDiscretization);
			simulationTimeDiscretization = (TimeDiscretization)dataModified.getOrDefault("simulationTimeDiscretization", simulationTimeDiscretization);
			timeToMaturityDiscretization = (TimeDiscretization)dataModified.getOrDefault("timeToMaturityDiscretization", timeToMaturityDiscretization);


			if(dataModified.getOrDefault("volatility", volatility) instanceof double[][]) {
				volatility = (double[][])dataModified.getOrDefault("volatility", volatility);
			}
			else {
				// TODO Implement handling for double[], double, RV[], RV
				throw new UnsupportedOperationException("volatility parameter type not supported.");
			}
		}

		final LIBORVolatilityModel newModel = new LIBORVolatilityModelPiecewiseConstant(randomVariableFactory, timeDiscretization, liborPeriodDiscretization, simulationTimeDiscretization, timeToMaturityDiscretization, volatility, isCalibrateable);
		return newModel;
	}

}
