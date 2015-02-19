/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 14.02.2015
 */

package net.finmath.functions;

import net.finmath.stochastic.RandomVariableInterface;

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
 */
public class JarqueBeraTest {

	/**
	 * 
	 */
	public JarqueBeraTest() {
	}

	/**
	 * Return the test statistic of the Jarque-Bera test for a given
	 * random variable.
	 * 
	 * @param randomVariable An object implementing {@link RandomVariableInterface}
	 * @return The test statistic of the Jarque-Bera test the given random variable.
	 */
	public double test(RandomVariableInterface randomVariable) {
		double mean = randomVariable.getAverage();
		double stdev = randomVariable.getStandardDeviation();
		
		double skewness = Math.pow(mean / stdev, 3);
		double kurtosis = Math.pow(mean / stdev, 4);
		
		double test = randomVariable.size() / 6.0 * ( skewness * skewness - 0.25 * (kurtosis-3.0)*(kurtosis-3.0));
		
		return test;
	}
}
