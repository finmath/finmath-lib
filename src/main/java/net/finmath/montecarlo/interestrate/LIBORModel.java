/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 04.02.2016
 */

package net.finmath.montecarlo.interestrate;

import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.automaticdifferentiation.IndependentModelParameterProvider;
import net.finmath.montecarlo.process.MonteCarloProcess;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;

/**
 * @author Christian Fries
 *
 * @version 1.0
 */
public interface LIBORModel extends TermStructureModel, IndependentModelParameterProvider {

	/**
	 * Return the forward rate at a given timeIndex and for a given liborIndex.
	 *
	 * @param process The discretization process generating this model. The process provides call backs for TimeDiscretization and allows calls to getProcessValue for timeIndices less or equal the given one.
	 * @param timeIndex The time index (associated with {@link MonteCarloProcess#getTimeDiscretization()}.
	 * @param liborIndex The forward rate index (associated with {@link LIBORModel#getLiborPeriodDiscretization()}.
	 * @return The forward rate.
	 * @throws CalculationException Thrown if calculation failed.
	 */
	RandomVariable getLIBOR(MonteCarloProcess process, int timeIndex, int liborIndex) throws CalculationException;

	/**
	 * The tenor time discretization of the forward rate curve.
	 *
	 * @return The tenor time discretization of the forward rate curve.
	 */
	TimeDiscretization getLiborPeriodDiscretization();

	/**
	 * Get the number of LIBORs in the LIBOR discretization.
	 *
	 * @return The number of LIBORs in the LIBOR discretization
	 */
	int getNumberOfLibors();

	/**
	 * The period start corresponding to a given forward rate discretization index.
	 *
	 * @param timeIndex The index corresponding to a given time (interpretation is start of period)
	 * @return The period start corresponding to a given forward rate discretization index.
	 */
	double getLiborPeriod(int timeIndex);

	/**
	 * Same as java.util.Arrays.binarySearch(liborPeriodDiscretization,time). Will return a negative value if the time is not found, but then -index-1 corresponds to the index of the smallest time greater than the given one.
	 *
	 * @param time The period start.
	 * @return The index corresponding to a given time (interpretation is start of period)
	 */
	int getLiborPeriodIndex(double time);

	/**
	 * Create a new object implementing LIBORModel, using the new data.
	 *
	 * @param dataModified A map with values to be used in constructions (keys are identical to parameter names of the constructors).
	 * @return A new object implementing LIBORModel, using the new data.
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	@Override
	LIBORModel getCloneWithModifiedData(Map<String, Object> dataModified) throws CalculationException;
}
