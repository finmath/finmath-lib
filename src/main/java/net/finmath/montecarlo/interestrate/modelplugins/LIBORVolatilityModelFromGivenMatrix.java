/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2006
 */
package net.finmath.montecarlo.interestrate.modelplugins;

import java.util.ArrayList;

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
	 * A cache for the parameter associated with this model, it is only used when getParameter is
	 * called repeatedly.
	 */
	private transient double[]		parameter = null;

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
		synchronized (this) {
			if(parameter == null) {
				ArrayList<Double> parameterArray = new ArrayList<Double>();
				for(int timeIndex = 0; timeIndex<getTimeDiscretization().getNumberOfTimeSteps(); timeIndex++) {
					for(int liborPeriodIndex = 0; liborPeriodIndex< getLiborPeriodDiscretization().getNumberOfTimeSteps(); liborPeriodIndex++) {
						if(getTimeDiscretization().getTime(timeIndex) < getLiborPeriodDiscretization().getTime(liborPeriodIndex) ) {
							parameterArray.add(volatility[timeIndex][liborPeriodIndex]);
						}
					}
				}
				parameter = new double[parameterArray.size()];
				for(int i=0; i<parameter.length; i++) parameter[i] = parameterArray.get(i);
			}
		}

		return parameter;
	}

	@Override
	public void setParameter(double[] parameter) {
		this.parameter = null;		// Invalidate cache
		int parameterIndex = 0;
		for(int timeIndex = 0; timeIndex<getTimeDiscretization().getNumberOfTimeSteps(); timeIndex++) {
			for(int liborPeriodIndex = 0; liborPeriodIndex< getLiborPeriodDiscretization().getNumberOfTimeSteps(); liborPeriodIndex++) {
				if(getTimeDiscretization().getTime(timeIndex) < getLiborPeriodDiscretization().getTime(liborPeriodIndex) ) {
					volatility[timeIndex][liborPeriodIndex] = Math.max(parameter[parameterIndex++],0.0);
				}
			}
		}
		return;
	}

	@Override
	public Object clone() {
	    // Clone the outer array.
	    double[][] newVolatilityArray = (double[][]) volatility.clone();

	    // Clone the contents of the array
	    int rows = newVolatilityArray.length;
	    for(int row=0;row<rows;row++){
	    	newVolatilityArray[row] = (double[]) newVolatilityArray[row].clone();
	    }
			 				
		return new LIBORVolatilityModelFromGivenMatrix(
				getTimeDiscretization(),
				getLiborPeriodDiscretization(),
				newVolatilityArray);
	}
}
