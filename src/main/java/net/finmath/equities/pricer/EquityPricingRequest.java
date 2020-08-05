package net.finmath.equities.pricer;

import java.util.ArrayList;

import net.finmath.equities.products.IOption;

/**
 * Class to store a pricing request handed to implementations of the IOptionPricer interface.
 * 
 * @author Andreas Grotz
 */

public class EquityPricingRequest {
	
	enum CalculationRequestType
	{
		Price,
		EqDelta,
		EqGamma,
		EqVega,
		Theta,
	}
	
	public final IOption option;
	public final ArrayList<CalculationRequestType> calcsRequested;
	
	public EquityPricingRequest(final IOption option, final ArrayList<CalculationRequestType> calcsRequested)
	{
		this.option = option;
		this.calcsRequested = calcsRequested;
	}
}