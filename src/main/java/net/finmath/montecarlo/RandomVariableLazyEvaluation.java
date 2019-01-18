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
import java.util.stream.IntStream;

import org.apache.commons.math3.util.FastMath;

import net.finmath.functions.DoubleTernaryOperator;
import net.finmath.stochastic.RandomVariable;

/**
 * Implements a Monte-Carlo random variable (like <code>RandomVariableFromDoubleArray</code> using
 * late evaluation of Java 8 streams
 *
 * Accesses performed exclusively through the interface
 * <code>RandomVariable</code> is thread safe (and does not mutate the class).
 *
 * The implementation require Java 8 or better.
 *
 * @TODO The implementation of getAverage does not use a Kahan summation, while <code>RandomVariableFromDoubleArray</code> does.
 *
 * @author Christian Fries
 * @author OSC
 * @version 2.0
 */
public class RandomVariableLazyEvaluation implements RandomVariable {

	/**
	 *
	 */
	private static final long serialVersionUID = 8413020544732461630L;

	private final double            time;	                // Time (filtration)

	// Operator
	private       IntToDoubleFunction   realizations;
	private final int                   size;
	// Data model for the non-stochastic case (if realizations==null)
	private final double                valueIfNonStochastic;

	private transient double[] realizationsArray = null;

	/**
	 * Create a random variable from a given other implementation of <code>RandomVariable</code>.
	 *
	 * @param value Object implementing <code>RandomVariable</code>.
	 */
	public RandomVariableLazyEvaluation(RandomVariable value) {
		super();
		this.time = value.getFiltrationTime();
		this.realizations = value.isDeterministic() ? null : value::get;
		this.size = value.size();
		this.valueIfNonStochastic = value.isDeterministic() ? value.get(0) : Double.NaN;
	}

	/**
	 * Create a non stochastic random variable, i.e. a constant.
	 *
	 * @param value the value, a constant.
	 */
	public RandomVariableLazyEvaluation(double value) {
		this(0.0, value);
	}

	/**
	 * Create a random variable by applying a function to a given other implementation of <code>RandomVariable</code>.
	 *
	 * @param value Object implementing <code>RandomVariable</code>.
	 * @param function A function mapping double to double.
	 */
	public RandomVariableLazyEvaluation(RandomVariable value, DoubleUnaryOperator function) {
		super();
		this.time = value.getFiltrationTime();
		this.realizations = value.isDeterministic() ? null : i -> function.applyAsDouble(value.get(i));
		this.size = value.size();
		this.valueIfNonStochastic = value.isDeterministic() ? function.applyAsDouble(value.get(0)) : Double.NaN;
	}

	/**
	 * Create a non stochastic random variable, i.e. a constant.
	 *
	 * @param time the filtration time, set to 0.0 if not used.
	 * @param value the value, a constant.
	 */
	public RandomVariableLazyEvaluation(double time, double value) {
		super();
		this.time = time;
		this.realizations = null;
		this.size = 1;
		this.valueIfNonStochastic = value;
	}

	/**
	 * Create a non stochastic random variable, i.e. a constant.
	 *
	 * @param time the filtration time, set to 0.0 if not used.
	 * @param numberOfPath The number of path/state of the associated Monte-Carlo simulation or lattice.
	 * @param value the value, a constant.
	 */
	public RandomVariableLazyEvaluation(double time, int numberOfPath, double value) {
		super();
		this.time = time;
		this.size = numberOfPath;
		this.realizations = i -> value;
		this.valueIfNonStochastic = Double.NaN;
	}

	/**
	 * Create a stochastic random variable.
	 *
	 * @param time the filtration time, set to 0.0 if not used.
	 * @param realisations the vector of realizations.
	 */
	public RandomVariableLazyEvaluation(double time, double[] realisations) {
		super();
		this.time = time;
		this.size = realisations.length;
		this.realizations = i->realisations[i];
		this.valueIfNonStochastic = Double.NaN;
		this.realizationsArray = realisations;
	}

	/**
	 * Create a stochastic random variable.
	 *
	 * @param time the filtration time, set to 0.0 if not used.
	 * @param realisations the vector of realizations.
	 * @param size The number of path/state of the associated Monte-Carlo simulation or lattice.
	 */
	public RandomVariableLazyEvaluation(double time, IntToDoubleFunction realisations, int size) {
		super();
		this.time = time;
		this.realizations = realisations;
		this.size = size;
		this.valueIfNonStochastic = Double.NaN;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#equals(net.finmath.montecarlo.RandomVariableFromDoubleArray)
	 */
	@Override
	public boolean equals(RandomVariable randomVariable) {
		if(this.time != randomVariable.getFiltrationTime()) {
			return false;
		}
		if(this.isDeterministic() && randomVariable.isDeterministic()) {
			return this.valueIfNonStochastic == randomVariable.get(0);
		}

		if(this.isDeterministic() != randomVariable.isDeterministic()) {
			return false;
		}

		for(int i=0; i<size(); i++) {
			if(get(i) != randomVariable.get(i)) {
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
		return 0;
	}


	@Override
	public double get(int pathOrState) {
		if(isDeterministic()) {
			return valueIfNonStochastic;
		} else {
			cache();
			return realizationsArray[pathOrState];
			//            return realizations.applyAsDouble(pathOrState);

		}
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#size()
	 */
	@Override
	public int size() {
		return size;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#getMin()
	 */
	@Override
	public double getMin() {
		if(isDeterministic()) {
			return valueIfNonStochastic;
		}
		return getRealizationsStream().min().getAsDouble();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#getMax()
	 */
	@Override
	public double getMax() {
		if(isDeterministic()) {
			return valueIfNonStochastic;
		}
		return getRealizationsStream().max().getAsDouble();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#getAverage()
	 */
	@Override
	public double getAverage() {
		if(isDeterministic()) {
			return valueIfNonStochastic;
		}
		if(size() == 0) {
			return Double.NaN;
		}

		return getRealizationsStream().sum()/size();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#getAverage(net.finmath.stochastic.RandomVariable)
	 */
	@Override
	public double getAverage(RandomVariable probabilities) {
		if(isDeterministic()) {
			return valueIfNonStochastic;
		}
		if(size() == 0) {
			return Double.NaN;
		}

		return this.cache().mult(probabilities).getRealizationsStream().sum();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#getVariance()
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
		for (double realization : getRealizations()) {
			sum += realization;
			sumOfSquared += realization * realization;
		}
		return sumOfSquared/size() - sum/size() * sum/size();
	}

	@Override
	public double getVariance(RandomVariable probabilities) {
		if(isDeterministic()) {
			return 0.0;
		}
		if(size() == 0) {
			return Double.NaN;
		}

		double mean			= 0.0;
		double secondMoment = 0.0;
		for(int i=0; i<size(); i++) {
			mean			+= get(i) * probabilities.get(i);
			secondMoment	+= get(i) * get(i) * probabilities.get(i);
		}
		return secondMoment - mean*mean;
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

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#getStandardDeviation(net.finmath.stochastic.RandomVariable)
	 */
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

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#getStandardError()
	 */
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

		return getStandardDeviation(probabilities)/size();
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

		double[] realizationsSorted = getRealizations().clone();
		Arrays.sort(realizationsSorted);

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

		double[] realizationsSorted = getRealizations().clone();
		Arrays.sort(realizationsSorted);

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
			Arrays.fill(histogramValues, 0.0);
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
			double[] realizationsSorted = getRealizations().clone();
			Arrays.sort(realizationsSorted);

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

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#expand()
	 */
	public RandomVariable expand(int numberOfPaths) {
		if(isDeterministic()) {
			// Expand random variable to a vector of path values
			double[] clone = new double[numberOfPaths];
			Arrays.fill(clone,valueIfNonStochastic);
			return new RandomVariableFromDoubleArray(time,clone);

		}

		return new RandomVariableFromDoubleArray(time,getRealizations());
	}


	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#getRealizations()
	 */
	@Override
	public double[] getRealizations() {
		if(isDeterministic()) {
			double[] result = new double[1];
			result[0] = get(0);
			return result;
		}
		else {
			cache();

			return realizationsArray;
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

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#getOperator()
	 */
	@Override
	public IntToDoubleFunction getOperator() {
		return realizations;
	}

	public RandomVariableFromDoubleArray getRandomVariable() {
		if(isDeterministic()) {
			return new RandomVariableFromDoubleArray(time, valueIfNonStochastic);
		} else {
			return new RandomVariableFromDoubleArray(time, getRealizations());
		}
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#getRealizationsStream()
	 */
	@Override
	public DoubleStream getRealizationsStream() {
		if(isDeterministic()) {
			return DoubleStream.generate(() -> valueIfNonStochastic);
		}
		else {
			return IntStream.range(0,size()).mapToDouble(realizations).parallel();
		}
	}

	@Override
	public RandomVariable apply(DoubleUnaryOperator operator) {
		if(isDeterministic()) {
			return new RandomVariableLazyEvaluation(time, operator.applyAsDouble(valueIfNonStochastic));
		}
		else
		{
			IntToDoubleFunction newRealizations = i -> operator.applyAsDouble(realizations.applyAsDouble(i));
			return new RandomVariableLazyEvaluation(time, newRealizations, size());
		}
	}

	@Override
	public RandomVariable cache() {
		synchronized (this)
		{
			if(realizationsArray == null) {
				realizationsArray = getRealizationsStream().toArray();
				realizations = i -> realizationsArray[i];
			}
		}
		return this;
	}

	@Override
	public RandomVariable apply(DoubleBinaryOperator operator, final RandomVariable argument) {

		double      newTime           = Math.max(time, argument.getFiltrationTime());

		if(isDeterministic() && argument.isDeterministic()) {
			return new RandomVariableLazyEvaluation(newTime, operator.applyAsDouble(valueIfNonStochastic, argument.get(0)));
		}
		else if(!isDeterministic() && argument.isDeterministic()) {
			IntToDoubleFunction newRealizations = i -> operator.applyAsDouble(realizations.applyAsDouble(i), argument.get(0));
			return new RandomVariableLazyEvaluation(newTime, newRealizations, size());
		}
		else if(isDeterministic() && !argument.isDeterministic()) {
			if(false) {
				final IntToDoubleFunction argumentRealizations = argument.getOperator();
				IntToDoubleFunction newRealizations = i -> operator.applyAsDouble(valueIfNonStochastic, argumentRealizations.applyAsDouble(i));
				return new RandomVariableLazyEvaluation(newTime, newRealizations, argument.size());
			}
			else {
				final double[] argumentRealizations = argument.getRealizations();
				IntToDoubleFunction newRealizations = i -> operator.applyAsDouble(valueIfNonStochastic, argumentRealizations[i]);
				return new RandomVariableLazyEvaluation(newTime, newRealizations, argument.size());
			}
		}
		else
		{
			if(false) {
				final IntToDoubleFunction argumentRealizations = argument.getOperator();
				IntToDoubleFunction newRealizations = i -> operator.applyAsDouble(realizations.applyAsDouble(i), argumentRealizations.applyAsDouble(i));
				return new RandomVariableLazyEvaluation(newTime, newRealizations, size());
			}
			else {
				final double[] argumentRealizations = argument.getRealizations();
				IntToDoubleFunction newRealizations = i -> operator.applyAsDouble(realizations.applyAsDouble(i), argumentRealizations[i]);
				return new RandomVariableLazyEvaluation(newTime, newRealizations, size());
			}
		}

	}

	public RandomVariable apply(DoubleBinaryOperator operatorOuter, DoubleBinaryOperator operatorInner, RandomVariable argument1, RandomVariable argument2)
	{
		double newTime = Math.max(time, argument1.getFiltrationTime());
		newTime = Math.max(newTime, argument2.getFiltrationTime());

		if(this.isDeterministic() && argument1.isDeterministic() && argument2.isDeterministic()) {
			return new RandomVariableLazyEvaluation(newTime, operatorOuter.applyAsDouble(valueIfNonStochastic, operatorInner.applyAsDouble(argument1.get(0), argument2.get(0))));
		}
		else {
			int newSize = Math.max(Math.max(this.size(), argument1.size()), argument2.size());

			if(false) {
				if(argument1.isDeterministic() && argument2.isDeterministic()) {
					final double	argument1Realization = argument1.get(0);
					final double	argument2Realization = argument2.get(0);
					final double	innerResult = operatorInner.applyAsDouble(argument1Realization, argument2Realization);
					return new RandomVariableLazyEvaluation(newTime,(int i) -> operatorOuter.applyAsDouble(realizations.applyAsDouble(i), innerResult), newSize);
				}
				else {
					return new RandomVariableLazyEvaluation(newTime,(int i) -> operatorOuter.applyAsDouble(realizations.applyAsDouble(i), operatorInner.applyAsDouble(argument1.get(i), argument2.get(i))), newSize);
				}
			}
			else {
				IntToDoubleFunction innerResult;
				if(argument1.isDeterministic() && argument2.isDeterministic()) {
					final double	argument1Realization = argument1.get(0);
					final double	argument2Realization = argument2.get(0);
					innerResult = i -> operatorInner.applyAsDouble(argument1Realization, argument2Realization);
				}
				else if(argument1.isDeterministic() && !argument2.isDeterministic()) {
					final double	argument1Realization	= argument1.get(0);
					final double[]	argument2Realizations	= argument2.getRealizations();
					innerResult = i -> operatorInner.applyAsDouble(argument1Realization, argument2Realizations[i]);
				}
				else if(!argument1.isDeterministic() && argument2.isDeterministic()) {
					final double[]	argument1Realizations	= argument1.getRealizations();
					final double	argument2Realization	= argument2.get(0);
					innerResult = i -> operatorInner.applyAsDouble(argument1Realizations[i], argument2Realization);
				}
				else {// if(!argument1.isDeterministic() && !argument2.isDeterministic()) {
					final double[]	argument1Realizations	= argument1.getRealizations();
					final double[]	argument2Realizations	= argument2.getRealizations();
					innerResult = i -> operatorInner.applyAsDouble(argument1Realizations[i], argument2Realizations[i]);
				}

				if(isDeterministic()) {
					return new RandomVariableLazyEvaluation(newTime,(int i) -> operatorOuter.applyAsDouble(valueIfNonStochastic,          innerResult.applyAsDouble(i)), newSize);
				}
				else {
					return new RandomVariableLazyEvaluation(newTime,(int i) -> operatorOuter.applyAsDouble(realizations.applyAsDouble(i), innerResult.applyAsDouble(i)), newSize);
				}

			}
		}
	}

	@Override
	public RandomVariable apply(DoubleTernaryOperator operator, RandomVariable argument1, RandomVariable argument2)
	{
		double newTime = Math.max(time, argument1.getFiltrationTime());
		newTime = Math.max(newTime, argument2.getFiltrationTime());

		if(this.isDeterministic() && argument1.isDeterministic() && argument2.isDeterministic()) {
			return new RandomVariableLazyEvaluation(newTime, operator.applyAsDouble(valueIfNonStochastic, argument1.get(0), argument2.get(0)));
		}
		else {
			int newSize = Math.max(Math.max(this.size(), argument1.size()), argument2.size());
			IntToDoubleFunction result;
			if(argument1.isDeterministic() && argument2.isDeterministic()) {
				final double	argument1Realization = argument1.get(0);
				final double	argument2Realization = argument2.get(0);
				if(isDeterministic()) {
					result = i -> operator.applyAsDouble(valueIfNonStochastic, argument1Realization, argument2Realization);
				}
				else {
					result = i -> operator.applyAsDouble(realizations.applyAsDouble(i), argument1Realization, argument2Realization);

				}
			}
			else if(argument1.isDeterministic() && !argument2.isDeterministic()) {
				final double	argument1Realization	= argument1.get(0);
				final double[]	argument2Realizations	= argument2.getRealizations();
				if(isDeterministic()) {
					result = i -> operator.applyAsDouble(valueIfNonStochastic, argument1Realization, argument2Realizations[i]);
				}
				else {
					result = i -> operator.applyAsDouble(realizations.applyAsDouble(i), argument1Realization, argument2Realizations[i]);

				}
			}
			else if(!argument1.isDeterministic() && argument2.isDeterministic()) {
				final double[]	argument1Realizations	= argument1.getRealizations();
				final double	argument2Realization	= argument2.get(0);
				if(isDeterministic()) {
					result = i -> operator.applyAsDouble(valueIfNonStochastic, argument1Realizations[i], argument2Realization);
				}
				else {
					result = i -> operator.applyAsDouble(realizations.applyAsDouble(i), argument1Realizations[i], argument2Realization);

				}
			}
			else {// if(!argument1.isDeterministic() && !argument2.isDeterministic()) {
				final double[]	argument1Realizations	= argument1.getRealizations();
				final double[]	argument2Realizations	= argument2.getRealizations();
				if(isDeterministic()) {
					result = i -> operator.applyAsDouble(valueIfNonStochastic, argument1Realizations[i], argument2Realizations[i]);
				}
				else {
					result = i -> operator.applyAsDouble(realizations.applyAsDouble(i), argument1Realizations[i], argument2Realizations[i]);

				}
			}

			return new RandomVariableLazyEvaluation(newTime, result, newSize);
		}
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#cap(double)
	 */
	@Override
	public RandomVariable cap(double cap) {
		return apply(x -> Math.min(x, cap));
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#floor(double)
	 */
	@Override
	public RandomVariable floor(double floor) {
		return apply(x -> Math.max(x, floor));
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#add(double)
	 */
	@Override
	public RandomVariable add(double value) {
		return apply(x -> x + value);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#sub(double)
	 */
	@Override
	public RandomVariable sub(double value) {
		return apply(x -> x - value);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#mult(double)
	 */
	@Override
	public RandomVariable mult(double value) {
		return apply(x -> x * value);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#div(double)
	 */
	@Override
	public RandomVariable div(double value) {
		return apply(x -> x / value);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#pow(double)
	 */
	@Override
	public RandomVariable pow(double exponent) {
		return apply(x -> FastMath.pow(x, exponent));
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#average()
	 */
	@Override
	public RandomVariable average() {
		return new RandomVariableLazyEvaluation(getAverage());
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#squared()
	 */
	@Override
	public RandomVariable squared() {
		return apply(x -> x * x);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#sqrt()
	 */
	@Override
	public RandomVariable sqrt() {
		return apply(FastMath::sqrt);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#exp()
	 */
	@Override
	public RandomVariable exp() {
		return apply(FastMath::exp);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#log()
	 */
	@Override
	public RandomVariable log() {
		return apply(FastMath::log);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#sin()
	 */
	@Override
	public RandomVariable sin() {
		return apply(FastMath::sin);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#cos()
	 */
	@Override
	public RandomVariable cos() {
		return apply(FastMath::cos);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#add(net.finmath.stochastic.RandomVariable)
	 */
	@Override
	public RandomVariable add(RandomVariable randomVariable) {
		return apply((x, y) -> x + y, randomVariable);

	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#sub(net.finmath.stochastic.RandomVariable)
	 */
	@Override
	public RandomVariable sub(RandomVariable randomVariable) {
		return apply((x, y) -> x - y, randomVariable);
	}

	@Override
	public RandomVariable bus(RandomVariable randomVariable) {
		return apply((x, y) -> -x + y, randomVariable);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#mult(net.finmath.stochastic.RandomVariable)
	 */
	@Override
	public RandomVariable mult(RandomVariable randomVariable) {
		return apply((x, y) -> x * y, randomVariable);
	}

	@Override
	public RandomVariable div(RandomVariable randomVariable) {
		return apply((x, y) -> x / y, randomVariable);
	}

	@Override
	public RandomVariable vid(RandomVariable randomVariable) {
		return apply((x, y) -> y / x, randomVariable);
	}

	@Override
	public RandomVariable cap(RandomVariable cap) {
		return apply(FastMath::min, cap);
	}

	@Override
	public RandomVariable floor(RandomVariable floor) {
		return apply(FastMath::max, floor);
	}

	@Override
	public RandomVariable accrue(RandomVariable rate, double periodLength) {
		return apply((x, y) -> x * (1.0 + y * periodLength), rate);
	}

	@Override
	public RandomVariable discount(RandomVariable rate, double periodLength) {
		return apply((x, y) -> x / (1.0 + y * periodLength), rate);
	}

	@Override
	public RandomVariable choose(RandomVariable valueIfTriggerNonNegative, RandomVariable valueIfTriggerNegative) {
		return apply(( x, y, z) -> (x >= 0 ? y : z), valueIfTriggerNonNegative, valueIfTriggerNegative);
	}

	@Override
	public RandomVariable invert() {
		return apply(x -> 1.0 / x);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#abs()
	 */
	@Override
	public RandomVariable abs() {
		return apply(Math::abs);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#addProduct(net.finmath.stochastic.RandomVariable, double)
	 */
	@Override
	public RandomVariable addProduct(final RandomVariable factor1, final double factor2) {
		return apply((x, y) -> x + y * factor2, factor1);
	}


	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#addProduct(net.finmath.stochastic.RandomVariable, net.finmath.stochastic.RandomVariable)
	 */
	@Override
	public RandomVariable addProduct(RandomVariable factor1, RandomVariable factor2) {
		return apply((x,y) -> x + y, (x, y) -> x * y, factor1, factor2);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#addRatio(net.finmath.stochastic.RandomVariable, net.finmath.stochastic.RandomVariable)
	 */
	@Override
	public RandomVariable addRatio(RandomVariable numerator, RandomVariable denominator) {
		return apply((x, y) -> x + y, (x, y) -> x / y, numerator, denominator);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#subRatio(net.finmath.stochastic.RandomVariable, net.finmath.stochastic.RandomVariable)
	 */
	@Override
	public RandomVariable subRatio(RandomVariable numerator, RandomVariable denominator) {
		return apply((x,y) -> x - y, (x, y) -> x / y, numerator, denominator);
	}

	@Override
	public RandomVariable isNaN() {
		if(isDeterministic()) {
			return new RandomVariableLazyEvaluation(time, Double.isNaN(valueIfNonStochastic) ? 1.0 : 0.0);
		}
		else {
			double[] newRealizations = new double[size()];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = Double.isNaN(get(i)) ? 1.0 : 0.0;
			}
			return new RandomVariableLazyEvaluation(time, newRealizations);
		}
	}
}
