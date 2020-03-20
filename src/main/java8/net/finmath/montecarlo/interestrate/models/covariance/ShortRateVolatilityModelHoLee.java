/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 24.01.2016
 */

package net.finmath.montecarlo.interestrate.models.covariance;

import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * @author Christian Fries
 * @version 1.0
 */
public class ShortRateVolatilityModelHoLee implements ShortRateVolatilityModel {

	private static final long serialVersionUID = -4958907273981969081L;

	private final RandomVariable volatility;

	private final TimeDiscretization timeDiscretization = new TimeDiscretizationFromArray(0.0);

	public ShortRateVolatilityModelHoLee(final double volatility) {
		super();
		this.volatility = new Scalar(volatility);
	}

	@Override
	public TimeDiscretization getTimeDiscretization() {
		return timeDiscretization;
	}

	@Override
	public RandomVariable getVolatility(final int timeIndex) {
		return volatility;
	}

	@Override
	public RandomVariable getMeanReversion(final int timeIndex) {
		return new Scalar(0.0);
	}
}
