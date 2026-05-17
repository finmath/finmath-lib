package net.finmath.finitedifference.solvers.adi;

import net.finmath.finitedifference.assetderivativevaluation.models.FDMMultiAssetBlackScholesModel;
import net.finmath.finitedifference.assetderivativevaluation.products.FiniteDifferenceEquityProduct;
import net.finmath.finitedifference.grids.SpaceTimeDiscretization;
import net.finmath.modelling.Exercise;

/**
 * ADI finite-difference solver for the two-dimensional multi-asset
 * Black-Scholes PDE.
 *
 * <p>
 * This class specializes {@link AbstractADI2D} to the
 * {@link FDMMultiAssetBlackScholesModel} in the case of two risky assets.
 * Let {@code S1} and {@code S2} denote the two asset prices. Under the
 * risk-neutral measure, the model dynamics are
 * </p>
 *
 * <p>
 * <i>
 * dS_i(t) = (r(t) - q_i(t)) S_i(t) dt + sigma_i S_i(t) dW_i(t),
 * </i>
 * </p>
 *
 * <p>
 * for {@code i = 1,2}, with instantaneous correlation
 * </p>
 *
 * <p>
 * <i>
 * d&lt;W_1, W_2&gt;_t = rho dt.
 * </i>
 * </p>
 *
 * <p>
 * In time-to-maturity coordinates {@code tau = T - t}, the pricing PDE for the
 * option value {@code u = u(tau, S1, S2)} can be written as
 * </p>
 *
 * <p>
 * <i>
 * u_tau = A_0 u + A_1 u + A_2 u,
 * </i>
 * </p>
 *
 * <p>
 * where {@code A0} contains the mixed derivative term together with
 * discounting,
 * while {@code A1} and {@code A2} contain the first- and second-asset drift and
 * diffusion terms, respectively. The ADI splitting and line solves are
 * inherited
 * from {@link AbstractADI2D}.
 * </p>
 *
 * <p>
 * This class is intentionally thin: all generic two-dimensional ADI logic,
 * boundary handling, internal constraints, and exercise treatment are provided
 * by the abstract base class.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class FDMMultiAssetBlackScholesADI2D extends AbstractADI2D {

	/**
	 * Creates the ADI solver for the two-dimensional multi-asset Black-Scholes
	 * PDE.
	 *
	 * @param model The multi-asset Black-Scholes finite-difference model.
	 * @param product The product to be valued.
	 * @param spaceTimeDiscretization The space-time discretization.
	 * @param exercise The exercise specification.
	 */
	public FDMMultiAssetBlackScholesADI2D(
			final FDMMultiAssetBlackScholesModel model,
			final FiniteDifferenceEquityProduct product,
			final SpaceTimeDiscretization spaceTimeDiscretization,
			final Exercise exercise) {
		super(model, product, spaceTimeDiscretization, exercise);

		if (spaceTimeDiscretization.getNumberOfSpaceGrids() != 2) {
			throw new IllegalArgumentException(
					"FDMMultiAssetBlackScholesADI2D requires exactly two space grids.");
		}
		if (model.getInitialValue() == null || model.getInitialValue().length != 2) {
			throw new IllegalArgumentException(
					"FDMMultiAssetBlackScholesADI2D requires a two-dimensional model state.");
		}
	}
}
