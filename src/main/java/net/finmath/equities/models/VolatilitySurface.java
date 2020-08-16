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

public interface VolatilitySurface extends Cloneable {

	double getVolatility(
			double strike,
			LocalDate expiryDate,
			EquityForwardStructure currentForwardStructure);

	double getVolatility(
			double strike,
			double  timeToMaturity,
			EquityForwardStructure currentForwardStructure);

	double getLocalVolatility(
			double strike,
			LocalDate expiryDate,
			EquityForwardStructure currentForwardStructure,
			double strikeShift,
			double timeShift);

	double getLocalVolatility(
			double logStrike,
			double timeToMaturity,
			EquityForwardStructure currentForwardStructure,
			double strikeShift,
			double timeShift);

	void calibrate(
			EquityForwardStructure forwardStructure,
			ArrayList<VolatilityPoint> volaPoints);

	ShiftedVolatilitySurface getShiftedSurface(double shift);
}
