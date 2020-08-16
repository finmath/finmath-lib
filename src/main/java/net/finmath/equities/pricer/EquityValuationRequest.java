package net.finmath.equities.pricer;

import java.util.ArrayList;

import net.finmath.equities.products.Option;

/**
 * Class to store a pricing request handed to implementations of the IOptionPricer interface.
 *
 * @author Andreas Grotz
 */

public class EquityValuationRequest {

	enum CalculationRequestType
	{
		Price,
		EqDelta,
		EqGamma,
		EqVega,
		Theta,
	}

	private final Option option;
	private final ArrayList<CalculationRequestType> calcsRequested;

	public EquityValuationRequest(final Option option, final ArrayList<CalculationRequestType> calcsRequested)
	{
		this.option = option;
		this.calcsRequested = calcsRequested;
	}

	public Option getOption() {
		return option;
	}

	public ArrayList<CalculationRequestType> getCalcsRequested() {
		return calcsRequested;
	}
}
