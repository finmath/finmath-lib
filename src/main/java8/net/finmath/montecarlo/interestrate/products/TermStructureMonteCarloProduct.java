/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 10.02.2004
 */
package net.finmath.montecarlo.interestrate.products;

import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.MonteCarloProduct;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.montecarlo.process.component.factortransform.FactorTransform;
import net.finmath.stochastic.RandomVariable;

/**
 * Interface for products requiring an LIBORModelMonteCarloSimulationModel as base class
 *
 * @author Christian Fries
 * @version 1.0
 */
public interface TermStructureMonteCarloProduct extends MonteCarloProduct {

	/**
	 * This method returns the value random variable of the product within the specified model, evaluated at a given evalutationTime.
	 * Note: For a lattice this is often the value conditional to evalutationTime, for a Monte-Carlo simulation this is the (sum of) value discounted to evaluation time.
	 * Cashflows prior evaluationTime are not considered.
	 *
	 * @param evaluationTime The time on which this products value should be observed.
	 * @param model The model used to price the product.
	 * @return The random variable representing the value of the product discounted to evaluation time
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	RandomVariable getValue(double evaluationTime, TermStructureMonteCarloSimulationModel model) throws CalculationException;

	/**
	 * This method returns the valuation of the product within the specified model, evaluated at a given evalutationTime.
	 * The valuation is returned in terms of a map. The map may contain additional information.
	 * Note: For a lattice this is often the value conditional to evalutationTime, for a Monte-Carlo simulation this is the (sum of) value discounted to evaluation time.
	 * Cashflows prior evaluationTime are not considered.
	 *
	 * @param evaluationTime The time on which this products value should be observed.
	 * @param model The model used to price the product.
	 * @return The random variable representing the value of the product discounted to evaluation time
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	Map<String, Object> getValues(double evaluationTime, TermStructureMonteCarloSimulationModel model) throws CalculationException;

	/**
	 * Overwrite this method if the product supplies a custom FactorDriftInterface to be used in proxy simulation.
	 *
	 * @param referenceScheme The reference scheme
	 * @param targetScheme The target scheme
	 * @return The FactorDriftInterface
	 */
	FactorTransform getFactorDrift(LIBORModelMonteCarloSimulationModel referenceScheme, LIBORModelMonteCarloSimulationModel targetScheme);
}
