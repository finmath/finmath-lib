/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 08.08.2005
 */
package net.finmath.montecarlo.interestrate.modelplugins;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.finmath.montecarlo.RandomVariable;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * @author Christian Fries
 */
public class LIBORVolatilityModelPiecewiseConstant extends LIBORVolatilityModel {

	private final TimeDiscretizationInterface	simulationTimeDiscretization;
	private final TimeDiscretizationInterface	timeToMaturityDiscretization;
	private Map<Integer, HashMap<Integer, Integer>> 	indexMap = new HashMap<Integer, HashMap<Integer, Integer>>();
	private double[] volatility;

	public LIBORVolatilityModelPiecewiseConstant(TimeDiscretizationInterface timeDiscretization, TimeDiscretizationInterface liborPeriodDiscretization, TimeDiscretizationInterface simulationTimeDiscretization, TimeDiscretizationInterface timeToMaturityDiscretization, double[] volatility) {
		super(timeDiscretization, liborPeriodDiscretization);

		/*
		 * Build index map
		 */
		double maxMaturity = timeToMaturityDiscretization.getTime(timeToMaturityDiscretization.getNumberOfTimes()-1);
		int volatilityIndex = 0;
		for(int simulationTime=0; simulationTime<simulationTimeDiscretization.getNumberOfTimes(); simulationTime++) {
			HashMap<Integer, Integer> timeToMaturityIndexing = new HashMap<Integer, Integer>();
			for(int timeToMaturity=0; timeToMaturity<timeToMaturityDiscretization.getNumberOfTimes(); timeToMaturity++) {
				if(simulationTimeDiscretization.getTime(simulationTime)+timeToMaturityDiscretization.getTime(timeToMaturity) > maxMaturity) continue;
				
				timeToMaturityIndexing.put(timeToMaturity,volatilityIndex++);
			}
			indexMap.put(simulationTime, timeToMaturityIndexing);
		}

		if(volatility.length == 1) {
			this.volatility = new double[volatilityIndex];
			Arrays.fill(this.volatility, volatility[0]);
		}
		else {
			this.volatility = volatility;
		}

		if(volatilityIndex != this.volatility.length) throw new IllegalArgumentException("volatility.length should equal simulationTimeDiscretization.getNumberOfTimes()*timeToMaturityDiscretization.getNumberOfTimes().");
		this.simulationTimeDiscretization = simulationTimeDiscretization;
		this.timeToMaturityDiscretization = timeToMaturityDiscretization;
	}

	public LIBORVolatilityModelPiecewiseConstant(TimeDiscretizationInterface timeDiscretization, TimeDiscretizationInterface liborPeriodDiscretization, TimeDiscretizationInterface simulationTimeDiscretization, TimeDiscretizationInterface timeToMaturityDiscretization, double volatility) {
		this(timeDiscretization, liborPeriodDiscretization, simulationTimeDiscretization, timeToMaturityDiscretization, new double[] { volatility });
	}

	@Override
	public double[] getParameter() {
		return volatility;
	}

	@Override
	public void setParameter(double[] parameter) {
		this.volatility = parameter;
	}

	@Override
	public RandomVariableInterface getVolatility(int timeIndex, int liborIndex) {
		// Create a very simple volatility model here
		double time             = getTimeDiscretization().getTime(timeIndex);
		double maturity         = getLiborPeriodDiscretization().getTime(liborIndex);
		double timeToMaturity   = maturity-time;

		double volatilityInstanteaneous; 
		if(timeToMaturity <= 0)
		{
			volatilityInstanteaneous = 0.0;   // This forward rate is already fixed, no volatility
		}
		else
		{
			int timeIndexSimulationTime = simulationTimeDiscretization.getTimeIndex(time);
			if(timeIndexSimulationTime < 0) timeIndexSimulationTime = -timeIndexSimulationTime-1-1;
			if(timeIndexSimulationTime < 0) timeIndexSimulationTime = 0;
			if(timeIndexSimulationTime >= simulationTimeDiscretization.getNumberOfTimes()) timeIndexSimulationTime--;

			int timeIndexTimeToMaturity = timeToMaturityDiscretization.getTimeIndex(timeToMaturity);
			if(timeIndexTimeToMaturity < 0) timeIndexTimeToMaturity = -timeIndexTimeToMaturity-1-1;
			if(timeIndexTimeToMaturity < 0) timeIndexTimeToMaturity = 0;
			if(timeIndexTimeToMaturity >= timeToMaturityDiscretization.getNumberOfTimes()) timeIndexTimeToMaturity--;

//			volatilityInstanteaneous = volatility[timeIndexSimulationTime * timeToMaturityDiscretization.getNumberOfTimes() + timeIndexTimeToMaturity];
			volatilityInstanteaneous = volatility[indexMap.get(timeIndexSimulationTime).get(timeIndexTimeToMaturity)];
		}
		if(volatilityInstanteaneous < 0.0) volatilityInstanteaneous = Math.max(volatilityInstanteaneous,0.0);

		return new RandomVariable(time, volatilityInstanteaneous);
	}

	@Override
	public Object clone() {
		return new LIBORVolatilityModelPiecewiseConstant(
				super.getTimeDiscretization(),
				super.getLiborPeriodDiscretization(),
				this.simulationTimeDiscretization,
				this.timeToMaturityDiscretization,
				this.volatility.clone()
				);
	}
}
