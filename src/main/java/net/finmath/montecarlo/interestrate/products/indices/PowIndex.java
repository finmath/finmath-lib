/*
 * Created on 22.11.2009
 */
package net.finmath.montecarlo.interestrate.products.indices;

import java.util.Set;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.products.components.AbstractProductComponent;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * A power index.
 * 
 * Provides the function <i>index(t)<sup>exponent</sup></i>, i.e. pow(index(t), exponent).
 * 
 * @author Christian Fries
 * @version 1.2
 */
public class PowIndex extends AbstractIndex {

	private static final long serialVersionUID = -1512137372132830198L;

	private final AbstractProductComponent	index;
	private final double					exponent;

	/**
	 * Creates the function pow(index(t), exponent)
	 * 
	 * @param index An index.
	 * @param exponent The exponent.
	 */
	public PowIndex(AbstractProductComponent index, double exponent) {
		super();
		this.index = index;
		this.exponent = exponent;
	}

	@Override
	public RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {
		return index.getValue(evaluationTime, model).pow(exponent);
	}

	@Override
	public Set<String> queryUnderlyings() {
		return index.queryUnderlyings();
	}

	@Override
	public String toString() {
		return "PowIndex [index=" + index + ", exponent=" + exponent + "]";
	}
}
