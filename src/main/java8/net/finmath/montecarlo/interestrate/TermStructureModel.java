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
import net.finmath.montecarlo.process.MonteCarloProcess;
import net.finmath.stochastic.RandomVariable;

/**
 * @author Christian Fries
 *
 * @version 1.0
 */
public interface TermStructureModel extends ProcessModel {

	/**
	 * Returns the time \( t \) forward rate on the models forward curve.
	 *
	 * Note: It is guaranteed that the random variable returned by this method is \( \mathcal{F}_{t} ) \)-measurable.
	 *
	 * @param process The discretization process generating this model. The process provides call backs for TimeDiscretization and allows calls to getProcessValue for timeIndices less or equal the given one.
	 * @param time The evaluation time.
	 * @param periodStart The period start of the forward rate.
	 * @param periodEnd The period end of the forward rate.
	 * @return The forward rate.
	 * @throws CalculationException Thrown if model fails to calculate the random variable.
	 */
	RandomVariable getForwardRate(MonteCarloProcess process, double time, double periodStart, double periodEnd) throws CalculationException;

	/**
	 * Returns the time \( t \) forward bond derived from the numeraire, i.e., \( P(T;t) = E( \frac{N(t)}{N(T)} \vert \mathcal{F}_{t} ) \).
	 *
	 * Note: It is guaranteed that the random variabble returned by this method is \( \mathcal{F}_{t} ) \)-measurable.
	 *
	 * @param process The discretization process generating this model. The process provides call backs for TimeDiscretization and allows calls to getProcessValue for timeIndices less or equal the given one.
	 * @param time The evaluation time.
	 * @param maturity The maturity.
	 * @return The forward bond P(T;t).
	 * @throws CalculationException Thrown if model fails to calculate the random variable.
	 */
	default RandomVariable getForwardDiscountBond(MonteCarloProcess process, final double time, final double maturity) throws CalculationException {
		throw new UnsupportedOperationException("The model does not support this method. Note: implemenation will become mandatory is future releases.");
	}

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

	/*+
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

	/**
	 * Returns the time \( t \) forward rate on the models forward curve.
	 *
	 * Note: It is guaranteed that the random variable returned by this method is \( \mathcal{F}_{t} ) \)-measurable.
	 *
	 * @param process The discretization process generating this model. The process provides call backs for TimeDiscretization and allows calls to getProcessValue for timeIndices less or equal the given one.
	 * @param time The evaluation time.
	 * @param periodStart The period start of the forward rate.
	 * @param periodEnd The period end of the forward rate.
	 * @return The forward rate.
	 * @throws CalculationException Thrown if model fails to calculate the random variable.
	 */
	default RandomVariable getLIBOR(MonteCarloProcess process, double time, double periodStart, double periodEnd) throws CalculationException {
		return getForwardRate(process, time, periodStart, periodEnd);
	}
}
