package net.finmath.equities.products;

import java.time.LocalDate;

/**
 * Abstract class to handle common methods and members for American and European options.
 *
 * @author Andreas Grotz
 */

public abstract class AbstractOption implements Option {

	private final LocalDate expiryDate;
	private final double strike;
	private final boolean isCallOption;

	public AbstractOption(
			final LocalDate expiryDate,
			final double strike,
			final boolean isCallOption)
	{
		this.expiryDate = expiryDate;
		this.strike = strike;
		this.isCallOption = isCallOption;
	}

	@Override
	public final LocalDate getExpiryDate() {
		return expiryDate;
	}

	@Override
	public final double getStrike() {
		return strike;
	}

	@Override
	public final boolean isCallOption() {
		return isCallOption;
	}

	@Override
	public final double callPutFactor() {
		if (isCallOption) {
			return 1.0;
		} else {
			return -1.0;
		}
	}

	@Override
	public final double getPayoff(double spot) {
		return Math.max(callPutFactor() * (spot - strike),0);
	}
}
