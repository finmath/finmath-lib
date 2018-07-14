/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 24.01.2016
 */

package net.finmath.montecarlo.interestrate.modelplugins;

import net.finmath.time.TimeDiscretizationInterface;

/**
 * @author Christian Fries
 */
public class ShortRateVolatilityModel implements ShortRateVolailityModelInterface {

	private TimeDiscretizationInterface timeDiscretization;
	private double[] volatility;
	private double[] meanReversion;

	public ShortRateVolatilityModel(TimeDiscretizationInterface timeDiscretization, double[] volatility, double[] meanReversion) {
		super();
		this.timeDiscretization = timeDiscretization;
		this.volatility = volatility;
		this.meanReversion = meanReversion;
	}

	@Override
	public TimeDiscretizationInterface getTimeDiscretization() {
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

