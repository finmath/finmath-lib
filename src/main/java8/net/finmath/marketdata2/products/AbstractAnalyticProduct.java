/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 12.10.2013
 */

package net.finmath.marketdata2.products;

import net.finmath.marketdata2.model.AnalyticModel;
import net.finmath.modelling.Model;
import net.finmath.stochastic.RandomVariable;

/**
 * @author Christian Fries
 *
 * @version 1.0
 */
public abstract class AbstractAnalyticProduct implements AnalyticProduct {

	/* (non-Javadoc)
	 * @see net.finmath.marketdata.products.ProductInterface#getValue(double, net.finmath.marketdata.products.ModelInterface)
	 */
	@Override
	public Object getValue(final double evaluationTime, final Model model) {
		throw new IllegalArgumentException("The product " + this.getClass()
		+ " cannot be valued against a model " + model.getClass() + "."
		+ "It requires a model of type " + AnalyticModel.class + ".");
	}

	public RandomVariable getValue(final AnalyticModel model) {
		return getValue(0.0, model);
	}
}
