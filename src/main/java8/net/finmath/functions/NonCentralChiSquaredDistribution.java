package net.finmath.functions;

import org.apache.commons.lang3.Validate;
import org.apache.commons.math3.special.Gamma;

/**
 * Implementation of the cumulative distribution function of the non-central &Chi;<sup>2</sup> distribution.
 *
 * The implementation is that of Ding's algorithm, splitting the sum since the first term
 * of the sum is not the largest one (and may be zero).
 *
 * The algorithm uses Kahan summation for the sums and limits the calculation time to avoid infinity loops.
 *
 * The method is currently intended to be used as a benchmark in unit tests only.
 *
 * Note: The implementation currently does not use an alternative algorithm
 * in cases of high non-centrality, like Benton and Krishnamoorthy. Since the number
 * of summation is limited, the method may be inaccurate for high non-centrality.
 *
 * @author Christian Fries
 * @author Ralph Rudd
 */
public class NonCentralChiSquaredDistribution {

	private final double nonCentralityHalf;
	private final double degreesOfFreedomHalf;

	private final int summationSplitIndex;
	private final int maxSummation;
	private final double pInitial;

	private static final double PRECISION = 1e-16;

	/**
	 * Create non-central &Chi;<sup>2</sup> distribution (non-central chi-squared distribution).
	 *
	 * @param degreesOfFreedom The number of degrees of freedom, positive
	 * @param nonCentrality The non-centrality parameter, not negative
	 */
	public NonCentralChiSquaredDistribution(final double degreesOfFreedom, double nonCentrality) {
		Validate.isTrue(degreesOfFreedom > 0, "Parameter degreesOfFreedom must be > 0 (given %d).", degreesOfFreedom);
		Validate.isTrue(nonCentrality >= 0, "Parameter nonCentrality must be >= 0 (given %d).", nonCentrality);

		degreesOfFreedomHalf = degreesOfFreedom / 2.0;
		nonCentralityHalf = nonCentrality / 2.0;
		summationSplitIndex = (int) Math.round(nonCentralityHalf);
		maxSummation = Math.max(summationSplitIndex, 10000);

		if (nonCentralityHalf == 0) {
			pInitial = 0.0;
		} else {
			pInitial = Math.exp(-nonCentralityHalf + summationSplitIndex * Math.log(nonCentralityHalf) - Gamma.logGamma(summationSplitIndex + 1));
		}
	}

	/**
	 * Cumulative distribution function of the non-central &Chi;<sup>2</sup> distribution
	 *
	 * @param x A sample point
	 * @return The probability of X being below x, given that X is non-central &Chi;<sup>2</sup> distributed
	 */
	public double cumulativeDistribution(final double x) {
		if (x < 0) {
			return 0.0;
		}

		final double xHalf		= x / 2.0;
		final double xHalfLog	= Math.log(xHalf);

		final double gammaRegularizedInitial = Gamma.regularizedGammaP(degreesOfFreedomHalf + summationSplitIndex, xHalf);

		double p = pInitial;
		double gammaRegularized = gammaRegularizedInitial;

		/*
		 * Sum of terms below summationSplitIndex, backward, using Kahan summation.
		 */
		double sum = p * gammaRegularized;
		double error = 0.0;
		double newSum = 0.0;
		for(int i=summationSplitIndex-1; i >= 0; i--) {

			p *= (i + 1) / nonCentralityHalf;
			gammaRegularized += Math.exp((degreesOfFreedomHalf + i) * xHalfLog - xHalf - Gamma.logGamma(degreesOfFreedomHalf + i + 1));

			// Kahan summation
			final double value = p * gammaRegularized - error;

			newSum = sum + value;
			error = (newSum - sum) - value;

			if(Math.abs(newSum - sum) / newSum <= PRECISION) {
				break;
			}
			sum = newSum;
		}

		p = pInitial;
		gammaRegularized = gammaRegularizedInitial;
		sum = newSum;

		/*
		 * Sum of terms above summationSplitIndex, forward, using Kahan summation.
		 * (to prevent infinity loops the sum has a MAXSUMMATION).
		 */
		for(int i=summationSplitIndex; i<summationSplitIndex+maxSummation; i++) {
			p *= nonCentralityHalf / (i+1);
			gammaRegularized -= Math.exp((degreesOfFreedomHalf + i) * xHalfLog - xHalf - Gamma.logGamma(degreesOfFreedomHalf + i + 1));

			// Kahan summation
			final double value = p * gammaRegularized - error;

			newSum = sum + value;
			error = (newSum - sum) - value;

			if(Math.abs(newSum - sum) / newSum <= PRECISION) {
				break;
			}
			sum = newSum;
		}

		return newSum;
	}

	/**
	 * @return The number of degrees of freedom
	 */
	public double getDegreesOfFreedom() {
		return degreesOfFreedomHalf * 2.0;
	}

	/**
	 * @return The non-centrality parameter
	 */
	public double getNonCentrality() {
		return nonCentralityHalf * 2.0;
	}
}
