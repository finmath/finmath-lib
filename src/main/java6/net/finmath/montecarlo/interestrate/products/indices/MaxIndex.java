/*
 * Created on 22.11.2009
 */
package net.finmath.montecarlo.interestrate.products.indices;

import java.util.Arrays;
import java.util.Set;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.products.components.AbstractProductComponent;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * A maximum index.
 * 
 * Provides the function max(index1(t), index2(t), ...).
 * 
 * @author Christian Fries
 * @version 1.2
 */
public class MaxIndex extends AbstractIndex {

	private static final long serialVersionUID = -1512137372132830198L;

	private final AbstractProductComponent[] indexArguments;

	/**
	 * Creates the function max(index1(t), index2(t), ...)
	 * 
	 * @param indexArguments An arguments list of <code>AbstractProductComponent</code>s or an array thereof. A minimum number of 2 arguments is required.
	 */
	public MaxIndex(AbstractProductComponent... indexArguments) {
		super();
		if(indexArguments.length < 1) throw new IllegalArgumentException("Missing arguments. Please provide one or more arguments.");
		this.indexArguments = indexArguments;
	}

	@Override
	public RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {
		RandomVariableInterface value = indexArguments[0].getValue(evaluationTime, model);
		for(AbstractProductComponent index : indexArguments) value = value.floor(index.getValue(evaluationTime, model));
		return value;
	}

	@Override
	public Set<String> queryUnderlyings() {
		Set<String> underlyingNames = null;
		for(AbstractProductComponent product : indexArguments) {
			Set<String> productUnderlyingNames = product.queryUnderlyings();
			if(productUnderlyingNames != null) {
				if(underlyingNames == null)	underlyingNames = productUnderlyingNames;
				else						underlyingNames.addAll(productUnderlyingNames);
			}
		}
		return underlyingNames;
	}

	@Override
	public String toString() {
		return "MaxIndex [indexArguments=" + Arrays.toString(indexArguments)
				+ "]";
	}
}
