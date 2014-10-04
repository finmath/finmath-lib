/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 26.11.2012
 */
package net.finmath.marketdata.products;

import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.marketdata.model.curves.DiscountCurveInterface;

/**
 * Implements the valuation of a single cashflow by a discount curve.
 * 
 * @TODO: Currency is neither checked nor used.
 * 
 * @author Christian Fries
 */
public class Cashflow extends AbstractAnalyticProduct implements AnalyticProductInterface {

	private final double	flowAmount;
	private final double	flowDate;
	private final boolean	isPayer;
	private final String	discountCurveName;

	/**
	 * Create a single deterministic cashflow at a fixed time.
	 * 
	 * @param currency The currency.
	 * @param flowAmount The amount of the cash flow.
	 * @param flowDate The flow date.
	 * @param isPayer If true, this cash flow will be multiplied by -1 prior valuation.
	 * @param discountCurveName Name of the discount curve for the cashflow.
	 */
	public Cashflow(String currency, double flowAmount, double flowDate, boolean isPayer, String discountCurveName) {
		super();
		this.flowAmount = flowAmount;
		this.flowDate = flowDate;
		this.isPayer = isPayer;
		this.discountCurveName = discountCurveName;
	}


	@Override
	public double getValue(double evaluationTime, AnalyticModelInterface model) {	
		DiscountCurveInterface	discountCurve	= model.getDiscountCurve(discountCurveName);
		
		if(discountCurve == null && flowDate > evaluationTime) throw new IllegalArgumentException("Model does not provide a discount curve under the name \"" + discountCurveName + "\"");
		
		double discountFactor	= flowDate > evaluationTime ? discountCurve.getDiscountFactor(model, flowDate) : 0.0;

		double value = (isPayer ? -1.0 : 1.0) * flowAmount * discountFactor;

		return value / discountCurve.getDiscountFactor(model, evaluationTime);
	}
}
