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
 * Finite difference model for option pricing under the Heston stochastic
 * volatility model.
 *
 * <p>
 * State variables are {@code (S, v)} where {@code S} is the spot and {@code v}
 * is the
 * instantaneous variance.
 * </p>
 *
 * <p>
 * The methods {@link #getDrift(double, double...)} and {@link
 * #getFactorLoading(double, double...)}
 * follow the conventions used in {@link FiniteDifferenceEquityModel}:
 * </p>
 * <ul>
 * <li>{@code getDrift} returns the vector of drifts for the state
 * variables.</li>
 * <li>{@code getFactorLoading} returns the matrix of factor loadings (here 2
 * factors), producing
 *       the correct covariance with correlation {@code rho}.</li>
 * </ul>
 *
 * <p>
 * Boundary conditions are provided through the {@link FiniteDifferenceBoundary}
 * interface methods,
 * via {@link FDBoundaryFactory}.
 * </p>
 *
 * @author Alessandro Gnoatto
 * @author Enrico De Vecchi
 */
public class FDMHestonModel implements FiniteDifferenceEquityModel {

	/**
	 * The initial spot.
	 */
	private final double initialSpot;
	/**
	 * The initial variance.
	 */
	private final double initialVariance;

	/**
	 * The risk free curve.
	 */
	private final DiscountCurve riskFreeCurve;
	/**
	 * The dividend yield curve.
	 */
	private final DiscountCurve dividendYieldCurve;

	// Heston parameters (risk-neutral)
	/**
	 * The kappa.
	 */
	private final double kappa;
	/**
	 * The theta v.
	 */
	private final double thetaV;
	/**
	 * The sigma.
	 */
	private final double sigma;    // vol-of-vol (often eta)
	/**
	 * The rho.
	 */
	private final double rho;

	/**
	 * The space time discretization.
	 */
	private final SpaceTimeDiscretization spaceTimeDiscretization;

	/**
	 * Constructs a Heston finite difference model for option pricing.
	 *
	 * @param initialSpot            Initial spot price.
	 * @param initialVariance        Initial variance.
	 * @param riskFreeCurve          Risk-free discount curve.
	 * @param dividendYieldCurve     Dividend yield discount curve.
	 * @param kappa                  Mean reversion speed of variance.
	 * @param thetaV                 Long-term mean of variance.
	 * @param sigma                  Vol-of-vol parameter.
	 * @param rho Correlation between spot and variance Brownian motions.
	 * @param spaceTimeDiscretization Grid object defining the spatial
	 *     discretization.
	 */
	public FDMHestonModel(
			final double initialSpot,
			final double initialVariance,
			final DiscountCurve riskFreeCurve,
			final DiscountCurve dividendYieldCurve,
			final double kappa,
			final double thetaV,
			final double sigma,
			final double rho,
			final SpaceTimeDiscretization spaceTimeDiscretization) {

		this.initialSpot = initialSpot;
		this.initialVariance = initialVariance;
		this.riskFreeCurve = riskFreeCurve;
		this.dividendYieldCurve = dividendYieldCurve;

		this.kappa = kappa;
		this.thetaV = thetaV;
		this.sigma = sigma;
		this.rho = rho;

		this.spaceTimeDiscretization = spaceTimeDiscretization;
	}

	/**
	 * Constructs a Heston finite difference model for option pricing without
	 * dividend yield curve
	 * (i.e. dividend yield is assumed to be zero).
	 *
	 * @param initialSpot            Initial spot price.
	 * @param initialVariance        Initial variance.
	 * @param riskFreeCurve          Risk-free discount curve.
	 * @param kappa                  Mean reversion speed of variance.
	 * @param thetaV                 Long-term mean of variance.
	 * @param sigma                  Vol-of-vol parameter.
	 * @param rho Correlation between spot and variance Brownian motions.
	 * @param spaceTimeDiscretization Grid object defining the spatial
	 *     discretization.
	 */
	public FDMHestonModel(
			final double initialSpot,
			final double initialVariance,
			final DiscountCurve riskFreeCurve,
			final double kappa,
			final double thetaV,
			final double sigma,
			final double rho,
			final SpaceTimeDiscretization spaceTimeDiscretization) {

		this.initialSpot = initialSpot;
		this.initialVariance = initialVariance;
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

		this.kappa = kappa;
		this.thetaV = thetaV;
		this.sigma = sigma;
		this.rho = rho;

		this.spaceTimeDiscretization = spaceTimeDiscretization;
	}

	/**
	 * Constructs a Heston finite difference model for option pricing from
	 * constant rates.
	 *
	 * @param initialSpot            Initial spot price.
	 * @param initialVariance        Initial variance.
	 * @param riskFreeRate           Constant risk-free rate.
	 * @param dividendYieldRate      Constant dividend yield rate.
	 * @param kappa                  Mean reversion speed of variance.
	 * @param thetaV                 Long-term mean of variance.
	 * @param sigma                  Vol-of-vol parameter.
	 * @param rho Correlation between spot and variance Brownian motions.
	 * @param spaceTimeDiscretization Grid object defining the spatial
	 *     discretization.
	 */
	public FDMHestonModel(
			final double initialSpot,
			final double initialVariance,
			final double riskFreeRate,
			final double dividendYieldRate,
			final double kappa,
			final double thetaV,
			final double sigma,
			final double rho,
			final SpaceTimeDiscretization spaceTimeDiscretization) {

		this.initialSpot = initialSpot;
		this.initialVariance = initialVariance;

		final double[] times = new double[] {0.0, 1.0};
		final InterpolationMethod interpolationMethod = InterpolationMethod.LINEAR;
		final InterpolationEntity interpolationEntity = InterpolationEntity.VALUE;
		final ExtrapolationMethod extrapolationMethod = ExtrapolationMethod.CONSTANT;

		final double[] givenAnnualizedZeroRates = new double[] {riskFreeRate, riskFreeRate};
		this.riskFreeCurve =
				DiscountCurveInterpolation.createDiscountCurveFromAnnualizedZeroRates(
						"dividendCurve",
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

		this.kappa = kappa;
		this.thetaV = thetaV;
		this.sigma = sigma;
		this.rho = rho;

		this.spaceTimeDiscretization = spaceTimeDiscretization;
	}

	/**
	 * Constructs a Heston finite difference model for option pricing from a
	 * constant risk-free
	 * rate and zero dividend yield.
	 *
	 * @param initialSpot            Initial spot price.
	 * @param initialVariance        Initial variance.
	 * @param riskFreeRate           Constant risk-free rate.
	 * @param kappa                  Mean reversion speed of variance.
	 * @param thetaV                 Long-term mean of variance.
	 * @param sigma                  Vol-of-vol parameter.
	 * @param rho Correlation between spot and variance Brownian motions.
	 * @param spaceTimeDiscretization Grid object defining the spatial
	 *     discretization.
	 */
	public FDMHestonModel(
			final double initialSpot,
			final double initialVariance,
			final double riskFreeRate,
			final double kappa,
			final double thetaV,
			final double sigma,
			final double rho,
			final SpaceTimeDiscretization spaceTimeDiscretization) {

		this.initialSpot = initialSpot;
		this.initialVariance = initialVariance;

		final double[] times = new double[] {0.0, 1.0};
		final InterpolationMethod interpolationMethod = InterpolationMethod.LINEAR;
		final InterpolationEntity interpolationEntity = InterpolationEntity.VALUE;
		final ExtrapolationMethod extrapolationMethod = ExtrapolationMethod.CONSTANT;

		final double[] givenAnnualizedZeroRates = new double[] {riskFreeRate, riskFreeRate};
		this.riskFreeCurve =
				DiscountCurveInterpolation.createDiscountCurveFromAnnualizedZeroRates(
						"dividendCurve",
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

		this.kappa = kappa;
		this.thetaV = thetaV;
		this.sigma = sigma;
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
	 * Returns the initial value of the system of SDEs.
	 *
	 * @return The initial spot.
	 */
	public double[] getInitialValue() {
		return new double[] {initialSpot, initialVariance};
	}

	/**
	 * Returns the initial variance value.
	 *
	 * @return The initial variance.
	 */
	/*public double getInitialVariance() {
		return initialVariance;
	}*/

	/**
	 * Returns the mean reversion speed of variance.
	 *
	 * @return The parameter {@code kappa}.
	 */
	public double getKappa() {
		return kappa;
	}

	/**
	 * Returns the long-term mean of variance.
	 *
	 * @return The parameter {@code thetaV}.
	 */
	public double getThetaV() {
		return thetaV;
	}

	/**
	 * Returns the vol-of-vol parameter.
	 *
	 * @return The parameter {@code sigma}.
	 */
	public double getSigma() {
		return sigma;
	}

	/**
	 * Returns the correlation between the Brownian motions.
	 *
	 * @return The parameter {@code rho}.
	 */
	public double getRho() {
		return rho;
	}

	@Override
	public SpaceTimeDiscretization getSpaceTimeDiscretization() {
		return this.spaceTimeDiscretization;
	}

	@Override
	public double[] getDrift(final double time, final double... stateVariables) {
		// stateVariables[0]=S, stateVariables[1]=v
		final double v = stateVariables.length > 1 ? stateVariables[1] : initialVariance;

		double t = time;
		if (t == 0) {
			t = 0.000001;
		}

		final double rF = getRiskFreeCurve().getDiscountFactor(t);
		final double r = -Math.log(rF) / t;

		final double dY = getDividendYieldCurve().getDiscountFactor(t);
		final double q = -Math.log(dY) / t;

		final double mu = (r - q) * stateVariables[0];		// drift for S
		final double muV = kappa * (thetaV - v);	// drift of the CIR

		return new double[] {mu, muV};
	}

	@Override
	public double[][] getFactorLoading(final double time, final double... stateVariables) {
		// Factors are independent. Correlation rho is embedded in the second
		// row.
		final double stock = stateVariables.length > 0 ? stateVariables[0] : initialSpot;
		final double variance = Math.max(0.0, stateVariables.length > 1 ? stateVariables[1] : initialVariance);
		final double sqrtV = Math.sqrt(variance);

		final double sqrtOneMinusRho2 = Math.sqrt(Math.max(0.0, 1.0 - rho * rho));

		// state dimension 2, factor dimension 2
		final double[][] b = new double[2][2];

		// dS = ... + S * sqrt(v) dW1
		b[0][0] = sqrtV * stock;	// percentage loading for S: will be multiplied by S in PDE operator
		b[0][1] = 0.0;

		// dv = ... + sigma * sqrt(v) ( rho dW1 + sqrt(1-rho^2) dW2 )
		b[1][0] = sigma * sqrtV * rho;
		b[1][1] = sigma * sqrtV * sqrtOneMinusRho2;

		return b;
	}

	/**
	 * Convenience method returning the covariance matrix {@code a = B B^T},
	 * derived from
	 * {@link #getFactorLoading(double, double...)}.
	 *
	 * @param time           Evaluation time.
	 * @param stateVariables State variables.
	 * @return The covariance matrix.
	 */
	public double[][] getCovariance(final double time, final double... stateVariables) {
		final double[][] factorLoading = getFactorLoading(time, stateVariables);

		final double a00 = factorLoading[0][0] * factorLoading[0][0] + factorLoading[0][1] * factorLoading[0][1];
		final double a01 = factorLoading[0][0] * factorLoading[1][0] + factorLoading[0][1] * factorLoading[1][1];
		final double a11 = factorLoading[1][0] * factorLoading[1][0] + factorLoading[1][1] * factorLoading[1][1];

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
		return new FDMHestonModel(
				initialSpot,
				initialVariance,
				riskFreeCurve,
				dividendYieldCurve,
				kappa,
				thetaV,
				sigma,
				rho,
				newSpaceTimeDiscretization
		);
	}
}
