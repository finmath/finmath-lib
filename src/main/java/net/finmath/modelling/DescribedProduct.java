package net.finmath.modelling;

public interface DescribedProduct<T extends ProductDescriptor> extends ProductInterface {

	/**
	 * Return a product descriptor representing this product.
	 *
	 * @return The product descriptor of this product.
	 */
	T getDescriptor();

}
