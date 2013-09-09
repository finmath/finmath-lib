/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 26.11.2012
 */
package net.finmath.marketdata.products;

import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.marketdata.model.curves.CurveInterface;
import net.finmath.marketdata.model.curves.DiscountCurveFromForwardCurve;
import net.finmath.marketdata.model.curves.DiscountCurveInterface;
import net.finmath.marketdata.model.curves.ForwardCurveInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * Implements the valuation of a swap annuity using curves (discount curve).
 * Support for day counting is limited to the capabilities of
 * <code>TimeDiscretizationInterface</code>.
 * 
 * @author Christian Fries
 */
public class SwapAnnuity implements AnalyticProductInterface {

	private final TimeDiscretizationInterface	tenor;
	private final String discountCurveName;

	/**
	 * Creates a swap annuity for a given tenor and discount curve.
	 * 
	 * @param tenor Tenor of the swap annuity.
	 * @param discountCurveName Name of the discount curve for the swap annuity.
	 */
    public SwapAnnuity(TimeDiscretizationInterface tenor, String discountCurveName) {
	    super();
	    this.tenor = tenor;
	    this.discountCurveName = discountCurveName;
    }

	/* (non-Javadoc)
	 * @see net.finmath.marketdata.products.AnalyticProductInterface#getValue(net.finmath.marketdata.model.AnalyticModel)
	 */
	@Override
	public double getValue(AnalyticModelInterface model) {	
		DiscountCurveInterface discountCurve = (DiscountCurveInterface) model.getCurve(discountCurveName);

		return getSwapAnnuity(tenor, discountCurve, model);
	}

	/**
	 * Function to calculate an (idealized) swap annuity for a given tenor and discount curve.
	 * 
	 * @param tenor The tenor discretization, i.e., the period start and end dates. End dates are considered payment dates and start of the next period.
	 * @param discountCurve The discount curve.
	 * @return The swap annuity.
	 */
	static public double getSwapAnnuity(TimeDiscretizationInterface tenor, DiscountCurveInterface discountCurve) {
    	return getSwapAnnuity(tenor, discountCurve, null);
	}

	/**
	 * Function to calculate an (idealized) single curve swap annuity for a given tenor and forward curve.
	 * The discount curve used to calculate the annuity is calculated from the forward curve using classical
	 * single curve interpretations of forwards and a default period length. The may be a crude approximation.
	 * 
	 * @param tenor The tenor discretization, i.e., the period start and end dates. End dates are considered payment dates and start of the next period.
	 * @param forwardCurve The forward curve.
	 * @return The swap annuity.
	 */
	static public double getSwapAnnuity(TimeDiscretizationInterface tenor, ForwardCurveInterface forwardCurve) {
		DiscountCurveInterface discountCurve = new DiscountCurveFromForwardCurve(forwardCurve.getName());
    	return getSwapAnnuity(tenor, discountCurve, new AnalyticModel( new CurveInterface[] {forwardCurve, discountCurve} ));
	}

	/**
	 * Function to calculate an (idealized) swap annuity for a given tenor and discount curve.
	 * 
	 * @param tenor The tenor discretization, i.e., the period start and end dates. End dates are considered payment dates and start of the next period.
	 * @param discountCurve The discount curve.
	 * @param model The model, needed only in case the discount curve evaluation depends on an additional curve.
	 * @return The swap annuity.
	 */
	static public double getSwapAnnuity(TimeDiscretizationInterface tenor, DiscountCurveInterface discountCurve, AnalyticModelInterface model) {
    	double value = 0.0;
		for(int periodIndex=0; periodIndex<tenor.getNumberOfTimeSteps(); periodIndex++) {
			double periodStart	= tenor.getTime(periodIndex);
			double periodEnd	= tenor.getTime(periodIndex+1);
			double discountFactor	= discountCurve.getDiscountFactor(model, periodEnd);
			value += (periodEnd-periodStart) * discountFactor;
		}
		return value;
	}
	
	@Override
	public String toString() {
		return "SwapAnnuity [tenor=" + tenor + ", discountCurveName="
				+ discountCurveName + "]";
	}
}
