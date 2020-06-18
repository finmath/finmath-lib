/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 03.02.2004
 */
package net.finmath.functions;

/**
 * @author Christian Fries
 * @version 1.0
 */
public class NormalDistribution {

	// Create normal distribution (for if we use Jakarta Commons Math)
	static final org.apache.commons.math3.distribution.NormalDistribution normalDistribution  = new org.apache.commons.math3.distribution.NormalDistribution();

	private NormalDistribution() {
	}

	/**
	 * Returns the value of the density at x.
	 *
	 * @param x Argument
	 * @return The value of the density at x.
	 */
	public static double density(final double x) {
		return normalDistribution.density(x); // FastMath.exp(-x*x/2.0) / FastMath.sqrt(FastMath.PI*2.0);
	}

	/**
	 * Cumulative distribution function of the standard normal distribution.
	 * The implementation is currently using Jakarta commons-math
	 *
	 * @param x A sample point
	 * @return The probability of being below x, given x is standard normal
	 */
	public static double cumulativeDistribution(final double x) {
		return normalDistribution.cumulativeProbability(x);
	}

	/**
	 * Inverse of the cumulative distribution function of the standard normal distribution using Jakarta commons-math
	 *
	 * @param p The probability
	 * @return The quantile
	 */
	public static double inverseCumulativeDistribution(final double p) {
		return inverseCumulativeNormalDistributionWichura(p);
		//        return normalDistribution.inverseCumulativeProbability(p);
	}

	/**
	 * Inverse of the cumulative distribution function of the standard normal distribution
	 *
	 * Java Version of
	 *
	 * Michael J. Wichura: Algorithm AS241 Appl. Statist. (1988) Vol. 37, No. 3 Produces the normal
	 * deviate z corresponding to a given lower tail area of p; z is accurate
	 * to about 1 part in 10**16.
	 *
	 * The hash sums below are the sums of the mantissas of the coefficients.
	 * they are included for use in checking transcription.
	 *
	 * @param p The probability (quantile).
	 * @return The argument of the cumulative distribution function being assigned to p.
	 */
	public static double inverseCumulativeNormalDistributionWichura(final double p) {
		final double zero = 0.e+00, one = 1.e+00, half = 0.5e+00;
		final double split1 = 0.425e+00, split2 = 5.e+00;
		final double const1 = 0.180625e+00, const2 = 1.6e+00;

		//  coefficients for p close to 0.5
		final double a0 = 3.3871328727963666080e+00;
		final double a1 = 1.3314166789178437745e+02;
		final double a2 = 1.9715909503065514427e+03;
		final double a3 = 1.3731693765509461125e+04;
		final double a4 = 4.5921953931549871457e+04;
		final double a5 = 6.7265770927008700853e+04;
		final double a6 = 3.3430575583588128105e+04;
		final double a7 = 2.5090809287301226727e+03;
		final double b1 = 4.2313330701600911252e+01;
		final double b2 = 6.8718700749205790830e+02;
		final double b3 = 5.3941960214247511077e+03;
		final double b4 = 2.1213794301586595867e+04;
		final double b5 = 3.9307895800092710610e+04;
		final double b6 = 2.8729085735721942674e+04;
		final double b7 = 5.2264952788528545610e+03;
		//  hash sum ab 55.8831928806149014439

		//  coefficients for p not close to 0, 0.5 or 1.
		final double c0 = 1.42343711074968357734e+00;
		final double c1 = 4.63033784615654529590e+00;
		final double c2 = 5.76949722146069140550e+00;
		final double c3 = 3.64784832476320460504e+00;
		final double c4 = 1.27045825245236838258e+00;
		final double c5 = 2.41780725177450611770e-01;
		final double c6 = 2.27238449892691845833e-02;
		final double c7 = 7.74545014278341407640e-04;
		final double d1 = 2.05319162663775882187e+00;
		final double d2 = 1.67638483018380384940e+00;
		final double d3 = 6.89767334985100004550e-01;
		final double d4 = 1.48103976427480074590e-01;
		final double d5 = 1.51986665636164571966e-02;
		final double d6 = 5.47593808499534494600e-04;
		final double d7 = 1.05075007164441684324e-09;
		//  hash sum cd 49.33206503301610289036

		//  coefficients for p near 0 or 1.
		final double e0 = 6.65790464350110377720e+00;
		final double e1 = 5.46378491116411436990e+00;
		final double e2 = 1.78482653991729133580e+00;
		final double e3 = 2.96560571828504891230e-01;
		final double e4 = 2.65321895265761230930e-02;
		final double e5 = 1.24266094738807843860e-03;
		final double e6 = 2.71155556874348757815e-05;
		final double e7 = 2.01033439929228813265e-07;
		final double f1 = 5.99832206555887937690e-01;
		final double f2 = 1.36929880922735805310e-01;
		final double f3 = 1.48753612908506148525e-02;
		final double f4 = 7.86869131145613259100e-04;
		final double f5 = 1.84631831751005468180e-05;
		final double f6 = 1.42151175831644588870e-07;
		final double f7 = 2.04426310338993978564e-15;
		//  hash sum ef 47.52583 31754 92896 71629

		final double q = p - half;
		double r, ppnd16;

		if (Math.abs(q) <= split1) {
			r = const1 - q * q;
			return q
					* (((((((a7 * r + a6) * r + a5) * r + a4) * r + a3) * r + a2) * r + a1) * r + a0)
					/ (((((((b7 * r + b6) * r + b5) * r + b4) * r + b3) * r + b2) * r + b1) * r + one);
		} else {
			if (q < zero) {
				r = p;
			} else {
				r = one - p;
			}

			if (r <= zero) {
				return zero;
			}
			r = Math.sqrt(-Math.log(r));
			if (r <= split2) {
				r -= const2;
				ppnd16 =
						(((((((c7 * r + c6) * r + c5) * r + c4) * r + c3) * r + c2) * r + c1) * r + c0)
						/ (((((((d7 * r + d6) * r + d5) * r + d4) * r + d3) * r + d2) * r + d1) * r + one);
			} else {
				r -= split2;
				ppnd16 =
						(((((((e7 * r + e6) * r + e5) * r + e4) * r + e3) * r + e2) * r + e1) * r + e0)
						/ (((((((f7 * r + f6) * r + f5) * r + f4) * r + f3) * r + f2) * r + f1) * r + one);
			}
			if (q < zero) {
				ppnd16 = -ppnd16;
			}

			return ppnd16;
		}
	}
}
