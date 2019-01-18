/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2006
 */
package net.finmath.montecarlo.interestrate.models.covariancemodels;

import java.util.ArrayList;

import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;

/**
 * Implements a simple volatility model using given piece-wise constant values on
 * a given discretization grid.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class LIBORVolatilityModelFromGivenMatrix extends LIBORVolatilityModel {

	private final RandomVariable[][]	volatility;

	/**
	 * A cache for the parameter associated with this model, it is only used when getParameter is
	 * called repeatedly.
	 */
	private transient RandomVariable[]	parameter;

	/**
	 * Creates a simple volatility model using given piece-wise constant values on
	 * a given discretization grid.
	 *
	 * @param timeDiscretizationFromArray Discretization of simulation time.
	 * @param liborPeriodDiscretization Discretization of tenor times.
	 * @param volatility Volatility matrix volatility[timeIndex][componentIndex] where timeIndex the index of the start time in timeDiscretizationFromArray and componentIndex from liborPeriodDiscretization
	 */
	public LIBORVolatilityModelFromGivenMatrix(
			TimeDiscretization	timeDiscretization,
			TimeDiscretization	liborPeriodDiscretization,
			RandomVariable[][]	volatility) {
		super(timeDiscretization, liborPeriodDiscretization);

		this.volatility = volatility.clone();
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.models.modelplugins.LIBORVolatilityModel#getVolatility(int, int)
	 */
	@Override
	public RandomVariable getVolatility(int timeIndex, int component) {

		return volatility[timeIndex][component];
	}

	@Override
	public RandomVariable[] getParameter() {
		synchronized (this) {
			if(parameter == null) {
				ArrayList<RandomVariable> parameterArray = new ArrayList<RandomVariable>();
				for(int timeIndex = 0; timeIndex<getTimeDiscretization().getNumberOfTimeSteps(); timeIndex++) {
					for(int liborPeriodIndex = 0; liborPeriodIndex< getLiborPeriodDiscretization().getNumberOfTimeSteps(); liborPeriodIndex++) {
						if(getTimeDiscretization().getTime(timeIndex) < getLiborPeriodDiscretization().getTime(liborPeriodIndex) ) {
							parameterArray.add(getVolatility(timeIndex,liborPeriodIndex));
						}
					}
				}
				parameter = parameterArray.toArray(new RandomVariable[] {});
			}
		}

		return parameter;
	}

	@Override
	public void setParameter(RandomVariable[] parameter) {
		this.parameter = null;		// Invalidate cache
		int parameterIndex = 0;
		for(int timeIndex = 0; timeIndex<getTimeDiscretization().getNumberOfTimeSteps(); timeIndex++) {
			for(int liborPeriodIndex = 0; liborPeriodIndex< getLiborPeriodDiscretization().getNumberOfTimeSteps(); liborPeriodIndex++) {
				if(getTimeDiscretization().getTime(timeIndex) < getLiborPeriodDiscretization().getTime(liborPeriodIndex) ) {
					volatility[timeIndex][liborPeriodIndex] = parameter[parameterIndex++];
				}
			}
		}

		return;
	}

	@Override
	public Object clone() {
		// Clone the outer array.
		RandomVariable[][] newVolatility = volatility.clone();

		// Clone the contents of the array
		return new LIBORVolatilityModelFromGivenMatrix(
				getTimeDiscretization(),
				getLiborPeriodDiscretization(),
				newVolatility);
	}
}
