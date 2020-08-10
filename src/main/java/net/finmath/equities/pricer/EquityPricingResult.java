package net.finmath.equities.pricer;

import java.util.HashMap;

import net.finmath.equities.pricer.EquityPricingRequest.CalculationRequestType;

/**
 * Class to store a pricing result provided by implementations of the IOptionPricer interface.
 *
 * @author Andreas Grotz
 */

public class EquityPricingResult {

	public final EquityPricingRequest request;
	public final HashMap<CalculationRequestType, Double> results;

	public EquityPricingResult(
			final EquityPricingRequest request, final HashMap<CalculationRequestType, Double> results)
	{
		this.request = request;
		this.results = results;
	}
}