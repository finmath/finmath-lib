/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 21.10.2007
 * Created on 02.02.2014
 */
package net.finmath.stochastic;

import java.io.Serializable;
import java.util.List;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntToDoubleFunction;
import java.util.stream.DoubleStream;

import net.finmath.functions.DoubleTernaryOperator;

/**
 * This interface describes the methods implemented by an immutable random variable.
 *
 * The random variable is immutable, i.e. method calls like add, sub, mult will return
 * a new instance and leave the method receiver random variable unchanged (immutable).
 * This is used to ensure that arguments or return values are not changed.
 *
 * @author Christian Fries
 * @version 1.6
 */
public interface RandomVariable extends Serializable {

	/**
	 * Compare this random variable with a given one
	 *
	 * @param randomVariable Random variable to compare with.
	 * @return True if this random variable and the given one are equal, otherwise false
	 */
	boolean equals(RandomVariable randomVariable);

	/**
	 * Returns the filtration time.
	 *
	 * @return The filtration time.
	 */
	double getFiltrationTime();

	/**
	 * Returns the type priority.
	 *
	 * @return The type priority.
	 * @see <a href="http://ssrn.com/abstract=3246127">ssrn abstract 3246127</a>
	 */
	int getTypePriority();

	/**
	 * Evaluate at a given path or state.
	 *
	 * @param pathOrState Index of the path or state.
	 * @return Value of this random variable at the given path or state.
	 */
	double get(int pathOrState);

	/**
	 * Returns the number of paths or states.
	 *
	 * @return Number of paths or states.
	 */
	int size();

	/**
	 * Check if this random variable is deterministic in the sense that it is represented by a single double value.
	 * Note that the methods returns false, if the random variable is represented by a vector where each element has the same value.
	 *
	 * @return True if this random variable is deterministic.
	 */
	boolean isDeterministic();

	/**
	 * Returns the underlying values and a random variable.
	 *
	 * If the implementation supports an "inner representation", returns the inner representation. Otherwise just returns this.
	 *
	 * @return The underling values.
	 */
	default RandomVariable getValues() { return this; }

	/**
	 * Returns a vector representing the realization of this random variable.
	 * This method is merely useful for analysis. Its interpretation depends on the context (Monte-Carlo or lattice).
	 * The method does not expose an internal data model.
	 *
	 * @return Vector of realizations of this random variable.
	 */
	double[] getRealizations();

	/**
	 * Returns the double value if isDeterministic() is true. otherwise throws an {@link UnsupportedOperationException}.
	 *
	 * @return The double value if isDeterministic() is true, otherwise throws an  an {@link UnsupportedOperationException}.
	 */
	Double doubleValue();

	/**
	 * Returns the operator path &rarr; this.get(path) corresponding to this random variable.
	 *
	 * @return The operator path &rarr; this.get(path) corresponding to this random variable.
	 */
	IntToDoubleFunction getOperator();

	/**
	 * Returns a stream of doubles corresponding to the realizations of this random variable.
	 *
	 * @return A stream of doubles corresponding to the realizations of this random variable.
	 */
	DoubleStream getRealizationsStream();

	/**
	 * Returns the minimum value attained by this random variable.
	 *
	 * @return The minimum value.
	 */
	double getMin();

	/**
	 * Returns the maximum value attained by this random variable.
	 *
	 * @return The maximum value.
	 */
	double getMax();

	/**
	 * Returns the expectation of this random variable.
	 * The result of this method has to agrees with <code>average().doubleValue()</code>.
	 *
	 * @return The average assuming equi-distribution.
	 */
	double getAverage();

	/**
	 * Returns the expectation of this random variable for a given probability measure (weight).
	 *
	 * The result of this method is (mathematically) equivalent to
	 * <br>
	 * <code>this.mult(probabilities).getAverage() / probabilities.getAverage()</code>
	 * <br>
	 * while the internal implementation may differ, e.g. being more efficient by performing multiplication and summation in the same loop.
	 *
	 * @param probabilities The probability weights.
	 * @return The average assuming the given probability weights.
	 */
	double getAverage(RandomVariable probabilities);

	/**
	 * Returns the variance of this random variable, i.e.,
	 * V where V = ((X-m)^2).getAverage() and X = this and m = X.getAverage().
	 *
	 * @return The average assuming equi-distribution.
	 */
	double getVariance();

	/**
	 * Returns the variance of this random variable, i.e.,
	 * V where V = ((X-m)^2).getAverage(probabilities) and X = this and m = X.getAverage(probabilities).
	 *
	 * @param probabilities The probability weights.
	 * @return The average assuming the given probability weights.
	 */
	double getVariance(RandomVariable probabilities);

	/**
	 * Returns the sample variance of this random variable, i.e.,
	 * V * size()/(size()-1) where V = getVariance().
	 *
	 * @return The sample variance.
	 */
	double getSampleVariance();

	/**
	 * Returns the standard deviation of this random variable, i.e.,
	 * sqrt(V) where V = ((X-m)^2).getAverage() and X = this and m = X.getAverage().
	 *
	 * @return The standard deviation assuming equi-distribution.
	 */
	double getStandardDeviation();

	/**
	 * Returns the standard deviation of this random variable, i.e.,
	 * sqrt(V) where V = ((X-m)^2).getAverage(probabilities) and X = this and m = X.getAverage(probabilities).
	 *
	 * @param probabilities The probability weights.
	 * @return The standard error assuming the given probability weights.
	 */
	double getStandardDeviation(RandomVariable probabilities);

	/**
	 * Returns the standard error (discretization error) of this random variable.
	 * For a Monte-Carlo simulation this is 1/Math.sqrt(n) * {@link #getStandardDeviation() }.
	 *
	 * @return The standard error assuming equi-distribution.
	 */
	double getStandardError();

	/**
	 * Returns the standard error (discretization error) of this random variable.
	 * For a Monte-Carlo simulation this is 1/Math.sqrt(n) * {@link #getStandardDeviation(RandomVariable) }.
	 *
	 * @param probabilities The probability weights.
	 * @return The standard error assuming the given probability weights.
	 */
	double getStandardError(RandomVariable probabilities);

	/**
	 * Returns the quantile value for this given random variable, i.e., the value x such that P(this &lt; x) = quantile,
	 * where P denotes the probability measure.
	 * The method will consider picewise constant values (with constant extrapolation) in the random variable.
	 * That is getQuantile(0) wiil return the smallest value and getQuantile(1) will return the largest value.
	 *
	 * @param quantile The quantile level.
	 * @return The quantile value assuming equi-distribution.
	 */
	double getQuantile(double quantile);

	/**
	 * Returns the quantile value for this given random variable, i.e., the value x such that P(this &lt; x) = quantile,
	 * where P denotes the probability measure.
	 *
	 * @param quantile The quantile level.
	 * @param probabilities The probability weights.
	 * @return The quantile value assuming the given probability weights.
	 */
	double getQuantile(double quantile, RandomVariable probabilities);

	/**
	 * Returns the expectation over a quantile for this given random variable.
	 * The method will consider picewise constant values (with constant extrapolation) in the random variable.
	 * For a &le; b the method returns (&Sigma;<sub>a &le; i &le; b</sub> x[i]) / (b-a+1), where
	 * <ul>
	 * <li>a = min(max((n+1) * quantileStart - 1, 0, 1);</li>
	 * <li>b = min(max((n+1) * quantileEnd - 1, 0, 1);</li>
	 * <li>n = this.size();</li>
	 * </ul>
	 * For quantileStart &gt; quantileEnd the method returns getQuantileExpectation(quantileEnd, quantileStart).
	 *
	 * @param quantileStart Lower bound of the integral.
	 * @param quantileEnd  Upper bound of the integral.
	 * @return The (conditional) expectation of the values between two quantile levels assuming equi-distribution.
	 */
	double getQuantileExpectation(double quantileStart, double quantileEnd);

	/**
	 * Generates a Histogram based on the realizations stored in this random variable.
	 * The returned <code>result</code> array's length is <code>intervalPoints.length+1</code>.
	 * <ul>
	 * 	<li>The value result[0] equals the relative frequency of values observed in the interval ( -infinity, intervalPoints[0] ].</li>
	 * 	<li>The value result[i] equals the relative frequency of values observed in the interval ( intervalPoints[i-1], intervalPoints[i] ].</li>
	 * 	<li>The value result[n] equals the relative frequency of values observed in the interval ( intervalPoints[n-1], infinity ).</li>
	 * </ul>
	 * where n = intervalPoints.length. Note that the intervals are open on the left, closed on the right, i.e.,
	 * result[i] contains the number of elements x with intervalPoints[i-1] &lt; x &le; intervalPoints[i].
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
	double[] getHistogram(double[] intervalPoints);

	/**
	 * Generates a histogram based on the realizations stored in this random variable
	 * using interval points calculated from the arguments, see also {@link #getHistogram(double[])}.
	 * The interval points are
	 * set with equal distance over an the interval of the specified standard deviation.
	 *
	 * The interval points used are
	 * <code>x[i] = mean + alpha[i] * standardDeviations * sigma</code>
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
	double[][] getHistogram(int numberOfPoints, double standardDeviations);

	/**
	 * Return a cacheable version of this object (often a self-reference).
	 * This method should be called when you store the object for later use,
	 * i.e., assign it, or when the object is consumed in a function, but later
	 * used also in another function.
	 *
	 * @return A cacheable version of this object (often a self-reference).
	 */
	RandomVariable cache();

	/**
	 * Applies x &rarr; operator(x) to this random variable.
	 * It returns a new random variable with the result.
	 *
	 * @param operator An unary operator/function, mapping RandomVariable to RandomVariable.
	 * @return New random variable with the result of the function.
	 */
	default RandomVariable appy(RandomOperator operator) {
		return operator.apply(this);
	}

	/**
	 * Applies x &rarr; operator(x) to this random variable.
	 * It returns a new random variable with the result.
	 *
	 * @param operator An unary operator/function, mapping double to double.
	 * @return New random variable with the result of the function.
	 */
	RandomVariable apply(DoubleUnaryOperator operator);

	/**
	 * Applies x &rarr; operator(x,y) to this random variable, where x is this random variable and y is a given random variable.
	 * It returns a new random variable with the result.
	 *
	 * @param operator A binary operator/function, mapping (double,double) to double.
	 * @param argument A random variable.
	 * @return New random variable with the result of the function.
	 */
	RandomVariable apply(DoubleBinaryOperator operator, RandomVariable argument);

	/**
	 * Applies x &rarr; operator(x,y,z) to this random variable, where x is this random variable and y and z are given random variable.
	 * It returns a new random variable with the result.
	 *
	 * @param operator A ternary operator/function, mapping (double,double,double) to double.
	 * @param argument1 A random variable representing y.
	 * @param argument2 A random variable representing z.
	 * @return New random variable with the result of the function.
	 */
	RandomVariable apply(DoubleTernaryOperator operator, RandomVariable argument1, RandomVariable argument2);

	/**
	 * Applies x &rarr; min(x,cap) to this random variable.
	 * It returns a new random variable with the result.
	 *
	 * @param cap The cap.
	 * @return New random variable with the result of the function.
	 */
	RandomVariable cap(double cap);

	/**
	 * Applies x &rarr; max(x,floor) to this random variable.
	 * It returns a new random variable with the result.
	 *
	 * @param floor The floor.
	 * @return New random variable with the result of the function.
	 */
	RandomVariable floor(double floor);

	/**
	 * Applies x &rarr; x + value to this random variable.
	 * It returns a new random variable with the result.
	 *
	 * @param value The value to add.
	 * @return New random variable with the result of the function.
	 */
	RandomVariable add(double value);

	/**
	 * Applies x &rarr; x - value to this random variable.
	 * @param value The value to subtract.
	 * @return New random variable with the result of the function.
	 */
	RandomVariable sub(double value);

	/**
	 * Applies x &rarr; value - x to this random variable.
	 * @param value The value from which this is subtracted.
	 * @return New random variable with the result of the function.
	 */
	default RandomVariable bus(double value) {
		return this.mult(-1).add(value);
	}

	/**
	 * Applies x &rarr; x * value to this random variable.
	 * @param value The value to multiply.
	 * @return New random variable with the result of the function.
	 */
	RandomVariable mult(double value);

	/**
	 * Applies x &rarr; x / value to this random variable.
	 * @param value The value to divide.
	 * @return New random variable with the result of the function.
	 */
	RandomVariable div(double value);

	/**
	 * Applies x &rarr; value / x to this random variable.
	 * @param value The numerator of the ratio where this is the denominator.
	 * @return New random variable with the result of the function.
	 */
	default RandomVariable vid(double value) {
		return invert().mult(value);
	}

	/**
	 * Applies x &rarr; pow(x,exponent) to this random variable.
	 * @param exponent The exponent.
	 * @return New random variable with the result of the function.
	 */
	RandomVariable pow(double exponent);

	/**
	 * Returns a random variable which is deterministic and corresponds
	 * the expectation of this random variable.
	 *
	 * @return New random variable being the expectation of this random variable.
	 */
	RandomVariable average();

	/**
	 * Returns a random variable which is deterministic and corresponds
	 * the expectation of this random variable.
	 *
	 * @return New random variable being the expectation of this random variable.
	 */
	default RandomVariable expectation() {
		return average();
	}

	/**
	 * Returns a random variable which is deterministic and corresponds
	 * the variance of this random variable.
	 *
	 * @return New random variable being the variance of this random variable and the argument.
	 */
	default RandomVariable variance()
	{
		final RandomVariable meanDeviation = this.sub(average());
		return meanDeviation.squared().average();
	}

	/**
	 * Returns a random variable which is deterministic and corresponds
	 * the covariance of this random variable and the argument.
	 *
	 * @param value The random variable Y to be used in Cov(X,Y) with X being this.
	 * @return New random variable being the covariance of this random variable and the argument.
	 */
	default RandomVariable covariance(RandomVariable value)
	{
		return this.sub(average()).mult(value.sub(value.average())).average();
	}

	/**
	 * Returns the conditional expectation using a given conditional expectation estimator.
	 *
	 * @param conditionalExpectationOperator A given conditional expectation estimator.
	 * @return The conditional expectation of this random variable (as a random variable)
	 */
	default RandomVariable getConditionalExpectation(final ConditionalExpectationEstimator conditionalExpectationOperator)
	{
		return conditionalExpectationOperator.getConditionalExpectation(this);
	}

	/**
	 * Applies x &rarr; x * x to this random variable.
	 * @return New random variable with the result of the function.
	 */
	RandomVariable squared();

	/**
	 * Applies x &rarr; sqrt(x) to this random variable.
	 * @return New random variable with the result of the function.
	 */
	RandomVariable sqrt();

	/**
	 * Applies x &rarr; exp(x) to this random variable.
	 * @return New random variable with the result of the function.
	 */
	RandomVariable exp();

	/**
	 * Applies x &rarr; expm1(x) (that is x &rarr; exp(x)-1.0) to this random variable.
	 * @return New random variable with the result of the function.
	 */
	default RandomVariable expm1() {
		return this.exp().sub(1.0);
	}

	/**
	 * Applies x &rarr; log(x) to this random variable.
	 * @return New random variable with the result of the function.
	 */
	RandomVariable log();

	/**
	 * Applies x &rarr; sin(x) to this random variable.
	 * @return New random variable with the result of the function.
	 */
	RandomVariable sin();

	/**
	 * Applies x &rarr; cos(x) to this random variable.
	 * @return New random variable with the result of the function.
	 */
	RandomVariable cos();

	/**
	 * Applies x &rarr; x+randomVariable to this random variable.
	 * @param randomVariable A random variable (compatible with this random variable).
	 * @return New random variable with the result of the function.
	 */
	RandomVariable add(RandomVariable randomVariable);

	/**
	 * Applies x &rarr; x-randomVariable to this random variable.
	 * @param randomVariable A random variable (compatible with this random variable).
	 * @return New random variable with the result of the function.
	 */
	RandomVariable sub(RandomVariable randomVariable);

	/**
	 * Applies x &rarr; randomVariable-x to this random variable.
	 * @param randomVariable A random variable (compatible with this random variable).
	 * @return New random variable with the result of the function.
	 */
	RandomVariable bus(RandomVariable randomVariable);

	/**
	 * Applies x &rarr; x*randomVariable to this random variable.
	 * @param randomVariable A random variable (compatible with this random variable).
	 * @return New random variable with the result of the function.
	 */
	RandomVariable mult(RandomVariable randomVariable);

	/**
	 * Applies x &rarr; x/randomVariable to this random variable.
	 * @param randomVariable A random variable (compatible with this random variable).
	 * @return New random variable with the result of the function.
	 */
	RandomVariable div(RandomVariable randomVariable);

	/**
	 * Applies x &rarr; randomVariable/x to this random variable.
	 * @param randomVariable A random variable (compatible with this random variable).
	 * @return New random variable with the result of the function.
	 */
	RandomVariable vid(RandomVariable randomVariable);

	/**
	 * Applies x &rarr; min(x,cap) to this random variable.
	 * @param cap The cap. A random variable (compatible with this random variable).
	 * @return New random variable with the result of the function.
	 */
	RandomVariable cap(RandomVariable cap);

	/**
	 * Applies x &rarr; max(x,floor) to this random variable.
	 * @param floor The floor. A random variable (compatible with this random variable).
	 * @return New random variable with the result of the function.
	 */
	RandomVariable floor(RandomVariable floor);

	/**
	 * Applies x &rarr; x * (1.0 + rate * periodLength) to this random variable.
	 * @param rate The accruing rate. A random variable (compatible with this random variable).
	 * @param periodLength The period length
	 * @return New random variable with the result of the function.
	 */
	RandomVariable accrue(RandomVariable rate, double periodLength);

	/**
	 * Applies x &rarr; x / (1.0 + rate * periodLength) to this random variable.
	 * @param rate The discounting rate. A random variable (compatible with this random variable).
	 * @param periodLength The period length
	 * @return New random variable with the result of the function.
	 */
	RandomVariable discount(RandomVariable rate, double periodLength);

	/**
	 * Applies x &rarr; (x &ge; 0 ? valueIfTriggerNonNegative : valueIfTriggerNegative)
	 * @param valueIfTriggerNonNegative The value used if this is greater or equal 0
	 * @param valueIfTriggerNegative The value used if the this is less than 0
	 * @return New random variable with the result of the function.
	 */
	RandomVariable choose(RandomVariable valueIfTriggerNonNegative, RandomVariable valueIfTriggerNegative);

	/**
	 * Applies x &rarr; 1/x to this random variable.
	 * @return New random variable with the result of the function.
	 */
	RandomVariable invert();

	/**
	 * Applies x &rarr; Math.abs(x), i.e. x &rarr; |x| to this random variable.
	 * @return New random variable with the result of the function.
	 */
	RandomVariable abs();

	/**
	 * Applies x &rarr; x + factor1 * factor2
	 * @param factor1 The factor 1. A random variable (compatible with this random variable).
	 * @param factor2 The factor 2.
	 * @return New random variable with the result of the function.
	 */
	RandomVariable addProduct(RandomVariable factor1, double factor2);

	/**
	 * Applies x &rarr; x + factor1 * factor2
	 * @param factor1 The factor 1. A random variable (compatible with this random variable).
	 * @param factor2 The factor 2. A random variable (compatible with this random variable).
	 * @return New random variable with the result of the function.

	 */
	RandomVariable addProduct(RandomVariable factor1, RandomVariable factor2);

	/**
	 * Applies x &rarr; x + numerator / denominator
	 *
	 * @param numerator The numerator of the ratio to add. A random variable (compatible with this random variable).
	 * @param denominator The denominator of the ratio to add. A random variable (compatible with this random variable).
	 * @return New random variable with the result of the function.

	 */
	RandomVariable addRatio(RandomVariable numerator, RandomVariable denominator);

	/**
	 * Applies x &rarr; x - numerator / denominator
	 *
	 * @param numerator The numerator of the ratio to sub. A random variable (compatible with this random variable).
	 * @param denominator The denominator of the ratio to sub. A random variable (compatible with this random variable).
	 * @return New random variable with the result of the function.

	 */
	RandomVariable subRatio(RandomVariable numerator, RandomVariable denominator);

	/**
	 * Applies \( x \mapsto x + \sum_{i=0}^{n-1} factor1_{i} * factor2_{i}
	 * @param factor1 The factor 1. A list of random variables (compatible with this random variable).
	 * @param factor2 The factor 2. A list of random variables (compatible with this random variable).
	 * @return New random variable with the result of the function.
	 */
	default RandomVariable addSumProduct(final RandomVariable[] factor1, final RandomVariable[] factor2)
	{
		RandomVariable result = this;
		for(int i=0; i<factor1.length; i++) {
			result = result.addProduct(factor1[i], factor2[i]);
		}
		return result;
	}

	/**
	 * Applies \( x \mapsto x + \sum_{i=0}^{n-1} factor1_{i} * factor2_{i}
	 * @param factor1 The factor 1. A list of random variables (compatible with this random variable).
	 * @param factor2 The factor 2. A list of random variables (compatible with this random variable).
	 * @return New random variable with the result of the function.
	 */
	default RandomVariable addSumProduct(final List<RandomVariable> factor1, final List<RandomVariable> factor2)
	{
		RandomVariable result = this;
		for(int i=0; i<factor1.size(); i++) {
			result = result.addProduct(factor1.get(i), factor2.get(i));
		}
		return result;
	}

	/**
	 * Applies x &rarr; (Double.isNaN(x) ? 1.0 : 0.0)
	 *
	 * @return A random variable which is 1.0 for all states that are NaN, otherwise 0.0.
	 */
	RandomVariable isNaN();
}
