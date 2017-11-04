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
 * This interface describes the methods implemented by an immutable random variable, i.e.
 * methods that leave a random variable unchanged (immutable).
 * This is used to ensure that arguments or return values are not changed.
 * 
 * For C++ guys: In C++ you could achieve this by making a return value const.
 *
 * <br>
 * 
 * <b>IMPORTANT:</b> As of version 1.3 / revision 487 the design of RandomVariable, RandomVariableInterface has changed:
 * All methods of RandomVariable leave the object immutable and the interface ImmutableRandomVariableInterface has been renamed
 * to RandomVariableInterface. Your code remains compatible if you perform the following changes:
 * <ul>
 * <li>Change calls to RandomVariableInterface objects value like <code>value.mult(argument);</code> to <code>value = value.mult(argument);</code>
 * <li>Remove calls to getMutableCopy() since they are no longer needed.
 * <li>Remove wrapping in RandomVariableMutableClone since they are no longer needed.
 * </ul>
 * The change has some performance impact, however, the original performance may be achieved again
 * via the use of Java 8 lambdas and the concept of the <code>RandomVariableAccumulatorInterface</code>.
 * <br>
 *
 * @author Christian Fries
 * @version 1.5
 */
public interface RandomVariableInterface extends Serializable {

	/**
	 * Compare this random variable with a given one
	 * 
	 * @param randomVariable Random variable to compare with.
	 * @return True if this random variable and the given one are equal, otherwise false
	 */
	boolean equals(RandomVariableInterface randomVariable);

	/**
	 * Returns the filtration time.
	 * 
	 * @return The filtration time.
	 */
	double getFiltrationTime();

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
	double getAverage(RandomVariableInterface probabilities);

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
	double getVariance(RandomVariableInterface probabilities);

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
	double getStandardDeviation(RandomVariableInterface probabilities);

	/**
	 * Returns the standard error (discretization error) of this random variable.
	 * For a Monte-Carlo simulation this is 1/Math.sqrt(n) * {@link #getStandardDeviation() }.
	 * 
	 * @return The standard error assuming equi-distribution.
	 */
	double getStandardError();

	/**
	 * Returns the standard error (discretization error) of this random variable.
	 * For a Monte-Carlo simulation this is 1/Math.sqrt(n) * {@link #getStandardDeviation(RandomVariableInterface) }.
	 * 
	 * @param probabilities The probability weights.
	 * @return The standard error assuming the given probability weights.
	 */
	double getStandardError(RandomVariableInterface probabilities);

	/**
	 * Returns the quantile value for this given random variable, i.e., the value x such that P(this &lt; x) = quantile,
	 * where P denotes the probability measure.
	 * The method will consider picewise constant values (with constant extrapolation) in the random variable.
	 * That is getQuantile(0) and getQuantile(1) will return the largest and smallest value.
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
	double getQuantile(double quantile, RandomVariableInterface probabilities);

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
	double[][] getHistogram(int numberOfPoints, double standardDeviations);

	/**
	 * Return a cacheable version of this object (often a self-reference).
	 * This method should be called when you store the object for later use,
	 * i.e., assign it, or when the object is consumed in a function, but later
	 * used also in another function.
	 *
	 * @return A cacheable version of this object (often a self-reference).
	 */
	RandomVariableInterface cache();

	/**
	 * Applies x &rarr; operator(x) to this random variable.
	 * It returns a new random variable with the result.
	 *
	 * @param operator An unary operator/function, mapping double to double.
	 * @return New random variable with the result of the function.
	 */
	RandomVariableInterface apply(DoubleUnaryOperator operator);

	/**
	 * Applies x &rarr; operator(x,y) to this random variable, where x is this random variable and y is a given random variable.
	 * It returns a new random variable with the result.
	 *
	 * @param operator A binary operator/function, mapping (double,double) to double.
	 * @param argument A random variable.
	 * @return New random variable with the result of the function.
	 */
	RandomVariableInterface apply(DoubleBinaryOperator operator, RandomVariableInterface argument);

	/**
	 * Applies x &rarr; operator(x,y,z) to this random variable, where x is this random variable and y and z are given random variable.
	 * It returns a new random variable with the result.
	 *
	 * @param operator A ternary operator/function, mapping (double,double,double) to double.
	 * @param argument1 A random variable representing y.
	 * @param argument2 A random variable representing z.
	 * @return New random variable with the result of the function.
	 */
	RandomVariableInterface apply(DoubleTernaryOperator operator, RandomVariableInterface argument1, RandomVariableInterface argument2);

	/**
	 * Applies x &rarr; min(x,cap) to this random variable.
	 * It returns a new random variable with the result.
	 *
	 * @param cap The cap.
	 * @return New random variable with the result of the function.
	 */
	default RandomVariableInterface cap(double cap) { return apply( x->Math.min(x,cap) ); }

	/**
	 * Applies x &rarr; max(x,floor) to this random variable.
	 * It returns a new random variable with the result.
	 *
	 * @param floor The floor.
	 * @return New random variable with the result of the function.
	 */
	RandomVariableInterface floor(double floor);

	/**
	 * Applies x &rarr; x + value to this random variable.
	 * It returns a new random variable with the result.
	 *
	 * @param value The value to add.
	 * @return New random variable with the result of the function.
	 */
	RandomVariableInterface add(double value);

	/**
	 * Applies x &rarr; x - value to this random variable.
	 * @param value The value to subtract.
	 * @return New random variable with the result of the function.
	 */
	RandomVariableInterface sub(double value);

	/**
	 * Applies x &rarr; x * value to this random variable.
	 * @param value The value to multiply.
	 * @return New random variable with the result of the function.
	 */
	RandomVariableInterface mult(double value);

	/**
	 * Applies x &rarr; x / value to this random variable.
	 * @param value The value to divide.
	 * @return New random variable with the result of the function.
	 */
	RandomVariableInterface div(double value);

	/**
	 * Applies x &rarr; pow(x,exponent) to this random variable.
	 * @param exponent The exponent.
	 * @return New random variable with the result of the function.
	 */
	RandomVariableInterface pow(double exponent);

	/**
	 * Returns a random variable which is deterministic and corresponds
	 * the expectation of this random variable.
	 * 
	 * @return New random variable being the expectation of this random variable.
	 */
	RandomVariableInterface average();

	/**
	 * Returns the conditional expectation using a given conditional expectation estimator.
	 * 
	 * @param conditionalExpectationOperator A given conditional expectation estimator.
	 * @return The conditional expectation of this random variable (as a random variable)
	 */
	default RandomVariableInterface getConditionalExpectation(ConditionalExpectationEstimatorInterface conditionalExpectationOperator)
	{
		return conditionalExpectationOperator.getConditionalExpectation(this);
	}

	/**
	 * Applies x &rarr; x * x to this random variable.
	 * @return New random variable with the result of the function.
	 */
	RandomVariableInterface squared();

	/**
	 * Applies x &rarr; sqrt(x) to this random variable.
	 * @return New random variable with the result of the function.
	 */
	RandomVariableInterface sqrt();

	/**
	 * Applies x &rarr; exp(x) to this random variable.
	 * @return New random variable with the result of the function.
	 */
	RandomVariableInterface exp();

	/**
	 * Applies x &rarr; log(x) to this random variable.
	 * @return New random variable with the result of the function.
	 */
	RandomVariableInterface log();

	/**
	 * Applies x &rarr; sin(x) to this random variable.
	 * @return New random variable with the result of the function.
	 */
	RandomVariableInterface sin();

	/**
	 * Applies x &rarr; cos(x) to this random variable.
	 * @return New random variable with the result of the function.
	 */
	RandomVariableInterface cos();

	/**
	 * Applies x &rarr; x+randomVariable to this random variable.
	 * @param randomVariable A random variable (compatible with this random variable).
	 * @return New random variable with the result of the function.
	 */
	RandomVariableInterface add(RandomVariableInterface randomVariable);

	/**
	 * Applies x &rarr; x-randomVariable to this random variable.
	 * @param randomVariable A random variable (compatible with this random variable).
	 * @return New random variable with the result of the function.
	 */
	RandomVariableInterface sub(RandomVariableInterface randomVariable);

	/**
	 * Applies x &rarr; x*randomVariable to this random variable.
	 * @param randomVariable A random variable (compatible with this random variable).
	 * @return New random variable with the result of the function.
	 */
	RandomVariableInterface mult(RandomVariableInterface randomVariable);

	/**
	 * Applies x &rarr; x/randomVariable to this random variable.
	 * @param randomVariable A random variable (compatible with this random variable).
	 * @return New random variable with the result of the function.
	 */
	RandomVariableInterface div(RandomVariableInterface randomVariable);

	/**
	 * Applies x &rarr; min(x,cap) to this random variable.
	 * @param cap The cap. A random variable (compatible with this random variable).
	 * @return New random variable with the result of the function.
	 */
	RandomVariableInterface cap(RandomVariableInterface cap);

	/**
	 * Applies x &rarr; max(x,floor) to this random variable.
	 * @param floor The floor. A random variable (compatible with this random variable).
	 * @return New random variable with the result of the function.
	 */
	RandomVariableInterface floor(RandomVariableInterface floor);

	/**
	 * Applies x &rarr; x * (1.0 + rate * periodLength) to this random variable.
	 * @param rate The accruing rate. A random variable (compatible with this random variable).
	 * @param periodLength The period length
	 * @return New random variable with the result of the function.
	 */
	RandomVariableInterface accrue(RandomVariableInterface rate, double periodLength);

	/**
	 * Applies x &rarr; x / (1.0 + rate * periodLength) to this random variable.
	 * @param rate The discounting rate. A random variable (compatible with this random variable).
	 * @param periodLength The period length
	 * @return New random variable with the result of the function.
	 */
	RandomVariableInterface discount(RandomVariableInterface rate, double periodLength);

	/**
	 * Applies x &rarr; (trigger &ge; 0 ? valueIfTriggerNonNegative : valueIfTriggerNegative)
	 * @param trigger The trigger. A random variable (compatible with this random variable).
	 * @param valueIfTriggerNonNegative The value used if the trigger is greater or equal 0
	 * @param valueIfTriggerNegative The value used if the trigger is less than 0
	 * @return New random variable with the result of the function.
	 */
	RandomVariableInterface barrier(RandomVariableInterface trigger, RandomVariableInterface valueIfTriggerNonNegative, RandomVariableInterface valueIfTriggerNegative);

	/**
	 * Applies x &rarr; (trigger &ge; 0 ? valueIfTriggerNonNegative : valueIfTriggerNegative)
	 * @param trigger The trigger. A random variable (compatible with this random variable).
	 * @param valueIfTriggerNonNegative The value used if the trigger is greater or equal 0
	 * @param valueIfTriggerNegative The value used if the trigger is less than 0
	 * @return New random variable with the result of the function.
	 */
	RandomVariableInterface barrier(RandomVariableInterface trigger, RandomVariableInterface valueIfTriggerNonNegative, double valueIfTriggerNegative);

	/**
	 * Applies x &rarr; 1/x to this random variable.
	 * @return New random variable with the result of the function.
	 */
	RandomVariableInterface invert();

	/**
	 * Applies x &rarr; Math.abs(x), i.e. x &rarr; |x| to this random variable.
	 * @return New random variable with the result of the function.
	 */
	RandomVariableInterface abs();

	/**
	 * Applies x &rarr; x + factor1 * factor2
	 * @param factor1 The factor 1. A random variable (compatible with this random variable).
	 * @param factor2 The factor 2.
	 * @return New random variable with the result of the function.
	 */
	RandomVariableInterface addProduct(RandomVariableInterface factor1, double factor2);

	/**
	 * Applies x &rarr; x + factor1 * factor2
	 * @param factor1 The factor 1. A random variable (compatible with this random variable).
	 * @param factor2 The factor 2. A random variable (compatible with this random variable).
	 * @return New random variable with the result of the function.

	 */
	RandomVariableInterface addProduct(RandomVariableInterface factor1, RandomVariableInterface factor2);

	/**
	 * Applies x &rarr; x + numerator / denominator
	 *
	 * @param numerator The numerator of the ratio to add. A random variable (compatible with this random variable).
	 * @param denominator The denominator of the ratio to add. A random variable (compatible with this random variable).
	 * @return New random variable with the result of the function.

	 */
	RandomVariableInterface addRatio(RandomVariableInterface numerator, RandomVariableInterface denominator);

	/**
	 * Applies x &rarr; x - numerator / denominator
	 *
	 * @param numerator The numerator of the ratio to sub. A random variable (compatible with this random variable).
	 * @param denominator The denominator of the ratio to sub. A random variable (compatible with this random variable).
	 * @return New random variable with the result of the function.

	 */
	RandomVariableInterface subRatio(RandomVariableInterface numerator, RandomVariableInterface denominator);

	/**
	 * Applies \( x \mapsto x + \sum_{i=0}^{n-1} factor1_{i} * factor2_{i}
	 * @param factor1 The factor 1. A list of random variables (compatible with this random variable).
	 * @param factor2 The factor 2. A list of random variables (compatible with this random variable).
	 * @return New random variable with the result of the function.

	 */
	default RandomVariableInterface addSumProduct(List<RandomVariableInterface> factor1, List<RandomVariableInterface> factor2)
	{
		RandomVariableInterface result = this;
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
	RandomVariableInterface isNaN();
}
