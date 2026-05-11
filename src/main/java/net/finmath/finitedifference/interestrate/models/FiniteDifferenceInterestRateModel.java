package net.finmath.finitedifference.interestrate.models;

import net.finmath.finitedifference.FiniteDifferenceModel;
import net.finmath.finitedifference.boundaries.BoundaryCondition;
import net.finmath.finitedifference.boundaries.StandardBoundaryCondition;
import net.finmath.finitedifference.grids.SpaceTimeDiscretization;
import net.finmath.finitedifference.interestrate.products.FiniteDifferenceInterestRateProduct;
import net.finmath.marketdata.model.AnalyticModel;

/**
 * Base interface for finite-difference interest-rate models.
 *
 * <p>
 * The interface is designed to remain close to the philosophy of the Monte
 * Carlo
 * interest-rate framework of finmath while exposing the additional ingredients
 * required by a partial-differential-equation discretization.
 * </p>
 *
 * <p>
 * A model implementing this interface is assumed to satisfy the following two
 * principles:
 * </p>
 * <ul>
 *   <li>it fits the initially observed term structure automatically through an
 *       underlying bootstrapped {@link AnalyticModel},</li>
 *   <li>it admits a finite-dimensional Markovian state representation, so that
 *       the PDE is solved on a grid of state variables rather than on a full
 *       curve of maturities.</li>
 * </ul>
 *
 * <p>
 * More precisely, the model is described by a state vector
 * </p>
 *
 * <p>
 * <i>
 * X_t = (X_t^{(1)}, \ldots, X_t^{(d)})
 * </i>
 * </p>
 *
 * <p>
 * evolving under dynamics of the form
 * </p>
 *
 * <p>
 * <i>
 * dX_t = \mu(t,X_t)\,dt + \lambda(t,X_t)\,dW_t,
 * </i>
 * </p>
 *
 * <p>
 * where the drift vector {@code \mu} and factor-loading matrix
 * {@code \lambda} are returned by {@link #getDrift(double, double...)} and
 * {@link #getFactorLoading(double, double...)}. Observable term-structure
 * quantities are reconstructed from the state. In particular, forward rates are
 * obtained through
 * </p>
 *
 * <p>
 * <i>
 * F(t;T_1,T_2) = \Phi(t,T_1,T_2,X_t),
 * </i>
 * </p>
 *
 * <p>
 * where the forwarding curve is identified explicitly in the multi-curve
 * setting.
 * </p>
 *
 * <p>
 * The interface does <b>not</b> encode a short-rate-only point of view. In
 * particular, no method like {@code getShortRate(...)} is required at this
 * level. This keeps the contract suitable for short-rate models, Gaussian HJM /
 * Cheyette models, and other Markovian term-structure specifications.
 * </p>
 *
 * <p>
 * The default boundary implementation returns
 * {@link StandardBoundaryCondition#none()} in every spatial direction.
 * Model-specific classes may override this when Dirichlet or Neumann boundary
 * information is available analytically.
 * </p>
 *s
 * @author Alessandro Gnoatto
 */
public interface FiniteDifferenceInterestRateModel extends FiniteDifferenceModel {

	/**
	 * Returns the analytic multi-curve model used as the initial condition of
	 * the
	 * stochastic term-structure model.
	 *
	 * <p>
	 * The returned {@link AnalyticModel} is assumed to contain the bootstrapped
	 * market curves used to anchor the model to the observed initial term
	 * structure.
	 * </p>
	 *
	 * @return The initial analytic multi-curve model.
	 */
	AnalyticModel getAnalyticModel();

	/**
	 * Returns the initial value of the Markovian state variables.
	 *
	 * @return The initial state vector.
	 */
	double[] getInitialValue();

	/**
	 * Returns the drift vector of the Markovian state process.
	 *
	 * <p>
	 * This method provides the deterministic part of the state dynamics
	 * </p>
	 *
	 * <p>
	 * <i>
	 * dX_t = \mu(t,X_t)\,dt + \lambda(t,X_t)\,dW_t.
	 * </i>
	 * </p>
	 *
	 * @param time The current time.
	 * @param stateVariables The current state vector.
	 * @return The drift vector at the given state.
	 */
	double[] getDrift(double time, double... stateVariables);

	/**
	 * Returns the factor-loading matrix of the Markovian state process.
	 *
	 * <p>
	 * If {@code d} denotes the state dimension and {@code m} the Brownian
	 * dimension, then the returned array has shape {@code d x m}.
	 * </p>
	 *
	 * @param time The current time.
	 * @param stateVariables The current state vector.
	 * @return The factor-loading matrix at the given state.
	 */
	double[][] getFactorLoading(double time, double... stateVariables);

	/**
	 * Returns the value at time {@code time} of the discount bond maturing at
	 * {@code maturity}, conditional on the current model state.
	 *
	 * <p>
	 * In a multi-curve setting this method refers to the discounting curve.
	 * </p>
	 *
	 * @param time The evaluation time.
	 * @param maturity The maturity of the discount bond.
	 * @param stateVariables The current state vector.
	 * @return The discount bond value.
	 */
	double getDiscountBond(double time, double maturity, double... stateVariables);

	/**
	 * Returns the simply compounded forward rate associated with a given
	 * forwarding curve and accrual period.
	 *
	 * <p>
	 * This is the finite-difference analogue of the Monte Carlo
	 * {@code getLibor(...)} philosophy. The forwarding curve is identified
	 * explicitly because, in a multi-curve framework, it is in general distinct
	 * from the discount curve.
	 * </p>
	 *
	 * @param forwardCurveName The name of the forwarding curve.
	 * @param time The evaluation time.
	 * @param periodStart The start of the accrual period.
	 * @param periodEnd The end of the accrual period.
	 * @param stateVariables The current state vector.
	 * @return The forward rate for the specified accrual period.
	 */
	double getForwardRate(
			String forwardCurveName,
			double time,
			double periodStart,
			double periodEnd,
			double... stateVariables);

	/**
	 * Alias for {@link #getForwardRate(String, double, double, double,
	 * double...)}
	 * using the Monte Carlo terminology.
	 *
	 * @param forwardCurveName The name of the forwarding curve.
	 * @param time The evaluation time.
	 * @param periodStart The start of the accrual period.
	 * @param periodEnd The end of the accrual period.
	 * @param stateVariables The current state vector.
	 * @return The forward rate for the specified accrual period.
	 */
	default double getLibor(
			final String forwardCurveName,
			final double time,
			final double periodStart,
			final double periodEnd,
			final double... stateVariables) {
		return getForwardRate(forwardCurveName, time, periodStart, periodEnd, stateVariables);
	}

	/**
	 * Returns a clone of the model using a modified space-time discretization.
	 *
	 * @param spaceTimeDiscretization The new space-time discretization.
	 * @return A clone with the modified discretization.
	 */
	FiniteDifferenceInterestRateModel getCloneWithModifiedSpaceTimeDiscretization(
			SpaceTimeDiscretization spaceTimeDiscretization);

	/**
	 * Returns the lower-boundary conditions for the current product and state.
	 *
	 * <p>
	 * The default implementation returns
	 * {@link StandardBoundaryCondition#none()} in every spatial direction.
	 * </p>
	 *
	 * @param product The product being priced.
	 * @param time The current time.
	 * @param stateVariables The current boundary state.
	 * @return Boundary conditions at the lower boundary.
	 */
	default BoundaryCondition[] getBoundaryConditionsAtLowerBoundary(
			final FiniteDifferenceInterestRateProduct product,
			final double time,
			final double... stateVariables) {

		final int numberOfSpaceGrids = getSpaceTimeDiscretization().getNumberOfSpaceGrids();
		final BoundaryCondition[] boundaryConditions = new BoundaryCondition[numberOfSpaceGrids];

		for (int i = 0; i < numberOfSpaceGrids; i++) {
			boundaryConditions[i] = StandardBoundaryCondition.none();
		}

		return boundaryConditions;
	}

	/**
	 * Returns the upper-boundary conditions for the current product and state.
	 *
	 * <p>
	 * The default implementation returns
	 * {@link StandardBoundaryCondition#none()} in every spatial direction.
	 * </p>
	 *
	 * @param product The product being priced.
	 * @param time The current time.
	 * @param stateVariables The current boundary state.
	 * @return Boundary conditions at the upper boundary.
	 */
	default BoundaryCondition[] getBoundaryConditionsAtUpperBoundary(
			final FiniteDifferenceInterestRateProduct product,
			final double time,
			final double... stateVariables) {

		final int numberOfSpaceGrids = getSpaceTimeDiscretization().getNumberOfSpaceGrids();
		final BoundaryCondition[] boundaryConditions = new BoundaryCondition[numberOfSpaceGrids];

		for (int i = 0; i < numberOfSpaceGrids; i++) {
			boundaryConditions[i] = StandardBoundaryCondition.none();
		}

		return boundaryConditions;
	}
}
