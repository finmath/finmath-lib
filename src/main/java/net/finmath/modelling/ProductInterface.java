/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 11.10.2013
 */

package net.finmath.modelling;

import net.finmath.marketdata.model.AnalyticModelInterface;

/**
 * @author Christian Fries
 *
 */
public interface ProductInterface {

	/**
	 * Return the valuation of the product using the given model.
	 * 
	 * @param model The model under which the product is valued.
	 * @return The value of the product using the given model.
	 */
    Object getValue(double evaluationTime, ModelInterface model);
}
