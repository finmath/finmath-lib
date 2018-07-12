/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 28 Feb 2015
 */

package net.finmath.montecarlo.process;

import java.util.HashMap;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;

import net.finmath.exception.CalculationException;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * A linear interpolated time discrete process, that is, given a collection of tuples
 * (Double,  RandomVariable) representing realizations \( X(t_{i}) \) this class implements
 * the {@link ProcessInterface} and creates a stochastic process \( t \mapsto X(t) \)
 * where
 * \[
 * 	X(t) = \frac{t_{i+1} - t}{t_{i+1}-t_{i}} X(t_{i}) + \frac{t - t_{i}}{t_{i+1}-t_{i}} X(t_{i+1}) 
 * \]
 * with \( t_{i} \leq t \leq t_{i+1} \).
 * 
 * Note: this is the interpolation scheme used in the convergence of the Euler-Maruyama scheme.
 * 
 * @author Christian Fries
 */
public class LinearInterpolatedTimeDiscreteProcess implements ProcessInterface {

	private TimeDiscretizationInterface timeDiscretization;
	private Map<Double, RandomVariableInterface> realizations;

	/**
	 * Create a time discrete process by linear interpolation of random variables.
	 * 
	 * @param realizations Given map from time to random variable. The map must not be modified.
	 */
	public LinearInterpolatedTimeDiscreteProcess(Map<Double, RandomVariableInterface> realizations) {
		super();
		this.timeDiscretization = new TimeDiscretization(realizations.keySet());
		this.realizations = new HashMap<>();
		this.realizations.putAll(realizations);
	}

	/**
	 * Private constructor. Note: The arguments are not cloned.
	 * 
	 * @param timeDiscretization The time discretization.
	 * @param realizations The map from Double to RandomVariableInterface.
	 */
	private LinearInterpolatedTimeDiscreteProcess(TimeDiscretizationInterface timeDiscretization, Map<Double, RandomVariableInterface> realizations) {
		this.timeDiscretization = timeDiscretization;
		this.realizations = realizations;
	}

	/**
	 * Create a new linear interpolated time discrete process by
	 * using the time discretization of this process and the sum of this process and the given one
	 * as its values.
	 * 
	 * @param process A given process.
	 * @return A new process representing the of this and the given process.
	 * @throws CalculationException Thrown if the given process fails to evaluate at a certain time point.
	 */
	public LinearInterpolatedTimeDiscreteProcess add(LinearInterpolatedTimeDiscreteProcess process) throws CalculationException {
		Map<Double, RandomVariableInterface> sum = new HashMap<>();

		for(double time: timeDiscretization) {
			sum.put(time, realizations.get(time).add(process.getProcessValue(time, 0)));
		}

		return new LinearInterpolatedTimeDiscreteProcess(timeDiscretization, sum);
	}

	/**
	 * Create a new process consisting of the interpolation of the random variables obtained by
	 * applying the given function to this process discrete set of random variables.
	 * That is \( t \mapsto Y(t) \)
	 * where
	 * \[
	 * 	Y(t) = \frac{t_{i+1} - t}{t_{i+1}-t_{i}} f(X(t_{i})) + \frac{t - t_{i}}{t_{i+1}-t_{i}} f(X(t_{i+1})) 
	 * \]
	 * with \( t_{i} \leq t \leq t_{i+1} \) and a given function \( f \).
	 * 
	 * @param function The function \( f \), a univariate function.
	 * @return A new process consisting of the interpolation of the random variables obtained by applying the given function to this process discrete set of random variables.
	 */
	public LinearInterpolatedTimeDiscreteProcess apply(DoubleUnaryOperator function) {
		Map<Double, RandomVariableInterface> result = new HashMap<>();

		for(double time: timeDiscretization) {
			result.put(time, realizations.get(time).apply(function));
		}

		return new LinearInterpolatedTimeDiscreteProcess(timeDiscretization, result);
	}

	/**
	 * Returns the (possibly interpolated) value of this stochastic process at a given time \( t \).
	 * 
	 * @param time The time \( t \).
	 * @param component The component to be returned (if this is a vector valued process), otherwise 0.
	 * @return The random variable \( X(t) \).
	 */
	public RandomVariableInterface getProcessValue(double time, int component) {
		double timeLower = timeDiscretization.getTimeIndexNearestLessOrEqual(time);
		double timeUpper = timeDiscretization.getTimeIndexNearestGreaterOrEqual(time);
		if(timeLower == timeUpper) {
			return realizations.get(timeLower);
		}

		RandomVariableInterface valueLower	= realizations.get(timeLower);
		RandomVariableInterface valueUpper	= realizations.get(timeUpper);

		return valueUpper.mult((time-timeLower)/(timeUpper-timeLower)).add(valueLower.mult((timeUpper-time)/(timeUpper-timeLower)));
	}

	@Override
	public RandomVariableInterface getProcessValue(int timeIndex, int component) {
		return realizations.get(timeDiscretization.getTime(timeIndex));

	}

	@Override
	public RandomVariableInterface getMonteCarloWeights(int timeIndex) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getNumberOfComponents() {
		return 1;
	}

	@Override
	public TimeDiscretizationInterface getTimeDiscretization() {
		return timeDiscretization;
	}

	@Override
	public double getTime(int timeIndex) {
		return timeDiscretization.getTime(timeIndex);
	}

	@Override
	public int getTimeIndex(double time) {
		return timeDiscretization.getTimeIndex(time);
	}

	@Override
	public ProcessInterface clone() {
		return new LinearInterpolatedTimeDiscreteProcess(timeDiscretization, realizations);
	}
}
