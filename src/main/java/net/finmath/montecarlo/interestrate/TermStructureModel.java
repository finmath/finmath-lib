/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 04.02.2016
 */

package net.finmath.montecarlo.interestrate;

import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.montecarlo.model.ProcessModel;
import net.finmath.stochastic.RandomVariable;

/**
 * @author Christian Fries
 *
 * @version 1.0
 */
public interface TermStructureModel extends ProcessModel {

	RandomVariable getLIBOR(double time, double periodStart, double periodEnd) throws CalculationException;

	/**
	 * Return the associated analytic model, a collection of market date object like discount curve, forward curve
	 * and volatility surfaces.
	 *
	 * @return The associated analytic model.
	 */
	AnalyticModel getAnalyticModel();

	/**
	 * Return the discount curve associated the forwards.
	 *
	 * @return the discount curve associated the forwards.
	 */
	DiscountCurve getDiscountCurve();

	/**
	 * Return the initial forward rate curve.
	 *
	 * @return the forward rate curve
	 */
	ForwardCurve getForwardRateCurve();

	/**
	 * Returns the term structure covariance model.
	 *
	 * @return the term structure covariance model.
	 */
	//	TermStructureCovarianceModelInterface getCovarianceModel();

	/**
	 * Create a new object implementing TermStructureModel, using the new data.
	 *
	 * @param dataModified A map with values to be used in constructions (keys are identical to parameter names of the constructors).
	 * @return A new object implementing TermStructureModel, using the new data.
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	@Override
	TermStructureModel getCloneWithModifiedData(Map<String, Object> dataModified) throws CalculationException;

}
