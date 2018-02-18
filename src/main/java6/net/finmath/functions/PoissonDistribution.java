/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 29.06.2014
 */

package net.finmath.functions;

/**
 * @author Christian Fries
 */
public class PoissonDistribution {
	final double lambda;

	public PoissonDistribution(double lambda) {
		super();
		this.lambda = lambda;
	}

	/**
	 * Return the inverse cumulative distribution function at x.
	 * 
	 * @param x Argument
	 * @return Inverse cumulative distribution function at x.
	 */
	public double inverseCumulativeDistribution(double x) {
		double p = Math.exp(-lambda);
		double dp = p;
		int k = 0;
		while(x > p) {
			k++;
			dp *= lambda / k;
			p += dp;
		}
		return k;
	}
}