package net.finmath.finitedifference.assetderivativevaluation.boundaries;

import net.finmath.finitedifference.assetderivativevaluation.products.FiniteDifferenceEquityProduct;

/**
 * Factory interface for creating {@link FiniteDifferenceBoundary}
 * instances associated with a given finite difference product.
 *
 * @author Andrea Mazzon
 */
public interface FiniteDifferenceBoundaryFactory {

	/**
	 * Creates a {@link FiniteDifferenceBoundary} for the specified product.
	 *
	 * @param product The finite difference product.
	 * @return The corresponding boundary implementation.
	 */
	FiniteDifferenceBoundary createBoundary(FiniteDifferenceEquityProduct product);
}
