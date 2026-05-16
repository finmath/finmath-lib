package net.finmath.finitedifference.assetderivativevaluation.boundaries;

import net.finmath.finitedifference.assetderivativevaluation.models.FDMBachelierModel;
import net.finmath.finitedifference.assetderivativevaluation.models.FDMBlackScholesModel;
import net.finmath.finitedifference.assetderivativevaluation.models.FDMCevModel;
import net.finmath.finitedifference.assetderivativevaluation.models.FiniteDifferenceEquityModel;
import net.finmath.finitedifference.solvers.TwoStateActiveBoundaryProvider;
import net.finmath.modelling.products.CallOrPut;

/**
 * Factory for active-state boundary providers used by the direct two-state
 * knock-in solver.
 *
 * <p>
 * The active state represents the post-activation value, i.e. the corresponding
 * vanilla option after the barrier has been hit.
 * </p>
 */
public final class ActiveBoundaryProviderFactory {

	private ActiveBoundaryProviderFactory() {
	}

	/**
	 * Performs the operation.
	 *
	 * @param model The value.
	 * @param strike The value.
	 * @param maturity The value.
	 * @param callOrPut The value.
	 * @return The value.
	 */
	public static TwoStateActiveBoundaryProvider createProvider(
			final FiniteDifferenceEquityModel model,
			final double strike,
			final double maturity,
			final CallOrPut callOrPut) {

		if (model instanceof FDMBlackScholesModel) {
			return new BlackScholesActiveBoundaryProvider(
					model,
					strike,
					maturity,
					callOrPut
			);
		}

		if (model instanceof FDMBachelierModel) {
			return new BachelierActiveBoundaryProvider(
					model,
					strike,
					maturity,
					callOrPut
			);
		}

		if (model instanceof FDMCevModel) {
			return new CevActiveBoundaryProvider(
					model,
					strike,
					maturity,
					callOrPut
			);
		}

		throw new IllegalArgumentException(
				"No active boundary provider available for model type: " + model.getClass().getName());
	}
}
