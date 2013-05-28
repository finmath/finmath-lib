/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2006
 */
package net.finmath.montecarlo.interestrate.modelplugins;

import net.finmath.montecarlo.RandomVariable;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * Implements a simple volatility model using given piceweise constant values on
 * a given discretization grid.
 * 
 * @author Christian Fries
 */
public class LIBORVolatilityModelFromGivenMatrix extends LIBORVolatilityModel {
	private double[][]		volatility;
	
	/**
	 * @param timeDiscretization Discretization of simulation time.
	 * @param liborPeriodDiscretization Discretizaiton of LIBOR times.
	 * @param volatility Volatility matrix olatility[timeIndex][componentIndex] where timeIndex the index of the start time in timeDiscretization and componentIndex from liborPeriodDiscretization
	 */
	public LIBORVolatilityModelFromGivenMatrix(
			TimeDiscretizationInterface	timeDiscretization,
			TimeDiscretizationInterface	liborPeriodDiscretization,
			double[][]	volatility) {
		super(timeDiscretization, liborPeriodDiscretization);

		this.volatility = volatility;
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.modelplugins.LIBORVolatilityModel#getVolatility(int, int)
	 */
	@Override
    public RandomVariableInterface getVolatility(int timeIndex, int component) {
		return new RandomVariable(getTimeDiscretization().getTime(timeIndex),volatility[timeIndex][component]);
	}

	@Override
	public double[] getParameter() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setParameter(double[] parameter) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object clone() {
		throw new UnsupportedOperationException();
	}
}
