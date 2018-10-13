/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2006
 */
package net.finmath.montecarlo.interestrate.modelplugins;

import java.util.ArrayList;

import net.finmath.montecarlo.AbstractRandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

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
	private final double[][]		volatilityMatrix;

	private boolean isCalibrateable = false;

	/**
	 * A cache for the parameter associated with this model, it is only used when getParameter is
	 * called repeatedly.
	 */
	private transient double[]	parameter;

	// A lazy init cache
	private transient RandomVariableInterface[][] volatility;

	/**
	 * Creates a simple volatility model using given piece-wise constant values on
	 * a given discretization grid.
	 *
	 * @param randomVariableFactory The random variable factor used to construct random variables from the parameters.
	 * @param timeDiscretization Discretization of simulation time.
	 * @param liborPeriodDiscretization Discretization of tenor times.
	 * @param volatility Volatility matrix volatility[timeIndex][componentIndex] where timeIndex the index of the start time in timeDiscretization and componentIndex from liborPeriodDiscretization
	 * @param isCalibrateable Set this to true, if the parameters are available for calibration.
	 */
	public LIBORVolatilityModelFromGivenMatrix(
			AbstractRandomVariableFactory randomVariableFactory,
			TimeDiscretizationInterface	timeDiscretization,
			TimeDiscretizationInterface	liborPeriodDiscretization,
			double[][]	volatility,
			boolean isCalibrateable) {
		super(timeDiscretization, liborPeriodDiscretization);

		this.randomVariableFactory = randomVariableFactory;
		this.volatilityMatrix = volatility;
		this.isCalibrateable = isCalibrateable;

		this.volatility = new RandomVariableInterface[timeDiscretization.getNumberOfTimeSteps()][liborPeriodDiscretization.getNumberOfTimeSteps()];
	}

	/**
	 * Creates a simple volatility model using given piece-wise constant values on
	 * a given discretization grid.
	 *
	 * @param randomVariableFactory The random variable factor used to construct random variables from the parameters.
	 * @param timeDiscretization Discretization of simulation time.
	 * @param liborPeriodDiscretization Discretization of tenor times.
	 * @param volatility Volatility matrix volatility[timeIndex][componentIndex] where timeIndex the index of the start time in timeDiscretization and componentIndex from liborPeriodDiscretization
	 */
	public LIBORVolatilityModelFromGivenMatrix(
			AbstractRandomVariableFactory randomVariableFactory,
			TimeDiscretizationInterface	timeDiscretization,
			TimeDiscretizationInterface	liborPeriodDiscretization,
			double[][]	volatility) {
		this(randomVariableFactory, timeDiscretization, liborPeriodDiscretization, volatility, true);
	}

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
		this(new RandomVariableFactory(), timeDiscretization, liborPeriodDiscretization, volatility);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.modelplugins.LIBORVolatilityModel#getVolatility(int, int)
	 */
	@Override
	public RandomVariableInterface getVolatility(int timeIndex, int component) {
		synchronized (volatility) {
			if(volatility[timeIndex][component] == null) {
				volatility[timeIndex][component] = randomVariableFactory.createRandomVariable(getTimeDiscretization().getTime(timeIndex), volatilityMatrix[timeIndex][component]);
			}
		}

		return volatility[timeIndex][component];
	}

	@Override
	public double[] getParameter() {
		synchronized (this) {
			if(parameter == null) {
				ArrayList<Double> parameterArray = new ArrayList<Double>();
				for(int timeIndex = 0; timeIndex<getTimeDiscretization().getNumberOfTimeSteps(); timeIndex++) {
					for(int liborPeriodIndex = 0; liborPeriodIndex< getLiborPeriodDiscretization().getNumberOfTimeSteps(); liborPeriodIndex++) {
						if(getTimeDiscretization().getTime(timeIndex) < getLiborPeriodDiscretization().getTime(liborPeriodIndex) ) {
							parameterArray.add(volatilityMatrix[timeIndex][liborPeriodIndex]);
						}
					}
				}
				parameter = new double[parameterArray.size()];
				for(int i=0; i<parameter.length; i++) {
					parameter[i] = parameterArray.get(i);
				}
			}
		}

		return parameter;
	}

	@Override
	public LIBORVolatilityModelFromGivenMatrix getCloneWithModifiedParameter(double[] parameter) {
		// Clone the outer array.
		double[][] newVolatilityArray = new double[getTimeDiscretization().getNumberOfTimeSteps()][getLiborPeriodDiscretization().getNumberOfTimeSteps()];

		int parameterIndex = 0;
		for(int timeIndex = 0; timeIndex<getTimeDiscretization().getNumberOfTimeSteps(); timeIndex++) {
			for(int liborPeriodIndex = 0; liborPeriodIndex< getLiborPeriodDiscretization().getNumberOfTimeSteps(); liborPeriodIndex++) {
				if(getTimeDiscretization().getTime(timeIndex) < getLiborPeriodDiscretization().getTime(liborPeriodIndex) ) {
					newVolatilityArray[timeIndex][liborPeriodIndex] = parameter[parameterIndex++];
				}
			}
		}

		return new LIBORVolatilityModelFromGivenMatrix(
				randomVariableFactory,
				getTimeDiscretization(),
				getLiborPeriodDiscretization(),
				newVolatilityArray);
	}

	@Override
	public Object clone() {
		// Clone the outer array.
		double[][] newVolatilityArray = volatilityMatrix.clone();

		// Clone the contents of the array
		int rows = newVolatilityArray.length;
		for(int row=0;row<rows;row++){
			newVolatilityArray[row] = newVolatilityArray[row].clone();
		}

		return new LIBORVolatilityModelFromGivenMatrix(
				randomVariableFactory,
				getTimeDiscretization(),
				getLiborPeriodDiscretization(),
				newVolatilityArray);
	}
}
