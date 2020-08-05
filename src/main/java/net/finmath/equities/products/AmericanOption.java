package net.finmath.equities.products;

import java.time.LocalDate;

/**
 * Class for American options.
 * 
 * @author Andreas Grotz
 */

public class AmericanOption extends Option {
	
	public AmericanOption(LocalDate expiryDate, double strike, boolean isCallOption) {
		super(expiryDate, strike, isCallOption);
	}

	public boolean isAmericanOption() {
		return true;
	}
}