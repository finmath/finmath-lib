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

import org.apache.commons.math3.util.FastMath;

import net.finmath.functions.DoubleTernaryOperator;
import net.finmath.stochastic.ConditionalExpectationEstimator;
import net.finmath.stochastic.RandomVariable;

/**
 * The class RandomVariableFromDoubleArray represents a random variable being the evaluation of a stochastic process
 * at a certain time within a Monte-Carlo simulation.
 * It is thus essentially a vector of doubles - the realizations - together with a double - the time.
 * The index of the vector represents path.
 * The class may also be used for non-stochastic quantities which may potentially be stochastic
 * (e.g. volatility). If only non-stochastic random variables are involved in an operation the class uses
 * optimized code.
 *
 * Accesses performed exclusively through the interface
 * <code>RandomVariable</code> is thread safe (and does not mutate the class).
 *
 * The implementation requires Java 8 or better.
 *
 * @author Christian Fries
 * @version 2.1
 */
public class RandomVariableFromDoubleArray implements RandomVariable {

	private static final long serialVersionUID = -1352953450936857742L;

	private static final int typePriorityDefault = 1;

	private final int typePriority;

	private final double      time;	                // Time (filtration)

	// Data model for the stochastic case (otherwise null)
	private final double[]    realizations;           // Realizations

	// Data model for the non-stochastic case (if realizations==null)
	private final double      valueIfNonStochastic;

	/**
	 * Create a random variable from a given other implementation of <code>RandomVariable</code>.
	 *
	 * @param value Object implementing <code>RandomVariable</code>.
	 */
	public RandomVariableFromDoubleArray(RandomVariable value) {
		super();
		time = value.getFiltrationTime();
		realizations = value.isDeterministic() ? null : value.getRealizations();
		valueIfNonStochastic = value.isDeterministic() ? value.get(0) : Double.NaN;
		typePriority = typePriorityDefault;
	}

	/**
	 * Create a non stochastic random variable, i.e. a constant.
	 *
	 * @param value the value, a constant.
	 */
	public RandomVariableFromDoubleArray(double value) {
		this(Double.NEGATIVE_INFINITY, value);
	}

	/**
	 * Create a random variable by applying a function to a given other implementation of <code>RandomVariable</code>.
	 *
	 * @param value Object implementing <code>RandomVariable</code>.
	 * @param function A function mapping double to double.
	 */
	public RandomVariableFromDoubleArray(RandomVariable value, DoubleUnaryOperator function) {
		super();
		time = value.getFiltrationTime();
		realizations = value.isDeterministic() ? null : value.getRealizationsStream().map(function).toArray();
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
	public RandomVariableFromDoubleArray(double time, double value, int typePriority) {
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
	public RandomVariableFromDoubleArray(double time, double value) {
		this(time, value, typePriorityDefault);
	}

	/**
	 * Create a non stochastic random variable, i.e. a constant.
	 *
	 * @param time the filtration time, set to 0.0 if not used.
	 * @param numberOfPath The number of paths.
	 * @param value the value, a constant.
	 */
	@Deprecated
	public RandomVariableFromDoubleArray(double time, int numberOfPath, double value) {
		super();
		this.time = time;
		realizations = new double[numberOfPath];
		java.util.Arrays.fill(realizations, value);
		valueIfNonStochastic = Double.NaN;
		typePriority = typePriorityDefault;
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
	 * @param typePriority The priority of this type in construction of result types. See "operator type priority" for details.
	 */
	public RandomVariableFromDoubleArray(double time, double[] realisations, int typePriority) {
		super();
		this.time = time;
		realizations = realisations;
		valueIfNonStochastic = Double.NaN;
		this.typePriority = typePriority;
		//		for(double value : realisations) if(Double.isNaN(value)) {
		//			throw new ArithmeticException("Not a Numbber");
		//		}
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
	public RandomVariableFromDoubleArray(double time, double[] realisations) {
		this(time, realisations, typePriorityDefault);
	}

	/**
	 * Create a stochastic random variable.
	 *
	 * @param time the filtration time, set to 0.0 if not used.
	 * @param realizations A map mapping integer (path or state) to double, representing this random variable.
	 * @param size The size, i.e., number of paths.
	 * @param typePriority The priority of this type in construction of result types. See "operator type priority" for details.
	 */
	public RandomVariableFromDoubleArray(double time, IntToDoubleFunction realizations, int size, int typePriority) {
		super();
		this.time = time;
		this.realizations = size == 1 ? null : new double[size];//IntStream.range(0,size).parallel().mapToDouble(realisations).toArray();
		valueIfNonStochastic = size == 1 ? realizations.applyAsDouble(0) : Double.NaN;
		if(size > 1) {
			IntStream.range(0,size).parallel().forEach(new IntConsumer() {
				@Override
				public void accept(int i) {
					RandomVariableFromDoubleArray.this.realizations[i] = realizations.applyAsDouble(i);
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
	public RandomVariableFromDoubleArray(double time, IntToDoubleFunction realizations, int size) {
		this(time, realizations, size, typePriorityDefault);
	}

	@Override
	public boolean equals(RandomVariable randomVariable) {
		if(time != randomVariable.getFiltrationTime()) {
			return false;
		}
		if(this.isDeterministic() && randomVariable.isDeterministic()) {
			return valueIfNonStochastic == randomVariable.get(0);
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
			double value = realizations[i] - error;		// Error corrected value
			double newSum = sum + value;				// New sum
			error = (newSum - sum) - value;				// New numerical error
			sum	= newSum;
		}
		return sum/realizations.length;
	}

	@Override
	public double getAverage(RandomVariable probabilities) {
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
	public double getVariance(RandomVariable probabilities) {
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
	public double getStandardDeviation(RandomVariable probabilities) {
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
	public double getStandardError(RandomVariable probabilities) {
		if(isDeterministic()) {
			return 0.0;
		}
		if(size() == 0) {
			return Double.NaN;
		}

		return getStandardDeviation(probabilities)/Math.sqrt(size());
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#getQuantile()
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
	 * @see net.finmath.stochastic.RandomVariable#getQuantile(net.finmath.stochastic.RandomVariable)
	 */
	@Override
	public double getQuantile(double quantile, RandomVariable probabilities) {
		if(isDeterministic()) {
			return valueIfNonStochastic;
		}
		if(size() == 0) {
			return Double.NaN;
		}

		throw new RuntimeException("Method not implemented.");
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#getConditionalVaR()
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
	 * @see net.finmath.stochastic.RandomVariable#getHistogram()
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
	 * @see net.finmath.stochastic.RandomVariable#getHistogram(int,double)
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
	 * @see net.finmath.stochastic.RandomVariable#isDeterministic()
	 */
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

	@Override
	public IntToDoubleFunction getOperator() {
		if(isDeterministic()) {
			return new IntToDoubleFunction() {
				@Override
				public double applyAsDouble(int i) {
					return valueIfNonStochastic;
				}
			};
		}
		else {
			return new IntToDoubleFunction() {
				@Override
				public double applyAsDouble(int i) {
					return realizations[i];
				}
			};
		}
	}

	@Override
	public RandomVariable apply(DoubleUnaryOperator operator) {
		if(isDeterministic()) {
			return new RandomVariableFromDoubleArray(time, operator.applyAsDouble(valueIfNonStochastic));
		}
		else
		{
			// Still faster than a parallel stream (2014.04)
			double[] result = new double[realizations.length];
			for(int i=0; i<result.length; i++) {
				result[i] = operator.applyAsDouble(realizations[i]);
			}
			return new RandomVariableFromDoubleArray(time, result);
		}
	}

	@Override
	public RandomVariable apply(DoubleBinaryOperator operator, RandomVariable argument) {
		double      newTime           = Math.max(time, argument.getFiltrationTime());

		if(isDeterministic() && argument.isDeterministic()) {
			return new RandomVariableFromDoubleArray(newTime, operator.applyAsDouble(valueIfNonStochastic, argument.get(0)));
		}
		else if(isDeterministic() && !argument.isDeterministic()) {
			// Still faster than a parallel stream (2014.04)
			double[] result = new double[argument.size()];
			for(int i=0; i<result.length; i++) {
				result[i] = operator.applyAsDouble(valueIfNonStochastic, argument.get(i));
			}
			return new RandomVariableFromDoubleArray(newTime, result);
		}
		else if(!isDeterministic() && argument.isDeterministic()) {
			// Still faster than a parallel stream (2014.04)
			double[] result = new double[this.size()];
			for(int i=0; i<result.length; i++) {
				result[i] = operator.applyAsDouble(realizations[i], argument.get(0));
			}
			return new RandomVariableFromDoubleArray(newTime, result);
		}
		else if(!isDeterministic() && !argument.isDeterministic()) {
			// Still faster than a parallel stream (2014.04)
			double[] result = new double[this.size()];
			for(int i=0; i<result.length; i++) {
				result[i] = operator.applyAsDouble(realizations[i], argument.get(i));
			}
			return new RandomVariableFromDoubleArray(newTime, result);
		}

		/*
		 * Dead code: slower
		 */
		int newSize = Math.max(this.size(), argument.size());

		IntToDoubleFunction argument0Operator = this.getOperator();
		IntToDoubleFunction argument1Operator = argument.getOperator();
		IntToDoubleFunction result = new IntToDoubleFunction() {
			@Override
			public double applyAsDouble(int i) {
				return operator.applyAsDouble(argument0Operator.applyAsDouble(i), argument1Operator.applyAsDouble(i));
			}
		};

		return new RandomVariableFromDoubleArray(newTime, result, newSize);
	}

	@Override
	public RandomVariable apply(DoubleTernaryOperator operator, RandomVariable argument1, RandomVariable argument2) {
		double newTime = Math.max(time, argument1.getFiltrationTime());
		newTime = Math.max(newTime, argument2.getFiltrationTime());

		int newSize = Math.max(Math.max(this.size(), argument1.size()), argument2.size());

		IntToDoubleFunction argument0Operator = this.getOperator();
		IntToDoubleFunction argument1Operator = argument1.getOperator();
		IntToDoubleFunction argument2Operator = argument2.getOperator();
		IntToDoubleFunction result = new IntToDoubleFunction() {
			@Override
			public double applyAsDouble(int i) {
				return operator.applyAsDouble(argument0Operator.applyAsDouble(i), argument1Operator.applyAsDouble(i), argument2Operator.applyAsDouble(i));
			}
		};

		return new RandomVariableFromDoubleArray(newTime, result, newSize);
	}

	public RandomVariable apply(DoubleBinaryOperator operatorOuter, DoubleBinaryOperator operatorInner, RandomVariable argument1, RandomVariable argument2)
	{
		return apply(new DoubleTernaryOperator() {
			@Override
			public double applyAsDouble(double x, double y, double z) {
				return operatorOuter.applyAsDouble(x,operatorInner.applyAsDouble(y,z));
			}
		}, argument1, argument2);
	}

	@Override
	public RandomVariable cap(double cap) {
		if(isDeterministic()) {
			double newValueIfNonStochastic = Math.min(valueIfNonStochastic,cap);
			return new RandomVariableFromDoubleArray(time, newValueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = Math.min(realizations[i],cap);
			}
			return new RandomVariableFromDoubleArray(time, newRealizations);
		}
	}

	@Override
	public RandomVariable floor(double floor) {
		if(isDeterministic()) {
			double newValueIfNonStochastic = Math.max(valueIfNonStochastic,floor);
			return new RandomVariableFromDoubleArray(time, newValueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = Math.max(realizations[i],floor);
			}
			return new RandomVariableFromDoubleArray(time, newRealizations);
		}
	}

	@Override
	public RandomVariable add(double value) {
		if(isDeterministic()) {
			double newValueIfNonStochastic = valueIfNonStochastic + value;
			return new RandomVariableFromDoubleArray(time, newValueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] + value;
			}
			return new RandomVariableFromDoubleArray(time, newRealizations);
		}
	}

	@Override
	public RandomVariable sub(double value) {
		if(isDeterministic()) {
			double newValueIfNonStochastic = valueIfNonStochastic - value;
			return new RandomVariableFromDoubleArray(time, newValueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] - value;
			}
			return new RandomVariableFromDoubleArray(time, newRealizations);
		}
	}

	@Override
	public RandomVariable mult(double value) {
		if(isDeterministic()) {
			double newValueIfNonStochastic = valueIfNonStochastic * value;
			return new RandomVariableFromDoubleArray(time, newValueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] * value;
			}
			return new RandomVariableFromDoubleArray(time, newRealizations);
		}
	}

	@Override
	public RandomVariable div(double value) {
		if(isDeterministic()) {
			double newValueIfNonStochastic = valueIfNonStochastic / value;
			return new RandomVariableFromDoubleArray(time, newValueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] / value;
			}
			return new RandomVariableFromDoubleArray(time, newRealizations);
		}
	}

	@Override
	public RandomVariable pow(double exponent) {
		if(isDeterministic()) {
			double newValueIfNonStochastic = Math.pow(valueIfNonStochastic,exponent);
			return new RandomVariableFromDoubleArray(time, newValueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = Math.pow(realizations[i],exponent);
			}
			return new RandomVariableFromDoubleArray(time, newRealizations);
		}
	}

	@Override
	public RandomVariable average() {
		return new RandomVariableFromDoubleArray(getAverage());
	}

	@Override
	public RandomVariable getConditionalExpectation(ConditionalExpectationEstimator conditionalExpectationOperator)
	{
		return conditionalExpectationOperator.getConditionalExpectation(this);
	}

	@Override
	public RandomVariable squared() {
		if(isDeterministic()) {
			double newValueIfNonStochastic = valueIfNonStochastic * valueIfNonStochastic;
			return new RandomVariableFromDoubleArray(time, newValueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] * realizations[i];
			}
			return new RandomVariableFromDoubleArray(time, newRealizations);
		}
	}

	@Override
	public RandomVariable sqrt() {
		if(isDeterministic()) {
			double newValueIfNonStochastic = Math.sqrt(valueIfNonStochastic);
			return new RandomVariableFromDoubleArray(time, newValueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = Math.sqrt(realizations[i]);
			}
			return new RandomVariableFromDoubleArray(time, newRealizations);
		}
	}

	@Override
	public RandomVariableFromDoubleArray exp() {
		if(isDeterministic()) {
			double newValueIfNonStochastic = FastMath.exp(valueIfNonStochastic);
			return new RandomVariableFromDoubleArray(time, newValueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = FastMath.exp(realizations[i]);
			}
			return new RandomVariableFromDoubleArray(time, newRealizations);
		}
	}

	@Override
	public RandomVariableFromDoubleArray log() {
		if(isDeterministic()) {
			double newValueIfNonStochastic = FastMath.log(valueIfNonStochastic);
			return new RandomVariableFromDoubleArray(time, newValueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = FastMath.log(realizations[i]);
			}
			return new RandomVariableFromDoubleArray(time, newRealizations);
		}
	}

	@Override
	public RandomVariable sin() {
		if(isDeterministic()) {
			double newValueIfNonStochastic = FastMath.sin(valueIfNonStochastic);
			return new RandomVariableFromDoubleArray(time, newValueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = FastMath.sin(realizations[i]);
			}
			return new RandomVariableFromDoubleArray(time, newRealizations);
		}
	}

	@Override
	public RandomVariable cos() {
		if(isDeterministic()) {
			double newValueIfNonStochastic = FastMath.cos(valueIfNonStochastic);
			return new RandomVariableFromDoubleArray(time, newValueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = FastMath.cos(realizations[i]);
			}
			return new RandomVariableFromDoubleArray(time, newRealizations);
		}
	}

	/*
	 * Binary operators: checking for return type priority.
	 */

	@Override
	public RandomVariable add(RandomVariable randomVariable) {
		if(randomVariable.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return randomVariable.add(this);
		}

		// Set time of this random variable to maximum of time with respect to which measurability is known.
		double newTime = Math.max(time, randomVariable.getFiltrationTime());

		if(isDeterministic() && randomVariable.isDeterministic()) {
			double newValueIfNonStochastic = valueIfNonStochastic + randomVariable.get(0);
			return new RandomVariableFromDoubleArray(newTime, newValueIfNonStochastic);
		}
		else if(isDeterministic()) {
			double[] newRealizations = new double[Math.max(size(), randomVariable.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = valueIfNonStochastic + randomVariable.get(i);
			}
			return new RandomVariableFromDoubleArray(newTime, newRealizations);
		} else {
			double[] newRealizations = new double[Math.max(size(), randomVariable.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] + randomVariable.get(i);
			}
			return new RandomVariableFromDoubleArray(newTime, newRealizations);
		}
	}

	@Override
	public RandomVariable sub(RandomVariable randomVariable) {
		if(randomVariable.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return randomVariable.bus(this);
		}

		// Set time of this random variable to maximum of time with respect to which measurability is known.
		double newTime = Math.max(time, randomVariable.getFiltrationTime());

		if(isDeterministic() && randomVariable.isDeterministic()) {
			double newValueIfNonStochastic = valueIfNonStochastic - randomVariable.get(0);
			return new RandomVariableFromDoubleArray(newTime, newValueIfNonStochastic);
		}
		else if(isDeterministic()) {
			double[] newRealizations = new double[Math.max(size(), randomVariable.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = valueIfNonStochastic - randomVariable.get(i);
			}
			return new RandomVariableFromDoubleArray(newTime, newRealizations);
		}
		else {
			double[] newRealizations = new double[Math.max(size(), randomVariable.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] - randomVariable.get(i);
			}
			return new RandomVariableFromDoubleArray(newTime, newRealizations);
		}
	}

	@Override
	public RandomVariable bus(RandomVariable randomVariable) {
		if(randomVariable.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return randomVariable.sub(this);
		}

		// Set time of this random variable to maximum of time with respect to which measurability is known.
		double newTime = Math.max(time, randomVariable.getFiltrationTime());

		if(isDeterministic() && randomVariable.isDeterministic()) {
			double newValueIfNonStochastic = randomVariable.get(0) - valueIfNonStochastic;
			return new RandomVariableFromDoubleArray(newTime, newValueIfNonStochastic);
		}
		else if(isDeterministic()) {
			double[] newRealizations = new double[Math.max(size(), randomVariable.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 =  randomVariable.get(i) - valueIfNonStochastic;
			}
			return new RandomVariableFromDoubleArray(newTime, newRealizations);
		}
		else {
			double[] newRealizations = new double[Math.max(size(), randomVariable.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = randomVariable.get(i) - realizations[i];
			}
			return new RandomVariableFromDoubleArray(newTime, newRealizations);
		}
	}

	@Override
	public RandomVariable mult(RandomVariable randomVariable) {
		if(randomVariable.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return randomVariable.mult(this);
		}

		// Set time of this random variable to maximum of time with respect to which measurability is known.
		double newTime = Math.max(time, randomVariable.getFiltrationTime());

		if(isDeterministic() && randomVariable.isDeterministic()) {
			double newValueIfNonStochastic = valueIfNonStochastic * randomVariable.get(0);
			return new RandomVariableFromDoubleArray(newTime, newValueIfNonStochastic);
		}
		else if(randomVariable.isDeterministic()) {
			return this.mult(randomVariable.get(0));
		}
		else if(isDeterministic()) {
			double[] newRealizations = new double[Math.max(size(), randomVariable.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = valueIfNonStochastic * randomVariable.get(i);
			}
			return new RandomVariableFromDoubleArray(newTime, newRealizations);
		}
		else {
			double[] newRealizations = new double[Math.max(size(), randomVariable.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] * randomVariable.get(i);
			}
			return new RandomVariableFromDoubleArray(newTime, newRealizations);
		}
	}

	@Override
	public RandomVariable div(RandomVariable randomVariable) {
		if(randomVariable.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return randomVariable.vid(this);
		}

		// Set time of this random variable to maximum of time with respect to which measurability is known.
		double newTime = Math.max(time, randomVariable.getFiltrationTime());

		if(isDeterministic() && randomVariable.isDeterministic()) {
			double newValueIfNonStochastic = valueIfNonStochastic / randomVariable.get(0);
			return new RandomVariableFromDoubleArray(newTime, newValueIfNonStochastic);
		}
		else if(isDeterministic()) {
			double[] newRealizations = new double[Math.max(size(), randomVariable.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = valueIfNonStochastic / randomVariable.get(i);
			}
			return new RandomVariableFromDoubleArray(newTime, newRealizations);
		}
		else {
			double[] newRealizations = new double[Math.max(size(), randomVariable.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] / randomVariable.get(i);
			}
			return new RandomVariableFromDoubleArray(newTime, newRealizations);
		}
	}

	@Override
	public RandomVariable vid(RandomVariable randomVariable) {
		if(randomVariable.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return randomVariable.div(this);
		}

		// Set time of this random variable to maximum of time with respect to which measurability is known.
		double newTime = Math.max(time, randomVariable.getFiltrationTime());

		if(isDeterministic() && randomVariable.isDeterministic()) {
			double newValueIfNonStochastic = randomVariable.get(0) / valueIfNonStochastic;
			return new RandomVariableFromDoubleArray(newTime, newValueIfNonStochastic);
		}
		else if(isDeterministic()) {
			double[] newRealizations = new double[Math.max(size(), randomVariable.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = randomVariable.get(i) / valueIfNonStochastic;
			}
			return new RandomVariableFromDoubleArray(newTime, newRealizations);
		}
		else {
			double[] newRealizations = new double[Math.max(size(), randomVariable.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = randomVariable.get(i) / realizations[i];
			}
			return new RandomVariableFromDoubleArray(newTime, newRealizations);
		}
	}

	@Override
	public RandomVariable cap(RandomVariable randomVariable) {
		if(randomVariable.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return randomVariable.cap(this);
		}

		// Set time of this random variable to maximum of time with respect to which measurability is known.
		double newTime = Math.max(time, randomVariable.getFiltrationTime());

		if(isDeterministic() && randomVariable.isDeterministic()) {
			double newValueIfNonStochastic = FastMath.min(valueIfNonStochastic, randomVariable.get(0));
			return new RandomVariableFromDoubleArray(newTime, newValueIfNonStochastic);
		}
		else if(isDeterministic()) {
			double[] newRealizations = new double[Math.max(size(), randomVariable.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = FastMath.min(valueIfNonStochastic, randomVariable.get(i));
			}
			return new RandomVariableFromDoubleArray(newTime, newRealizations);
		} else {
			double[] newRealizations = new double[Math.max(size(), randomVariable.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = FastMath.min(realizations[i], randomVariable.get(i));
			}
			return new RandomVariableFromDoubleArray(newTime, newRealizations);
		}
	}

	@Override
	public RandomVariable floor(RandomVariable randomVariable) {
		if(randomVariable.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return randomVariable.floor(this);
		}

		// Set time of this random variable to maximum of time with respect to which measurability is known.
		double newTime = Math.max(time, randomVariable.getFiltrationTime());

		if(isDeterministic() && randomVariable.isDeterministic()) {
			double newValueIfNonStochastic = FastMath.max(valueIfNonStochastic, randomVariable.get(0));
			return new RandomVariableFromDoubleArray(newTime, newValueIfNonStochastic);
		}
		else if(isDeterministic()) {
			double[] newRealizations = new double[Math.max(size(), randomVariable.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = FastMath.max(valueIfNonStochastic, randomVariable.get(i));
			}
			return new RandomVariableFromDoubleArray(newTime, newRealizations);
		} else {
			double[] newRealizations = new double[Math.max(size(), randomVariable.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = FastMath.max(realizations[i], randomVariable.get(i));
			}
			return new RandomVariableFromDoubleArray(newTime, newRealizations);
		}
	}

	@Override
	public RandomVariable accrue(RandomVariable rate, double periodLength) {
		if(rate.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return rate.mult(periodLength).add(1.0).mult(this);
		}

		// Set time of this random variable to maximum of time with respect to which measurability is known.
		double newTime = Math.max(time, rate.getFiltrationTime());

		if(isDeterministic() && rate.isDeterministic()) {
			double newValueIfNonStochastic = valueIfNonStochastic * (1 + rate.get(0) * periodLength);
			return new RandomVariableFromDoubleArray(newTime, newValueIfNonStochastic);
		}
		else if(isDeterministic() && !rate.isDeterministic()) {
			double[] newRealizations = new double[Math.max(size(), rate.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = valueIfNonStochastic * (1 + rate.get(i) * periodLength);
			}
			return new RandomVariableFromDoubleArray(newTime, newRealizations);
		}
		else if(!isDeterministic() && rate.isDeterministic()) {
			double rateValue = rate.get(0);
			double[] newRealizations = new double[Math.max(size(), rate.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] * (1 + rateValue * periodLength);
			}
			return new RandomVariableFromDoubleArray(newTime, newRealizations);
		}
		else {
			double[] newRealizations = new double[Math.max(size(), rate.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] * (1 + rate.get(i) * periodLength);
			}
			return new RandomVariableFromDoubleArray(newTime, newRealizations);
		}
	}

	@Override
	public RandomVariable discount(RandomVariable rate, double periodLength) {
		if(rate.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return rate.mult(periodLength).add(1.0).invert().mult(this);
		}

		// Set time of this random variable to maximum of time with respect to which measurability is known.
		double newTime = Math.max(time, rate.getFiltrationTime());

		if(isDeterministic() && rate.isDeterministic()) {
			double newValueIfNonStochastic = valueIfNonStochastic / (1 + rate.get(0) * periodLength);
			return new RandomVariableFromDoubleArray(newTime, newValueIfNonStochastic);
		}
		else if(isDeterministic() && !rate.isDeterministic()) {
			double[] newRealizations = new double[Math.max(size(), rate.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = valueIfNonStochastic / (1.0 + rate.get(i) * periodLength);
			}
			return new RandomVariableFromDoubleArray(newTime, newRealizations);
		}
		else if(!isDeterministic() && rate.isDeterministic()) {
			double rateValue = rate.get(0);
			double[] newRealizations = new double[Math.max(size(), rate.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] / (1.0 + rateValue * periodLength);
			}
			return new RandomVariableFromDoubleArray(newTime, newRealizations);
		}
		else {
			double[] newRealizations = new double[Math.max(size(), rate.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] / (1.0 + rate.get(i) * periodLength);
			}
			return new RandomVariableFromDoubleArray(newTime, newRealizations);
		}
	}

	/*
	 * Ternary operators: checking for return type priority.
	 * @TODO add checking for return type priority.
	 */

	@Override
	public RandomVariable choose(RandomVariable valueIfTriggerNonNegative, RandomVariable valueIfTriggerNegative) {
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
			int numberOfPaths = this.size();
			double[] newRealizations = new double[numberOfPaths];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i] = realizations[i] >= 0.0 ? valueIfTriggerNonNegative.get(i) : valueIfTriggerNegative.get(i);
			}
			return new RandomVariableFromDoubleArray(newTime, newRealizations);
		}
	}

	@Override
	public RandomVariable invert() {
		if(isDeterministic()) {
			double newValueIfNonStochastic = 1.0/valueIfNonStochastic;
			return new RandomVariableFromDoubleArray(time, newValueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = 1.0/realizations[i];
			}
			return new RandomVariableFromDoubleArray(time, newRealizations);
		}
	}

	@Override
	public RandomVariable abs() {
		if(isDeterministic()) {
			double newValueIfNonStochastic = Math.abs(valueIfNonStochastic);
			return new RandomVariableFromDoubleArray(time, newValueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[realizations.length];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = Math.abs(realizations[i]);
			}
			return new RandomVariableFromDoubleArray(time, newRealizations);
		}
	}

	@Override
	public RandomVariable addProduct(RandomVariable factor1, double factor2) {
		if(factor1.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return factor1.mult(factor2).add(this);
		}

		// Set time of this random variable to maximum of time with respect to which measurability is known.
		double newTime = Math.max(time, factor1.getFiltrationTime());

		if(isDeterministic() && factor1.isDeterministic()) {
			double newValueIfNonStochastic = valueIfNonStochastic + (factor1.get(0) * factor2);
			return new RandomVariableFromDoubleArray(newTime, newValueIfNonStochastic);
		}
		else if(isDeterministic() && !factor1.isDeterministic()) {
			double[] newRealizations = new double[Math.max(size(), factor1.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = valueIfNonStochastic + factor1.get(i) * factor2;
			}
			return new RandomVariableFromDoubleArray(newTime, newRealizations);
		}
		else if(!isDeterministic() && factor1.isDeterministic()) {
			double factor1Value = factor1.get(0);
			double[] newRealizations = new double[Math.max(size(), factor1.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] + factor1Value * factor2;
			}
			return new RandomVariableFromDoubleArray(newTime, newRealizations);
		}
		else {
			double[] newRealizations = new double[Math.max(size(), factor1.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] + factor1.get(i) * factor2;
			}
			return new RandomVariableFromDoubleArray(newTime, newRealizations);
		}
	}

	@Override
	public RandomVariable addProduct(RandomVariable factor1, RandomVariable factor2) {
		if(factor1.getTypePriority() > this.getTypePriority() || factor2.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return factor1.mult(factor2).add(this);
		}

		// Set time of this random variable to maximum of time with respect to which measurability is known.
		double newTime = Math.max(Math.max(time, factor1.getFiltrationTime()), factor2.getFiltrationTime());

		if(isDeterministic() && factor1.isDeterministic() && factor2.isDeterministic()) {
			double newValueIfNonStochastic = valueIfNonStochastic + (factor1.get(0) * factor2.get(0));
			return new RandomVariableFromDoubleArray(newTime, newValueIfNonStochastic);
		}
		else if(isDeterministic() && !factor1.isDeterministic() && !factor2.isDeterministic()) {
			double[] newRealizations = new double[Math.max(size(), factor1.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = valueIfNonStochastic + factor1.get(i) * factor2.get(i);
			}
			return new RandomVariableFromDoubleArray(newTime, newRealizations);
		}
		else if(!isDeterministic() && !factor1.isDeterministic() && !factor2.isDeterministic()) {
			double[] newRealizations = new double[Math.max(size(), factor1.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = realizations[i] + factor1.get(i) * factor2.get(i);
			}
			return new RandomVariableFromDoubleArray(newTime, newRealizations);
		}
		else {
			double[] newRealizations = new double[Math.max(Math.max(size(), factor1.size()), factor2.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = get(i) + factor1.get(i) * factor2.get(i);
			}
			return new RandomVariableFromDoubleArray(newTime, newRealizations);
		}
	}

	@Override
	public RandomVariable addSumProduct(List<RandomVariable> factor1, List<RandomVariable> factor2)
	{
		RandomVariable result = this;
		for(int i=0; i<factor1.size(); i++) {
			result = result.addProduct(factor1.get(i), factor2.get(i));
		}
		return result;
	}

	@Override
	public RandomVariable addRatio(RandomVariable numerator, RandomVariable denominator) {
		if(numerator.getTypePriority() > this.getTypePriority() || denominator.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return numerator.div(denominator).add(this);
		}

		// Set time of this random variable to maximum of time with respect to which measurability is known.
		double newTime = Math.max(Math.max(time, numerator.getFiltrationTime()), denominator.getFiltrationTime());

		if(isDeterministic() && numerator.isDeterministic() && denominator.isDeterministic()) {
			double newValueIfNonStochastic = valueIfNonStochastic + (numerator.get(0) / denominator.get(0));
			return new RandomVariableFromDoubleArray(newTime, newValueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[Math.max(Math.max(size(), numerator.size()), denominator.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = get(i) + numerator.get(i) / denominator.get(i);
			}
			return new RandomVariableFromDoubleArray(newTime, newRealizations);
		}
	}

	@Override
	public RandomVariable subRatio(RandomVariable numerator, RandomVariable denominator) {
		if(numerator.getTypePriority() > this.getTypePriority() || denominator.getTypePriority() > this.getTypePriority()) {
			// Check type priority
			return numerator.div(denominator).mult(-1).add(this);
		}

		// Set time of this random variable to maximum of time with respect to which measurability is known.
		double newTime = Math.max(Math.max(time, numerator.getFiltrationTime()), denominator.getFiltrationTime());

		if(isDeterministic() && numerator.isDeterministic() && denominator.isDeterministic()) {
			double newValueIfNonStochastic = valueIfNonStochastic - (numerator.get(0) / denominator.get(0));
			return new RandomVariableFromDoubleArray(newTime, newValueIfNonStochastic);
		}
		else {
			double[] newRealizations = new double[Math.max(Math.max(size(), numerator.size()), denominator.size())];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = get(i) - numerator.get(i) / denominator.get(i);
			}
			return new RandomVariableFromDoubleArray(newTime, newRealizations);
		}
	}

	@Override
	public RandomVariable isNaN() {
		if(isDeterministic()) {
			return new RandomVariableFromDoubleArray(time, Double.isNaN(valueIfNonStochastic) ? 1.0 : 0.0);
		}
		else {
			double[] newRealizations = new double[size()];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = Double.isNaN(get(i)) ? 1.0 : 0.0;
			}
			return new RandomVariableFromDoubleArray(time, newRealizations);
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
