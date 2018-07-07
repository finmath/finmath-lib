/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2006
 */
package net.finmath.montecarlo;

import java.util.Arrays;
import java.util.List;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntToDoubleFunction;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import org.apache.commons.math3.util.FastMath;

import net.finmath.functions.DoubleTernaryOperator;
import net.finmath.stochastic.ConditionalExpectationEstimatorInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * The class RandomVariable represents a random variable being the evaluation of a stochastic process
 * at a certain time within a Monte-Carlo simulation.
 * It is thus essentially a vector of doubles - the realizations - together with a double - the time.
 * The index of the vector represents path.
 * The class may also be used for non-stochastic quantities which may potentially be stochastic
 * (e.g. volatility). If only non-stochastic random variables are involved in an operation the class uses
 * optimized code.
 *
 * Accesses performed exclusively through the interface
 * <code>RandomVariableInterface</code> is thread safe (and does not mutate the class).
 * 
 * The implementation require Java 8 or better.
 *
 * @author Christian Fries
 * @version 2.0
 */
public class RandomVariable implements RandomVariableInterface {

	private static final long serialVersionUID = -1352953450936857742L;

	private final double      time;	                // Time (filtration)

	// Data model for the stochastic case (otherwise null)
	private final double[]    realizations;           // Realizations

	// Data model for the non-stochastic case (if realizations==null)
	private final double      valueIfNonStochastic;

	/**
	 * Create a random variable from a given other implementation of <code>RandomVariableInterface</code>.
	 *
	 * @param value Object implementing <code>RandomVariableInterface</code>.
	 */
	public RandomVariable(RandomVariableInterface value) {
		super();
		this.time = value.getFiltrationTime();
		this.realizations = value.isDeterministic() ? null : value.getRealizations();
		this.valueIfNonStochastic = value.isDeterministic() ? value.get(0) : Double.NaN;
	}

	/**
	 * Create a non stochastic random variable, i.e. a constant.
	 *
	 * @param value the value, a constant.
	 */
	public RandomVariable(double value) {
		this(-Double.MAX_VALUE, value);
	}

	/**
	 * Create a random variable by applying a function to a given other implementation of <code>RandomVariableInterface</code>.
	 *
	 * @param value Object implementing <code>RandomVariableInterface</code>.
	 * @param function A function mapping double to double.
	 */
	public RandomVariable(RandomVariableInterface value, DoubleUnaryOperator function) {
		super();
		this.time = value.getFiltrationTime();
		this.realizations = value.isDeterministic() ? null : value.getRealizationsStream().map(function).toArray();
		this.valueIfNonStochastic = value.isDeterministic() ? function.applyAsDouble(value.get(0)) : Double.NaN;
	}


	/**
	 * Create a non stochastic random variable, i.e. a constant.
	 *
	 * @param time the filtration time, set to 0.0 if not used.
	 * @param value the value, a constant.
	 */
	public RandomVariable(double time, double value) {
		super();
		this.time = time;
		this.realizations = null;
		this.valueIfNonStochastic = value;
	}

	/**
	 * Create a non stochastic random variable, i.e. a constant.
	 *
	 * @param time the filtration time, set to 0.0 if not used.
	 * @param numberOfPath The number of paths.
	 * @param value the value, a constant.
	 */
	public RandomVariable(double time, int numberOfPath, double value) {
		super();
		this.time = time;
		this.realizations = new double[numberOfPath];
		java.util.Arrays.fill(this.realizations, value);
		this.valueIfNonStochastic = Double.NaN;
	}

	/**
	 * Create a stochastic random variable.
	 * 
	 * Important: The realizations array is not cloned (not defensive copy is made).
	 * 
	 * @TODO A future version should perform a defensive copy.
	 * 
	 * @param time the filtration time, set to 0.0 if not used.
	 * @param realisations the vector of realizations.
	 */
	public RandomVariable(double time, double[] realisations) {
		super();
		this.time = time;
		this.realizations = realisations;
		this.valueIfNonStochastic = Double.NaN;
	}

	/**
	 * Create a stochastic random variable.
	 *
	 * @param time the filtration time, set to 0.0 if not used.
	 * @param realizations A map mapping integer (path or state) to double, representing this random variable.
	 * @param size The size, i.e., number of paths.
	 */
	public RandomVariable(double time, IntToDoubleFunction realizations, int size) {
		super();
		this.time = time;
		this.realizations = size == 1 ? null : new double[size];//IntStream.range(0,size).parallel().mapToDouble(realisations).toArray();
		this.valueIfNonStochastic = size == 1 ? realizations.applyAsDouble(0) : Double.NaN;
		if(size > 1) {
			IntStream.range(0,size).parallel().forEach(i -> 
			this.realizations[i] = realizations.applyAsDouble(i)
					);
		}
	}

	@Override
	public boolean equals(RandomVariableInterface randomVariable) {
		if(this.time != randomVariable.getFiltrationTime()) {
			return false;
		}
		if(this.isDeterministic() && randomVariable.isDeterministic()) {
			return this.valueIfNonStochastic == randomVariable.get(0);
		}

		if(this.isDeterministic() != randomVariable.isDeterministic()) {
			return false;
		}

		for(int i=0; i<realizations.length; i++) {
			if(realizations[i] != randomVariable.get(i)) {
				return false;
			}
		}

		return true;
	}

	@Override
	public double getFiltrationTime() {
		return time;
	}

	@Override
	public double get(int pathOrState) {
		if(isDeterministic()) {
			return valueIfNonStochastic;
		} else {
			return realizations[pathOrState];
		}
	}

	@Override
	public int size() {
		if(isDeterministic()) {
			return 1;
		} else {
			return realizations.length;
		}
	}

	@Override
	public double getMin() {
		if(isDeterministic()) {
			return valueIfNonStochastic;
		}
		double min = Double.MAX_VALUE;
		if(realizations.length != 0)
		 {
			min = realizations[0];     /// @see getMax()
		}
		for(int i=0; i<realizations.length; i++) {
			min = Math.min(realizations[i],min);
		}
		return min;
	}

	@Override
	public double getMax() {
		if(isDeterministic()) {
			return valueIfNonStochastic;
		}
		double max = -Double.MAX_VALUE;
		if(realizations.length != 0) {
			max = realizations[0];
		}
		for(int i=0; i<realizations.length; i++) {
			max = Math.max(realizations[i],max);
		}
		return max;
	}

	@Override
	public double getAverage() {
		if(isDeterministic()) {
			return valueIfNonStochastic;
		}
		if(size() == 0) {
			return Double.NaN;
		}

		/*
		 * Kahan summation on realizations[i]
		 */
		double sum = 0.0;								// Running sum
		double error = 0.0;								// Running error compensation
		for(int i=0; i<realizations.length; i++)  {
			double value = realizations[i] - error;		// Error corrected value
			double newSum = sum + value;				// New sum
			error = (newSum - sum) - value;				// New numerical error
			sum	= newSum;
		}
		return sum/realizations.length;
	}

	@Override
	public double getAverage(RandomVariableInterface probabilities) {
		if(isDeterministic()) {
			return valueIfNonStochastic;
		}
		if(size() == 0) {
			return Double.NaN;
		}

		/*
		 * Kahan summation on (realizations[i] * probabilities.get(i))
		 */
		double sum = 0.0;
		double error = 0.0;														// Running error compensation
		for(int i=0; i<realizations.length; i++)  {
			double value = realizations[i] * probabilities.get(i) - error;		// Error corrected value
			double newSum = sum + value;				// New sum
			error = (newSum - sum) - value;				// New numerical error
			sum	= newSum;
		}
		return sum / realizations.length;
	}

	@Override
	public double getVariance() {
		if(isDeterministic() || size() == 1) {
			return 0.0;
		}
		if(size() == 0) {
			return Double.NaN;
		}

		double average = getAverage();

		/*
		 * Kahan summation on (realizations[i] - average)^2
		 */
		double sum = 0.0;
		double errorOfSum	= 0.0;
		for(int i=0; i<realizations.length; i++) {
			double value	= (realizations[i] - average)*(realizations[i] - average) - errorOfSum;
			double newSum	= sum + value;
			errorOfSum		= (newSum - sum) - value;
			sum				= newSum;
		}
		return sum/realizations.length;
	}

	@Override
	public double getVariance(RandomVariableInterface probabilities) {
		if(isDeterministic()) {
			return 0.0;
		}
		if(size() == 0) {
			return Double.NaN;
		}

		double average = getAverage(probabilities);

		/*
		 * Kahan summation on (realizations[i] - average)^2 * probabilities.get(i)
		 */
		double sum = 0.0;
		double errorOfSum	= 0.0;
		for(int i=0; i<realizations.length; i++) {
			double value	= (realizations[i] - average) * (realizations[i] - average) * probabilities.get(i) - errorOfSum;
			double newSum	= sum + value;
			errorOfSum		= (newSum - sum) - value;
			sum				= newSum;
		}
		return sum;
	}

	@Override
	public double getSampleVariance() {
		if(isDeterministic() || size() == 1) {
			return 0.0;
		}
		if(size() == 0) {
			return Double.NaN;
		}

		return getVariance() * size()/(size()-1);
	}

	@Override
	public double getStandardDeviation() {
		if(isDeterministic()) {
			return 0.0;
		}
		if(size() == 0) {
			return Double.NaN;
		}

		return Math.sqrt(getVariance());
	}

	@Override
	public double getStandardDeviation(RandomVariableInterface probabilities) {
		if(isDeterministic()) {
			return 0.0;
		}
		if(size() == 0) {
			return Double.NaN;
		}

		return Math.sqrt(getVariance(probabilities));
	}

	@Override
	public double getStandardError() {
		if(isDeterministic()) {
			return 0.0;
		}
		if(size() == 0) {
			return Double.NaN;
		}

		return getStandardDeviation()/Math.sqrt(size());
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getStandardError(net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public double getStandardError(RandomVariableInterface probabilities) {
		if(isDeterministic()) {
			return 0.0;
		}
		if(size() == 0) {
			return Double.NaN;
		}

		return getStandardDeviation(probabilities)/Math.sqrt(size());
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getQuantile()
	 */
	@Override
	public double getQuantile(double quantile) {
		if(isDeterministic()) {
			return valueIfNonStochastic;
		}
		if(size() == 0) {
			return Double.NaN;
		}

		double[] realizationsSorted = realizations.clone();
		java.util.Arrays.sort(realizationsSorted);

		int indexOfQuantileValue = Math.min(Math.max((int)Math.round((size()+1) * quantile - 1), 0), size()-1);

		return realizationsSorted[indexOfQuantileValue];
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getQuantile(net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public double getQuantile(double quantile, RandomVariableInterface probabilities) {
		if(isDeterministic()) {
			return valueIfNonStochastic;
		}
		if(size() == 0) {
			return Double.NaN;
		}

		throw new RuntimeException("Method not implemented.");
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getConditionalVaR()
	 */
	@Override
	public double getQuantileExpectation(double quantileStart, double quantileEnd) {
		if(isDeterministic()) {
			return valueIfNonStochastic;
		}
		if(size() == 0) {
			return Double.NaN;
		}
		if(quantileStart > quantileEnd) {
			return getQuantileExpectation(quantileEnd, quantileStart);
		}

		double[] realizationsSorted = realizations.clone();
		java.util.Arrays.sort(realizationsSorted);

		int indexOfQuantileValueStart	= Math.min(Math.max((int)Math.round((size()+1) * quantileStart - 1), 0), size()-1);
		int indexOfQuantileValueEnd		= Math.min(Math.max((int)Math.round((size()+1) * quantileEnd - 1), 0), size()-1);

		double quantileExpectation = 0.0;
		for (int i=indexOfQuantileValueStart; i<=indexOfQuantileValueEnd;i++) {
			quantileExpectation += realizationsSorted[i];
		}
		quantileExpectation /= indexOfQuantileValueEnd-indexOfQuantileValueStart+1;

		return quantileExpectation;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getHistogram()
	 */
	@Override
	public double[] getHistogram(double[] intervalPoints)
	{
		double[] histogramValues = new double[intervalPoints.length+1];

		if(isDeterministic()) {
			/*
			 * If the random variable is deterministic we will return an array
			 * consisting of 0's and one and only one 1.
			 */
			java.util.Arrays.fill(histogramValues, 0.0);
			for (int intervalIndex=0; intervalIndex<intervalPoints.length; intervalIndex++)
			{
				if(valueIfNonStochastic > intervalPoints[intervalIndex]) {
					histogramValues[intervalIndex] = 1.0;
					break;
				}
			}
			histogramValues[intervalPoints.length] = 1.0;
		}
		else {
			/*
			 * If the random variable is deterministic we will return an array
			 * representing a density, where the sum of the entries is one.
			 * There is one exception:
			 * If the size of the random variable is 0, all entries will be zero.
			 */
			double[] realizationsSorted = realizations.clone();
			java.util.Arrays.sort(realizationsSorted);

			int sampleIndex=0;
			for (int intervalIndex=0; intervalIndex<intervalPoints.length; intervalIndex++)
			{
				int sampleCount = 0;
				while (sampleIndex < realizationsSorted.length &&
						realizationsSorted[sampleIndex] <= intervalPoints[intervalIndex])
				{
					sampleIndex++;
					sampleCount++;
				}
				histogramValues[intervalIndex] = sampleCount;
			}
			histogramValues[intervalPoints.length] = realizationsSorted.length-sampleIndex;

			// Normalize histogramValues
			if(realizationsSorted.length > 0) {
				for(int i=0; i<histogramValues.length; i++) {
					histogramValues[i] /= realizationsSorted.length;
				}
			}
		}

		return histogramValues;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getHistogram(int,double)
	 */
	@Override
	public double[][] getHistogram(int numberOfPoints, double standardDeviations) {
		double[] intervalPoints = new double[numberOfPoints];
		double[] anchorPoints	= new double[numberOfPoints+1];
		double center	= getAverage();
		double radius	= standardDeviations * getStandardDeviation();
		double stepSize	= (numberOfPoints-1) / 2.0;
		for(int i=0; i<numberOfPoints;i++) {
			double alpha = (-(double)(numberOfPoints-1) / 2.0 + i) / stepSize;
			intervalPoints[i]	= center + alpha * radius;
			anchorPoints[i]		= center + alpha * radius - radius / (2 * stepSize);
		}
		anchorPoints[numberOfPoints] = center + 1 * radius + radius / (2 * stepSize);

		double[][] result = new double[2][];
		result[0] = anchorPoints;
		result[1] = getHistogram(intervalPoints);

		return result;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#isDeterministic()
	 */
	@Override
	public boolean isDeterministic() {
		return realizations == null;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#expand()
	 */
	public RandomVariableInterface expand(int numberOfPaths) {
		if(isDeterministic()) {
			// Expand random variable to a vector of path values
			double[] clone = new double[numberOfPaths];
			java.util.Arrays.fill(clone,valueIfNonStochastic);
			return new RandomVariable(time,clone);

		}

		return new RandomVariable(time,realizations.clone());
	}

	@Override
	public RandomVariableInterface cache() {
		return this;
	}

	@Override
	public DoubleStream getRealizationsStream() {
		if(isDeterministic()) {
			return DoubleStream.generate(() -> {
				return valueIfNonStochastic;
			});
		}
		else {
			return Arrays.stream(realizations);
		}
	}

	@Override
	public double[] getRealizations() {
		if(isDeterministic()) {
			double[] result = new double[1];
			result[0] = get(0);
			return result;
		}
		else {
			return realizations.clone();
		}
	}

	@Override
	public Double doubleValue() {
		if(isDeterministic()) {
			return valueIfNonStochastic;
		} else {
			throw new UnsupportedOperationException("The random variable is non-deterministic");
		}
	}

	public IntToDoubleFunction getOperator() {
		if(isDeterministic()) {
			return i -> valueIfNonStochastic;
		}
		else {
			return i -> realizations[i];
		}
	}

	@Override
	public RandomVariableInterface apply(DoubleUnaryOperator operator) {
		if(isDeterministic()) {
			return new RandomVariable(time, operator.applyAsDouble(valueIfNonStochastic));
		}
		else
		{
			// Still faster than a parallel stream (2014.04)
			double[] result = new double[this.realizations.length];
			for(int i=0; i<result.length; i++) {
				result[i] = operator.applyAsDouble(realizations[i]);
			}
			return new RandomVariable(time, result);
		}
	}

	@Override
	public RandomVariableInterface apply(DoubleBinaryOperator operator, RandomVariableInterface argument) {
		double      newTime           = Math.max(time, argument.getFiltrationTime());

		if(isDeterministic() && argument.isDeterministic()) {
			return new RandomVariable(newTime, operator.applyAsDouble(valueIfNonStochastic, argument.get(0)));
		}
		else if(isDeterministic() && !argument.isDeterministic()) {
			// Still faster than a parallel stream (2014.04)
			double[] result = new double[argument.size()];
			for(int i=0; i<result.length; i++) {
				result[i] = operator.applyAsDouble(valueIfNonStochastic, argument.get(i));
			}
			return new RandomVariable(newTime, result);
		}
		else if(!isDeterministic() && argument.isDeterministic()) {
			// Still faster than a parallel stream (2014.04)
			double[] result = new double[this.size()];
			for(int i=0; i<result.length; i++) {
				result[i] = operator.applyAsDouble(realizations[i], argument.get(0));
			}
			return new RandomVariable(newTime, result);
		}
		else if(!isDeterministic() && !argument.isDeterministic()) {
			// Still faster than a parallel stream (2014.04)
			double[] result = new double[this.size()];
			for(int i=0; i<result.length; i++) {
				result[i] = operator.applyAsDouble(realizations[i], argument.get(i));
			}
			return new RandomVariable(newTime, result);
		}

		/*
		 * Dead code: slower
		 */
		int newSize = Math.max(this.size(), argument.size());

		IntToDoubleFunction argument0Operator = this.getOperator();
		IntToDoubleFunction argument1Operator = argument.getOperator();
		IntToDoubleFunction result = i -> operator.applyAsDouble(argument0Operator.applyAsDouble(i), argument1Operator.applyAsDouble(i));

		return new RandomVariable(newTime, result, newSize);
	}

	@Override
	public RandomVariableInterface apply(DoubleTernaryOperator operator, RandomVariableInterface argument1, RandomVariableInterface argument2) {
		double newTime = Math.max(time, argument1.getFiltrationTime());
		newTime = Math.max(newTime, argument2.getFiltrationTime());

		int newSize = Math.max(Math.max(this.size(), argument1.size()), argument2.size());

		IntToDoubleFunction argument0Operator = this.getOperator();
		IntToDoubleFunction argument1Operator = argument1.getOperator();
		IntToDoubleFunction argument2Operator = argument2.getOperator();
		IntToDoubleFunction result = i -> operator.applyAsDouble(argument0Operator.applyAsDouble(i), argument1Operator.applyAsDouble(i), argument2Operator.applyAsDouble(i));

		return new RandomVariable(newTime, result, newSize);
	}

	public RandomVariableInterface apply(DoubleBinaryOperator operatorOuter, DoubleBinaryOperator operatorInner, RandomVariableInterface argument1, RandomVariableInterface argument2)
	{
		return apply((x,y,z) -> operatorOuter.applyAsDouble(x,operatorInner.applyAsDouble(y,z)), argument1, argument2);
	}

	public RandomVariableInterface cap(double cap) {
		if(isDeterministic()) {
			double newValueIfNonStochastic = Math.min(valueIfNonStochastic,cap);
			return new RandomVariable(time, newValueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = Math.min(realizations[i],cap);
			}
			return new RandomVariable(time, newRealizations);
		}
	}

	@Override
	public RandomVariableInterface floor(double floor) {
		if(isDeterministic()) {
			double newValueIfNonStochastic = Math.max(valueIfNonStochastic,floor);
			return new RandomVariable(time, newValueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = Math.max(realizations[i],floor);
			}
			return new RandomVariable(time, newRealizations);
		}
	}

	@Override
	public RandomVariableInterface add(double value) {
		if(isDeterministic()) {
			double newValueIfNonStochastic = valueIfNonStochastic + value;
			return new RandomVariable(time, newValueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] + value;
			}
			return new RandomVariable(time, newRealizations);
		}
	}

	@Override
	public RandomVariableInterface sub(double value) {
		if(isDeterministic()) {
			double newValueIfNonStochastic = valueIfNonStochastic - value;
			return new RandomVariable(time, newValueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] - value;
			}
			return new RandomVariable(time, newRealizations);
		}
	}

	@Override
	public RandomVariableInterface mult(double value) {
		if(isDeterministic()) {
			double newValueIfNonStochastic = valueIfNonStochastic * value;
			return new RandomVariable(time, newValueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] * value;
			}
			return new RandomVariable(time, newRealizations);
		}
	}

	@Override
	public RandomVariableInterface div(double value) {
		if(isDeterministic()) {
			double newValueIfNonStochastic = valueIfNonStochastic / value;
			return new RandomVariable(time, newValueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] / value;
			}
			return new RandomVariable(time, newRealizations);
		}
	}

	@Override
	public RandomVariableInterface pow(double exponent) {
		if(isDeterministic()) {
			double newValueIfNonStochastic = Math.pow(valueIfNonStochastic,exponent);
			return new RandomVariable(time, newValueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = Math.pow(realizations[i],exponent);
			}
			return new RandomVariable(time, newRealizations);
		}
	}

	@Override
	public RandomVariableInterface average() {
		return new RandomVariable(getAverage());
	}

	@Override
	public RandomVariableInterface getConditionalExpectation(ConditionalExpectationEstimatorInterface conditionalExpectationOperator)
	{
		return conditionalExpectationOperator.getConditionalExpectation(this);
	}

	@Override
	public RandomVariableInterface squared() {
		if(isDeterministic()) {
			double newValueIfNonStochastic = valueIfNonStochastic * valueIfNonStochastic;
			return new RandomVariable(time, newValueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] * realizations[i];
			}
			return new RandomVariable(time, newRealizations);
		}
	}

	@Override
	public RandomVariableInterface sqrt() {
		if(isDeterministic()) {
			double newValueIfNonStochastic = Math.sqrt(valueIfNonStochastic);
			return new RandomVariable(time, newValueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = Math.sqrt(realizations[i]);
			}
			return new RandomVariable(time, newRealizations);
		}
	}

	@Override
	public RandomVariable exp() {
		if(isDeterministic()) {
			double newValueIfNonStochastic = FastMath.exp(valueIfNonStochastic);
			return new RandomVariable(time, newValueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = FastMath.exp(realizations[i]);
			}
			return new RandomVariable(time, newRealizations);
		}
	}

	@Override
	public RandomVariable log() {
		if(isDeterministic()) {
			double newValueIfNonStochastic = FastMath.log(valueIfNonStochastic);
			return new RandomVariable(time, newValueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = FastMath.log(realizations[i]);
			}
			return new RandomVariable(time, newRealizations);
		}
	}

	@Override
	public RandomVariableInterface sin() {
		if(isDeterministic()) {
			double newValueIfNonStochastic = FastMath.sin(valueIfNonStochastic);
			return new RandomVariable(time, newValueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = FastMath.sin(realizations[i]);
			}
			return new RandomVariable(time, newRealizations);
		}
	}

	@Override
	public RandomVariableInterface cos() {
		if(isDeterministic()) {
			double newValueIfNonStochastic = FastMath.cos(valueIfNonStochastic);
			return new RandomVariable(time, newValueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = FastMath.cos(realizations[i]);
			}
			return new RandomVariable(time, newRealizations);
		}
	}

	@Override
	public RandomVariableInterface add(RandomVariableInterface randomVariable) {
		// Set time of this random variable to maximum of time with respect to which measurability is known.
		double newTime = Math.max(time, randomVariable.getFiltrationTime());

		if(isDeterministic() && randomVariable.isDeterministic()) {
			double newValueIfNonStochastic = valueIfNonStochastic + randomVariable.get(0);
			return new RandomVariable(newTime, newValueIfNonStochastic);
		}
		else if(isDeterministic()) {
			return randomVariable.add(valueIfNonStochastic);
		} else {
			double[] newRealizations = new double[Math.max(size(), randomVariable.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] + randomVariable.get(i);
			}
			return new RandomVariable(newTime, newRealizations);
		}
	}

	@Override
	public RandomVariableInterface sub(RandomVariableInterface randomVariable) {
		// Set time of this random variable to maximum of time with respect to which measurability is known.
		double newTime = Math.max(time, randomVariable.getFiltrationTime());

		if(isDeterministic() && randomVariable.isDeterministic()) {
			double newValueIfNonStochastic = valueIfNonStochastic - randomVariable.get(0);
			return new RandomVariable(newTime, newValueIfNonStochastic);
		}
		else if(isDeterministic()) {
			double[] newRealizations = new double[Math.max(size(), randomVariable.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = valueIfNonStochastic - randomVariable.get(i);
			}
			return new RandomVariable(newTime, newRealizations);
		}
		else {
			double[] newRealizations = new double[Math.max(size(), randomVariable.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] - randomVariable.get(i);
			}
			return new RandomVariable(newTime, newRealizations);
		}
	}

	@Override
	public RandomVariableInterface mult(RandomVariableInterface randomVariable) {
		// Set time of this random variable to maximum of time with respect to which measurability is known.
		double newTime = Math.max(time, randomVariable.getFiltrationTime());

		if(isDeterministic() && randomVariable.isDeterministic()) {
			double newValueIfNonStochastic = valueIfNonStochastic * randomVariable.get(0);
			return new RandomVariable(newTime, newValueIfNonStochastic);
		}
		else if(isDeterministic()) {
			return randomVariable.mult(valueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[Math.max(size(), randomVariable.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] * randomVariable.get(i);
			}
			return new RandomVariable(newTime, newRealizations);
		}
	}

	@Override
	public RandomVariableInterface div(RandomVariableInterface randomVariable) {
		// Set time of this random variable to maximum of time with respect to which measurability is known.
		double newTime = Math.max(time, randomVariable.getFiltrationTime());

		if(isDeterministic() && randomVariable.isDeterministic()) {
			double newValueIfNonStochastic = valueIfNonStochastic / randomVariable.get(0);
			return new RandomVariable(newTime, newValueIfNonStochastic);
		}
		else if(isDeterministic()) {
			double[] newRealizations = new double[Math.max(size(), randomVariable.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = valueIfNonStochastic / randomVariable.get(i);
			}
			return new RandomVariable(newTime, newRealizations);
		}
		else {
			double[] newRealizations = new double[Math.max(size(), randomVariable.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] / randomVariable.get(i);
			}
			return new RandomVariable(newTime, newRealizations);
		}
	}

	@Override
	public RandomVariableInterface cap(RandomVariableInterface randomVariable) {
		// Set time of this random variable to maximum of time with respect to which measurability is known.
		double newTime = Math.max(time, randomVariable.getFiltrationTime());

		if(isDeterministic() && randomVariable.isDeterministic()) {
			double newValueIfNonStochastic = FastMath.min(valueIfNonStochastic, randomVariable.get(0));
			return new RandomVariable(newTime, newValueIfNonStochastic);
		}
		else if(isDeterministic()) {
			return randomVariable.cap(valueIfNonStochastic);
		} else {
			double[] newRealizations = new double[Math.max(size(), randomVariable.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = FastMath.min(realizations[i], randomVariable.get(i));
			}
			return new RandomVariable(newTime, newRealizations);
		}
	}

	@Override
	public RandomVariableInterface floor(RandomVariableInterface randomVariable) {
		// Set time of this random variable to maximum of time with respect to which measurability is known.
		double newTime = Math.max(time, randomVariable.getFiltrationTime());

		if(isDeterministic() && randomVariable.isDeterministic()) {
			double newValueIfNonStochastic = FastMath.max(valueIfNonStochastic, randomVariable.get(0));
			return new RandomVariable(newTime, newValueIfNonStochastic);
		}
		else if(isDeterministic()) {
			return randomVariable.floor(valueIfNonStochastic);
		} else {
			double[] newRealizations = new double[Math.max(size(), randomVariable.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = FastMath.max(realizations[i], randomVariable.get(i));
			}
			return new RandomVariable(newTime, newRealizations);
		}
	}

	@Override
	public RandomVariableInterface accrue(RandomVariableInterface rate, double periodLength) {
		// Set time of this random variable to maximum of time with respect to which measurability is known.
		double newTime = Math.max(time, rate.getFiltrationTime());

		if(isDeterministic() && rate.isDeterministic()) {
			double newValueIfNonStochastic = valueIfNonStochastic * (1 + rate.get(0) * periodLength);
			return new RandomVariable(newTime, newValueIfNonStochastic);
		}
		else if(isDeterministic() && !rate.isDeterministic()) {
			double[] rateRealizations = rate.getRealizations();
			double[] newRealizations = new double[Math.max(size(), rate.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = valueIfNonStochastic * (1 + rateRealizations[i] * periodLength);
			}
			return new RandomVariable(newTime, newRealizations);
		}
		else if(!isDeterministic() && rate.isDeterministic()) {
			double rateValue = rate.get(0);
			double[] newRealizations = new double[Math.max(size(), rate.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] * (1 + rateValue * periodLength);
			}
			return new RandomVariable(newTime, newRealizations);
		}
		else {
			double[] rateRealizations = rate.getRealizations();
			double[] newRealizations = new double[Math.max(size(), rate.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] * (1 + rateRealizations[i] * periodLength);
			}
			return new RandomVariable(newTime, newRealizations);
		}
	}

	@Override
	public RandomVariableInterface discount(RandomVariableInterface rate, double periodLength) {
		// Set time of this random variable to maximum of time with respect to which measurability is known.
		double newTime = Math.max(time, rate.getFiltrationTime());

		if(isDeterministic() && rate.isDeterministic()) {
			double newValueIfNonStochastic = valueIfNonStochastic / (1 + rate.get(0) * periodLength);
			return new RandomVariable(newTime, newValueIfNonStochastic);
		}
		else if(isDeterministic() && !rate.isDeterministic()) {
			double[] rateRealizations = rate.getRealizations();
			double[] newRealizations = new double[Math.max(size(), rate.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = valueIfNonStochastic / (1.0 + rateRealizations[i] * periodLength);
			}
			return new RandomVariable(newTime, newRealizations);
		}
		else if(!isDeterministic() && rate.isDeterministic()) {
			double rateValue = rate.get(0);
			double[] newRealizations = new double[Math.max(size(), rate.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] / (1.0 + rateValue * periodLength);
			}
			return new RandomVariable(newTime, newRealizations);
		}
		else {
			double[] rateRealizations = rate.getRealizations();
			double[] newRealizations = new double[Math.max(size(), rate.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] / (1.0 + rateRealizations[i] * periodLength);
			}
			return new RandomVariable(newTime, newRealizations);
		}
	}

	@Override
	public RandomVariableInterface barrier(RandomVariableInterface trigger, RandomVariableInterface valueIfTriggerNonNegative, RandomVariableInterface valueIfTriggerNegative) {
		// Set time of this random variable to maximum of time with respect to which measurability is known.
		double newTime = Math.max(time, trigger.getFiltrationTime());
		newTime = Math.max(newTime, valueIfTriggerNonNegative.getFiltrationTime());
		newTime = Math.max(newTime, valueIfTriggerNegative.getFiltrationTime());

		if(isDeterministic() && trigger.isDeterministic() && valueIfTriggerNonNegative.isDeterministic() && valueIfTriggerNegative.isDeterministic()) {
			double newValueIfNonStochastic = trigger.get(0) >= 0 ? valueIfTriggerNonNegative.get(0) : valueIfTriggerNegative.get(0);
			return new RandomVariable(newTime, newValueIfNonStochastic);
		}
		else {
			int numberOfPaths = Math.max(Math.max(trigger.size(), valueIfTriggerNonNegative.size()), valueIfTriggerNegative.size());
			double[] newRealizations = new double[numberOfPaths];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i] = trigger.get(i) >= 0.0 ? valueIfTriggerNonNegative.get(i) : valueIfTriggerNegative.get(i);
			}
			return new RandomVariable(newTime, newRealizations);
		}
	}

	@Override
	public RandomVariableInterface barrier(RandomVariableInterface trigger, RandomVariableInterface valueIfTriggerNonNegative, double valueIfTriggerNegative) {
		return this.barrier(trigger, valueIfTriggerNonNegative, new RandomVariable(valueIfTriggerNonNegative.getFiltrationTime(), valueIfTriggerNegative));
	}

	@Override
	public RandomVariableInterface invert() {
		if(isDeterministic()) {
			double newValueIfNonStochastic = 1.0/valueIfNonStochastic;
			return new RandomVariable(time, newValueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = 1.0/realizations[i];
			}
			return new RandomVariable(time, newRealizations);
		}
	}

	@Override
	public RandomVariableInterface abs() {
		if(isDeterministic()) {
			double newValueIfNonStochastic = Math.abs(valueIfNonStochastic);
			return new RandomVariable(time, newValueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = Math.abs(realizations[i]);
			}
			return new RandomVariable(time, newRealizations);
		}
	}

	@Override
	public RandomVariableInterface addProduct(RandomVariableInterface factor1, double factor2) {
		// Set time of this random variable to maximum of time with respect to which measurability is known.
		double newTime = Math.max(time, factor1.getFiltrationTime());

		if(isDeterministic() && factor1.isDeterministic()) {
			double newValueIfNonStochastic = valueIfNonStochastic + (factor1.get(0) * factor2);
			return new RandomVariable(newTime, newValueIfNonStochastic);
		}
		else if(isDeterministic() && !factor1.isDeterministic()) {
			double[] factor1Realizations = factor1.getRealizations();
			double[] newRealizations = new double[Math.max(size(), factor1.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = valueIfNonStochastic + factor1Realizations[i] * factor2;
			}
			return new RandomVariable(newTime, newRealizations);
		}
		else if(!isDeterministic() && factor1.isDeterministic()) {
			double factor1Value = factor1.get(0);
			double[] newRealizations = new double[Math.max(size(), factor1.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] + factor1Value * factor2;
			}
			return new RandomVariable(newTime, newRealizations);
		}
		else {
			double[] factor1Realizations = factor1.getRealizations();
			double[] newRealizations = new double[Math.max(size(), factor1.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] + factor1Realizations[i] * factor2;
			}
			return new RandomVariable(newTime, newRealizations);
		}
	}

	@Override
	public RandomVariableInterface addProduct(RandomVariableInterface factor1, RandomVariableInterface factor2) {
		// Set time of this random variable to maximum of time with respect to which measurability is known.
		double newTime = Math.max(Math.max(time, factor1.getFiltrationTime()), factor2.getFiltrationTime());

		if(isDeterministic() && factor1.isDeterministic() && factor2.isDeterministic()) {
			double newValueIfNonStochastic = valueIfNonStochastic + (factor1.get(0) * factor2.get(0));
			return new RandomVariable(newTime, newValueIfNonStochastic);
		}
		else if(isDeterministic() && !factor1.isDeterministic() && !factor2.isDeterministic()) {
			double[] factor1Realizations = factor1.getRealizations();
			double[] factor2Realizations = factor2.getRealizations();
			double[] newRealizations = new double[Math.max(size(), factor1.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = valueIfNonStochastic + factor1Realizations[i] * factor2Realizations[i];
			}
			return new RandomVariable(newTime, newRealizations);
		}
		else if(!isDeterministic() && !factor1.isDeterministic() && !factor2.isDeterministic()) {
			double[] factor1Realizations = factor1.getRealizations();
			double[] factor2Realizations = factor2.getRealizations();
			double[] newRealizations = new double[Math.max(size(), factor1.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] + factor1Realizations[i] * factor2Realizations[i];
			}
			return new RandomVariable(newTime, newRealizations);
		}
		else {
			double[] newRealizations = new double[Math.max(Math.max(size(), factor1.size()), factor2.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = get(i) + factor1.get(i) * factor2.get(i);
			}
			return new RandomVariable(newTime, newRealizations);
		}
	}

	@Override
	public RandomVariableInterface addSumProduct(List<RandomVariableInterface> factor1, List<RandomVariableInterface> factor2)
	{
		RandomVariableInterface result = this;
		for(int i=0; i<factor1.size(); i++) {
			result = result.addProduct(factor1.get(i), factor2.get(i));
		}
		return result;
	}

	@Override
	public RandomVariableInterface addRatio(RandomVariableInterface numerator, RandomVariableInterface denominator) {
		// Set time of this random variable to maximum of time with respect to which measurability is known.
		double newTime = Math.max(Math.max(time, numerator.getFiltrationTime()), denominator.getFiltrationTime());

		if(isDeterministic() && numerator.isDeterministic() && denominator.isDeterministic()) {
			double newValueIfNonStochastic = valueIfNonStochastic + (numerator.get(0) / denominator.get(0));
			return new RandomVariable(newTime, newValueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[Math.max(Math.max(size(), numerator.size()), denominator.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = get(i) + numerator.get(i) / denominator.get(i);
			}
			return new RandomVariable(newTime, newRealizations);
		}
	}

	@Override
	public RandomVariableInterface subRatio(RandomVariableInterface numerator, RandomVariableInterface denominator) {
		// Set time of this random variable to maximum of time with respect to which measurability is known.
		double newTime = Math.max(Math.max(time, numerator.getFiltrationTime()), denominator.getFiltrationTime());

		if(isDeterministic() && numerator.isDeterministic() && denominator.isDeterministic()) {
			double newValueIfNonStochastic = valueIfNonStochastic - (numerator.get(0) / denominator.get(0));
			return new RandomVariable(newTime, newValueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[Math.max(Math.max(size(), numerator.size()), denominator.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = get(i) - numerator.get(i) / denominator.get(i);
			}
			return new RandomVariable(newTime, newRealizations);
		}
	}

	@Override
	public RandomVariableInterface isNaN() {
		if(isDeterministic()) {
			return new RandomVariable(time, Double.isNaN(valueIfNonStochastic) ? 1.0 : 0.0);
		}
		else {
			double[] newRealizations = new double[size()];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = Double.isNaN(get(i)) ? 1.0 : 0.0;
			}
			return new RandomVariable(time, newRealizations);
		}
	}

	@Override
	public String toString() {
		return super.toString()
				+ "\n" + "time: " + time
				+ "\n" + "realizations: " +
				(isDeterministic() ? valueIfNonStochastic : Arrays.toString(realizations));
	}
}
