/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 10.01.2021
 */
package net.finmath.functions;

/**
 * @author Christian Fries
 * @version 1.0
 */
public class LogNormalDistribution {

	// Create normal distribution (for if we use Jakarta Commons Math)
	static final org.apache.commons.math3.distribution.LogNormalDistribution logNormalDistribution  = new org.apache.commons.math3.distribution.LogNormalDistribution();

	public static class LogNormalDistributionParameters {
		private final double mean;
		private final double standardDeviation;
		private final double mu;
		private final double sigma;

		public LogNormalDistributionParameters(double mean, double standardDeviation, double mu, double sigma) {
			super();
			this.mean = mean;
			this.standardDeviation = standardDeviation;
			this.mu = mu;
			this.sigma = sigma;
		}

		public double getMean() {
			return mean;
		}

		public double getStandardDeviation() {
			return standardDeviation;
		}

		public double getMu() {
			return mu;
		}

		public double getSigma() {
			return sigma;
		}
	}

	private LogNormalDistribution() {
	}

	public static LogNormalDistributionParameters getParametersFromMuAndSigma(double mu, double sigma) {
		final double mean = Math.exp(mu) * Math.exp(sigma*sigma/2);
		final double standardDeviation = Math.exp(mu) * Math.sqrt((Math.exp(Math.pow(sigma,2))-1)*Math.exp(Math.pow(sigma,2)));

		return new LogNormalDistributionParameters(mean, standardDeviation, mu, sigma);
	}

	public static LogNormalDistributionParameters getParametersFromMeanAndStdDev(double mean, double standardDeviation) {
		final double mu = Math.log(  mean / Math.sqrt(Math.pow(standardDeviation / mean,2)+1 ) );
		final double sigma = Math.sqrt( 2 * (Math.log(mean)-mu) );

		return new LogNormalDistributionParameters(mean, standardDeviation, mu, sigma);
	}

	/**
	 * Returns the value of the density at x.
	 *
	 * @param x Argument
	 * @return The value of the density at x.
	 */
	public static double density(final double x) {
		return logNormalDistribution.density(x);
	}

	/**
	 * Cumulative distribution function of the standard normal distribution.
	 * The implementation is currently using Jakarta commons-math
	 *
	 * @param x A sample point
	 * @return The probability of being below x, given x is standard normal
	 */
	public static double cumulativeDistribution(final double x) {
		return logNormalDistribution.cumulativeProbability(x);
	}

	/**
	 * Inverse of the cumulative distribution function of the standard normal distribution using Jakarta commons-math
	 *
	 * @param p The probability
	 * @return The quantile
	 */
	public static double inverseCumulativeDistribution(final double p) {
		return logNormalDistribution.inverseCumulativeProbability(p);
	}

}
