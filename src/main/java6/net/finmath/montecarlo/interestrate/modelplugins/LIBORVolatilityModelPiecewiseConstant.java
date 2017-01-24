/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 08.08.2005
 */
package net.finmath.montecarlo.interestrate.modelplugins;

import java.util.Arrays;

import net.finmath.montecarlo.RandomVariable;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * @author Christian Fries
 */
public class LIBORVolatilityModelPiecewiseConstant extends LIBORVolatilityModel {

	private final TimeDiscretizationInterface	simulationTimeDiscretization;
	private final TimeDiscretizationInterface	timeToMaturityDiscretization;
	private double[] volatility;

	public LIBORVolatilityModelPiecewiseConstant(TimeDiscretizationInterface timeDiscretization, TimeDiscretizationInterface liborPeriodDiscretization, TimeDiscretizationInterface simulationTimeDiscretization, TimeDiscretizationInterface timeToMaturityDiscretization, double[] volatility) {
		super(timeDiscretization, liborPeriodDiscretization);

		if(volatility.length == 1) {
			this.volatility = new double[simulationTimeDiscretization.getNumberOfTimes()*timeToMaturityDiscretization.getNumberOfTimes()];
			Arrays.fill(this.volatility, volatility[0]);
		}
		else {
			this.volatility = volatility;
		}

		if(simulationTimeDiscretization.getNumberOfTimes()*timeToMaturityDiscretization.getNumberOfTimes() != this.volatility.length) throw new IllegalArgumentException("volatility.length should equal simulationTimeDiscretization.getNumberOfTimes()*timeToMaturityDiscretization.getNumberOfTimes().");
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

			volatilityInstanteaneous = volatility[timeIndexSimulationTime * timeToMaturityDiscretization.getNumberOfTimes() + timeIndexTimeToMaturity];
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
