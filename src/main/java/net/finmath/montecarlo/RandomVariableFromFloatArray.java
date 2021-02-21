/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2006
 */
package net.finmath.montecarlo;

import java.util.Arrays;
import java.util.List;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntToDoubleFunction;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import net.finmath.functions.DoubleTernaryOperator;
import net.finmath.stochastic.ConditionalExpectationEstimator;
import net.finmath.stochastic.RandomVariable;

/**
 * The class RandomVariableFromFloatArray represents a random variable being the evaluation of a stochastic process
 * at a certain time within a Monte-Carlo simulation.
 * It is thus essentially a vector of floating point numbers - the realizations - together with a double - the time.
 * The index of the vector represents path.
 * The class may also be used for non-stochastic quantities which may potentially be stochastic
 * (e.g. volatility). If only non-stochastic random variables are involved in an operation the class uses
 * optimized code.
 *
 * Accesses performed exclusively through the interface
 * <code>RandomVariable</code> is thread safe (and does not mutate the class).
 *
 * This implementation uses floats for the realizations (consuming less memory compared to using doubles). However,
 * the calculation of the average is performed using double precision.
 *
 * @author Christian Fries
 * @version 1.8
 */
public class RandomVariableFromFloatArray implements RandomVariable {

	private static final long serialVersionUID = -1352953450936857742L;

	private static final int typePriorityDefault = 1;

	private final int typePriority;

	private final double      time;	                // Time (filtration)

	// Data model for the stochastic case (otherwise null)
	private final float[]    realizations;           // Realizations

	// Data model for the non-stochastic case (if realizations==null)
	private final double      valueIfNonStochastic;

	/**
	 * Create a random variable from a given other implementation of <code>RandomVariable</code>.
	 *
	 * @param value Object implementing <code>RandomVariable</code>.
	 */
	public RandomVariableFromFloatArray(final RandomVariable value) {
		super();
		time = value.getFiltrationTime();
		realizations = value.isDeterministic() ? null : getFloatArray(value.getRealizations());
		valueIfNonStochastic = value.isDeterministic() ? value.doubleValue() : Double.NaN;
		typePriority = typePriorityDefault;
	}

	/**
	 * Create a non stochastic random variable, i.e. a constant.
	 *
	 * @param value the value, a constant.
	 */
	public RandomVariableFromFloatArray(final double value) {
		this(Double.NEGATIVE_INFINITY, value, typePriorityDefault);
	}

	/**
	 * Create a random variable by applying a function to a given other implementation of <code>RandomVariable</code>.
	 *
	 * @param value Object implementing <code>RandomVariable</code>.
	 * @param function A function mapping double to double.
	 */
	public RandomVariableFromFloatArray(final RandomVariable value, final DoubleUnaryOperator function) {
		super();
		time = value.getFiltrationTime();
		realizations = value.isDeterministic() ? null : getFloatArray(value.getRealizationsStream().map(function).toArray());
		valueIfNonStochastic = value.isDeterministic() ? function.applyAsDouble(value.get(0)) : Double.NaN;
		typePriority = typePriorityDefault;
	}


	/**
	 * Create a non stochastic random variable, i.e. a constant.
	 *
	 * @param time the filtration time, set to 0.0 if not used.
	 * @param value the value, a constant.
	 * @param typePriority The priority of this type in construction of result types. See "operator type priority" for details.
	 */
	public RandomVariableFromFloatArray(final double time, final double value, final int typePriority) {
		super();
		this.time = time;
		realizations = null;
		valueIfNonStochastic = value;
		this.typePriority = typePriority;
	}

	/**
	 * Create a non stochastic random variable, i.e. a constant.
	 *
	 * @param time the filtration time, set to 0.0 if not used.
	 * @param value the value, a constant.
	 */
	public RandomVariableFromFloatArray(final double time, final double value) {
		this(time, value, typePriorityDefault);
	}

	/**
	 * Create a non stochastic random variable, i.e. a constant.
	 *
	 * @param time the filtration time, set to 0.0 if not used.
	 * @param newRealizations the value, a constant.
	 */
	public RandomVariableFromFloatArray(final double time, final float[] newRealizations) {
		this(time, newRealizations, typePriorityDefault);
	}

	/**
	 * Create a non stochastic random variable, i.e. a constant.
	 *
	 * @param time the filtration time, set to 0.0 if not used.
	 * @param numberOfPath The number of paths.
	 * @param value the value, a constant.
	 */
	@Deprecated
	public RandomVariableFromFloatArray(final double time, final int numberOfPath, final double value) {
		super();
		this.time = time;
		realizations = new float[numberOfPath];
		java.util.Arrays.fill(realizations, (float)value);
		valueIfNonStochastic = Double.NaN;
		typePriority = typePriorityDefault;
	}

	/**
	 * Create a stochastic random variable.
	 *
	 * Important: The realizations array is not cloned (no defensive copy is made).
	 *
	 * @TODO A future version should perform a defensive copy.
	 *
	 * @param time the filtration time, set to 0.0 if not used.
	 * @param realisations the vector of realizations.
	 * @param typePriority The priority of this type in construction of result types. See "operator type priority" for details.
	 */
	public RandomVariableFromFloatArray(final double time, final float[] realisations, final int typePriority) {
		super();
		this.time = time;
		realizations = realisations;
		valueIfNonStochastic = Double.NaN;
		this.typePriority = typePriority;
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
	public RandomVariableFromFloatArray(final double time, final double[] realisations) {
		this(time, getFloatArray(realisations), typePriorityDefault);
	}

	/**
	 * Create a stochastic random variable.
	 *
	 * @param time the filtration time, set to 0.0 if not used.
	 * @param realizations A map mapping integer (path or state) to double, representing this random variable.
	 * @param size The size, i.e., number of paths.
	 * @param typePriority The priority of this type in construction of result types. See "operator type priority" for details.
	 */
	public RandomVariableFromFloatArray(final double time, final IntToDoubleFunction realizations, final int size, final int typePriority) {
		super();
		this.time = time;
		this.realizations = size == 1 ? null : new float[size];//IntStream.range(0,size).parallel().mapToDouble(realisations).toArray();
		valueIfNonStochastic = size == 1 ? realizations.applyAsDouble(0) : Double.NaN;
		if(size > 1) {
			IntStream.range(0,size).parallel().forEach(new IntConsumer() {
				@Override
				public void accept(final int i) {
					RandomVariableFromFloatArray.this.realizations[i] = (float) realizations.applyAsDouble(i);
				}
			}
					);
		}
		this.typePriority = typePriority;
	}

	/**
	 * Create a stochastic random variable.
	 *
	 * @param time the filtration time, set to 0.0 if not used.
	 * @param realizations A map mapping integer (path or state) to double, representing this random variable.
	 * @param size The size, i.e., number of paths.
	 */
	public RandomVariableFromFloatArray(final double time, final IntToDoubleFunction realizations, final int size) {
		this(time, realizations, size, typePriorityDefault);
	}

	private static float[] getFloatArray(final double[] arrayOfDouble) {
		final float[] arrayOfFloat = new float[arrayOfDouble.length];
		for(int i=0; i<arrayOfDouble.length; i++) {
			arrayOfFloat[i] = (float)arrayOfDouble[i];
		}
		return arrayOfFloat;
	}

	private double[] getDoubleArray(final float[] arrayOfFloat) {
		final double[] arrayOfDouble = new double[arrayOfFloat.length];
		for(int i=0; i<arrayOfFloat.length; i++) {
			arrayOfDouble[i] = arrayOfFloat[i];
		}
		return arrayOfDouble;
	}

	@Override
	public boolean equals(final RandomVariable randomVariable) {
		if(time != randomVariable.getFiltrationTime()) {
			return false;
		}
		if(this.isDeterministic() && randomVariable.isDeterministic()) {
			return valueIfNonStochastic == randomVariable.doubleValue();
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
	public int getTypePriority() {
		return typePriority;
	}

	@Override
	public double get(final int pathOrState) {
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
		if(realizations.length != 0) {
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
			final double value = realizations[i] - error;		// Error corrected value
			final double newSum = sum + value;				// New sum
			error = (newSum - sum) - value;								// New numerical error
			sum	= newSum;
		}
		return sum/realizations.length;
	}

	@Override
	public double getAverage(final RandomVariable probabilities) {
		if(isDeterministic()) {
			return valueIfNonStochastic * probabilities.getAverage();
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
			final double value = realizations[i] * probabilities.get(i) - error;		// Error corrected value
			final double newSum = sum + value;				// New sum
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

		final double average = getAverage();

		/*
		 * Kahan summation on (realizations[i] - average)^2
		 */
		double sum = 0.0;
		double errorOfSum	= 0.0;
		for(int i=0; i<realizations.length; i++) {
			final double value	= (realizations[i] - average)*(realizations[i] - average) - errorOfSum;
			final double newSum	= sum + value;
			errorOfSum		= (newSum - sum) - value;
			sum				= newSum;
		}
		return sum/realizations.length;
	}

	@Override
	public double getVariance(final RandomVariable probabilities) {
		if(isDeterministic()) {
			return 0.0;
		}
		if(size() == 0) {
			return Double.NaN;
		}

		final double average = getAverage(probabilities);

		/*
		 * Kahan summation on (realizations[i] - average)^2 * probabilities.get(i)
		 */
		double sum = 0.0;
		double errorOfSum	= 0.0;
		for(int i=0; i<realizations.length; i++) {
			final double value	= (realizations[i] - average) * (realizations[i] - average) * probabilities.get(i) - errorOfSum;
			final double newSum	= sum + value;
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
	public double getStandardDeviation(final RandomVariable probabilities) {
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
	 * @see net.finmath.stochastic.RandomVariable#getStandardError(net.finmath.stochastic.RandomVariable)
	 */
	@Override
	public double getStandardError(final RandomVariable probabilities) {
		if(isDeterministic()) {
			return 0.0;
		}
		if(size() == 0) {
			return Double.NaN;
		}

		return getStandardDeviation(probabilities)/Math.sqrt(size());
	}

	@Override
	public double getQuantile(final double quantile) {
		if(isDeterministic()) {
			return valueIfNonStochastic;
		}
		if(size() == 0) {
			return Double.NaN;
		}

		final float[] realizationsSorted = realizations.clone();
		java.util.Arrays.sort(realizationsSorted);

		final int indexOfQuantileValue = Math.min(Math.max((int)Math.round((size()+1) * quantile - 1), 0), size()-1);

		return realizationsSorted[indexOfQuantileValue];
	}

	@Override
	public double getQuantile(final double quantile, final RandomVariable probabilities) {
		if(isDeterministic()) {
			return valueIfNonStochastic;
		}
		if(size() == 0) {
			return Double.NaN;
		}

		throw new RuntimeException("Method not implemented.");
	}

	@Override
	public double getQuantileExpectation(final double quantileStart, final double quantileEnd) {
		if(isDeterministic()) {
			return valueIfNonStochastic;
		}
		if(size() == 0) {
			return Double.NaN;
		}
		if(quantileStart > quantileEnd) {
			return getQuantileExpectation(quantileEnd, quantileStart);
		}

		final float[] realizationsSorted = realizations.clone();
		java.util.Arrays.sort(realizationsSorted);

		final int indexOfQuantileValueStart	= Math.min(Math.max((int)Math.round((size()+1) * quantileStart - 1), 0), size()-1);
		final int indexOfQuantileValueEnd		= Math.min(Math.max((int)Math.round((size()+1) * quantileEnd - 1), 0), size()-1);

		double quantileExpectation = 0.0;
		for (int i=indexOfQuantileValueStart; i<=indexOfQuantileValueEnd;i++) {
			quantileExpectation += realizationsSorted[i];
		}
		quantileExpectation /= indexOfQuantileValueEnd-indexOfQuantileValueStart+1;

		return quantileExpectation;
	}

	@Override
	public double[] getHistogram(final double[] intervalPoints)
	{
		final double[] histogramValues = new double[intervalPoints.length+1];

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
			final float[] realizationsSorted = realizations.clone();
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

	@Override
	public double[][] getHistogram(final int numberOfPoints, final double standardDeviations) {
		final double[] intervalPoints = new double[numberOfPoints];
		final double[] anchorPoints	= new double[numberOfPoints+1];
		final double center	= getAverage();
		final double radius	= standardDeviations * getStandardDeviation();
		final double stepSize	= (numberOfPoints-1) / 2.0;
		for(int i=0; i<numberOfPoints;i++) {
			final double alpha = (-(double)(numberOfPoints-1) / 2.0 + i) / stepSize;
			intervalPoints[i]	= center + alpha * radius;
			anchorPoints[i]		= center + alpha * radius - radius / (2 * stepSize);
		}
		anchorPoints[numberOfPoints] = center + 1 * radius + radius / (2 * stepSize);

		final double[][] result = new double[2][];
		result[0] = anchorPoints;
		result[1] = getHistogram(intervalPoints);

		return result;
	}

	@Override
	public boolean isDeterministic() {
		return realizations == null;
	}

	@Override
	public RandomVariable cache() {
		return this;
	}

	@Override
	public DoubleStream getRealizationsStream() {
		if(isDeterministic()) {
			return DoubleStream.generate(new DoubleSupplier() {
				@Override
				public double getAsDouble() {
					return valueIfNonStochastic;
				}
			});
		} else {
			return Arrays.stream(getDoubleArray(realizations));
		}
	}

	@Override
	public double[] getRealizations() {
		if(isDeterministic()) {
			final double[] result = new double[] { doubleValue() };
			return result;
		} else {
			return getDoubleArray(realizations);
		}
	}

	@Override
	public Double doubleValue() {
		if(isDeterministic()) {
			return valueIfNonStochastic;
		}
		else if(size() == 1) {
			return getAverage();
		}
		else {
			throw new UnsupportedOperationException("The random variable is non-deterministic");
		}
	}

	@Override
	public IntToDoubleFunction getOperator() {
		if(isDeterministic()) {
			return new IntToDoubleFunction() {
				@Override
				public double applyAsDouble(final int i) {
					return valueIfNonStochastic;
				}
			};
		}
		else {
			return new IntToDoubleFunction() {
				@Override
				public double applyAsDouble(final int i) {
					return realizations[i];
				}
			};
		}
	}

	@Override
	public RandomVariable apply(final DoubleUnaryOperator operator) {
		if(isDeterministic()) {
			return new RandomVariableFromFloatArray(time, operator.applyAsDouble(valueIfNonStochastic));
		} else
		{
			// Still faster than a parallel stream (2014.04)
			final double[] result = new double[realizations.length];
			for(int i=0; i<result.length; i++) {
				result[i] = operator.applyAsDouble(realizations[i]);
			}
			return new RandomVariableFromFloatArray(time, result);
		}
	}

	@Override
	public RandomVariable apply(final DoubleBinaryOperator operator, final RandomVariable argument) {

		final double      newTime           = Math.max(time, argument.getFiltrationTime());

		if(isDeterministic() && argument.isDeterministic()) {
			return new RandomVariableFromFloatArray(newTime, operator.applyAsDouble(valueIfNonStochastic, argument.get(0)));
		} else if(isDeterministic() && !argument.isDeterministic()) {
			// Still faster than a parallel stream (2014.04)
			final double[] result = new double[argument.size()];
			for(int i=0; i<result.length; i++) {
				result[i] = operator.applyAsDouble(valueIfNonStochastic, argument.get(i));
			}
			return new RandomVariableFromFloatArray(newTime, result);
		}
		else if(!isDeterministic() && argument.isDeterministic()) {
			// Still faster than a parallel stream (2014.04)
			final double[] result = new double[this.size()];
			for(int i=0; i<result.length; i++) {
				result[i] = operator.applyAsDouble(realizations[i], argument.get(0));
			}
			return new RandomVariableFromFloatArray(newTime, result);
		}
		else if(!isDeterministic() && !argument.isDeterministic()) {
			// Still faster than a parallel stream (2014.04)
			final double[] result = new double[this.size()];
			for(int i=0; i<result.length; i++) {
				result[i] = operator.applyAsDouble(realizations[i], argument.get(i));
			}
			return new RandomVariableFromFloatArray(newTime, result);
		}

		/*
		 * Dead code: slower
		 */
		final int newSize = Math.max(this.size(), argument.size());

		final IntToDoubleFunction argument0Operator = this.getOperator();
		final IntToDoubleFunction argument1Operator = argument.getOperator();
		final IntToDoubleFunction result = new IntToDoubleFunction() {
			@Override
			public double applyAsDouble(final int i) {
				return operator.applyAsDouble(argument0Operator.applyAsDouble(i), argument1Operator.applyAsDouble(i));
			}
		};

		return new RandomVariableFromFloatArray(newTime, result, newSize);
	}

	@Override
	public RandomVariable apply(final DoubleTernaryOperator operator, final RandomVariable argument1, final RandomVariable argument2) {
		double newTime = Math.max(time, argument1.getFiltrationTime());
		newTime = Math.max(newTime, argument2.getFiltrationTime());

		final int newSize = Math.max(Math.max(this.size(), argument1.size()), argument2.size());

		final IntToDoubleFunction argument0Operator = this.getOperator();
		final IntToDoubleFunction argument1Operator = argument1.getOperator();
		final IntToDoubleFunction argument2Operator = argument2.getOperator();
		final IntToDoubleFunction result = new IntToDoubleFunction() {
			@Override
			public double applyAsDouble(final int i) {
				return operator.applyAsDouble(argument0Operator.applyAsDouble(i), argument1Operator.applyAsDouble(i), argument2Operator.applyAsDouble(i));
			}
		};

		return new RandomVariableFromFloatArray(newTime, result, newSize);
	}

	@Override
	public RandomVariable cap(final double cap) {
		if(isDeterministic()) {
			final double newValueIfNonStochastic = Math.min(valueIfNonStochastic,cap);
			return new RandomVariableFromFloatArray(time, newValueIfNonStochastic);
		}
		else {
			final float[] newRealizations = new float[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = Math.min(realizations[i],(float)cap);
			}
			return new RandomVariableFromFloatArray(time, newRealizations);
		}
	}

	@Override
	public RandomVariable floor(final double floor) {
		if(isDeterministic()) {
			final double newValueIfNonStochastic = Math.max(valueIfNonStochastic,floor);
			return new RandomVariableFromFloatArray(time, newValueIfNonStochastic);
		}
		else {
			final float[] newRealizations = new float[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = Math.max(realizations[i],(float)floor);
			}
			return new RandomVariableFromFloatArray(time, newRealizations);
		}
	}

	@Override
	public RandomVariable add(final double value) {
		if(isDeterministic()) {
			final double newValueIfNonStochastic = valueIfNonStochastic + value;
			return new RandomVariableFromFloatArray(time, newValueIfNonStochastic);
		}
		else {
			final float[] newRealizations = new float[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] + (float)value;
			}
			return new RandomVariableFromFloatArray(time, newRealizations);
		}
	}

	@Override
	public RandomVariable sub(final double value) {
		if(isDeterministic()) {
			final double newValueIfNonStochastic = valueIfNonStochastic - value;
			return new RandomVariableFromFloatArray(time, newValueIfNonStochastic);
		}
		else {
			final float[] newRealizations = new float[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] - (float)value;
			}
			return new RandomVariableFromFloatArray(time, newRealizations);
		}
	}

	@Override
	public RandomVariable bus(final double value) {
		if(isDeterministic()) {
			final double newValueIfNonStochastic = value - valueIfNonStochastic;
			return new RandomVariableFromFloatArray(time, newValueIfNonStochastic);
		}
		else {
			final float[] newRealizations = new float[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = (float)value - realizations[i];
			}
			return new RandomVariableFromFloatArray(time, newRealizations);
		}
	}

	@Override
	public RandomVariable mult(final double value) {
		if(isDeterministic()) {
			final double newValueIfNonStochastic = valueIfNonStochastic * value;
			return new RandomVariableFromFloatArray(time, newValueIfNonStochastic);
		}
		else {
			final float[] newRealizations = new float[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] * (float)value;
			}
			return new RandomVariableFromFloatArray(time, newRealizations);
		}
	}

	@Override
	public RandomVariable div(final double value) {
		if(isDeterministic()) {
			final double newValueIfNonStochastic = valueIfNonStochastic / value;
			return new RandomVariableFromFloatArray(time, newValueIfNonStochastic);
		}
		else {
			final float[] newRealizations = new float[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] / (float)value;
			}
			return new RandomVariableFromFloatArray(time, newRealizations);
		}
	}

	@Override
	public RandomVariable vid(final double value) {
		if(isDeterministic()) {
			final double newValueIfNonStochastic = value / valueIfNonStochastic;
			return new RandomVariableFromFloatArray(time, newValueIfNonStochastic);
		}
		else {
			final float[] newRealizations = new float[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = (float)value / realizations[i];
			}
			return new RandomVariableFromFloatArray(time, newRealizations);
		}
	}

	@Override
	public RandomVariable pow(final double exponent) {
		if(isDeterministic()) {
			final double newValueIfNonStochastic = Math.pow(valueIfNonStochastic,exponent);
			return new RandomVariableFromFloatArray(time, newValueIfNonStochastic);
		}
		else {
			final float[] newRealizations = new float[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = (float) Math.pow(realizations[i],(float)exponent);
			}
			return new RandomVariableFromFloatArray(time, newRealizations);
		}
	}

	@Override
	public RandomVariable average() {
		return new RandomVariableFromFloatArray(getAverage());
	}

	@Override
	public RandomVariable getConditionalExpectation(final ConditionalExpectationEstimator conditionalExpectationOperator)
	{
		return conditionalExpectationOperator.getConditionalExpectation(this);
	}

	@Override
	public RandomVariable squared() {
		if(isDeterministic()) {
			final double newValueIfNonStochastic = valueIfNonStochastic * valueIfNonStochastic;
			return new RandomVariableFromFloatArray(time, newValueIfNonStochastic);
		}
		else {
			final float[] newRealizations = new float[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] * realizations[i];
			}
			return new RandomVariableFromFloatArray(time, newRealizations);
		}
	}

	@Override
	public RandomVariable sqrt() {
		if(isDeterministic()) {
			final double newValueIfNonStochastic = Math.sqrt(valueIfNonStochastic);
			return new RandomVariableFromFloatArray(time, newValueIfNonStochastic);
		}
		else {
			final float[] newRealizations = new float[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = (float)Math.sqrt(realizations[i]);
			}
			return new RandomVariableFromFloatArray(time, newRealizations);
		}
	}

	@Override
	public RandomVariable invert() {
		if(isDeterministic()) {
			final double newValueIfNonStochastic = 1.0/valueIfNonStochastic;
			return new RandomVariableFromFloatArray(time, newValueIfNonStochastic);
		}
		else {
			final float[] newRealizations = new float[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = 1.0f/realizations[i];
			}
			return new RandomVariableFromFloatArray(time, newRealizations);
		}
	}

	@Override
	public RandomVariable abs() {
		if(isDeterministic()) {
			final double newValueIfNonStochastic = Math.abs(valueIfNonStochastic);
			return new RandomVariableFromFloatArray(time, newValueIfNonStochastic);
		}
		else {
			final float[] newRealizations = new float[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = Math.abs(realizations[i]);
			}
			return new RandomVariableFromFloatArray(time, newRealizations);
		}
	}

	@Override
	public RandomVariableFromFloatArray exp() {
		if(isDeterministic()) {
			final double newValueIfNonStochastic = Math.exp(valueIfNonStochastic);
			return new RandomVariableFromFloatArray(time, newValueIfNonStochastic);
		}
		else {
			final float[] newRealizations = new float[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = (float)Math.exp(realizations[i]);
			}
			return new RandomVariableFromFloatArray(time, newRealizations);
		}
	}

	@Override
	public RandomVariableFromFloatArray log() {
		if(isDeterministic()) {
			final double newValueIfNonStochastic = Math.log(valueIfNonStochastic);
			return new RandomVariableFromFloatArray(time, newValueIfNonStochastic);
		}
		else {
			final float[] newRealizations = new float[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = (float)Math.log(realizations[i]);
			}
			return new RandomVariableFromFloatArray(time, newRealizations);
		}
	}

	@Override
	public RandomVariable sin() {
		if(isDeterministic()) {
			final double newValueIfNonStochastic = Math.sin(valueIfNonStochastic);
			return new RandomVariableFromFloatArray(time, newValueIfNonStochastic);
		}
		else {
			final float[] newRealizations = new float[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = (float)Math.sin(realizations[i]);
			}
			return new RandomVariableFromFloatArray(time, newRealizations);
		}
	}

	@Override
	public RandomVariable cos() {
		if(isDeterministic()) {
			final double newValueIfNonStochastic = Math.cos(valueIfNonStochastic);
			return new RandomVariableFromFloatArray(time, newValueIfNonStochastic);
		}
		else {
			final float[] newRealizations = new float[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = (float)Math.cos(realizations[i]);
			}
			return new RandomVariableFromFloatArray(time, newRealizations);
		}
	}

	/*
	 * Binary operators: checking for return type priority.
	 */

	@Override
	public RandomVariable add(final RandomVariable randomVariable) {
		if(randomVariable.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return randomVariable.add(this);
		}

		// Set time of this random variable to maximum of time with respect to which measurability is known.
		final double newTime = Math.max(time, randomVariable.getFiltrationTime());

		if(isDeterministic() && randomVariable.isDeterministic()) {
			final double newValueIfNonStochastic = valueIfNonStochastic + randomVariable.get(0);
			return new RandomVariableFromFloatArray(newTime, newValueIfNonStochastic);
		}
		else if(isDeterministic()) {
			final float[] newRealizations = new float[Math.max(size(), randomVariable.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = ((float)valueIfNonStochastic + (float)randomVariable.get(i));
			}
			return new RandomVariableFromFloatArray(newTime, newRealizations);
		} else {
			final float[] newRealizations = new float[Math.max(size(), randomVariable.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] + (float)randomVariable.get(i);
			}
			return new RandomVariableFromFloatArray(newTime, newRealizations);
		}
	}

	@Override
	public RandomVariable sub(final RandomVariable randomVariable) {
		if(randomVariable.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return randomVariable.bus(this);
		}

		// Set time of this random variable to maximum of time with respect to which measurability is known.
		final double newTime = Math.max(time, randomVariable.getFiltrationTime());

		if(isDeterministic() && randomVariable.isDeterministic()) {
			final double newValueIfNonStochastic = valueIfNonStochastic - randomVariable.get(0);
			return new RandomVariableFromFloatArray(newTime, newValueIfNonStochastic);
		}
		else if(isDeterministic()) {
			final float[] newRealizations = new float[Math.max(size(), randomVariable.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = (float)valueIfNonStochastic - (float)randomVariable.get(i);
			}
			return new RandomVariableFromFloatArray(newTime, newRealizations);
		}
		else {
			final float[] newRealizations = new float[Math.max(size(), randomVariable.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] - (float)randomVariable.get(i);
			}
			return new RandomVariableFromFloatArray(newTime, newRealizations);
		}
	}

	@Override
	public RandomVariable bus(final RandomVariable randomVariable) {
		if(randomVariable.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return randomVariable.sub(this);
		}

		// Set time of this random variable to maximum of time with respect to which measurability is known.
		final double newTime = Math.max(time, randomVariable.getFiltrationTime());

		if(isDeterministic() && randomVariable.isDeterministic()) {
			final double newValueIfNonStochastic = randomVariable.get(0) - valueIfNonStochastic;
			return new RandomVariableFromFloatArray(newTime, newValueIfNonStochastic);
		}
		else if(isDeterministic()) {
			final float[] newRealizations = new float[Math.max(size(), randomVariable.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = (float)randomVariable.get(i) - (float)valueIfNonStochastic;
			}
			return new RandomVariableFromFloatArray(newTime, newRealizations);
		}
		else {
			final float[] newRealizations = new float[Math.max(size(), randomVariable.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = (float)randomVariable.get(i) - realizations[i];
			}
			return new RandomVariableFromFloatArray(newTime, newRealizations);
		}
	}

	@Override
	public RandomVariable mult(final RandomVariable randomVariable) {
		if(randomVariable.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return randomVariable.mult(this);
		}

		// Set time of this random variable to maximum of time with respect to which measurability is known.
		final double newTime = Math.max(time, randomVariable.getFiltrationTime());

		if(isDeterministic() && randomVariable.isDeterministic()) {
			final double newValueIfNonStochastic = valueIfNonStochastic * randomVariable.get(0);
			return new RandomVariableFromFloatArray(newTime, newValueIfNonStochastic);
		}
		else if(randomVariable.isDeterministic()) {
			return this.mult(randomVariable.get(0));
		} else if(isDeterministic()) {
			final float[] newRealizations = new float[Math.max(size(), randomVariable.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = (float)valueIfNonStochastic * (float)randomVariable.get(i);
			}
			return new RandomVariableFromFloatArray(newTime, newRealizations);
		}
		else {
			final float[] newRealizations = new float[Math.max(size(), randomVariable.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] * (float)randomVariable.get(i);
			}
			return new RandomVariableFromFloatArray(newTime, newRealizations);
		}
	}

	@Override
	public RandomVariable div(final RandomVariable randomVariable) {
		if(randomVariable.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return randomVariable.vid(this);
		}

		// Set time of this random variable to maximum of time with respect to which measurability is known.
		final double newTime = Math.max(time, randomVariable.getFiltrationTime());

		if(isDeterministic() && randomVariable.isDeterministic()) {
			final double newValueIfNonStochastic = valueIfNonStochastic / randomVariable.get(0);
			return new RandomVariableFromFloatArray(newTime, newValueIfNonStochastic);
		}
		else if(isDeterministic()) {
			final float[] newRealizations = new float[Math.max(size(), randomVariable.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i] = (float)valueIfNonStochastic / (float)randomVariable.get(i);
			}
			return new RandomVariableFromFloatArray(newTime, newRealizations);
		}
		else {
			final float[] newRealizations = new float[Math.max(size(), randomVariable.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] / (float)randomVariable.get(i);
			}
			return new RandomVariableFromFloatArray(newTime, newRealizations);
		}
	}

	@Override
	public RandomVariable vid(final RandomVariable randomVariable) {
		if(randomVariable.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return randomVariable.div(this);
		}

		// Set time of this random variable to maximum of time with respect to which measurability is known.
		final double newTime = Math.max(time, randomVariable.getFiltrationTime());

		if(isDeterministic() && randomVariable.isDeterministic()) {
			final double newValueIfNonStochastic = randomVariable.get(0) / valueIfNonStochastic;
			return new RandomVariableFromFloatArray(newTime, newValueIfNonStochastic);
		}
		else if(isDeterministic()) {
			final float[] newRealizations = new float[Math.max(size(), randomVariable.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = (float)randomVariable.get(i) / (float)valueIfNonStochastic;
			}
			return new RandomVariableFromFloatArray(newTime, newRealizations);
		}
		else {
			final float[] newRealizations = new float[Math.max(size(), randomVariable.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = (float)randomVariable.get(i) / realizations[i];
			}
			return new RandomVariableFromFloatArray(newTime, newRealizations);
		}
	}

	@Override
	public RandomVariable cap(final RandomVariable randomVariable) {
		if(randomVariable.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return randomVariable.cap(this);
		}

		// Set time of this random variable to maximum of time with respect to which measurability is known.
		final double newTime = Math.max(time, randomVariable.getFiltrationTime());

		if(isDeterministic() && randomVariable.isDeterministic()) {
			final double newValueIfNonStochastic = Math.min(valueIfNonStochastic, randomVariable.get(0));
			return new RandomVariableFromFloatArray(newTime, newValueIfNonStochastic);
		}
		else if(isDeterministic()) {
			final float[] newRealizations = new float[Math.max(size(), randomVariable.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = Math.min((float)valueIfNonStochastic, (float)randomVariable.get(i));
			}
			return new RandomVariableFromFloatArray(newTime, newRealizations);
		} else {
			final float[] newRealizations = new float[Math.max(size(), randomVariable.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = Math.min(realizations[i], (float)randomVariable.get(i));
			}
			return new RandomVariableFromFloatArray(newTime, newRealizations);
		}
	}

	@Override
	public RandomVariable floor(final RandomVariable randomVariable) {
		if(randomVariable.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return randomVariable.floor(this);
		}

		// Set time of this random variable to maximum of time with respect to which measurability is known.
		final double newTime = Math.max(time, randomVariable.getFiltrationTime());

		if(isDeterministic() && randomVariable.isDeterministic()) {
			final double newValueIfNonStochastic = Math.max(valueIfNonStochastic, randomVariable.get(0));
			return new RandomVariableFromFloatArray(newTime, newValueIfNonStochastic);
		}
		else if(isDeterministic()) {
			final float[] newRealizations = new float[Math.max(size(), randomVariable.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = (float) Math.max(valueIfNonStochastic, (float)randomVariable.get(i));
			}
			return new RandomVariableFromFloatArray(newTime, newRealizations);
		}
		else if(randomVariable.isDeterministic()) {
			return this.floor(randomVariable.doubleValue());
		}
		else {
			final float[] newRealizations = new float[Math.max(size(), randomVariable.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = Math.max(realizations[i], (float)randomVariable.get(i));
			}
			return new RandomVariableFromFloatArray(newTime, newRealizations);
		}
	}

	@Override
	public RandomVariable accrue(final RandomVariable rate, final double periodLength) {
		if(rate.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return rate.mult(periodLength).add(1.0).mult(this);
		}

		// Set time of this random variable to maximum of time with respect to which measurability is known.
		final double newTime = Math.max(time, rate.getFiltrationTime());

		if(rate.isDeterministic()) {
			return this.mult(1.0 + rate.get(0) * periodLength);
		} else if(isDeterministic() && !rate.isDeterministic()) {
			final float[] newRealizations = new float[Math.max(size(), rate.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 =  (float)valueIfNonStochastic * (1 + (float)rate.get(i) * (float)periodLength);
			}
			return new RandomVariableFromFloatArray(newTime, newRealizations);
		}
		else {
			final float[] newRealizations = new float[Math.max(size(), rate.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] * (1.0f + (float)rate.get(i) * (float)periodLength);
			}
			return new RandomVariableFromFloatArray(newTime, newRealizations);
		}
	}

	@Override
	public RandomVariable discount(final RandomVariable rate, final double periodLength) {
		if(rate.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return rate.mult(periodLength).add(1.0).vid(this);
		}

		// Set time of this random variable to maximum of time with respect to which measurability is known.
		final double newTime = Math.max(time, rate.getFiltrationTime());

		if(rate.isDeterministic()) {
			return this.div(1.0 + rate.doubleValue() * periodLength);
		} else if(isDeterministic() && !rate.isDeterministic()) {
			final float[] newRealizations = new float[Math.max(size(), rate.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = (float)valueIfNonStochastic / (1.0f + (float)rate.get(i) * (float)periodLength);
			}
			return new RandomVariableFromFloatArray(newTime, newRealizations);
		}
		else {
			final float[] newRealizations = new float[Math.max(size(), rate.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] / (1.0f + (float)rate.get(i) * (float)periodLength);
			}
			return new RandomVariableFromFloatArray(newTime, newRealizations);
		}
	}

	/*
	 * Ternary operators: checking for return type priority.
	 * @TODO add checking for return type priority.
	 */

	@Override
	public RandomVariable choose(final RandomVariable valueIfTriggerNonNegative, final RandomVariable valueIfTriggerNegative) {
		// Set time of this random variable to maximum of time with respect to which measurability is known.
		double newTime = time;
		newTime = Math.max(newTime, valueIfTriggerNonNegative.getFiltrationTime());
		newTime = Math.max(newTime, valueIfTriggerNegative.getFiltrationTime());

		if(isDeterministic()) {
			if(valueIfNonStochastic >= 0) {
				return valueIfTriggerNonNegative;
			} else {
				return valueIfTriggerNegative;
			}
		}
		else {
			final int numberOfPaths = this.size();
			final float[] newRealizations = new float[numberOfPaths];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i] = (float) (realizations[i] >= 0.0 ? valueIfTriggerNonNegative.get(i) : valueIfTriggerNegative.get(i));
			}
			return new RandomVariableFromFloatArray(newTime, newRealizations);
		}
	}

	@Override
	public RandomVariable addProduct(final RandomVariable factor1, final double factor2) {
		if(factor1.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return factor1.mult(factor2).add(this);
		}

		// Set time of this random variable to maximum of time with respect to which measurability is known.
		final double newTime = Math.max(time, factor1.getFiltrationTime());

		if(factor1.isDeterministic()) {
			return this.add(factor1.get(0) * factor2);
		} else if(isDeterministic() && !factor1.isDeterministic()) {
			final float[] newRealizations = new float[Math.max(size(), factor1.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = (float)valueIfNonStochastic + (float)factor1.get(i) * (float)factor2;
			}
			return new RandomVariableFromFloatArray(newTime, newRealizations);
		}
		else if(!isDeterministic() && factor1.isDeterministic()) {
			final float factor1Value = (float)factor1.get(0);
			final float[] newRealizations = new float[Math.max(size(), factor1.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] + factor1Value * (float)factor2;
			}
			return new RandomVariableFromFloatArray(newTime, newRealizations);
		}
		else {
			final float[] newRealizations = new float[Math.max(size(), factor1.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] + (float)factor1.get(i) * (float)factor2;
			}
			return new RandomVariableFromFloatArray(newTime, newRealizations);
		}
	}

	@Override
	public RandomVariable addProduct(final RandomVariable factor1, final RandomVariable factor2) {
		if(factor1.getTypePriority() > this.getTypePriority() || factor2.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return factor1.mult(factor2).add(this);
		}

		// Set time of this random variable to maximum of time with respect to which measurability is known.
		final double newTime = Math.max(Math.max(time, factor1.getFiltrationTime()), factor2.getFiltrationTime());

		if(isDeterministic() && factor1.isDeterministic() && factor2.isDeterministic()) {
			final double newValueIfNonStochastic = valueIfNonStochastic + (factor1.doubleValue() * factor2.doubleValue());
			return new RandomVariableFromDoubleArray(newTime, newValueIfNonStochastic);
		}
		else if(factor1.isDeterministic() && factor2.isDeterministic()) {
			return add(factor1.doubleValue() * factor2.doubleValue());
		}
		else if(factor2.isDeterministic()) {
			return this.addProduct(factor1, factor2.doubleValue());
		}
		else if(factor1.isDeterministic()) {
			return this.addProduct(factor2, factor1.doubleValue());
		}
		else if(!this.isDeterministic() && !factor1.isDeterministic() && !factor2.isDeterministic()) {
			final float[] newRealizations = new float[Math.max(Math.max(size(), factor1.size()), factor2.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] + (float)factor1.get(i) * (float)factor2.get(i);
			}
			return new RandomVariableFromFloatArray(newTime, newRealizations);
		}
		else {
			return this.add(factor1.mult(factor2));
		}
	}

	@Override
	public RandomVariable addSumProduct(final List<RandomVariable> factor1, final List<RandomVariable> factor2)
	{
		RandomVariable result = this;
		for(int i=0; i<factor1.size(); i++) {
			result = result.addProduct(factor1.get(i), factor2.get(i));
		}
		return result;
	}

	@Override
	public RandomVariable addRatio(final RandomVariable numerator, final RandomVariable denominator) {
		if(numerator.getTypePriority() > this.getTypePriority() || denominator.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return numerator.div(denominator).add(this);
		}

		// Set time of this random variable to maximum of time with respect to which measurability is known.
		final double newTime = Math.max(Math.max(time, numerator.getFiltrationTime()), denominator.getFiltrationTime());

		if(isDeterministic() && numerator.isDeterministic() && denominator.isDeterministic()) {
			final double newValueIfNonStochastic = valueIfNonStochastic + (numerator.get(0) / denominator.get(0));
			return new RandomVariableFromFloatArray(newTime, newValueIfNonStochastic);
		}
		else {
			final float[] newRealizations = new float[Math.max(Math.max(size(), numerator.size()), denominator.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = (float)get(i) + (float)numerator.get(i) / (float)denominator.get(i);
			}
			return new RandomVariableFromFloatArray(newTime, newRealizations);
		}
	}

	@Override
	public RandomVariable subRatio(final RandomVariable numerator, final RandomVariable denominator) {
		if(numerator.getTypePriority() > this.getTypePriority() || denominator.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return numerator.div(denominator).mult(-1).add(this);
		}

		// Set time of this random variable to maximum of time with respect to which measurability is known.
		final double newTime = Math.max(Math.max(time, numerator.getFiltrationTime()), denominator.getFiltrationTime());

		if(isDeterministic() && numerator.isDeterministic() && denominator.isDeterministic()) {
			final double newValueIfNonStochastic = valueIfNonStochastic - (numerator.get(0) / denominator.get(0));
			return new RandomVariableFromFloatArray(newTime, newValueIfNonStochastic);
		}
		else {
			final float[] newRealizations = new float[Math.max(Math.max(size(), numerator.size()), denominator.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = (float)get(i) - (float)numerator.get(i) / (float)denominator.get(i);
			}
			return new RandomVariableFromFloatArray(newTime, newRealizations);
		}
	}

	@Override
	public RandomVariable isNaN() {
		if(isDeterministic()) {
			return new RandomVariableFromFloatArray(time, Double.isNaN(valueIfNonStochastic) ? 1.0f : 0.0f);
		} else {
			final float[] newRealizations = new float[size()];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = Double.isNaN(get(i)) ? 1.0f : 0.0f;
			}
			return new RandomVariableFromFloatArray(time, newRealizations);
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " [time=" + time + ", realizations="
				+ (isDeterministic() ? valueIfNonStochastic : Arrays.toString(realizations)) + ", isDeterministic()=" + isDeterministic() + ", typePriority=" + typePriority + "]";
	}
}
