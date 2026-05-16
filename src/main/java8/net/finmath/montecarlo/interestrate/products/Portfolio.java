/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 15.02.2004
 */
package net.finmath.montecarlo.interestrate.products;

import java.util.Set;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.products.components.AbstractProductComponent;
import net.finmath.stochastic.RandomVariable;

/**
 * Implements the pricing of a portfolio of AbstractLIBORMonteCarloProduct products
 * under a AbstractLIBORMarketModel. The products can be scaled by weights.
 *
 * The value of the portfolio is that of
 * \( \sum_{i=0}^{n} weights\[i\] \cdot products\[i\] \text{.} \)
 *
 * Note: Currently the products have to be of the same currency.
 *
 * @author Christian Fries
 * @date 08.09.2006
 * @version 1.2
 */
public class Portfolio extends AbstractProductComponent {

	private static final long serialVersionUID = -1360506093081238482L;

	private final AbstractTermStructureMonteCarloProduct[]	products;
	private final double[]							weights;

	/**
	 * Creates a portfolio consisting of a single product and a weight.
	 *
	 * The currency of this portfolio is the currency of the product.
	 *
	 * @param product A product.
	 * @param weight A weight.
	 */
	public Portfolio(final AbstractTermStructureMonteCarloProduct product, final double weight) {
		super(product.getCurrency());
		products = new AbstractTermStructureMonteCarloProduct[] { product };
		weights = new double[] { weight };
	}

	/**
	 * Creates a portfolio consisting of a set of products and a weights.
	 *
	 * Note: Currently the products have to be of the same currency.
	 *
	 * @param products An array of products.
	 * @param weights An array of weights (having the same lengths as the array of products).
	 */
	public Portfolio(final AbstractTermStructureMonteCarloProduct[] products, final double[] weights) {
		super();
		final String currency = products[0].getCurrency();
		for(final AbstractTermStructureMonteCarloProduct product : products) {
			if(currency != null && !currency.equals(product.getCurrency())) {
				throw new IllegalArgumentException("Product currencies do not match. Please use a constructor providing the currency of the result.");
			}
		}

		this.products = products;
		this.weights = weights;
	}

	/**
	 * Creates a portfolio consisting of a set of products and a weights.
	 *
	 * Note: Currently the products have to be of the same currency, namely the one provided.
	 *
	 * @param currency The currency of the value of this portfolio when calling <code>getValue</code>.
	 * @param products An array of products.
	 * @param weights An array of weights (having the same lengths as the array of products).
	 */
	public Portfolio(final String currency, final AbstractTermStructureMonteCarloProduct[] products, final double[] weights) {
		super(currency);

		for(final AbstractTermStructureMonteCarloProduct product : products) {
			if(!currency.equals(product.getCurrency())) {
				throw new IllegalArgumentException("Product currencies do not match. Currency conversion (via model FX) is not supported yet.");
			}
		}

		this.products = products;
		this.weights = weights;
	}

	@Override
	public String getCurrency() {
		// @TODO We report only the currency of the first item, because mixed currency portfolios are currently not allowed.
		return (products != null && products.length > 0) ? products[0].getCurrency() : null;
	}

	@Override
	public Set<String> queryUnderlyings() {
		Set<String> underlyingNames = null;
		for(final TermStructureMonteCarloProduct product : products) {
			Set<String> productUnderlyingNames;
			if(product instanceof AbstractProductComponent) {
				productUnderlyingNames = ((AbstractProductComponent)product).queryUnderlyings();
			} else {
				throw new IllegalArgumentException("Underlying cannot be queried for underlyings.");
			}

			if(productUnderlyingNames != null) {
				if(underlyingNames == null) {
					underlyingNames = productUnderlyingNames;
				} else {
					underlyingNames.addAll(productUnderlyingNames);
				}
			}
		}
		return underlyingNames;
	}

	/**
	 * This method returns the value random variable of the product within the specified model, evaluated at a given evalutationTime.
	 * Note: For a lattice this is often the value conditional to evalutationTime, for a Monte-Carlo simulation this is the (sum of) value discounted to evaluation time.
	 * Cashflows prior evaluationTime are not considered.
	 *
	 * @TODO The conversion between different currencies is currently not performed.
	 * @param evaluationTime The time on which this products value should be observed.
	 * @param model The model used to price the product.
	 * @return The random variable representing the value of the product discounted to evaluation time
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	@Override
	public RandomVariable getValue(final double evaluationTime, final TermStructureMonteCarloSimulationModel model) throws CalculationException {
		RandomVariable values = new RandomVariableFromDoubleArray(0.0);

		for(int productIndex = 0; productIndex < products.length; productIndex++) {
			final RandomVariable    valueOfProduct = products[productIndex].getValue(evaluationTime, model);
			final double   				   weightOfProduct = weights[productIndex];

			values = values.addProduct(valueOfProduct, weightOfProduct);
		}
		return values;
	}

	/**
	 * @return the products
	 */
	public TermStructureMonteCarloProduct[] getProducts() {
		return products.clone();
	}

	/**
	 * @return the weights
	 */
	public double[] getWeights() {
		return weights.clone();
	}
}
