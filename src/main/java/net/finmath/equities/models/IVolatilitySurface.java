package net.finmath.equities.models;

import net.finmath.equities.marketdata.VolatilityPoint;
import java.time.LocalDate;
import java.util.ArrayList;

/**
 * Interface for an equity volatility surface. Currently implemented are a flat surface
 * and the SVI volatility surface parametrization from Gatheral's 2013 paper.
 *
 * @author Andreas Grotz
 */

public interface IVolatilitySurface extends Cloneable {

	public double getVolatility(
			double strike,
			LocalDate expiryDate,
			IEquityForwardStructure currentForwardStructure);

	public double getVolatility(
			double strike,
			double  timeToMaturity,
			IEquityForwardStructure currentForwardStructure);

	public double getLocalVolatility(
			double strike,
			LocalDate expiryDate,
			IEquityForwardStructure currentForwardStructure,
			double strikeShift,
			double timeShift);

	public double getLocalVolatility(
			double logStrike,
			double timeToMaturity,
			IEquityForwardStructure currentForwardStructure,
			double strikeShift,
			double timeShift);

	public void calibrate(
			IEquityForwardStructure forwardStructure,
			ArrayList<VolatilityPoint> volaPoints);

	public IShiftedVolatilitySurface getShiftedSurface(double shift);
}