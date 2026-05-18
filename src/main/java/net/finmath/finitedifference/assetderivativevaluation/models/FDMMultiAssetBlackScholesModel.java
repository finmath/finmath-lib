package net.finmath.finitedifference.assetderivativevaluation.models;

import java.time.LocalDate;
import java.util.Arrays;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.CholeskyDecomposition;
import org.apache.commons.math3.linear.NonPositiveDefiniteMatrixException;
import org.apache.commons.math3.linear.RealMatrix;

import net.finmath.finitedifference.assetderivativevaluation.boundaries.FDBoundaryFactory;
import net.finmath.finitedifference.assetderivativevaluation.boundaries.FiniteDifferenceBoundary;
import net.finmath.finitedifference.assetderivativevaluation.products.FiniteDifferenceEquityProduct;
import net.finmath.finitedifference.boundaries.BoundaryCondition;
import net.finmath.finitedifference.boundaries.StandardBoundaryCondition;
import net.finmath.finitedifference.grids.SpaceTimeDiscretization;
import net.finmath.marketdata.model.curves.CurveInterpolation.ExtrapolationMethod;
import net.finmath.marketdata.model.curves.CurveInterpolation.InterpolationEntity;
import net.finmath.marketdata.model.curves.CurveInterpolation.InterpolationMethod;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.DiscountCurveInterpolation;

/**
 * Finite-difference model for a multi-asset Black-Scholes market with constant
 * volatilities and constant instantaneous correlation.
 *
 * <p>
 * The model is dimension agnostic and describes an arbitrary number
 * {@code n >= 1} of risky assets
 * {@code (S_1, ..., S_n)}. For asset {@code i}, the dynamics under the
 * pricing measure are
 * </p>
 *
 * <p>
 * <i>
 * dS_i(t) = (r(t) - q_i(t)) S_i(t) dt + sigma_i S_i(t) dW_i(t),
 * </i>
 * </p>
 *
 * <p>
 * with instantaneous covariation structure
 * </p>
 *
 * <p>
 * <i>
 * d&lt;W_i, W_j&gt;_t = rho_{i,j} dt.
 * </i>
 * </p>
 *
 * <p>
 * Here {@code r(t)} is the risk-free rate implied by a single risk-free
 * discount
 * curve, while each asset has its own dividend-yield discount curve and hence
 * its own dividend yield {@code q_i(t)}.
 * </p>
 *
 * <p>
 * If {@code L} denotes a Cholesky factor of the correlation matrix
 * {@code rho}, then the factor-loading matrix is chosen as
 * </p>
 *
 * <p>
 * <i>
 * b_{i,j}(t,S) = sigma_i S_i L_{i,j},
 * </i>
 * </p>
 *
 * <p>
 * which yields the instantaneous covariance matrix
 * </p>
 *
 * <p>
 * <i>
 * (b b^T)_{i,j} = rho_{i,j} sigma_i sigma_j S_i S_j.
 * </i>
 * </p>
 *
 * <p>
 * The class is a model abstraction only. It is intentionally agnostic with
 * respect to the dimensional limits of the available PDE solvers. In
 * particular,
 * the model may be created for any dimension, even if a given finite-difference
 * solver currently supports only dimensions one, two, or three.
 * </p>
 *
 * <p>
 * For boundary conditions, this first implementation returns
 * {@link StandardBoundaryCondition#none()} in every spatial direction by
 * default.
 * This keeps the model generic and leaves product-specific asymptotic boundary
 * design to specialized boundary logic introduced later.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class FDMMultiAssetBlackScholesModel implements FiniteDifferenceEquityModel {

	/**
	 * The time floor.
	 */
	private static final double TIME_FLOOR = 1E-6;
	/**
	 * The correlation tolerance.
	 */
	private static final double CORRELATION_TOLERANCE = 1E-10;

	/**
	 * The initial values.
	 */
	private final double[] initialValues;
	/**
	 * The risk free curve.
	 */
	private final DiscountCurve riskFreeCurve;
	/**
	 * The dividend yield curves.
	 */
	private final DiscountCurve[] dividendYieldCurves;
	/**
	 * The volatilities.
	 */
	private final double[] volatilities;
	/**
	 * The correlation matrix.
	 */
	private final double[][] correlationMatrix;
	/**
	 * The cholesky factor.
	 */
	private final double[][] choleskyFactor;
	/**
	 * The space time discretization.
	 */
	private final SpaceTimeDiscretization spaceTimeDiscretization;

	/**
	 * Creates a multi-asset Black-Scholes finite-difference model.
	 *
	 * @param initialValues Initial asset values.
	 * @param riskFreeCurve Risk-free discount curve.
	 * @param dividendYieldCurves Dividend-yield discount curves, one per asset.
	 * @param volatilities Constant volatilities, one per asset.
	 * @param correlationMatrix Instantaneous correlation matrix.
	 * @param spaceTimeDiscretization Space-time discretization.
	 */
	public FDMMultiAssetBlackScholesModel(
			final double[] initialValues,
			final DiscountCurve riskFreeCurve,
			final DiscountCurve[] dividendYieldCurves,
			final double[] volatilities,
			final double[][] correlationMatrix,
			final SpaceTimeDiscretization spaceTimeDiscretization) {

		if (initialValues == null || initialValues.length == 0) {
			throw new IllegalArgumentException("initialValues must contain at least one asset.");
		}
		if (riskFreeCurve == null) {
			throw new IllegalArgumentException("riskFreeCurve must not be null.");
		}
		if (dividendYieldCurves == null) {
			throw new IllegalArgumentException("dividendYieldCurves must not be null.");
		}
		if (volatilities == null) {
			throw new IllegalArgumentException("volatilities must not be null.");
		}
		if (correlationMatrix == null) {
			throw new IllegalArgumentException("correlationMatrix must not be null.");
		}
		if (spaceTimeDiscretization == null) {
			throw new IllegalArgumentException("spaceTimeDiscretization must not be null.");
		}

		final int numberOfAssets = initialValues.length;

		if (dividendYieldCurves.length != numberOfAssets) {
			throw new IllegalArgumentException(
					"dividendYieldCurves must have the same length as initialValues.");
		}
		if (volatilities.length != numberOfAssets) {
			throw new IllegalArgumentException(
					"volatilities must have the same length as initialValues.");
		}
		if (spaceTimeDiscretization.getNumberOfSpaceGrids() != numberOfAssets) {
			throw new IllegalArgumentException(
					"The number of space grids must match the model dimension.");
		}

		for (int i = 0; i < numberOfAssets; i++) {
			if (initialValues[i] <= 0.0) {
				throw new IllegalArgumentException("All initial asset values must be strictly positive.");
			}
			if (dividendYieldCurves[i] == null) {
				throw new IllegalArgumentException("Dividend-yield curves must not contain null entries.");
			}
			if (volatilities[i] < 0.0) {
				throw new IllegalArgumentException("Volatilities must be non-negative.");
			}
		}

		validateCorrelationMatrix(correlationMatrix, numberOfAssets);

		this.initialValues = initialValues.clone();
		this.riskFreeCurve = riskFreeCurve;
		this.dividendYieldCurves = dividendYieldCurves.clone();
		this.volatilities = volatilities.clone();
		this.correlationMatrix = deepCopyMatrix(correlationMatrix);
		this.choleskyFactor = computeCholeskyFactor(correlationMatrix);
		this.spaceTimeDiscretization = spaceTimeDiscretization;
	}

	/**
	 * Creates a multi-asset Black-Scholes finite-difference model from constant
	 * rates and constant volatilities.
	 *
	 * @param initialValues Initial asset values.
	 * @param riskFreeRate Constant risk-free rate.
	 * @param dividendYieldRates Constant dividend-yield rates, one per asset.
	 * @param volatilities Constant volatilities, one per asset.
	 * @param correlationMatrix Instantaneous correlation matrix.
	 * @param spaceTimeDiscretization Space-time discretization.
	 */
	public FDMMultiAssetBlackScholesModel(
			final double[] initialValues,
			final double riskFreeRate,
			final double[] dividendYieldRates,
			final double[] volatilities,
			final double[][] correlationMatrix,
			final SpaceTimeDiscretization spaceTimeDiscretization) {
		this(
				initialValues,
				createFlatDiscountCurve("riskFreeCurve", riskFreeRate),
				createFlatDividendCurves(dividendYieldRates),
				volatilities,
				correlationMatrix,
				spaceTimeDiscretization
				);
	}

	/**
	 * Creates a multi-asset Black-Scholes finite-difference model with zero
	 * dividend
	 * yield for every asset.
	 *
	 * @param initialValues Initial asset values.
	 * @param riskFreeCurve Risk-free discount curve.
	 * @param volatilities Constant volatilities, one per asset.
	 * @param correlationMatrix Instantaneous correlation matrix.
	 * @param spaceTimeDiscretization Space-time discretization.
	 */
	public FDMMultiAssetBlackScholesModel(
			final double[] initialValues,
			final DiscountCurve riskFreeCurve,
			final double[] volatilities,
			final double[][] correlationMatrix,
			final SpaceTimeDiscretization spaceTimeDiscretization) {
		this(
				initialValues,
				riskFreeCurve,
				createZeroDividendCurves(initialValues.length),
				volatilities,
				correlationMatrix,
				spaceTimeDiscretization
				);
	}

	@Override
	public DiscountCurve getRiskFreeCurve() {
		return riskFreeCurve;
	}

	/**
	 * Returns the legacy single-asset dividend-yield accessor.
	 *
	 * <p>
	 * For true multi-asset models with more than one dividend curve, callers
	 * should
	 * use {@link #getDividendYieldCurves()} instead.
	 * </p>
	 *
	 * @return The unique dividend-yield curve if the model has dimension one.
	 */
	@Override
	public DiscountCurve getDividendYieldCurve() {
		if (dividendYieldCurves.length == 1) {
			return dividendYieldCurves[0];
		}

		throw new UnsupportedOperationException(
				"Multi-asset model: use getDividendYieldCurves() instead of getDividendYieldCurve().");
	}

	@Override
	public DiscountCurve[] getDividendYieldCurves() {
		return dividendYieldCurves.clone();
	}

	/**
	 * Returns the number of risky assets of the model.
	 *
	 * @return The number of risky assets.
	 */
	public int getNumberOfAssets() {
		return initialValues.length;
	}

	/**
	 * Returns the asset volatilities.
	 *
	 * @return The volatilities.
	 */
	public double[] getVolatilities() {
		return volatilities.clone();
	}

	/**
	 * Returns the instantaneous correlation matrix.
	 *
	 * @return The correlation matrix.
	 */
	public double[][] getCorrelationMatrix() {
		return deepCopyMatrix(correlationMatrix);
	}

	@Override
	public double[] getInitialValue() {
		return initialValues.clone();
	}

	@Override
	public double[] getDrift(final double time, final double... stateVariables) {
		validateStateVariables(stateVariables);

		final double effectiveTime = Math.max(time, TIME_FLOOR);

		final double riskFreeRate = getAnnualizedZeroRate(riskFreeCurve, effectiveTime);
		final double[] drift = new double[initialValues.length];

		for (int i = 0; i < initialValues.length; i++) {
			final double dividendYieldRate = getAnnualizedZeroRate(dividendYieldCurves[i], effectiveTime);
			drift[i] = (riskFreeRate - dividendYieldRate) * stateVariables[i];
		}

		return drift;
	}

	@Override
	public double[][] getFactorLoading(final double time, final double... stateVariables) {
		validateStateVariables(stateVariables);

		final int numberOfAssets = initialValues.length;
		final double[][] factorLoading = new double[numberOfAssets][numberOfAssets];

		for (int i = 0; i < numberOfAssets; i++) {
			final double sigmaTimesState = volatilities[i] * stateVariables[i];

			for (int j = 0; j < numberOfAssets; j++) {
				factorLoading[i][j] = sigmaTimesState * choleskyFactor[i][j];
			}
		}

		return factorLoading;
	}

	@Override
	public SpaceTimeDiscretization getSpaceTimeDiscretization() {
		return spaceTimeDiscretization;
	}

	@Override
	public BoundaryCondition[] getBoundaryConditionsAtLowerBoundary(
			final FiniteDifferenceEquityProduct product,
			final double time,
			final double... riskFactors) {

		final FiniteDifferenceBoundary boundary =
				FDBoundaryFactory.createBoundary(this, product);

		return boundary.getBoundaryConditionsAtLowerBoundary(product, time, riskFactors);
	}

	@Override
	public BoundaryCondition[] getBoundaryConditionsAtUpperBoundary(
			final FiniteDifferenceEquityProduct product,
			final double time,
			final double... riskFactors) {

		final FiniteDifferenceBoundary boundary =
				FDBoundaryFactory.createBoundary(this, product);

		return boundary.getBoundaryConditionsAtUpperBoundary(product, time, riskFactors);
	}


	@Override
	public FiniteDifferenceEquityModel getCloneWithModifiedSpaceTimeDiscretization(
			final SpaceTimeDiscretization newSpaceTimeDiscretization) {
		return new FDMMultiAssetBlackScholesModel(
				initialValues,
				riskFreeCurve,
				dividendYieldCurves,
				volatilities,
				correlationMatrix,
				newSpaceTimeDiscretization
				);
	}

	private void validateStateVariables(final double[] stateVariables) {
		if (stateVariables == null || stateVariables.length != initialValues.length) {
			throw new IllegalArgumentException(
					"stateVariables must have the same dimension as the model.");
		}
	}

	private static void validateCorrelationMatrix(final double[][] correlationMatrix, final int dimension) {
		if (correlationMatrix.length != dimension) {
			throw new IllegalArgumentException(
					"correlationMatrix must be square and match the model dimension.");
		}

		for (int i = 0; i < dimension; i++) {
			if (correlationMatrix[i] == null || correlationMatrix[i].length != dimension) {
				throw new IllegalArgumentException(
						"correlationMatrix must be square and match the model dimension.");
			}

			if (Math.abs(correlationMatrix[i][i] - 1.0) > CORRELATION_TOLERANCE) {
				throw new IllegalArgumentException(
						"The diagonal entries of correlationMatrix must be equal to 1.");
			}

			for (int j = 0; j < dimension; j++) {
				final double rho = correlationMatrix[i][j];

				if (rho < -1.0 - CORRELATION_TOLERANCE || rho > 1.0 + CORRELATION_TOLERANCE) {
					throw new IllegalArgumentException(
							"All correlation-matrix entries must lie in [-1,1].");
				}

				if (Math.abs(rho - correlationMatrix[j][i]) > CORRELATION_TOLERANCE) {
					throw new IllegalArgumentException("correlationMatrix must be symmetric.");
				}
			}
		}
	}

	private static double[][] computeCholeskyFactor(final double[][] correlationMatrix) {
		try {
			final RealMatrix matrix = new Array2DRowRealMatrix(correlationMatrix);
			return new CholeskyDecomposition(matrix, 1E-12, 1E-12).getL().getData();
		} catch (final NonPositiveDefiniteMatrixException exception) {
			throw new IllegalArgumentException(
					"correlationMatrix must be positive definite for Cholesky decomposition.",
					exception
					);
		}
	}

	private static double getAnnualizedZeroRate(final DiscountCurve curve, final double time) {
		final double safeTime = Math.max(time, TIME_FLOOR);
		final double discountFactor = curve.getDiscountFactor(safeTime);
		return -Math.log(discountFactor) / safeTime;
	}

	private static double[][] deepCopyMatrix(final double[][] matrix) {
		final double[][] copy = new double[matrix.length][];
		for (int i = 0; i < matrix.length; i++) {
			copy[i] = matrix[i].clone();
		}
		return copy;
	}

	private static DiscountCurve[] createZeroDividendCurves(final int dimension) {
		final DiscountCurve[] curves = new DiscountCurve[dimension];

		for (int i = 0; i < dimension; i++) {
			curves[i] = createFlatDiscountCurve("dividendCurve" + i, 0.0);
		}

		return curves;
	}

	private static DiscountCurve[] createFlatDividendCurves(final double[] dividendYieldRates) {
		if (dividendYieldRates == null || dividendYieldRates.length == 0) {
			throw new IllegalArgumentException(
					"dividendYieldRates must contain at least one entry.");
		}

		final DiscountCurve[] curves = new DiscountCurve[dividendYieldRates.length];

		for (int i = 0; i < dividendYieldRates.length; i++) {
			curves[i] = createFlatDiscountCurve("dividendCurve" + i, dividendYieldRates[i]);
		}

		return curves;
	}

	private static DiscountCurve createFlatDiscountCurve(final String name, final double rate) {
		final double[] times = new double[] {0.0, 1.0};
		final double[] givenAnnualizedZeroRates = new double[] {rate, rate };

		return DiscountCurveInterpolation.createDiscountCurveFromAnnualizedZeroRates(
				name,
				LocalDate.of(2010, 8, 1),
				times,
				givenAnnualizedZeroRates,
				InterpolationMethod.LINEAR,
				ExtrapolationMethod.CONSTANT,
				InterpolationEntity.VALUE
				);
	}

	@Override
	public String toString() {
		return "MultiAssetBlackScholesModel [initialValues=" + Arrays.toString(initialValues)
		+ ", volatilities=" + Arrays.toString(volatilities)
		+ ", dimension=" + initialValues.length
		+ "]";
	}
}
