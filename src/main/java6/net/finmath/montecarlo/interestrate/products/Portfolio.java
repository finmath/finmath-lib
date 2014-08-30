/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 * 
 * Created on 15.02.2004
 */
package net.finmath.montecarlo.interestrate.products;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;

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
 * @version 1.1
 */
public class Portfolio extends AbstractLIBORMonteCarloProduct {
	
	private AbstractLIBORMonteCarloProduct[]	products;
	private double[]							weights;

	/**
	 * Creates a portfolio consisting of a single product and a weight.
	 * 
	 * The currency of this portfolio is the currency of the product.
	 * 
	 * @param product A product.
	 * @param weight A weight.
	 */
	public Portfolio(AbstractLIBORMonteCarloProduct product, double weight) {
		super(product.getCurrency());
		this.products = new AbstractLIBORMonteCarloProduct[] { product };
		this.weights = new double[] { weight };
	}

	/**
	 * Creates a portfolio consisting of a set of products and a weights.
	 * 
	 * Note: Currently the products have to be of the same currency.
	 * 
	 * @param products An array of products.
	 * @param weights An array of weights (having the same lengths as the array of products).
	 */
	public Portfolio(AbstractLIBORMonteCarloProduct[] products, double[] weights) {
		super();
		String currency = products[0].getCurrency();
		for(AbstractLIBORMonteCarloProduct product : products) if(!currency.equals(product.getCurrency()))
			throw new IllegalArgumentException("Product currencies do not match. Please use a constructor providing the currency of the result.");

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
	public Portfolio(String currency, AbstractLIBORMonteCarloProduct[] products, double[] weights) {
		super(currency);

		for(AbstractLIBORMonteCarloProduct product : products) if(!currency.equals(product.getCurrency()))
			throw new IllegalArgumentException("Product currencies do not match. Currency conversion (via model FX) is not supported yet.");

		this.products = products;
		this.weights = weights;
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
	public RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {
		RandomVariableInterface values = new RandomVariable(0.0);

		for(int productIndex = 0; productIndex < products.length; productIndex++) {
			RandomVariableInterface    valueOfProduct = products[productIndex].getValue(evaluationTime, model);
			double   				   weightOfProduct = weights[productIndex];

			values = values.addProduct(valueOfProduct, weightOfProduct);
		}
		return values;
	}
}
