/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 24.01.2016
 */

package net.finmath.montecarlo.interestrate.modelplugins;

import net.finmath.time.TimeDiscretizationFromArray;
import net.finmath.time.TimeDiscretization;

/**
 * @author Christian Fries
 * @version 1.0
 */
public class ShortRateVolatilityModelHoLee implements ShortRateVolatilityModelInterface {

	private static final long serialVersionUID = -4958907273981969081L;

	private final double volatility;

	private final TimeDiscretization timeDiscretization = new TimeDiscretizationFromArray(0.0);

	public ShortRateVolatilityModelHoLee(double volatility) {
		super();
		this.volatility = volatility;
	}

	@Override
	public TimeDiscretization getTimeDiscretization() {
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
