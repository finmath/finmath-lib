package net.finmath.modelling.productfactory;

import java.util.ArrayList;
import java.util.List;

import net.finmath.modelling.DescribedProduct;
import net.finmath.modelling.ProductDescriptor;
import net.finmath.modelling.ProductFactory;

/**
 * Implements a product factory based on a cascade of given factories. When invoking <code>getProductFromDescriptor(ProductDescriptor productDescriptor)</code>
 * the cascade will query all its individual factories until one builds the product. When querying the factories the cascade will start at index 0.
 *
 * @author Christian Fries
 * @author Roland Bachl
 *
 * @param <T> The base class of the product descriptors which can be handled by this <code>ProductFactory</code>.
 * @version 1.0
 */
public class ProductFactoryCascade<T extends ProductDescriptor> implements ProductFactory<T> {

	private final ArrayList<ProductFactory<? extends T>> factories;

	/**
	 * Construct an empty factory cascade. This will build no products until amended.
	 */
	public ProductFactoryCascade() {
		super();
		factories = new ArrayList<>(0);
	}

	/**
	 * Construct a factory cascade from an ordered list of product factories. When querying the factories the cascade will start at index 0.
	 *
	 * @param factories A list of product factories, i.e. object implementing <code>ProductFactory</code> for the product descriptor <code>T</code>.
	 */
	public ProductFactoryCascade(final List<ProductFactory<? extends T>> factories) {
		super();
		this.factories = new ArrayList<>();
		this.factories.addAll(factories);
	}

	/**
	 * Add a given factory to the list of factories at the BEGINNING.
	 *
	 * @param factory The factory to be added.
	 * @return Cascade with amended factory list.
	 */
	public ProductFactoryCascade<T> addFactoryBefore(final ProductFactory<? extends T> factory) {
		final ArrayList<ProductFactory<? extends T>> factories = new ArrayList<>(this.factories.size()+1);
		factories.addAll(this.factories);
		factories.add(0, factory);
		return new ProductFactoryCascade<>(factories);
	}

	/**
	 * Add a given factory to the list of factories at the END.
	 *
	 * @param factory The factory to be added.
	 * @return Cascade with amended factory list.
	 */
	public ProductFactoryCascade<T> addFactoryAfter(final ProductFactory<? extends T> factory) {
		final ArrayList<ProductFactory<? extends T>> factories = new ArrayList<>(this.factories.size()+1);
		factories.addAll(this.factories);
		factories.add(factory);
		return new ProductFactoryCascade<>(factories);
	}

	@Override
	public DescribedProduct<? extends T> getProductFromDescriptor(final ProductDescriptor productDescriptor) {
		DescribedProduct<? extends T> product = null;
		for(final ProductFactory<? extends T> factory : factories) {
			try {
				product = factory.getProductFromDescriptor(productDescriptor);
			} catch ( final IllegalArgumentException e) {continue;}
			if(product != null) {
				return product;
			}
		}

		// Product not found
		throw new IllegalArgumentException("Unsupported product type " + productDescriptor.name());
	}
}
