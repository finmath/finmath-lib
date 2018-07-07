/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 26.11.2012
 */
package net.finmath.analytic.products;

import net.finmath.analytic.model.AnalyticModelInterface;
import net.finmath.analytic.model.curves.DiscountCurveInterface;
import net.finmath.analytic.model.curves.ForwardCurveInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.ScheduleInterface;

/**
 * Implements the valuation of a swap leg with unit notional of 1 using curves (discount curve, forward curve).
 * 
 * The swap leg valuation supports distinct discounting and forward curves.
 * 
 * Support for day counting is provided via the class implementing
 * <code>ScheduleInterface</code>.
 * 
 * @author Christian Fries
 */
public class SwapLeg extends AbstractAnalyticProduct implements AnalyticProductInterface {

	private final ScheduleInterface		legSchedule;
	private final String				forwardCurveName;
	private final double				spread;
	private final String				discountCurveName;
	private final String				discountCurveForNotionalResetName;
	private boolean						isNotionalExchanged = false;

	/**
	 * Creates a swap leg. 
	 * 
	 * @param legSchedule Schedule of the leg.
	 * @param forwardCurveName Name of the forward curve, leave empty if this is a fix leg.
	 * @param spread Fixed spread on the forward or fix rate.
	 * @param discountCurveName Name of the discount curve for the leg.
	 * @param discountCurveForNotionalResetName Name of the discount curve used for notional reset. If it is left empty or equal to discountCurveName then there is no notional reset.
	 * @param isNotionalExchanged If true, the leg will pay notional at the beginning of each swap period and receive notional at the end of the swap period. Note that the cash flow date for the notional is periodStart and periodEnd (not fixingDate and paymentDate).
	 */
	public SwapLeg(ScheduleInterface legSchedule, String forwardCurveName, double spread, String discountCurveName, String discountCurveForNotionalResetName, boolean isNotionalExchanged) {
		super();
		this.legSchedule = legSchedule;
		this.forwardCurveName = forwardCurveName;
		this.spread = spread;
		this.discountCurveName = discountCurveName;
		this.discountCurveForNotionalResetName = discountCurveForNotionalResetName=="" ? discountCurveName : discountCurveForNotionalResetName; // empty discountCurveForNotionalResetName is interpreted as no notional reset
		this.isNotionalExchanged = isNotionalExchanged;
	}

	/**
	 * Creates a swap leg without notional reset.
	 * 
	 * @param legSchedule Schedule of the leg.
	 * @param forwardCurveName Name of the forward curve, leave empty if this is a fix leg.
	 * @param spread Fixed spread on the forward or fix rate.
	 * @param discountCurveName Name of the discount curve for the leg.
	 * @param isNotionalExchanged If true, the leg will pay notional at the beginning of each swap period and receive notional at the end of the swap period. Note that the cash flow date for the notional is periodStart and periodEnd (not fixingDate and paymentDate).
	 */
	public SwapLeg(ScheduleInterface legSchedule, String forwardCurveName, double spread, String discountCurveName, boolean isNotionalExchanged) {
		this(legSchedule, forwardCurveName, spread, discountCurveName, discountCurveName, isNotionalExchanged);
	}

	/**
	 * Creates a swap leg without notional reset and without notional exchange.
	 * 
	 * @param legSchedule Schedule of the leg.
	 * @param forwardCurveName Name of the forward curve, leave empty if this is a fix leg.
	 * @param spread Fixed spread on the forward or fix rate.
	 * @param discountCurveName Name of the discount curve for the leg.
	 */
	public SwapLeg(ScheduleInterface legSchedule, String forwardCurveName, double spread, String discountCurveName) {
		this(legSchedule, forwardCurveName, spread, discountCurveName, discountCurveName, false);
	}


	@Override
	public RandomVariableInterface getValue(double evaluationTime, AnalyticModelInterface model) {	
		if(model==null) {
			throw new IllegalArgumentException("model==null");
		}

		DiscountCurveInterface discountCurve = model.getDiscountCurve(discountCurveName);
		DiscountCurveInterface discountCurveForNotionalReset = model.getDiscountCurve(discountCurveForNotionalResetName);
		if(discountCurve == null) {
			throw new IllegalArgumentException("No discount curve with name '" + discountCurveName + "' was found in the model:\n" + model.toString());
		}
		if(discountCurveForNotionalReset == null) {
			throw new IllegalArgumentException("No discountCurveForNotionalReset with name '" + discountCurveForNotionalResetName + "' was found in the model:\n" + model.toString());
		}

		ForwardCurveInterface forwardCurve = model.getForwardCurve(forwardCurveName);
		if(forwardCurve == null && forwardCurveName != null && forwardCurveName.length() > 0) {
			throw new IllegalArgumentException("No forward curve with name '" + forwardCurveName + "' was found in the model:\n" + model.toString());
		}

		RandomVariableInterface value = model.getRandomVariableForConstant(0.0);
		for(int periodIndex=0; periodIndex<legSchedule.getNumberOfPeriods(); periodIndex++) {
			double fixingDate	= legSchedule.getFixing(periodIndex);
			double periodStart	= legSchedule.getPeriodStart(periodIndex);
			double periodEnd	= legSchedule.getPeriodEnd(periodIndex);
			double paymentDate	= legSchedule.getPayment(periodIndex);
			double periodLength	= legSchedule.getPeriodLength(periodIndex);

			RandomVariableInterface forward = model.getRandomVariableForConstant(spread);
			if(forwardCurve != null) {
				forward = forward.add(forwardCurve.getForward(model, fixingDate, paymentDate-fixingDate));
			}

			// note that notional=1 if discountCurveForNotionalReset=discountCurve
			RandomVariableInterface notional = discountCurveForNotionalReset.getDiscountFactor(model,periodStart).div(discountCurve.getDiscountFactor(model,periodStart));
			RandomVariableInterface discountFactor = paymentDate > evaluationTime ? discountCurve.getDiscountFactor(model, paymentDate) : model.getRandomVariableForConstant(0.0);
			value = value.add(notional.mult(forward).mult(periodLength).mult(discountFactor));

			// Consider notional payments if required
			if(isNotionalExchanged) {
				if(periodEnd > evaluationTime) {
					value = value.add(notional.mult(discountCurve.getDiscountFactor(model, periodEnd)));
				}
				if(periodStart > evaluationTime) {
					value = value.sub(notional.mult(discountCurve.getDiscountFactor(model, periodStart)));
				}
			}
		}

		return value.div(discountCurve.getDiscountFactor(model, evaluationTime));
	}

	public ScheduleInterface getSchedule() {
		return legSchedule;
	}

	public String getForwardCurveName() {
		return forwardCurveName;
	}

	public double getSpread() {
		return spread;
	}

	public String getDiscountCurveName() {
		return discountCurveName;
	}

	public boolean isNotionalExchanged() {
		return isNotionalExchanged;
	}

	@Override
	public String toString() {
		return "SwapLeg [legSchedule=" + legSchedule 
				+ ", forwardCurveName=" + forwardCurveName 
				+ ", spread=" + spread
				+ ", discountCurveName=" + discountCurveName
				+ ", discountCurveForNotionalResetName=" + discountCurveForNotionalResetName
				+ ", isNotionalExchanged=" + isNotionalExchanged + "]";
	}
}
