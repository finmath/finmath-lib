/*
 * Created on 06.12.2009
 */
package net.finmath.montecarlo.interestrate.products.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

import net.finmath.concurrency.FutureWrapper;
import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.AbstractMonteCarloProduct;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * A collection of product components (like periods, options, etc.) paying the sum of their payouts.
 * 
 * @author Christian Fries
 * @version 1.1
 */
public class ProductCollection extends AbstractProductComponent {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3058874897795789705L;
	private Collection<AbstractProductComponent> products;

	/**
	 * Creates a collection of product components paying the sum of their payouts.
	 * 
	 * @param products Array of AbstractProductComponent objects
	 */
	public ProductCollection(AbstractProductComponent... products) {
		this(Arrays.asList(products));
	}

	/**
	 * Creates a collection of product components paying the sum of their payouts.
	 * 
	 * @param products Collection of AbstractProductComponent objects
	 */
	public ProductCollection(Collection<AbstractProductComponent> products) {
		super();
		this.products = products;
	}

	@Override
	public String getCurrency() {
		// @TODO: We report only the currency of the first item.
		return products.iterator().next().getCurrency();
	}

	/**
	 * Returns the collection containing all products as an unmodifiable collection.
	 * 
	 * @return the collection containing all products.
	 */
	public Collection<AbstractProductComponent> getProducts() {
		return Collections.unmodifiableCollection(products);
	}

	@Override
	public Set<String> queryUnderlyings() {
		Set<String> underlyingNames = null;
		for(AbstractProductComponent product : products) {
			Set<String> productUnderlyingNames = product.queryUnderlyings();
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
	 * @param evaluationTime The time on which this products value should be observed.
	 * @param model The model used to price the product.
	 * @return The random variable representing the value of the product discounted to evaluation time
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method. 
	 * @see net.finmath.montecarlo.AbstractMonteCarloProduct#getValue(double, net.finmath.montecarlo.MonteCarloSimulationInterface)
	 */
	@Override
	public RandomVariableInterface getValue(final double evaluationTime, final LIBORModelMonteCarloSimulationInterface model) throws CalculationException {

		// Ignite asynchronous calculation if possible
		ArrayList< Future<RandomVariableInterface> > results = new ArrayList< Future<RandomVariableInterface> >();
		for(final AbstractMonteCarloProduct product : products) {
			Future<RandomVariableInterface> valueFuture;
			try {
				valueFuture = executor.submit(
						new Callable<RandomVariableInterface>() {
							public RandomVariableInterface call() throws CalculationException {
								return product.getValue(evaluationTime, model);
							}    					
						}
						);
			}
			catch(RejectedExecutionException e) {
				valueFuture = new FutureWrapper<RandomVariableInterface>(product.getValue(evaluationTime, model));
			}

			results.add(valueFuture);
		}

		// Collect results
		RandomVariableInterface values = model.getRandomVariableForConstant(0.0);
		try {
			for(Future<RandomVariableInterface> valueFuture : results) {
				values = values.add(valueFuture.get());
			}
		} catch (InterruptedException e) {
			throw e.getCause() instanceof CalculationException ? (CalculationException)(e.getCause()) : new CalculationException(e.getCause());
		} catch (ExecutionException e) {
			if(CalculationException.class.isInstance(e.getCause())) {
				throw (CalculationException)(e.getCause());
			}
			else if(RuntimeException.class.isInstance(e.getCause())) {
				throw (RuntimeException)(e.getCause());
			}
			else {
				throw new CalculationException(e.getCause());
			}
		}

		// Return values
		return values;
	}

	@Override
	public String toString() {
		return "ProductCollection [products=" + products + "]";
	}
}
