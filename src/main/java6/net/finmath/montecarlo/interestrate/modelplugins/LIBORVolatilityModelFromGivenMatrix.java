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
 * Implements a simple volatility model using given piece-wise constant values on
 * a given discretization grid.
 * 
 * @author Christian Fries
 */
public class LIBORVolatilityModelFromGivenMatrix extends LIBORVolatilityModel {
	private final double[][]		volatility;
	
	/**
	 * Creates a simple volatility model using given piece-wise constant values on
 	 * a given discretization grid.
 	 * 
	 * @param timeDiscretization Discretization of simulation time.
	 * @param liborPeriodDiscretization Discretization of tenor times.
	 * @param volatility Volatility matrix volatility[timeIndex][componentIndex] where timeIndex the index of the start time in timeDiscretization and componentIndex from liborPeriodDiscretization
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
		int rows = volatility.length;
		int cols = volatility[0].length;
		int size = cols * rows;

		double[] parameter = new double[size];
		for(int row=0; row<volatility.length; row++)
			System.arraycopy(volatility[row], 0, parameter, row*cols, cols);

		return parameter;
	}

	@Override
	public void setParameter(double[] parameter) {
		int cols = volatility[0].length;

		for(int row=0; row<volatility.length; row++)
			System.arraycopy(parameter, row*cols, volatility[row], 0, cols);

		return;
	}

	@Override
	public Object clone() {
		return new LIBORVolatilityModelFromGivenMatrix(
				getTimeDiscretization(),
				getLiborPeriodDiscretization(),
				volatility
				);
	}
}
