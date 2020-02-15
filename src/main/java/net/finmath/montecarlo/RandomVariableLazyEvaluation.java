/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2006
 */
package net.finmath.montecarlo;

import java.util.Arrays;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntToDoubleFunction;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

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
	public RandomVariableLazyEvaluation(final RandomVariable value) {
		super();
		time = value.getFiltrationTime();
		realizations = value.isDeterministic() ? null : value::get;
		size = value.size();
		valueIfNonStochastic = value.isDeterministic() ? value.get(0) : Double.NaN;
	}

	/**
	 * Create a non stochastic random variable, i.e. a constant.
	 *
	 * @param value the value, a constant.
	 */
	public RandomVariableLazyEvaluation(final double value) {
		this(0.0, value);
	}

	/**
	 * Create a random variable by applying a function to a given other implementation of <code>RandomVariable</code>.
	 *
	 * @param value Object implementing <code>RandomVariable</code>.
	 * @param function A function mapping double to double.
	 */
	public RandomVariableLazyEvaluation(final RandomVariable value, final DoubleUnaryOperator function) {
		super();
		time = value.getFiltrationTime();
		realizations = value.isDeterministic() ? null : new IntToDoubleFunction() {
			@Override
			public double applyAsDouble(final int i) {
				return function.applyAsDouble(value.get(i));
			}
		};
		size = value.size();
		valueIfNonStochastic = value.isDeterministic() ? function.applyAsDouble(value.get(0)) : Double.NaN;
	}

	/**
	 * Create a non stochastic random variable, i.e. a constant.
	 *
	 * @param time the filtration time, set to 0.0 if not used.
	 * @param value the value, a constant.
	 */
	public RandomVariableLazyEvaluation(final double time, final double value) {
		super();
		this.time = time;
		realizations = null;
		size = 1;
		valueIfNonStochastic = value;
	}

	/**
	 * Create a non stochastic random variable, i.e. a constant.
	 *
	 * @param time the filtration time, set to 0.0 if not used.
	 * @param numberOfPath The number of path/state of the associated Monte-Carlo simulation or lattice.
	 * @param value the value, a constant.
	 */
	public RandomVariableLazyEvaluation(final double time, final int numberOfPath, final double value) {
		super();
		this.time = time;
		size = numberOfPath;
		realizations = new IntToDoubleFunction() {
			@Override
			public double applyAsDouble(final int i) {
				return value;
			}
		};
		valueIfNonStochastic = Double.NaN;
	}

	/**
	 * Create a stochastic random variable.
	 *
	 * @param time the filtration time, set to 0.0 if not used.
	 * @param realisations the vector of realizations.
	 */
	public RandomVariableLazyEvaluation(final double time, final double[] realisations) {
		super();
		this.time = time;
		size = realisations.length;
		realizations = new IntToDoubleFunction() {
			@Override
			public double applyAsDouble(final int i) {
				return realisations[i];
			}
		};
		valueIfNonStochastic = Double.NaN;
		realizationsArray = realisations;
	}

	/**
	 * Create a stochastic random variable.
	 *
	 * @param time the filtration time, set to 0.0 if not used.
	 * @param realisations the vector of realizations.
	 * @param size The number of path/state of the associated Monte-Carlo simulation or lattice.
	 */
	public RandomVariableLazyEvaluation(final double time, final IntToDoubleFunction realisations, final int size) {
		super();
		this.time = time;
		realizations = realisations;
		this.size = size;
		valueIfNonStochastic = Double.NaN;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#equals(net.finmath.montecarlo.RandomVariableFromDoubleArray)
	 */
	@Override
	public boolean equals(final RandomVariable randomVariable) {
		if(time != randomVariable.getFiltrationTime()) {
			return false;
		}
		if(this.isDeterministic() && randomVariable.isDeterministic()) {
			return valueIfNonStochastic == randomVariable.get(0);
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
	public double get(final int pathOrState) {
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
	public double getAverage(final RandomVariable probabilities) {
		if(isDeterministic()) {
			return valueIfNonStochastic * probabilities.getAverage();
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
		for (final double realization : getRealizations()) {
			sum += realization;
			sumOfSquared += realization * realization;
		}
		return sumOfSquared/size() - sum/size() * sum/size();
	}

	@Override
	public double getVariance(final RandomVariable probabilities) {
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
	public double getStandardDeviation(final RandomVariable probabilities) {
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
	public double getStandardError(final RandomVariable probabilities) {
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
	public double getQuantile(final double quantile) {
		if(isDeterministic()) {
			return valueIfNonStochastic;
		}
		if(size() == 0) {
			return Double.NaN;
		}

		final double[] realizationsSorted = getRealizations().clone();
		Arrays.sort(realizationsSorted);

		final int indexOfQuantileValue = Math.min(Math.max((int)Math.round((size()+1) * quantile - 1), 0), size()-1);

		return realizationsSorted[indexOfQuantileValue];
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#getQuantile(net.finmath.stochastic.RandomVariable)
	 */
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

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#getConditionalVaR()
	 */
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

		final double[] realizationsSorted = getRealizations().clone();
		Arrays.sort(realizationsSorted);

		final int indexOfQuantileValueStart	= Math.min(Math.max((int)Math.round((size()+1) * quantileStart - 1), 0), size()-1);
		final int indexOfQuantileValueEnd		= Math.min(Math.max((int)Math.round((size()+1) * quantileEnd - 1), 0), size()-1);

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
	public double[] getHistogram(final double[] intervalPoints)
	{
		final double[] histogramValues = new double[intervalPoints.length+1];

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
			final double[] realizationsSorted = getRealizations().clone();
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
	public RandomVariable expand(final int numberOfPaths) {
		if(isDeterministic()) {
			// Expand random variable to a vector of path values
			final double[] clone = new double[numberOfPaths];
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
			final double[] result = new double[1];
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
			return DoubleStream.generate(new DoubleSupplier() {
				@Override
				public double getAsDouble() {
					return valueIfNonStochastic;
				}
			});
		}
		else {
			return IntStream.range(0,size()).mapToDouble(realizations).parallel();
		}
	}

	@Override
	public RandomVariable apply(final DoubleUnaryOperator operator) {
		if(isDeterministic()) {
			return new RandomVariableLazyEvaluation(time, operator.applyAsDouble(valueIfNonStochastic));
		}
		else
		{
			final IntToDoubleFunction newRealizations = new IntToDoubleFunction() {
				@Override
				public double applyAsDouble(final int i) {
					return operator.applyAsDouble(realizations.applyAsDouble(i));
				}
			};
			return new RandomVariableLazyEvaluation(time, newRealizations, size());
		}
	}

	@Override
	public RandomVariable cache() {
		synchronized (this)
		{
			if(realizationsArray == null) {
				realizationsArray = getRealizationsStream().toArray();
				realizations = new IntToDoubleFunction() {
					@Override
					public double applyAsDouble(final int i) {
						return realizationsArray[i];
					}
				};
			}
		}
		return this;
	}

	@Override
	public RandomVariable apply(final DoubleBinaryOperator operator, final RandomVariable argument) {

		final double      newTime           = Math.max(time, argument.getFiltrationTime());

		if(isDeterministic() && argument.isDeterministic()) {
			return new RandomVariableLazyEvaluation(newTime, operator.applyAsDouble(valueIfNonStochastic, argument.get(0)));
		}
		else if(!isDeterministic() && argument.isDeterministic()) {
			final IntToDoubleFunction newRealizations = new IntToDoubleFunction() {
				@Override
				public double applyAsDouble(final int i) {
					return operator.applyAsDouble(realizations.applyAsDouble(i), argument.get(0));
				}
			};
			return new RandomVariableLazyEvaluation(newTime, newRealizations, size());
		}
		else if(isDeterministic() && !argument.isDeterministic()) {
			if(false) {
				final IntToDoubleFunction argumentRealizations = argument.getOperator();
				final IntToDoubleFunction newRealizations = new IntToDoubleFunction() {
					@Override
					public double applyAsDouble(final int i) {
						return operator.applyAsDouble(valueIfNonStochastic, argumentRealizations.applyAsDouble(i));
					}
				};
				return new RandomVariableLazyEvaluation(newTime, newRealizations, argument.size());
			}
			else {
				final double[] argumentRealizations = argument.getRealizations();
				final IntToDoubleFunction newRealizations = new IntToDoubleFunction() {
					@Override
					public double applyAsDouble(final int i) {
						return operator.applyAsDouble(valueIfNonStochastic, argumentRealizations[i]);
					}
				};
				return new RandomVariableLazyEvaluation(newTime, newRealizations, argument.size());
			}
		}
		else
		{
			if(false) {
				final IntToDoubleFunction argumentRealizations = argument.getOperator();
				final IntToDoubleFunction newRealizations = new IntToDoubleFunction() {
					@Override
					public double applyAsDouble(final int i) {
						return operator.applyAsDouble(realizations.applyAsDouble(i), argumentRealizations.applyAsDouble(i));
					}
				};
				return new RandomVariableLazyEvaluation(newTime, newRealizations, size());
			}
			else {
				final double[] argumentRealizations = argument.getRealizations();
				final IntToDoubleFunction newRealizations = new IntToDoubleFunction() {
					@Override
					public double applyAsDouble(final int i) {
						return operator.applyAsDouble(realizations.applyAsDouble(i), argumentRealizations[i]);
					}
				};
				return new RandomVariableLazyEvaluation(newTime, newRealizations, size());
			}
		}

	}

	public RandomVariable apply(final DoubleBinaryOperator operatorOuter, final DoubleBinaryOperator operatorInner, final RandomVariable argument1, final RandomVariable argument2)
	{
		double newTime = Math.max(time, argument1.getFiltrationTime());
		newTime = Math.max(newTime, argument2.getFiltrationTime());

		if(this.isDeterministic() && argument1.isDeterministic() && argument2.isDeterministic()) {
			return new RandomVariableLazyEvaluation(newTime, operatorOuter.applyAsDouble(valueIfNonStochastic, operatorInner.applyAsDouble(argument1.get(0), argument2.get(0))));
		}
		else {
			final int newSize = Math.max(Math.max(this.size(), argument1.size()), argument2.size());

			if(false) {
				if(argument1.isDeterministic() && argument2.isDeterministic()) {
					final double	argument1Realization = argument1.get(0);
					final double	argument2Realization = argument2.get(0);
					final double	innerResult = operatorInner.applyAsDouble(argument1Realization, argument2Realization);
					return new RandomVariableLazyEvaluation(newTime,new IntToDoubleFunction() {
						@Override
						public double applyAsDouble(final int i) {
							return operatorOuter.applyAsDouble(realizations.applyAsDouble(i), innerResult);
						}
					}, newSize);
				}
				else {
					return new RandomVariableLazyEvaluation(newTime,new IntToDoubleFunction() {
						@Override
						public double applyAsDouble(final int i) {
							return operatorOuter.applyAsDouble(realizations.applyAsDouble(i), operatorInner.applyAsDouble(argument1.get(i), argument2.get(i)));
						}
					}, newSize);
				}
			}
			else {
				IntToDoubleFunction innerResult;
				if(argument1.isDeterministic() && argument2.isDeterministic()) {
					final double	argument1Realization = argument1.get(0);
					final double	argument2Realization = argument2.get(0);
					innerResult = new IntToDoubleFunction() {
						@Override
						public double applyAsDouble(final int i) {
							return operatorInner.applyAsDouble(argument1Realization, argument2Realization);
						}
					};
				}
				else if(argument1.isDeterministic() && !argument2.isDeterministic()) {
					final double	argument1Realization	= argument1.get(0);
					final double[]	argument2Realizations	= argument2.getRealizations();
					innerResult = new IntToDoubleFunction() {
						@Override
						public double applyAsDouble(final int i) {
							return operatorInner.applyAsDouble(argument1Realization, argument2Realizations[i]);
						}
					};
				}
				else if(!argument1.isDeterministic() && argument2.isDeterministic()) {
					final double[]	argument1Realizations	= argument1.getRealizations();
					final double	argument2Realization	= argument2.get(0);
					innerResult = new IntToDoubleFunction() {
						@Override
						public double applyAsDouble(final int i) {
							return operatorInner.applyAsDouble(argument1Realizations[i], argument2Realization);
						}
					};
				}
				else {// if(!argument1.isDeterministic() && !argument2.isDeterministic()) {
					final double[]	argument1Realizations	= argument1.getRealizations();
					final double[]	argument2Realizations	= argument2.getRealizations();
					innerResult = new IntToDoubleFunction() {
						@Override
						public double applyAsDouble(final int i) {
							return operatorInner.applyAsDouble(argument1Realizations[i], argument2Realizations[i]);
						}
					};
				}

				if(isDeterministic()) {
					return new RandomVariableLazyEvaluation(newTime,new IntToDoubleFunction() {
						@Override
						public double applyAsDouble(final int i) {
							return operatorOuter.applyAsDouble(valueIfNonStochastic,          innerResult.applyAsDouble(i));
						}
					}, newSize);
				}
				else {
					return new RandomVariableLazyEvaluation(newTime,new IntToDoubleFunction() {
						@Override
						public double applyAsDouble(final int i) {
							return operatorOuter.applyAsDouble(realizations.applyAsDouble(i), innerResult.applyAsDouble(i));
						}
					}, newSize);
				}

			}
		}
	}

	@Override
	public RandomVariable apply(final DoubleTernaryOperator operator, final RandomVariable argument1, final RandomVariable argument2)
	{
		double newTime = Math.max(time, argument1.getFiltrationTime());
		newTime = Math.max(newTime, argument2.getFiltrationTime());

		if(this.isDeterministic() && argument1.isDeterministic() && argument2.isDeterministic()) {
			return new RandomVariableLazyEvaluation(newTime, operator.applyAsDouble(valueIfNonStochastic, argument1.get(0), argument2.get(0)));
		}
		else {
			final int newSize = Math.max(Math.max(this.size(), argument1.size()), argument2.size());
			IntToDoubleFunction result;
			if(argument1.isDeterministic() && argument2.isDeterministic()) {
				final double	argument1Realization = argument1.get(0);
				final double	argument2Realization = argument2.get(0);
				if(isDeterministic()) {
					result = new IntToDoubleFunction() {
						@Override
						public double applyAsDouble(final int i) {
							return operator.applyAsDouble(valueIfNonStochastic, argument1Realization, argument2Realization);
						}
					};
				}
				else {
					result = new IntToDoubleFunction() {
						@Override
						public double applyAsDouble(final int i) {
							return operator.applyAsDouble(realizations.applyAsDouble(i), argument1Realization, argument2Realization);
						}
					};

				}
			}
			else if(argument1.isDeterministic() && !argument2.isDeterministic()) {
				final double	argument1Realization	= argument1.get(0);
				final double[]	argument2Realizations	= argument2.getRealizations();
				if(isDeterministic()) {
					result = new IntToDoubleFunction() {
						@Override
						public double applyAsDouble(final int i) {
							return operator.applyAsDouble(valueIfNonStochastic, argument1Realization, argument2Realizations[i]);
						}
					};
				}
				else {
					result = new IntToDoubleFunction() {
						@Override
						public double applyAsDouble(final int i) {
							return operator.applyAsDouble(realizations.applyAsDouble(i), argument1Realization, argument2Realizations[i]);
						}
					};

				}
			}
			else if(!argument1.isDeterministic() && argument2.isDeterministic()) {
				final double[]	argument1Realizations	= argument1.getRealizations();
				final double	argument2Realization	= argument2.get(0);
				if(isDeterministic()) {
					result = new IntToDoubleFunction() {
						@Override
						public double applyAsDouble(final int i) {
							return operator.applyAsDouble(valueIfNonStochastic, argument1Realizations[i], argument2Realization);
						}
					};
				}
				else {
					result = new IntToDoubleFunction() {
						@Override
						public double applyAsDouble(final int i) {
							return operator.applyAsDouble(realizations.applyAsDouble(i), argument1Realizations[i], argument2Realization);
						}
					};

				}
			}
			else {// if(!argument1.isDeterministic() && !argument2.isDeterministic()) {
				final double[]	argument1Realizations	= argument1.getRealizations();
				final double[]	argument2Realizations	= argument2.getRealizations();
				if(isDeterministic()) {
					result = new IntToDoubleFunction() {
						@Override
						public double applyAsDouble(final int i) {
							return operator.applyAsDouble(valueIfNonStochastic, argument1Realizations[i], argument2Realizations[i]);
						}
					};
				}
				else {
					result = new IntToDoubleFunction() {
						@Override
						public double applyAsDouble(final int i) {
							return operator.applyAsDouble(realizations.applyAsDouble(i), argument1Realizations[i], argument2Realizations[i]);
						}
					};

				}
			}

			return new RandomVariableLazyEvaluation(newTime, result, newSize);
		}
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#cap(double)
	 */
	@Override
	public RandomVariable cap(final double cap) {
		return apply(new DoubleUnaryOperator() {
			@Override
			public double applyAsDouble(final double x) {
				return Math.min(x, cap);
			}
		});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#floor(double)
	 */
	@Override
	public RandomVariable floor(final double floor) {
		return apply(new DoubleUnaryOperator() {
			@Override
			public double applyAsDouble(final double x) {
				return Math.max(x, floor);
			}
		});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#add(double)
	 */
	@Override
	public RandomVariable add(final double value) {
		return apply(new DoubleUnaryOperator() {
			@Override
			public double applyAsDouble(final double x) {
				return x + value;
			}
		});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#sub(double)
	 */
	@Override
	public RandomVariable sub(final double value) {
		return apply(new DoubleUnaryOperator() {
			@Override
			public double applyAsDouble(final double x) {
				return x - value;
			}
		});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#mult(double)
	 */
	@Override
	public RandomVariable mult(final double value) {
		return apply(new DoubleUnaryOperator() {
			@Override
			public double applyAsDouble(final double x) {
				return x * value;
			}
		});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#div(double)
	 */
	@Override
	public RandomVariable div(final double value) {
		return apply(new DoubleUnaryOperator() {
			@Override
			public double applyAsDouble(final double x) {
				return x / value;
			}
		});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#pow(double)
	 */
	@Override
	public RandomVariable pow(final double exponent) {
		return apply(new DoubleUnaryOperator() {
			@Override
			public double applyAsDouble(final double x) {
				return Math.pow(x, exponent);
			}
		});
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
		return apply(new DoubleUnaryOperator() {
			@Override
			public double applyAsDouble(final double x) {
				return x * x;
			}
		});
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#sqrt()
	 */
	@Override
	public RandomVariable sqrt() {
		return apply(Math::sqrt);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#exp()
	 */
	@Override
	public RandomVariable exp() {
		return apply(Math::exp);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#log()
	 */
	@Override
	public RandomVariable log() {
		return apply(Math::log);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#sin()
	 */
	@Override
	public RandomVariable sin() {
		return apply(Math::sin);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#cos()
	 */
	@Override
	public RandomVariable cos() {
		return apply(Math::cos);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#add(net.finmath.stochastic.RandomVariable)
	 */
	@Override
	public RandomVariable add(final RandomVariable randomVariable) {
		return apply(new DoubleBinaryOperator() {
			@Override
			public double applyAsDouble(final double x, final double y) {
				return x + y;
			}
		}, randomVariable);

	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#sub(net.finmath.stochastic.RandomVariable)
	 */
	@Override
	public RandomVariable sub(final RandomVariable randomVariable) {
		return apply(new DoubleBinaryOperator() {
			@Override
			public double applyAsDouble(final double x, final double y) {
				return x - y;
			}
		}, randomVariable);
	}

	@Override
	public RandomVariable bus(final RandomVariable randomVariable) {
		return apply(new DoubleBinaryOperator() {
			@Override
			public double applyAsDouble(final double x, final double y) {
				return -x + y;
			}
		}, randomVariable);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#mult(net.finmath.stochastic.RandomVariable)
	 */
	@Override
	public RandomVariable mult(final RandomVariable randomVariable) {
		return apply(new DoubleBinaryOperator() {
			@Override
			public double applyAsDouble(final double x, final double y) {
				return x * y;
			}
		}, randomVariable);
	}

	@Override
	public RandomVariable div(final RandomVariable randomVariable) {
		return apply(new DoubleBinaryOperator() {
			@Override
			public double applyAsDouble(final double x, final double y) {
				return x / y;
			}
		}, randomVariable);
	}

	@Override
	public RandomVariable vid(final RandomVariable randomVariable) {
		return apply(new DoubleBinaryOperator() {
			@Override
			public double applyAsDouble(final double x, final double y) {
				return y / x;
			}
		}, randomVariable);
	}

	@Override
	public RandomVariable cap(final RandomVariable cap) {
		return apply(Math::min, cap);
	}

	@Override
	public RandomVariable floor(final RandomVariable floor) {
		return apply(Math::max, floor);
	}

	@Override
	public RandomVariable accrue(final RandomVariable rate, final double periodLength) {
		return apply(new DoubleBinaryOperator() {
			@Override
			public double applyAsDouble(final double x, final double y) {
				return x * (1.0 + y * periodLength);
			}
		}, rate);
	}

	@Override
	public RandomVariable discount(final RandomVariable rate, final double periodLength) {
		return apply(new DoubleBinaryOperator() {
			@Override
			public double applyAsDouble(final double x, final double y) {
				return x / (1.0 + y * periodLength);
			}
		}, rate);
	}

	@Override
	public RandomVariable choose(final RandomVariable valueIfTriggerNonNegative, final RandomVariable valueIfTriggerNegative) {
		return apply(new DoubleTernaryOperator() {
			@Override
			public double applyAsDouble(final double x, final double y, final double z) {
				return (x >= 0 ? y : z);
			}
		}, valueIfTriggerNonNegative, valueIfTriggerNegative);
	}

	@Override
	public RandomVariable invert() {
		return apply(new DoubleUnaryOperator() {
			@Override
			public double applyAsDouble(final double x) {
				return 1.0 / x;
			}
		});
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
		return apply(new DoubleBinaryOperator() {
			@Override
			public double applyAsDouble(final double x, final double y) {
				return x + y * factor2;
			}
		}, factor1);
	}


	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#addProduct(net.finmath.stochastic.RandomVariable, net.finmath.stochastic.RandomVariable)
	 */
	@Override
	public RandomVariable addProduct(final RandomVariable factor1, final RandomVariable factor2) {
		return apply(new DoubleBinaryOperator() {
			@Override
			public double applyAsDouble(final double x, final double y) {
				return x + y;
			}
		}, new DoubleBinaryOperator() {
			@Override
			public double applyAsDouble(final double x, final double y) {
				return x * y;
			}
		}, factor1, factor2);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#addRatio(net.finmath.stochastic.RandomVariable, net.finmath.stochastic.RandomVariable)
	 */
	@Override
	public RandomVariable addRatio(final RandomVariable numerator, final RandomVariable denominator) {
		return apply(new DoubleBinaryOperator() {
			@Override
			public double applyAsDouble(final double x, final double y) {
				return x + y;
			}
		}, new DoubleBinaryOperator() {
			@Override
			public double applyAsDouble(final double x, final double y) {
				return x / y;
			}
		}, numerator, denominator);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#subRatio(net.finmath.stochastic.RandomVariable, net.finmath.stochastic.RandomVariable)
	 */
	@Override
	public RandomVariable subRatio(final RandomVariable numerator, final RandomVariable denominator) {
		return apply(new DoubleBinaryOperator() {
			@Override
			public double applyAsDouble(final double x, final double y) {
				return x - y;
			}
		}, new DoubleBinaryOperator() {
			@Override
			public double applyAsDouble(final double x, final double y) {
				return x / y;
			}
		}, numerator, denominator);
	}

	@Override
	public RandomVariable isNaN() {
		if(isDeterministic()) {
			return new RandomVariableLazyEvaluation(time, Double.isNaN(valueIfNonStochastic) ? 1.0 : 0.0);
		}
		else {
			final double[] newRealizations = new double[size()];
			for(int i=0; i<newRealizations.length; i++) {
				newRealizations[i]		 = Double.isNaN(get(i)) ? 1.0 : 0.0;
			}
			return new RandomVariableLazyEvaluation(time, newRealizations);
		}
	}
}
