/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 02.03.2014
 */

package net.finmath.marketdata.products;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

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

	private final static int chunkSize			= 20;
	private final static int numberOfThreads	= 32;
	private static ExecutorService				executorService = Executors.newFixedThreadPool(numberOfThreads, new ThreadFactory() {
		@Override
		public Thread newThread(Runnable runnable) {
			Thread thread = Executors.defaultThreadFactory().newThread(runnable);
			thread.setDaemon(true);
			return thread;
		}
	});

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
	 * @param weight A weight used in the valuation as a multiplicator.
	 */
	public Portfolio(AnalyticProductInterface product, double weight) {
		super();
		this.products = new ArrayList<AnalyticProductInterface>();
		this.weights = new ArrayList<Double>();
		this.products.add(product);
		this.weights.add(weight);
	}

	@Override
	public double getValue(final double evaluationTime, final AnalyticModelInterface model) {
		double value = 0.0;

		if(products.size() < 3 * chunkSize) {
			for(int j=0; j<products.size(); j++) {
				value += weights.get(j) * products.get(j).getValue(evaluationTime, model);
			}
			return value;
		}
		else {
			ArrayList<Future<Double>>	values = new ArrayList<Future<Double>>();
			for(int i=0; i<products.size(); i+=chunkSize) {
				final int start = i;

				values.add(i/chunkSize, executorService.submit(new Callable<Double>() {
					public Double call() {
						double value = 0.0;
						for(int j=start; j<Math.min(products.size(),start+chunkSize); j++) {
							value += weights.get(j) * products.get(j).getValue(evaluationTime, model);
						}
						return value;
					}
				}));
			}

			try {
				for(int i=0; i<values.size(); i++) {
					value += values.get(i).get().doubleValue();
				}
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			} catch (ExecutionException e) {
				throw new RuntimeException(e);
			}
		}
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
