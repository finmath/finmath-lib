package net.finmath.equities.pricer;

import net.finmath.equities.marketdata.FlatYieldCurve;
import net.finmath.equities.models.IEquityForwardStructure;
import net.finmath.equities.models.IVolatilitySurface;

/**
 * Interface for option pricers that handle pricing requests.
 * Currently implemented are an analytic pricer and a PDE pricer for lognormal models with Buehler dividends.
 *
 * @author Andreas Grotz
 */

public interface IOptionPricer extends Cloneable {

	public EquityPricingResult calculate(
			EquityPricingRequest request,
			IEquityForwardStructure forwardStructure,
			FlatYieldCurve discountCurve,
			IVolatilitySurface volSurface);
}