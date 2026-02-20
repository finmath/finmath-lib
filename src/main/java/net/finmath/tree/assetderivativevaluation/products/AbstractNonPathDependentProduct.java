package net.finmath.tree.assetderivativevaluation.products;

import net.finmath.stochastic.RandomVariable;
import net.finmath.tree.AbstractTreeProduct;
import net.finmath.tree.TreeModel;

import java.util.function.DoubleUnaryOperator;


/**
 * Base class for non–path-dependent options priced on a TreeModel.
 * Early–exercise features(if any) and the backward induction logic are delegated to subclasses via
 * getValues(double, TreeModel).
 * 
 * The payoff function is protected so that we can define specialized subclasses
 * for the most important products. This is the approach followed in the class
 * EuropeanOption
 * 
 * @author Carlo Andrea Tramentozzi
 * @author Alessandro Gnoatto
 */
public abstract class AbstractNonPathDependentProduct extends AbstractTreeProduct {

	/**
	 * Creates a non–path-dependent option with a given maturity and payoff.
	 *
	 * @param maturity
	 *        Contract maturity (model time units), typically T.
	 */
	public AbstractNonPathDependentProduct(double maturity){
		super(maturity);
	}

	/**
	 * Returns the payoff function f(S).
	 *
	 * @return The payoff function.
	 */
	public abstract DoubleUnaryOperator getPayOffFunction();

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
