/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2006
 */
package net.finmath.montecarlo.interestrate.covariancemodels;

import java.util.ArrayList;

import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * Implements a simple volatility model using given piece-wise constant values on
 * a given discretization grid.
 *
 * @author Christian Fries
 */
public class LIBORVolatilityModelFromGivenMatrix extends LIBORVolatilityModel {

	private final RandomVariableInterface[][]	volatility;

	/**
	 * A cache for the parameter associated with this model, it is only used when getParameter is
	 * called repeatedly.
	 */
	private transient RandomVariableInterface[]	parameter;

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
			RandomVariableInterface[][]	volatility) {
		super(timeDiscretization, liborPeriodDiscretization);

		this.volatility = volatility.clone();
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.modelplugins.LIBORVolatilityModel#getVolatility(int, int)
	 */
	@Override
	public RandomVariableInterface getVolatility(int timeIndex, int component) {

		return volatility[timeIndex][component];
	}

	@Override
	public RandomVariableInterface[] getParameter() {
		synchronized (this) {
			if(parameter == null) {
				ArrayList<RandomVariableInterface> parameterArray = new ArrayList<RandomVariableInterface>();
				for(int timeIndex = 0; timeIndex<getTimeDiscretization().getNumberOfTimeSteps(); timeIndex++) {
					for(int liborPeriodIndex = 0; liborPeriodIndex< getLiborPeriodDiscretization().getNumberOfTimeSteps(); liborPeriodIndex++) {
						if(getTimeDiscretization().getTime(timeIndex) < getLiborPeriodDiscretization().getTime(liborPeriodIndex) ) {
							parameterArray.add(getVolatility(timeIndex,liborPeriodIndex));
						}
					}
				}
				parameter = parameterArray.toArray(new RandomVariableInterface[] {});
			}
		}

		return parameter;
	}

	@Override
	public void setParameter(RandomVariableInterface[] parameter) {
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
		RandomVariableInterface[][] newVolatility = volatility.clone();

		// Clone the contents of the array
		return new LIBORVolatilityModelFromGivenMatrix(
				getTimeDiscretization(),
				getLiborPeriodDiscretization(),
				newVolatility);
	}
}
