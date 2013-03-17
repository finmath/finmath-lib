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
 * Implements the valuation of a swap using curves (discount curve, forward curve).
 * The swap valuation supports distinct discounting and forward curve.
 * Support for day counting is limited to the capabilities of
 * <code>TimeDiscretizationInterface</code>.
 * 
 * @author Christian Fries
 */
public class Swap implements AnalyticProductInterface {

	private SwapLeg legReceiver;
	private SwapLeg legPayer;
	
	/**
	 * Creates a swap. The swap has a unit notional of 1.
	 * 
	 * @param tenorReceiveLeg Tenor of the receiver leg.
	 * @param spreadReceive Fixed spread on the forward or fix rate.
	 * @param discountCurveReceiveName Name of the discount curve for the receiver leg.
	 * @param tenorPayLeg Tenor of the payer leg.
	 * @param forwardCurvePayName Name of the forward curve, leave empty if this is a fix leg.
	 * @param discountCurvePayName Name of the discount curve for the payer leg.
	 */
    public Swap(TimeDiscretizationInterface tenorReceiveLeg,
            double spreadReceive,
            String discountCurveReceiveName,
            TimeDiscretizationInterface tenorPayLeg,
            String forwardCurvePayName,
            String discountCurvePayName) {
	    super();

		legReceiver		= new SwapLeg(tenorReceiveLeg, null /* forwardCurveReceiveName */, spreadReceive, discountCurveReceiveName);
		legPayer		= new SwapLeg(tenorPayLeg, forwardCurvePayName, 0.0 /* spreadPay */, discountCurvePayName);

    }


	/**
	 * Creates a swap.
	 * 
	 * @param tenorReceiveLeg Tenor of the receiver leg.
	 * @param forwardCurveReceiveName Name of the forward curve, leave empty if this is a fix leg.
	 * @param spreadReceive Fixed spread on the forward or fix rate.
	 * @param discountCurveReceiveName Name of the discount curve for the receiver leg.
	 * @param tenorPayLeg Tenor of the payer leg.
	 * @param forwardCurvePayName Name of the forward curve, leave empty if this is a fix leg.
	 * @param spreadPay Fixed spread on the forward or fix rate.
	 * @param discountCurvePayName Name of the discount curve for the payer leg.
	 */
    public Swap(TimeDiscretizationInterface tenorReceiveLeg,
            String forwardCurveReceiveName, double spreadReceive,
            String discountCurveReceiveName,
            TimeDiscretizationInterface tenorPayLeg,
            String forwardCurvePayName, double spreadPay,
            String discountCurvePayName) {
	    super();
		legReceiver		= new SwapLeg(tenorReceiveLeg, forwardCurveReceiveName, spreadReceive, discountCurveReceiveName);
		legPayer		= new SwapLeg(tenorPayLeg, forwardCurvePayName, spreadPay, discountCurvePayName);
    }

	/* (non-Javadoc)
	 * @see net.finmath.marketdata.products.AnalyticProductInterface#getValue(net.finmath.marketdata.model.AnalyticModel)
	 */
	@Override
	public double getValue(AnalyticModelInterface model) {	
		
		double valueReceiverLeg	= legReceiver.getValue(model);
		double valuePayerLeg	= legPayer.getValue(model);
		
		return valueReceiverLeg - valuePayerLeg;
	}

	static public double getForwardSwapRate(TimeDiscretizationInterface fixTenor, TimeDiscretizationInterface floatTenor, ForwardCurveInterface forwardCurve) {
		return getForwardSwapRate(fixTenor, floatTenor, forwardCurve, null);
	}

	static public double getForwardSwapRate(TimeDiscretizationInterface fixTenor, TimeDiscretizationInterface floatTenor, ForwardCurveInterface forwardCurve, AnalyticModelInterface model) {
		DiscountCurveInterface discountCurve = model == null ? null : model.getDiscountCurve(forwardCurve.getDiscountCurveName());
		if(discountCurve == null) {
			discountCurve	= new DiscountCurveFromForwardCurve(forwardCurve.getName());
			model			= new AnalyticModel(new CurveInterface[] { forwardCurve });
		}

		double swapAnnuity	= SwapAnnuity.getSwapAnnuity(fixTenor, discountCurve, model);
		
		double floatLeg = 0;
		for(int periodIndex=0; periodIndex<floatTenor.getNumberOfTimeSteps(); periodIndex++) {
			double periodStart	= floatTenor.getTime(periodIndex);
			double periodEnd	= floatTenor.getTime(periodIndex+1);
			double forward			= forwardCurve.getForward(model, periodStart);
			double discountFactor	= discountCurve.getDiscountFactor(model, periodEnd);
			floatLeg += forward * (periodEnd-periodStart) * discountFactor;
		}
		
		return floatLeg / swapAnnuity;
	}
}
