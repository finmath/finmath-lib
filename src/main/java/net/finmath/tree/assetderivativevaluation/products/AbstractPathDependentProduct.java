package net.finmath.tree.assetderivativevaluation.products;

import net.finmath.tree.AbstractTreeProduct;
import net.finmath.tree.TreeModel;
import net.finmath.stochastic.RandomVariable;

import java.util.function.DoubleUnaryOperator;

/**
 * Base class for path–dependent tree products (e.g. Asian options).
 * It defines the common contract and utilities to:
 *   Store a payoff function f(A) or f(S) depending on the product
 *   Map continuous times to the model’s discrete time index
 *   Expose convenience getters for values at a given evaluation time.
 */

public abstract class AbstractPathDependentProduct extends AbstractTreeProduct {

	/** Payoff function applied where appropriate (e.g. on a running average A). */
	private final DoubleUnaryOperator payOffFunction;

	/**
	 * Creates a path–dependent product.
	 *
	 * @param maturity        Contract maturity in model time units.
	 * @param payOffFunction  Payoff function (e.g., a -> Math.max(a-K, 0.0) for an Asian call).
	 */
	public AbstractPathDependentProduct(double maturity, DoubleUnaryOperator payOffFunction) {
		super(maturity);
		this.payOffFunction = payOffFunction;
	}

	/** Getter for the payoff function used by concrete implementations. */
	protected DoubleUnaryOperator getPayOffFunction(){
		return payOffFunction;
	}

	/**
	 * Maps a (continuous) time to the closest model time index using the model time step.
	 *
	 * @param maturity A time on the model grid (e.g., evaluation time or maturity).
	 * @param model    The tree model providing the time step.
	 * @return The integer time index k =  maturity / dt.
	 */
	protected final int timeToIndex(double maturity, TreeModel model) {
		return (int) Math.round(maturity / model.getTimeStep());
	}

	/**
	 * Convenience scalar price at t = 0.
	 *
	 * @param model The tree model.
	 * @return The (scalar) price at time 0 obtained from getValue(double, TreeModel).
	 */
	public double getValue(TreeModel model ){
		RandomVariable v0 = getValue(0.0,model);
		return v0.get(0);
	}


	/**
	 * Convenience accessor returning the value vector at a given evaluation time.
	 * This delegates to getValues(double, TreeModel) and returns its first element,
	 * which – by convention – corresponds to the evaluation time.
	 *
	 * @param evaluationTime Evaluation time (usually 0.0).
	 * @param model           The tree model.
	 * @return The RandomVariable holding the option value across states at evalutationTime.
	 */
	public RandomVariable getValue(double evaluationTime ,TreeModel model) {
		RandomVariable[] levels = getValues(evaluationTime,model);
		return levels[0];
	}

	/**
	 * Computes the value process on the lattice from evaluationTime to maturity.
	 * Implementations :
	 * Build the path–dependent state (running average) where needed, Apply the payoff at maturity,
	 * Propagate values backward using the model’s discounted conditional expectation, possibly
	 * with approximations/projections specific to the product.
	 * @param evaluationTime The time at which the value is requested.
	 * @param model          The tree model.
	 * @return An array of RandomVariable from evaluationTime (index 0) to maturity (last index).
	 */
	public abstract RandomVariable[] getValues(double evaluationTime, TreeModel model);

}
