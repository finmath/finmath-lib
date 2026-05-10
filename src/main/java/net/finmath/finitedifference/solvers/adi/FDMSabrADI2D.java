package net.finmath.finitedifference.solvers.adi;

import net.finmath.finitedifference.assetderivativevaluation.models.FDMSabrModel;
import net.finmath.finitedifference.assetderivativevaluation.products.FiniteDifferenceEquityProduct;
import net.finmath.finitedifference.grids.SpaceTimeDiscretization;
import net.finmath.modelling.Exercise;

/**
 * ADI finite-difference solver for two-dimensional SABR PDEs.
 *
 * <p>
 * This class specializes {@link AbstractADI2D} to the
 * {@link FDMSabrModel}. In the standard lifted two-dimensional SABR setting,
 * the
 * state vector is given by the forward (or spot) level <i>F</i> and the
 * volatility
 * factor <i>&alpha;</i>. In time-to-maturity coordinates
 * <i>&tau; = T - t</i>, the option value
 * <i>u = u(&tau;, F, &alpha;)</i> satisfies a two-dimensional backward
 * parabolic PDE
 * of the form
 * </p>
 *
 * <p>
 * <i>
 * &part;u / &part;&tau; = {@literal \mathcal{L}}<sub>SABR</sub> u,
 * </i>
 * </p>
 *
 * <p>
 * where the SABR generator contains drift terms, diffusion terms in both state
 * variables, and a mixed derivative term induced by the correlation parameter
 * <i>&rho;</i>. The ADI splitting implemented in the parent class decomposes
 * this
 * operator into directional contributions which are treated alternately and
 * implicitly along the two coordinate directions.
 * </p>
 *
 * <p>
 * This class does not alter the generic ADI stepping logic, boundary handling,
 * or
 * exercise treatment provided by {@link AbstractADI2D}; it simply binds that
 * machinery to the coefficient structure supplied by {@link FDMSabrModel}.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class FDMSabrADI2D extends AbstractADI2D {

	/**
	 * Creates the ADI solver for a two-dimensional SABR PDE.
	 *
	 * @param model The SABR finite-difference model.
	 * @param product The product to be valued.
	 * @param spaceTimeDiscretization The space-time discretization.
	 * @param exercise The exercise specification.
	 */
	public FDMSabrADI2D(
			final FDMSabrModel model,
			final FiniteDifferenceEquityProduct product,
			final SpaceTimeDiscretization spaceTimeDiscretization,
			final Exercise exercise) {
		super(model, product, spaceTimeDiscretization, exercise);
	}
}
