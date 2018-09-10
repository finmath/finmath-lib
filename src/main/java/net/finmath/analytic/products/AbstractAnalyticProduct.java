/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 12.10.2013
 */

package net.finmath.analytic.products;

import net.finmath.analytic.model.AnalyticModelInterface;
import net.finmath.modelling.ModelInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * @author Christian Fries
 *
 * @version 1.0
 */
public abstract class AbstractAnalyticProduct implements AnalyticProductInterface {

	/* (non-Javadoc)
	 * @see net.finmath.marketdata.products.ProductInterface#getValue(double, net.finmath.marketdata.products.ModelInterface)
	 */
	@Override
	public Object getValue(double evaluationTime, ModelInterface model) {
		throw new IllegalArgumentException("The product " + this.getClass()
		+ " cannot be valued against a model " + model.getClass() + "."
		+ "It requires a model of type " + AnalyticModelInterface.class + ".");
	}

	public RandomVariableInterface getValue(AnalyticModelInterface model) {
		return getValue(0.0, model);
	}
}
