/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 26.11.2012
 */
package net.finmath.marketdata2.products;

import net.finmath.marketdata2.model.AnalyticModel;
import net.finmath.marketdata2.model.curves.DiscountCurveInterface;
import net.finmath.stochastic.RandomVariable;

/**
 * Implements the valuation of a single cashflow by a discount curve.
 *
 * @TODO Currency is neither checked nor used.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class Cashflow extends AbstractAnalyticProduct implements AnalyticProduct {

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
	public Cashflow(final String currency, final double flowAmount, final double flowDate, final boolean isPayer, final String discountCurveName) {
		super();
		this.flowAmount = flowAmount;
		this.flowDate = flowDate;
		this.isPayer = isPayer;
		this.discountCurveName = discountCurveName;
	}


	@Override
	public RandomVariable getValue(final double evaluationTime, final AnalyticModel model) {
		if(model==null) {
			throw new IllegalArgumentException("model==null");
		}

		final DiscountCurveInterface discountCurve = model.getDiscountCurve(discountCurveName);
		if(discountCurve == null) {
			throw new IllegalArgumentException("No discount curve with name '" + discountCurveName + "' was found in the model:\n" + model.toString());
		}

		final RandomVariable discountFactor = flowDate > evaluationTime ? discountCurve.getDiscountFactor(model, flowDate) : model.getRandomVariableForConstant(0.0);
		final RandomVariable value = discountFactor.mult(flowAmount * (isPayer ? -1.0 : 1.0));

		return value.div(discountCurve.getDiscountFactor(model, evaluationTime));
	}
}
