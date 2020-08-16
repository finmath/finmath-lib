package net.finmath.equities.pricer;

import java.util.HashMap;

import net.finmath.equities.pricer.EquityValuationRequest.CalculationRequestType;

/**
 * Class to store a pricing result provided by implementations of the IOptionPricer interface.
 *
 * @author Andreas Grotz
 */

public class EquityValuationResult {

	private final EquityValuationRequest request;
	private final HashMap<CalculationRequestType, Double> results;

	public EquityValuationResult(
			final EquityValuationRequest request, final HashMap<CalculationRequestType, Double> results)
	{
		this.request = request;
		this.results = results;
	}
}
