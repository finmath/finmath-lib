/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 29.06.2014
 */

package net.finmath.functions;

/**
 * @author Christian Fries
 * @version 1.0
 */
public class GammaDistribution {

	private final org.apache.commons.math3.distribution.GammaDistribution gammaDistribution;

	public GammaDistribution(final double shape, final double scale) {
		super();
		gammaDistribution = new org.apache.commons.math3.distribution.GammaDistribution(shape, scale);
	}

	/**
	 * Return the inverse cumulative distribution function at x.
	 *
	 * @param x Argument
	 * @return Inverse cumulative distribution function at x.
	 */
	public double inverseCumulativeDistribution(final double x) {
		return gammaDistribution.inverseCumulativeProbability(x);
	}
}
