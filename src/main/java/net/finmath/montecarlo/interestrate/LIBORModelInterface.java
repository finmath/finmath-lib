/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 04.02.2016
 */

package net.finmath.montecarlo.interestrate;

import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.marketdata.model.curves.DiscountCurveInterface;
import net.finmath.marketdata.model.curves.ForwardCurveInterface;
import net.finmath.montecarlo.model.AbstractModelInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * @author Christian Fries
 *
 */
public interface LIBORModelInterface extends AbstractModelInterface {

	RandomVariableInterface getLIBOR(int timeIndex, int liborIndex) throws CalculationException;

	/**
	 * The tenor time discretization of the forward rate curve.
	 * 
	 * @return The tenor time discretization of the forward rate curve.
	 */
	TimeDiscretizationInterface getLiborPeriodDiscretization();

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
	 * Return the associated analytic model, a collection of market date object like discount curve, forward curve
	 * and volatility surfaces.
	 * 
	 * @return The associated analytic model.
	 */
	AnalyticModelInterface getAnalyticModel();

	/**
	 * Return the discount curve associated the forwards.
	 * 
	 * @return the discount curve associated the forwards.
	 */
	DiscountCurveInterface getDiscountCurve();

	/**
	 * Return the initial forward rate curve.
	 * 
	 * @return the forward rate curve
	 */
	ForwardCurveInterface getForwardRateCurve();

	/**
	 * Create a new object implementing LIBORMarketModelInterface, using the new data.
	 * 
	 * @param dataModified A map with values to be used in constructions (keys are identical to parameter names of the constructors).
	 * @return A new object implementing LIBORMarketModelInterface, using the new data.
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	LIBORMarketModelInterface getCloneWithModifiedData(Map<String, Object> dataModified) throws CalculationException;

}