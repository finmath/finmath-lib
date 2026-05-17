package net.finmath.tree;

/**
 * Contract for a product that can be priced on a (recombining) tree model.
 * Implementations encapsulate the payoff and exercise style
 * (e.g. European, American, path-dependent) and use the TreeModel's
 * risk-neutral dynamics and discounting to compute a present value.
 * */

public interface TreeProduct {

	/**
	 * Prices this product under the given tree model.
	 *
	 * @param model
	 *        The tree model providing spot levels, discounting and
	 *        conditional expectations (e.g. CRR, Jarrow–Rudd, Leisen–Reimer,
	 *        or a trinomial model); must not be {@code null}.
	 * @return The present value (time 0) of the product under {@code model},
	 *         in the same currency units as the model’s numéraire.
	 */
	double getValue(TreeModel model );
}
