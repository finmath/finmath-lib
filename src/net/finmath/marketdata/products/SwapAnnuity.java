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

	private TimeDiscretizationInterface	tenor;
	private String discountCurveName;

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

	static public double getSwapAnnuity(TimeDiscretizationInterface tenor, DiscountCurveInterface discountCurve) {
    	return getSwapAnnuity(tenor, discountCurve, null);
	}

	static public double getSwapAnnuity(TimeDiscretizationInterface tenor, ForwardCurveInterface forwardCurve) {
		DiscountCurveInterface discountCurve = new DiscountCurveFromForwardCurve(forwardCurve.getName());
    	return getSwapAnnuity(tenor, discountCurve, new AnalyticModel( new CurveInterface[] {forwardCurve, discountCurve} ));
	}

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
}
