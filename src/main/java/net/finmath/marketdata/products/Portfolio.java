/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 02.03.2014
 */

package net.finmath.marketdata.products;

import java.util.ArrayList;

import net.finmath.marketdata.model.AnalyticModelInterface;

/**
 * Implements the valuation of a portfolio of products implementing
 * <code>AnalyticProductInterface</code>.
 * 
 * @author Christian Fries
 */
public class Portfolio extends AbstractAnalyticProduct implements AnalyticProductInterface {

	private ArrayList<AnalyticProductInterface>	products;
	private ArrayList<Double>					weights;

	/**
	 * Create a portfolio of products implementing
	 * <code>AnalyticProductInterface</code>. The portfolio consists
	 * of an array of products and a corresponding array of weights.
	 * The value of the portfolio is given by the sum over
	 * <code>
	 * 	weights[i] * products.get(i).getValue(evaluationTime, model)
	 * </code>
	 * 
	 * Note that a product in the array of products may itself be
	 * a <code>Portfolio</code> (hence you may easily combine portfolios).
	 * 
	 * @param products Array of products implementing <code>AnalyticProductInterface</code>.
	 * @param weights Array of weights used in the valuation as a multiplicator.
	 */
	public Portfolio(ArrayList<AnalyticProductInterface> products, ArrayList<Double> weights) {
		super();
		this.products = (ArrayList<AnalyticProductInterface>)products.clone();
		this.weights = (ArrayList<Double>)weights.clone();
	}

	@Override
	public double getValue(double evaluationTime, AnalyticModelInterface model) {
		double value = 0.0;
		for(int i=0; i<products.size(); i++) value += weights.get(i) * products.get(i).getValue(evaluationTime, model);

		return value;
	}
}
