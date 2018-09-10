/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 08.08.2005
 */
package net.finmath.montecarlo.interestrate.covariancemodels;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.finmath.montecarlo.RandomVariable;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * @author Christian Fries
 * @version
 */
public class LIBORVolatilityModelPiecewiseConstant extends LIBORVolatilityModel {

	private final TimeDiscretizationInterface	simulationTimeDiscretization;
	private final TimeDiscretizationInterface	timeToMaturityDiscretization;

	private Map<Integer, HashMap<Integer, Integer>> 	indexMap = new HashMap<Integer, HashMap<Integer, Integer>>();
	private RandomVariableInterface[] volatilities;
	private final	boolean		isCalibrateable;

	public LIBORVolatilityModelPiecewiseConstant(TimeDiscretizationInterface timeDiscretization, TimeDiscretizationInterface liborPeriodDiscretization, TimeDiscretizationInterface simulationTimeDiscretization, TimeDiscretizationInterface timeToMaturityDiscretization, RandomVariableInterface[][] volatility, boolean isCalibrateable) {
		super(timeDiscretization, liborPeriodDiscretization);

		/*
		 * Build index map
		 */
		double maxMaturity = timeToMaturityDiscretization.getTime(timeToMaturityDiscretization.getNumberOfTimes()-1);
		int volatilityIndex = 0;
		for(int simulationTime=0; simulationTime<simulationTimeDiscretization.getNumberOfTimes(); simulationTime++) {
			HashMap<Integer, Integer> timeToMaturityIndexing = new HashMap<Integer, Integer>();
			for(int timeToMaturity=0; timeToMaturity<timeToMaturityDiscretization.getNumberOfTimes(); timeToMaturity++) {
				if(simulationTimeDiscretization.getTime(simulationTime)+timeToMaturityDiscretization.getTime(timeToMaturity) > maxMaturity) {
					continue;
				}

				timeToMaturityIndexing.put(timeToMaturity,volatilityIndex++);
			}
			indexMap.put(simulationTime, timeToMaturityIndexing);
		}

		// Flatten parameter matrix
		this.volatilities = new RandomVariableInterface[volatilityIndex];
		for(Integer simulationTime : indexMap.keySet()) {
			for(Integer timeToMaturity : indexMap.get(simulationTime).keySet()) {
				this.volatilities[indexMap.get(simulationTime).get(timeToMaturity)] = volatility[simulationTime][timeToMaturity];
			}
		}

		this.simulationTimeDiscretization = simulationTimeDiscretization;
		this.timeToMaturityDiscretization = timeToMaturityDiscretization;
		this.isCalibrateable = isCalibrateable;
	}

	public LIBORVolatilityModelPiecewiseConstant(TimeDiscretizationInterface timeDiscretization, TimeDiscretizationInterface liborPeriodDiscretization, TimeDiscretizationInterface simulationTimeDiscretization, TimeDiscretizationInterface timeToMaturityDiscretization, RandomVariableInterface[] volatility, boolean isCalibrateable) {
		super(timeDiscretization, liborPeriodDiscretization);

		/*
		 * Build index map
		 */
		double maxMaturity = timeToMaturityDiscretization.getTime(timeToMaturityDiscretization.getNumberOfTimes()-1);
		int volatilityIndex = 0;
		for(int simulationTime=0; simulationTime<simulationTimeDiscretization.getNumberOfTimes(); simulationTime++) {
			HashMap<Integer, Integer> timeToMaturityIndexing = new HashMap<Integer, Integer>();
			for(int timeToMaturity=0; timeToMaturity<timeToMaturityDiscretization.getNumberOfTimes(); timeToMaturity++) {
				if(simulationTimeDiscretization.getTime(simulationTime)+timeToMaturityDiscretization.getTime(timeToMaturity) > maxMaturity) {
					continue;
				}

				timeToMaturityIndexing.put(timeToMaturity,volatilityIndex++);
			}
			indexMap.put(simulationTime, timeToMaturityIndexing);
		}

		if(volatility.length == 1) {
			this.volatilities = new RandomVariableInterface[volatilityIndex];
			Arrays.fill(this.volatilities, volatility[0]);
		}
		else {
			this.volatilities = volatility;
		}

		if(volatilityIndex != this.volatilities.length) {
			throw new IllegalArgumentException("volatility.length should equal simulationTimeDiscretization.getNumberOfTimes()*timeToMaturityDiscretization.getNumberOfTimes().");
		}
		this.simulationTimeDiscretization = simulationTimeDiscretization;
		this.timeToMaturityDiscretization = timeToMaturityDiscretization;
		this.isCalibrateable = isCalibrateable;
	}

	public LIBORVolatilityModelPiecewiseConstant(TimeDiscretizationInterface timeDiscretization, TimeDiscretizationInterface liborPeriodDiscretization, TimeDiscretizationInterface simulationTimeDiscretization, TimeDiscretizationInterface timeToMaturityDiscretization, RandomVariableInterface volatility, boolean isCalibrateable) {
		this(timeDiscretization, liborPeriodDiscretization, simulationTimeDiscretization, timeToMaturityDiscretization, new RandomVariableInterface[] { volatility }, isCalibrateable);
	}

	@Override
	public RandomVariableInterface[] getParameter() {
		if(isCalibrateable) {
			return volatilities;
		} else {
			return null;
		}
	}

	@Override
	public void setParameter(RandomVariableInterface[] parameter) {
		if(isCalibrateable) {
			this.volatilities = parameter;
		}
	}

	@Override
	public RandomVariableInterface getVolatility(int timeIndex, int liborIndex) {
		// Create a very simple volatility model here
		double time             = getTimeDiscretization().getTime(timeIndex);
		double maturity         = getLiborPeriodDiscretization().getTime(liborIndex);
		double timeToMaturity   = maturity-time;

		double volatilityInstanteaneous;
		if(timeToMaturity <= 0) {
			return new RandomVariable(0.0);  // This forward rate is already fixed, no volatility
		}
		else
		{
			int timeIndexSimulationTime = simulationTimeDiscretization.getTimeIndex(time);
			if(timeIndexSimulationTime < 0) {
				timeIndexSimulationTime = -timeIndexSimulationTime-1-1;
			}
			if(timeIndexSimulationTime < 0) {
				timeIndexSimulationTime = 0;
			}
			if(timeIndexSimulationTime >= simulationTimeDiscretization.getNumberOfTimes()) {
				timeIndexSimulationTime--;
			}

			int timeIndexTimeToMaturity = timeToMaturityDiscretization.getTimeIndex(timeToMaturity);
			if(timeIndexTimeToMaturity < 0) {
				timeIndexTimeToMaturity = -timeIndexTimeToMaturity-1-1;
			}
			if(timeIndexTimeToMaturity < 0) {
				timeIndexTimeToMaturity = 0;
			}
			if(timeIndexTimeToMaturity >= timeToMaturityDiscretization.getNumberOfTimes()) {
				timeIndexTimeToMaturity--;
			}

			return volatilities[indexMap.get(timeIndexSimulationTime).get(timeIndexTimeToMaturity)];
		}
	}

	@Override
	public Object clone() {
		return new LIBORVolatilityModelPiecewiseConstant(
				super.getTimeDiscretization(),
				super.getLiborPeriodDiscretization(),
				this.simulationTimeDiscretization,
				this.timeToMaturityDiscretization,
				this.volatilities.clone(),
				this.isCalibrateable
				);
	}
}
