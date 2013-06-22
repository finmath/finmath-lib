/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2006
 */
package net.finmath.montecarlo;

import net.finmath.stochastic.ImmutableRandomVariableInterface;
import net.finmath.stochastic.RandomVariableInterface;

import org.apache.commons.math3.util.FastMath;

/**
 * The class RandomVariable represents a random variable being the evaluation of a stochastic process
 * at a certain time within a Monte-Carlo simulation.
 * It is thus essentially a vector of doubles - the realizations - together with a double - the time.
 * The index of the vector represents path.
 * The class may also be used for non-stochastic quantities which may potentially be stochastic
 * (e.g. volatility). If only non-stochastic random variables are involved in an operation the class uses
 * optimized code.
 * 
 * The class is mutable and it is not thread safe, accesses performed exclusively through the interface
 * <code>ImmutableRandomVariableInterface</code> is thread safe (and does not mutate the class).
 * 
 * @author Christian Fries
 * @version 1.8
 */
public class RandomVariable implements RandomVariableInterface, ImmutableRandomVariableInterface {

	private double      time;	                // Time (filtration)

	// Data model for the stochastic case (otherwise null)
	private double[]    realizations;           // Realizations

	// Data model for the non-stochastic case (if realizations==null)
	private double      valueIfNonStochastic;

	/**
	 * Create a non stochastic random variable, i.e. a constant.
	 *
	 * @param value the value, a constant.
	 */
	public RandomVariable(double value) {
		this(0.0, value);
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
	 * @param time the filtration time, set to 0.0 if not used.
	 * @param realisations the vector of realizations.
	 */
	public RandomVariable(double time, double[] realisations) {
		super();
		this.time = time;
		this.realizations = realisations;
		this.valueIfNonStochastic = Double.NaN;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getMutableCopy()
	 */
	public RandomVariable getMutableCopy() {
		if(isDeterministic())	return new RandomVariable(time, valueIfNonStochastic);
		else					return new RandomVariable(time, realizations.clone());        
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#equals(net.finmath.montecarlo.RandomVariable)
	 */
	@Override
	public boolean equals(ImmutableRandomVariableInterface randomVariable) {
		if(this.time != randomVariable.getFiltrationTime()) return false;
		if(this.isDeterministic() && randomVariable.isDeterministic()) {
			return this.valueIfNonStochastic == randomVariable.get(0);
		}

		if(this.isDeterministic() != randomVariable.isDeterministic()) return false;

		for(int i=0; i<realizations.length; i++) if(realizations[i] != randomVariable.get(i)) return false;

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
		if(isDeterministic())   return valueIfNonStochastic;
		else               		return realizations[pathOrState];
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#set(int,double)
	 */
	public void set(int pathOrState, double value) {
		if(isDeterministic())   valueIfNonStochastic		= value;
		else               		realizations[pathOrState]	= value;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#size()
	 */
	@Override
	public int size() {
		if(isDeterministic())    return 1;
		else                     return realizations.length;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getMin()
	 */
	public double getMin() {
		if(isDeterministic()) return valueIfNonStochastic;
		double min = Double.MAX_VALUE;
		if(realizations.length != 0) min = realizations[0];     /// @see getMax()
		for(int i=0; i<realizations.length; i++) min = Math.min(realizations[i],min);
		return min;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.ImmutableRandomVariableInterface#getMax()
	 */
	@Override
	public double getMax() {
		if(isDeterministic()) return valueIfNonStochastic;
		double max = Double.MIN_VALUE;
		if(realizations.length != 0) max = realizations[0];     /// @bug Workaround. There seems to be a bug in Java 1.4 with Math.max(Double.MIN_VALUE,0.0)
		for(int i=0; i<realizations.length; i++) max = Math.max(realizations[i],max);
		return max;
	}

	/**
	 * @return Sum of all realizations.
	 */
	public double getSum() {
		if(isDeterministic())	return valueIfNonStochastic;
		if(size() == 0)			return Double.NaN;

		double sum = 0.0;
		for(int i=0; i<realizations.length; i++) sum += realizations[i];
		return sum;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.ImmutableRandomVariableInterface#getAverage()
	 */
	public double getAverage() {
		if(isDeterministic())	return valueIfNonStochastic;
		if(size() == 0)			return Double.NaN;

		double sum = 0.0;
		for(int i=0; i<realizations.length; i++) sum += realizations[i];
		return sum/realizations.length;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.ImmutableRandomVariableInterface#getAverage(net.finmath.stochastic.ImmutableRandomVariableInterface)
	 */
	@Override
	public double getAverage(ImmutableRandomVariableInterface probabilities) {
		if(isDeterministic())	return valueIfNonStochastic;
		if(size() == 0)			return Double.NaN;

		double average = 0.0;
		for(int i=0; i<realizations.length; i++) average += realizations[i] * probabilities.get(i);
		return average;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.ImmutableRandomVariableInterface#getVariance()
	 */
	@Override
	public double getVariance() {
		if(isDeterministic())	return 0.0;
		if(size() == 0)			return Double.NaN;

		double sum			= 0.0;
		double sumOfSquared = 0.0;
		for(double realization : realizations) {
			sum				+= realization;
			sumOfSquared	+= realization * realization;
		}
		return sumOfSquared/realizations.length - sum/realizations.length * sum/realizations.length;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.ImmutableRandomVariableInterface#getVariance(net.finmath.stochastic.ImmutableRandomVariableInterface)
	 */
	public double getVariance(ImmutableRandomVariableInterface probabilities) {
		if(isDeterministic())	return 0.0;
		if(size() == 0)			return Double.NaN;

		double mean			= 0.0;
		double secondMoment = 0.0;
		for(int i=0; i<realizations.length; i++) {
			mean			+= realizations[i] * probabilities.get(i);
			secondMoment	+= realizations[i] * realizations[i] * probabilities.get(i);
		}
		return secondMoment - mean*mean;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.ImmutableRandomVariableInterface#getStandardDeviation()
	 */
	@Override
	public double getStandardDeviation() {
		if(isDeterministic())	return 0.0;
		if(size() == 0)			return Double.NaN;

		return Math.sqrt(getVariance());
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.ImmutableRandomVariableInterface#getStandardDeviation(net.finmath.stochastic.ImmutableRandomVariableInterface)
	 */
	@Override
	public double getStandardDeviation(ImmutableRandomVariableInterface probabilities) {
		if(isDeterministic())	return 0.0;
		if(size() == 0)			return Double.NaN;

		return Math.sqrt(getVariance(probabilities));
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.ImmutableRandomVariableInterface#getStandardError()
	 */
	@Override
	public double getStandardError() {
		if(isDeterministic())	return 0.0;
		if(size() == 0)			return Double.NaN;

		return getStandardDeviation()/Math.sqrt(size());
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.ImmutableRandomVariableInterface#getStandardError(net.finmath.stochastic.ImmutableRandomVariableInterface)
	 */
	@Override
	public double getStandardError(ImmutableRandomVariableInterface probabilities) {
		if(isDeterministic())	return 0.0;
		if(size() == 0)			return Double.NaN;

		return getStandardDeviation(probabilities)/size();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.ImmutableRandomVariableInterface#getQuantile()
	 */
	@Override
	public double getQuantile(double quantile) {
		if(isDeterministic())	return valueIfNonStochastic;
		if(size() == 0)			return Double.NaN;

		double[] realizationsSorted = realizations.clone();
		java.util.Arrays.sort(realizationsSorted);

		int indexOfQuantileValue = Math.min(Math.max((int)Math.round((size()+1) * (1-quantile) - 1), 0), size()-1);

		return realizationsSorted[indexOfQuantileValue];
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.ImmutableRandomVariableInterface#getQuantile(net.finmath.stochastic.ImmutableRandomVariableInterface)
	 */
	public double getQuantile(double quantile, ImmutableRandomVariableInterface probabilities) {
		if(isDeterministic())	return valueIfNonStochastic;
		if(size() == 0)			return Double.NaN;

		throw new RuntimeException("Method not implemented.");
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.ImmutableRandomVariableInterface#getConditionalVaR()
	 */
	@Override
	public double getQuantileExpectation(double quantileStart, double quantileEnd) {
		if(isDeterministic())	return valueIfNonStochastic;
		if(size() == 0)			return Double.NaN;
		if(quantileStart > quantileEnd) return getQuantileExpectation(quantileEnd, quantileStart);

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
	 * @see net.finmath.stochastic.ImmutableRandomVariableInterface#getHistogram()
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
				for(int i=0; i<histogramValues.length; i++) histogramValues[i] /= realizationsSorted.length;
			}
		}

		return histogramValues;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.ImmutableRandomVariableInterface#getHistogram(int,double)
	 */
	@Override
	public double[][] getHistogram(int numberOfPoints, double standardDeviations) {
		double[] intervalPoints = new double[numberOfPoints];
		double[] anchorPoints	= new double[numberOfPoints+1];
		double center	= getAverage();
		double radius	= standardDeviations * getStandardDeviation();
		double stepSize	= (double) (numberOfPoints-1) / 2.0;
		for(int i=0; i<numberOfPoints;i++) {
			double alpha = (-(double)(numberOfPoints-1) / 2.0 + (double)i) / stepSize;
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
	 * @see net.finmath.stochastic.ImmutableRandomVariableInterface#isDeterministic()
	 */
	@Override
	public boolean isDeterministic() {
		return realizations == null;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#expand()
	 */
	public void expand(int numberOfPaths) {
		if(isDeterministic()) {
			// Expand random variable to a vector of path values
			realizations = new double[numberOfPaths];
			java.util.Arrays.fill(realizations,valueIfNonStochastic);
		}
		return;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.ImmutableRandomVariableInterface#getRealizations()
	 */
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

	/**
	 * Returns the realizations as double array. If the random variable is deterministic, then it is expanded
	 * to the given number of paths.
	 *
	 * @param numberOfPaths Number of paths.
	 * @return The realization as double array.
	 */
	public double[] getRealizations(int numberOfPaths) {

		if(!isDeterministic() && realizations.length != numberOfPaths) throw new RuntimeException("Inconsistent number of paths.");
		this.expand(numberOfPaths);

		return realizations;//.clone();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#setRealisations(double[])
	 */
	private void setRealisations(double[] realisations) {
		this.realizations = realisations;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#cap(double)
	 */
	public RandomVariableInterface cap(double cap) {
		if(isDeterministic())							valueIfNonStochastic = Math.min(valueIfNonStochastic,cap);
		else for(int i=0; i<realizations.length; i++)	realizations[i]		 = Math.min(realizations[i],cap);
		return this;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#floor(double)
	 */
	public RandomVariableInterface floor(double floor) {
		if(isDeterministic())							valueIfNonStochastic = Math.max(valueIfNonStochastic,floor);
		else for(int i=0; i<realizations.length; i++)	realizations[i]		 = Math.max(realizations[i],floor);
		return this;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#add(double)
	 */
	public RandomVariableInterface add(double value) {
		if(isDeterministic())							valueIfNonStochastic	+= value;
		else for(int i=0; i<realizations.length; i++)	realizations[i]			+= value;
		return this;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#sub(double)
	 */
	public RandomVariableInterface sub(double value) {
		if(isDeterministic())							valueIfNonStochastic	-= value;
		else for(int i=0; i<realizations.length; i++)	realizations[i]			-= value;
		return this;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#mult(double)
	 */
	public RandomVariableInterface mult(double value) {
		if(isDeterministic())							valueIfNonStochastic	*= value;
		else for(int i=0; i<realizations.length; i++)	realizations[i]			*= value;
		return this;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#div(double)
	 */
	public RandomVariableInterface div(double value) {
		if(isDeterministic())							valueIfNonStochastic	/= value;
		else for(int i=0; i<realizations.length; i++)	realizations[i]			/= value;
		return this;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#pow(double)
	 */
	public RandomVariableInterface pow(double exponent) {
		if(isDeterministic())							valueIfNonStochastic	= FastMath.pow(valueIfNonStochastic,exponent);
		else for(int i=0; i<realizations.length; i++)	realizations[i]			= FastMath.pow(realizations[i],exponent);
		return this;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#squared()
	 */
	public RandomVariableInterface squared() {
		if(isDeterministic())							valueIfNonStochastic	*= valueIfNonStochastic;
		else for(int i=0; i<realizations.length; i++)	realizations[i]			*= realizations[i];
		return this;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#sqrt()
	 */
	public RandomVariableInterface sqrt() {
		if(isDeterministic())							valueIfNonStochastic	= FastMath.sqrt(valueIfNonStochastic);
		else for(int i=0; i<realizations.length; i++)	realizations[i]			= FastMath.sqrt(realizations[i]);
		return this;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#exp()
	 */
	public RandomVariable exp() {
		if(isDeterministic())                        	valueIfNonStochastic    = FastMath.exp(valueIfNonStochastic);
		else {
			for(int i=0; i<realizations.length; i++)	realizations[i]         = FastMath.exp(realizations[i]);
		}
		return this;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#log()
	 */
	public RandomVariable log() {
		if(isDeterministic())                        	valueIfNonStochastic    = FastMath.log(valueIfNonStochastic);
		else for(int i=0; i<realizations.length; i++)   realizations[i]         = FastMath.log(realizations[i]);
		return this;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#sin()
	 */
	public RandomVariableInterface sin() {
		if(isDeterministic())                        	valueIfNonStochastic    = Math.sin(valueIfNonStochastic);
		else for(int i=0; i<realizations.length; i++)   realizations[i]         = Math.sin(realizations[i]);
		return this;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#cos()
	 */
	public RandomVariableInterface cos() {
		if(isDeterministic())                        	valueIfNonStochastic    = Math.cos(valueIfNonStochastic);
		else for(int i=0; i<realizations.length; i++)   realizations[i]         = Math.cos(realizations[i]);
		return this;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#add(net.finmath.stochastic.ImmutableRandomVariableInterface)
	 */
	public RandomVariableInterface add(ImmutableRandomVariableInterface randomVariable) {
		if(realizations == null && randomVariable.isDeterministic())	valueIfNonStochastic	+= randomVariable.get(0);
		else {
			if(isDeterministic()) this.expand(randomVariable.size());
			for(int i=0; i<realizations.length; i++)					realizations[i]			+= randomVariable.get(i);
		}

		// Set time of this random variable to maximum of time with respect to which measurability is known.
		time = Math.max(time, randomVariable.getFiltrationTime());

		return this;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#sub(net.finmath.stochastic.ImmutableRandomVariableInterface)
	 */
	public RandomVariableInterface sub(ImmutableRandomVariableInterface randomVariable) {
		if(isDeterministic() && randomVariable.isDeterministic())	valueIfNonStochastic	-= randomVariable.get(0);
		else {
			if(isDeterministic()) this.expand(randomVariable.size());
			for(int i=0; i<realizations.length; i++)				realizations[i]			-= randomVariable.get(i);
		}

		// Set time of this random variable to maximum of time with respect to which measurability is known.
		time = Math.max(time, randomVariable.getFiltrationTime());

		return this;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#mult(net.finmath.stochastic.ImmutableRandomVariableInterface)
	 */
	public RandomVariable mult(ImmutableRandomVariableInterface randomVariable) {
		if(isDeterministic() && randomVariable.isDeterministic())   valueIfNonStochastic	*= randomVariable.get(0);
		else {
			if(isDeterministic()) this.expand(randomVariable.size());
			for(int i=0; i<realizations.length; i++)										realizations[i]			*= randomVariable.get(i);
		}

		// Set time of this random variable to maximum of time with respect to which measurability is known.
		time = Math.max(time, randomVariable.getFiltrationTime());

		return this;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#div(net.finmath.stochastic.ImmutableRandomVariableInterface)
	 */
	public RandomVariable div(ImmutableRandomVariableInterface randomVariable) {
		if(isDeterministic() && randomVariable.isDeterministic())  valueIfNonStochastic	/= randomVariable.get(0);
		else {
			if(isDeterministic()) this.expand(randomVariable.size());
			for(int i=0; i<realizations.length; i++)										realizations[i]			/= randomVariable.get(i);
		}

		// Set time of this random variable to maximum of time with respect to which measurability is known.
		time = Math.max(time, randomVariable.getFiltrationTime());

		return this;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#cap(net.finmath.stochastic.ImmutableRandomVariableInterface)
	 */
	public RandomVariableInterface cap(ImmutableRandomVariableInterface cap) {
		if(isDeterministic() && cap.isDeterministic())		valueIfNonStochastic	= FastMath.min(valueIfNonStochastic,cap.get(0));
		else {
			if(isDeterministic()) this.expand(cap.size());
			for(int i=0; i<realizations.length; i++)			realizations[i]		= FastMath.min(realizations[i],cap.get(i));
		}

		// Set time of this random variable to maximum of time with respect to which measurability is known.
		time = Math.max(time, cap.getFiltrationTime());

		return this;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#floor(net.finmath.stochastic.ImmutableRandomVariableInterface)
	 */
	public RandomVariableInterface floor(ImmutableRandomVariableInterface floor) {
		if(isDeterministic() && floor.isDeterministic())		valueIfNonStochastic	= FastMath.max(valueIfNonStochastic,floor.get(0));
		else {
			if(isDeterministic()) this.expand(floor.size());
			for(int i=0; i<realizations.length; i++)			realizations[i]			= FastMath.max(realizations[i],floor.get(i));
		}

		// Set time of this random variable to maximum of time with respect to which measurability is known.
		time = Math.max(time, floor.getFiltrationTime());

		return this;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#accrue(net.finmath.stochastic.ImmutableRandomVariableInterface, double)
	 */
	public RandomVariableInterface accrue(ImmutableRandomVariableInterface rate, double periodLength) {
		if(isDeterministic() && rate.isDeterministic())		valueIfNonStochastic	*= 1.0 + rate.get(0) * periodLength;
		else {
			if(isDeterministic()) this.expand(rate.size());
			for(int i=0; i<realizations.length; i++)		realizations[i]			*= 1.0 + rate.get(i) * periodLength;
		}

		// Set time of this random variable to maximum of time with respect to which measurability is known.
		time = Math.max(time, rate.getFiltrationTime());

		return this;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#discount(net.finmath.stochastic.ImmutableRandomVariableInterface, double)
	 */
	public RandomVariableInterface discount(ImmutableRandomVariableInterface rate, double periodLength) {
		if(isDeterministic() && rate.isDeterministic())		valueIfNonStochastic	/= 1.0 + rate.get(0) * periodLength;
		else {
			if(isDeterministic()) this.expand(rate.size());
			for(int i=0; i<realizations.length; i++)		realizations[i]			/= 1.0 + rate.get(i) * periodLength;
		}

		// Set time of this random variable to maximum of time with respect to which measurability is known.
		time = Math.max(time, rate.getFiltrationTime());

		return this;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#barrier(net.finmath.stochastic.ImmutableRandomVariableInterface, net.finmath.stochastic.ImmutableRandomVariableInterface, net.finmath.stochastic.ImmutableRandomVariableInterface)
	 */
	public RandomVariableInterface barrier(ImmutableRandomVariableInterface trigger, ImmutableRandomVariableInterface valueIfTriggerNonNegative, ImmutableRandomVariableInterface valueIfTriggerNegative) {
		if(isDeterministic() && trigger.isDeterministic() && valueIfTriggerNonNegative.isDeterministic() && valueIfTriggerNegative.isDeterministic())		valueIfNonStochastic	= trigger.get(0) >= 0 ? valueIfTriggerNonNegative.get(0) : valueIfTriggerNegative.get(0);
		else {
			if(isDeterministic()) {
				int numberOfPaths = Math.max(Math.max(trigger.size(), valueIfTriggerNonNegative.size()), valueIfTriggerNegative.size());
				this.expand(numberOfPaths);
			}
			for(int i=0; i<realizations.length; i++)			realizations[i]			= trigger.get(i) >= 0.0 ? valueIfTriggerNonNegative.get(i) : valueIfTriggerNegative.get(i);
		}

		// Set time of this random variable to maximum of time with respect to which measurability is known.
		time = Math.max(time, trigger.getFiltrationTime());
		time = Math.max(time, valueIfTriggerNonNegative.getFiltrationTime());
		time = Math.max(time, valueIfTriggerNegative.getFiltrationTime());

		return this;
	}

	public RandomVariableInterface barrier(ImmutableRandomVariableInterface trigger, ImmutableRandomVariableInterface valueIfTriggerNonNegative, double valueIfTriggerNegative) {
		return this.barrier(trigger, valueIfTriggerNonNegative, new RandomVariable(valueIfTriggerNonNegative.getFiltrationTime(), valueIfTriggerNegative));
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#invert()
	 */
	public RandomVariableInterface invert() {
		if(isDeterministic())							valueIfNonStochastic	= 1.0/valueIfNonStochastic;
		else for(int i=0; i<realizations.length; i++)	realizations[i]			= 1.0/realizations[i];
		return this;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#abs()
	 */
	public RandomVariableInterface abs() {
		if(isDeterministic())							valueIfNonStochastic	= Math.abs(valueIfNonStochastic);
		else for(int i=0; i<realizations.length; i++)	realizations[i]			= Math.abs(realizations[i]);
		return this;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#addProduct(net.finmath.stochastic.ImmutableRandomVariableInterface, double)
	 */
	public RandomVariableInterface addProduct(ImmutableRandomVariableInterface factor1, double factor2) {
		if(isDeterministic() && factor1.isDeterministic())		valueIfNonStochastic	+= factor1.get(0) * factor2;
		else
		{
			if(isDeterministic()) this.expand(factor1.size());
			for(int i=0; i<realizations.length; i++)			realizations[i]			+= factor1.get(i) * factor2;
		}

		// Set time of this random variable to maximum of time with respect to which measurability is known.
		time = Math.max(time, factor1.getFiltrationTime());

		return this;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#addProduct(net.finmath.stochastic.ImmutableRandomVariableInterface, net.finmath.stochastic.ImmutableRandomVariableInterface)
	 */
	public RandomVariableInterface addProduct(ImmutableRandomVariableInterface factor1, ImmutableRandomVariableInterface factor2) {
		if(isDeterministic() && factor1.isDeterministic() && factor2.isDeterministic())	valueIfNonStochastic	+= factor1.get(0) * factor2.get(0);
		else {
			if(isDeterministic()) this.expand(Math.max(factor1.size(), factor2.size()));
			for(int i=0; i<realizations.length; i++)									realizations[i]			+= factor1.get(i) * factor2.get(i);
		}

		// Set time of this random variable to maximum of time with respect to which measurability is known.
		time = Math.max(time, factor1.getFiltrationTime());
		time = Math.max(time, factor2.getFiltrationTime());

		return this;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#addRatio(net.finmath.stochastic.ImmutableRandomVariableInterface, net.finmath.stochastic.ImmutableRandomVariableInterface)
	 */
	public RandomVariableInterface addRatio(ImmutableRandomVariableInterface numerator, ImmutableRandomVariableInterface denominator) {
		if(isDeterministic() && numerator.isDeterministic() && denominator.isDeterministic())	valueIfNonStochastic	+= numerator.get(0) / denominator.get(0);
		else {
			if(isDeterministic()) this.expand(Math.max(numerator.size(), denominator.size()));
			for(int i=0; i<realizations.length; i++)											realizations[i]			+= numerator.get(i) / denominator.get(i);
		}

		// Set time of this random variable to maximum of time with respect to which measurability is known.
		time = Math.max(time, numerator.getFiltrationTime());
		time = Math.max(time, denominator.getFiltrationTime());

		return this;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#subRatio(net.finmath.stochastic.ImmutableRandomVariableInterface, net.finmath.stochastic.ImmutableRandomVariableInterface)
	 */
	public RandomVariableInterface subRatio(ImmutableRandomVariableInterface numerator, ImmutableRandomVariableInterface denominator) {
		if(isDeterministic() && numerator.isDeterministic() && denominator.isDeterministic())	valueIfNonStochastic	-= numerator.get(0) / denominator.get(0);
		else {
			if(isDeterministic()) this.expand(Math.max(numerator.size(), denominator.size()));
			for(int i=0; i<realizations.length; i++)											realizations[i]			-= numerator.get(i) / denominator.get(i);
		}

		// Set time of this random variable to maximum of time with respect to which measurability is known.
		time = Math.max(time, numerator.getFiltrationTime());
		time = Math.max(time, denominator.getFiltrationTime());

		return this;
	}
}
