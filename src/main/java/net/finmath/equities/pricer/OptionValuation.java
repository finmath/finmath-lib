package net.finmath.equities.pricer;

import net.finmath.equities.marketdata.FlatYieldCurve;
import net.finmath.equities.models.EquityForwardStructure;
import net.finmath.equities.models.VolatilitySurface;

/**
 * Interface for option pricers that handle pricing requests.
 * Currently implemented are an analytic pricer and a PDE pricer for lognormal models with Buehler dividends.
 *
 * @author Andreas Grotz
 */

public interface OptionValuation extends Cloneable {

	EquityValuationResult calculate(
			EquityValuationRequest request,
			EquityForwardStructure forwardStructure,
			FlatYieldCurve discountCurve,
			VolatilitySurface volSurface);
}
