/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 06.12.2015
 */
package net.finmath.marketdata2.products;

import net.finmath.marketdata2.model.AnalyticModel;
import net.finmath.stochastic.RandomVariable;

/**
 * Implements an analytic product given by the ratio
 * of two analytic products.
 *
 * This class may become handy when constructing calibrations products.
 * For example: The swap rate is the performance of a <code>SwapLeg</code> relative to a <code>SwapAnnuity</code>.
 *
 * Note: In the strict sense, the performance is an index an not a product (e.g. it does not have a currency unit).
 * With respect to the implementation we do not make a difference here and implement the AbstractAnalyticProduct interface.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class Performance extends AbstractAnalyticProduct implements AnalyticProduct {

	private final AbstractAnalyticProduct productNumerator;
	private final AbstractAnalyticProduct productDenominator;

	/**
	 * Creates a Performance product.
	 *
	 * @param productNumerator A product implementing AbstractAnalyticProduct for the numerator.
	 * @param productDenominator A product implementing AbstractAnalyticProduct for the denominator.
	 */
	public Performance(final AbstractAnalyticProduct productNumerator, final AbstractAnalyticProduct productDenominator) {
		super();
		this.productNumerator = productNumerator;
		this.productDenominator = productDenominator;
	}

	@Override
	public RandomVariable getValue(final double evaluationTime, final AnalyticModel model) {

		final RandomVariable valueNumerator	= productNumerator.getValue(evaluationTime, model);
		final RandomVariable valueDenominator	= productDenominator.getValue(evaluationTime, model);

		final RandomVariable value = valueNumerator.div(valueDenominator);
		return value;
	}

	@Override
	public String toString() {
		return "Performance [productNumerator=" + productNumerator
				+ ", productDenominator=" + productDenominator + "]";
	}
}
