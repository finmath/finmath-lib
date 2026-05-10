package net.finmath.finitedifference;

import net.finmath.modelling.Model;
import net.finmath.modelling.Product;

/**
 * Base interface for finite-difference products.
 *
 * <p>
 * The interface abstracts the common structure shared by all finite-difference
 * products, independently of the asset class. A product is parameterized by the
 * specific finite-difference model type against which it can be valued.
 * </p>
 *
 * @param <M> The finite-difference model type compatible with the product.
 *
 * @author Alessandro Gnoatto
 */
public interface FiniteDifferenceProduct<M extends FiniteDifferenceModel> extends Product {

	/**
	 * Returns the value.
	 *
	 * @param evaluationTime The value.
	 * @param model The value.
	 * @return The value.
	 */
	double[] getValue(double evaluationTime, M model);

	/**
	 * Returns the value.
	 *
	 * @param model The value.
	 * @return The value.
	 */
	double[][] getValues(M model);

	/**
	 * Returns the value.
	 *
	 * @return The value.
	 */
	Class<M> getModelClass();

	@Override
	default Object getValue(final double evaluationTime, final Model model) {

		if (getModelClass().isInstance(model)) {
			return getValue(evaluationTime, getModelClass().cast(model));
		} else {
			throw new IllegalArgumentException(
					"The product " + this.getClass()
					+ " cannot be valued against a model "
					+ model.getClass() + ". "
					+ "It requires a model of type "
					+ getModelClass() + "."
			);
		}
	}
}
