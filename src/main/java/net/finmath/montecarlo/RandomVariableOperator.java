/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
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

import net.finmath.functions.DoubleTernaryOperator;
import net.finmath.stochastic.RandomVariableInterface;

import org.apache.commons.math3.util.FastMath;

/**
 * Implements a Monte-Carlo random variable (like <code>RandomVariable</code> using
 * late evaluation of Java 8 streams
 *
 * Accesses performed exclusively through the interface
 * <code>RandomVariableInterface</code> is thread safe (and does not mutate the class).
 *
 * The implementation require Java 8 or better.
 *
 * @TODO The implementation of getAverage does not use a Kahan summation, while <code>RandomVariable</code> does.
 *
 * @author Christian Fries
 * @author OSC
 * @version 2.0
 */
public class RandomVariableOperator implements RandomVariableInterface {

    private final double            time;	                // Time (filtration)

    // Operator
    private       IntToDoubleFunction   realizations;
    private final int                   size;
    // Data model for the non-stochastic case (if realizations==null)
    private final double                valueIfNonStochastic;

    private transient double[] realizationsArray = null;

    /**
     * Create a random variable from a given other implementation of <code>RandomVariableInterface</code>.
     *
     * @param value Object implementing <code>RandomVariableInterface</code>.
     */
    public RandomVariableOperator(RandomVariableInterface value) {
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
    public RandomVariableOperator(double value) {
        this(0.0, value);
    }

    /**
     * Create a random variable by applying a function to a given other implementation of <code>RandomVariableInterface</code>.
     *
     * @param value Object implementing <code>RandomVariableInterface</code>.
     * @param function A function mapping double to double.
     */
    public RandomVariableOperator(RandomVariableInterface value, DoubleUnaryOperator function) {
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
    public RandomVariableOperator(double time, double value) {
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
     * @param value the value, a constant.
     */
    public RandomVariableOperator(double time, int numberOfPath, double value) {
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
    public RandomVariableOperator(double time, double[] realisations) {
        super();
        this.time = time;
        this.size = realisations.length;
        this.realizations = i->realisations[i];
        this.valueIfNonStochastic = Double.NaN;
    }

    /**
     * Create a stochastic random variable.
     *
     * @param time the filtration time, set to 0.0 if not used.
     * @param realisations the vector of realizations.
     */
    public RandomVariableOperator(double time, IntToDoubleFunction realisations, int size) {
        super();
        this.time = time;
        this.realizations = realisations;
        this.size = size;
        this.valueIfNonStochastic = Double.NaN;
    }

    @Override
    @Deprecated
    public RandomVariableInterface getMutableCopy() {
        return this;
    }

    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#equals(net.finmath.montecarlo.RandomVariable)
     */
    @Override
    public boolean equals(RandomVariableInterface randomVariable) {
        if(this.time != randomVariable.getFiltrationTime()) return false;
        if(this.isDeterministic() && randomVariable.isDeterministic()) {
            return this.valueIfNonStochastic == randomVariable.get(0);
        }

        if(this.isDeterministic() != randomVariable.isDeterministic()) return false;

        for(int i=0; i<size(); i++) if(get(i) != randomVariable.get(i)) return false;

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
        else {
            cache();
            return realizationsArray[pathOrState];
//            return realizations.applyAsDouble(pathOrState);

        }
    }

    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#size()
     */
    @Override
    public int size() {
        return size;
    }

    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#getMin()
     */
    @Override
    public double getMin() {
        if(isDeterministic()) return valueIfNonStochastic;
        return getRealizationsStream().min().getAsDouble();
    }

    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#getMax()
     */
    @Override
    public double getMax() {
        if(isDeterministic()) return valueIfNonStochastic;
        return getRealizationsStream().max().getAsDouble();
    }

    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#getAverage()
     */
    @Override
    public double getAverage() {
        if(isDeterministic())	return valueIfNonStochastic;
        if(size() == 0)			return Double.NaN;

        return getRealizationsStream().sum()/size();
    }

    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#getAverage(net.finmath.stochastic.RandomVariableInterface)
     */
    @Override
    public double getAverage(RandomVariableInterface probabilities) {
        if(isDeterministic())	return valueIfNonStochastic;
        if(size() == 0)			return Double.NaN;

        return this.cache().mult(probabilities).getRealizationsStream().sum();
    }

    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#getVariance()
     */
    @Override
    public double getVariance() {
        if(isDeterministic())	return 0.0;
        if(size() == 0)			return Double.NaN;

        double sum			= 0.0;
        double sumOfSquared = 0.0;
        for (double realization : getRealizations()) {
            sum += realization;
            sumOfSquared += realization * realization;
        }
        return sumOfSquared/size() - sum/size() * sum/size();
    }

    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#getVariance(net.finmath.stochastic.RandomVariableInterface)
     */
    @Override
    public double getVariance(RandomVariableInterface probabilities) {
        if(isDeterministic())	return 0.0;
        if(size() == 0)			return Double.NaN;

        double mean			= 0.0;
        double secondMoment = 0.0;
        for(int i=0; i<size(); i++) {
            mean			+= get(i) * probabilities.get(i);
            secondMoment	+= get(i) * get(i) * probabilities.get(i);
        }
        return secondMoment - mean*mean;
    }

    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#getStandardDeviation()
     */
    @Override
    public double getStandardDeviation() {
        if(isDeterministic())	return 0.0;
        if(size() == 0)			return Double.NaN;

        return Math.sqrt(getVariance());
    }

    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#getStandardDeviation(net.finmath.stochastic.RandomVariableInterface)
     */
    @Override
    public double getStandardDeviation(RandomVariableInterface probabilities) {
        if(isDeterministic())	return 0.0;
        if(size() == 0)			return Double.NaN;

        return Math.sqrt(getVariance(probabilities));
    }

    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#getStandardError()
     */
    @Override
    public double getStandardError() {
        if(isDeterministic())	return 0.0;
        if(size() == 0)			return Double.NaN;

        return getStandardDeviation()/Math.sqrt(size());
    }

    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#getStandardError(net.finmath.stochastic.RandomVariableInterface)
     */
    @Override
    public double getStandardError(RandomVariableInterface probabilities) {
        if(isDeterministic())	return 0.0;
        if(size() == 0)			return Double.NaN;

        return getStandardDeviation(probabilities)/size();
    }

    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#getQuantile()
     */
    @Override
    public double getQuantile(double quantile) {
        if(isDeterministic())	return valueIfNonStochastic;
        if(size() == 0)			return Double.NaN;

        double[] realizationsSorted = getRealizations().clone();
        Arrays.sort(realizationsSorted);

        int indexOfQuantileValue = Math.min(Math.max((int)Math.round((size()+1) * (1-quantile) - 1), 0), size()-1);

        return realizationsSorted[indexOfQuantileValue];
    }

    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#getQuantile(net.finmath.stochastic.RandomVariableInterface)
     */
    @Override
    public double getQuantile(double quantile, RandomVariableInterface probabilities) {
        if(isDeterministic())	return valueIfNonStochastic;
        if(size() == 0)			return Double.NaN;

        throw new RuntimeException("Method not implemented.");
    }

    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#getConditionalVaR()
     */
    @Override
    public double getQuantileExpectation(double quantileStart, double quantileEnd) {
        if(isDeterministic())	return valueIfNonStochastic;
        if(size() == 0)			return Double.NaN;
        if(quantileStart > quantileEnd) return getQuantileExpectation(quantileEnd, quantileStart);

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
                for(int i=0; i<histogramValues.length; i++) histogramValues[i] /= realizationsSorted.length;
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
            Arrays.fill(clone,valueIfNonStochastic);
            return new RandomVariable(time,clone);

        }

        return new RandomVariable(time,getRealizations());
    }


    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#getRealizations()
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

    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#getOperator(int)
     */
    @Override
    public double[] getRealizations(int numberOfPaths) {

        if(isDeterministic()) {
            // Expand random variable to a vector of path values
            double[] v = new double[numberOfPaths];
            Arrays.fill(v,valueIfNonStochastic);
            return v;
        }

        if(!isDeterministic() && size() != numberOfPaths) throw new RuntimeException("Inconsistent number of paths.");

        return getRealizations();
    }


    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#getOperator()
     */
    @Override
    public IntToDoubleFunction getOperator() {
        return realizations;
    }

    public RandomVariable getRandomVariable() {
    	if(isDeterministic())	return new RandomVariable(time, valueIfNonStochastic);
    	else					return new RandomVariable(time, getRealizations());
    }

    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#getRealizationsStream()
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
    public RandomVariableInterface apply(DoubleUnaryOperator operator) {
        if(isDeterministic()) {
            return new RandomVariableOperator(time, operator.applyAsDouble(valueIfNonStochastic));
        }
        else
        {
            IntToDoubleFunction newRealizations = i -> operator.applyAsDouble(realizations.applyAsDouble(i));
            return new RandomVariableOperator(time, newRealizations, size());
        }
    }

    @Override
    public RandomVariableInterface cache() {
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
    public RandomVariableInterface apply(DoubleBinaryOperator operator, final RandomVariableInterface argument) {

        double      newTime           = Math.max(time, argument.getFiltrationTime());

        if(isDeterministic() && argument.isDeterministic()) {
            return new RandomVariableOperator(newTime, operator.applyAsDouble(valueIfNonStochastic, argument.get(0)));
        }
        else if(!isDeterministic() && argument.isDeterministic()) {
	        IntToDoubleFunction newRealizations = i -> operator.applyAsDouble(realizations.applyAsDouble(i), argument.get(0));
	        return new RandomVariableOperator(newTime, newRealizations, size());
        }
        else if(isDeterministic() && !argument.isDeterministic()) {
	        if(false) {
		        final IntToDoubleFunction argumentRealizations = argument.getOperator();
		        IntToDoubleFunction newRealizations = i -> operator.applyAsDouble(valueIfNonStochastic, argumentRealizations.applyAsDouble(i));
		        return new RandomVariableOperator(newTime, newRealizations, argument.size());
	        }
	        else {
		        final double[] argumentRealizations = argument.getRealizations();
		        IntToDoubleFunction newRealizations = i -> operator.applyAsDouble(valueIfNonStochastic, argumentRealizations[i]);
		        return new RandomVariableOperator(newTime, newRealizations, argument.size());
	        }
        }
        else
        {
	        if(false) {
		        final IntToDoubleFunction argumentRealizations = argument.getOperator();
		        IntToDoubleFunction newRealizations = i -> operator.applyAsDouble(realizations.applyAsDouble(i), argumentRealizations.applyAsDouble(i));
		        return new RandomVariableOperator(newTime, newRealizations, size());
	        }
	        else {
		        final double[] argumentRealizations = argument.getRealizations();
		        IntToDoubleFunction newRealizations = i -> operator.applyAsDouble(realizations.applyAsDouble(i), argumentRealizations[i]);
		        return new RandomVariableOperator(newTime, newRealizations, size());
	        }
        }

    }

    public RandomVariableInterface apply(DoubleBinaryOperator operatorOuter, DoubleBinaryOperator operatorInner, RandomVariableInterface argument1, RandomVariableInterface argument2)
    {
        double newTime = Math.max(time, argument1.getFiltrationTime());
        newTime = Math.max(newTime, argument2.getFiltrationTime());

        if(this.isDeterministic() && argument1.isDeterministic() && argument2.isDeterministic()) {
            return new RandomVariableOperator(newTime, operatorOuter.applyAsDouble(valueIfNonStochastic, operatorInner.applyAsDouble(argument1.get(0), argument2.get(0))));
        }
        else {
            int newSize = Math.max(Math.max(this.size(), argument1.size()), argument2.size());

            if(false) {
            	if(argument1.isDeterministic() && argument2.isDeterministic()) {
            		final double	argument1Realization = argument1.get(0);
            		final double	argument2Realization = argument2.get(0);
            		final double	innerResult = operatorInner.applyAsDouble(argument1Realization, argument2Realization);
            		return new RandomVariableOperator(newTime,(int i) -> operatorOuter.applyAsDouble(realizations.applyAsDouble(i), innerResult), newSize);
            	}
            	else {
            		return new RandomVariableOperator(newTime,(int i) -> operatorOuter.applyAsDouble(realizations.applyAsDouble(i), operatorInner.applyAsDouble(argument1.get(i), argument2.get(i))), newSize);
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
                    return new RandomVariableOperator(newTime,(int i) -> operatorOuter.applyAsDouble(valueIfNonStochastic,          innerResult.applyAsDouble(i)), newSize);
                }
                else {
                    return new RandomVariableOperator(newTime,(int i) -> operatorOuter.applyAsDouble(realizations.applyAsDouble(i), innerResult.applyAsDouble(i)), newSize);
                }

            }
        }
    }

    public RandomVariableInterface apply(DoubleTernaryOperator operator, RandomVariableInterface argument1, RandomVariableInterface argument2)
    {
        double newTime = Math.max(time, argument1.getFiltrationTime());
        newTime = Math.max(newTime, argument2.getFiltrationTime());

        if(this.isDeterministic() && argument1.isDeterministic() && argument2.isDeterministic()) {
            return new RandomVariableOperator(newTime, operator.applyAsDouble(valueIfNonStochastic, argument1.get(0), argument2.get(0)));
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

            return new RandomVariableOperator(newTime, result, newSize);
        }
    }

    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#cap(double)
     */
    @Override
    public RandomVariableInterface cap(double cap) {
        return apply(x -> Math.min(x, cap));
    }

    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#floor(double)
     */
    @Override
    public RandomVariableInterface floor(double floor) {
        return apply(x -> Math.max(x, floor));
    }

    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#add(double)
     */
    @Override
    public RandomVariableInterface add(double value) {
        return apply(x -> x + value);
    }

    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#sub(double)
     */
    @Override
    public RandomVariableInterface sub(double value) {
        return apply(x -> x - value);
    }

    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#mult(double)
     */
    @Override
    public RandomVariableInterface mult(double value) {
        return apply(x -> x * value);
    }

    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#div(double)
     */
    @Override
    public RandomVariableInterface div(double value) {
        return apply(x -> x / value);
    }

    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#pow(double)
     */
    @Override
    public RandomVariableInterface pow(double exponent) {
        return apply(x -> FastMath.pow(x, exponent));
    }

    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#squared()
     */
    @Override
    public RandomVariableInterface squared() {
        return apply(x -> x * x);
    }

    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#sqrt()
     */
    @Override
    public RandomVariableInterface sqrt() {
        return apply(FastMath::sqrt);
    }

    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#exp()
     */
    @Override
    public RandomVariableInterface exp() {
        return apply(FastMath::exp);
    }

    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#log()
     */
    @Override
    public RandomVariableInterface log() {
        return apply(FastMath::log);
    }

    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#sin()
     */
    @Override
    public RandomVariableInterface sin() {
        return apply(FastMath::sin);
    }

    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#cos()
     */
    @Override
    public RandomVariableInterface cos() {
        return apply(FastMath::cos);
    }

    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#add(net.finmath.stochastic.RandomVariableInterface)
     */
    @Override
    public RandomVariableInterface add(RandomVariableInterface randomVariable) {
        return apply((x, y) -> x + y, randomVariable);

    }

    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#sub(net.finmath.stochastic.RandomVariableInterface)
     */
    @Override
    public RandomVariableInterface sub(RandomVariableInterface randomVariable) {
        return apply((x, y) -> x - y, randomVariable);
    }

    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#mult(net.finmath.stochastic.RandomVariableInterface)
     */
    @Override
    public RandomVariableInterface mult(RandomVariableInterface randomVariable) {
        return apply((x, y) -> x * y, randomVariable);
    }

    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#div(net.finmath.stochastic.RandomVariableInterface)
     */
    @Override
    public RandomVariableInterface div(RandomVariableInterface randomVariable) {
        return apply((x, y) -> x / y, randomVariable);
    }

    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#cap(net.finmath.stochastic.RandomVariableInterface)
     */
    @Override
    public RandomVariableInterface cap(RandomVariableInterface cap) {
        return apply(FastMath::min, cap);
    }

    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#floor(net.finmath.stochastic.RandomVariableInterface)
     */
    @Override
    public RandomVariableInterface floor(RandomVariableInterface floor) {
        return apply(FastMath::max, floor);
    }

    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#accrue(net.finmath.stochastic.RandomVariableInterface, double)
     */
    @Override
    public RandomVariableInterface accrue(RandomVariableInterface rate, double periodLength) {
        return apply((x, y) -> x * (1.0 + y * periodLength), rate);
    }

    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#discount(net.finmath.stochastic.RandomVariableInterface, double)
     */
    @Override
    public RandomVariableInterface discount(RandomVariableInterface rate, double periodLength) {
        return apply((x, y) -> x / (1.0 + y * periodLength), rate);
    }

    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#barrier(net.finmath.stochastic.RandomVariableInterface, net.finmath.stochastic.RandomVariableInterface, net.finmath.stochastic.RandomVariableInterface)
     */
    @Override
    public RandomVariableInterface barrier(RandomVariableInterface trigger, RandomVariableInterface valueIfTriggerNonNegative, RandomVariableInterface valueIfTriggerNegative) {
        return trigger.apply(( x, y, z) -> (x >= 0 ? y : z), valueIfTriggerNonNegative, valueIfTriggerNegative);
    }

    @Override
    public RandomVariableInterface barrier(RandomVariableInterface trigger, RandomVariableInterface valueIfTriggerNonNegative, double valueIfTriggerNegative) {
        return this.barrier(trigger, valueIfTriggerNonNegative, new RandomVariableOperator(valueIfTriggerNonNegative.getFiltrationTime(), valueIfTriggerNegative));
    }

    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#invert()
     */
    @Override
    public RandomVariableInterface invert() {
        return apply(x -> 1.0 / x);
    }

    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#abs()
     */
    @Override
    public RandomVariableInterface abs() {
        return apply(Math::abs);
    }

    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#addProduct(net.finmath.stochastic.RandomVariableInterface, double)
     */
    @Override
    public RandomVariableInterface addProduct(final RandomVariableInterface factor1, final double factor2) {
        return apply((x, y) -> x + y * factor2, factor1);
    }


    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#addProduct(net.finmath.stochastic.RandomVariableInterface, net.finmath.stochastic.RandomVariableInterface)
     */
    @Override
    public RandomVariableInterface addProduct(RandomVariableInterface factor1, RandomVariableInterface factor2) {
        return apply((x,y) -> x + y, (x, y) -> x * y, factor1, factor2);
    }

    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#addRatio(net.finmath.stochastic.RandomVariableInterface, net.finmath.stochastic.RandomVariableInterface)
     */
    @Override
    public RandomVariableInterface addRatio(RandomVariableInterface numerator, RandomVariableInterface denominator) {
        return apply((x, y) -> x + y, (x, y) -> x / y, numerator, denominator);
    }

    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#subRatio(net.finmath.stochastic.RandomVariableInterface, net.finmath.stochastic.RandomVariableInterface)
     */
    @Override
    public RandomVariableInterface subRatio(RandomVariableInterface numerator, RandomVariableInterface denominator) {
        return apply((x,y) -> x - y, (x, y) -> x / y, numerator, denominator);
    }
}
