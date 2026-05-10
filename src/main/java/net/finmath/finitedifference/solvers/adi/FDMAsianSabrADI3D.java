package net.finmath.finitedifference.solvers.adi;

import net.finmath.finitedifference.assetderivativevaluation.models.FiniteDifferenceEquityModel;
import net.finmath.finitedifference.assetderivativevaluation.products.FiniteDifferenceEquityProduct;
import net.finmath.finitedifference.boundaries.BoundaryCondition;
import net.finmath.finitedifference.grids.SpaceTimeDiscretization;
import net.finmath.finitedifference.solvers.ThomasSolver;
import net.finmath.finitedifference.solvers.TridiagonalMatrix;
import net.finmath.modelling.Exercise;

/**
 * Specialized three-dimensional ADI solver for arithmetic Asian options under a
 * lifted SABR
 * state <i>(S,\alpha,I)</i>.
 *
 * <p>
 * The lifted formulation augments the SABR state variables spot <i>S</i> and
 * volatility
 * factor <i>\alpha</i> by the running integral
 * </p>
 *
 * <p>
 * <i>I_t = \int_0^t S_u \, du</i>,
 * </p>
 *
 * <p>
 * so that an arithmetic-average payoff can be represented as a terminal payoff
 * in the
 * three-dimensional state vector <i>(S_t,\alpha_t,I_t)</i>. The dynamics are of
 * the form
 * </p>
 *
 * <p>
 * <i>dS_t = \mu_S dt + diffusion terms</i>,
 * </p>
 *
 * <p>
 * <i>d\alpha_t = \mu_{\alpha} dt + diffusion terms</i>,
 * </p>
 *
 * <p>
 * <i>dI_t = S_t dt</i>.
 * </p>
 *
 * <p>
 * In time-to-maturity coordinates <i>\tau = T - t</i>, the backward pricing PDE
 * is split as
 * </p>
 *
 * <p>
 * <i>u_{\tau} = A_0 u + A_1 u + A_2 u + A_3 u</i>,
 * </p>
 *
 * <p>
 * where
 * </p>
 * <ul>
 * <li><i>A_0</i> contains discounting and mixed-derivative terms and is treated
 * explicitly,</li>
 * <li><i>A_1</i> contains the <i>S</i>-direction drift and diffusion and is
 * treated implicitly,</li>
 * <li><i>A_2</i> contains the <i>\alpha</i>-direction drift and diffusion and
 * is treated implicitly,</li>
 * <li><i>A_3</i> is the transport term <i>S u_I</i> and is treated by a
 * specialized implicit upwind solve.</li>
 * </ul>
 *
 * <p>
 * There is no diffusion in the integral direction and no mixed derivative
 * involving
 * <i>I</i>. Hence the <i>I</i>-direction is a pure transport direction. In
 * <i>\tau</i>-time, the consistent upwind discretization is forward in
 * <i>I</i>.
 * </p>
 *
 * <p>
 * Relative to the generic {@link AbstractADI3D} implementation, this class
 * specializes
 * the third-direction transport operator and the corresponding implicit line
 * solve while
 * leaving the first and second implicit line solves aligned with the standard
 * three-dimensional SABR discretization.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class FDMAsianSabrADI3D extends AbstractADI3D {

	/**
	 * The stencil builder.
	 */
	private final ADI3DStencilBuilder stencilBuilder;

	/**
	 * Creates the lifted three-dimensional ADI solver for arithmetic Asian
	 * pricing under SABR.
	 *
	 * @param model The finite-difference model.
	 * @param product The product to be valued.
	 * @param spaceTimeDiscretization The space-time discretization.
	 * @param exercise The exercise specification.
	 */
	public FDMAsianSabrADI3D(
			final FiniteDifferenceEquityModel model,
			final FiniteDifferenceEquityProduct product,
			final SpaceTimeDiscretization spaceTimeDiscretization,
			final Exercise exercise) {
		super(model, product, spaceTimeDiscretization, exercise);
		this.stencilBuilder = new ADI3DStencilBuilder(model, x0Grid, x1Grid, x2Grid);
	}

	/**
	 * Applies the explicit transport operator {@code A3 u = S u_I}.
	 *
	 * <p>
	 * The derivative in the integral direction is approximated by forward
	 * upwinding:
	 * </p>
	 *
	 * <p>
	 * <i>u_I(S_i,\alpha_j,I_k) \approx (u_{i,j,k+1} - u_{i,j,k}) / (I_{k+1} -
	 * I_k)</i>.
	 * </p>
	 *
	 * @param u Current solution vector.
	 * @param time Current running time.
	 * @return Explicit contribution of {@code A3}.
	 */
	@Override
	protected double[] applyA3Explicit(final double[] u, final double time) {
		final double[] out = new double[n];

		for (int i0 = 0; i0 < n0; i0++) {
			final double s = x0Grid[i0];

			for (int i1 = 0; i1 < n1; i1++) {
				for (int i2 = 0; i2 < n2 - 1; i2++) {
					final double dIUp = x2Grid[i2 + 1] - x2Grid[i2];
					out[flatten(i0, i1, i2)] =
							s * (u[flatten(i0, i1, i2 + 1)] - u[flatten(i0, i1, i2)]) / dIUp;
				}

				out[flatten(i0, i1, n2 - 1)] = 0.0;
			}
		}

		return out;
	}

	/**
	 * Solves the implicit systems in the first spatial direction.
	 *
	 * @param rhs Right-hand side vector.
	 * @param time Current running time.
	 * @param dt Time-step size.
	 * @return Updated solution vector.
	 */
	@Override
	protected double[] solveFirstDirectionLines(
			final double[] rhs,
			final double time,
			final double dt) {

		final double[] out = rhs.clone();

		for (int i2 = 0; i2 < n2; i2++) {
			for (int i1 = 0; i1 < n1; i1++) {
				final TridiagonalMatrix m =
						stencilBuilder.buildFirstDirectionLineMatrix(time, dt, theta, i1, i2);

				final double[] lineRhs = new double[n0];
				for (int i0 = 0; i0 < n0; i0++) {
					lineRhs[i0] = rhs[flatten(i0, i1, i2)];
				}

				final double lowerBoundaryValue =
						getLowerBoundaryValueForFirstDirection(time, i1, i2, lineRhs[0]);
				final double upperBoundaryValue =
						getUpperBoundaryValueForFirstDirection(time, i1, i2, lineRhs[n0 - 1]);

				overwriteBoundaryRow(m, lineRhs, 0, lowerBoundaryValue);
				overwriteBoundaryRow(m, lineRhs, n0 - 1, upperBoundaryValue);

				final double[] solved = ThomasSolver.solve(m.lower, m.diag, m.upper, lineRhs);

				for (int i0 = 0; i0 < n0; i0++) {
					out[flatten(i0, i1, i2)] = solved[i0];
				}
			}
		}

		return out;
	}

	/**
	 * Solves the implicit systems in the second spatial direction.
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

		for (int i2 = 0; i2 < n2; i2++) {
			for (int i0 = 0; i0 < n0; i0++) {
				final TridiagonalMatrix m =
						stencilBuilder.buildSecondDirectionLineMatrix(time, dt, theta, i0, i2);

				final double[] lineRhs = new double[n1];
				for (int i1 = 0; i1 < n1; i1++) {
					lineRhs[i1] = rhs[flatten(i0, i1, i2)];
				}

				final double lowerBoundaryValue =
						getLowerBoundaryValueForSecondDirection(time, i0, i2, lineRhs[0]);
				final double upperBoundaryValue =
						getUpperBoundaryValueForSecondDirection(time, i0, i2, lineRhs[n1 - 1]);

				overwriteBoundaryRow(m, lineRhs, 0, lowerBoundaryValue);
				overwriteBoundaryRow(m, lineRhs, n1 - 1, upperBoundaryValue);

				final double[] solved = ThomasSolver.solve(m.lower, m.diag, m.upper, lineRhs);

				for (int i1 = 0; i1 < n1; i1++) {
					out[flatten(i0, i1, i2)] = solved[i1];
				}
			}
		}

		return out;
	}

	/**
	 * Solves the implicit systems in the integral direction.
	 *
	 * <p>
	 * For fixed <i>(S,\alpha)</i>, the third-direction transport discretization
	 * is
	 * </p>
	 *
	 * <p>
	 * <i>(1 + \lambda_k) v_k - \lambda_k v_{k+1} = rhs_k</i>,
	 * </p>
	 *
	 * <p>
	 * where <i>\lambda_k = \theta \Delta \tau S / (I_{k+1} - I_k)</i>.
	 * </p>
	 *
	 * @param rhs Right-hand side vector.
	 * @param time Current running time.
	 * @param dt Time-step size.
	 * @return Updated solution vector.
	 */
	@Override
	protected double[] solveThirdDirectionLines(
			final double[] rhs,
			final double time,
			final double dt) {

		final double[] out = rhs.clone();

		for (int i1 = 0; i1 < n1; i1++) {
			for (int i0 = 0; i0 < n0; i0++) {
				final double s = x0Grid[i0];

				final TridiagonalMatrix m = new TridiagonalMatrix(n2);
				final double[] lineRhs = new double[n2];

				for (int i2 = 0; i2 < n2; i2++) {
					lineRhs[i2] = rhs[flatten(i0, i1, i2)];
				}

				/*
				 * PDE rows for i2 = 0,...,n2-2:
				 *
				 * (1 + lambda_k) v_k - lambda_k v_{k+1} = rhs_k
				 */
				for (int i2 = 0; i2 < n2 - 1; i2++) {
					final double dIUp = x2Grid[i2 + 1] - x2Grid[i2];
					final double lambda = theta * dt * s / dIUp;

					m.lower[i2] = 0.0;
					m.diag[i2] = 1.0 + lambda;
					m.upper[i2] = -lambda;
				}

				/*
				 * Last row: default identity, then overwrite only if upper I
				 * boundary is Dirichlet.
				 */
				m.lower[n2 - 1] = 0.0;
				m.diag[n2 - 1] = 1.0;
				m.upper[n2 - 1] = 0.0;

				/*
				 * Upper I boundary is the inflow side.
				 */
				final BoundaryCondition[] upperConditions =
						model.getBoundaryConditionsAtUpperBoundary(product, time, x0Grid[i0], x1Grid[i1], x2Grid[n2 - 1]);

				if (upperConditions != null
						&& upperConditions.length > 2
						&& upperConditions[2] != null
						&& upperConditions[2].isDirichlet()) {
					overwriteBoundaryRow(m, lineRhs, n2 - 1, upperConditions[2].getValue());
				}

				/*
				 * Lower I boundary: overwrite only if explicitly Dirichlet.
				 * For Asian options this is typically NONE, so row 0 remains a
				 * PDE row.
				 */
				final BoundaryCondition[] lowerConditions =
						model.getBoundaryConditionsAtLowerBoundary(product, time, x0Grid[i0], x1Grid[i1], x2Grid[0]);

				if (lowerConditions != null
						&& lowerConditions.length > 2
						&& lowerConditions[2] != null
						&& lowerConditions[2].isDirichlet()) {
					overwriteBoundaryRow(m, lineRhs, 0, lowerConditions[2].getValue());
				}

				final double[] solved = ThomasSolver.solve(m.lower, m.diag, m.upper, lineRhs);

				for (int i2 = 0; i2 < n2; i2++) {
					out[flatten(i0, i1, i2)] = solved[i2];
				}
			}
		}

		return out;
	}
}
