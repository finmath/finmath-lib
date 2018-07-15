/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2006
 */
package net.finmath.montecarlo;

import java.util.Arrays;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntToDoubleFunction;
import java.util.stream.DoubleStream;

import org.apache.commons.math3.util.FastMath;

import net.finmath.functions.DoubleTernaryOperator;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * The class RandomVariable represents a random variable being the evaluation of a stochastic process
 * at a certain time within a Monte-Carlo simulation.
 * It is thus essentially a vector of floating point numbers - the realizations - together with a double - the time.
 * The index of the vector represents path.
 * The class may also be used for non-stochastic quantities which may potentially be stochastic
 * (e.g. volatility). If only non-stochastic random variables are involved in an operation the class uses
 * optimized code.
 *
 * Accesses performed exclusively through the interface
 * <code>RandomVariableInterface</code> is thread safe (and does not mutate the class).
 *
 * This implementation uses floats for the realizations (consuming less memory compared to using doubles). However,
 * the calculation of the average is performed using double precision.
 *
 * @author Christian Fries
 * @version 1.8
 */
public class RandomVariableLowMemory implements RandomVariableInterface {

	private static final long serialVersionUID = 7620120320663270600L;

	private final double      time;	                // Time (filtration)

	// Data model for the stochastic case (otherwise null)
	private final float[]    realizations;           // Realizations

	// Data model for the non-stochastic case (if realizations==null)
	private final double      valueIfNonStochastic;

	/**
	 * Create a random variable from a given other implementation of <code>RandomVariableInterface</code>.
	 *
	 * @param value Object implementing <code>RandomVariableInterface</code>.
	 */
	public RandomVariableLowMemory(RandomVariableInterface value) {
		super();
		this.time = value.getFiltrationTime();
		this.realizations = value.isDeterministic() ? null : getFloatArray(value.getRealizations());
		this.valueIfNonStochastic = value.isDeterministic() ? value.get(0) : Double.NaN;
	}

	/**
	 * Create a non stochastic random variable, i.e. a constant.
	 *
	 * @param value the value, a constant.
	 */
	public RandomVariableLowMemory(double value) {
		this(-Double.MAX_VALUE, value);
	}

	/**
	 * Create a non stochastic random variable, i.e. a constant.
	 *
	 * @param time the filtration time, set to 0.0 if not used.
	 * @param value the value, a constant.
	 */
	public RandomVariableLowMemory(double time, double value) {
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
	public RandomVariableLowMemory(double time, int numberOfPath, double value) {
		super();
		this.time = time;
		this.realizations = new float[numberOfPath];
		java.util.Arrays.fill(this.realizations, (float)value);
		this.valueIfNonStochastic = Double.NaN;
	}

	/**
	 * Create a stochastic random variable.
	 *
	 * @param time the filtration time, set to 0.0 if not used.
	 * @param realisations the vector of realizations.
	 */
	public RandomVariableLowMemory(double time, float[] realisations) {
		super();
		this.time = time;
		this.realizations = realisations;
		this.valueIfNonStochastic = Double.NaN;
	}

	/**
	 * Create a stochastic random variable.
	 *
	 * @param time the filtration time, set to 0.0 if not used.
	 * @param realisations the vector of realizations.
	 */
	public RandomVariableLowMemory(double time, double[] realisations) {
		super();
		this.time = time;
		this.realizations = getFloatArray(realisations);
		this.valueIfNonStochastic = Double.NaN;
	}

	private float[] getFloatArray(double[] arrayOfDouble) {
		float[] arrayOfFloat = new float[arrayOfDouble.length];
		for(int i=0; i<arrayOfDouble.length; i++) {
			arrayOfFloat[i] = (float)arrayOfDouble[i];
		}
		return arrayOfFloat;
	}

	private double[] getDoubleArray(float[] arrayOfFloat) {
		double[] arrayOfDouble = new double[arrayOfFloat.length];
		for(int i=0; i<arrayOfFloat.length; i++) {
			arrayOfDouble[i] = arrayOfFloat[i];
		}
		return arrayOfDouble;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getMutableCopy()
	 */
	public RandomVariableLowMemory getMutableCopy() {
		return this;

		//if(isDeterministic())	return new RandomVariable(time, valueIfNonStochastic);
		//else					return new RandomVariable(time, realizations.clone());
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#equals(net.finmath.montecarlo.RandomVariable)
	 */
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

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getFiltrationTime()
	 */
	@Override
	public double getFiltrationTime() {
		return time;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#get(int)
	 */
	@Override
	public double get(int pathOrState) {
		if(isDeterministic()) {
			return valueIfNonStochastic;
		} else {
			return realizations[pathOrState];
		}
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#size()
	 */
	@Override
	public int size() {
		if(isDeterministic()) {
			return 1;
		} else {
			return realizations.length;
		}
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getMin()
	 */
	@Override
	public double getMin() {
		if(isDeterministic()) {
			return valueIfNonStochastic;
		}
		double min = Double.MAX_VALUE;
		if(realizations.length != 0) {
			min = realizations[0];     /// @see getMax()
		}
		for(int i=0; i<realizations.length; i++) {
			min = Math.min(realizations[i],min);
		}
		return min;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getMax()
	 */
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

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getAverage()
	 */
	public double getAverage() {
		if(isDeterministic()) {
			return valueIfNonStochastic;
		}
		if(size() == 0) {
			return Double.NaN;
		}

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

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getAverage(net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public double getAverage(RandomVariableInterface probabilities) {
		if(isDeterministic()) {
			return valueIfNonStochastic;
		}
		if(size() == 0) {
			return Double.NaN;
		}

		double sum = 0.0;
		double error = 0.0;														// Running error compensation
		for(int i=0; i<realizations.length; i++)  {
			double value = (realizations[i]) * probabilities.get(i) - error;		// Error corrected value
			double newSum = sum + value;				// New sum
			error = (newSum - sum) - value;				// New numerical error
			sum	= newSum;
		}
		return sum / realizations.length;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getVariance()
	 */
	@Override
	public double getVariance() {
		if(isDeterministic()) {
			return 0.0;
		}
		if(size() == 0) {
			return Double.NaN;
		}

		double sum			= 0.0;
		double sumOfSquared = 0.0;
		double errorOfSum			= 0.0;
		double errorOfSumSquared	= 0.0;
		for(int i=0; i<realizations.length; i++) {
			double value	= realizations[i] - errorOfSum;
			double newSum	= sum + value;
			errorOfSum		= (newSum - sum) - value;
			sum				= newSum;

			double valueSquared		= realizations[i] * realizations[i] - errorOfSumSquared;
			double newSumOfSquared	= sumOfSquared + valueSquared;
			errorOfSumSquared		= (newSumOfSquared-sumOfSquared) - valueSquared;
			sumOfSquared			= newSumOfSquared;
		}
		return (sumOfSquared/realizations.length - sum/realizations.length*sum/realizations.length);
	}

	@Override
	public double getVariance(RandomVariableInterface probabilities) {
		if(isDeterministic()) {
			return 0.0;
		}
		if(size() == 0) {
			return Double.NaN;
		}

		double sum			= 0.0;
		double sumOfSquared = 0.0;
		double errorOfSum			= 0.0;
		double errorOfSumSquared	= 0.0;
		for(int i=0; i<realizations.length; i++) {
			double value	= realizations[i] * probabilities.get(i) - errorOfSum;
			double newSum	= sum + value;
			errorOfSum		= (newSum - sum) - value;
			sum				= newSum;

			double valueSquared		= realizations[i] * realizations[i] * probabilities.get(i) - errorOfSumSquared;
			double newSumOfSquared	= sumOfSquared + valueSquared;
			errorOfSumSquared		= (newSumOfSquared-sumOfSquared) - valueSquared;
			sumOfSquared			= newSumOfSquared;
		}
		return (sumOfSquared - sum*sum)/realizations.length;
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

		float[] realizationsSorted = realizations.clone();
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

		float[] realizationsSorted = realizations.clone();
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
			float[] realizationsSorted = realizations.clone();
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
			return new RandomVariableLowMemory(time,clone);

		}

		return new RandomVariableLowMemory(time,realizations.clone());
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
			return Arrays.stream(getDoubleArray(realizations));
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

	@Override
	public double[] getRealizations() {
		if(isDeterministic()) {
			double[] result = new double[1];
			result[0] = get(0);
			return result;
		}
		else {
			return getDoubleArray(realizations);
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

	public RandomVariableInterface cap(double cap) {
		if(isDeterministic()) {
			double newValueIfNonStochastic = Math.min(valueIfNonStochastic,cap);
			return new RandomVariableLowMemory(time, newValueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = Math.min(realizations[i],cap);
			}
			return new RandomVariableLowMemory(time, newRealizations);
		}
	}

	@Override
	public RandomVariableInterface floor(double floor) {
		if(isDeterministic()) {
			double newValueIfNonStochastic = Math.max(valueIfNonStochastic,floor);
			return new RandomVariableLowMemory(time, newValueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = Math.max(realizations[i],floor);
			}
			return new RandomVariableLowMemory(time, newRealizations);
		}
	}

	@Override
	public RandomVariableInterface add(double value) {
		if(isDeterministic()) {
			double newValueIfNonStochastic = valueIfNonStochastic + value;
			return new RandomVariableLowMemory(time, newValueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] + value;
			}
			return new RandomVariableLowMemory(time, newRealizations);
		}
	}

	@Override
	public RandomVariableInterface sub(double value) {
		if(isDeterministic()) {
			double newValueIfNonStochastic = valueIfNonStochastic - value;
			return new RandomVariableLowMemory(time, newValueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] - value;
			}
			return new RandomVariableLowMemory(time, newRealizations);
		}
	}

	@Override
	public RandomVariableInterface mult(double value) {
		if(isDeterministic()) {
			double newValueIfNonStochastic = valueIfNonStochastic * value;
			return new RandomVariableLowMemory(time, newValueIfNonStochastic);
		}
		else {
			float[] newRealizations = new float[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = (float) (realizations[i] * value);
			}
			return new RandomVariableLowMemory(time, newRealizations);
		}
	}

	@Override
	public RandomVariableInterface div(double value) {
		if(isDeterministic()) {
			double newValueIfNonStochastic = valueIfNonStochastic / value;
			return new RandomVariableLowMemory(time, newValueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] / value;
			}
			return new RandomVariableLowMemory(time, newRealizations);
		}
	}

	@Override
	public RandomVariableInterface pow(double exponent) {
		if(isDeterministic()) {
			double newValueIfNonStochastic = Math.pow(valueIfNonStochastic,exponent);
			return new RandomVariableLowMemory(time, newValueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = Math.pow(realizations[i],exponent);
			}
			return new RandomVariableLowMemory(time, newRealizations);
		}
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#average()
	 */
	@Override
	public RandomVariableInterface average() {
		return new RandomVariableLowMemory(getAverage());
	}

	@Override
	public RandomVariableInterface squared() {
		if(isDeterministic()) {
			double newValueIfNonStochastic = valueIfNonStochastic * valueIfNonStochastic;
			return new RandomVariableLowMemory(time, newValueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] * realizations[i];
			}
			return new RandomVariableLowMemory(time, newRealizations);
		}
	}

	@Override
	public RandomVariableInterface sqrt() {
		if(isDeterministic()) {
			double newValueIfNonStochastic = Math.sqrt(valueIfNonStochastic);
			return new RandomVariableLowMemory(time, newValueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = Math.sqrt(realizations[i]);
			}
			return new RandomVariableLowMemory(time, newRealizations);
		}
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#exp()
	 */
	public RandomVariableLowMemory exp() {
		if(isDeterministic()) {
			double newValueIfNonStochastic = FastMath.exp(valueIfNonStochastic);
			return new RandomVariableLowMemory(time, newValueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = FastMath.exp(realizations[i]);
			}
			return new RandomVariableLowMemory(time, newRealizations);
		}
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#log()
	 */
	public RandomVariableLowMemory log() {
		if(isDeterministic()) {
			double newValueIfNonStochastic = FastMath.log(valueIfNonStochastic);
			return new RandomVariableLowMemory(time, newValueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = FastMath.log(realizations[i]);
			}
			return new RandomVariableLowMemory(time, newRealizations);
		}
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#sin()
	 */
	public RandomVariableInterface sin() {
		if(isDeterministic()) {
			double newValueIfNonStochastic = FastMath.sin(valueIfNonStochastic);
			return new RandomVariableLowMemory(time, newValueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = FastMath.sin(realizations[i]);
			}
			return new RandomVariableLowMemory(time, newRealizations);
		}
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#cos()
	 */
	public RandomVariableInterface cos() {
		if(isDeterministic()) {
			double newValueIfNonStochastic = FastMath.cos(valueIfNonStochastic);
			return new RandomVariableLowMemory(time, newValueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = FastMath.cos(realizations[i]);
			}
			return new RandomVariableLowMemory(time, newRealizations);
		}
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#add(net.finmath.stochastic.RandomVariableInterface)
	 */
	public RandomVariableInterface add(RandomVariableInterface randomVariable) {
		// Set time of this random variable to maximum of time with respect to which measurability is known.
		double newTime = Math.max(time, randomVariable.getFiltrationTime());

		if(isDeterministic() && randomVariable.isDeterministic()) {
			double newValueIfNonStochastic = valueIfNonStochastic + randomVariable.get(0);
			return new RandomVariableLowMemory(newTime, newValueIfNonStochastic);
		}
		else if(isDeterministic()) {
			return randomVariable.add(this);
		} else {
			double[] newRealizations = new double[Math.max(size(), randomVariable.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] + randomVariable.get(i);
			}
			return new RandomVariableLowMemory(newTime, newRealizations);
		}
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#sub(net.finmath.stochastic.RandomVariableInterface)
	 */
	public RandomVariableInterface sub(RandomVariableInterface randomVariable) {
		// Set time of this random variable to maximum of time with respect to which measurability is known.
		double newTime = Math.max(time, randomVariable.getFiltrationTime());

		if(isDeterministic() && randomVariable.isDeterministic()) {
			double newValueIfNonStochastic = valueIfNonStochastic - randomVariable.get(0);
			return new RandomVariableLowMemory(newTime, newValueIfNonStochastic);
		}
		else if(isDeterministic()) {
			double[] newRealizations = new double[Math.max(size(), randomVariable.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = valueIfNonStochastic - randomVariable.get(i);
			}
			return new RandomVariableLowMemory(newTime, newRealizations);
		}
		else {
			double[] newRealizations = new double[Math.max(size(), randomVariable.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] - randomVariable.get(i);
			}
			return new RandomVariableLowMemory(newTime, newRealizations);
		}
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#mult(net.finmath.stochastic.RandomVariableInterface)
	 */
	public RandomVariableInterface mult(RandomVariableInterface randomVariable) {
		// Set time of this random variable to maximum of time with respect to which measurability is known.
		double newTime = Math.max(time, randomVariable.getFiltrationTime());

		if(isDeterministic() && randomVariable.isDeterministic()) {
			double newValueIfNonStochastic = valueIfNonStochastic * randomVariable.get(0);
			return new RandomVariableLowMemory(newTime, newValueIfNonStochastic);
		}
		else if(isDeterministic()) {
			float[] newRealizations = new float[Math.max(size(), randomVariable.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = (float) (valueIfNonStochastic * randomVariable.get(i));
			}
			return new RandomVariableLowMemory(newTime, newRealizations);
		}
		else {
			float[] newRealizations = new float[Math.max(size(), randomVariable.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = (float) (realizations[i] * randomVariable.get(i));
			}
			return new RandomVariableLowMemory(newTime, newRealizations);
		}
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#div(net.finmath.stochastic.RandomVariableInterface)
	 */
	public RandomVariableInterface div(RandomVariableInterface randomVariable) {
		// Set time of this random variable to maximum of time with respect to which measurability is known.
		double newTime = Math.max(time, randomVariable.getFiltrationTime());

		if(isDeterministic() && randomVariable.isDeterministic()) {
			double newValueIfNonStochastic = valueIfNonStochastic / randomVariable.get(0);
			return new RandomVariableLowMemory(newTime, newValueIfNonStochastic);
		}
		else if(isDeterministic()) {
			double[] newRealizations = new double[Math.max(size(), randomVariable.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = valueIfNonStochastic / randomVariable.get(i);
			}
			return new RandomVariableLowMemory(newTime, newRealizations);
		}
		else {
			double[] newRealizations = new double[Math.max(size(), randomVariable.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] / randomVariable.get(i);
			}
			return new RandomVariableLowMemory(newTime, newRealizations);
		}
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#cap(net.finmath.stochastic.RandomVariableInterface)
	 */
	public RandomVariableInterface cap(RandomVariableInterface randomVariable) {
		// Set time of this random variable to maximum of time with respect to which measurability is known.
		double newTime = Math.max(time, randomVariable.getFiltrationTime());

		if(isDeterministic() && randomVariable.isDeterministic()) {
			double newValueIfNonStochastic = FastMath.min(valueIfNonStochastic, randomVariable.get(0));
			return new RandomVariableLowMemory(newTime, newValueIfNonStochastic);
		}
		else if(isDeterministic()) {
			return randomVariable.cap(this);
		} else {
			double[] newRealizations = new double[Math.max(size(), randomVariable.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = FastMath.min(realizations[i], randomVariable.get(i));
			}
			return new RandomVariableLowMemory(newTime, newRealizations);
		}
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#floor(net.finmath.stochastic.RandomVariableInterface)
	 */
	public RandomVariableInterface floor(RandomVariableInterface randomVariable) {
		// Set time of this random variable to maximum of time with respect to which measurability is known.
		double newTime = Math.max(time, randomVariable.getFiltrationTime());

		if(isDeterministic() && randomVariable.isDeterministic()) {
			double newValueIfNonStochastic = FastMath.max(valueIfNonStochastic, randomVariable.get(0));
			return new RandomVariableLowMemory(newTime, newValueIfNonStochastic);
		}
		else if(isDeterministic()) {
			return randomVariable.floor(this);
		} else {
			double[] newRealizations = new double[Math.max(size(), randomVariable.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = FastMath.max(realizations[i], randomVariable.get(i));
			}
			return new RandomVariableLowMemory(newTime, newRealizations);
		}
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#accrue(net.finmath.stochastic.RandomVariableInterface, double)
	 */
	public RandomVariableInterface accrue(RandomVariableInterface rate, double periodLength) {
		// Set time of this random variable to maximum of time with respect to which measurability is known.
		double newTime = Math.max(time, rate.getFiltrationTime());

		if(isDeterministic() && rate.isDeterministic()) {
			double newValueIfNonStochastic = valueIfNonStochastic * (1 + rate.get(0) * periodLength);
			return new RandomVariableLowMemory(newTime, newValueIfNonStochastic);
		}
		else if(isDeterministic() && !rate.isDeterministic()) {
			double[] rateRealizations = rate.getRealizations();
			double[] newRealizations = new double[Math.max(size(), rate.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = valueIfNonStochastic * (1 + rateRealizations[i] * periodLength);
			}
			return new RandomVariableLowMemory(newTime, newRealizations);
		}
		else if(!isDeterministic() && rate.isDeterministic()) {
			double rateValue = rate.get(0);
			double[] newRealizations = new double[Math.max(size(), rate.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] * (1 + rateValue * periodLength);
			}
			return new RandomVariableLowMemory(newTime, newRealizations);
		}
		else {
			double[] rateRealizations = rate.getRealizations();
			double[] newRealizations = new double[Math.max(size(), rate.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] * (1 + rateRealizations[i] * periodLength);
			}
			return new RandomVariableLowMemory(newTime, newRealizations);
		}
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#discount(net.finmath.stochastic.RandomVariableInterface, double)
	 */
	public RandomVariableInterface discount(RandomVariableInterface rate, double periodLength) {
		// Set time of this random variable to maximum of time with respect to which measurability is known.
		double newTime = Math.max(time, rate.getFiltrationTime());

		if(isDeterministic() && rate.isDeterministic()) {
			double newValueIfNonStochastic = valueIfNonStochastic / (1 + rate.get(0) * periodLength);
			return new RandomVariableLowMemory(newTime, newValueIfNonStochastic);
		}
		else if(isDeterministic() && !rate.isDeterministic()) {
			double[] rateRealizations = rate.getRealizations();
			double[] newRealizations = new double[Math.max(size(), rate.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = valueIfNonStochastic / (1.0 + rateRealizations[i] * periodLength);
			}
			return new RandomVariableLowMemory(newTime, newRealizations);
		}
		else if(!isDeterministic() && rate.isDeterministic()) {
			double rateValue = rate.get(0);
			double[] newRealizations = new double[Math.max(size(), rate.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] / (1.0 + rateValue * periodLength);
			}
			return new RandomVariableLowMemory(newTime, newRealizations);
		}
		else {
			double[] rateRealizations = rate.getRealizations();
			double[] newRealizations = new double[Math.max(size(), rate.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] / (1.0 + rateRealizations[i] * periodLength);
			}
			return new RandomVariableLowMemory(newTime, newRealizations);
		}
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#barrier(net.finmath.stochastic.RandomVariableInterface, net.finmath.stochastic.RandomVariableInterface, net.finmath.stochastic.RandomVariableInterface)
	 */
	public RandomVariableInterface barrier(RandomVariableInterface trigger, RandomVariableInterface valueIfTriggerNonNegative, RandomVariableInterface valueIfTriggerNegative) {
		// Set time of this random variable to maximum of time with respect to which measurability is known.
		double newTime = Math.max(time, trigger.getFiltrationTime());
		newTime = Math.max(newTime, valueIfTriggerNonNegative.getFiltrationTime());
		newTime = Math.max(newTime, valueIfTriggerNegative.getFiltrationTime());

		if(isDeterministic() && trigger.isDeterministic() && valueIfTriggerNonNegative.isDeterministic() && valueIfTriggerNegative.isDeterministic()) {
			double newValueIfNonStochastic = trigger.get(0) >= 0 ? valueIfTriggerNonNegative.get(0) : valueIfTriggerNegative.get(0);
			return new RandomVariableLowMemory(newTime, newValueIfNonStochastic);
		}
		else {
			int numberOfPaths = Math.max(Math.max(trigger.size(), valueIfTriggerNonNegative.size()), valueIfTriggerNegative.size());
			double[] newRealizations = new double[numberOfPaths];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i] = trigger.get(i) >= 0.0 ? valueIfTriggerNonNegative.get(i) : valueIfTriggerNegative.get(i);
			}
			return new RandomVariableLowMemory(newTime, newRealizations);
		}
	}

	public RandomVariableInterface barrier(RandomVariableInterface trigger, RandomVariableInterface valueIfTriggerNonNegative, double valueIfTriggerNegative) {
		return this.barrier(trigger, valueIfTriggerNonNegative, new RandomVariableLowMemory(valueIfTriggerNonNegative.getFiltrationTime(), valueIfTriggerNegative));
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#invert()
	 */
	public RandomVariableInterface invert() {
		if(isDeterministic()) {
			double newValueIfNonStochastic = 1.0/valueIfNonStochastic;
			return new RandomVariableLowMemory(time, newValueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = 1.0/realizations[i];
			}
			return new RandomVariableLowMemory(time, newRealizations);
		}
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#abs()
	 */
	public RandomVariableInterface abs() {
		if(isDeterministic()) {
			double newValueIfNonStochastic = Math.abs(valueIfNonStochastic);
			return new RandomVariableLowMemory(time, newValueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = Math.abs(realizations[i]);
			}
			return new RandomVariableLowMemory(time, newRealizations);
		}
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#addProduct(net.finmath.stochastic.RandomVariableInterface, double)
	 */
	public RandomVariableInterface addProduct(RandomVariableInterface factor1, double factor2) {
		// Set time of this random variable to maximum of time with respect to which measurability is known.
		double newTime = Math.max(time, factor1.getFiltrationTime());

		if(isDeterministic() && factor1.isDeterministic()) {
			double newValueIfNonStochastic = valueIfNonStochastic + (factor1.get(0) * factor2);
			return new RandomVariableLowMemory(newTime, newValueIfNonStochastic);
		}
		else if(isDeterministic() && !factor1.isDeterministic()) {
			double[] factor1Realizations = factor1.getRealizations();
			double[] newRealizations = new double[Math.max(size(), factor1.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = valueIfNonStochastic + factor1Realizations[i] * factor2;
			}
			return new RandomVariableLowMemory(newTime, newRealizations);
		}
		else if(!isDeterministic() && factor1.isDeterministic()) {
			double factor1Value = factor1.get(0);
			double[] newRealizations = new double[Math.max(size(), factor1.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] + factor1Value * factor2;
			}
			return new RandomVariableLowMemory(newTime, newRealizations);
		}
		else {
			double[] factor1Realizations = factor1.getRealizations();
			double[] newRealizations = new double[Math.max(size(), factor1.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] + factor1Realizations[i] * factor2;
			}
			return new RandomVariableLowMemory(newTime, newRealizations);
		}
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#addProduct(net.finmath.stochastic.RandomVariableInterface, net.finmath.stochastic.RandomVariableInterface)
	 */
	public RandomVariableInterface addProduct(RandomVariableInterface factor1, RandomVariableInterface factor2) {
		// Set time of this random variable to maximum of time with respect to which measurability is known.
		double newTime = Math.max(Math.max(time, factor1.getFiltrationTime()), factor2.getFiltrationTime());

		if(isDeterministic() && factor1.isDeterministic() && factor2.isDeterministic()) {
			double newValueIfNonStochastic = valueIfNonStochastic + (factor1.get(0) * factor2.get(0));
			return new RandomVariableLowMemory(newTime, newValueIfNonStochastic);
		}
		else if(isDeterministic() && !factor1.isDeterministic() && !factor2.isDeterministic()) {
			double[] factor1Realizations = factor1.getRealizations();
			double[] factor2Realizations = factor2.getRealizations();
			double[] newRealizations = new double[Math.max(size(), factor1.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = valueIfNonStochastic + factor1Realizations[i] * factor2Realizations[i];
			}
			return new RandomVariableLowMemory(newTime, newRealizations);
		}
		else if(!isDeterministic() && !factor1.isDeterministic() && !factor2.isDeterministic()) {
			double[] factor1Realizations = factor1.getRealizations();
			double[] factor2Realizations = factor2.getRealizations();
			double[] newRealizations = new double[Math.max(size(), factor1.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] + factor1Realizations[i] * factor2Realizations[i];
			}
			return new RandomVariableLowMemory(newTime, newRealizations);
		}
		else {
			double[] newRealizations = new double[Math.max(Math.max(size(), factor1.size()), factor2.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = get(i) + factor1.get(i) * factor2.get(i);
			}
			return new RandomVariableLowMemory(newTime, newRealizations);
		}
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#addRatio(net.finmath.stochastic.RandomVariableInterface, net.finmath.stochastic.RandomVariableInterface)
	 */
	public RandomVariableInterface addRatio(RandomVariableInterface numerator, RandomVariableInterface denominator) {
		// Set time of this random variable to maximum of time with respect to which measurability is known.
		double newTime = Math.max(Math.max(time, numerator.getFiltrationTime()), denominator.getFiltrationTime());

		if(isDeterministic() && numerator.isDeterministic() && denominator.isDeterministic()) {
			double newValueIfNonStochastic = valueIfNonStochastic + (numerator.get(0) / denominator.get(0));
			return new RandomVariableLowMemory(newTime, newValueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[Math.max(Math.max(size(), numerator.size()), denominator.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = get(i) + numerator.get(i) / denominator.get(i);
			}
			return new RandomVariableLowMemory(newTime, newRealizations);
		}
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#subRatio(net.finmath.stochastic.RandomVariableInterface, net.finmath.stochastic.RandomVariableInterface)
	 */
	public RandomVariableInterface subRatio(RandomVariableInterface numerator, RandomVariableInterface denominator) {
		// Set time of this random variable to maximum of time with respect to which measurability is known.
		double newTime = Math.max(Math.max(time, numerator.getFiltrationTime()), denominator.getFiltrationTime());

		if(isDeterministic() && numerator.isDeterministic() && denominator.isDeterministic()) {
			double newValueIfNonStochastic = valueIfNonStochastic - (numerator.get(0) / denominator.get(0));
			return new RandomVariableLowMemory(newTime, newValueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[Math.max(Math.max(size(), numerator.size()), denominator.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = get(i) - numerator.get(i) / denominator.get(i);
			}
			return new RandomVariableLowMemory(newTime, newRealizations);
		}
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#isNaN()
	 */
	@Override
	public RandomVariableInterface isNaN() {
		if(isDeterministic()) {
			return new RandomVariableLowMemory(time, Double.isNaN(valueIfNonStochastic) ? 1.0f : 0.0f);
		}
		else {
			float[] newRealizations = new float[size()];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = Double.isNaN(get(i)) ? 1.0f : 0.0f;
			}
			return new RandomVariableLowMemory(time, newRealizations);
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return super.toString()
				+ "\n" + "time: " + time
				+ "\n" + "realizations: " + Arrays.toString(realizations);
	}
}
