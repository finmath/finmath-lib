/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 02.03.2014
 */

package net.finmath.marketdata2.products;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.finmath.marketdata2.model.AnalyticModel;
import net.finmath.stochastic.RandomVariable;

/**
 * Implements the valuation of a portfolio of products implementing
 * <code>AnalyticProductInterface</code>.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class Portfolio extends AbstractAnalyticProduct implements AnalyticProduct {

	private final ArrayList<AnalyticProduct>	products;
	private final ArrayList<Double>					weights;

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
	public Portfolio(final List<AnalyticProduct> products, final List<Double> weights) {
		super();
		this.products = new ArrayList<>();
		this.weights = new ArrayList<>();
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
	public Portfolio(final Portfolio portfolio, final List<AnalyticProduct> products, final List<Double> weights) {
		super();
		this.products = new ArrayList<>();
		this.weights = new ArrayList<>();
		this.products.addAll(portfolio.getProducts());
		this.weights.addAll(portfolio.getWeights());
		this.products.addAll(products);
		this.weights.addAll(weights);
	}

	/**
	 * Create a portfolio consisting of a single product with a given weight.
	 *
	 * @param product A product, implementing  implementing <code>AnalyticProductInterface</code>.
	 * @param weight A weight used in the valuation as a multiplicator.
	 */
	public Portfolio(final AnalyticProduct product, final double weight) {
		super();
		products = new ArrayList<>();
		weights = new ArrayList<>();
		products.add(product);
		weights.add(weight);
	}

	/**
	 * Create a portfolio of products implementing
	 * <code>AnalyticProductInterface</code>.
	 *
	 * The value of the portfolio is given by the sum over
	 * <code>
	 * 	products.get(i).getValue(evaluationTime, model)
	 * </code>
	 *
	 * Note that a product in the array of products may itself be
	 * a <code>Portfolio</code> (hence you may easily combine portfolios).
	 *
	 * @param products Array of products implementing <code>AnalyticProductInterface</code>.
	 */
	public Portfolio(final List<AnalyticProduct> products) {
		this(products, Collections.nCopies(products.size(), Double.valueOf(1.0)));
	}

	@Override
	public RandomVariable getValue(final double evaluationTime, final AnalyticModel model) {
		RandomVariable value = model.getRandomVariableForConstant(0.0);

		final List<RandomVariable> productValues	= products.parallelStream().map(new Function<AnalyticProduct, RandomVariable>() {
			@Override
			public RandomVariable apply(final AnalyticProduct product) {
				return product.getValue(evaluationTime, model);
			}
		}).collect(Collectors.toList());
		final List<RandomVariable> weightsRandomVariables = weights.parallelStream().map(new Function<Double, RandomVariable>() {
			@Override
			public RandomVariable apply(final Double weight) {
				return model.getRandomVariableForConstant(weight);
			}
		}).collect(Collectors.toList());

		value = value.addSumProduct(productValues, weightsRandomVariables);

		return value;
	}

	/**
	 * Returns the list of products as an unmodifiable list.
	 * Calling <code>add</code> on this list will result in an {@link UnsupportedOperationException}.
	 *
	 * @return The list of products as an unmodifiable list.
	 */
	public List<AnalyticProduct> getProducts() {
		return Collections.unmodifiableList(products);
	}

	/**
	 * Returns the list of weights as an unmodifiable list.
	 * Calling <code>add</code> on this list will result in an {@link UnsupportedOperationException}.
	 *
	 * @return The list of weights as an unmodifiable list.
	 */
	public List<Double> getWeights() {
		return Collections.unmodifiableList(weights);
	}
}
