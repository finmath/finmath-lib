package net.finmath.modelling;

/**
 * Interface for models which can provide a complete description of their model parameters (independent of the implementation of the numerical method).
 *
 * @author Christian Fries
 *
 * @param <M> An interface extending the <code>ModelDescriptor</code> interface, being rich enough to describe the model implementing this interface.
 * @version 1.0
 */
public interface DescribedModel<M extends ModelDescriptor> extends Model {

	/**
	 * Return a model descriptor representing this model.
	 *
	 * @return The model descriptor of this model.
	 */
	M getDescriptor();

	/**
	 * Construct a product from a product descriptor, which may be valued by this model.
	 *
	 * @param productDescriptor Given product descriptor.
	 * @return An instance of a product implementation.
	 */
	DescribedProduct<? extends ProductDescriptor> getProductFromDescriptor(ProductDescriptor productDescriptor);
}
