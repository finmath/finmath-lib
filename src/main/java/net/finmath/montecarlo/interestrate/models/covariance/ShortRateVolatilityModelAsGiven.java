/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 24.01.2016
 */

package net.finmath.montecarlo.interestrate.models.covariance;

import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
import net.finmath.time.TimeDiscretization;

/**
 * A short rate volatility model from given volatility and mean reversion.
 * Note that his model does not implement {@link ShortRateVolatilityModelParametric} and {@link ShortRateVolatilityModelCalibrateable}.
 * If you require a calibration use {@link ShortRateVolatilityModelPiecewiseConstant} instead.

 * @author Christian Fries
 * @version 1.0
 */
public class ShortRateVolatilityModelAsGiven implements ShortRateVolatilityModel {

	private static final long serialVersionUID = 2471249188261414930L;

	private TimeDiscretization timeDiscretization;
	private double[] volatility;
	private double[] meanReversion;

	public ShortRateVolatilityModelAsGiven(TimeDiscretization timeDiscretization, double[] volatility, double[] meanReversion) {
		super();
		this.timeDiscretization = timeDiscretization;
		this.volatility = volatility;
		this.meanReversion = meanReversion;
	}

	@Override
	public TimeDiscretization getTimeDiscretization() {
		return timeDiscretization;
	}

	@Override
	public RandomVariable getVolatility(int timeIndex) {
		return new Scalar(volatility[timeIndex]);
	}

	@Override
	public RandomVariable getMeanReversion(int timeIndex) {
		return new Scalar(meanReversion[timeIndex]);
	}
}
