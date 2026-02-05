package net.finmath.tree;

import net.finmath.stochastic.RandomVariable;

/**
 * Base class for products that can be priced on a (recombining) with TreeModel.
 * This abstraction provides a small template:
 *   getValue(TreeModel) returns the scalar time–0 price.
 *   getValue(double, TreeModel) returns the value as a RandomVariable at a given evaluation time.
 *   getValues(double, TreeModel) must be implemented by subclasses and
 *       should return the full vector of values (per state) at each time level,
 *       produced by a backward induction. By convention the element at index
 *       0 corresponds to time 0.
 * Subclasses (e.g. European, American, Asian ) implement their
 * payoff logic and early–exercise features within getValues(double, TreeModel).
 * 
 * @author Carlo Andrea Tramentozzi
 * @author Alessandro Gnoatto
 * @author Andrea Mazzon
 */
public abstract class AbstractTreeProduct implements TreeProduct {

	/** Contract maturity (in model time units). */
	private final double maturity;

	/**
	 * Creates a tree-priced product with the given maturity.
	 *
	 * @param maturity The contract maturity
	 * must be compatible with the model time grid.
	 */
	public AbstractTreeProduct(double maturity){
		this.maturity = maturity;
	}

	/**
	 * Returns the time–0 present value under the given model
	 * This is a convenience method delegating to getValue(double, TreeModel)}ù
	 * with evaluationTime = 0.0 and extracting the scalar value from
	 * the returned RandomVariable.
	 *
	 * @param model The tree model to use (providing discounting and conditional expectations).
	 * @return The time–0 price.
	 * @throws IllegalArgumentException If model is null or the evaluation fails.
	 */
	@Override
	public double getValue(TreeModel model ){
		RandomVariable v0 = getValue(0.0,model);
		return v0.get(0);
	}
	/**
	 * Returns the product value at a given evaluation time as a RandomVariable.
	 * The default implementation calls getValues(double, TreeModel) and
	 * returns the first component (time–level 0). Subclasses should ensure
	 * their {@link #getValues(double, TreeModel)} respects this convention.
	 * Internally, the method creates all values from time zero and returns the level
	 * of interest. While this is inefficient for non-path-dependent products, it ensures
	 * a correct implementation for path dependent products. The alternative would be to provide as 
	 * input the current value of the path-dependent functional, which would break the interface.
	 *
	 * @param evaluationTime The time at which the value is requested (must be >= 0).
	 * @param model The tree model to use.
	 * @return The value at evalutationTime as a random variable on the tree.
	 * @throws IllegalArgumentException If the inputs are invalid (see validate(double, TreeModel)).
	 */
	public RandomVariable getValue(double evaluationTime, TreeModel model) {
		RandomVariable[] levels = getValues(0.0,model);
		//This is dangerous as it assumes a uniform time discretization
		int index = (int) Math.round(evaluationTime / model.getTimeStep());
		return levels[index];
	}

	/**
	 * Computes the vector of values at each time/state on the tree (via backward induction)
	 * Implementations should:
	 * Validate inputs via validate(double, TreeModel).
	 * Return an array whose element at index k is a RandomVariable
	 *       representing the value at time level k (with length equal to the number of states at that level).
	 *
	 * @param evaluationTime The time at which valuation is performed
	 * @param model The tree model (spot lattice, discounting, conditional expectation).
	 * @return An array of value random variables, one per time level, indexed from 0 to maturity level.
	 */
	public abstract RandomVariable[] getValues(double evaluationTime, TreeModel model);

	/**
	 * Basic input validation helper used by implementations.
	 *
	 * @param t The evaluation time (must be >= 0).
	 * @param m The model (must not be null).
	 * @throws IllegalArgumentException If {@code m == null} or {@code t < 0}.
	 */
	protected void validate(double t, TreeModel m) {
		if(m == null) throw new IllegalArgumentException("model null");
		if(t < 0)     throw new IllegalArgumentException("evaluationTime < 0");
	}


	/**
	 * Returns the contract maturity.
	 *
	 * @return The maturity T.
	 */
	protected double getMaturity(){
		return maturity;
	}

}

