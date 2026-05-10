package net.finmath.finitedifference.solvers.adi;

import net.finmath.finitedifference.assetderivativevaluation.models.FiniteDifferenceEquityModel;
import net.finmath.finitedifference.solvers.TridiagonalMatrix;

/**
 * Builds tridiagonal line matrices and exact directional operator actions
 * for alternating direction implicit (ADI) solves for two-dimensional
 * finite difference models.
 *
 * <p>
 * The state variables are interpreted generically as
 * </p>
 * <ul>
 *   <li>state variable 0: first spatial direction,</li>
 *   <li>state variable 1: second spatial direction.</li>
 * </ul>
 *
 * <p>
 * The PDE operator is split into the directional parts
 * </p>
 * <ul>
 * <li>{@code A1}: drift and diffusion terms in the first spatial
 * direction.</li>
 * <li>{@code A2}: drift and diffusion terms in the second spatial
 * direction.</li>
 * </ul>
 *
 * <p>
 * This builder returns tridiagonal matrices corresponding to
 * </p>
 * <pre>
 * (I - theta * dt * A1)
 * (I - theta * dt * A2)
 * </pre>
 * <p>
 * and also provides exact applications of {@code A1} and {@code A2} to a
 * flattened 2D state vector using the same coefficients.
 * </p>
 */
public class ADI2DStencilBuilder {

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
	 * The n0.
	 */
	private final int n0;
	/**
	 * The n1.
	 */
	private final int n1;

	/**
	 * Performs the operation.
	 *
	 * @param model The value.
	 * @param x0Grid The value.
	 * @param x1Grid The value.
	 */
	public ADI2DStencilBuilder(
			final FiniteDifferenceEquityModel model,
			final double[] x0Grid,
			final double[] x1Grid) {
		this.model = model;
		this.x0Grid = x0Grid;
		this.x1Grid = x1Grid;
		this.n0 = x0Grid.length;
		this.n1 = x1Grid.length;
	}

	/**
	 * Coefficients of the 1D three-point stencil
	 *
	 * <pre>
	 * lower * u[i-1] + diag * u[i] + upper * u[i+1]
	 * </pre>
	 */
	public static final class DirectionalCoefficients {
		/**
		 * The lower.
		 */
		private final double lower;
		/**
		 * The diag.
		 */
		private final double diag;
		/**
		 * The upper.
		 */
		private final double upper;

		/**
		 * Performs the operation.
		 *
		 * @param lower The value.
		 * @param diag The value.
		 * @param upper The value.
		 */
		public DirectionalCoefficients(
				final double lower,
				final double diag,
				final double upper) {
			this.lower = lower;
			this.diag = diag;
			this.upper = upper;
		}

		/**
		 * Returns the value.
		 *
		 * @return The value.
		 */
		public double getLower() {
			return lower;
		}

		/**
		 * Returns the value.
		 *
		 * @return The value.
		 */
		public double getDiag() {
			return diag;
		}

		/**
		 * Returns the value.
		 *
		 * @return The value.
		 */
		public double getUpper() {
			return upper;
		}
	}

	/**
	 * Performs the operation.
	 *
	 * @param time The value.
	 * @param dt The value.
	 * @param theta The value.
	 * @param x1Index The value.
	 * @return The value.
	 */
	public TridiagonalMatrix buildFirstDirectionLineMatrix(
			final double time,
			final double dt,
			final double theta,
			final int x1Index) {

		final TridiagonalMatrix m = new TridiagonalMatrix(n0);

		m.diag[0] = 1.0;
		m.diag[n0 - 1] = 1.0;

		for (int i = 1; i < n0 - 1; i++) {
			final DirectionalCoefficients c = getFirstDirectionCoefficients(time, i, x1Index);

			m.lower[i] = -theta * dt * c.getLower();
			m.diag[i]  = 1.0 - theta * dt * c.getDiag();
			m.upper[i] = -theta * dt * c.getUpper();
		}

		return m;
	}

	/**
	 * Performs the operation.
	 *
	 * @param time The value.
	 * @param dt The value.
	 * @param theta The value.
	 * @param x0Index The value.
	 * @return The value.
	 */
	public TridiagonalMatrix buildSecondDirectionLineMatrix(
			final double time,
			final double dt,
			final double theta,
			final int x0Index) {

		final TridiagonalMatrix m = new TridiagonalMatrix(n1);

		m.diag[0] = 1.0;
		m.diag[n1 - 1] = 1.0;

		for (int j = 1; j < n1 - 1; j++) {
			final DirectionalCoefficients c = getSecondDirectionCoefficients(time, x0Index, j);

			m.lower[j] = -theta * dt * c.getLower();
			m.diag[j]  = 1.0 - theta * dt * c.getDiag();
			m.upper[j] = -theta * dt * c.getUpper();
		}

		return m;
	}

	/**
	 * Performs the operation.
	 *
	 * @param u The value.
	 * @param time The value.
	 * @return The value.
	 */
	public double[] applyFirstDirectionOperator(final double[] u, final double time) {
		if (u.length != n0 * n1) {
			throw new IllegalArgumentException("State vector has wrong length.");
		}

		final double[] out = new double[u.length];

		for (int j = 0; j < n1; j++) {
			applyFirstDirectionOperatorOnSlice(u, out, time, j);
		}

		return out;
	}

	/**
	 * Performs the operation.
	 *
	 * @param u The value.
	 * @param time The value.
	 * @return The value.
	 */
	public double[] applySecondDirectionOperator(final double[] u, final double time) {
		if (u.length != n0 * n1) {
			throw new IllegalArgumentException("State vector has wrong length.");
		}

		final double[] out = new double[u.length];

		for (int i = 0; i < n0; i++) {
			applySecondDirectionOperatorOnSlice(u, out, time, i);
		}

		return out;
	}

	/**
	 * Performs the operation.
	 *
	 * @param u The value.
	 * @param out The value.
	 * @param time The value.
	 * @param x1Index The value.
	 */
	public void applyFirstDirectionOperatorOnSlice(
			final double[] u,
			final double[] out,
			final double time,
			final int x1Index) {

		for (int i = 1; i < n0 - 1; i++) {
			final DirectionalCoefficients c = getFirstDirectionCoefficients(time, i, x1Index);

			final int k = flatten(i, x1Index);
			out[k] =
					c.getLower() * u[flatten(i - 1, x1Index)]
					+ c.getDiag() * u[k]
					+ c.getUpper() * u[flatten(i + 1, x1Index)];
		}
	}

	/**
	 * Performs the operation.
	 *
	 * @param u The value.
	 * @param out The value.
	 * @param time The value.
	 * @param x0Index The value.
	 */
	public void applySecondDirectionOperatorOnSlice(
			final double[] u,
			final double[] out,
			final double time,
			final int x0Index) {

		for (int j = 1; j < n1 - 1; j++) {
			final DirectionalCoefficients c = getSecondDirectionCoefficients(time, x0Index, j);

			final int k = flatten(x0Index, j);
			out[k] =
					c.getLower() * u[flatten(x0Index, j - 1)]
					+ c.getDiag() * u[k]
					+ c.getUpper() * u[flatten(x0Index, j + 1)];
		}
	}

	/**
	 * Returns the value.
	 *
	 * @param time The value.
	 * @param x0Index The value.
	 * @param x1Index The value.
	 * @return The value.
	 */
	public DirectionalCoefficients getFirstDirectionCoefficients(
			final double time,
			final int x0Index,
			final int x1Index) {

		final double x0 = x0Grid[x0Index];
		final double x1 = x1Grid[x1Index];

		final double[] drift = model.getDrift(time, x0, x1);
		final double[][] factorLoading = model.getFactorLoading(time, x0, x1);

		final double mu0 = drift[0];

		double a00 = 0.0;
		for (int f = 0; f < factorLoading[0].length; f++) {
			a00 += factorLoading[0][f] * factorLoading[0][f];
		}

		final double dxDown = x0Grid[x0Index] - x0Grid[x0Index - 1];
		final double dxUp = x0Grid[x0Index + 1] - x0Grid[x0Index];
		final double dxSum = dxDown + dxUp;
		final double dxProd = dxDown * dxUp;

		final double lower = -mu0 / dxSum + a00 / (dxSum * dxDown);
		final double diag  = -a00 / dxProd;
		final double upper =  mu0 / dxSum + a00 / (dxSum * dxUp);

		return new DirectionalCoefficients(lower, diag, upper);
	}

	/**
	 * Returns the value.
	 *
	 * @param time The value.
	 * @param x0Index The value.
	 * @param x1Index The value.
	 * @return The value.
	 */
	public DirectionalCoefficients getSecondDirectionCoefficients(
			final double time,
			final int x0Index,
			final int x1Index) {

		final double x0 = x0Grid[x0Index];
		final double x1 = x1Grid[x1Index];

		final double[] drift = model.getDrift(time, x0, x1);
		final double[][] factorLoading = model.getFactorLoading(time, x0, x1);

		final double mu1 = drift[1];

		double a11 = 0.0;
		for (int f = 0; f < factorLoading[1].length; f++) {
			a11 += factorLoading[1][f] * factorLoading[1][f];
		}

		final double dxDown = x1Grid[x1Index] - x1Grid[x1Index - 1];
		final double dxUp = x1Grid[x1Index + 1] - x1Grid[x1Index];
		final double dxSum = dxDown + dxUp;
		final double dxProd = dxDown * dxUp;

		final double lower = -mu1 / dxSum + a11 / (dxSum * dxDown);
		final double diag  = -a11 / dxProd;
		final double upper =  mu1 / dxSum + a11 / (dxSum * dxUp);

		return new DirectionalCoefficients(lower, diag, upper);
	}

	private int flatten(final int i0, final int i1) {
		return i0 + i1 * n0;
	}
}
