/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 17.08.2013
 */

package net.finmath.montecarlo.products;

import java.util.Arrays;
import java.util.Optional;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.AbstractMonteCarloProduct;
import net.finmath.montecarlo.MonteCarloProduct;
import net.finmath.montecarlo.MonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;

/**
 * A portfolio of products, each product being of AbstractMonteCarloProduct type.
 * The valuation is performed multi-threaded over the portfolio of products.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class PortfolioMonteCarloProduct extends AbstractMonteCarloProduct {

	private final MonteCarloProduct[] products;
	private final double[] weights;
	private final Optional<Integer> numberOfThreads;

	/**
	 * Create a portfolio of products, each product being of AbstractMonteCarloProduct type
	 * and weighted with a given weight.
	 *
	 * @param products An array of products.
	 * @param weights An array of weights.
	 * @param numberOfThreads Number of parallel threads to used. Required to be &gt; 0.
	 */
	public PortfolioMonteCarloProduct(
			final MonteCarloProduct[] products,
			final double[] weights,
			final Optional<Integer> numberOfThreads) {
		super();
		this.products = products;
		this.weights = weights;
		this.numberOfThreads = numberOfThreads;

		if(numberOfThreads.isPresent() && numberOfThreads.get() < 1) {
			throw new IllegalArgumentException("The parameter numberOfThreads is required to be > 0 if present.");
		}
	}

	/**
	 * Create a portfolio of products, each product being of AbstractMonteCarloProduct type
	 * and weighted with a given weight.
	 *
	 * @param products An array of products.
	 * @param weights An array of weights.
	 */
	public PortfolioMonteCarloProduct(
			final MonteCarloProduct[] products,
			final double[] weights) {
		this(products, weights, Optional.empty());
	}

	/**
	 * Create a portfolio of products, each product being of AbstractMonteCarloProduct type.
	 *
	 * @param products An array of products.
	 */
	public PortfolioMonteCarloProduct(final MonteCarloProduct[] products) {
		this(products, weightsOfOne(products.length));
	}

	/**
	 * Helper returns an array of 1.0's with given length.
	 *
	 * @param length Length of the array.
	 * @return Array of double with given length, each entry being 1.0.
	 */
	private static double[] weightsOfOne(final int length) {
		final double[] weightsOfOne = new double[length];
		Arrays.fill(weightsOfOne, 1.0);
		return weightsOfOne;
	}

	@Override
	public RandomVariable getValue(final double evaluationTime, final MonteCarloSimulationModel model) throws CalculationException {

		if(products == null || products.length == 0) {
			return null;
		}

		final int numberOfThreadsEffective = numberOfThreads.orElse(Runtime.getRuntime().availableProcessors());
		final ExecutorService executor = Executors.newFixedThreadPool(numberOfThreadsEffective);

		RandomVariable value = null;
		try {
			// Start calculation threads for each product
			final Vector<Future<RandomVariable>> values = new Vector<>(products.length);
			for(int i=0; i<products.length; i++) {
				final MonteCarloProduct product = products[i];
				final double weight = weights[i];

				final Callable<RandomVariable> worker = new  Callable<RandomVariable>() {
					@Override
					public RandomVariable call() throws CalculationException {
						return product.getValue(evaluationTime, model).mult(weight);
					}
				};
				values.add(i, executor.submit(worker));
			}

			// Collect and sum results
			value = values.get(0).get();
			for(int i=1; i<products.length; i++) {
				value = value.add(values.get(i).get());
			}
		} catch (InterruptedException | ExecutionException e) {
			throw new CalculationException(e.getCause());
		} finally {
			executor.shutdown();
		}

		return value;
	}
}
