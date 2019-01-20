/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2006
 */
package net.finmath.montecarlo.interestrate.models.covariance;

import java.util.ArrayList;

import net.finmath.montecarlo.AbstractRandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFactory;
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

	private static final long serialVersionUID = -8017326082950665302L;

	private final AbstractRandomVariableFactory	randomVariableFactory;
	private final RandomVariable[][]			volatility;

	private final boolean isCalibrateable;

	/**
	 * A cache for the parameter associated with this model, it is only used when getParameter is called repeatedly.
	 */
	private transient RandomVariable[]	parameter;

	/**
	 * Creates a simple volatility model using given piece-wise constant values on
	 * a given discretization grid.
	 *
	 * @param randomVariableFactory The random variable factor used to construct random variables from the parameters.
	 * @param timeDiscretization Discretization of simulation time.
	 * @param liborPeriodDiscretization Discretization of tenor times.
	 * @param volatility Volatility matrix volatility[timeIndex][componentIndex] where timeIndex the index of the start time in timeDiscretizationFromArray and componentIndex from liborPeriodDiscretization
	 * @param isCalibrateable Set this to true, if the parameters are available for calibration.
	 */
	public LIBORVolatilityModelFromGivenMatrix(
			AbstractRandomVariableFactory randomVariableFactory,
			TimeDiscretization	timeDiscretization,
			TimeDiscretization	liborPeriodDiscretization,
			RandomVariable[][]	volatility,
			boolean isCalibrateable) {
		super(timeDiscretization, liborPeriodDiscretization);

		this.randomVariableFactory = randomVariableFactory;
		this.volatility = volatility.clone();
		this.isCalibrateable = isCalibrateable;
	}

	/**
	 * Creates a simple volatility model using given piece-wise constant values on
	 * a given discretization grid.
	 *
	 * @param timeDiscretization Discretization of simulation time.
	 * @param liborPeriodDiscretization Discretization of tenor times.
	 * @param volatility Volatility matrix volatility[timeIndex][componentIndex] where timeIndex the index of the start time in timeDiscretizationFromArray and componentIndex from liborPeriodDiscretization
	 * @param isCalibrateable Set this to true, if the parameters are available for calibration.
	 */
	public LIBORVolatilityModelFromGivenMatrix(
			TimeDiscretization	timeDiscretization,
			TimeDiscretization	liborPeriodDiscretization,
			RandomVariable[][]	volatility,
			boolean isCalibrateable) {
		super(timeDiscretization, liborPeriodDiscretization);

		this.randomVariableFactory = null;
		this.volatility = volatility.clone();
		this.isCalibrateable = isCalibrateable;
	}

	/**
	 * Creates a simple volatility model using given piece-wise constant values on
	 * a given discretization grid.
	 *
	 * @param timeDiscretization Discretization of simulation time.
	 * @param liborPeriodDiscretization Discretization of tenor times.
	 * @param volatility Volatility matrix volatility[timeIndex][componentIndex] where timeIndex the index of the start time in timeDiscretizationFromArray and componentIndex from liborPeriodDiscretization
	 */
	public LIBORVolatilityModelFromGivenMatrix(
			TimeDiscretization	timeDiscretization,
			TimeDiscretization	liborPeriodDiscretization,
			RandomVariable[][]	volatility) {
		this(timeDiscretization, liborPeriodDiscretization, volatility, false);
	}

	/**
	 * Creates a simple volatility model using given piece-wise constant values on
	 * a given discretization grid.
	 *
	 * @param randomVariableFactory The random variable factor used to construct random variables from the parameters.
	 * @param timeDiscretization Discretization of simulation time.
	 * @param liborPeriodDiscretization Discretization of tenor times.
	 * @param volatility Volatility matrix volatility[timeIndex][componentIndex] where timeIndex the index of the start time in timeDiscretizationFromArray and componentIndex from liborPeriodDiscretization
	 * @param isCalibrateable Set this to true, if the parameters are available for calibration.
	 */
	public LIBORVolatilityModelFromGivenMatrix(
			AbstractRandomVariableFactory randomVariableFactory,
			TimeDiscretization	timeDiscretization,
			TimeDiscretization	liborPeriodDiscretization,
			double[][]	volatility,
			boolean isCalibrateable) {
		super(timeDiscretization, liborPeriodDiscretization);

		this.randomVariableFactory = randomVariableFactory;
		this.volatility = randomVariableFactory.createRandomVariableMatrix(volatility);
		this.isCalibrateable = isCalibrateable;

	}

	/**
	 * Creates a simple volatility model using given piece-wise constant values on
	 * a given discretization grid.
	 *
	 * @param randomVariableFactory The random variable factor used to construct random variables from the parameters.
	 * @param timeDiscretization Discretization of simulation time.
	 * @param liborPeriodDiscretization Discretization of tenor times.
	 * @param volatility Volatility matrix volatility[timeIndex][componentIndex] where timeIndex the index of the start time in timeDiscretizationFromArray and componentIndex from liborPeriodDiscretization
	 */
	public LIBORVolatilityModelFromGivenMatrix(
			AbstractRandomVariableFactory randomVariableFactory,
			TimeDiscretization	timeDiscretization,
			TimeDiscretization	liborPeriodDiscretization,
			double[][]	volatility) {
		this(randomVariableFactory, timeDiscretization, liborPeriodDiscretization, volatility, true);
	}

	/**
	 * Creates a simple volatility model using given piece-wise constant values on
	 * a given discretization grid.
	 *
	 * @param timeDiscretization Discretization of simulation time.
	 * @param liborPeriodDiscretization Discretization of tenor times.
	 * @param volatility Volatility matrix volatility[timeIndex][componentIndex] where timeIndex the index of the start time in timeDiscretizationFromArray and componentIndex from liborPeriodDiscretization
	 */
	public LIBORVolatilityModelFromGivenMatrix(
			TimeDiscretization	timeDiscretization,
			TimeDiscretization	liborPeriodDiscretization,
			double[][]	volatility) {
		this(new RandomVariableFactory(), timeDiscretization, liborPeriodDiscretization, volatility);
	}


	@Override
	public RandomVariable getVolatility(int timeIndex, int component) {
		return volatility[timeIndex][component];
	}

	@Override
	public RandomVariable[] getParameter() {
		synchronized (volatility) {
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
	public LIBORVolatilityModelFromGivenMatrix getCloneWithModifiedParameter(RandomVariable[] parameter) {
		RandomVariable[][] newVoltility = new RandomVariable[getTimeDiscretization().getNumberOfTimeSteps()][getLiborPeriodDiscretization().getNumberOfTimeSteps()];

		int parameterIndex = 0;
		for(int timeIndex = 0; timeIndex<getTimeDiscretization().getNumberOfTimeSteps(); timeIndex++) {
			for(int liborPeriodIndex = 0; liborPeriodIndex< getLiborPeriodDiscretization().getNumberOfTimeSteps(); liborPeriodIndex++) {
				if(getTimeDiscretization().getTime(timeIndex) < getLiborPeriodDiscretization().getTime(liborPeriodIndex) ) {
					newVoltility[timeIndex][liborPeriodIndex] = parameter[parameterIndex++];
				}
			}
		}

		return new LIBORVolatilityModelFromGivenMatrix(randomVariableFactory, getTimeDiscretization(), getLiborPeriodDiscretization(), newVoltility, isCalibrateable);
	}

	@Override
	public Object clone() {
		return new LIBORVolatilityModelFromGivenMatrix(randomVariableFactory, getTimeDiscretization(), getLiborPeriodDiscretization(), volatility.clone(), isCalibrateable);
	}
}
