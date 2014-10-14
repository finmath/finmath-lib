/*
 * Created on 06.12.2009
 *
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 */
package net.finmath.montecarlo.interestrate.products.indices;

import java.util.HashSet;
import java.util.Set;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * An index throwing an exception if his <code>getValue</code> method is called.
 * 
 * This class can be used to created indices which triggers an exception only upon usage (i.e., late).
 * 
 * A typical use case is a financial product referencing an index in one of its past payments,
 * which is not supported by the current model. In that case the product can be valued as of a future
 * date, but no necessarily as of a past date.
 * 
 * @author Christian Fries
 */
public class UnsupportedIndex extends AbstractIndex {

	private static final long serialVersionUID = 5375406324063846793L;
	private final Exception exception;
	
	/**
	 * Creates an unsupported index throwing an exception if his <code>getValue</code> method is called.
	 * 
	 * @param name The name of the index.
	 * @param exception The exception to be thrown if this index is valued.
	 */
	public UnsupportedIndex(String name, Exception exception) {
		super(name, null);
		this.exception = exception;
	}

	/**
	 * Creates an unsupported index throwing an exception if his <code>getValue</code> method is called.
	 * 
	 * @param exception The exception.
	 */
	public UnsupportedIndex(Exception exception) {
		super();
		this.exception = exception;
	}

	@Override
	public RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {
		throw new CalculationException(exception);
	}

	@Override
	public Set<String> queryUnderlyings() {
		if(getName() == null) return null;

		Set<String> underlying = new HashSet<String>();
		underlying.add(getName());
		return underlying;
	}
}
