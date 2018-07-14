/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 17.08.2013
 */

package net.finmath.montecarlo.products;

import java.util.Arrays;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.AbstractMonteCarloProduct;
import net.finmath.montecarlo.MonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * A portfolio of products, each product being of AbstractMonteCarloProduct type.
 * The valuation is performed multi-threaded over the portfolio of products.
 *
 * @author Christian Fries
 */
public class PortfolioMonteCarloProduct extends AbstractMonteCarloProduct {

	private AbstractMonteCarloProduct[] products;
	private double weights[];

	/**
	 * Create a portfolio of products, each product being of AbstractMonteCarloProduct type.
	 *
	 * @param products An array of products.
	 */
	public PortfolioMonteCarloProduct(
			AbstractMonteCarloProduct[] products) {
		this(products, weightsOfOne(products.length));
	}

	/**
	 * Create a portfolio of products, each product being of AbstractMonteCarloProduct type
	 * and weighted with a given weight.
	 *
	 * @param products An array of products.
	 * @param weights An array of weights.
	 */
	public PortfolioMonteCarloProduct(
			AbstractMonteCarloProduct[] products,
			double[] weights) {
		super();
		this.products = products;
		this.weights = weights;
	}

	/**
	 * Helper returns an array of 1.0's with given length.
	 *
	 * @param length Length of the array.
	 * @return Array of double with given length, each entry being 1.0.
	 */
	private static double[] weightsOfOne(int length) {
		double[] weightsOfOne = new double[length];
		Arrays.fill(weightsOfOne, 1.0);
		return weightsOfOne;
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.AbstractMonteCarloProduct#getValue(double, net.finmath.montecarlo.MonteCarloSimulationInterface)
	 */
	@Override
	public RandomVariableInterface getValue(final double evaluationTime, final MonteCarloSimulationInterface model) throws CalculationException {

		if(products == null || products.length == 0) {
			return null;
		}

		// We do not allocate more threads the twice the number of processors.
		int numberOfThreads = Math.min(Math.max(2 * Runtime.getRuntime().availableProcessors(),1),products.length);
		ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

		RandomVariableInterface value = null;
		try {
			// Start calculation threads for each product
			Vector<Future<RandomVariableInterface>> values = new Vector<Future<RandomVariableInterface>>(products.length);
			for(int i=0; i<products.length; i++) {
				final AbstractMonteCarloProduct product = products[i];
				final double weight = weights[i];

				Callable<RandomVariableInterface> worker = new  Callable<RandomVariableInterface>() {
					public RandomVariableInterface call() throws CalculationException {
						return product.getValue(evaluationTime, model).mult(weight);
					}
				};
				executor.submit(worker);
			}

			// Collect and sum results
			value = values.get(0).get();
			for(int i=1; i<products.length; i++) {
				value = value.add(values.get(i).get());
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} finally {
			executor.shutdown();
		}

		return value;
	}
}

