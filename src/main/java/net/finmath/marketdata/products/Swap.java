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
import net.finmath.time.RegularSchedule;
import net.finmath.time.ScheduleInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * Implements the valuation of a swap using curves (discount curve, forward curve).
 * The swap valuation supports distinct discounting and forward curve.
 * Support for day counting is limited to the capabilities of
 * <code>TimeDiscretizationInterface</code>.
 * 
 * The swap is just the composition of two <code>SwapLeg</code>s, namely the
 * receiver leg and the payer leg. The value of the swap is the value of the receiver leg minus the value of the payer leg.
 * 
 * @author Christian Fries
 */
public class Swap extends AbstractAnalyticProduct implements AnalyticProductInterface {

	private final SwapLeg legReceiver;
	private final SwapLeg legPayer;
	
	/**
	 * Creates a swap with notional exchange. The swap has a unit notional of 1.
	 * 
	 * @param scheduleReceiveLeg Schedule of the receiver leg.
	 * @param spreadReceive Fixed spread on the forward or fix rate.
	 * @param discountCurveReceiveName Name of the discount curve for the receiver leg.
	 * @param schedulePayLeg Schedule of the payer leg.
	 * @param forwardCurvePayName Name of the forward curve, leave empty if this is a fix leg.
	 * @param discountCurvePayName Name of the discount curve for the payer leg.
	 */
    public Swap(ScheduleInterface scheduleReceiveLeg,
            double spreadReceive,
            String discountCurveReceiveName,
            ScheduleInterface schedulePayLeg,
            String forwardCurvePayName,
            String discountCurvePayName) {
	    super();

		legReceiver		= new SwapLeg(scheduleReceiveLeg, null /* forwardCurveReceiveName */, spreadReceive, discountCurveReceiveName, true);
		legPayer		= new SwapLeg(schedulePayLeg, forwardCurvePayName, 0.0 /* spreadPay */, discountCurvePayName, true);

    }


	/**
	 * Creates a swap with notional exchange. The swap has a unit notional of 1.
	 * 
	 * @param scheduleReceiveLeg Schedule of the receiver leg.
	 * @param forwardCurveReceiveName Name of the forward curve, leave empty if this is a fix leg.
	 * @param spreadReceive Fixed spread on the forward or fix rate.
	 * @param discountCurveReceiveName Name of the discount curve for the receiver leg.
	 * @param schedulePayLeg Schedule of the payer leg.
	 * @param forwardCurvePayName Name of the forward curve, leave empty if this is a fix leg.
	 * @param spreadPay Fixed spread on the forward or fix rate.
	 * @param discountCurvePayName Name of the discount curve for the payer leg.
	 */
    public Swap(ScheduleInterface scheduleReceiveLeg,
            String forwardCurveReceiveName, double spreadReceive,
            String discountCurveReceiveName,
            ScheduleInterface schedulePayLeg,
            String forwardCurvePayName, double spreadPay,
            String discountCurvePayName) {
	    super();
		legReceiver		= new SwapLeg(scheduleReceiveLeg, forwardCurveReceiveName, spreadReceive, discountCurveReceiveName, true);
		legPayer		= new SwapLeg(schedulePayLeg, forwardCurvePayName, spreadPay, discountCurvePayName, true);
    }

	/* (non-Javadoc)
	 * @see net.finmath.marketdata.products.AnalyticProductInterface#getValue(double, net.finmath.marketdata.model.AnalyticModelInterface)
	 */
	@Override
	public double getValue(double evaluationTime, AnalyticModelInterface model) {	
		
		double valueReceiverLeg	= legReceiver.getValue(evaluationTime, model);
		double valuePayerLeg	= legPayer.getValue(evaluationTime, model);
		
		return valueReceiverLeg - valuePayerLeg;
	}

	static public double getForwardSwapRate(TimeDiscretizationInterface fixTenor, TimeDiscretizationInterface floatTenor, ForwardCurveInterface forwardCurve) {
		return getForwardSwapRate(new RegularSchedule(fixTenor), new RegularSchedule(floatTenor), forwardCurve);
	}

	static public double getForwardSwapRate(TimeDiscretizationInterface fixTenor, TimeDiscretizationInterface floatTenor, ForwardCurveInterface forwardCurve, DiscountCurveInterface discountCurve) {
		AnalyticModel model = null;
		if(discountCurve != null) {
			model			= new AnalyticModel(new CurveInterface[] { forwardCurve, discountCurve });
		}
		return getForwardSwapRate(new RegularSchedule(fixTenor), new RegularSchedule(floatTenor), forwardCurve, model);
	}

	static public double getForwardSwapRate(ScheduleInterface fixSchedule, ScheduleInterface floatSchedule, ForwardCurveInterface forwardCurve) {
		return getForwardSwapRate(fixSchedule, floatSchedule, forwardCurve, null);
	}

	static public double getForwardSwapRate(ScheduleInterface fixSchedule, ScheduleInterface floatSchedule, ForwardCurveInterface forwardCurve, AnalyticModelInterface model) {
		DiscountCurveInterface discountCurve = model == null ? null : model.getDiscountCurve(forwardCurve.getDiscountCurveName());
		if(discountCurve == null) {
			discountCurve	= new DiscountCurveFromForwardCurve(forwardCurve.getName());
			model			= new AnalyticModel(new CurveInterface[] { forwardCurve, discountCurve });
		}

		double evaluationTime = fixSchedule.getFixing(0);	// Consider all values
		double swapAnnuity	= SwapAnnuity.getSwapAnnuity(evaluationTime, fixSchedule, discountCurve, model);
		
		double floatLeg = 0;
		for(int periodIndex=0; periodIndex<floatSchedule.getNumberOfPeriods(); periodIndex++) {
			double fixing			= floatSchedule.getFixing(periodIndex);
			double payment			= floatSchedule.getPayment(periodIndex);
			double periodLength		= floatSchedule.getPeriodLength(periodIndex);

			double forward			= forwardCurve.getForward(model, fixing);
			double discountFactor	= discountCurve.getDiscountFactor(model, payment);

			floatLeg += forward * periodLength * discountFactor;
		}
		
		double valueFloatLeg = floatLeg / discountCurve.getDiscountFactor(model, evaluationTime);
		
		return valueFloatLeg / swapAnnuity;
	}

	/**
	 * Return the receiver leg of the swap, i.e. the leg who's value is added to the swap value.
	 * 
	 * @return The receiver leg of the swap.
	 */
	public SwapLeg getLegReceiver() {
		return legReceiver;
	}

	/**
	 * Return the payer leg of the swap, i.e. the leg who's value is subtracted from the swap value.
	 * 
	 * @return The payer leg of the swap.
	 */
	public SwapLeg getLegPayer() {
		return legPayer;
	}

	@Override
	public String toString() {
		return "Swap [legReceiver=" + legReceiver + ", legPayer=" + legPayer
				+ "]";
	}
}
