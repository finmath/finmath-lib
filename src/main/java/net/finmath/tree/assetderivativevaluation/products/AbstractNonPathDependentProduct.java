package net.finmath.tree.assetderivativevaluation.products;

import net.finmath.stochastic.RandomVariable;
import net.finmath.tree.AbstractTreeProduct;
import net.finmath.tree.TreeModel;

import java.util.function.DoubleUnaryOperator;


/**
 * Base class for non–path-dependent options priced on a TreeModel.
 * The payoff is fully specified by a DoubleUnaryOperator acting on the spot
 * at maturity (e.g. s -> Math.max(s-K,0) for a call). Early–exercise features
 * (if any) and the backward induction logic are delegated to subclasses via
 * getValues(double, TreeModel).
 * 
 * @author Carlo Andrea Tramentozzi
 */
public abstract class AbstractNonPathDependentProduct extends AbstractTreeProduct {

	/** Payoff function f(S) applied at maturity (or when exercised). */
	private final DoubleUnaryOperator payOffFunction;

	/**
	 * Creates a non–path-dependent option with a given maturity and payoff.
	 *
	 * @param maturity
	 *        Contract maturity (model time units), typically T.
	 * @param payOffFunction
	 *        Payoff function f(S) (e.g. call/put); must not be null.
	 */
	public AbstractNonPathDependentProduct(double maturity, DoubleUnaryOperator payOffFunction ){
		super(maturity);
		this.payOffFunction = payOffFunction;
	}

	/**
	 * Returns the payoff function f(S).
	 *
	 * @return The payoff function.
	 */
	protected DoubleUnaryOperator getPayOffFunction(){
		return payOffFunction;
	}

	/**
	 * Converts a model time to the corresponding lattice time index by rounding
	 * to the nearest step: index = round(time / model.getTimeStep()).
	 * This helper is useful to map maturity (or any evaluation time aligned to the
	 * model discretization) to an integer level of the tree.
	 * @param maturity A model time (e.g. T) to map to a lattice index.
	 * @param model The tree model providing the time step.
	 * @return The nearest lattice index for the given time.
	 */
	protected final int timeToIndex(double maturity, TreeModel model){
		return (int)Math.round(maturity/model.getTimeStep());
	}

	/**
	 * Computes the vector of option values on the lattice (via backward induction).
	 * @param evaluationTime The time at which valuation is performed (e.g. 0.0).
	 * @param model The tree model to use; must not be null.
	 * @return The array of value random variables per time level.
	 */
	@Override
	public abstract RandomVariable[] getValues(double evaluationTime, TreeModel model);
}
