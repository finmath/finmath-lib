/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 21.10.2007
 */
package net.finmath.stochastic;

/**
 * This interface describes the methods that leave a random variable unchanged (immutable).
 * This is used to ensure that arguments or return values are not changed.
 * Doing so we avoid unnecessary copy operations (defensive copy), e.g. when returning references to simulated random variables.
 * For C++ guys: In C++ you could achieve this by making a return value const. On this object you may only operate using
 * the const methods. However, using an interface is more flexible than a C++ const.
 *
 * @author Christian Fries
 * @version 1.2
 */
public interface ImmutableRandomVariableInterface {

	RandomVariableInterface getMutableCopy();

	/**
	 * Compare this random variable with a given one.
	 * 
	 * @param randomVariable Random variable to compare with.
	 * @return True if this random variable and the given one are equal, otherwise false
	 */
	public boolean equals(ImmutableRandomVariableInterface randomVariable);

	/**
	 * Returns the filtration time.
	 * 
	 * @return The filtration time.
	 */
	public double getFiltrationTime();

	/**
	 * Evaluate at a given path or state.
	 * 
	 * @param pathOrState Index of the path or state.
	 * @return Value of this random variable at the given path or state.
	 */
	public double get(int pathOrState);

	/**
	 * Returns the number of paths or states.
	 * 
	 * @return Number of paths or states.
	 */
	public int size();

	/**
	 * Check if this random variable is deterministic in the sense that it is represented by a single double value.
	 * Note that the methods returns false, if the random variable is represented by a vector where each element has the same value.
	 * 
	 * @return True if this random variable is deterministic.
	 */
	public boolean isDeterministic();

	/**
	 * Returns a vector representing the realization of this random variable.
	 * This method is merely useful for analysis. Its interpretation depends on the context (Monte-Carlo or lattice).
	 * The method does not expose an internal data model.
	 * 
	 * @return Vector of realizations of this random variable.
	 */
	public double[] getRealizations();

	/**
	 * Returns the realizations as double array. If the random variable is deterministic, then it is expanded
	 * to the given number of paths.
	 * 
	 * @param numberOfPaths
	 * @return The realization as double array.
	 */
	public double[] getRealizations(int numberOfPaths);
	
	/**
	 * Returns the minimum value attained by this random variable.
	 * 
	 * @return The minimum value.
	 */
	public double getMin();

	/**
	 * Returns the maximum value attained by this random variable.
	 * 
	 * @return The maximum value.
	 */
	public double getMax();

	/**
	 * Returns the expectation of this random variable.
	 * 
	 * @return The average assuming equi-distribution.
	 */
	public double getAverage();

	/**
	 * Returns the expectation of this random variable for a given probability measure.
	 * 
	 * @param probabilities The probability weights.
	 * @return The average assuming the given probability weights.
	 */
	public double getAverage(ImmutableRandomVariableInterface probabilities);

	/**
	 * Returns the variance of this random variable, i.e.,
	 * V where V = ((X-m)^2).getAverage() and X = this and m = X.getAverage().
	 * 
	 * @return The average assuming equi-distribution.
	 */
	public double getVariance();

	/**
	 * Returns the variance of this random variable, i.e.,
	 * V where V = ((X-m)^2).getAverage(probabilities) and X = this and m = X.getAverage(probabilities).
	 * 
	 * @param probabilities The probability weights.
	 * @return The average assuming the given probability weights.
	 */
	public double getVariance(ImmutableRandomVariableInterface probabilities);

	/**
	 * Returns the standard deviation of this random variable, i.e.,
	 * sqrt(V) where V = ((X-m)^2).getAverage() and X = this and m = X.getAverage().
	 * 
	 * @return The standard deviation assuming equi-distribution.
	 */
	public double getStandardDeviation();

	/**
	 * Returns the standard deviation of this random variable, i.e.,
	 * sqrt(V) where V = ((X-m)^2).getAverage(probabilities) and X = this and m = X.getAverage(probabilities).
	 * 
	 * @param probabilities The probability weights.
	 * @return The standard error assuming the given probability weights.
	 */
	public double getStandardDeviation(ImmutableRandomVariableInterface probabilities);

	/**
	 * Returns the standard error (discretization error) of this random variable.
	 * For a Monte-Carlo simulation this is 1/Math.sqrt(n) * {@link #getStandardDeviation() }.
	 * 
	 * @return The standard error assuming equi-distribution.
	 */
	public double getStandardError();

	/**
	 * Returns the standard error (discretization error) of this random variable.
	 * For a Monte-Carlo simulation this is 1/Math.sqrt(n) * {@link #getStandardDeviation(ImmutableRandomVariableInterface) }.
	 * 
	 * @param probabilities The probability weights.
	 * @return The standard error assuming the given probability weights.
	 */
	public double getStandardError(ImmutableRandomVariableInterface probabilities);

	/**
	 * Returns the quantile value for this given random variable, i.e., the value x such that P(this < x) = quantile,
	 * where P denotes the probability measure.
	 * The method will consider picewise constant values (with constant extrapolation) in the random variable.
	 * That is getQuantile(0) and getQuantile(1) will return the largest and smallest value.
	 * 
	 * @param quantile The quantile level.
	 * @return The quantile value assuming equi-distribution.
	 */
	public double getQuantile(double quantile);

	/**
	 * Returns the quantile value for this given random variable, i.e., the value x such that P(this < x) = quantile,
	 * where P denotes the probability measure.
	 * 
	 * @param quantile The quantile level.
	 * @param probabilities The probability weights.
	 * @return The quantile value assuming the given probability weights.
	 */
	public double getQuantile(double quantile, ImmutableRandomVariableInterface probabilities);
			
	/**
	 * Returns the expectation over a quantile for this given random variable.
	 * The method will consider picewise constant values (with constant extrapolation) in the random variable.
	 * For a &leq; b the method returns (&Sigma;<sub>a &leq; i &leq; b</sub> x[i]) / (b-a+1), where
	 * <ul>
	 * <li>a = min(max((n+1) * quantileStart - 1, 0, 1);</li>
	 * <li>b = min(max((n+1) * quantileEnd - 1, 0, 1);</li>
	 * <li>n = this.size();</li>
	 * </ul>
	 * For quantileStart > quantileEnd the method returns getQuantileExpectation(quantileEnd, quantileStart).
	 * 
	 * @param quantileStart Lower bound of the integral.
	 * @param quantileEnd  Upper bound of the integral.
	 * @return The (conditional) expectation of the values between two quantile levels assuming equi-distribution.
	 */
	public double getQuantileExpectation(double quantileStart, double quantileEnd);
	
	/**
	 * Generates a Histogram based on the realizations stored in this random variable.
	 * The returned <code>result</code> array's length is <code>intervalPoints.length+1</code>.
	 * <ul>
	 * 	<li>The value result[0] equals the relative frequency of values observed in the interval ( -infinity, intervalPoints[0] ].</li>
	 * 	<li>The value result[i] equals the relative frequency of values observed in the interval ( intervalPoints[i-1], intervalPoints[i] ].</li>
	 * 	<li>The value result[n] equals the relative frequency of values observed in the interval ( intervalPoints[n-1], infinity ).</li>
	 * </ul>
	 * where n = intervalPoints.length. Note that the intervals are open on the left, closed on the right, i.e.,
	 * result[i] contains the number of elements x with intervalPoints[i-1] < x &leq; intervalPoints[i].
	 * 
	 * Thus, is you have a random variable which only takes values contained in the (sorted) array
	 * <code>possibleValues</code>, then <code>result = getHistogram(possibleValues)</code> returns an
	 * array where <code>result[i]</code> is the relative frequency of occurrence of <code>possibleValues[i]</code>.
	 * 
	 * The sum of result[i] over all i is equal to 1, except for uninitialized random
	 * variables where all values are 0.
	 * 
	 * @param intervalPoints Array of ascending values defining the interval boundaries.
	 * @return A histogram with respect to a provided interval.
	 */
	public double[] getHistogram(double[] intervalPoints);
	
	/**
	 * Generates a histogram based on the realizations stored in this random variable
	 * using interval points calculated from the arguments, see also {@link #getHistogram(double[])}.
	 * The interval points are
	 * set with equal distance over an the interval of the specified standard deviation.
	 * 
	 * The interval points used are
	 * <center>
	 * <code>x[i] = mean + alpha[i] * standardDeviations * sigma</code>
	 * </center>
	 * where
	 * <ul>
	 * <li>i = 0,..., numberOfPoints-1,</li>
	 * <li>alpha[i] = (i - (numberOfPoints-1)/2.0) / ((numberOfPoints-1)/2.0),</li>
	 * <li>mean = {@link #getAverage()},</li>
	 * <li>sigma = {@link #getStandardDeviation()}.</li>
	 * </ul>
	 * 
	 * The methods <code>result</code> is an array of two vectors, where result[0] are the
	 * intervals center points ('anchor points') and result[1] contains the relative frequency for the interval.
	 * The 'anchor point' for the interval (-infinity, x[0]) is x[0] - 1/2 (x[1]-x[0])
	 * and the 'anchor point' for the interval (x[n], infinity) is x[n] + 1/2 (x[n]-x[n-1]).
	 * Here n = numberOfPoints is the number of interval points.
	 * 
	 * @param numberOfPoints The number of interval points.
	 * @param standardDeviations The number of standard deviations defining the discretization radius.
	 * @return A histogram, given as double[2][], where result[0] are the center point of the intervals and result[1] is the value of {@link #getHistogram(double[])} for the given the interval points. The length of result[0] and result[1] is numberOfPoints+1.
	 */
	public double[][] getHistogram(int numberOfPoints, double standardDeviations);
}
