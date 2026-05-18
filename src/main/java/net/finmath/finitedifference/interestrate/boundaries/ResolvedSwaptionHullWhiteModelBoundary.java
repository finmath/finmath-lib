package net.finmath.finitedifference.interestrate.boundaries;

import net.finmath.finitedifference.boundaries.BoundaryCondition;
import net.finmath.finitedifference.interestrate.models.FDMHullWhiteModel;
import net.finmath.finitedifference.interestrate.products.FiniteDifferenceInterestRateProduct;
import net.finmath.finitedifference.interestrate.products.Swaption;

/**
 * Boundary wrapper for {@link Swaption.ResolvedSwaption} under
 * {@link FDMHullWhiteModel}.
 *
 * <p>
 * The internal product {@link Swaption.ResolvedSwaption} is only an adapter
 * used
 * to resolve exercise dates and event conditions. Boundary conditions should
 * still be taken from the original outer {@link Swaption}.
 * </p>
 *
 * <p>
 * This wrapper allows the boundary factory to keep using the runtime product
 * class name while delegating all boundary logic to
 * {@link SwaptionHullWhiteModelBoundary}.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class ResolvedSwaptionHullWhiteModelBoundary implements FiniteDifferenceInterestRateBoundary {

	/**
	 * The delegate.
	 */
	private final SwaptionHullWhiteModelBoundary delegate;

	/**
	 * Creates the boundary wrapper.
	 *
	 * @param model The Hull-White model.
	 */
	public ResolvedSwaptionHullWhiteModelBoundary(final FDMHullWhiteModel model) {
		this.delegate = new SwaptionHullWhiteModelBoundary(model);
	}

	@Override
	public BoundaryCondition[] getBoundaryConditionsAtLowerBoundary(
			final FiniteDifferenceInterestRateProduct product,
			final double time,
			final double... stateVariables) {

		final Swaption originalSwaption = unwrap(product);

		return delegate.getBoundaryConditionsAtLowerBoundary(
				originalSwaption,
				time,
				stateVariables
				);
	}

	@Override
	public BoundaryCondition[] getBoundaryConditionsAtUpperBoundary(
			final FiniteDifferenceInterestRateProduct product,
			final double time,
			final double... stateVariables) {

		final Swaption originalSwaption = unwrap(product);

		return delegate.getBoundaryConditionsAtUpperBoundary(
				originalSwaption,
				time,
				stateVariables
				);
	}

	private Swaption unwrap(final FiniteDifferenceInterestRateProduct product) {
		if (!(product instanceof Swaption.ResolvedSwaption)) {
			throw new IllegalArgumentException(
					"ResolvedSwaptionHullWhiteModelBoundary requires a Swaption.ResolvedSwaption product."
					);
		}

		return ((Swaption.ResolvedSwaption) product).getOriginalSwaption();
	}
}
