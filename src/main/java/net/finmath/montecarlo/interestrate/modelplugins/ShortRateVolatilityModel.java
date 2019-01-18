/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 24.01.2016
 */

package net.finmath.montecarlo.interestrate.modelplugins;

import net.finmath.time.TimeDiscretization;

/**
 * @author Christian Fries
 * @version 1.0
 */
public class ShortRateVolatilityModel implements ShortRateVolatilityModelInterface {

	private static final long serialVersionUID = 2471249188261414930L;

	private TimeDiscretization timeDiscretization;
	private double[] volatility;
	private double[] meanReversion;

	public ShortRateVolatilityModel(TimeDiscretization timeDiscretization, double[] volatility, double[] meanReversion) {
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
	public double getVolatility(int timeIndex) {
		return volatility[timeIndex];
	}

	@Override
	public double getMeanReversion(int timeIndex) {
		return meanReversion[timeIndex];
	}
}
