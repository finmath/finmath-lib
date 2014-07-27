/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 03.09.2006
 */
package net.finmath.montecarlo.interestrate.products.components;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.SynchronousQueue;
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

	public static ThreadPoolExecutor executor = new ThreadPoolExecutor(16, 16, 10L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());

	public AbstractProductComponent() {
		super();
	}

	public Map<String, Object> getValues(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {
		RandomVariableInterface value = this.getValue(evaluationTime, model);
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("value", value);
		return result;
	}
}
