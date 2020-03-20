/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 23.03.2014
 */
package net.finmath.fouriermethod.products;

import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.fouriermethod.models.CharacteristicFunctionModel;
import net.finmath.modelling.Model;
import net.finmath.modelling.Product;

/**
 * @author Christian Fries
 *
 * @version 1.0
 */
public interface FourierTransformProduct extends Product {

	@Override
	Double getValue(double evaluationTime, Model model);

	@Override
	Map<String, Object> getValues(double evaluationTime, Model model);

	/**
	 * This method returns the value random variable of the product within the specified model, evaluated at a given evalutationTime.
	 * Note: For a lattice this is often the value conditional to evalutationTime, for a Monte-Carlo simulation this is the (sum of) value discounted to evaluation time.
	 * Cashflows prior evaluationTime are not considered.
	 *
	 * @param model The model used to price the product.
	 * @return The random variable representing the value of the product discounted to evaluation time
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	double getValue(CharacteristicFunctionModel model) throws CalculationException;

	/**
	 * Return the maturity of the associated payoff.
	 *
	 * @return The maturity of the associated payoff.
	 */
	double getMaturity();

	/**
	 * Return the lower bound of the imaginary part of the domain where
	 * the characteristic function can be integrated.
	 *
	 * @return the lower bound of the imaginary part of the domain of integration.
	 */
	double getIntegrationDomainImagLowerBound();

	/**
	 * Return the upper bound of the imaginary part of the domain where
	 * the characteristic function can be integrated.
	 *
	 * @return the upper bound of the imaginary part of the domain of integration.
	 */
	double getIntegrationDomainImagUpperBound();

}
