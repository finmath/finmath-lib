/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 24.01.2016
 */

package net.finmath.montecarlo.interestrate.modelplugins;

import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * @author Christian Fries
 * @version 1.0
 */
public class ShortRateVolatilityModelHoLee implements ShortRateVolatilityModelInterface {

	private final double volatility;

	private final TimeDiscretizationInterface timeDiscretization = new TimeDiscretization(0.0);

	public ShortRateVolatilityModelHoLee(double volatility) {
		super();
		this.volatility = volatility;
	}

	@Override
	public TimeDiscretizationInterface getTimeDiscretization() {
		return timeDiscretization;
	}

	@Override
	public double getVolatility(int timeIndex) {
		return volatility;
	}

	@Override
	public double getMeanReversion(int timeIndex) {
		return 0.0;
	}
}
