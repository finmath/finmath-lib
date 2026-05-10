package net.finmath.finitedifference.solvers.adi;

import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;

import net.finmath.finitedifference.assetderivativevaluation.models.FDMSabrModel;
import net.finmath.finitedifference.assetderivativevaluation.products.FiniteDifferenceEquityProduct;
import net.finmath.finitedifference.grids.SpaceTimeDiscretization;
import net.finmath.finitedifference.solvers.ThomasSolver;
import net.finmath.finitedifference.solvers.TridiagonalMatrix;
import net.finmath.modelling.Exercise;

/**
 * Barrier-aware ADI finite difference solver for the two-dimensional SABR PDE.
 *
 * <p>
 * This class supports:
 * </p>
 * <ul>
 * <li>standard direct barrier solves via {@link
 * BarrierPDEMode#OUT_STANDARD},</li>
 *   <li>pre-hit continuation solves for direct knock-in pricing via
 *       {@link BarrierPDEMode#IN_PRE_HIT}.</li>
 * </ul>
 *
 * <p>
 * In pre-hit mode, the barrier is treated as an interface on the first state
 * variable and the activated post-hit value is injected through an
 * {@link ActivatedBarrierTrace2D}. The exercise obstacle is intentionally
 * disabled in that mode.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class FDMBarrierSabrADI2D extends AbstractADI2D {

	/**
	 * The sabr model.
	 */
	private final FDMSabrModel sabrModel;
	/**
	 * The barrier mode.
	 */
	private final BarrierPDEMode barrierMode;
	/**
	 * The pre hit specification.
	 */
	private final BarrierPreHitSpecification preHitSpecification;

	/**
	 * Creates the barrier-aware SABR ADI solver.
	 *
	 * @param model The SABR model.
	 * @param product The finite-difference product.
	 * @param spaceTimeDiscretization The discretization to use.
	 * @param exercise The exercise specification.
	 * @param barrierMode The barrier PDE mode.
	 * @param preHitSpecification The pre-hit specification. Required in
	 *     IN_PRE_HIT mode.
	 */
	public FDMBarrierSabrADI2D(
			final FDMSabrModel model,
			final FiniteDifferenceEquityProduct product,
			final SpaceTimeDiscretization spaceTimeDiscretization,
			final Exercise exercise,
			final BarrierPDEMode barrierMode,
			final BarrierPreHitSpecification preHitSpecification) {
		super(model, product, spaceTimeDiscretization, exercise);

		if (barrierMode == null) {
			throw new IllegalArgumentException("barrierMode must not be null.");
		}
		if (barrierMode == BarrierPDEMode.IN_PRE_HIT && preHitSpecification == null) {
			throw new IllegalArgumentException(
					"preHitSpecification must not be null in IN_PRE_HIT mode.");
		}

		this.sabrModel = model;
		this.barrierMode = barrierMode;
		this.preHitSpecification = preHitSpecification;
	}

	@Override
	public double[][] getValues(final double time, final DoubleUnaryOperator valueAtMaturity) {
		return getValues(
				time,
				(x0, x1) -> valueAtMaturity.applyAsDouble(x0),
				(runningTime, x0, x1) -> valueAtMaturity.applyAsDouble(x0));
	}

	@Override
	public double[][] getValues(final double time, final DoubleBinaryOperator valueAtMaturity) {
		return getValues(
				time,
				valueAtMaturity,
				(runningTime, x0, x1) -> valueAtMaturity.applyAsDouble(x0, x1));
	}

	@Override
	public double[][] getValues(
			final double time,
			final DoubleBinaryOperator valueAtMaturity,
			final DoubleTernaryOperator exerciseValue) {

		final int timeLength = spaceTimeDiscretization.getTimeDiscretization().getNumberOfTimeSteps() + 1;
		final int numberOfTimeSteps = spaceTimeDiscretization.getTimeDiscretization().getNumberOfTimeSteps();

		double[] u = new double[n];
		for (int j = 0; j < n1; j++) {
			for (int i = 0; i < n0; i++) {
				u[flatten(i, j)] = valueAtMaturity.applyAsDouble(x0Grid[i], x1Grid[j]);
			}
		}

		applyOuterBoundaries(time, u);
		applyInternalConstraints(time, u);
		applyBarrierTraceIfNeeded(time, u);
		u = sanitize(u);

		final RealMatrix solutionSurface = new Array2DRowRealMatrix(n, timeLength);
		solutionSurface.setColumn(0, u.clone());

		for (int m = 0; m < numberOfTimeSteps; m++) {
			final double dt = spaceTimeDiscretization.getTimeDiscretization().getTimeStep(m);

			final double tauNext = spaceTimeDiscretization.getTimeDiscretization().getTime(m + 1);
			final double runningTimeNext =
					spaceTimeDiscretization.getTimeDiscretization().getLastTime() - tauNext;

			u = performStableDouglasStep(u, runningTimeNext, dt);

			applyInternalConstraints(runningTimeNext, u);
			applyOuterBoundaries(runningTimeNext, u);
			applyBarrierTraceIfNeeded(runningTimeNext, u);

			if (!isPreHitMode()) {
				applyExerciseObstacleIfNeeded(runningTimeNext, tauNext, u, exerciseValue);
			}

			applyInternalConstraints(runningTimeNext, u);
			applyOuterBoundaries(runningTimeNext, u);
			applyBarrierTraceIfNeeded(runningTimeNext, u);

			u = sanitize(u);

			solutionSurface.setColumn(m + 1, u.clone());
		}

		return solutionSurface.getData();
	}

	@Override
	protected double[] solveFirstDirectionLines(
			final double[] rhs,
			final double time,
			final double dt) {

		if (!isPreHitMode()) {
			return super.solveFirstDirectionLines(rhs, time, dt);
		}

		final double[] out = rhs.clone();

		for (int j = 0; j < n1; j++) {
			final TridiagonalMatrix matrix =
					stencilBuilder.buildFirstDirectionLineMatrix(time, dt, theta, j);

			final double[] lineRhs = new double[n0];
			for (int i = 0; i < n0; i++) {
				lineRhs[i] = rhs[flatten(i, j)];
			}

			final double lowerBoundaryValue =
					getLowerBoundaryValueForFirstDirection(time, j, lineRhs[0]);
			final double upperBoundaryValue =
					getUpperBoundaryValueForFirstDirection(time, j, lineRhs[n0 - 1]);

			overwriteBoundaryRow(matrix, lineRhs, 0, lowerBoundaryValue);
			overwriteBoundaryRow(matrix, lineRhs, n0 - 1, upperBoundaryValue);

			overwriteBarrierTraceRow(matrix, lineRhs, j, time);

			final double[] solved = ThomasSolver.solve(
					matrix.lower,
					matrix.diag,
					matrix.upper,
					lineRhs
			);

			for (int i = 0; i < n0; i++) {
				out[flatten(i, j)] = solved[i];
			}
		}

		return out;
	}

	@Override
	protected void applyOuterBoundaries(final double time, final double[] u) {
		super.applyOuterBoundaries(time, u);
		applyBarrierTraceIfNeeded(time, u);
	}

	/**
	 * Returns true if this solver is in pre-hit continuation mode.
	 *
	 * @return True if in pre-hit mode.
	 */
	protected boolean isPreHitMode() {
		return barrierMode == BarrierPDEMode.IN_PRE_HIT;
	}

	/**
	 * Overwrites the barrier interface row in a first-direction tridiagonal
	 * system.
	 *
	 * @param matrix The tridiagonal matrix.
	 * @param rhs The corresponding right-hand side.
	 * @param secondIndex The second-state index.
	 * @param runningTime The running time.
	 */
	protected void overwriteBarrierTraceRow(
			final TridiagonalMatrix matrix,
			final double[] rhs,
			final int secondIndex,
			final double runningTime) {

		if (!isPreHitMode()) {
			return;
		}

		final int barrierRow = preHitSpecification.getBarrierSpotIndex();
		final double barrierValue = getBarrierTraceValue(secondIndex, runningTime);

		overwriteBoundaryRow(matrix, rhs, barrierRow, barrierValue);
	}

	/**
	 * Applies the barrier trace directly to the flattened solution vector.
	 *
	 * @param runningTime The running time.
	 * @param u The solution vector.
	 */
	protected void applyBarrierTraceIfNeeded(
			final double runningTime,
			final double[] u) {

		if (!isPreHitMode()) {
			return;
		}

		final int barrierRow = preHitSpecification.getBarrierSpotIndex();

		for (int j = 0; j < n1; j++) {
			u[flatten(barrierRow, j)] = getBarrierTraceValue(j, runningTime);
		}
	}

	/**
	 * Returns the activated barrier trace value at the given second-state row
	 * and running time.
	 *
	 * @param secondIndex The second-state index.
	 * @param runningTime The running time.
	 * @return The corresponding trace value.
	 */
	protected double getBarrierTraceValue(
			final int secondIndex,
			final double runningTime) {

		final double tau =
				Math.max(
						0.0,
						spaceTimeDiscretization.getTimeDiscretization().getLastTime() - runningTime
				);

		return preHitSpecification.getActivatedTrace().getValue(secondIndex, tau);
	}

	/**
	 * Returns the SABR model used by this solver.
	 *
	 * @return The SABR model.
	 */
	public FDMSabrModel getSabrModel() {
		return sabrModel;
	}

	/**
	 * Returns the barrier PDE mode.
	 *
	 * @return The barrier mode.
	 */
	public BarrierPDEMode getBarrierMode() {
		return barrierMode;
	}

	/**
	 * Returns the pre-hit specification, or null if not in pre-hit mode.
	 *
	 * @return The pre-hit specification.
	 */
	public BarrierPreHitSpecification getPreHitSpecification() {
		return preHitSpecification;
	}
}
