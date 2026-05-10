package net.finmath.finitedifference.assetderivativevaluation.models;

import java.util.Optional;

import net.finmath.finitedifference.FiniteDifferenceModel;
import net.finmath.finitedifference.assetderivativevaluation.boundaries.FiniteDifferenceBoundary;
import net.finmath.finitedifference.grids.SpaceTimeDiscretization;
import net.finmath.marketdata.model.curves.DiscountCurve;

/**
 * Interface for a finite-difference equity model.
 *
 * <p>
 * Implementations provide the coefficients and market data required by the
 * finite-difference solvers, in particular discount curves, drift coefficients,
 * factor loadings, boundary conditions, and the initial state.
 * </p>
 *
 * <p>
 * The framework is formulated in terms of the <i>chosen PDE state
 * variables</i>.
 * It does <b>not</b> assume that the first spatial coordinate is necessarily
 * the
 * spot price {@code S}. It may instead be {@code S}, {@code log(S)}, or any
 * other state variable for which the model provides consistent coefficients.
 * </p>
 *
 * <p>
 * Therefore, {@link #getDrift(double, double...)} and
 * {@link #getFactorLoading(double, double...)} must return coefficients that
 * are
 * expressed in exactly the same coordinates as those used in the space-time
 * discretization.
 * </p>
 *
 * <p>
 * Example:
 * </p>
 * <ul>
 *   <li>if the grid variable is {@code S}, then the drift and factor loadings
 *       should correspond to the SDE for {@code S},</li>
 *   <li>if the grid variable is {@code log(S)}, then the drift and factor
 *       loadings should correspond to the SDE for {@code log(S)}.</li>
 * </ul>
 *
 * <p>
 * The interface is dimension agnostic: it supports one-dimensional and
 * multi-dimensional equity models alike. In particular, multi-asset models may
 * provide one dividend-yield curve per underlying via
 * {@link #getDividendYieldCurves()}.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public interface FiniteDifferenceEquityModel extends FiniteDifferenceModel, FiniteDifferenceBoundary {

	/**
	 * Returns the risk-free discount curve used for pricing.
	 *
	 * @return The risk-free discount curve.
	 */
	DiscountCurve getRiskFreeCurve();

	/**
	 * Returns the dividend-yield discount curve.
	 *
	 * <p>
	 * This is the legacy single-asset accessor and remains part of the
	 * interface to
	 * preserve backward compatibility with the current code base.
	 * </p>
	 *
	 * <p>
	 * For true multi-asset models with one dividend curve per underlying,
	 * prefer
	 * {@link #getDividendYieldCurves()}. Such models may override this method
	 * with a
	 * convention suitable for backward compatibility.
	 * </p>
	 *
	 * @return The dividend-yield discount curve.
	 */
	DiscountCurve getDividendYieldCurve();

	/**
	 * Returns the dividend-yield discount curves of the model.
	 *
	 * <p>
	 * The default implementation preserves backward compatibility with the
	 * current
	 * single-asset code base by wrapping {@link #getDividendYieldCurve()} into
	 * a
	 * one-element array.
	 * </p>
	 *
	 * <p>
	 * Multi-asset models should override this method and return one dividend-
	 * yield
	 * curve per underlying asset, ordered consistently with the model state
	 * vector.
	 * </p>
	 *
	 * @return The dividend-yield discount curves.
	 */
	default DiscountCurve[] getDividendYieldCurves() {
		return new DiscountCurve[] {getDividendYieldCurve() };
	}

	/**
	 * Returns the drift vector of the model state variables.
	 *
	 * <p>
	 * The returned coefficients must be expressed in the same coordinates as
	 * the PDE
	 * state variables used by the finite-difference solver.
	 * </p>
	 *
	 * <p>
	 * If the state vector is {@code X = (X1, ..., Xn)}, then this method
	 * returns the
	 * vector {@code mu(t, X)} in
	 * </p>
	 *
	 * <p>
	 * {@code dX_i(t) = mu_i(t, X_t) dt + ...}.
	 * </p>
	 *
	 * @param time The evaluation time.
	 * @param stateVariables The current values of the model state variables.
	 * @return The drift vector.
	 */
	double[] getDrift(double time, double... stateVariables);

	/**
	 * Returns the factor-loading matrix of the model state variables.
	 *
	 * <p>
	 * The returned coefficients must be expressed in the same coordinates as
	 * the PDE
	 * state variables used by the finite-difference solver.
	 * </p>
	 *
	 * <p>
	 * If the state vector is {@code X = (X1, ..., Xn)}, then this method
	 * returns the
	 * matrix {@code b(t, X)} in
	 * </p>
	 *
	 * <p>
	 * {@code dX_i(t) = mu_i(t, X_t) dt + sum_j b_{i,j}(t, X_t) dW_j(t)}.
	 * </p>
	 *
	 * @param time The evaluation time.
	 * @param stateVariables The current values of the model state variables.
	 * @return The factor-loading matrix.
	 */
	double[][] getFactorLoading(double time, double... stateVariables);

	/**
	 * Returns the optional jump component of the infinitesimal generator.
	 *
	 * <p>
	 * The default implementation returns {@link Optional#empty()},
	 * corresponding to a
	 * purely diffusive model.
	 * </p>
	 *
	 * <p>
	 * If present, the returned {@link JumpComponent} provides the data needed
	 * to
	 * assemble the non-local jump part of the generator. The local coefficients
	 * returned by {@link #getDrift(double, double...)} and
	 * {@link #getFactorLoading(double, double...)} keep their current meaning
	 * and
	 * should remain consistent with the PDE state variables used by the
	 * finite-difference discretization.
	 * </p>
	 *
	 * <p>
	 * In particular, under the stock-coordinate convention intended for jump
	 * models in
	 * this framework, {@link #getDrift(double, double...)} should continue to
	 * return
	 * the drift of the local first-order term, while the jump component defines
	 * the
	 * non-local operator in compensated form.
	 * </p>
	 *
	 * <p>
	 * For a one-dimensional spot variable {@code S}, the jump contribution is
	 * understood in the form
	 * </p>
	 *
	 * <pre>
	 * integral [ f(S exp(y)) - f(S) - S (exp(y) - 1) f'(S) ] nu(dy).
	 * </pre>
	 *
	 * <p>
	 * The interpretation of the jump data must remain consistent with the
	 * chosen PDE
	 * state variables.
	 * </p>
	 *
	 * @return An {@link Optional} containing the jump component of the
	 *     infinitesimal
	 * generator, or {@link Optional#empty()} if the model has no jump part.
	 */
	default Optional<JumpComponent> getJumpComponent() {
		return Optional.empty();
	}

	/**
	 * Returns a clone of this model with a modified space-time discretization.
	 *
	 * <p>
	 * The returned model should represent the same stochastic dynamics and
	 * market
	 * data as the original one, but on the provided discretization.
	 * </p>
	 *
	 * @param newSpaceTimeDiscretization The new space-time discretization.
	 * @return A clone of this model with the modified discretization.
	 */
	FiniteDifferenceEquityModel getCloneWithModifiedSpaceTimeDiscretization(
			SpaceTimeDiscretization newSpaceTimeDiscretization);

	/**
	 * Returns the initial state vector of the model.
	 *
	 * <p>
	 * The returned values must be consistent with the state variables used by
	 * the
	 * PDE. For example, if the first spatial coordinate is {@code log(S)}, then
	 * the
	 * first component should be {@code log(S0)} rather than {@code S0}.
	 * </p>
	 *
	 * @return The initial state vector.
	 */
	double[] getInitialValue();
}
