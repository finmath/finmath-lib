package net.finmath.singleswaprate.model.volatilities;

import java.time.LocalDate;
import java.util.Map;

import net.finmath.marketdata.model.volatilities.VolatilitySurface.QuotingConvention;
import net.finmath.singleswaprate.model.VolatilityCubeModel;

/**
 * Interface to be implemented by classes providing a volatility cube for a {@link VolatilityCubeModel}. The cubes provide volatilities in three dimensions,
 * being termination, maturity and strike. Maturity being the maturity of a physically settled swaption. Termination being the end date of the underlying swap of said swaption
 * and strike being its strike rate inabsolute terms (not moneyness). The dates are stored as double in correlation to the cubes reference date.
 *
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public interface VolatilityCube {

	/**
	 * Return the volatility at the specified coordinates in the desired quotation.
	 *
	 * @param model A model providing context.
	 * @param termination End date of the underlying.
	 * @param maturity Maturity date of the option.
	 * @param strike Strike rate of the option.
	 * @param quotingConvention Desired quoting convention.
	 * @return The volatility.
	 */
	double getValue(VolatilityCubeModel model, double termination, double maturity, double strike, QuotingConvention quotingConvention);

	/**
	 * Return the volatility at the specified coordinates in the desired quotation.
	 *
	 * @param termination End date of the underlying.
	 * @param maturity Maturity date of the option.
	 * @param strike Strike rate of the option.
	 * @param quotingConvention Desired quoting convention.
	 * @return The volatility.
	 */
	double getValue(double termination, double maturity, double strike, QuotingConvention quotingConvention);

	/**
	 * Returns the name of the volatility cube.
	 *
	 * @return The name of the volatility cube.
	 */
	String getName();

	/**
	 * Return the reference date of this cube, i.e. the date
	 * associated with t=0.
	 *
	 * @return The date identified as t=0.
	 */
	LocalDate getReferenceDate();

	/**
	 * Return the correlation decay parameter of the cube. This is used to determine the correlation between tenors in a derived volvol cube.
	 *
	 * @return The correlation decay parameter.
	 */
	double getCorrelationDecay();

	/**
	 * Return the IBOR vs OIS decorrelation parameter. This parameter scales the convexity adjustment in a multi curve model, using different curves for forward rates and discounting.
	 *
	 * @return The IBOR vs OIS decorrelation parameter.
	 */
	double getIborOisDecorrelation();

	/**
	 * Returns a map with all implementation dependent parameters of this volatility cube.
	 *
	 * @return A map of all parameters.
	 */
	Map<String, Object> getParameters();

	/**
	 * Returns the lowest possible value of strike that can be evaluated by this cube. This is relevant for instance when an implementation uses a SABR model with displacement.
	 *
	 * @param model A model for context.
	 * @return Lowest possible strike this volatility cube supports.
	 */
	double getLowestStrike(VolatilityCubeModel model);

}
