/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 14.02.2015
 */

package net.finmath.functions;

import net.finmath.stochastic.RandomVariable;

/**
 * Class providing the test statistic of the Jarque-Bera test.
 *
 * The test statistic is given by
 * \[
 * 	\frac{n}{6} \left( S^{2} + \frac{1}{4} \left( K - 3 \right)^{2} \right)
 * \]
 * where \( S \) is the skewness and \( K \) is the kurtosis of the given random variable.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class JarqueBeraTest {

	/**
	 * Create an instance of the Jarque-Bera test.
	 */
	public JarqueBeraTest() {
	}

	/**
	 * Return the test statistic of the Jarque-Bera test for a given
	 * random variable.
	 *
	 * @param randomVariable An object implementing {@link RandomVariable}
	 * @return The test statistic of the Jarque-Bera test the given random variable.
	 */
	public double test(final RandomVariable randomVariable) {
		final double mean		= randomVariable.getAverage();
		final double stdev	= randomVariable.getStandardDeviation();

		final double skewness = randomVariable.sub(mean).pow(3).getAverage() / Math.pow(stdev, 3);
		final double kurtosis = randomVariable.sub(mean).pow(4).getAverage() / Math.pow(stdev, 4);

		final double test = randomVariable.size() / 6.0 * ( skewness * skewness + 0.25 * (kurtosis-3.0)*(kurtosis-3.0));

		return test;
	}
}
