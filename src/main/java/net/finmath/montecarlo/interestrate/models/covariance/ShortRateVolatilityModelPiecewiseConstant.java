/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 24.01.2016
 */

package net.finmath.montecarlo.interestrate.models.covariance;

import java.util.Arrays;

import net.finmath.montecarlo.AbstractRandomVariableFactory;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;

/**
 * @author Christian Fries
 * @version 1.0
 */
public class ShortRateVolatilityModelPiecewiseConstant extends AbstractShortRateVolatilityModelParametric implements ShortRateVolatilityModelInterface {

	private static final long serialVersionUID = 4266489807755944607L;

	private TimeDiscretization timeDiscretization;
	private TimeDiscretization volatilityTimeDiscretization;
	private double[] volatility;
	private double[] meanReversion;
	private final AbstractRandomVariableFactory randomVariableFactory;
	private final boolean isVolatilityCalibrateable;
	private final RandomVariable[] volatilityRandomVariables;

	public ShortRateVolatilityModelPiecewiseConstant(AbstractRandomVariableFactory randomVariableFactory, TimeDiscretization timeDiscretization, TimeDiscretization volatilityTimeDiscretization, double[] volatility, double[] meanReversion, boolean isVolatilityCalibrateable) {
		super(timeDiscretization);

		this.timeDiscretization = timeDiscretization;
		this.volatilityTimeDiscretization = volatilityTimeDiscretization;
		this.meanReversion = meanReversion;
		this.randomVariableFactory = randomVariableFactory;

		//Check whether all parameter need to be calibrated
		double maxMaturity = timeDiscretization.getTime(timeDiscretization.getNumberOfTimes()-1);
		int volatilityIndex = 0;
		for(int volatilityTime=0; volatilityTime < volatilityTimeDiscretization.getNumberOfTimes(); volatilityTime++) {
			if(volatilityTimeDiscretization.getTime(volatilityTime) <= maxMaturity) {
				volatilityIndex++;
			}
		}

		//Fill volatility parameter
		if(volatility.length == 1) {
			this.volatility = new double[volatilityIndex];
			Arrays.fill(this.volatility, volatility[0]);
		}
		else if(volatility.length == volatilityIndex) {
			this.volatility = volatility;
		}
		else {
			throw new IllegalArgumentException("Volatility length does not match number of free parameters.");
		}

		if(volatilityIndex != this.volatility.length) {
			throw new IllegalArgumentException("volatility.length should equal volatilityTimeDiscretization.getNumberOfTimes().");
		}

		//Mean reversion needs to be nonzero
		for(int meanReversionIndex = 0; meanReversionIndex < meanReversion.length; meanReversionIndex++) {
			if(meanReversion[meanReversionIndex]==0) {
				throw new IllegalArgumentException("Mean reversion needs to be nonzero");
			}
		}
		//Fill mean reversion parameter (assume that volatility and mean reversion have the discretization)
		if(meanReversion.length == 1) {
			this.meanReversion = new double[volatilityIndex];
			Arrays.fill(this.meanReversion, meanReversion[0]);
		}
		else if(meanReversion.length == volatilityIndex) {
			this.meanReversion = meanReversion;
		}
		else {
			throw new IllegalArgumentException("Mean reversion length does not match number of free parameters.");
		}

		if(volatilityIndex != this.meanReversion.length) {
			throw new IllegalArgumentException("meanReversion.length should equal volatilityTimeDiscretization.getNumberOfTimes().");
		}

		this.isVolatilityCalibrateable = isVolatilityCalibrateable;
		this.volatilityRandomVariables = new RandomVariable[volatilityTimeDiscretization.getNumberOfTimes()];

	}

	@Override
	public double getVolatility(int timeIndex) {

		//Get time for a certain index
		double time = timeDiscretization.getTime(timeIndex);

		//Find index on the volatility discretization according to the time value
		int volatilityTimeIndex = volatilityTimeDiscretization.getTimeIndexNearestLessOrEqual(time);
		return volatility[volatilityTimeIndex];
	}

	public RandomVariable getVolatility(double time) {

		//Find index on the volatility discretization according to the time value
		int volatilityTimeIndex = volatilityTimeDiscretization.getTimeIndexNearestLessOrEqual(time);
		//		double volatilityParameter =  volatility[volatilityTimeIndex];

		double volatilityInstanteaneous;
		if(time <= 0) {
			//Already fixed, no volatility
			volatilityInstanteaneous = 0.0;
			return randomVariableFactory.createRandomVariable(time, volatilityInstanteaneous);
		} else {
			synchronized (volatilityRandomVariables) {

				RandomVariable volatilityRandomVariable = volatilityRandomVariables[volatilityTimeIndex];
				if(volatilityRandomVariable == null) {
					volatilityInstanteaneous = volatility[volatilityTimeIndex];
					volatilityRandomVariable = randomVariableFactory.createRandomVariable(time, volatilityInstanteaneous);
					volatilityRandomVariables[volatilityTimeIndex] = volatilityRandomVariable;
				}
				return volatilityRandomVariable;
			}
		}
	}

	@Override
	public double getMeanReversion(int timeIndex) {

		//Get time for a certain index
		double time = timeDiscretization.getTime(timeIndex);

		//Find index on the volatility discretization according to the time value
		int meanReversionTimeIndex = volatilityTimeDiscretization.getTimeIndexNearestLessOrEqual(time);
		return meanReversion[meanReversionTimeIndex];
	}

	@Override
	public double[] getParameter() {
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
				this.volatility.clone(),
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
				parameters,
				this.meanReversion,
				this.isVolatilityCalibrateable
				);
	}

}
