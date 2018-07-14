/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 03.09.2006
 */
package net.finmath.montecarlo.interestrate.products.components;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * Base class for product components.
 *
 * Product components are small functions mapping a vector of
 * random variables to a random variable.
 *
 * Components are numeraire adjusted and can be valued on its own.
 *
 * @author Christian Fries
 */
public abstract class AbstractProductComponent extends AbstractLIBORMonteCarloProduct  implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = -916286619811716575L;

	protected static ThreadPoolExecutor executor = new ThreadPoolExecutor(
			10 + Runtime.getRuntime().availableProcessors(),
			100 + 2*Runtime.getRuntime().availableProcessors(),
			10L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new ThreadFactory() {
				@Override
				public Thread newThread(Runnable runnable) {
					Thread thread = Executors.defaultThreadFactory().newThread(runnable);
					thread.setDaemon(true);
					return thread;
				}
			});

	public AbstractProductComponent(String currency) {
		super(currency);
	}

	public AbstractProductComponent() {
		this(null);
	}

	/**
	 * Returns a set of underlying names referenced by this product component (i.e., required for valuation) or null if none.
	 *
	 * @return A set of underlying names referenced by this product component (i.e., required for valuation) or null if none.
	 */
	public abstract Set<String> queryUnderlyings();

	public Map<String, Object> getValues(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {
		RandomVariableInterface value = this.getValue(evaluationTime, model);
		Map<String, Object> result = new HashMap<>();
		result.put("value", value);
		return result;
	}
}
