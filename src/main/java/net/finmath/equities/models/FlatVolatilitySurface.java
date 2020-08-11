package net.finmath.equities.models;

import java.time.LocalDate;
import java.util.ArrayList;

import net.finmath.equities.marketdata.VolatilityPoint;

/**
 * This class implements the volatility interfaces for a flat volatility surface.
 *
 * @author Andreas Grotz
 */

public class FlatVolatilitySurface  implements IVolatilitySurface, IShiftedVolatilitySurface {

	private final double volatility;
	private final double volShift;

	public FlatVolatilitySurface(double volatility) {
		this(volatility, 0.0);
	}

	public FlatVolatilitySurface(double volatility, double volShift) {
		this.volatility = volatility;
		this.volShift = volShift;
	}

	@Override
	public IShiftedVolatilitySurface getShiftedSurface(double shift) {
		assert volShift == 0.0 : "Surface is already shifted";
		return new FlatVolatilitySurface(this.volatility, shift);
	}

	public double getShift() {
		return volShift;
	}

	@Override
	public double getVolatility(
			double strike,
			LocalDate expiryDate,
			IEquityForwardStructure currentForwardStructure)
	{
		return volatility + volShift;
	}

	@Override
	public double getVolatility(double strike, double timeToMaturity, IEquityForwardStructure currentForwardStructure) {
		return volatility + volShift;
	}

	@Override
	public double getLocalVolatility(
			double strike,
			LocalDate expiryDate,
			IEquityForwardStructure currentForwardStructure,
			double strikeShift,
			double timeShift)
	{
		return volatility + volShift;
	}

	@Override
	public double getLocalVolatility(
			double logStrike,
			double timeToMaturity,
			IEquityForwardStructure currentForwardStructure,
			double strikeShift,
			double timeShift)
	{
		return volatility + volShift;
	}

	public void calibrate(
			IEquityForwardStructure forwardStructure,
			ArrayList<VolatilityPoint> volaPoints)
	{
		assert false : "A flat surface cannot be calibrated";
	}
}
