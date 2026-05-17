package net.finmath.finitedifference;

import net.finmath.finitedifference.grids.SpaceTimeDiscretization;
import net.finmath.modelling.Model;

/**
 * Basic interface for any model that provides a finite difference
 * approximation of a partial differential equation (PDE),
 * either for equity or interest rate models.
 * <p>
 * A {@code FiniteDifferenceModel} must provide access to the
 * {@link SpaceTimeDiscretization} defining the grid used
 * for the numerical scheme.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public interface FiniteDifferenceModel extends Model {

	/**
	 * Returns the space-time discretization used by this finite difference
	 * model.
	 * <p>
	 * The discretization defines the grid in both time and space
	 * on which the PDE approximation is constructed.
	 * </p>
	 *
	 * @return The {@link SpaceTimeDiscretization} used by this model.
	 */
	SpaceTimeDiscretization getSpaceTimeDiscretization();
}
