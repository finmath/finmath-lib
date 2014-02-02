/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2006
 */
package net.finmath.montecarlo;

import java.util.Arrays;
import java.util.PrimitiveIterator;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntToDoubleFunction;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

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
 * Accesses performed exclusively through the interface
 * <code>RandomVariableInterface</code> is thread safe (and does not mutate the class).
 * 
 * The implementation require Java 8 or better.
 *
 * @author Christian Fries
 * @version 2.0
 */
public class RandomVariable implements RandomVariableInterface {

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
        this(Double.MIN_VALUE, value);
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
        return this;

        //if(isDeterministic())	return new RandomVariable(time, valueIfNonStochastic);
        //else					return new RandomVariable(time, realizations.clone());
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
  /*  public void set(int pathOrState, double value) {

    }
*/
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

    /**
     * @return Sum of all realizations.
     */
    public double getSum() {
        if(isDeterministic())	return valueIfNonStochastic;
        if(size() == 0)			return Double.NaN;
        return getRealizationsStream().sum();
    }

    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#getAverage()
     */
    @Override
    public double getAverage() {
        if(isDeterministic())	return valueIfNonStochastic;
        if(size() == 0)			return Double.NaN;
        return getRealizationsStream().sum()/realizations.length;
    }

    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#getAverage(net.finmath.stochastic.RandomVariableInterface)
     */
    @Override
    public double getAverage(RandomVariableInterface probabilities) {
        if(isDeterministic())	return valueIfNonStochastic;
        if(size() == 0)			return Double.NaN;

        double average = 0.0;
        for(int i=0; i<realizations.length; i++) average += realizations[i] * probabilities.get(i);
        return average;
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
        for (double realization : realizations) {
            sum += realization;
            sumOfSquared += realization * realization;
        }
        return sumOfSquared/realizations.length - sum/realizations.length * sum/realizations.length;
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
        for(int i=0; i<realizations.length; i++) {
            mean			+= realizations[i] * probabilities.get(i);
            secondMoment	+= realizations[i] * realizations[i] * probabilities.get(i);
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

        return getStandardDeviation(probabilities)/Math.sqrt(size());
    }

    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#getQuantile()
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
            java.util.Arrays.fill(clone,valueIfNonStochastic);
            return new RandomVariable(time,clone);

        }

        return new RandomVariable(time,realizations.clone());
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
            return realizations;//.clone();
        }
    }

    public IntToDoubleFunction getOperator() {
        return i -> realizations[i];
    }

    /**
     * Returns the realizations as double array. If the random variable is deterministic, then it is expanded
     * to the given number of paths.
     *
     * @param numberOfPaths Number of Paths
     * @return The realization as double array.
     */
    @Override
    public double[] getRealizations(int numberOfPaths) {

        if(isDeterministic()) {
            // Expand random variable to a vector of path values
            double[] v = new double[numberOfPaths];
            java.util.Arrays.fill(v,valueIfNonStochastic);
            return v;
        }

        if(!isDeterministic() && realizations.length != numberOfPaths) throw new RuntimeException("Inconsistent number of paths.");

        return realizations.clone();
    }


    public RandomVariableInterface apply(DoubleUnaryOperator operator) {
        if(isDeterministic()) {
            return new RandomVariable(time, operator.applyAsDouble(valueIfNonStochastic));
        }
        else
       {
           boolean isUseStream = true;
           if(isUseStream) {
                return new RandomVariable(time, this.getRealizationsStream().parallel().map(operator::applyAsDouble).toArray());
           }
           else {
               double[] newRealizations = new double[realizations.length];

               for(int i=0; i<newRealizations.length; i++)
               {
                   newRealizations[i] = operator.applyAsDouble(realizations[i]);
               }

               return new RandomVariable(time,newRealizations);
           }
        }
    }

    public RandomVariableInterface apply(DoubleBinaryOperator operator, RandomVariableInterface argument) {

        double      newTime           = Math.max(time, argument.getFiltrationTime());
        double[]    newRealizations;

        if(isDeterministic() && argument.isDeterministic()) {
            return new RandomVariable(newTime, operator.applyAsDouble(valueIfNonStochastic, argument.get(0)));
        }
        else if(isDeterministic())  // this is deterministic , argument is nondeterministic
        {
            DoubleUnaryOperator unaryOperator = x -> operator.applyAsDouble(valueIfNonStochastic,x);
            boolean isUseStream = true;
            if(isUseStream) {
                return new RandomVariable(time, argument.getRealizationsStream().parallel().map(unaryOperator).toArray());
            }
            else {
                newRealizations = argument.getRealizations();

                for(int i=0; i<newRealizations.length; i++)
                {
                    newRealizations[i] = operator.applyAsDouble(valueIfNonStochastic, newRealizations[i]);
                }

                return new RandomVariable(time,newRealizations);
            }
        }
        else if(argument.isDeterministic()) // argument is deterministic
        {
            double argumentValue = argument.get(0);
            boolean isUseStream = true;
            if(isUseStream) {
                DoubleUnaryOperator unaryOperator = x -> operator.applyAsDouble(x, argumentValue);
                return new RandomVariable(time, this.getRealizationsStream().parallel().map(unaryOperator).toArray());
            }
            else {
                newRealizations = new double[Math.max(this.size(),argument.size())];

                for(int i=0; i<newRealizations.length; i++)
                {
                    newRealizations[i] = operator.applyAsDouble(realizations[i], argumentValue);
                }

                return new RandomVariable(time,newRealizations);
            }

        }
        else        // both nondeterministic
        {
            int newSize = Math.max(this.size(), argument.size());
            boolean isUseStream = false;
            if(isUseStream) {
                double[] argumentRealizations = argument.getRealizations();
                return new RandomVariable(newTime,
                        IntStream.range(0, newSize).parallel().mapToDouble((i) -> {
                            return operator.applyAsDouble(realizations[i], argumentRealizations[i]);
                        }).toArray()
                );
            }
            else {
                double[] argumentRealizations = argument.getRealizations();

                newRealizations = new double[newSize];

                for(int i=0; i<newRealizations.length; i++)
                {
                    newRealizations[i] =  operator.applyAsDouble(realizations[i], argumentRealizations[i]);
                }
            }
        }

        return new RandomVariable(newTime, newRealizations);

    }

    public RandomVariableInterface apply(DoubleBinaryOperator operatorOuter, DoubleBinaryOperator operatorInner, RandomVariableInterface argument1, RandomVariableInterface argument2)
    {
        double newTime = Math.max(time, argument1.getFiltrationTime());
        newTime = Math.max(newTime, argument2.getFiltrationTime());

        if(this.isDeterministic() && argument1.isDeterministic() && argument2.isDeterministic()) {
            return new RandomVariable(newTime, operatorOuter.applyAsDouble(valueIfNonStochastic, operatorInner.applyAsDouble(argument1.get(0), argument2.get(0))));
        }
        else if(!this.isDeterministic() && !argument1.isDeterministic() && !argument2.isDeterministic()) {
            int newSize = Math.max(Math.max(this.size(), argument1.size()), argument2.size());
            double[] a1 = argument1.getRealizations();
            double[] a2 = argument2.getRealizations();
            return new RandomVariable(newTime,
                    IntStream.range(0, newSize).parallel().mapToDouble((i) -> {
                        return operatorOuter.applyAsDouble(realizations[i], operatorInner.applyAsDouble(a1[i], a2[i]));
                    }).toArray()
            );
        }

        final PrimitiveIterator.OfDouble argument1Iterator = argument1.getRealizationsStream().iterator();
        final PrimitiveIterator.OfDouble argument2Iterator = argument2.getRealizationsStream().iterator();
        final PrimitiveIterator.OfDouble thisIterator     = this.getRealizationsStream().iterator();

        int newSize = Math.max(Math.max(this.size(), argument1.size()), argument2.size());

        double[] newRealizations = new double[newSize];

        for(int i=0; i<newRealizations.length; i++)
        {
            newRealizations[i] = operatorOuter.applyAsDouble(thisIterator.nextDouble(), operatorInner.applyAsDouble(argument1Iterator.nextDouble(), argument2Iterator.nextDouble()));
        }

        return new RandomVariable(newTime, newRealizations);
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
        // Set time of this random variable to maximum of time with respect to which measurability is known.
        double newTime = Math.max(time, trigger.getFiltrationTime());
        newTime = Math.max(newTime, valueIfTriggerNonNegative.getFiltrationTime());
        newTime = Math.max(newTime, valueIfTriggerNegative.getFiltrationTime());

        if(isDeterministic() && trigger.isDeterministic() && valueIfTriggerNonNegative.isDeterministic() && valueIfTriggerNegative.isDeterministic())
            return new RandomVariable(newTime,trigger.get(0) >= 0 ? valueIfTriggerNonNegative.get(0) : valueIfTriggerNegative.get(0));
        else {
            int numberOfPaths = Math.max(Math.max(trigger.size(), valueIfTriggerNonNegative.size()), valueIfTriggerNegative.size());
            double[] newRealizations = new double[numberOfPaths];
            for(int i=0; i<newRealizations.length; i++) {
                newRealizations[i]			= trigger.get(i) >= 0.0 ? valueIfTriggerNonNegative.get(i) : valueIfTriggerNegative.get(i);
            }
            return new RandomVariable(newTime,newRealizations);
        }
    }

    @Override
    public RandomVariableInterface barrier(RandomVariableInterface trigger, RandomVariableInterface valueIfTriggerNonNegative, double valueIfTriggerNegative) {
        return this.barrier(trigger, valueIfTriggerNonNegative, new RandomVariable(valueIfTriggerNonNegative.getFiltrationTime(), valueIfTriggerNegative));
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
    public RandomVariableInterface addProduct(RandomVariableInterface factor1, double factor2) {
        return apply((x, y) -> x + y * factor2, factor1);
    }

    private RandomVariableInterface appply2Old(RandomVariableInterface factor1, RandomVariableInterface factor2, DoubleBinaryOperator func, DoubleBinaryOperator valFunc)
    {

        double t = Math.max(time, factor1.getFiltrationTime());
        t = Math.max(t, factor2.getFiltrationTime());

        boolean vd = isDeterministic();
        boolean f1d = factor1.isDeterministic();
        boolean f2d = factor2.isDeterministic();

        if(f1d & f2d & vd)
            return new RandomVariable(t, valFunc.applyAsDouble(valueIfNonStochastic, func.applyAsDouble(factor1.get(0), factor2.get(0))));

        double[] v;
        double[] f1v;
        double f1s;
        double[] f2v;
        double f2s;



        if(vd)  // this is deterministic , app is nondeterministic
        {
            int size = Math.max(factor1.size(),factor2.size());
            v = new double[size];
        }
        else
        {
            v = realizations.clone();
        }

        f1v = factor1.getRealizations();
        f1s = f1v[0];

        f2v = factor2.getRealizations();
        f2s = f2v[0];

        if(f1d & f2d & vd)
        {
            throw  new UnsupportedOperationException("Must return earlier");
        }
        else if(f1d & f2d)
        {
            for(int i=0; i<v.length; i++)
            {
                v[i] = valFunc.applyAsDouble(v[i], func.applyAsDouble(f1s, f2s));
            }
        }
        else if(f1d & vd)
        {
            for(int i=0; i<v.length; i++)
            {
                v[i] = valFunc.applyAsDouble(valueIfNonStochastic, func.applyAsDouble(f1s, f2v[i]));
            }
        }
        else if(f2d & vd)
        {
            for(int i=0; i<v.length; i++)
            {
                v[i] = valFunc.applyAsDouble(valueIfNonStochastic, func.applyAsDouble(f1v[i], f2s));
            }
        }
        else if(f2d)
        {
            for(int i=0; i<v.length; i++)
            {
                v[i] = valFunc.applyAsDouble(v[i], func.applyAsDouble(f1v[i], f2s));
            }
        }
        else if(f1d)
        {
            for(int i=0; i<v.length; i++)
            {
                v[i] = valFunc.applyAsDouble(v[i], func.applyAsDouble(f1s, f2v[i]));
            }
        }
        else if(vd)
        {
            for(int i=0; i<v.length; i++)
            {
                v[i] = valFunc.applyAsDouble(valueIfNonStochastic, func.applyAsDouble(f1v[i], f2v[i]));
            }
        }
        else if(!f1d & !f2d & !vd)
        {
            for(int i=0; i<v.length; i++)
            {
                v[i] = valFunc.applyAsDouble(v[i], func.applyAsDouble(f1v[i], f2v[i]));
            }
        }
        else
        {
            throw new UnsupportedOperationException("Logic is hard") ;
        }

        return new RandomVariable(t,v);

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
