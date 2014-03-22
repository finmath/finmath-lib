/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 02.03.2014
 */

package net.finmath.marketdata.products;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.finmath.marketdata.model.AnalyticModelInterface;

/**
 * Implements the valuation of a portfolio of products implementing
 * <code>AnalyticProductInterface</code>.
 * 
 * @author Christian Fries
 */
public class Portfolio extends AbstractAnalyticProduct implements AnalyticProductInterface {

	private ArrayList<AnalyticProductInterface>	products;
	private ArrayList<Double>					weights;

	/**
	 * Create a portfolio of products implementing
	 * <code>AnalyticProductInterface</code>. The portfolio consists
	 * of an array of products and a corresponding array of weights.
	 * The value of the portfolio is given by the sum over
	 * <code>
	 * 	weights[i] * products.get(i).getValue(evaluationTime, model)
	 * </code>
	 * 
	 * Note that a product in the array of products may itself be
	 * a <code>Portfolio</code> (hence you may easily combine portfolios).
	 * 
	 * @param products Array of products implementing <code>AnalyticProductInterface</code>.
	 * @param weights Array of weights used in the valuation as a multiplicator.
	 */
	public Portfolio(List<AnalyticProductInterface> products, List<Double> weights) {
		super();
		this.products = new ArrayList<AnalyticProductInterface>();
		this.weights = new ArrayList<Double>();
		this.products.addAll(products);
		this.weights.addAll(weights);
	}

	/**
	 * Create a portfolio of products implementing
	 * <code>AnalyticProductInterface</code>. The portfolio consists
	 * of an array of products and a corresponding array of weights.
	 * The value of the portfolio is given by the sum over
	 * <code>
	 * 	weights[i] * products.get(i).getValue(evaluationTime, model)
	 * </code>
	 * 
	 * The portfolio is created by taking all products and weights of a given portfolio
	 * and adding other given products and weights.
	 * 
	 * @param portfolio A given portfolio, which will become part of this portfolio.
	 * @param products Array of products implementing <code>AnalyticProductInterface</code>.
	 * @param weights Array of weights used in the valuation as a multiplicator.
	 */
	public Portfolio(Portfolio portfolio, List<AnalyticProductInterface> products, List<Double> weights) {
		super();
		this.products = new ArrayList<AnalyticProductInterface>();
		this.weights = new ArrayList<Double>();
		this.products.addAll(portfolio.getProducts());
		this.weights.addAll(portfolio.getWeights());
		this.products.addAll(products);
		this.weights.addAll(weights);
	}
	
	/**
	 * Create a portfolio consisting of a single product with a given weight.
	 * @param product A product, implementing  implementing <code>AnalyticProductInterface</code>.
	 * @param weights A weight used in the valuation as a multiplicator.
	 */
	public Portfolio(AnalyticProductInterface product, double weight) {
		super();
		this.products = new ArrayList<AnalyticProductInterface>();
		this.weights = new ArrayList<Double>();
		this.products.add(product);
		this.weights.add(weight);
	}

	@Override
	public double getValue(double evaluationTime, AnalyticModelInterface model) {
		double value = 0.0;
		for(int i=0; i<products.size(); i++) value += weights.get(i) * products.get(i).getValue(evaluationTime, model);

		return value;
	}
	
	/**
	 * Returns the list of products as an unmodifiable list. Calling <code>add</code> on this list will result in an {@link UnsupportedOperationException}.
	 * 
	 * @return The list of products as an unmodifiable list.
	 */
	public List<AnalyticProductInterface> getProducts() {
		return Collections.unmodifiableList(products);
	}

	/**
	 * Returns the list of weights as an unmodifiable list. Calling <code>add</code> on this list will result in an {@link UnsupportedOperationException}.
	 * 
	 * @return The list of weights as an unmodifiable list.
	 */
	public List<Double> getWeights() {
		return Collections.unmodifiableList(weights);
	}
}
