package net.finmath.finitedifference.solvers.adi;

import net.finmath.finitedifference.assetderivativevaluation.models.FiniteDifferenceEquityModel;
import net.finmath.finitedifference.solvers.TridiagonalMatrix;

/**
 * Builds tridiagonal line matrices for alternating direction implicit (ADI)
 * solves for three-dimensional finite difference models.
 *
 * <p>
 * The state variables are interpreted generically as
 * </p>
 * <ul>
 *   <li>state variable 0: first spatial direction,</li>
 *   <li>state variable 1: second spatial direction,</li>
 *   <li>state variable 2: third spatial direction.</li>
 * </ul>
 *
 * <p>
 * The PDE operator is split into the directional parts
 * </p>
 * <ul>
 * <li>{@code A1}: drift and diffusion terms in the first spatial
 * direction,</li>
 * <li>{@code A2}: drift and diffusion terms in the second spatial
 * direction,</li>
 * <li>{@code A3}: drift and diffusion terms in the third spatial
 * direction.</li>
 * </ul>
 *
 * <p>
 * This builder returns the tridiagonal matrix corresponding to
 * </p>
 * <pre>
 * (I - theta * dt * A1)
 * </pre>
 * <p>
 * on one fixed slice of the second and third state variables, or
 * </p>
 * <pre>
 * (I - theta * dt * A2)
 * </pre>
 * <p>
 * on one fixed slice of the first and third state variables, or
 * </p>
 * <pre>
 * (I - theta * dt * A3)
 * </pre>
 * <p>
 * on one fixed slice of the first and second state variables.
 * </p>
 *
 * <p>
 * The coefficients are obtained from the model drift and factor loading and
 * are discretized by central finite differences on possibly non-uniform grids.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class ADI3DStencilBuilder {

	/**
	 * The model.
	 */
	private final FiniteDifferenceEquityModel model;
	/**
	 * The x0 grid.
	 */
	private final double[] x0Grid;
	/**
	 * The x1 grid.
	 */
	private final double[] x1Grid;
	/**
	 * The x2 grid.
	 */
	private final double[] x2Grid;

	/**
	 * Creates a stencil builder for three-dimensional ADI line solves.
	 *
	 * @param model The finite difference model providing drift and factor
	 *     loadings.
	 * @param x0Grid The grid for the first state variable.
	 * @param x1Grid The grid for the second state variable.
	 * @param x2Grid The grid for the third state variable.
	 */
	public ADI3DStencilBuilder(
			final FiniteDifferenceEquityModel model,
			final double[] x0Grid,
			final double[] x1Grid,
			final double[] x2Grid) {
		this.model = model;
		this.x0Grid = x0Grid;
		this.x1Grid = x1Grid;
		this.x2Grid = x2Grid;
	}

	/**
	 * Builds the tridiagonal matrix for the implicit solve in the first spatial
	 * direction on a fixed slice of the second and third state variables.
	 *
	 * <p>
	 * The returned matrix represents
	 * </p>
	 * <pre>
	 * (I - theta * dt * A1)
	 * </pre>
	 * <p>
	 * where {@code A1} contains only first-direction drift and diffusion terms.
	 * </p>
	 *
	 * @param time The running time at which coefficients are evaluated.
	 * @param dt The time step size.
	 * @param theta The ADI weight.
	 * @param x1Index The fixed index in the second state-variable grid.
	 * @param x2Index The fixed index in the third state-variable grid.
	 * @return The tridiagonal matrix for the first-direction implicit line
	 *     solve.
	 */
	public TridiagonalMatrix buildFirstDirectionLineMatrix(
			final double time,
			final double dt,
			final double theta,
			final int x1Index,
			final int x2Index) {

		final int n0 = x0Grid.length;
		final double x1 = x1Grid[x1Index];
		final double x2 = x2Grid[x2Index];

		final TridiagonalMatrix m = new TridiagonalMatrix(n0);

		/*
		 * Boundary rows are overwritten by the caller.
		 */
		m.diag[0] = 1.0;
		m.diag[n0 - 1] = 1.0;

		for (int i = 1; i < n0 - 1; i++) {
			final double x0 = x0Grid[i];

			final double[] drift = model.getDrift(time, x0, x1, x2);
			final double[][] factorLoading = model.getFactorLoading(time, x0, x1, x2);

			final double mu0 = drift[0];

			double a00 = 0.0;
			for (int f = 0; f < factorLoading[0].length; f++) {
				a00 += factorLoading[0][f] * factorLoading[0][f];
			}

			final double dxDown = x0Grid[i] - x0Grid[i - 1];
			final double dxUp = x0Grid[i + 1] - x0Grid[i];
			final double dxSum = dxDown + dxUp;
			final double dxProd = dxDown * dxUp;

			final double lowerA1 = -mu0 / dxSum + a00 / (dxSum * dxDown);
			final double diagA1  = -a00 / dxProd;
			final double upperA1 =  mu0 / dxSum + a00 / (dxSum * dxUp);

			m.lower[i] = -theta * dt * lowerA1;
			m.diag[i]  = 1.0 - theta * dt * diagA1;
			m.upper[i] = -theta * dt * upperA1;
		}

		return m;
	}

	/**
	 * Builds the tridiagonal matrix for the implicit solve in the second
	 * spatial
	 * direction on a fixed slice of the first and third state variables.
	 *
	 * <p>
	 * The returned matrix represents
	 * </p>
	 * <pre>
	 * (I - theta * dt * A2)
	 * </pre>
	 * <p>
	 * where {@code A2} contains only second-direction drift and diffusion
	 * terms.
	 * </p>
	 *
	 * @param time The running time at which coefficients are evaluated.
	 * @param dt The time step size.
	 * @param theta The ADI weight.
	 * @param x0Index The fixed index in the first state-variable grid.
	 * @param x2Index The fixed index in the third state-variable grid.
	 * @return The tridiagonal matrix for the second-direction implicit line
	 *     solve.
	 */
	public TridiagonalMatrix buildSecondDirectionLineMatrix(
			final double time,
			final double dt,
			final double theta,
			final int x0Index,
			final int x2Index) {

		final int n1 = x1Grid.length;
		final double x0 = x0Grid[x0Index];
		final double x2 = x2Grid[x2Index];

		final TridiagonalMatrix m = new TridiagonalMatrix(n1);

		/*
		 * Boundary rows are overwritten by the caller.
		 */
		m.diag[0] = 1.0;
		m.diag[n1 - 1] = 1.0;

		for (int j = 1; j < n1 - 1; j++) {
			final double x1 = x1Grid[j];

			final double[] drift = model.getDrift(time, x0, x1, x2);
			final double[][] factorLoading = model.getFactorLoading(time, x0, x1, x2);

			final double mu1 = drift[1];

			double a11 = 0.0;
			for (int f = 0; f < factorLoading[1].length; f++) {
				a11 += factorLoading[1][f] * factorLoading[1][f];
			}

			final double dxDown = x1Grid[j] - x1Grid[j - 1];
			final double dxUp = x1Grid[j + 1] - x1Grid[j];
			final double dxSum = dxDown + dxUp;
			final double dxProd = dxDown * dxUp;

			final double lowerA2 = -mu1 / dxSum + a11 / (dxSum * dxDown);
			final double diagA2  = -a11 / dxProd;
			final double upperA2 =  mu1 / dxSum + a11 / (dxSum * dxUp);

			m.lower[j] = -theta * dt * lowerA2;
			m.diag[j]  = 1.0 - theta * dt * diagA2;
			m.upper[j] = -theta * dt * upperA2;
		}

		return m;
	}

	/**
	 * Builds the tridiagonal matrix for the implicit solve in the third spatial
	 * direction on a fixed slice of the first and second state variables.
	 *
	 * <p>
	 * The returned matrix represents
	 * </p>
	 * <pre>
	 * (I - theta * dt * A3)
	 * </pre>
	 * <p>
	 * where {@code A3} contains only third-direction drift and diffusion terms.
	 * </p>
	 *
	 * @param time The running time at which coefficients are evaluated.
	 * @param dt The time step size.
	 * @param theta The ADI weight.
	 * @param x0Index The fixed index in the first state-variable grid.
	 * @param x1Index The fixed index in the second state-variable grid.
	 * @return The tridiagonal matrix for the third-direction implicit line
	 *     solve.
	 */
	public TridiagonalMatrix buildThirdDirectionLineMatrix(
			final double time,
			final double dt,
			final double theta,
			final int x0Index,
			final int x1Index) {

		final int n2 = x2Grid.length;
		final double x0 = x0Grid[x0Index];
		final double x1 = x1Grid[x1Index];

		final TridiagonalMatrix m = new TridiagonalMatrix(n2);

		/*
		 * Boundary rows are overwritten by the caller.
		 */
		m.diag[0] = 1.0;
		m.diag[n2 - 1] = 1.0;

		for (int k = 1; k < n2 - 1; k++) {
			final double x2 = x2Grid[k];

			final double[] drift = model.getDrift(time, x0, x1, x2);
			final double[][] factorLoading = model.getFactorLoading(time, x0, x1, x2);

			final double mu2 = drift[2];

			double a22 = 0.0;
			for (int f = 0; f < factorLoading[2].length; f++) {
				a22 += factorLoading[2][f] * factorLoading[2][f];
			}

			final double dxDown = x2Grid[k] - x2Grid[k - 1];
			final double dxUp = x2Grid[k + 1] - x2Grid[k];
			final double dxSum = dxDown + dxUp;
			final double dxProd = dxDown * dxUp;

			final double lowerA3 = -mu2 / dxSum + a22 / (dxSum * dxDown);
			final double diagA3  = -a22 / dxProd;
			final double upperA3 =  mu2 / dxSum + a22 / (dxSum * dxUp);

			m.lower[k] = -theta * dt * lowerA3;
			m.diag[k]  = 1.0 - theta * dt * diagA3;
			m.upper[k] = -theta * dt * upperA3;
		}

		return m;
	}
}
