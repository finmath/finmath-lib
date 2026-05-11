package net.finmath.finitedifference.solvers.adi;

import net.finmath.finitedifference.assetderivativevaluation.models.FiniteDifferenceEquityModel;
import net.finmath.finitedifference.assetderivativevaluation.products.FiniteDifferenceEquityProduct;
import net.finmath.finitedifference.grids.SpaceTimeDiscretization;
import net.finmath.finitedifference.solvers.ThomasSolver;
import net.finmath.finitedifference.solvers.TridiagonalMatrix;
import net.finmath.modelling.Exercise;

/**
 * Specialized ADI solver for arithmetic Asian options in the lifted state
 * <i>(S,I)</i>.
 *
 * <p>
 * The lifted formulation augments the spot process <i>S</i> by the running
 * integral
 * </p>
 *
 * <p>
 * <i>I_t = \int_0^t S_u \, du</i>,
 * </p>
 *
 * <p>
 * so that an arithmetic-average payoff can be represented as a terminal payoff
 * in the
 * two-dimensional state variables <i>(S_t,I_t)</i>. The state dynamics are
 * </p>
 *
 * <p>
 * <i>dS_t = \mu_S dt + \sigma(S_t,t) dW_t</i>,
 * </p>
 *
 * <p>
 * <i>dI_t = S_t dt</i>.
 * </p>
 *
 * <p>
 * In time-to-maturity coordinates <i>\tau = T - t</i>, the backward pricing PDE
 * is
 * written as
 * </p>
 *
 * <p>
 * <i>u_{\tau} = A_0 u + A_1 u + A_2 u</i>,
 * </p>
 *
 * <p>
 * with
 * </p>
 *
 * <p>
 * <i>A_0 u = -r u</i>,
 * </p>
 *
 * <p>
 * <i>A_1 u = \mu_S u_S + \frac{1}{2} a_{SS} u_{SS}</i>,
 * </p>
 *
 * <p>
 * <i>A_2 u = S u_I</i>.
 * </p>
 *
 * <p>
 * Hence there is no mixed derivative and no diffusion in the integral
 * direction.
 * The second state direction is a pure transport direction. In
 * <i>\tau</i>-time,
 * transport is propagated toward increasing <i>I</i>, so the consistent upwind
 * discretization is forward in the <i>I</i> direction.
 * </p>
 *
 * <p>
 * Relative to the generic {@link AbstractADI2D} implementation, this class
 * therefore:
 * </p>
 * <ul>
 *   <li>uses only discounting in the explicit {@code A0} part,</li>
 *   <li>uses a forward-upwind discretization for {@code A2},</li>
 * <li>solves the second-direction implicit systems with the corresponding
 * transport stencil.</li>
 * </ul>
 *
 * @author Alessandro Gnoatto
 */
public class FDMAsianADI2D extends AbstractADI2D {

	/**
	 * Creates the lifted two-dimensional ADI solver for arithmetic Asian
	 * products.
	 *
	 * @param model The finite-difference model.
	 * @param product The product to be valued.
	 * @param spaceTimeDiscretization The space-time discretization.
	 * @param exercise The exercise specification.
	 */
	public FDMAsianADI2D(
			final FiniteDifferenceEquityModel model,
			final FiniteDifferenceEquityProduct product,
			final SpaceTimeDiscretization spaceTimeDiscretization,
			final Exercise exercise) {
		super(model, product, spaceTimeDiscretization, exercise);
	}

	/**
	 * Applies the explicit operator part {@code A0}.
	 *
	 * <p>
	 * For the lifted arithmetic Asian PDE, {@code A0} contains only the
	 * discounting term
	 * <i>-r u</i>.
	 * </p>
	 *
	 * @param u Current solution vector.
	 * @param time Current running time.
	 * @return Explicit contribution of {@code A0}.
	 */
	@Override
	protected double[] applyA0Explicit(final double[] u, final double time) {
		final double[] out = new double[getN()];

		final double tSafe = Math.max(time, 1E-10);
		final double discountFactor = getModel().getRiskFreeCurve().getDiscountFactor(tSafe);
		final double r = -Math.log(discountFactor) / tSafe;

		for (int j = 0; j < getN1(); j++) {
			for (int i = 0; i < getN0(); i++) {
				out[flatten(i, j)] = -r * u[flatten(i, j)];
			}
		}

		return out;
	}

	/**
	 * Applies the explicit transport operator {@code A2 u = S u_I}.
	 *
	 * <p>
	 * The derivative in the integral direction is approximated by forward
	 * upwinding:
	 * </p>
	 *
	 * <p>
	 * <i>u_I(S_i,I_j) \approx (u_{i,j+1} - u_{i,j}) / (I_{j+1} - I_j)</i>.
	 * </p>
	 *
	 * <p>
	 * This is the appropriate upwind choice in time-to-maturity coordinates for
	 * the
	 * transport equation induced by <i>dI_t = S_t dt</i>.
	 * </p>
	 *
	 * @param u Current solution vector.
	 * @param time Current running time.
	 * @return Explicit contribution of {@code A2}.
	 */
	@Override
	protected double[] applyA2Explicit(final double[] u, final double time) {
		final double[] out = new double[getN()];

		for (int i = 0; i < getN0(); i++) {
			final double s = getX0Grid()[i];

			for (int j = 0; j < getN1() - 1; j++) {
				final double dIUp = getX1Grid()[j + 1] - getX1Grid()[j];
				out[flatten(i, j)] = s * (u[flatten(i, j + 1)] - u[flatten(i, j)]) / dIUp;
			}

			out[flatten(i, getN1() - 1)] = 0.0;
		}

		return out;
	}

	/**
	 * Solves the implicit systems in the integral direction.
	 *
	 * <p>
	 * For each fixed spot index, the second-direction system corresponds to the
	 * transport
	 * discretization
	 * </p>
	 *
	 * <p>
	 * <i>(1 + \lambda_j) v_j - \lambda_j v_{j+1} = rhs_j</i>,
	 * </p>
	 *
	 * <p>
	 * where <i>\lambda_j = \theta \Delta \tau S / (I_{j+1} - I_j)</i>.
	 * </p>
	 *
	 * <p>
	 * The upper integral boundary is the inflow side and is imposed if it is
	 * Dirichlet.
	 * The lower integral boundary is overwritten only when an explicit
	 * Dirichlet condition
	 * is provided.
	 * </p>
	 *
	 * @param rhs Right-hand side vector.
	 * @param time Current running time.
	 * @param dt Time-step size.
	 * @return Updated solution vector.
	 */
	@Override
	protected double[] solveSecondDirectionLines(
			final double[] rhs,
			final double time,
			final double dt) {

		final double[] out = rhs.clone();

		for (int i = 0; i < getN0(); i++) {
			final double s = getX0Grid()[i];

			final TridiagonalMatrix m = new TridiagonalMatrix(getN1());
			final double[] lineRhs = new double[getN1()];

			for (int j = 0; j < getN1(); j++) {
				lineRhs[j] = rhs[flatten(i, j)];
			}

			/*
			 * PDE rows for j = 0,...,n1-2:
			 *
			 * (1 + lambda_j) v_j - lambda_j v_{j+1} = rhs_j
			 */
			for (int j = 0; j < getN1() - 1; j++) {
				final double dIUp = getX1Grid()[j + 1] - getX1Grid()[j];
				final double lambda = getTheta() * dt * s / dIUp;

				m.getLowerDiagonal()[j] = 0.0;
				m.getMainDiagonal()[j] = 1.0 + lambda;
				m.getUpperDiagonal()[j] = -lambda;
			}

			/*
			 * Last row: default identity, then overwrite only if upper boundary
			 * is Dirichlet.
			 */
			m.getLowerDiagonal()[getN1() - 1] = 0.0;
			m.getMainDiagonal()[getN1() - 1] = 1.0;
			m.getUpperDiagonal()[getN1() - 1] = 0.0;

			/*
			 * Upper I boundary is the inflow side.
			 */
			final net.finmath.finitedifference.boundaries.BoundaryCondition[] upperConditions =
					getModel().getBoundaryConditionsAtUpperBoundary(getProduct(), time, getX0Grid()[i], getX1Grid()[getN1() - 1]);

			if (upperConditions != null
					&& upperConditions.length > 1
					&& upperConditions[1] != null
					&& upperConditions[1].isDirichlet()) {
				overwriteBoundaryRow(m, lineRhs, getN1() - 1, upperConditions[1].getValue());
			}

			/*
			 * Lower I boundary: overwrite ONLY if explicitly Dirichlet.
			 * For AsianOption it is NONE, so row 0 remains a PDE row.
			 */
			final net.finmath.finitedifference.boundaries.BoundaryCondition[] lowerConditions =
					getModel().getBoundaryConditionsAtLowerBoundary(getProduct(), time, getX0Grid()[i], getX1Grid()[0]);

			if (lowerConditions != null
					&& lowerConditions.length > 1
					&& lowerConditions[1] != null
					&& lowerConditions[1].isDirichlet()) {
				overwriteBoundaryRow(m, lineRhs, 0, lowerConditions[1].getValue());
			}

			final double[] solved = ThomasSolver.solve(m.getLowerDiagonal(), m.getMainDiagonal(), m.getUpperDiagonal(), lineRhs);

			for (int j = 0; j < getN1(); j++) {
				out[flatten(i, j)] = solved[j];
			}
		}

		return out;
	}
}
