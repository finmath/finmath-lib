/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 12.10.2013
 */

package net.finmath.marketdata.products;

import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.modelling.ModelInterface;

/**
 * @author Christian Fries
 *
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

	public double getValue(AnalyticModelInterface model) {
		return getValue(0.0, model);
	}
}
