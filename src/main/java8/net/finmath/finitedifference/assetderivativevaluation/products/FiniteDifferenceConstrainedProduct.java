package net.finmath.finitedifference.assetderivativevaluation.products;

/**
 * Marker-style extension of {@link FiniteDifferenceEquityProduct} for products
 * that may
 * impose internal state constraints.
 *
 * <p>
 * Products not requiring such constraints do not need to implement this
 * interface.
 * Solvers can detect support via {@code instanceof}.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public interface FiniteDifferenceConstrainedProduct
extends FiniteDifferenceEquityProduct, FiniteDifferenceInternalStateConstraint {
}
