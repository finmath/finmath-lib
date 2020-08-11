package net.finmath.equities.products;

import java.time.LocalDate;

/**
 * Interface to handle American and European options.
 *
 * @author Andreas Grotz
 */

public interface IOption extends Cloneable {

	double getStrike();

	LocalDate getExpiryDate();

	boolean isCallOption();

	boolean isAmericanOption();

	double callPutFactor();

	double getPayoff(double spot);
}
