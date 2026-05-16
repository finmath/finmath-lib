package net.finmath.finitedifference.solvers.adi;

import net.finmath.finitedifference.assetderivativevaluation.models.FDMHestonModel;
import net.finmath.finitedifference.assetderivativevaluation.products.FiniteDifferenceEquityProduct;
import net.finmath.finitedifference.grids.SpaceTimeDiscretization;
import net.finmath.modelling.Exercise;

/**
 * ADI finite-difference solver for the two-dimensional Heston PDE.
 *
 * <p>
 * Under the Heston model, the state vector is typically given by the spot
 * <i>S</i> and the variance <i>v</i>. The option value
 * <i>u = u(\tau,S,v)</i>, written in time-to-maturity coordinates
 * <i>\tau = T - t</i>, satisfies a two-dimensional parabolic PDE of the form
 * </p>
 *
 * <p>
 * <i>
 * \frac{\partial u}{\partial \tau}
 * = \mathcal{L}_{\mathrm{Heston}} u,
 * </i>
 * </p>
 *
 * <p>
 * where the Heston generator contains drift terms in both state variables,
 * diffusion terms in both directions, and a mixed derivative induced by the
 * correlation between the Brownian motions driving spot and variance.
 * </p>
 *
 * <p>
 * This class provides the concrete specialization of {@link AbstractADI2D} for
 * the Heston model. The spatial operator is discretized by the generic
 * two-dimensional ADI machinery, while the model-specific coefficients are
 * obtained from {@link FDMHestonModel}. The resulting scheme is suitable for
 * European, Bermudan, and American-style products supported by the surrounding
 * finite-difference framework.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class FDMHestonADI2D extends AbstractADI2D {

	/**
	 * Creates the ADI solver for the two-dimensional Heston PDE.
	 *
	 * @param model The Heston finite-difference model.
	 * @param product The product to be valued.
	 * @param spaceTimeDiscretization The space-time discretization.
	 * @param exercise The exercise specification.
	 */
	public FDMHestonADI2D(
			final FDMHestonModel model,
			final FiniteDifferenceEquityProduct product,
			final SpaceTimeDiscretization spaceTimeDiscretization,
			final Exercise exercise) {
		super(model, product, spaceTimeDiscretization, exercise);
	}
}
