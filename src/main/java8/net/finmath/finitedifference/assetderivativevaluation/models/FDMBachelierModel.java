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
 * Finite difference model for option pricing under the Bachelier (normal)
 * model.
 *
 * <p>
 * The model assumes an arithmetic Brownian motion for the underlying:
 * </p>
 *
 * <pre>
 * dS(t) = (r(t) - q(t)) S(t) dt + sigma dW(t)
 * </pre>
 *
 * <p>
 * where {@code r(t)} is the risk-free rate, {@code q(t)} is the dividend yield,
 * and {@code sigma} is a
 * constant (normal) volatility.
 * </p>
 *
 * <p>
 * This class follows the same design principles as the other finite-difference
 * equity model implementations:
 * it provides drift and factor loadings via {@link #getDrift(double,
 * double...)} and
 * {@link #getFactorLoading(double, double...)}, and delegates boundary values
 * to a boundary implementation
 * created through {@link FDBoundaryFactory}.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class FDMBachelierModel implements FiniteDifferenceEquityModel {

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
	 * The volatility.
	 */
	private final double volatility;

	/**
	 * The space time discretization.
	 */
	private final SpaceTimeDiscretization spaceTimeDiscretization;

	/**
	 * Creates a Bachelier model from discount curves.
	 *
	 * @param initialValue            Initial underlying value {@code S(0)}.
	 * @param riskFreeCurve           Risk-free discount curve.
	 * @param dividendYieldCurve      Dividend yield discount curve.
	 * @param volatility              Normal volatility {@code sigma}.
	 * @param spaceTimeDiscretization Space-time discretization.
	 */
	public FDMBachelierModel(
			final double initialValue,
			final DiscountCurve riskFreeCurve,
			final DiscountCurve dividendYieldCurve,
			final double volatility,
			final SpaceTimeDiscretization spaceTimeDiscretization) {

		this.initialValue = initialValue;
		this.riskFreeCurve = riskFreeCurve;
		this.dividendYieldCurve = dividendYieldCurve;
		this.volatility = volatility;
		this.spaceTimeDiscretization = spaceTimeDiscretization;
	}

	/**
	 * Convenience constructor without dividend yield curve (i.e., {@code q =
	 * 0}).
	 *
	 * @param initialValue            Initial underlying value {@code S(0)}.
	 * @param riskFreeCurve           Risk-free discount curve.
	 * @param volatility              Normal volatility {@code sigma}.
	 * @param spaceTimeDiscretization Space-time discretization.
	 */
	public FDMBachelierModel(
			final double initialValue,
			final DiscountCurve riskFreeCurve,
			final double volatility,
			final SpaceTimeDiscretization spaceTimeDiscretization) {

		this(
				initialValue,
				riskFreeCurve,
				createFlatZeroCurve("dividendCurve"),
				volatility,
				spaceTimeDiscretization);
	}

	/**
	 * Creates a Bachelier model from constant rates (flat curves).
	 *
	 * @param initialValue            Initial underlying value {@code S(0)}.
	 * @param riskFreeRate            Constant risk-free rate {@code r}.
	 * @param dividendYieldRate       Constant dividend yield {@code q}.
	 * @param volatility              Normal volatility {@code sigma}.
	 * @param spaceTimeDiscretization Space-time discretization.
	 */
	public FDMBachelierModel(
			final double initialValue,
			final double riskFreeRate,
			final double dividendYieldRate,
			final double volatility,
			final SpaceTimeDiscretization spaceTimeDiscretization) {

		this(
				initialValue,
				createFlatCurveFromZeroRate("riskFreeCurve", riskFreeRate),
				createFlatCurveFromZeroRate("dividendCurve", dividendYieldRate),
				volatility,
				spaceTimeDiscretization);
	}

	/**
	 * Convenience constructor from a constant risk-free rate (flat curve) and
	 * {@code q = 0}.
	 *
	 * @param initialValue            Initial underlying value {@code S(0)}.
	 * @param riskFreeRate            Constant risk-free rate {@code r}.
	 * @param volatility              Normal volatility {@code sigma}.
	 * @param spaceTimeDiscretization Space-time discretization.
	 */
	public FDMBachelierModel(
			final double initialValue,
			final double riskFreeRate,
			final double volatility,
			final SpaceTimeDiscretization spaceTimeDiscretization) {

		this(initialValue, riskFreeRate, 0.0, volatility, spaceTimeDiscretization);
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
	 * Returns {@code S(0)}.
	 *
	 * @return Initial underlying value.
	 */
	@Override
	public double[] getInitialValue() {
		return new double[] {initialValue};
	}

	/**
	 * Returns the normal volatility {@code sigma}.
	 *
	 * @return Normal volatility.
	 */
	public double getVolatility() {
		return volatility;
	}

	@Override
	public SpaceTimeDiscretization getSpaceTimeDiscretization() {
		return spaceTimeDiscretization;
	}

	@Override
	public double[] getDrift(final double time, final double... stateVariables) {
		final double s = stateVariables.length > 0 ? stateVariables[0] : initialValue;

		final double tSafe = time == 0.0 ? 1e-6 : time;

		final double r = -Math.log(riskFreeCurve.getDiscountFactor(tSafe)) / tSafe;
		final double q = -Math.log(dividendYieldCurve.getDiscountFactor(tSafe)) / tSafe;

		// Bachelier drift in spot coordinates: (r-q) * S
		return new double[] {(r - q) * s };
	}

	@Override
	public double[][] getFactorLoading(final double time, final double... stateVariables) {
		// Single factor, additive diffusion: sigma dW
		return new double[][] {{volatility } };
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

	private static DiscountCurve createFlatZeroCurve(final String name) {
		return createFlatCurveFromZeroRate(name, 0.0);
	}

	private static DiscountCurve createFlatCurveFromZeroRate(final String name, final double zeroRate) {
		final double[] times = new double[] {0.0, 1.0 };
		final double[] zeroRates = new double[] {zeroRate, zeroRate };

		final InterpolationMethod interpolationMethod = InterpolationMethod.LINEAR;
		final ExtrapolationMethod extrapolationMethod = ExtrapolationMethod.CONSTANT;
		final InterpolationEntity interpolationEntity = InterpolationEntity.LOG_OF_VALUE_PER_TIME;

		return DiscountCurveInterpolation.createDiscountCurveFromAnnualizedZeroRates(
				name,
				null,
				times,
				zeroRates,
				interpolationMethod,
				extrapolationMethod,
				interpolationEntity);
	}

	@Override
	public FiniteDifferenceEquityModel getCloneWithModifiedSpaceTimeDiscretization(
			final SpaceTimeDiscretization newSpaceTimeDiscretization) {
		return new FDMBachelierModel(
				initialValue,
				riskFreeCurve,
				dividendYieldCurve,
				volatility,
				newSpaceTimeDiscretization
				);
	}
}
