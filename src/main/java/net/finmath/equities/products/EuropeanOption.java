package net.finmath.equities.products;

import java.time.LocalDate;

/**
 * Class for European options.
 * 
 * @author Andreas Grotz
 */

public class EuropeanOption extends Option {
	
	public EuropeanOption(LocalDate expiryDate, double strike, boolean isCallOption) {
		super(expiryDate, strike, isCallOption);
	}

	public boolean isAmericanOption() {
		return false;
	}
}