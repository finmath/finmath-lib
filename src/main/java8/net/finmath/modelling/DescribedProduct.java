package net.finmath.modelling;

/**
 * Interface for products which can provide a complete description of themself, i.e. the model parameters (independent of the implementation of the numerical method).
 *
 * @author Christian Fries
 *
 * @param <T> An interface extending the <code>ProductDescriptor</code> interface, being rich enough to describe the product implementing this interface.
 * @version 1.0
 */
public interface DescribedProduct<T extends ProductDescriptor> extends Product {

	/**
	 * Return a product descriptor representing this product.
	 *
	 * @return The product descriptor of this product.
	 */
	T getDescriptor();
}
