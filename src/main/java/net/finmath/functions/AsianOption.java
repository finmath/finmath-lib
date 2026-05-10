package net.finmath.functions;

import net.finmath.modelling.products.CallOrPut;

/**
 * Utility class collecting analytical or semi-analytical pricing formulas for
 * Asian options.
 *
 * <p>Included formulas:</p>
 * <ul>
 * <li>Continuous geometric-average price Asian option (Black-Scholes exact
 * formula)</li>
 * <li>Discrete geometric-average price Asian option (Black-Scholes exact
 * formula)</li>
 * <li>Discrete geometric-average strike Asian option (Black-Scholes exact
 * formula)</li>
 * <li>Continuous arithmetic-average price Asian option, Levy approximation</li>
 * <li>Discrete arithmetic-average price Asian option, Turnbull-Wakeman
 * approximation</li>
 * </ul>
 *
 * @author Alessandro Gnoatto
 */
public final class AsianOption {

	private AsianOption() {
	}

	/**
	 * Black-style price using forward, strike, std-dev and discount factor.
	 * @param optionType The value.
	 * @param forward The value.
	 * @param strike The value.
	 * @param stdDev The value.
	 * @param discountFactor The value.
	 * @return The value.
	 */
	public static double blackPrice(
			final CallOrPut optionType,
			final double forward,
			final double strike,
			final double stdDev,
			final double discountFactor) {

		if (forward <= 0.0) {
			throw new IllegalArgumentException("forward must be positive.");
		}
		if (strike < 0.0) {
			throw new IllegalArgumentException("strike must be non-negative.");
		}
		if (stdDev < 0.0) {
			throw new IllegalArgumentException("stdDev must be non-negative.");
		}
		if (discountFactor <= 0.0) {
			throw new IllegalArgumentException("discountFactor must be positive.");
		}

		if (stdDev == 0.0) {
			final double intrinsic = optionType == CallOrPut.CALL
					? Math.max(forward - strike, 0.0)
					: Math.max(strike - forward, 0.0);
			return discountFactor * intrinsic;
		}

		final double d1 = (Math.log(forward / strike) + 0.5 * stdDev * stdDev) / stdDev;
		final double d2 = d1 - stdDev;

		if (optionType == CallOrPut.CALL) {
			return discountFactor * (forward * NormalDistribution.cumulativeDistribution(d1)
					- strike * NormalDistribution.cumulativeDistribution(d2));
		} else {
			return discountFactor * (strike * NormalDistribution.cumulativeDistribution(-d2)
					- forward * NormalDistribution.cumulativeDistribution(-d1));
		}
	}

	/**
	 * Exact Black-Scholes price for a continuous geometric-average Asian
	 * option.
	 *
	 * @param spot spot S0
	 * @param strike strike K
	 * @param riskFreeRate continuously compounded risk-free rate r
	 * @param dividendYield continuously compounded dividend yield q
	 * @param volatility Black-Scholes volatility sigma
	 * @param maturity maturity T
	 * @param optionType The value.
	 * @return The value.
	 */
	public static double priceContinuousGeometricAveragePrice(
			final CallOrPut optionType,
			final double spot,
			final double strike,
			final double riskFreeRate,
			final double dividendYield,
			final double volatility,
			final double maturity) {

		final double variance = volatility * volatility * maturity / 3.0;
		final double adjustedDividendYield = 0.5 * (riskFreeRate + dividendYield + volatility * volatility / 6.0);
		final double riskFreeDiscount = Math.exp(-riskFreeRate * maturity);
		final double dividendDiscount = Math.exp(-adjustedDividendYield * maturity);
		final double forward = spot * dividendDiscount / riskFreeDiscount;

		return blackPrice(optionType, forward, strike, Math.sqrt(variance), riskFreeDiscount);
	}

	/**
	 * Exact Black-Scholes price for a discrete geometric-average price Asian
	 * option.
	 *
	 * <p>This matches the QuantLib analytic discrete geometric average price
	 * engine.</p>
	 *
	 * @param futureFixingTimes strictly non-negative, increasing fixing times
	 *     (in years from valuation time) for remaining fixings
	 * @param pastFixings number of already observed fixings
	 * @param runningProduct product of already observed fixings; use 1.0 if
	 *     pastFixings = 0
	 * @param maturity option maturity / exercise time
	 * @param optionType The value.
	 * @param spot The value.
	 * @param strike The value.
	 * @param riskFreeRate The value.
	 * @param dividendYield The value.
	 * @param volatility The value.
	 * @return The value.
	 */
	public static double priceDiscreteGeometricAveragePrice(
			final CallOrPut optionType,
			final double spot,
			final double strike,
			final double riskFreeRate,
			final double dividendYield,
			final double volatility,
			final double maturity,
			final double[] futureFixingTimes,
			final int pastFixings,
			final double runningProduct) {

		checkNonNegativeAndSorted(futureFixingTimes);

		final double[] times = futureFixingTimes;
		final int remainingFixings = times.length;
		final int numberOfFixings = pastFixings + remainingFixings;
		if (numberOfFixings <= 0) {
			throw new IllegalArgumentException("At least one fixing is required.");
		}

		final double N = numberOfFixings;
		final double pastWeight = pastFixings / N;
		final double futureWeight = 1.0 - pastWeight;

		double timeSum = 0.0;
		for (final double t : times) {
			timeSum += t;
		}

		double temp = 0.0;
		for (int i = pastFixings + 1; i < numberOfFixings; i++) {
			temp += times[i - pastFixings - 1] * (N - i);
		}

		final double variance = volatility * volatility / (N * N) * (timeSum + 2.0 * temp);
		final double nu = riskFreeRate - dividendYield - 0.5 * volatility * volatility;

		final int m = (pastFixings == 0 ? 1 : pastFixings);
		final double runningLog = pastFixings == 0 ? 0.0 : Math.log(runningProduct);
		final double muG = pastWeight * runningLog / m
				+ futureWeight * Math.log(spot)
				+ nu * timeSum / N;

		final double forward = Math.exp(muG + 0.5 * variance);
		final double discount = Math.exp(-riskFreeRate * maturity);

		return blackPrice(optionType, forward, strike, Math.sqrt(Math.max(variance, 0.0)), discount);
	}

	/**
	 * Exact Black-Scholes price for a discrete geometric-average strike Asian
	 * option.
	 *
	 * <p>This mirrors the QuantLib engine limitation: past fixings are not
	 * supported.</p>
	 *
	 * @param fixingTimesFromStart fixing times measured from the first fixing
	 *     date; normally the first entry is 0.0
	 * @param residualTime time from first fixing date to exercise date
	 * @param runningProduct product of past fixings; only 1.0 with pastFixings
	 *     = 0 is supported here
	 * @param optionType The value.
	 * @param spot The value.
	 * @param riskFreeRate The value.
	 * @param dividendYield The value.
	 * @param volatility The value.
	 * @param pastFixings The value.
	 * @return The value.
	 */
	public static double priceDiscreteGeometricAverageStrike(
			final CallOrPut optionType,
			final double spot,
			final double riskFreeRate,
			final double dividendYield,
			final double volatility,
			final double residualTime,
			final double[] fixingTimesFromStart,
			final int pastFixings,
			final double runningProduct) {

		if (pastFixings != 0) {
			throw new IllegalArgumentException("pastFixings currently not supported for discrete geometric average strike.");
		}

		checkNonNegativeAndSorted(fixingTimesFromStart);

		final double[] times = fixingTimesFromStart;
		final int remainingFixings = times.length;
		final int numberOfFixings = pastFixings + remainingFixings;
		if (numberOfFixings <= 0) {
			throw new IllegalArgumentException("At least one fixing is required.");
		}

		final double N = numberOfFixings;
		final double pastWeight = pastFixings / N;
		final double futureWeight = 1.0 - pastWeight;

		double timeSum = 0.0;
		for (final double t : times) {
			timeSum += t;
		}

		final double nu = riskFreeRate - dividendYield - 0.5 * volatility * volatility;

		double temp = 0.0;
		for (int i = pastFixings + 1; i < numberOfFixings; i++) {
			temp += times[i - pastFixings - 1] * (N - i);
		}

		final double variance = volatility * volatility / (N * N) * (timeSum + 2.0 * temp);
		final double covarianceTerm = volatility * volatility / N * timeSum;
		final double sigmaSum2 = variance + volatility * volatility * residualTime - 2.0 * covarianceTerm;
		final double safeSigmaSum2 = Math.max(sigmaSum2, 0.0);

		final int m = (pastFixings == 0 ? 1 : pastFixings);
		final double runningLogAverage = Math.log(runningProduct) / m;
		final double muG = pastWeight * runningLogAverage
				+ futureWeight * Math.log(spot)
				+ nu * timeSum / N;

		if (safeSigmaSum2 == 0.0) {
			final double discountedSpot = spot * Math.exp(-dividendYield * residualTime);
			final double discountedGeomAverage = Math.exp(muG + 0.5 * variance - riskFreeRate * residualTime);
			return optionType == CallOrPut.CALL
					? Math.max(discountedSpot - discountedGeomAverage, 0.0)
					: Math.max(discountedGeomAverage - discountedSpot, 0.0);
		}

		final double sqrtSigmaSum = Math.sqrt(safeSigmaSum2);
		final double y1 = (Math.log(spot)
				+ (riskFreeRate - dividendYield) * residualTime
				- muG - 0.5 * variance + 0.5 * safeSigmaSum2) / sqrtSigmaSum;
		final double y2 = y1 - sqrtSigmaSum;

		final double term1 = spot * Math.exp(-dividendYield * residualTime);
		final double term2 = Math.exp(muG + 0.5 * variance - riskFreeRate * residualTime);

		if (optionType == CallOrPut.CALL) {
			return term1 * NormalDistribution.cumulativeDistribution(y1)
					- term2 * NormalDistribution.cumulativeDistribution(y2);
		} else {
			return -term1 * NormalDistribution.cumulativeDistribution(-y1)
					+ term2 * NormalDistribution.cumulativeDistribution(-y2);
		}
	}

	/**
	 * Levy approximation for a continuous arithmetic-average price Asian
	 * option.
	 *
	 * @param averagingStartTime start of averaging window, measured from
	 *     valuation time; must satisfy 0 <= averagingStartTime <= maturity
	 * @param maturity maturity / exercise time T2 from valuation
	 * @param currentAverage already accrued arithmetic average if averaging has
	 *     started; ignored if averagingStartTime == 0
	 * @param optionType The value.
	 * @param spot The value.
	 * @param strike The value.
	 * @param riskFreeRate The value.
	 * @param dividendYield The value.
	 * @param volatility The value.
	 * @return The value.
	 */
	public static double priceContinuousArithmeticAveragePriceLevy(
			final CallOrPut optionType,
			final double spot,
			final double strike,
			final double riskFreeRate,
			final double dividendYield,
			final double volatility,
			final double averagingStartTime,
			final double maturity,
			final double currentAverage) {

		if (averagingStartTime > maturity) {
			throw new IllegalArgumentException("averagingStartTime must be <= maturity.");
		}

		final double T2 = maturity;
		final double T = maturity - averagingStartTime;
		if (T <= 0.0) {
			throw new IllegalArgumentException("original averaging length T must be positive.");
		}

		final double b = riskFreeRate - dividendYield;
		final double discount = Math.exp(-riskFreeRate * T2);

		final double Se;
		if (Math.abs(b) > 1.0e-12) {
			Se = (spot / (T * b)) * (Math.exp((b - riskFreeRate) * T2) - Math.exp(-riskFreeRate * T2));
		} else {
			Se = spot * T2 / T * Math.exp(-riskFreeRate * T2);
		}

		final double X;
		if (averagingStartTime > 0.0) {
			X = strike - (averagingStartTime / T) * currentAverage;
		} else {
			X = strike;
		}

		if (X <= 0.0) {
			final double intrinsic = optionType == CallOrPut.CALL
					? Math.max(Se - X * discount, 0.0)
					: Math.max(X * discount - Se, 0.0);
			return intrinsic;
		}

		final double m;
		if (Math.abs(b) > 1.0e-12) {
			m = (Math.exp(b * T2) - 1.0) / b;
		} else {
			m = T2;
		}

		final double denom = b + volatility * volatility;
		if (Math.abs(denom) < 1.0e-14) {
			throw new IllegalArgumentException("b + volatility^2 is too close to zero for the Levy formula.");
		}

		final double expTermDenom = 2.0 * b + volatility * volatility;
		final double expTerm;
		if (Math.abs(expTermDenom) < 1.0e-14) {
			expTerm = T2;
		} else {
			expTerm = (Math.exp((2.0 * b + volatility * volatility) * T2) - 1.0) / expTermDenom;
		}

		final double M = (2.0 * spot * spot / denom) * (expTerm - m);
		final double D = M / (T * T);
		final double V = Math.log(D) - 2.0 * (riskFreeRate * T2 + Math.log(Se));
		final double safeV = Math.max(V, 0.0);

		if (safeV == 0.0) {
			final double intrinsic = optionType == CallOrPut.CALL
					? Math.max(Se - X * discount, 0.0)
					: Math.max(X * discount - Se, 0.0);
			return intrinsic;
		}

		final double sqrtV = Math.sqrt(safeV);
		final double d1 = (0.5 * Math.log(D) - Math.log(X)) / sqrtV;
		final double d2 = d1 - sqrtV;

		if (optionType == CallOrPut.CALL) {
			return Se * NormalDistribution.cumulativeDistribution(d1)
					- X * discount * NormalDistribution.cumulativeDistribution(d2);
		} else {
			return Se * NormalDistribution.cumulativeDistribution(d1)
					- X * discount * NormalDistribution.cumulativeDistribution(d2)
					- Se + X * discount;
		}
	}

	/**
	 * Turnbull-Wakeman approximation for a discrete arithmetic-average price
	 * Asian option.
	 *
	 * @param futureFixingTimes remaining fixing times in years from valuation
	 *     time
	 * @param pastFixings number of already observed fixings
	 * @param runningSum sum of already observed fixings; use 0.0 if pastFixings
	 *     = 0
	 * @param exerciseTime option maturity / exercise time, typically >= max
	 *     futureFixingTimes
	 * @param optionType The value.
	 * @param spot The value.
	 * @param strike The value.
	 * @param riskFreeRate The value.
	 * @param dividendYield The value.
	 * @param volatility The value.
	 * @return The value.
	 */
	public static double priceDiscreteArithmeticAveragePriceTurnbullWakeman(
			final CallOrPut optionType,
			final double spot,
			final double strike,
			final double riskFreeRate,
			final double dividendYield,
			final double volatility,
			final double exerciseTime,
			final double[] futureFixingTimes,
			final int pastFixings,
			final double runningSum) {

		checkNonNegativeAndSorted(futureFixingTimes);

		final double[] times = futureFixingTimes;
		final int futureFixings = times.length;
		final int m = futureFixings + pastFixings;
		if (m <= 0) {
			throw new IllegalArgumentException("At least one fixing is required.");
		}

		final double accruedAverage = pastFixings == 0 ? 0.0 : runningSum / m;
		final double effectiveStrike = strike - accruedAverage;
		final double discount = Math.exp(-riskFreeRate * exerciseTime);

		if (effectiveStrike <= 0.0) {
			if (optionType == CallOrPut.CALL) {
				double expectedAverage = accruedAverage;
				for (final double t : times) {
					expectedAverage += (spot * Math.exp((riskFreeRate - dividendYield) * t)) / m;
				}
				return discount * (expectedAverage - strike);
			}
			return 0.0;
		}

		final int n = times.length;
		if (n == 0) {
			final double realizedAverage = pastFixings == 0 ? 0.0 : runningSum / pastFixings;
			final double payoff = optionType == CallOrPut.CALL
					? Math.max(realizedAverage - strike, 0.0)
					: Math.max(strike - realizedAverage, 0.0);
			return discount * payoff;
		}

		final double[] forwards = new double[n];
		final double[] spotVars = new double[n];
		double EA = 0.0;

		for (int i = 0; i < n; i++) {
			final double t = times[i];
			forwards[i] = spot * Math.exp((riskFreeRate - dividendYield) * t);
			spotVars[i] = volatility * volatility * t;
			EA += forwards[i];
		}
		EA /= m;

		double EA2 = 0.0;
		for (int i = 0; i < n; i++) {
			EA2 += forwards[i] * forwards[i] * Math.exp(spotVars[i]);
			for (int j = 0; j < i; j++) {
				EA2 += 2.0 * forwards[i] * forwards[j] * Math.exp(spotVars[j]);
			}
		}
		EA2 /= (double) m * m;

		final double tn = times[n - 1];
		final double sigmaA = Math.sqrt(Math.max(Math.log(EA2 / (EA * EA)) / tn, 0.0));
		return blackPrice(optionType, EA, effectiveStrike, sigmaA * Math.sqrt(tn), discount);
	}

	private static void checkNonNegativeAndSorted(final double[] times) {
		for (int i = 0; i < times.length; i++) {
			if (times[i] < 0.0) {
				throw new IllegalArgumentException("Fixing times must be non-negative.");
			}
			if (i > 0 && times[i] < times[i - 1]) {
				throw new IllegalArgumentException("Fixing times must be sorted increasingly.");
			}
		}
	}
}
