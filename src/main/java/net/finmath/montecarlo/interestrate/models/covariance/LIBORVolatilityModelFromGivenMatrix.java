/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2006
 */
package net.finmath.montecarlo.interestrate.models.covariance;

import java.util.ArrayList;
import java.util.Map;

import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFromArrayFactory;
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

	private final RandomVariableFactory	randomVariableFactory;
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
			final RandomVariableFactory randomVariableFactory,
			final TimeDiscretization	timeDiscretization,
			final TimeDiscretization	liborPeriodDiscretization,
			final RandomVariable[][]	volatility,
			final boolean isCalibrateable) {
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
			final TimeDiscretization	timeDiscretization,
			final TimeDiscretization	liborPeriodDiscretization,
			final RandomVariable[][]	volatility,
			final boolean isCalibrateable) {
		super(timeDiscretization, liborPeriodDiscretization);

		randomVariableFactory = new RandomVariableFromArrayFactory();
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
			final TimeDiscretization	timeDiscretization,
			final TimeDiscretization	liborPeriodDiscretization,
			final RandomVariable[][]	volatility) {
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
			final RandomVariableFactory randomVariableFactory,
			final TimeDiscretization	timeDiscretization,
			final TimeDiscretization	liborPeriodDiscretization,
			final double[][]	volatility,
			final boolean isCalibrateable) {
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
			final RandomVariableFactory randomVariableFactory,
			final TimeDiscretization	timeDiscretization,
			final TimeDiscretization	liborPeriodDiscretization,
			final double[][]	volatility) {
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
			final TimeDiscretization	timeDiscretization,
			final TimeDiscretization	liborPeriodDiscretization,
			final double[][]	volatility) {
		this(new RandomVariableFromArrayFactory(), timeDiscretization, liborPeriodDiscretization, volatility);
	}


	@Override
	public RandomVariable getVolatility(final int timeIndex, final int component) {
		return volatility[timeIndex][component];
	}

	@Override
	public RandomVariable[] getParameter() {
		synchronized (volatility) {
			if(parameter == null) {
				final ArrayList<RandomVariable> parameterArray = new ArrayList<>();
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
	public LIBORVolatilityModelFromGivenMatrix getCloneWithModifiedParameter(final RandomVariable[] parameter) {
		final RandomVariable[][] newVoltility = new RandomVariable[getTimeDiscretization().getNumberOfTimeSteps()][getLiborPeriodDiscretization().getNumberOfTimeSteps()];

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

	@Override
	public LIBORVolatilityModel getCloneWithModifiedData(final Map<String, Object> dataModified) {
		RandomVariableFactory randomVariableFactory = null;
		TimeDiscretization timeDiscretization = this.getTimeDiscretization();
		TimeDiscretization liborPeriodDiscretization = this.getLiborPeriodDiscretization();
		final RandomVariable[][] volatility = this.volatility;
		boolean isCalibrateable = this.isCalibrateable;

		if(dataModified != null) {
			// Explicitly passed covarianceModel has priority
			randomVariableFactory = (RandomVariableFactory)dataModified.getOrDefault("randomVariableFactory", randomVariableFactory);
			timeDiscretization = (TimeDiscretization)dataModified.getOrDefault("timeDiscretization", timeDiscretization);
			liborPeriodDiscretization = (TimeDiscretization)dataModified.getOrDefault("liborPeriodDiscretization", liborPeriodDiscretization);
			isCalibrateable = (boolean)dataModified.getOrDefault("isCalibrateable", isCalibrateable);

			if(dataModified.containsKey("randomVariableFactory")) {
				// Possible to do this using streams?
				for(int i=0;i<volatility.length;i++) {
					for(int j = 0; j<volatility[i].length;j++) {
						volatility[i][j] = randomVariableFactory.createRandomVariable(volatility[i][j].doubleValue());
					}
				}
			}
		}

		final LIBORVolatilityModel newModel = new LIBORVolatilityModelFromGivenMatrix(randomVariableFactory, timeDiscretization, liborPeriodDiscretization, volatility, isCalibrateable);
		return newModel;
	}
}
