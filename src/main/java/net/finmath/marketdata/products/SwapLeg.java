/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 26.11.2012
 */
package net.finmath.marketdata.products;

import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.marketdata.model.curves.DiscountCurveInterface;
import net.finmath.marketdata.model.curves.ForwardCurveInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * Implements the valuation of a swap leg using curves (discount curve, forward curve).
 * The swap leg valuation supports distinct discounting and forward curves.
 * Support for day counting is limited to the capabilities of
 * <code>TimeDiscretizationInterface</code>.
 * 
 * @author Christian Fries
 */
public class SwapLeg implements AnalyticProductInterface {

	private final TimeDiscretizationInterface	tenorLeg;
	private final String						forwardCurveName;
	private final double						spread;
	private final String						discountCurveName;
	private boolean								isNotionalExchanged = false;

	/**
	 * Creates a swap leg. The swap leg has a unit notional of 1.
	 * 
	 * @param tenorLeg Tenor of the leg.
	 * @param forwardCurveName Name of the forward curve, leave empty if this is a fix leg.
	 * @param spread Fixed spread on the forward or fix rate.
	 * @param discountCurveName Name of the discount curve for the leg.
	 * @param isNotionalExchanged If true, the leg will pay notional at the beginning of the swap and receive notional at the end of the swap.
	 */
    public SwapLeg(TimeDiscretizationInterface tenorLeg, String forwardCurveName, double spread, String discountCurveName, boolean isNotionalExchanged) {
	    super();
	    this.tenorLeg = tenorLeg;
	    this.forwardCurveName = forwardCurveName;
	    this.spread = spread;
	    this.discountCurveName = discountCurveName;
	    this.isNotionalExchanged = isNotionalExchanged;
    }

    /**
	 * Creates a swap leg (without notional exchange). The swap leg has a unit notional of 1.
	 * 
	 * @param tenorLeg Tenor of the leg.
	 * @param forwardCurveName Name of the forward curve, leave empty if this is a fix leg.
	 * @param spread Fixed spread on the forward or fix rate.
	 * @param discountCurveName Name of the discount curve for the leg.
	 */
    public SwapLeg(TimeDiscretizationInterface tenorLeg, String forwardCurveName, double spread, String discountCurveName) {
	    super();
	    this.tenorLeg = tenorLeg;
	    this.forwardCurveName = forwardCurveName;
	    this.spread = spread;
	    this.discountCurveName = discountCurveName;
    }


	/* (non-Javadoc)
	 * @see net.finmath.marketdata.products.AnalyticProductInterface#getValue(net.finmath.marketdata.model.AnalyticModel)
	 */
	@Override
	public double getValue(AnalyticModelInterface model) {	
		ForwardCurveInterface	forwardCurve	= model.getForwardCurve(forwardCurveName);
		DiscountCurveInterface	discountCurve	= model.getDiscountCurve(discountCurveName);
		
		DiscountCurveInterface	discountCurveForForward = null;
		if(forwardCurve == null && forwardCurveName != null && forwardCurveName.length() > 0) {
			// User might like to get forward from discount curve.
			discountCurveForForward	= model.getDiscountCurve(forwardCurveName);
			
			if(discountCurveForForward == null) {
				// User specified a name for the forward curve, but no curve was found.
				throw new IllegalArgumentException("No curve of the name " + forwardCurveName + " was found in the model.");
			}
		}

		double value = 0.0;
		for(int periodIndex=0; periodIndex<tenorLeg.getNumberOfTimeSteps(); periodIndex++) {
			double periodStart	= tenorLeg.getTime(periodIndex);
			double periodEnd	= tenorLeg.getTime(periodIndex+1);
			double forward		= spread;
			if(forwardCurve != null) {
				forward			+= forwardCurve.getForward(model, periodStart);
			}
			else if(discountCurveForForward != null) {
				forward			+= (discountCurveForForward.getDiscountFactor(periodStart) / discountCurveForForward.getDiscountFactor(periodEnd) - 1.0) / (periodEnd - periodStart);
			}
			double discountFactor	= discountCurve.getDiscountFactor(model, periodEnd);
			value += forward * (periodEnd-periodStart) * discountFactor;
			if(isNotionalExchanged) value += discountCurve.getDiscountFactor(model, periodEnd) - discountCurve.getDiscountFactor(model, periodStart);
		}
		return value;		
	}

	@Override
	public String toString() {
		return "SwapLeg [tenorLeg=" + tenorLeg + ", forwardCurveName="
				+ forwardCurveName + ", spread=" + spread
				+ ", discountCurveName=" + discountCurveName
				+ ", isNotionalExchanged=" + isNotionalExchanged + "]";
	}
}
