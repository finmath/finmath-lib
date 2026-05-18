package net.finmath.finitedifference.assetderivativevaluation.models;

import net.finmath.finitedifference.assetderivativevaluation.boundaries.FDBoundaryFactory;
import net.finmath.finitedifference.assetderivativevaluation.boundaries.FiniteDifferenceBoundary;
import net.finmath.finitedifference.assetderivativevaluation.products.FiniteDifferenceEquityProduct;
import net.finmath.finitedifference.boundaries.BoundaryCondition;
import net.finmath.finitedifference.grids.SpaceTimeDiscretization;
import net.finmath.marketdata.model.curves.CurveInterpolation.ExtrapolationMethod;
import net.finmath.marketdata.model.curves.CurveInterpolation.InterpolationEntity;
import net.finmath.marketdata.model.curves.CurveInterpolation.InterpolationMethod;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.DiscountCurveInterpolation;

/**
 * Finite difference model for option pricing under the SABR stochastic
 * volatility model.
 *
 * <p>
 * The state variables are {@code (S, alpha)} where
 * </p>
 * <ul>
 *   <li>{@code S} is the spot,</li>
 *   <li>{@code alpha} is the stochastic volatility factor.</li>
 * </ul>
 *
 * <p>
 * Under the risk-neutral dynamics with dividend yield, the model is given by
 * </p>
 * <pre>
 *   dS = (r - q) S dt + alpha S^beta dW_1
 *   d alpha = nu alpha ( rho dW_1 + sqrt(1-rho^2) dW_2 ).
 * </pre>
 *
 * <p>
 * The methods {@link #getDrift(double, double...)} and
 * {@link #getFactorLoading(double, double...)} follow the conventions used in
 * {@link FiniteDifferenceEquityModel}.
 * </p>
 *
 * <p>
 * Although one may formulate the PDE in transformed coordinates such as
 * {@code log(S)}, this class deliberately works in the variables {@code (S,
 * alpha)}
 * so that payoff functions can continue to use the spot directly.
 * </p>
 *
 * <p>
 * Boundary conditions are delegated through {@link FDBoundaryFactory}.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class FDMSabrModel implements FiniteDifferenceEquityModel {

	/**
	 * The initial spot.
	 */
	private final double initialSpot;
	/**
	 * The initial alpha.
	 */
	private final double initialAlpha;

	/**
	 * The risk free curve.
	 */
	private final DiscountCurve riskFreeCurve;
	/**
	 * The dividend yield curve.
	 */
	private final DiscountCurve dividendYieldCurve;

	// SABR parameters (risk-neutral)
	/**
	 * The beta.
	 */
	private final double beta;
	/**
	 * The nu.
	 */
	private final double nu;
	/**
	 * The rho.
	 */
	private final double rho;

	/**
	 * The space time discretization.
	 */
	private final SpaceTimeDiscretization spaceTimeDiscretization;

	/**
	 * Constructs a SABR finite difference model from discount curves.
	 *
	 * @param initialSpot Initial spot price.
	 * @param initialAlpha Initial volatility factor.
	 * @param riskFreeCurve Risk-free discount curve.
	 * @param dividendYieldCurve Dividend yield discount curve.
	 * @param beta SABR elasticity parameter.
	 * @param nu SABR volatility-of-volatility parameter.
	 * @param rho Correlation between spot and volatility Brownian motions.
	 * @param spaceTimeDiscretization Space-time discretization.
	 */
	public FDMSabrModel(
			final double initialSpot,
			final double initialAlpha,
			final DiscountCurve riskFreeCurve,
			final DiscountCurve dividendYieldCurve,
			final double beta,
			final double nu,
			final double rho,
			final SpaceTimeDiscretization spaceTimeDiscretization) {

		this.initialSpot = initialSpot;
		this.initialAlpha = initialAlpha;
		this.riskFreeCurve = riskFreeCurve;
		this.dividendYieldCurve = dividendYieldCurve;

		this.beta = beta;
		this.nu = nu;
		this.rho = rho;

		this.spaceTimeDiscretization = spaceTimeDiscretization;
	}

	/**
	 * Constructs a SABR finite difference model with zero dividend yield.
	 *
	 * @param initialSpot Initial spot price.
	 * @param initialAlpha Initial volatility factor.
	 * @param riskFreeCurve Risk-free discount curve.
	 * @param beta SABR elasticity parameter.
	 * @param nu SABR volatility-of-volatility parameter.
	 * @param rho Correlation between spot and volatility Brownian motions.
	 * @param spaceTimeDiscretization Space-time discretization.
	 */
	public FDMSabrModel(
			final double initialSpot,
			final double initialAlpha,
			final DiscountCurve riskFreeCurve,
			final double beta,
			final double nu,
			final double rho,
			final SpaceTimeDiscretization spaceTimeDiscretization) {

		this.initialSpot = initialSpot;
		this.initialAlpha = initialAlpha;
		this.riskFreeCurve = riskFreeCurve;

		final double[] times = new double[] {0.0, 1.0};
		final double[] givenAnnualizedZeroRates = new double[] {0.0, 0.0};
		final InterpolationMethod interpolationMethod = InterpolationMethod.LINEAR;
		final InterpolationEntity interpolationEntity = InterpolationEntity.LOG_OF_VALUE_PER_TIME;
		final ExtrapolationMethod extrapolationMethod = ExtrapolationMethod.CONSTANT;

		this.dividendYieldCurve =
				DiscountCurveInterpolation.createDiscountCurveFromAnnualizedZeroRates(
						"dividendCurve",
						null,
						times,
						givenAnnualizedZeroRates,
						interpolationMethod,
						extrapolationMethod,
						interpolationEntity);

		this.beta = beta;
		this.nu = nu;
		this.rho = rho;

		this.spaceTimeDiscretization = spaceTimeDiscretization;
	}

	/**
	 * Constructs a SABR finite difference model from constant rates.
	 *
	 * @param initialSpot Initial spot price.
	 * @param initialAlpha Initial volatility factor.
	 * @param riskFreeRate Constant risk-free rate.
	 * @param dividendYieldRate Constant dividend yield rate.
	 * @param beta SABR elasticity parameter.
	 * @param nu SABR volatility-of-volatility parameter.
	 * @param rho Correlation between spot and volatility Brownian motions.
	 * @param spaceTimeDiscretization Space-time discretization.
	 */
	public FDMSabrModel(
			final double initialSpot,
			final double initialAlpha,
			final double riskFreeRate,
			final double dividendYieldRate,
			final double beta,
			final double nu,
			final double rho,
			final SpaceTimeDiscretization spaceTimeDiscretization) {

		this.initialSpot = initialSpot;
		this.initialAlpha = initialAlpha;

		final double[] times = new double[] {0.0, 1.0};
		final InterpolationMethod interpolationMethod = InterpolationMethod.LINEAR;
		final InterpolationEntity interpolationEntity = InterpolationEntity.VALUE;
		final ExtrapolationMethod extrapolationMethod = ExtrapolationMethod.CONSTANT;

		final double[] givenAnnualizedZeroRates = new double[] {riskFreeRate, riskFreeRate};
		this.riskFreeCurve =
				DiscountCurveInterpolation.createDiscountCurveFromAnnualizedZeroRates(
						"riskFreeCurve",
						null,
						times,
						givenAnnualizedZeroRates,
						interpolationMethod,
						extrapolationMethod,
						interpolationEntity);

		final double[] givenAnnualizedZeroRates1 = new double[] {dividendYieldRate, dividendYieldRate};
		this.dividendYieldCurve =
				DiscountCurveInterpolation.createDiscountCurveFromAnnualizedZeroRates(
						"dividendCurve",
						null,
						times,
						givenAnnualizedZeroRates1,
						interpolationMethod,
						extrapolationMethod,
						interpolationEntity);

		this.beta = beta;
		this.nu = nu;
		this.rho = rho;

		this.spaceTimeDiscretization = spaceTimeDiscretization;
	}

	/**
	 * Constructs a SABR finite difference model from a constant risk-free rate
	 * and zero dividend yield.
	 *
	 * @param initialSpot Initial spot price.
	 * @param initialAlpha Initial volatility factor.
	 * @param riskFreeRate Constant risk-free rate.
	 * @param beta SABR elasticity parameter.
	 * @param nu SABR volatility-of-volatility parameter.
	 * @param rho Correlation between spot and volatility Brownian motions.
	 * @param spaceTimeDiscretization Space-time discretization.
	 */
	public FDMSabrModel(
			final double initialSpot,
			final double initialAlpha,
			final double riskFreeRate,
			final double beta,
			final double nu,
			final double rho,
			final SpaceTimeDiscretization spaceTimeDiscretization) {

		this.initialSpot = initialSpot;
		this.initialAlpha = initialAlpha;

		final double[] times = new double[] {0.0, 1.0};
		final InterpolationMethod interpolationMethod = InterpolationMethod.LINEAR;
		final InterpolationEntity interpolationEntity = InterpolationEntity.VALUE;
		final ExtrapolationMethod extrapolationMethod = ExtrapolationMethod.CONSTANT;

		final double[] givenAnnualizedZeroRates = new double[] {riskFreeRate, riskFreeRate};
		this.riskFreeCurve =
				DiscountCurveInterpolation.createDiscountCurveFromAnnualizedZeroRates(
						"riskFreeCurve",
						null,
						times,
						givenAnnualizedZeroRates,
						interpolationMethod,
						extrapolationMethod,
						interpolationEntity);

		final double[] givenAnnualizedZeroRates1 = new double[] {0.0, 0.0};
		this.dividendYieldCurve =
				DiscountCurveInterpolation.createDiscountCurveFromAnnualizedZeroRates(
						"dividendCurve",
						null,
						times,
						givenAnnualizedZeroRates1,
						interpolationMethod,
						extrapolationMethod,
						interpolationEntity);

		this.beta = beta;
		this.nu = nu;
		this.rho = rho;

		this.spaceTimeDiscretization = spaceTimeDiscretization;
	}

	@Override
	public DiscountCurve getRiskFreeCurve() {
		return riskFreeCurve;
	}

	@Override
	public DiscountCurve getDividendYieldCurve() {
		return dividendYieldCurve;
	}

	/**
	 * Returns the initial value of the state vector {@code (S, alpha)}.
	 *
	 * @return The initial state vector.
	 */
	@Override
	public double[] getInitialValue() {
		return new double[] {initialSpot, initialAlpha};
	}

	/**
	 * Returns the initial spot.
	 *
	 * @return The initial spot.
	 */
	public double getInitialSpot() {
		return initialSpot;
	}

	/**
	 * Returns the initial volatility factor.
	 *
	 * @return The initial volatility factor.
	 */
	public double getInitialAlpha() {
		return initialAlpha;
	}

	/**
	 * Returns the SABR elasticity parameter.
	 *
	 * @return The parameter {@code beta}.
	 */
	public double getBeta() {
		return beta;
	}

	/**
	 * Returns the SABR volatility-of-volatility parameter.
	 *
	 * @return The parameter {@code nu}.
	 */
	public double getNu() {
		return nu;
	}

	/**
	 * Returns the correlation between spot and volatility Brownian motions.
	 *
	 * @return The parameter {@code rho}.
	 */
	public double getRho() {
		return rho;
	}

	@Override
	public SpaceTimeDiscretization getSpaceTimeDiscretization() {
		return spaceTimeDiscretization;
	}

	@Override
	public double[] getDrift(final double time, final double... stateVariables) {
		// stateVariables[0] = S, stateVariables[1] = alpha
		final double s = stateVariables.length > 0 ? stateVariables[0] : initialSpot;
		final double alpha = stateVariables.length > 1 ? Math.max(0.0, stateVariables[1]) : initialAlpha;

		double t = time;
		if (t == 0.0) {
			t = 0.000001;
		}

		final double riskFreeDiscountFactor = getRiskFreeCurve().getDiscountFactor(t);
		final double r = -Math.log(riskFreeDiscountFactor) / t;

		final double dividendDiscountFactor = getDividendYieldCurve().getDiscountFactor(t);
		final double q = -Math.log(dividendDiscountFactor) / t;

		final double muS = (r - q) * s;
		final double muAlpha = 0.0;

		return new double[] {muS, muAlpha};
	}

	@Override
	public double[][] getFactorLoading(final double time, final double... stateVariables) {
		// Factors are independent. Correlation rho is embedded in the second
		// row.
		final double s = stateVariables.length > 0 ? Math.max(0.0, stateVariables[0]) : initialSpot;
		final double alpha = stateVariables.length > 1 ? Math.max(0.0, stateVariables[1]) : initialAlpha;

		final double sToBeta = Math.pow(s, beta);
		final double sqrtOneMinusRho2 = Math.sqrt(Math.max(0.0, 1.0 - rho * rho));

		final double[][] b = new double[2][2];

		// dS = ... + alpha * S^beta dW1
		b[0][0] = alpha * sToBeta;
		b[0][1] = 0.0;

		// d alpha = ... + nu alpha ( rho dW1 + sqrt(1-rho^2) dW2 )
		b[1][0] = nu * alpha * rho;
		b[1][1] = nu * alpha * sqrtOneMinusRho2;

		return b;
	}

	/**
	 * Convenience method returning the covariance matrix {@code a = B B^T},
	 * derived from {@link #getFactorLoading(double, double...)}.
	 *
	 * @param time Evaluation time.
	 * @param stateVariables State variables.
	 * @return The covariance matrix.
	 */
	public double[][] getCovariance(final double time, final double... stateVariables) {
		final double[][] b = getFactorLoading(time, stateVariables);

		final double a00 = b[0][0] * b[0][0] + b[0][1] * b[0][1];
		final double a01 = b[0][0] * b[1][0] + b[0][1] * b[1][1];
		final double a11 = b[1][0] * b[1][0] + b[1][1] * b[1][1];

		return new double[][] {
			{a00, a01},
			{a01, a11}
		};
	}

	@Override
	public BoundaryCondition[] getBoundaryConditionsAtLowerBoundary(
			final FiniteDifferenceEquityProduct product,
			final double time,
			final double... riskFactors) {

		final FiniteDifferenceBoundary boundary = FDBoundaryFactory.createBoundary(this, product);
		return boundary.getBoundaryConditionsAtLowerBoundary(product, time, riskFactors);
	}

	@Override
	public BoundaryCondition[] getBoundaryConditionsAtUpperBoundary(
			final FiniteDifferenceEquityProduct product,
			final double time,
			final double... riskFactors) {

		final FiniteDifferenceBoundary boundary = FDBoundaryFactory.createBoundary(this, product);
		return boundary.getBoundaryConditionsAtUpperBoundary(product, time, riskFactors);
	}

	@Override
	public FiniteDifferenceEquityModel getCloneWithModifiedSpaceTimeDiscretization(
			final SpaceTimeDiscretization newSpaceTimeDiscretization) {
		return new FDMSabrModel(
				initialSpot,
				initialAlpha,
				riskFreeCurve,
				dividendYieldCurve,
				beta,
				nu,
				rho,
				newSpaceTimeDiscretization
				);
	}
}
