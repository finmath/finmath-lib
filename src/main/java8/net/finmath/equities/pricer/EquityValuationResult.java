package net.finmath.equities.pricer;

import java.util.Map;

import net.finmath.equities.pricer.EquityValuationRequest.CalculationRequestType;

/**
 * Class to store a pricing result provided by implementations of the IOptionPricer interface.
 *
 * @author Andreas Grotz
 */

public class EquityValuationResult {

	private final EquityValuationRequest request;
	private final Map<CalculationRequestType, Double> results;

	public EquityValuationResult(
			final EquityValuationRequest request, final Map<CalculationRequestType, Double> results2)
	{
		this.request = request;
		this.results = results2;
	}
}
