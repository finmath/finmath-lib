package net.finmath.modelling;

public interface DescribedModel<M extends ModelDescriptor> extends ModelInterface {

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