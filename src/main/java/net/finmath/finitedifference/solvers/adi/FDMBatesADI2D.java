package net.finmath.finitedifference.solvers.adi;

import java.util.Optional;

import net.finmath.finitedifference.assetderivativevaluation.models.FDMBatesModel;
import net.finmath.finitedifference.assetderivativevaluation.models.JumpComponent;
import net.finmath.finitedifference.assetderivativevaluation.products.FiniteDifferenceEquityProduct;
import net.finmath.modelling.Exercise;

/**
 * ADI finite difference solver for the two-dimensional Bates PIDE.
 *
 * <p>
 * The local part of the Bates model is identical to the Heston model and is
 * therefore treated by the same stabilized Douglas-type ADI splitting already
 * implemented in {@link AbstractADI2D}.
 * </p>
 *
 * <p>
 * The jump contribution is injected explicitly on the asset dimension only. In
 * other words, the solver keeps the local ADI step unchanged and augments the
 * explicit operator by the non-local term
 * </p>
 *
 * <pre>
 * integral [ u(S exp(y), v) - u(S, v) - S (exp(y) - 1) partial_S u(S, v) ]
 * nu(dy).
 * </pre>
 *
 * <p>
 * The current implementation assumes:
 * </p>
 * <ul>
 *   <li>the jump component acts on state variable index {@code 0},</li>
 *   <li>state variables are {@code (S, v)},</li>
 *   <li>the non-local term is treated explicitly,</li>
 *   <li>interpolation in the asset direction is linear with constant
 *       extrapolation outside the asset grid.</li>
 * </ul>
 *
 * @author Alessandro Gnoatto
 */
public class FDMBatesADI2D extends AbstractADI2D {

	/**
	 * The default quadrature points per side.
	 */
	private static final int DEFAULT_QUADRATURE_POINTS_PER_SIDE = 200;

	/**
	 * The model.
	 */
	private final FDMBatesModel model;
	/**
	 * The jump component.
	 */
	private final JumpComponent jumpComponent;
	/**
	 * The quadrature points per side.
	 */
	private final int quadraturePointsPerSide;

	/**
	 * Creates a Bates ADI solver with a default number of quadrature cells on
	 * each side of zero.
	 *
	 * @param model The Bates finite-difference model.
	 * @param product The product to be valued.
	 * @param spaceTimeDiscretization The space-time discretization.
	 * @param exercise The exercise specification.
	 */
	public FDMBatesADI2D(
			final FDMBatesModel model,
			final FiniteDifferenceEquityProduct product,
			final net.finmath.finitedifference.grids.SpaceTimeDiscretization spaceTimeDiscretization,
			final Exercise exercise) {
		this(
				model,
				product,
				spaceTimeDiscretization,
				exercise,
				DEFAULT_QUADRATURE_POINTS_PER_SIDE
		);
	}

	/**
	 * Creates a Bates ADI solver.
	 *
	 * @param model The Bates finite-difference model.
	 * @param product The product to be valued.
	 * @param spaceTimeDiscretization The space-time discretization.
	 * @param exercise The exercise specification.
	 * @param quadraturePointsPerSide Number of midpoint quadrature cells used
	 *     on
	 *        each side of zero for the jump integral.
	 */
	public FDMBatesADI2D(
			final FDMBatesModel model,
			final FiniteDifferenceEquityProduct product,
			final net.finmath.finitedifference.grids.SpaceTimeDiscretization spaceTimeDiscretization,
			final Exercise exercise,
			final int quadraturePointsPerSide) {
		super(model, product, spaceTimeDiscretization, exercise);

		if (model == null) {
			throw new IllegalArgumentException("Model must not be null.");
		}
		if (quadraturePointsPerSide <= 0) {
			throw new IllegalArgumentException("quadraturePointsPerSide must be positive.");
		}

		final Optional<JumpComponent> optionalJumpComponent = model.getJumpComponent();
		if (!optionalJumpComponent.isPresent()) {
			throw new IllegalArgumentException("FDMBatesADI2D requires a non-empty jump component.");
		}
		if (optionalJumpComponent.get().getStateVariableIndex() != 0) {
			throw new IllegalArgumentException(
					"FDMBatesADI2D currently supports jumps only on state variable index 0."
			);
		}

		this.model = model;
		this.jumpComponent = optionalJumpComponent.get();
		this.quadraturePointsPerSide = quadraturePointsPerSide;
	}

	/**
	 * Returns the explicit operator used in the ADI predictor step.
	 *
	 * <p>
	 * This overrides the local explicit operator by adding the non-local jump
	 * contribution evaluated on the current time layer.
	 * </p>
	 *
	 * @param u The current flattened solution vector.
	 * @param time The current running time.
	 * @return The explicit operator applied to {@code u}.
	 */
	@Override
	protected double[] applyFullExplicitOperator(final double[] u, final double time) {
		final double[] localOperator = super.applyFullExplicitOperator(u, time);
		final double[] jumpContribution = computeExplicitJumpContribution(u, time);

		final double[] out = new double[getN()];
		for (int k = 0; k < getN(); k++) {
			out[k] = localOperator[k] + jumpContribution[k];
		}
		return out;
	}

	/**
	 * Computes the explicit jump contribution on the full two-dimensional grid.
	 *
	 * <p>
	 * The computation is performed slice by slice in the variance direction.
	 * For
	 * each fixed variance index, the solution is viewed as a one-dimensional
	 * function of the asset variable and the jump integral is evaluated on that
	 * slice.
	 * </p>
	 *
	 * @param u The current flattened solution vector.
	 * @param time The current running time.
	 * @return The jump contribution on the full flattened grid.
	 */
	protected double[] computeExplicitJumpContribution(final double[] u, final double time) {

		final double[] out = new double[getN()];

		for (int j = 0; j < getN1(); j++) {
			final double variance = getX1Grid()[j];

			final double[] slice = new double[getN0()];
			for (int i = 0; i < getN0(); i++) {
				slice[i] = u[flatten(i, j)];
			}

			final double[] firstDerivative = computeFirstDerivative(getX0Grid(), slice);

			for (int i = 0; i < getN0(); i++) {
				final double stock = getX0Grid()[i];
				final double lowerBound =
						jumpComponent.getLowerIntegrationBound(time, stock, variance);
				final double upperBound =
						jumpComponent.getUpperIntegrationBound(time, stock, variance);

				out[flatten(i, j)] = integrateJumpContributionAtNode(
						slice,
						firstDerivative,
						stock,
						variance,
						time,
						lowerBound,
						upperBound,
						i
				);
			}
		}

		return out;
	}

	/**
	 * Computes the jump integral at one grid node.
	 *
	 * @param slice The solution values on the current variance slice.
	 * @param firstDerivative The first asset derivative on the current slice.
	 * @param stock The stock value at the node.
	 * @param variance The variance value at the node.
	 * @param time The current running time.
	 * @param lowerBound The lower integration bound.
	 * @param upperBound The upper integration bound.
	 * @param stockIndex The index of the stock node on the current slice.
	 * @return The jump contribution at the given node.
	 */
	protected double integrateJumpContributionAtNode(
			final double[] slice,
			final double[] firstDerivative,
			final double stock,
			final double variance,
			final double time,
			final double lowerBound,
			final double upperBound,
			final int stockIndex) {

		if (!(lowerBound < upperBound)) {
			return 0.0;
		}

		double integral = 0.0;

		if (lowerBound < 0.0) {
			final double negativeUpper = Math.min(upperBound, 0.0);
			if (lowerBound < negativeUpper) {
				integral += midpointIntegrate(
						slice,
						firstDerivative[stockIndex],
						stock,
						variance,
						time,
						lowerBound,
						negativeUpper,
						quadraturePointsPerSide,
						stockIndex
				);
			}
		}

		if (upperBound > 0.0) {
			final double positiveLower = Math.max(lowerBound, 0.0);
			if (positiveLower < upperBound) {
				integral += midpointIntegrate(
						slice,
						firstDerivative[stockIndex],
						stock,
						variance,
						time,
						positiveLower,
						upperBound,
						quadraturePointsPerSide,
						stockIndex
				);
			}
		}

		return integral;
	}

	/**
	 * Evaluates one subinterval of the jump integral by midpoint quadrature.
	 *
	 * @param slice The solution values on the current variance slice.
	 * @param derivativeAtStock The first asset derivative at the current node.
	 * @param stock The stock value at the current node.
	 * @param variance The variance value at the current node.
	 * @param time The current running time.
	 * @param a The lower endpoint of the quadrature interval.
	 * @param b The upper endpoint of the quadrature interval.
	 * @param numberOfCells The number of midpoint cells.
	 * @param stockIndex The stock index of the current node.
	 * @return The quadrature approximation on the given interval.
	 */
	protected double midpointIntegrate(
			final double[] slice,
			final double derivativeAtStock,
			final double stock,
			final double variance,
			final double time,
			final double a,
			final double b,
			final int numberOfCells,
			final int stockIndex) {

		if (!(a < b)) {
			return 0.0;
		}

		final double h = (b - a) / numberOfCells;
		double integral = 0.0;

		for (int k = 0; k < numberOfCells; k++) {
			final double y = a + (k + 0.5) * h;
			final double shiftedStock = stock * Math.exp(y);
			final double shiftedValue =
					interpolateLinearWithConstantExtrapolation(getX0Grid(), slice, shiftedStock);

			final double levyDensity =
					jumpComponent.getLevyDensity(time, y, stock, variance);

			final double integrand =
					shiftedValue
					- slice[stockIndex]
					- stock * (Math.exp(y) - 1.0) * derivativeAtStock;

			integral += integrand * levyDensity;
		}

		return h * integral;
	}

	/**
	 * Computes the first derivative of a one-dimensional slice.
	 *
	 * @param grid The grid in the asset direction.
	 * @param values The values on the slice.
	 * @return The approximate first derivative.
	 */
	protected double[] computeFirstDerivative(final double[] grid, final double[] values) {

		final int n = grid.length;
		final double[] derivative = new double[n];

		if (n < 2) {
			throw new IllegalArgumentException("Need at least two spatial grid points.");
		}

		if (n == 2) {
			final double slope = (values[1] - values[0]) / (grid[1] - grid[0]);
			derivative[0] = slope;
			derivative[1] = slope;
			return derivative;
		}

		for (int i = 0; i < n; i++) {
			if (i == 0) {
				final double h = grid[1] - grid[0];
				derivative[i] = (values[1] - values[0]) / h;
			} else if (i == n - 1) {
				final double h = grid[n - 1] - grid[n - 2];
				derivative[i] = (values[n - 1] - values[n - 2]) / h;
			} else {
				final double h0 = grid[i] - grid[i - 1];
				final double h1 = grid[i + 1] - grid[i];

				final double lowerWeight = -h1 / (h0 * (h0 + h1));
				final double diagWeight = (h1 - h0) / (h0 * h1);
				final double upperWeight = h0 / (h1 * (h0 + h1));

				derivative[i] =
						lowerWeight * values[i - 1]
						+ diagWeight * values[i]
						+ upperWeight * values[i + 1];
			}
		}

		return derivative;
	}

	/**
	 * Evaluates a slice at a shifted asset value by linear interpolation with
	 * constant extrapolation outside the grid range.
	 *
	 * @param grid The asset grid.
	 * @param values The values on the current variance slice.
	 * @param x The evaluation point.
	 * @return The interpolated value.
	 */
	protected double interpolateLinearWithConstantExtrapolation(
			final double[] grid,
			final double[] values,
			final double x) {

		if (x <= grid[0]) {
			return values[0];
		}
		if (x >= grid[grid.length - 1]) {
			return values[values.length - 1];
		}

		int left = 0;
		int right = grid.length - 1;

		while (right - left > 1) {
			final int mid = (left + right) >>> 1;
			if (grid[mid] <= x) {
				left = mid;
			} else {
				right = mid;
			}
		}

		final double x0 = grid[left];
		final double x1 = grid[left + 1];
		final double u0 = values[left];
		final double u1 = values[left + 1];

		final double weight = (x - x0) / (x1 - x0);
		return (1.0 - weight) * u0 + weight * u1;
	}

	/**
	 * Returns the associated Bates model.
	 *
	 * @return The Bates model.
	 */
	public FDMBatesModel getModel() {
		return model;
	}
}
