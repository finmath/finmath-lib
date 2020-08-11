package net.finmath.equities.models;

import java.time.LocalDate;
import java.util.ArrayList;

import net.finmath.equities.marketdata.VolatilityPoint;

/**
 * Interface for an equity volatility surface. Currently implemented are a flat surface
 * and the SVI volatility surface parametrization from Gatheral's 2013 paper.
 *
 * @author Andreas Grotz
 */

public interface IVolatilitySurface extends Cloneable {

	double getVolatility(
			double strike,
			LocalDate expiryDate,
			IEquityForwardStructure currentForwardStructure);

	double getVolatility(
			double strike,
			double  timeToMaturity,
			IEquityForwardStructure currentForwardStructure);

	double getLocalVolatility(
			double strike,
			LocalDate expiryDate,
			IEquityForwardStructure currentForwardStructure,
			double strikeShift,
			double timeShift);

	double getLocalVolatility(
			double logStrike,
			double timeToMaturity,
			IEquityForwardStructure currentForwardStructure,
			double strikeShift,
			double timeShift);

	void calibrate(
			IEquityForwardStructure forwardStructure,
			ArrayList<VolatilityPoint> volaPoints);

	IShiftedVolatilitySurface getShiftedSurface(double shift);
}
