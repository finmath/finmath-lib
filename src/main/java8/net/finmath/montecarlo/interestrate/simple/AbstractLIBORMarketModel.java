/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 04.02.2004
 */
package net.finmath.montecarlo.interestrate.simple;

import java.time.LocalDateTime;

import net.finmath.montecarlo.BrownianMotionLazyInit;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.montecarlo.templatemethoddesign.LogNormalProcess;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;

/**
 * This class represents an abstract base class for a LIBOR Market Model.
 * Derive from this class and implement initial value, volatility and correlation (via factor loadings).
 *
 * @author Christian Fries
 * @version 0.5
 * @since finmath-lib 4.1.0
 */
public abstract class AbstractLIBORMarketModel extends LogNormalProcess implements LIBORModelMonteCarloSimulationModel {

	// Discretization of the yield curve into LIBORs
	private final TimeDiscretization liborPeriodDiscretization;

	/**
	 * @param liborPeriodDiscretization2 The tenor
	 * @param brownianMotionLazyInit The Brownian motion (includes the specification of time discretization, number of factors, and number of paths.
	 */
	public AbstractLIBORMarketModel(
			final TimeDiscretization			liborPeriodDiscretization2,
			final BrownianMotionLazyInit		brownianMotionLazyInit) {
		super(liborPeriodDiscretization2.getNumberOfTimeSteps(), brownianMotionLazyInit);
		liborPeriodDiscretization = liborPeriodDiscretization2;
	}

	//	@Override
	@Override
	public LocalDateTime getReferenceDate() {
		throw new UnsupportedOperationException("This model does not provide a reference date. Reference dates will be mandatory in a future version.");
	}

	@Override
	public abstract Object getCloneWithModifiedSeed(int seed);

	/**
	 * @param timeIndex The time index at which the numeraire is requested.
	 * @return The numeraire at the specified time as <code>RandomVariableFromDoubleArray</code>
	 */
	public abstract RandomVariable getNumeraire(int timeIndex);

	/**
	 * @param time The time at which the numeraire is requested.
	 * @return The numeraire at the specified time as <code>RandomVariableFromDoubleArray</code>
	 */
	@Override
	public RandomVariable getNumeraire(final double time) {
		return getNumeraire(getTimeIndex(time));
	}

	@Override
	public RandomVariable getLIBOR(final int timeIndex, final int liborIndex)
	{
		// This method is just a psynonym - call getProcessValue of super class
		return getProcessValue(timeIndex, liborIndex);
	}

	/**
	 * This method returns the vector of LIBORs at a certain time index.
	 *
	 * @param timeIndex Time index at which the process should be observed
	 * @return The process realizations
	 */
	@Override
	public RandomVariable[] getLIBORs(final int timeIndex)
	{
		final RandomVariable[] randomVariableVector = new RandomVariable[getNumberOfComponents()];
		for(int componentIndex=0; componentIndex<getNumberOfComponents(); componentIndex++) {
			randomVariableVector[componentIndex] = getLIBOR(timeIndex, componentIndex);
		}

		return randomVariableVector;
	}

	/**
	 * @param time			Simulation time
	 * @param periodStart	Start time of period
	 * @param periodEnd		End time of period
	 * @return				The LIBOR rate as seen on simulation time for the specified period
	 */
	@Override
	public RandomVariable getForwardRate(final double time, final double periodStart, final double periodEnd)
	{
		final int periodStartIndex	= getLiborPeriodIndex(periodStart);
		final int periodEndIndex		= getLiborPeriodIndex(periodEnd);

		final int timeIndex			= getTimeIndex(time);

		// If this is a model primitive then return it
		if(periodStartIndex+1==periodEndIndex) {
			return getProcessValue(timeIndex, periodStartIndex);
		}

		// The requeste LIBOR is not a model primitive. We need to calculate it (slow!)
		final double[] libor = new double[getNumberOfPaths()];
		java.util.Arrays.fill(libor,1.0);

		// Calculate the value of the forward bond
		for(int periodIndex = periodStartIndex; periodIndex<periodEndIndex; periodIndex++)
		{
			final double subPeriodLength = getLiborPeriod(periodIndex+1) - getLiborPeriod(periodIndex);
			final RandomVariable liborOverSubPeriod = getLIBOR(timeIndex, periodIndex);

			for(int path = 0; path<getNumberOfPaths(); path++) {
				libor[path] *= (1 + liborOverSubPeriod.get(path) * subPeriodLength);
			}
		}

		// Calculate the libor
		for(int path = 0; path<getNumberOfPaths(); path++) {
			libor[path] -= 1;
			libor[path] /= (periodEnd - periodStart);
		}

		return new RandomVariableFromDoubleArray(time,libor);
	}

	@Override
	public RandomVariable getMonteCarloWeights(final double time) {
		return getMonteCarloWeights(getTimeIndex(time));
	}

	/**
	 * @return The number of LIBORs in the LIBOR discretization
	 */
	@Override
	public int getNumberOfLibors()
	{
		// This is just a synonym to number of components
		return getNumberOfComponents();
	}

	/**
	 * @return The index corresponding to a given time (interpretation is start of period)
	 */
	@Override
	public double getLiborPeriod(final int timeIndex) {
		return liborPeriodDiscretization.getTime(timeIndex);
	}

	/**
	 * Same as java.util.Arrays.binarySearch(liborPeriodDiscretization,time). Will return a negative value if the time is not found, but then -index-1 corresponds to the index of the smallest time greater than the given one.
	 *
	 * @return The index corresponding to a given time (interpretation is start of period)
	 */
	@Override
	public int getLiborPeriodIndex(final double time) {
		return liborPeriodDiscretization.getTimeIndex(time);
	}

	@Override
	public TimeDiscretization getLiborPeriodDiscretization() {
		return liborPeriodDiscretization;
	}
}
