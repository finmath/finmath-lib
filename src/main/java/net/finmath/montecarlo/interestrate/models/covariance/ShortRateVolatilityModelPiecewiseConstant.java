/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 24.01.2016
 */

package net.finmath.montecarlo.interestrate.models.covariance;

import java.util.Arrays;

import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
import net.finmath.time.TimeDiscretization;

/**
 * Short rate volatility model with a piecewise constant volatility and a piecewise constant mean reversion.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class ShortRateVolatilityModelPiecewiseConstant extends AbstractShortRateVolatilityModelParametric implements ShortRateVolatilityModel {

	private static final long serialVersionUID = 4266489807755944607L;

	private final TimeDiscretization timeDiscretization;
	private final TimeDiscretization volatilityTimeDiscretization;

	private RandomVariable[] volatility;
	private RandomVariable[] meanReversion;
	private final RandomVariableFactory randomVariableFactory;
	private final boolean isVolatilityCalibrateable;
	private final boolean isMeanReversionCalibrateable;

	public ShortRateVolatilityModelPiecewiseConstant(final RandomVariableFactory randomVariableFactory, final TimeDiscretization timeDiscretization, final TimeDiscretization volatilityTimeDiscretization, final RandomVariable[] volatility, final RandomVariable[] meanReversion, final boolean isVolatilityCalibrateable, final boolean isMeanReversionCalibrateable) {
		super(timeDiscretization);

		if(volatility.length != volatilityTimeDiscretization.getNumberOfTimes()) {
			throw new IllegalArgumentException("volatility.length should equal volatilityTimeDiscretization.getNumberOfTimes().");
		}

		if(meanReversion.length != volatilityTimeDiscretization.getNumberOfTimes()) {
			throw new IllegalArgumentException("meanReversion.length should equal volatilityTimeDiscretization.getNumberOfTimes().");
		}

		this.timeDiscretization = timeDiscretization;
		this.volatilityTimeDiscretization = volatilityTimeDiscretization;
		this.randomVariableFactory = randomVariableFactory;
		this.volatility = volatility;
		this.meanReversion = meanReversion;
		this.isVolatilityCalibrateable = isVolatilityCalibrateable;
		this.isMeanReversionCalibrateable = isMeanReversionCalibrateable;
	}

	public ShortRateVolatilityModelPiecewiseConstant(final RandomVariableFactory randomVariableFactory, final TimeDiscretization timeDiscretization, final TimeDiscretization volatilityTimeDiscretization, final double[] volatility, final double[] meanReversion, final boolean isVolatilityCalibrateable, final boolean isMeanReversionCalibrateable) {
		super(timeDiscretization);

		this.timeDiscretization = timeDiscretization;
		this.volatilityTimeDiscretization = volatilityTimeDiscretization;
		this.randomVariableFactory = randomVariableFactory;

		//Check whether all parameter need to be calibrated
		final double maxMaturity = timeDiscretization.getTime(timeDiscretization.getNumberOfTimes()-1);
		int volatilityIndex = 0;
		for(int volatilityTime=0; volatilityTime < volatilityTimeDiscretization.getNumberOfTimes(); volatilityTime++) {
			if(volatilityTimeDiscretization.getTime(volatilityTime) <= maxMaturity) {
				volatilityIndex++;
			}
		}

		// Fill volatility parameter
		if(volatility.length == 1) {
			this.volatility = new RandomVariable[volatilityIndex];
			Arrays.fill(this.volatility, randomVariableFactory.createRandomVariable(volatility[0]));
		}
		else if(volatility.length == volatilityIndex) {
			this.volatility = randomVariableFactory.createRandomVariableArray(volatility);
		}
		else {
			throw new IllegalArgumentException("Volatility length (" + volatility.length + ") does not match number of free parameters " + volatilityIndex + ".");
		}

		if(volatilityIndex != this.volatility.length) {
			throw new IllegalArgumentException("volatility.length should equal volatilityTimeDiscretization.getNumberOfTimes().");
		}

		// Mean reversion needs to be nonzero
		for(int meanReversionIndex = 0; meanReversionIndex < meanReversion.length; meanReversionIndex++) {
			if(meanReversion[meanReversionIndex]==0) {
				throw new IllegalArgumentException("Mean reversion needs to be nonzero");
			}
		}

		// Fill mean reversion parameter (assume that volatility and mean reversion have the discretization)
		if(meanReversion.length == 1) {
			this.meanReversion = new RandomVariable[volatilityIndex];
			Arrays.fill(this.meanReversion, randomVariableFactory.createRandomVariable(meanReversion[0]));
		}
		else if(meanReversion.length == volatilityIndex) {
			this.meanReversion = randomVariableFactory.createRandomVariableArray(meanReversion);
		}
		else {
			throw new IllegalArgumentException("Mean reversion length does not match number of free parameters.");
		}

		if(volatilityIndex != this.meanReversion.length) {
			throw new IllegalArgumentException("meanReversion.length should equal volatilityTimeDiscretization.getNumberOfTimes().");
		}

		this.isVolatilityCalibrateable = isVolatilityCalibrateable;
		this.isMeanReversionCalibrateable = isMeanReversionCalibrateable;
	}

	public ShortRateVolatilityModelPiecewiseConstant(final RandomVariableFactory randomVariableFactory, final TimeDiscretization timeDiscretization, final TimeDiscretization volatilityTimeDiscretization, final double[] volatility, final double[] meanReversion, final boolean isVolatilityCalibrateable) {
		this(randomVariableFactory, timeDiscretization, volatilityTimeDiscretization, volatility, meanReversion, isVolatilityCalibrateable, false);
	}

	public ShortRateVolatilityModelPiecewiseConstant(final RandomVariableFactory randomVariableFactory, final TimeDiscretization timeDiscretization, final TimeDiscretization volatilityTimeDiscretization, final RandomVariable[] volatility, final RandomVariable[] meanReversion, final boolean isVolatilityCalibrateable) {
		this(randomVariableFactory, timeDiscretization, volatilityTimeDiscretization, volatility, meanReversion, isVolatilityCalibrateable, false);
	}

	@Override
	public RandomVariable getVolatility(final int timeIndex) {

		//Get time for a certain index
		final double time = timeDiscretization.getTime(timeIndex);

		//Find index on the volatility discretization according to the time value
		final int volatilityTimeIndex = volatilityTimeDiscretization.getTimeIndexNearestLessOrEqual(time);
		return volatility[volatilityTimeIndex];
	}

	public RandomVariable getVolatility(final double time) {

		if(time <= 0) {
			// Already fixed, no volatility
			return new Scalar(0.0);
		}
		else {
			final int timeIndex = volatilityTimeDiscretization.getTimeIndexNearestLessOrEqual(time);
			return volatility[timeIndex];
		}
	}

	@Override
	public RandomVariable getMeanReversion(final int timeIndex) {

		//Get time for a certain index
		final double time = timeDiscretization.getTime(timeIndex);

		final int meanReversionTimeIndex = volatilityTimeDiscretization.getTimeIndexNearestLessOrEqual(time);
		return meanReversion[meanReversionTimeIndex];
	}

	@Override
	public RandomVariable[] getParameter() {
		final int volatilityParameterLength = (isVolatilityCalibrateable ? volatility.length : 0);
		final int parameterLength = volatilityParameterLength + (isMeanReversionCalibrateable ? meanReversion.length : 0);
		if(parameterLength == 0) {
			return null;
		}

		final RandomVariable[] parameter = new RandomVariable[parameterLength];
		if(isVolatilityCalibrateable) {
			System.arraycopy(volatility, 0, parameter, 0, volatility.length);
		}

		if(isMeanReversionCalibrateable) {
			System.arraycopy(meanReversion, 0, parameter, volatilityParameterLength, meanReversion.length);
		}

		return parameter;
	}

	@Override
	public Object clone() {
		return new ShortRateVolatilityModelPiecewiseConstant(
				randomVariableFactory,
				super.getTimeDiscretization(),
				volatilityTimeDiscretization,
				volatility,
				meanReversion,
				isVolatilityCalibrateable
				);
	}

	@Override
	public AbstractShortRateVolatilityModelParametric getCloneWithModifiedParameters(final RandomVariable[] parameters) {
		return new ShortRateVolatilityModelPiecewiseConstant(
				randomVariableFactory,
				super.getTimeDiscretization(),
				volatilityTimeDiscretization,
				parameters,
				meanReversion,
				isVolatilityCalibrateable
				);
	}

	@Override
	public AbstractShortRateVolatilityModelParametric getCloneWithModifiedParameters(final double[] parameters) {
		RandomVariable[] newVolatility = volatility;
		RandomVariable[] newMeanReversion = meanReversion;

		if(isVolatilityCalibrateable && !isMeanReversionCalibrateable) {
			newVolatility = randomVariableFactory.createRandomVariableArray(parameters);
		}
		else if(!isVolatilityCalibrateable && isMeanReversionCalibrateable) {
			newMeanReversion = randomVariableFactory.createRandomVariableArray(parameters);
		}
		else if(isVolatilityCalibrateable && isMeanReversionCalibrateable) {
			final double[] newVolatilityParameters = new double[volatility.length];
			final double[] newMeanReversionParameters = new double[meanReversion.length];
			System.arraycopy(parameters, 0, newVolatilityParameters, 0, newVolatilityParameters.length);
			System.arraycopy(parameters, newVolatilityParameters.length, newMeanReversionParameters, 0, newMeanReversionParameters.length);

			newVolatility = randomVariableFactory.createRandomVariableArray(newVolatilityParameters);
			newMeanReversion = randomVariableFactory.createRandomVariableArray(newMeanReversionParameters);
		}
		else {
			return this;
		}

		return new ShortRateVolatilityModelPiecewiseConstant(
				randomVariableFactory,
				super.getTimeDiscretization(),
				volatilityTimeDiscretization,
				newVolatility,
				newMeanReversion,
				isVolatilityCalibrateable
				);
	}

	/**
	 * Returns the time discretization used for the picewise constant volatility and mean reversion.
	 *
	 * @return The volatility discretization.
	 */
	public TimeDiscretization getVolatilityTimeDiscretization() {
		return volatilityTimeDiscretization;
	}

}
