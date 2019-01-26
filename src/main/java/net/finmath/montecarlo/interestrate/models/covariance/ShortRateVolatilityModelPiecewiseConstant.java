/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 24.01.2016
 */

package net.finmath.montecarlo.interestrate.models.covariance;

import java.util.Arrays;

import net.finmath.montecarlo.AbstractRandomVariableFactory;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
import net.finmath.time.TimeDiscretization;

/**
 * @author Christian Fries
 * @version 1.0
 */
public class ShortRateVolatilityModelPiecewiseConstant extends AbstractShortRateVolatilityModelParametric implements ShortRateVolatilityModelInterface {

	private static final long serialVersionUID = 4266489807755944607L;

	private TimeDiscretization timeDiscretization;
	private TimeDiscretization volatilityTimeDiscretization;
	private RandomVariable[] volatility;
	private RandomVariable[] meanReversion;
	private final AbstractRandomVariableFactory randomVariableFactory;
	private final boolean isVolatilityCalibrateable;

	public ShortRateVolatilityModelPiecewiseConstant(AbstractRandomVariableFactory randomVariableFactory, TimeDiscretization timeDiscretization, TimeDiscretization volatilityTimeDiscretization, RandomVariable[] volatility, RandomVariable[] meanReversion, boolean isVolatilityCalibrateable) {
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
	}

	public ShortRateVolatilityModelPiecewiseConstant(AbstractRandomVariableFactory randomVariableFactory, TimeDiscretization timeDiscretization, TimeDiscretization volatilityTimeDiscretization, double[] volatility, double[] meanReversion, boolean isVolatilityCalibrateable) {
		super(timeDiscretization);

		this.timeDiscretization = timeDiscretization;
		this.volatilityTimeDiscretization = volatilityTimeDiscretization;
		this.randomVariableFactory = randomVariableFactory;

		//Check whether all parameter need to be calibrated
		double maxMaturity = timeDiscretization.getTime(timeDiscretization.getNumberOfTimes()-1);
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
			throw new IllegalArgumentException("Volatility length does not match number of free parameters.");
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
	}

	@Override
	public RandomVariable getVolatility(int timeIndex) {

		//Get time for a certain index
		double time = timeDiscretization.getTime(timeIndex);

		//Find index on the volatility discretization according to the time value
		int volatilityTimeIndex = volatilityTimeDiscretization.getTimeIndexNearestLessOrEqual(time);
		return volatility[volatilityTimeIndex];
	}

	public RandomVariable getVolatility(double time) {

		if(time <= 0) {
			// Already fixed, no volatility
			return new Scalar(0.0);
		}
		else {
			int timeIndex = volatilityTimeDiscretization.getTimeIndexNearestLessOrEqual(time);
			return volatility[timeIndex];
		}
	}

	@Override
	public RandomVariable getMeanReversion(int timeIndex) {

		//Get time for a certain index
		double time = timeDiscretization.getTime(timeIndex);

		int meanReversionTimeIndex = volatilityTimeDiscretization.getTimeIndexNearestLessOrEqual(time);
		return meanReversion[meanReversionTimeIndex];
	}

	@Override
	public RandomVariable[] getParameter() {
		if(isVolatilityCalibrateable) {
			return volatility;
		} else {
			return null;
		}
	}

	@Override
	public Object clone() {
		return new ShortRateVolatilityModelPiecewiseConstant(
				this.randomVariableFactory,
				super.getTimeDiscretization(),
				this.volatilityTimeDiscretization,
				this.volatility,
				this.meanReversion,
				this.isVolatilityCalibrateable
				);
	}

	@Override
	public AbstractShortRateVolatilityModelParametric getCloneWithModifiedParameters(RandomVariable[] parameters) {
		return new ShortRateVolatilityModelPiecewiseConstant(
				this.randomVariableFactory,
				super.getTimeDiscretization(),
				this.volatilityTimeDiscretization,
				parameters,
				this.meanReversion,
				this.isVolatilityCalibrateable
				);
	}

	@Override
	public AbstractShortRateVolatilityModelParametric getCloneWithModifiedParameters(double[] parameters) {
		return new ShortRateVolatilityModelPiecewiseConstant(
				this.randomVariableFactory,
				super.getTimeDiscretization(),
				this.volatilityTimeDiscretization,
				randomVariableFactory.createRandomVariableArray(parameters),
				this.meanReversion,
				this.isVolatilityCalibrateable
				);
	}

}
