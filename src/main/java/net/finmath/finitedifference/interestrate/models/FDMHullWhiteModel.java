package net.finmath.finitedifference.interestrate.models;

import net.finmath.finitedifference.boundaries.BoundaryCondition;
import net.finmath.finitedifference.grids.SpaceTimeDiscretization;
import net.finmath.finitedifference.interestrate.boundaries.FDInterestRateBoundaryFactory;
import net.finmath.finitedifference.interestrate.boundaries.FiniteDifferenceInterestRateBoundary;
import net.finmath.finitedifference.interestrate.products.FiniteDifferenceInterestRateProduct;
import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.montecarlo.interestrate.models.covariance.ShortRateVolatilityModel;
import net.finmath.time.TimeDiscretization;

/**
 * One-factor Hull-White interest-rate model for finite-difference valuation.
 *
 * <p>
 * The model is formulated in shifted-state form
 * </p>
 *
 * <p>
 * <i>
 * r^{d}(t) = x(t) + \alpha(t),
 * </i>
 * </p>
 *
 * <p>
 * where {@code r^d} denotes the discounting short rate, {@code x} is the
 * one-dimensional Markovian state variable, and {@code \alpha} is the
 * deterministic shift ensuring exact fit to the initial discount curve.
 * </p>
 *
 * <p>
 * The Markovian state variable follows
 * </p>
 *
 * <p>
 * <i>
 * d x(t) = -a(t) x(t)\,dt + \sigma(t)\,dW(t), \qquad x(0)=0,
 * </i>
 * </p>
 *
 * <p>
 * where the mean reversion {@code a} and the instantaneous short-rate
 * volatility {@code \sigma} are provided by a
 * {@link ShortRateVolatilityModel}. As in the Monte Carlo Hull-White
 * implementation of finmath, these coefficients are interpreted as piecewise
 * constant on the volatility-model time discretization.
 * </p>
 *
 * <p>
 * The discount bond is represented in affine form
 * </p>
 *
 * <p>
 * <i>
 * P^{d}(t,T) = A(t,T)\exp\bigl(-B(t,T)x(t)\bigr).
 * </i>
 * </p>
 *
 * <p>
 * In a multiple-curve setting, forwarding rates are constructed through a
 * deterministic multiplicative-spread extension. If
 * {@code \delta = T_{2}-T_{1}}, we set
 * </p>
 *
 * <p>
 * <i>
 * 1 + \delta F^{j}(t;T_{1},T_{2})
 * =
 * S^{j}(T_{1},T_{2})
 * \bigl(1 + \delta F^{d}(t;T_{1},T_{2})\bigr),
 * </i>
 * </p>
 *
 * <p>
 * where {@code F^d} is the discount-curve forward rate implied by the model and
 * the deterministic spread factor is calibrated from the initial curves by
 * </p>
 *
 * <p>
 * <i>
 * S^{j}(T_{1},T_{2})
 * =
 * \frac{1+\delta F^{j}(0;T_{1},T_{2})}
 *      {1+\delta F^{d}(0;T_{1},T_{2})}.
 * </i>
 * </p>
 *
 * <p>
 * This keeps the finite-difference state dimension equal to one while remaining
 * consistent with the initial multi-curve market configuration represented by
 * the {@link AnalyticModel}.
 * </p>
 *
 * <p>
 * The class is deliberately close in spirit to the Monte Carlo
 * {@code HullWhiteModelWithShiftExtension}, while exposing the ingredients
 * required by the finite-difference framework.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class FDMHullWhiteModel implements FiniteDifferenceInterestRateModel {

	/**
	 * The zero tolerance.
	 */
	private static final double ZERO_TOLERANCE = 1E-12;
	/**
	 * The forward difference step.
	 */
	private static final double FORWARD_DIFFERENCE_STEP = 1E-4;

	/**
	 * The analytic model.
	 */
	private final AnalyticModel analyticModel;
	/**
	 * The discount curve.
	 */
	private final DiscountCurve discountCurve;
	/**
	 * The volatility model.
	 */
	private final ShortRateVolatilityModel volatilityModel;
	/**
	 * The space time discretization.
	 */
	private final SpaceTimeDiscretization spaceTimeDiscretization;

	/**
	 * Creates a one-factor Hull-White finite-difference model.
	 *
	 * @param analyticModel The initial analytic multi-curve model.
	 * @param discountCurve The discount curve used for discount-bond
	 *     calibration.
	 * @param volatilityModel The short-rate volatility model specifying mean
	 *        reversion and volatility.
	 * @param spaceTimeDiscretization The finite-difference space-time
	 *        discretization.
	 */
	public FDMHullWhiteModel(
			final AnalyticModel analyticModel,
			final DiscountCurve discountCurve,
			final ShortRateVolatilityModel volatilityModel,
			final SpaceTimeDiscretization spaceTimeDiscretization) {

		if (analyticModel == null) {
			throw new IllegalArgumentException("analyticModel must not be null.");
		}
		if (discountCurve == null) {
			throw new IllegalArgumentException("discountCurve must not be null.");
		}
		if (volatilityModel == null) {
			throw new IllegalArgumentException("volatilityModel must not be null.");
		}
		if (spaceTimeDiscretization == null) {
			throw new IllegalArgumentException("spaceTimeDiscretization must not be null.");
		}
		if (spaceTimeDiscretization.getNumberOfSpaceGrids() != 1) {
			throw new IllegalArgumentException("HullWhiteModel requires a one-dimensional space discretization.");
		}

		this.analyticModel = analyticModel;
		this.discountCurve = discountCurve;
		this.volatilityModel = volatilityModel;
		this.spaceTimeDiscretization = spaceTimeDiscretization;
	}

	@Override
	public AnalyticModel getAnalyticModel() {
		return analyticModel;
	}

	/**
	 * Returns the discount curve used for discount-bond calibration.
	 *
	 * @return The discount curve.
	 */
	public DiscountCurve getDiscountCurve() {
		return discountCurve;
	}

	/**
	 * Returns the short-rate volatility model.
	 *
	 * @return The short-rate volatility model.
	 */
	public ShortRateVolatilityModel getVolatilityModel() {
		return volatilityModel;
	}

	@Override
	public SpaceTimeDiscretization getSpaceTimeDiscretization() {
		return spaceTimeDiscretization;
	}

	@Override
	public double[] getInitialValue() {
		return new double[] {0.0 };
	}

	@Override
	public double[] getDrift(final double time, final double... stateVariables) {
		validateStateVariables(stateVariables);

		final double meanReversion = getMeanReversion(time);
		final double stateVariable = stateVariables[0];

		return new double[] {-meanReversion * stateVariable };
	}

	@Override
	public double[][] getFactorLoading(final double time, final double... stateVariables) {
		final double volatility = getVolatility(time);

		return new double[][] {{volatility } };
	}

	@Override
	public double getDiscountBond(final double time, final double maturity, final double... stateVariables) {
		validateStateVariables(stateVariables);

		if (maturity < time - ZERO_TOLERANCE) {
			throw new IllegalArgumentException("Require maturity >= time.");
		}
		if (Math.abs(maturity - time) <= ZERO_TOLERANCE) {
			return 1.0;
		}

		final double stateVariable = stateVariables[0];
		final double deterministicShift = getDeterministicShortRateShift(time);

		final double a = getA(time, maturity);
		final double b = getB(time, maturity);

		return a * Math.exp(-b * (stateVariable + deterministicShift));
	}

	/**
	 * Returns the deterministic shift alpha(t) in the representation
	 *
	 * r(t) = x(t) + alpha(t).
	 *
	 * @param time The time t.
	 * @return The deterministic short-rate shift alpha(t).
	 */
	public double getDeterministicShortRateShift(final double time) {
		return getInitialInstantaneousForwardRate(time) + getDV(0.0, time);
	}

	private double getDV(final double time, final double maturity) {
		if (maturity <= time) {
			return 0.0;
		}

		final TimeDiscretization timeDiscretization = volatilityModel.getTimeDiscretization();

		final int timeIndexStart = getVolatilityIntervalIndex(time);
		final int timeIndexEnd = getVolatilityIntervalIndex(maturity);

		double integral = 0.0;
		double timePrevious = time;

		for (int timeIndex = timeIndexStart + 1; timeIndex <= timeIndexEnd; timeIndex++) {
			final double timeNext = timeDiscretization.getTime(timeIndex);
			final double meanReversion =
					volatilityModel.getMeanReversion(timeIndex - 1).doubleValue();
			final double volatility =
					volatilityModel.getVolatility(timeIndex - 1).doubleValue();

			if (Math.abs(meanReversion) < ZERO_TOLERANCE) {
				integral += volatility * volatility * (timeNext - timePrevious);
			} else {
				integral += volatility * volatility
						* (Math.exp(-getMRTime(timeNext, maturity))
								- Math.exp(-getMRTime(timePrevious, maturity)))
						/ (meanReversion * meanReversion);

				integral -= volatility * volatility
						* (Math.exp(-2.0 * getMRTime(timeNext, maturity))
								- Math.exp(-2.0 * getMRTime(timePrevious, maturity)))
						/ (2.0 * meanReversion * meanReversion);
			}

			timePrevious = timeNext;
		}

		final double timeNext = maturity;
		final double meanReversion =
				volatilityModel.getMeanReversion(timeIndexEnd).doubleValue();
		final double volatility =
				volatilityModel.getVolatility(timeIndexEnd).doubleValue();

		if (Math.abs(meanReversion) < ZERO_TOLERANCE) {
			integral += volatility * volatility * (timeNext - timePrevious);
		} else {
			integral += volatility * volatility
					* (Math.exp(-getMRTime(timeNext, maturity))
							- Math.exp(-getMRTime(timePrevious, maturity)))
					/ (meanReversion * meanReversion);

			integral -= volatility * volatility
					* (Math.exp(-2.0 * getMRTime(timeNext, maturity))
							- Math.exp(-2.0 * getMRTime(timePrevious, maturity)))
					/ (2.0 * meanReversion * meanReversion);
		}

		return integral;
	}

	@Override
	public double getForwardRate(
			final String forwardCurveName,
			final double time,
			final double periodStart,
			final double periodEnd,
			final double... stateVariables) {

		validateStateVariables(stateVariables);

		if (periodEnd <= periodStart) {
			throw new IllegalArgumentException("Require periodEnd > periodStart.");
		}
		if (periodStart < time - ZERO_TOLERANCE) {
			throw new IllegalArgumentException("Require periodStart >= time.");
		}

		final double accrualPeriod = periodEnd - periodStart;

		final double discountBondStart = getDiscountBond(time, periodStart, stateVariables);
		final double discountBondEnd = getDiscountBond(time, periodEnd, stateVariables);

		final double discountCurveForwardRate =
				(discountBondStart / discountBondEnd - 1.0) / accrualPeriod;

		if (forwardCurveName == null) {
			return discountCurveForwardRate;
		}

		final ForwardCurve forwardCurve = analyticModel.getForwardCurve(forwardCurveName);
		if (forwardCurve == null) {
			return discountCurveForwardRate;
		}

		final double initialForwardRate = forwardCurve.getForward(analyticModel, periodStart);
		final double initialDiscountForwardRate = getInitialDiscountForwardRate(periodStart, periodEnd);

		final double multiplicativeSpreadFactor =
				(1.0 + accrualPeriod * initialForwardRate)
				/ (1.0 + accrualPeriod * initialDiscountForwardRate);

		return (multiplicativeSpreadFactor * (1.0 + accrualPeriod * discountCurveForwardRate) - 1.0)
				/ accrualPeriod;
	}

	@Override
	public FDMHullWhiteModel getCloneWithModifiedSpaceTimeDiscretization(
			final SpaceTimeDiscretization newSpaceTimeDiscretization) {
		return new FDMHullWhiteModel(
				analyticModel,
				discountCurve,
				volatilityModel,
				newSpaceTimeDiscretization
				);
	}

	/**
	 * Returns the affine coefficient {@code A(t,T)} in the discount-bond
	 * formula
	 * {@code P(t,T) = A(t,T) exp(-B(t,T) x(t))}.
	 *
	 * @param time The time {@code t}.
	 * @param maturity The maturity {@code T}.
	 * @return The value of {@code A(t,T)}.
	 */
	public double getA(final double time, final double maturity) {
		if (Math.abs(maturity - time) <= ZERO_TOLERANCE) {
			return 1.0;
		}

		final double discountFactorAtTime = discountCurve.getDiscountFactor(analyticModel, time);
		final double discountFactorAtMaturity = discountCurve.getDiscountFactor(analyticModel, maturity);
		final double instantaneousForwardRate = getInitialInstantaneousForwardRate(time);
		final double b = getB(time, maturity);

		final double logarithmOfA =
				Math.log(discountFactorAtMaturity / discountFactorAtTime)
				+ b * instantaneousForwardRate
				- 0.5 * getShortRateConditionalVariance(0.0, time) * b * b;

		return Math.exp(logarithmOfA);
	}

	/**
	 * Returns the affine coefficient
	 *
	 *
	 * <p>
	 * <i>
	 * B(t,T) = \int_{t}^{T} \exp\left(-\int_{u}^{T} a(s)\,ds\right)\,du.
	 * </i>
	 * </p>
	 *
	 * <p>
	 * The implementation supports piecewise constant mean reversion levels, as
	 * provided by the volatility model.
	 * </p>
	 *
	 * @param time The time {@code t}.
	 * @param maturity The maturity {@code T}.
	 * @return The value of {@code B(t,T)}.
	 */
	public double getB(final double time, final double maturity) {
		if (maturity <= time) {
			return 0.0;
		}

		final TimeDiscretization timeDiscretization = volatilityModel.getTimeDiscretization();

		final int timeIndexStart = getVolatilityIntervalIndex(time);
		final int timeIndexEnd = getVolatilityIntervalIndex(maturity);

		double integral = 0.0;
		double timePrev = time;

		for (int timeIndex = timeIndexStart + 1; timeIndex <= timeIndexEnd; timeIndex++) {
			final double timeNext = timeDiscretization.getTime(timeIndex);
			final double meanReversion = volatilityModel.getMeanReversion(timeIndex - 1).doubleValue();

			integral += getBContribution(timePrev, timeNext, maturity, meanReversion);
			timePrev = timeNext;
		}

		final double timeNext = maturity;
		final double meanReversion = volatilityModel.getMeanReversion(timeIndexEnd).doubleValue();
		integral += getBContribution(timePrev, timeNext, maturity, meanReversion);

		return integral;
	}

	/**
	 * Returns the conditional variance
	 *
	 *
	 * <p>
	 * <i>
	 * Var(r(T)\,|\,r(t))
	 * =
	 * \int_{t}^{T}\sigma^{2}(u)
	 * \exp\left(-2\int_{u}^{T} a(s)\,ds\right)\,du.
	 * </i>
	 * </p>
	 *
	 * @param time The conditioning time.
	 * @param maturity The maturity.
	 * @return The conditional short-rate variance.
	 */
	public double getShortRateConditionalVariance(final double time, final double maturity) {
		if (maturity <= time) {
			return 0.0;
		}

		final TimeDiscretization timeDiscretization = volatilityModel.getTimeDiscretization();

		final int timeIndexStart = getVolatilityIntervalIndex(time);
		final int timeIndexEnd = getVolatilityIntervalIndex(maturity);

		double integral = 0.0;
		double timePrev = time;

		for (int timeIndex = timeIndexStart + 1; timeIndex <= timeIndexEnd; timeIndex++) {
			final double timeNext = timeDiscretization.getTime(timeIndex);
			final double meanReversion = volatilityModel.getMeanReversion(timeIndex - 1).doubleValue();
			final double volatility = volatilityModel.getVolatility(timeIndex - 1).doubleValue();

			integral += getShortRateVarianceContribution(
					timePrev,
					timeNext,
					maturity,
					meanReversion,
					volatility
					);

			timePrev = timeNext;
		}

		final double timeNext = maturity;
		final double meanReversion = volatilityModel.getMeanReversion(timeIndexEnd).doubleValue();
		final double volatility = volatilityModel.getVolatility(timeIndexEnd).doubleValue();

		integral += getShortRateVarianceContribution(
				timePrev,
				timeNext,
				maturity,
				meanReversion,
				volatility
				);

		return integral;
	}

	/**
	 * Returns the integrated bond variance
	 * {@code Var(log P(t,T) | F_0)} contribution
	 * {@code B(t,T)^2 Var(r(t) | r(0))}.
	 *
	 * @param time The time {@code t}.
	 * @param maturity The bond maturity {@code T}.
	 * @return The integrated bond squared volatility.
	 */
	public double getIntegratedBondSquaredVolatility(final double time, final double maturity) {
		final double b = getB(time, maturity);
		return getShortRateConditionalVariance(0.0, time) * b * b;
	}

	private void validateStateVariables(final double... stateVariables) {
		if (stateVariables == null || stateVariables.length != 1) {
			throw new IllegalArgumentException("HullWhiteModel requires exactly one state variable.");
		}
	}

	private double getMeanReversion(final double time) {
		final int timeIndex = getVolatilityIntervalIndex(time);
		return volatilityModel.getMeanReversion(timeIndex).doubleValue();
	}

	private double getVolatility(final double time) {
		final int timeIndex = getVolatilityIntervalIndex(time);
		return volatilityModel.getVolatility(timeIndex).doubleValue();
	}

	private int getVolatilityIntervalIndex(final double time) {
		final TimeDiscretization timeDiscretization = volatilityModel.getTimeDiscretization();

		int timeIndex = timeDiscretization.getTimeIndex(time);
		if (timeIndex < 0) {
			timeIndex = -timeIndex - 2;
		}

		/*
		 * Special case:
		 * A volatility model specified on TimeDiscretizationFromArray(0.0)
		 * is interpreted as piecewise constant with one single parameter set
		 * valid for all times, exactly as in the Monte Carlo Hull-White setup.
		 */
		final int maxIndex;
		if (timeDiscretization.getNumberOfTimeSteps() > 0) {
			maxIndex = timeDiscretization.getNumberOfTimeSteps() - 1;
		} else if (timeDiscretization.getNumberOfTimes() > 0) {
			maxIndex = 0;
		} else {
			throw new IllegalArgumentException("Volatility-model time discretization is empty.");
		}

		if (timeIndex < 0) {
			timeIndex = 0;
		}
		if (timeIndex > maxIndex) {
			timeIndex = maxIndex;
		}

		return timeIndex;
	}

	private double getInitialDiscountForwardRate(final double periodStart, final double periodEnd) {
		final double accrualPeriod = periodEnd - periodStart;
		final double discountFactorAtStart = discountCurve.getDiscountFactor(analyticModel, periodStart);
		final double discountFactorAtEnd = discountCurve.getDiscountFactor(analyticModel, periodEnd);

		return (discountFactorAtStart / discountFactorAtEnd - 1.0) / accrualPeriod;
	}

	private double getInitialInstantaneousForwardRate(final double time) {
		final double leftTime = Math.max(time - FORWARD_DIFFERENCE_STEP, 0.0);
		final double rightTime = Math.max(time + FORWARD_DIFFERENCE_STEP, FORWARD_DIFFERENCE_STEP);

		final double discountFactorAtLeft = discountCurve.getDiscountFactor(analyticModel, leftTime);
		final double discountFactorAtRight = discountCurve.getDiscountFactor(analyticModel, rightTime);

		return -(Math.log(discountFactorAtRight) - Math.log(discountFactorAtLeft)) / (rightTime - leftTime);
	}

	/**
	 * Returns the value.
	 *
	 * @param time The value.
	 * @param maturity The value.
	 * @return The value.
	 */
	public double getMRTime(final double time, final double maturity) {
		if (maturity <= time) {
			return 0.0;
		}

		final TimeDiscretization timeDiscretization = volatilityModel.getTimeDiscretization();

		final int timeIndexStart = getVolatilityIntervalIndex(time);
		final int timeIndexEnd = getVolatilityIntervalIndex(maturity);

		double integral = 0.0;
		double timePrev = time;

		for (int timeIndex = timeIndexStart + 1; timeIndex <= timeIndexEnd; timeIndex++) {
			final double timeNext = timeDiscretization.getTime(timeIndex);
			final double meanReversion = volatilityModel.getMeanReversion(timeIndex - 1).doubleValue();

			integral += meanReversion * (timeNext - timePrev);
			timePrev = timeNext;
		}

		final double timeNext = maturity;
		final double meanReversion = volatilityModel.getMeanReversion(timeIndexEnd).doubleValue();
		integral += meanReversion * (timeNext - timePrev);

		return integral;
	}

	private double getBContribution(
			final double timePrevious,
			final double timeNext,
			final double maturity,
			final double meanReversion) {

		if (Math.abs(meanReversion) < ZERO_TOLERANCE) {
			return timeNext - timePrevious;
		}

		return (Math.exp(-getMRTime(timeNext, maturity)) - Math.exp(-getMRTime(timePrevious, maturity)))
				/ meanReversion;
	}

	private double getShortRateVarianceContribution(
			final double timePrevious,
			final double timeNext,
			final double maturity,
			final double meanReversion,
			final double volatility) {

		if (Math.abs(meanReversion) < ZERO_TOLERANCE) {
			return volatility * volatility * (timeNext - timePrevious);
		}

		return volatility * volatility
				* (Math.exp(-2.0 * getMRTime(timeNext, maturity))
						- Math.exp(-2.0 * getMRTime(timePrevious, maturity)))
				/ (2.0 * meanReversion);
	}

	@Override
	public BoundaryCondition[] getBoundaryConditionsAtLowerBoundary(
			final FiniteDifferenceInterestRateProduct product,
			final double time,
			final double... stateVariables) {

		final FiniteDifferenceInterestRateBoundary boundary =
				FDInterestRateBoundaryFactory.createBoundary(this, product);

		return boundary.getBoundaryConditionsAtLowerBoundary(product, time, stateVariables);
	}

	@Override
	public BoundaryCondition[] getBoundaryConditionsAtUpperBoundary(
			final FiniteDifferenceInterestRateProduct product,
			final double time,
			final double... stateVariables) {

		final FiniteDifferenceInterestRateBoundary boundary =
				FDInterestRateBoundaryFactory.createBoundary(this, product);

		return boundary.getBoundaryConditionsAtUpperBoundary(product, time, stateVariables);
	}
}
