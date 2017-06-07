/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 08.08.2005
 */
package net.finmath.montecarlo.interestrate.modelplugins;

import net.finmath.math.stochastic.RandomVariableInterface;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * Implements a piecewise constant volatility model, where
 * \( \sigma(t,T) = sigma_{i} \) where \( i = \max \{ j : \tau_{j} \leq T-t \} \) and
 * \( \tau_{0}, \tau_{1}, \ldots, \tau_{n-1} \) is a given time discretization.
 * 
 * @author Christian Fries
 */
public class LIBORVolatilityModelTimeHomogenousPiecewiseConstant extends LIBORVolatilityModel {

	private final TimeDiscretizationInterface timeToMaturityDiscretization;
	private double[] volatility;

	/**
	 * Create a piecewise constant volatility model, where
	 * \( \sigma(t,T) = sigma_{i} \) where \( i = \max \{ j : \tau_{j} \leq T-t \} \) and
	 * \( \tau_{0}, \tau_{1}, \ldots, \tau_{n-1} \) is a given time discretization.
	 * 
	 * @param timeDiscretization The simulation time discretization t<sub>j</sub>.
	 * @param liborPeriodDiscretization The period time discretization T<sub>i</sub>.
	 * @param timeToMaturityDiscretization The discretization \( \tau_{0}, \tau_{1}, \ldots, \tau_{n-1} \)  of the piecewise constant volatility function.
	 * @param volatility The values \( \sigma_{0}, \sigma_{1}, \ldots, \sigma_{n-1} \) of the piecewise constant volatility function.
	 */
	public LIBORVolatilityModelTimeHomogenousPiecewiseConstant(TimeDiscretizationInterface timeDiscretization, TimeDiscretizationInterface liborPeriodDiscretization, TimeDiscretizationInterface timeToMaturityDiscretization, double[] volatility) {
		super(timeDiscretization, liborPeriodDiscretization);

		if(timeToMaturityDiscretization.getTime(0) != 0) throw new IllegalArgumentException("timeToMaturityDiscretization should start with 0 as first time point.");
		if(timeToMaturityDiscretization.getNumberOfTimes() != volatility.length) throw new IllegalArgumentException("volatility.length should equal timeToMaturityDiscretization.getNumberOfTimes() .");
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
			int timeIndexTimeToMaturity = timeToMaturityDiscretization.getTimeIndex(timeToMaturity);
			if(timeIndexTimeToMaturity < 0) timeIndexTimeToMaturity = -timeIndexTimeToMaturity-1-1;
			if(timeIndexTimeToMaturity < 0) timeIndexTimeToMaturity = 0;
			if(timeIndexTimeToMaturity >= timeToMaturityDiscretization.getNumberOfTimes()) timeIndexTimeToMaturity--;
			volatilityInstanteaneous = volatility[timeIndexTimeToMaturity];
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
