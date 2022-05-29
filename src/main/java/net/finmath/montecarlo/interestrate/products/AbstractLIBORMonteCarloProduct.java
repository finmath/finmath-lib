/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 10.02.2004
 */
package net.finmath.montecarlo.interestrate.products;

/**
 * For backward compatibility - same as AbstractTermStructureMonteCarloProduct.
 *
 * @author Christian Fries
 * @version 1.0
 */
public abstract class AbstractLIBORMonteCarloProduct extends AbstractTermStructureMonteCarloProduct {

	/**
	 * @param currency The currency of this product (may be null for "any currency").
	 */
	public AbstractLIBORMonteCarloProduct(final String currency) {
		super(currency);
	}

	/**
	 *
	 */
	public AbstractLIBORMonteCarloProduct() {
		super(null);
	}
}
