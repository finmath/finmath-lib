/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 08.08.2005
 */
package net.finmath.montecarlo.interestrate.modelplugins;

import net.finmath.montecarlo.RandomVariable;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * @author Christian Fries
 */
public class LIBORVolatilityModelTimeHomogenousPiecewiseConstant extends LIBORVolatilityModel {

	private final TimeDiscretizationInterface timeToMaturityDiscretization;
	private double[] volatility;

	/**
	 * @param timeDiscretization The simulation time discretization t<sub>j</sub>.
	 * @param liborPeriodDiscretization The period time discretization T<sub>i</sub>.
	 * @param timeToMaturityDiscretization The discretization of the piecewise constant volatility function.
	 * @param volatility The values of the piecewise constant volatility function.
	 */
	public LIBORVolatilityModelTimeHomogenousPiecewiseConstant(TimeDiscretizationInterface timeDiscretization, TimeDiscretizationInterface liborPeriodDiscretization, TimeDiscretizationInterface timeToMaturityDiscretization, double[] volatility) {
		super(timeDiscretization, liborPeriodDiscretization);

		if(timeToMaturityDiscretization.getTime(0) != 0) throw new IllegalArgumentException("timeToMaturityDiscretization should start with 0 as first time point.");
		if(timeToMaturityDiscretization.getNumberOfTimeSteps() != volatility.length) throw new IllegalArgumentException("volatility.length should equal timeToMaturityDiscretization.getNumberOfTimeSteps() .");
		this.timeToMaturityDiscretization = timeToMaturityDiscretization;
		this.volatility = volatility;
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
			timeIndex = timeToMaturityDiscretization.getTimeIndex(timeToMaturity);
			if(timeIndex < 0) timeIndex = -timeIndex-1-1;
			volatilityInstanteaneous = volatility[timeIndex];
		}
		if(volatilityInstanteaneous < 0.0) volatilityInstanteaneous = Math.max(volatilityInstanteaneous,0.0);

		return new RandomVariable(getTimeDiscretization().getTime(timeIndex),volatilityInstanteaneous);
	}

	@Override
	public Object clone() {
		return new LIBORVolatilityModelTimeHomogenousPiecewiseConstant(
				super.getTimeDiscretization(),
				super.getLiborPeriodDiscretization(),
				this.timeToMaturityDiscretization,
				this.volatility.clone()
				);
	}
}
