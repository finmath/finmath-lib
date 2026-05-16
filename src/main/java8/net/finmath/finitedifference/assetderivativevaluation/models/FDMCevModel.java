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
 * Finite difference model for option pricing under the Constant Elasticity of
 * Variance (CEV) model.
 *
 * <p>
 * Under the risk-neutral measure the CEV dynamics are typically written as
 * </p>
 * <p>
 * {@code dS_t = (r(t) - q(t)) S_t dt + sigma * S_t^beta dW_t}.
 * </p>
 *
 * <p>
 * In this class, {@link #getDrift(double, double...)} returns the
 * <em>absolute drift</em>,
 * and {@link #getFactorLoading(double, double...)} returns the <em>absolute
 * factor loading</em>.
 * Hence, for CEV we return
 * {@code sigma * S^(beta)}.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class FDMCevModel implements FiniteDifferenceEquityModel {

	/**
	 * The initial value.
	 */
	private final double initialValue;
	/**
	 * The risk free curve.
	 */
	private final DiscountCurve riskFreeCurve;
	/**
	 * The dividend yield curve.
	 */
	private final DiscountCurve dividendYieldCurve;

	/**
	 * The sigma.
	 */
	private final double sigma;
	/**
	 * The beta.
	 */
	private final double beta;

	/**
	 * The space time discretization.
	 */
	private final SpaceTimeDiscretization spaceTimeDiscretization;

	/**
	 * Minimum time used to avoid division by zero.
	 */
	private static final double MINIMUM_TIME = 1.0E-6;

	/**
	 * Creates a CEV model instance using explicit discount curves for {@code r}
	 * and {@code q}.
	 *
	 * @param initialValue Initial spot {@code S(0)}.
	 * @param riskFreeCurve Risk-free discount curve.
	 * @param dividendYieldCurve Dividend yield discount curve.
	 * @param sigma CEV volatility scale parameter {@code sigma}.
	 * @param beta CEV elasticity parameter {@code beta}.
	 * @param spaceTimeDiscretization The space-time discretization.
	 */
	public FDMCevModel(
			final double initialValue,
			final DiscountCurve riskFreeCurve,
			final DiscountCurve dividendYieldCurve,
			final double sigma,
			final double beta,
			final SpaceTimeDiscretization spaceTimeDiscretization) {

		this.initialValue = initialValue;
		this.riskFreeCurve = riskFreeCurve;
		this.dividendYieldCurve = dividendYieldCurve;
		this.sigma = sigma;
		this.beta = beta;
		this.spaceTimeDiscretization = spaceTimeDiscretization;
	}

	/**
	 * Creates a CEV model instance assuming zero dividend yield ({@code q =
	 * 0}),
	 * consistent with the constructor style of {@code FDMBlackScholesModel}.
	 *
	 * @param initialValue Initial spot {@code S(0)}.
	 * @param riskFreeCurve Risk-free discount curve.
	 * @param sigma CEV volatility scale parameter {@code sigma}.
	 * @param beta CEV elasticity parameter {@code beta}.
	 * @param spaceTimeDiscretization The space-time discretization.
	 */
	public FDMCevModel(
			final double initialValue,
			final DiscountCurve riskFreeCurve,
			final double sigma,
			final double beta,
			final SpaceTimeDiscretization spaceTimeDiscretization) {

		this.initialValue = initialValue;
		this.riskFreeCurve = riskFreeCurve;

		final double[] times = new double[] {0.0, 1.0 };
		final double[] givenAnnualizedZeroRates = new double[] {0.0, 0.0 };
		final InterpolationMethod interpolationMethod = InterpolationMethod.LINEAR;
		final InterpolationEntity interpolationEntity = InterpolationEntity.LOG_OF_VALUE_PER_TIME;
		final ExtrapolationMethod extrapolationMethod = ExtrapolationMethod.CONSTANT;

		this.dividendYieldCurve = DiscountCurveInterpolation.createDiscountCurveFromAnnualizedZeroRates(
				"dividendCurve",
				null,
				times,
				givenAnnualizedZeroRates,
				interpolationMethod,
				extrapolationMethod,
				interpolationEntity
		);

		this.sigma = sigma;
		this.beta = beta;
		this.spaceTimeDiscretization = spaceTimeDiscretization;
	}

	/**
	 * Creates a CEV model instance using constant rates {@code r} and {@code
	 * q}.
	 *
	 * @param initialValue Initial spot {@code S(0)}.
	 * @param riskFreeRate Constant risk-free rate {@code r}.
	 * @param dividendYieldRate Constant dividend yield {@code q}.
	 * @param sigma CEV volatility scale parameter {@code sigma}.
	 * @param beta CEV elasticity parameter {@code beta}.
	 * @param spaceTimeDiscretization The space-time discretization.
	 */
	public FDMCevModel(
			final double initialValue,
			final double riskFreeRate,
			final double dividendYieldRate,
			final double sigma,
			final double beta,
			final SpaceTimeDiscretization spaceTimeDiscretization) {

		this.initialValue = initialValue;

		final double[] timesR = new double[] {0.0, 1.0 };
		final double[] zeroRatesR = new double[] {riskFreeRate, riskFreeRate };
		final InterpolationMethod interpolationMethodR = InterpolationMethod.LINEAR;
		final InterpolationEntity interpolationEntityR = InterpolationEntity.VALUE;
		final ExtrapolationMethod extrapolationMethodR = ExtrapolationMethod.CONSTANT;

		this.riskFreeCurve = DiscountCurveInterpolation.createDiscountCurveFromAnnualizedZeroRates(
				"riskFreeCurve",
				null,
				timesR,
				zeroRatesR,
				interpolationMethodR,
				extrapolationMethodR,
				interpolationEntityR
		);

		final double[] timesQ = new double[] {0.0, 1.0 };
		final double[] zeroRatesQ = new double[] {dividendYieldRate, dividendYieldRate };
		final InterpolationMethod interpolationMethodQ = InterpolationMethod.LINEAR;
		final InterpolationEntity interpolationEntityQ = InterpolationEntity.VALUE;
		final ExtrapolationMethod extrapolationMethodQ = ExtrapolationMethod.CONSTANT;

		this.dividendYieldCurve = DiscountCurveInterpolation.createDiscountCurveFromAnnualizedZeroRates(
				"dividendCurve",
				null,
				timesQ,
				zeroRatesQ,
				interpolationMethodQ,
				extrapolationMethodQ,
				interpolationEntityQ
		);

		this.sigma = sigma;
		this.beta = beta;
		this.spaceTimeDiscretization = spaceTimeDiscretization;
	}

	/**
	 * Creates a CEV model instance using constant {@code r} and assuming zero
	 * dividend yield ({@code q = 0}).
	 *
	 * @param initialValue Initial spot {@code S(0)}.
	 * @param riskFreeRate Constant risk-free rate {@code r}.
	 * @param sigma CEV volatility scale parameter {@code sigma}.
	 * @param beta CEV elasticity parameter {@code beta}.
	 * @param spaceTimeDiscretization The space-time discretization.
	 */
	public FDMCevModel(
			final double initialValue,
			final double riskFreeRate,
			final double sigma,
			final double beta,
			final SpaceTimeDiscretization spaceTimeDiscretization) {

		this.initialValue = initialValue;

		final double[] timesR = new double[] {0.0, 1.0 };
		final double[] zeroRatesR = new double[] {riskFreeRate, riskFreeRate };
		final InterpolationMethod interpolationMethodR = InterpolationMethod.LINEAR;
		final InterpolationEntity interpolationEntityR = InterpolationEntity.VALUE;
		final ExtrapolationMethod extrapolationMethodR = ExtrapolationMethod.CONSTANT;

		this.riskFreeCurve = DiscountCurveInterpolation.createDiscountCurveFromAnnualizedZeroRates(
				"riskFreeCurve",
				null,
				timesR,
				zeroRatesR,
				interpolationMethodR,
				extrapolationMethodR,
				interpolationEntityR
		);

		final double[] timesQ = new double[] {0.0, 1.0 };
		final double[] zeroRatesQ = new double[] {0.0, 0.0 };
		final InterpolationMethod interpolationMethodQ = InterpolationMethod.LINEAR;
		final InterpolationEntity interpolationEntityQ = InterpolationEntity.VALUE;
		final ExtrapolationMethod extrapolationMethodQ = ExtrapolationMethod.CONSTANT;

		this.dividendYieldCurve = DiscountCurveInterpolation.createDiscountCurveFromAnnualizedZeroRates(
				"dividendCurve",
				null,
				timesQ,
				zeroRatesQ,
				interpolationMethodQ,
				extrapolationMethodQ,
				interpolationEntityQ
		);

		this.sigma = sigma;
		this.beta = beta;
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
	 * Returns the initial spot {@code S(0)}.
	 *
	 * @return The initial spot.
	 */
	public double[] getInitialValue() {
		return new double[] {initialValue};
	}

	/**
	 * Returns the CEV volatility scale parameter {@code sigma}.
	 *
	 * @return {@code sigma}.
	 */
	public double getSigma() {
		return sigma;
	}

	/**
	 * Returns the CEV elasticity parameter {@code beta}.
	 *
	 * @return {@code beta}.
	 */
	public double getBeta() {
		return beta;
	}

	@Override
	public SpaceTimeDiscretization getSpaceTimeDiscretization() {
		return spaceTimeDiscretization;
	}

	@Override
	public double[] getDrift(final double time, final double... stateVariables) {
		final double effectiveTime = time == 0.0 ? MINIMUM_TIME : time;

		final double[] result = new double[1];

		final double rF = getRiskFreeCurve().getDiscountFactor(effectiveTime);
		final double riskFreeRate = -Math.log(rF) / effectiveTime;

		final double dY = getDividendYieldCurve().getDiscountFactor(effectiveTime);
		final double dividendYieldRate = -Math.log(dY) / effectiveTime;

		result[0] = (riskFreeRate - dividendYieldRate) * stateVariables[0];
		return result;
	}

	@Override
	public double[][] getFactorLoading(final double time, final double... stateVariables) {
		final double S = stateVariables.length > 0 ? stateVariables[0] : initialValue;

		final double loading;
		if (S <= 0.0) {
			loading = 0.0;
		} else {
			loading = sigma * Math.pow(S, beta);
		}

		return new double[][] {{loading } };
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
		return new FDMCevModel(
				initialValue,
				riskFreeCurve,
				dividendYieldCurve,
				sigma,
				beta,
				newSpaceTimeDiscretization
		);
	}
}
