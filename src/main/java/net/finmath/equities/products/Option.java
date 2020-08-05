package net.finmath.equities.products;

import java.time.LocalDate;

/**
 * Abstract class to handle common methods and members for American and European options.
 * 
 * @author Andreas Grotz
 */

public abstract class Option implements IOption {
	
	private final LocalDate expiryDate;
	private final double strike;
	private final boolean isCallOption;
	
	public Option(
		final LocalDate expiryDate,
		final double strike,
		final boolean isCallOption)
	{
		this.expiryDate = expiryDate;
		this.strike = strike;
		this.isCallOption = isCallOption;
	}
	
	public final LocalDate getExpiryDate() {
		return expiryDate;
	}
	
	public final double getStrike() {
		return strike;
	}
	
	public final boolean isCallOption() {
		return isCallOption;
	}
	
	public final double callPutFactor() {
		if (isCallOption)
			return 1.0;
		else
			return -1.0;
	}
	
	public final double getPayoff(double spot) {
		return Math.max(callPutFactor() * (spot - strike),0);
	}
}