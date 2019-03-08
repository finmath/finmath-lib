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

import net.finmath.montecarlo.AbstractRandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
import net.finmath.time.TimeDiscretization;

/**
 * @author Christian Fries
 * @version 1.0
 */
public class LIBORVolatilityModelPiecewiseConstant extends LIBORVolatilityModel {

	private static final long serialVersionUID = 3258093488453501312L;

	private final AbstractRandomVariableFactory	randomVariableFactory;

	private final TimeDiscretization	simulationTimeDiscretization;
	private final TimeDiscretization	timeToMaturityDiscretization;

	private Map<Integer, Map<Integer, Integer>> 	indexMap = new ConcurrentHashMap<>();
	private RandomVariable[] volatility;
	private final	boolean		isCalibrateable;

	public LIBORVolatilityModelPiecewiseConstant(TimeDiscretization timeDiscretization, TimeDiscretization liborPeriodDiscretization, TimeDiscretization simulationTimeDiscretization, TimeDiscretization timeToMaturityDiscretization, RandomVariable[] volatility, boolean isCalibrateable) {
		super(timeDiscretization, liborPeriodDiscretization);

		this.randomVariableFactory = new RandomVariableFactory();

		/*
		 * Build index map
		 */
		double maxMaturity = liborPeriodDiscretization.getTime(liborPeriodDiscretization.getNumberOfTimes()-1);
		int volatilityIndex = 0;
		for(int simulationTime=0; simulationTime<simulationTimeDiscretization.getNumberOfTimes(); simulationTime++) {
			HashMap<Integer, Integer> timeToMaturityIndexing = new HashMap<>();
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

	public LIBORVolatilityModelPiecewiseConstant(AbstractRandomVariableFactory randomVariableFactory, TimeDiscretization timeDiscretization, TimeDiscretization liborPeriodDiscretization, TimeDiscretization simulationTimeDiscretization, TimeDiscretization timeToMaturityDiscretization, double[][] volatility, boolean isCalibrateable) {
		super(timeDiscretization, liborPeriodDiscretization);

		this.randomVariableFactory = randomVariableFactory;

		/*
		 * Build index map
		 */
		double maxMaturity = liborPeriodDiscretization.getTime(liborPeriodDiscretization.getNumberOfTimes()-1);
		int volatilityIndex = 0;
		for(int simulationTime=0; simulationTime<simulationTimeDiscretization.getNumberOfTimes(); simulationTime++) {
			Map<Integer, Integer> timeToMaturityIndexing = new ConcurrentHashMap<>();
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
		for(Integer simulationTime : indexMap.keySet()) {
			for(Integer timeToMaturity : indexMap.get(simulationTime).keySet()) {
				this.volatility[indexMap.get(simulationTime).get(timeToMaturity)] = randomVariableFactory.createRandomVariable(volatility[simulationTime][timeToMaturity]);
			}
		}

		this.simulationTimeDiscretization = simulationTimeDiscretization;
		this.timeToMaturityDiscretization = timeToMaturityDiscretization;
		this.isCalibrateable = isCalibrateable;
	}

	public LIBORVolatilityModelPiecewiseConstant(AbstractRandomVariableFactory randomVariableFactory, TimeDiscretization timeDiscretization, TimeDiscretization liborPeriodDiscretization, TimeDiscretization simulationTimeDiscretization, TimeDiscretization timeToMaturityDiscretization, double[] volatility, boolean isCalibrateable) {
		super(timeDiscretization, liborPeriodDiscretization);

		this.randomVariableFactory = randomVariableFactory;

		/*
		 * Build index map
		 */
		double maxMaturity = liborPeriodDiscretization.getTime(liborPeriodDiscretization.getNumberOfTimes()-1);
		int volatilityIndex = 0;
		for(int simulationTime=0; simulationTime<simulationTimeDiscretization.getNumberOfTimes(); simulationTime++) {
			HashMap<Integer, Integer> timeToMaturityIndexing = new HashMap<>();
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
			for(int i=0; i<volatility.length; i++) this.volatility[i] = randomVariableFactory.createRandomVariable(volatility[i]);
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

	public LIBORVolatilityModelPiecewiseConstant(TimeDiscretization timeDiscretization, TimeDiscretization liborPeriodDiscretization, TimeDiscretization simulationTimeDiscretization, TimeDiscretization timeToMaturityDiscretization, double[] volatility, boolean isCalibrateable) {
		this(new RandomVariableFactory(), timeDiscretization, liborPeriodDiscretization, simulationTimeDiscretization, timeToMaturityDiscretization, volatility, isCalibrateable);
	}

	public LIBORVolatilityModelPiecewiseConstant(TimeDiscretization timeDiscretization, TimeDiscretization liborPeriodDiscretization, TimeDiscretization simulationTimeDiscretization, TimeDiscretization timeToMaturityDiscretization, double volatility, boolean isCalibrateable) {
		this(timeDiscretization, liborPeriodDiscretization, simulationTimeDiscretization, timeToMaturityDiscretization, new double[] { volatility }, isCalibrateable);
	}

	public LIBORVolatilityModelPiecewiseConstant(TimeDiscretization timeDiscretization, TimeDiscretization liborPeriodDiscretization, TimeDiscretization simulationTimeDiscretization, TimeDiscretization timeToMaturityDiscretization, double[] volatility) {
		this(timeDiscretization, liborPeriodDiscretization, simulationTimeDiscretization, timeToMaturityDiscretization, volatility, true);
	}

	public LIBORVolatilityModelPiecewiseConstant(TimeDiscretization timeDiscretization, TimeDiscretization liborPeriodDiscretization, TimeDiscretization simulationTimeDiscretization, TimeDiscretization timeToMaturityDiscretization, double volatility) {
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
	public LIBORVolatilityModel getCloneWithModifiedParameter(RandomVariable[] parameter) {
		return new LIBORVolatilityModelPiecewiseConstant(
				super.getTimeDiscretization(),
				super.getLiborPeriodDiscretization(),
				this.simulationTimeDiscretization,
				this.timeToMaturityDiscretization,
				parameter,
				this.isCalibrateable
				);
	}

	@Override
	public RandomVariable getVolatility(int timeIndex, int liborIndex) {
		// Create a very simple volatility model here
		double time             = getTimeDiscretization().getTime(timeIndex);
		double maturity         = getLiborPeriodDiscretization().getTime(liborIndex);
		double timeToMaturity   = maturity-time;

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

			return volatility[indexMap.get(timeIndexSimulationTime).get(timeIndexTimeToMaturity)];
		}
	}

	@Override
	public Object clone() {
		return new LIBORVolatilityModelPiecewiseConstant(
				super.getTimeDiscretization(),
				super.getLiborPeriodDiscretization(),
				this.simulationTimeDiscretization,
				this.timeToMaturityDiscretization,
				this.volatility.clone(),
				this.isCalibrateable
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
	public LIBORVolatilityModel getCloneWithModifiedData(Map<String, Object> dataModified) {
		AbstractRandomVariableFactory randomVariableFactory = this.randomVariableFactory;
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
			randomVariableFactory = (AbstractRandomVariableFactory)dataModified.getOrDefault("randomVariableFactory", randomVariableFactory);
			timeDiscretization = (TimeDiscretization)dataModified.getOrDefault("timeDiscretization", timeDiscretization);
			liborPeriodDiscretization = (TimeDiscretization)dataModified.getOrDefault("liborPeriodDiscretization", liborPeriodDiscretization);
			simulationTimeDiscretization = (TimeDiscretization)dataModified.getOrDefault("simulationTimeDiscretization", simulationTimeDiscretization);
			timeToMaturityDiscretization = (TimeDiscretization)dataModified.getOrDefault("timeToMaturityDiscretization", timeToMaturityDiscretization);

			
			if(dataModified.getOrDefault("volatility", volatility) instanceof double[][]) {
				volatility = (double[][])dataModified.getOrDefault("volatility", volatility);
			}else {
				// TODO Implement handling for double[], double, RV[], RV
			}
		}
		
		LIBORVolatilityModel newModel = new LIBORVolatilityModelPiecewiseConstant(randomVariableFactory, timeDiscretization, liborPeriodDiscretization, simulationTimeDiscretization, timeToMaturityDiscretization, volatility, isCalibrateable);
		return newModel;
	}

}
