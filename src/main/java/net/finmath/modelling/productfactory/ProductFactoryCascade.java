package net.finmath.modelling.productfactory;

import java.util.ArrayList;
import java.util.Collection;

import net.finmath.modelling.DescribedProduct;
import net.finmath.modelling.ProductDescriptor;
import net.finmath.modelling.ProductFactory;

/**
 * Implements a product factory based on a cascade of given factories.
 *
 * @author Christian Fries
 * @author Roland Bachl
 *
 * @param <T> The base class of the product descriptors which can be handled by this <code>ProductFactory</code>.
 */
public abstract class ProductFactoryCascade<T extends ProductDescriptor> implements ProductFactory<T> {

	private ArrayList<ProductFactory<T>> factories;

	public ProductFactoryCascade() {
		super();
		factories = new ArrayList<ProductFactory<T>>(1);
	}

	public ProductFactoryCascade(Collection<ProductFactory<T>> factories) {
		super();
		this.factories = new ArrayList<ProductFactory<T>>();
		this.factories.addAll(factories);
	}

	/* (non-Javadoc)
	 * @see net.finmath.modelling.DescribedModel#getProductFromDesciptor(net.finmath.modelling.ProductDescriptor)
	 * @TODO: Fix unchecked cast
	 */
	public DescribedProduct<? extends T> getProductFromDescriptor(ProductDescriptor productDescriptor) {
		DescribedProduct<T> product;
		for(ProductFactory<?> factory : factories) {
			product = (DescribedProduct<T>) factory.getProductFromDescriptor(productDescriptor);
			if(product != null) {
				return product;
			}
		}

		// Product not found
		throw new IllegalArgumentException("Unsupported product type " + productDescriptor.name());
	}
}
