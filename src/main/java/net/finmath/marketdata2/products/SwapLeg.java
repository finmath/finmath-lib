/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 26.11.2012
 */
package net.finmath.marketdata2.products;

import net.finmath.marketdata2.model.AnalyticModel;
import net.finmath.marketdata2.model.curves.DiscountCurveInterface;
import net.finmath.marketdata2.model.curves.ForwardCurveInterface;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.Schedule;

/**
 * Implements the valuation of a swap leg with unit notional of 1 using curves (discount curve, forward curve).
 *
 * The swap leg valuation supports distinct discounting and forward curves.
 *
 * Support for day counting is provided via the class implementing
 * <code>Schedule</code>.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class SwapLeg extends AbstractAnalyticProduct implements AnalyticProduct {

	private final Schedule		legSchedule;
	private final String				forwardCurveName;
	private final double				spread;
	private final String				discountCurveName;
	private final String				discountCurveForNotionalResetName;
	private boolean						isNotionalExchanged = false;

	/**
	 * Creates a swap leg.
	 *
	 * @param legSchedule ScheduleFromPeriods of the leg.
	 * @param forwardCurveName Name of the forward curve, leave empty if this is a fix leg.
	 * @param spread Fixed spread on the forward or fix rate.
	 * @param discountCurveName Name of the discount curve for the leg.
	 * @param discountCurveForNotionalResetName Name of the discount curve used for notional reset. If it is left empty or equal to discountCurveName then there is no notional reset.
	 * @param isNotionalExchanged If true, the leg will pay notional at the beginning of each swap period and receive notional at the end of the swap period. Note that the cash flow date for the notional is periodStart and periodEnd (not fixingDate and paymentDate).
	 */
	public SwapLeg(final Schedule legSchedule, final String forwardCurveName, final double spread, final String discountCurveName, final String discountCurveForNotionalResetName, final boolean isNotionalExchanged) {
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
	 * @param legSchedule ScheduleFromPeriods of the leg.
	 * @param forwardCurveName Name of the forward curve, leave empty if this is a fix leg.
	 * @param spread Fixed spread on the forward or fix rate.
	 * @param discountCurveName Name of the discount curve for the leg.
	 * @param isNotionalExchanged If true, the leg will pay notional at the beginning of each swap period and receive notional at the end of the swap period. Note that the cash flow date for the notional is periodStart and periodEnd (not fixingDate and paymentDate).
	 */
	public SwapLeg(final Schedule legSchedule, final String forwardCurveName, final double spread, final String discountCurveName, final boolean isNotionalExchanged) {
		this(legSchedule, forwardCurveName, spread, discountCurveName, discountCurveName, isNotionalExchanged);
	}

	/**
	 * Creates a swap leg without notional reset and without notional exchange.
	 *
	 * @param legSchedule ScheduleFromPeriods of the leg.
	 * @param forwardCurveName Name of the forward curve, leave empty if this is a fix leg.
	 * @param spread Fixed spread on the forward or fix rate.
	 * @param discountCurveName Name of the discount curve for the leg.
	 */
	public SwapLeg(final Schedule legSchedule, final String forwardCurveName, final double spread, final String discountCurveName) {
		this(legSchedule, forwardCurveName, spread, discountCurveName, discountCurveName, false);
	}


	@Override
	public RandomVariable getValue(final double evaluationTime, final AnalyticModel model) {
		if(model==null) {
			throw new IllegalArgumentException("model==null");
		}

		final DiscountCurveInterface discountCurve = model.getDiscountCurve(discountCurveName);
		final DiscountCurveInterface discountCurveForNotionalReset = model.getDiscountCurve(discountCurveForNotionalResetName);
		if(discountCurve == null) {
			throw new IllegalArgumentException("No discount curve with name '" + discountCurveName + "' was found in the model:\n" + model.toString());
		}
		if(discountCurveForNotionalReset == null) {
			throw new IllegalArgumentException("No discountCurveForNotionalReset with name '" + discountCurveForNotionalResetName + "' was found in the model:\n" + model.toString());
		}

		final ForwardCurveInterface forwardCurve = model.getForwardCurve(forwardCurveName);
		if(forwardCurve == null && forwardCurveName != null && forwardCurveName.length() > 0) {
			throw new IllegalArgumentException("No forward curve with name '" + forwardCurveName + "' was found in the model:\n" + model.toString());
		}

		RandomVariable value = model.getRandomVariableForConstant(0.0);
		for(int periodIndex=0; periodIndex<legSchedule.getNumberOfPeriods(); periodIndex++) {
			final double fixingDate	= legSchedule.getFixing(periodIndex);
			final double periodStart	= legSchedule.getPeriodStart(periodIndex);
			final double periodEnd	= legSchedule.getPeriodEnd(periodIndex);
			final double paymentDate	= legSchedule.getPayment(periodIndex);
			final double periodLength	= legSchedule.getPeriodLength(periodIndex);

			RandomVariable forward = model.getRandomVariableForConstant(spread);
			if(forwardCurve != null) {
				forward = forward.add(forwardCurve.getForward(model, fixingDate, paymentDate-fixingDate));
			}

			// note that notional=1 if discountCurveForNotionalReset=discountCurve
			final RandomVariable notional = discountCurveForNotionalReset.getDiscountFactor(model,periodStart).div(discountCurve.getDiscountFactor(model,periodStart));
			final RandomVariable discountFactor = paymentDate > evaluationTime ? discountCurve.getDiscountFactor(model, paymentDate) : model.getRandomVariableForConstant(0.0);
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

	public Schedule getSchedule() {
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
