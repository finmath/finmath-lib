package net.finmath.finitedifference.solvers.adi;

import net.finmath.finitedifference.assetderivativevaluation.models.FiniteDifferenceEquityModel;
import net.finmath.finitedifference.assetderivativevaluation.products.FiniteDifferenceEquityProduct;
import net.finmath.finitedifference.boundaries.BoundaryCondition;
import net.finmath.finitedifference.grids.SpaceTimeDiscretization;
import net.finmath.finitedifference.solvers.ThomasSolver;
import net.finmath.finitedifference.solvers.TridiagonalMatrix;
import net.finmath.modelling.Exercise;

/**
 * Specialized 3D ADI solver for arithmetic Asian options under a lifted Heston
 * state
 * (S, v, I), where
 *
 *   dS_t = mu_S dt + diffusion terms,
 *   dv_t = mu_v dt + diffusion terms,
 *   dI_t = S_t dt.
 *
 * In time-to-maturity tau = T - t, the backward pricing PDE is split as
 *
 *   u_tau = A0 u + A1 u + A2 u + A3 u
 *
 * with
 *
 *   A0: discount term and mixed derivative terms (treated explicitly)
 *   A1: S-direction drift and diffusion (treated implicitly)
 *   A2: v-direction drift and diffusion (treated implicitly)
 * A3: I-direction transport S * u_I (treated with a specialized implicit upwind
 * solve)
 *
 * There is:
 * - no diffusion in the I direction
 * - no mixed derivative involving I
 *
 * The I direction is pure transport. In tau-time, the correct upwind
 * discretization is forward in I.
 *
 * @author Alessandro Gnoatto
 */
public class FDMAsianHestonADI3D extends AbstractADI3D {

	/**
	 * The stencil builder.
	 */
	private final ADI3DStencilBuilder stencilBuilder;

	/**
	 * Performs the operation.
	 *
	 * @param model The value.
	 * @param product The value.
	 * @param spaceTimeDiscretization The value.
	 * @param exercise The value.
	 */
	public FDMAsianHestonADI3D(
			final FiniteDifferenceEquityModel model,
			final FiniteDifferenceEquityProduct product,
			final SpaceTimeDiscretization spaceTimeDiscretization,
			final Exercise exercise) {
		super(model, product, spaceTimeDiscretization, exercise);
		this.stencilBuilder = new ADI3DStencilBuilder(model, x0Grid, x1Grid, x2Grid);
	}

	/**
	 * Explicit application of A3 u = S * u_I
	 * using forward upwinding in I:
	 *
	 *   u_I(S_i, v_j, I_k) ~ (u_{i,j,k+1} - u_{i,j,k}) / (I_{k+1} - I_k)
	 *
	 * for k = 0,...,n2-2.
	 *
	 * The top I-row k = n2 - 1 is handled via the upper I boundary.
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

				final double[] solved = ThomasSolver.solve(m.getLowerDiagonal(), m.getMainDiagonal(), m.getUpperDiagonal(), lineRhs);

				for (int i0 = 0; i0 < n0; i0++) {
					out[flatten(i0, i1, i2)] = solved[i0];
				}
			}
		}

		return out;
	}

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

				/*
				 * Lower boundary in second direction (v-direction):
				 * overwrite ONLY if explicitly Dirichlet.
				 */
				final BoundaryCondition[] lowerConditions =
						model.getBoundaryConditionsAtLowerBoundary(product, time, x0Grid[i0], x1Grid[0], x2Grid[i2]);

				if (lowerConditions != null
						&& lowerConditions.length > 1
						&& lowerConditions[1] != null
						&& lowerConditions[1].isDirichlet()) {
					overwriteBoundaryRow(m, lineRhs, 0, lowerConditions[1].getValue());
				}

				/*
				 * Upper boundary in second direction (v-direction):
				 * overwrite ONLY if explicitly Dirichlet.
				 */
				final BoundaryCondition[] upperConditions =
						model.getBoundaryConditionsAtUpperBoundary(product, time, x0Grid[i0], x1Grid[n1 - 1], x2Grid[i2]);

				if (upperConditions != null
						&& upperConditions.length > 1
						&& upperConditions[1] != null
						&& upperConditions[1].isDirichlet()) {
					overwriteBoundaryRow(m, lineRhs, n1 - 1, upperConditions[1].getValue());
				}

				final double[] solved = ThomasSolver.solve(m.getLowerDiagonal(), m.getMainDiagonal(), m.getUpperDiagonal(), lineRhs);

				for (int i1 = 0; i1 < n1; i1++) {
					out[flatten(i0, i1, i2)] = solved[i1];
				}
			}
		}

		return out;
	}

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

					m.getLowerDiagonal()[i2] = 0.0;
					m.getMainDiagonal()[i2] = 1.0 + lambda;
					m.getUpperDiagonal()[i2] = -lambda;
				}

				/*
				 * Last row: default identity, then overwrite only if upper I
				 * boundary is Dirichlet.
				 */
				m.getLowerDiagonal()[n2 - 1] = 0.0;
				m.getMainDiagonal()[n2 - 1] = 1.0;
				m.getUpperDiagonal()[n2 - 1] = 0.0;

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

				final double[] solved = ThomasSolver.solve(m.getLowerDiagonal(), m.getMainDiagonal(), m.getUpperDiagonal(), lineRhs);

				for (int i2 = 0; i2 < n2; i2++) {
					out[flatten(i0, i1, i2)] = solved[i2];
				}
			}
		}

		return out;
	}
}
