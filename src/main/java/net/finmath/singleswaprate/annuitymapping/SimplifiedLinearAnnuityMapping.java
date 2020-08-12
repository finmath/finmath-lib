package net.finmath.singleswaprate.annuitymapping;

import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurveFromDiscountCurve;
import net.finmath.marketdata.products.Swap;
import net.finmath.marketdata.products.SwapAnnuity;
import net.finmath.time.Schedule;

/**
 * Provides a light-weight linear annuity mapping.
 *
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public class SimplifiedLinearAnnuityMapping implements AnnuityMapping {

	private final double slope;
	private final double intercept;
	private final double initialAnnuity;

	public SimplifiedLinearAnnuityMapping(final Schedule schedule, final double initialAnnuity,
			final double initialSwapRate, final double discountFactor) {
		super();

		double intercept = 0.0;
		for(int index = 0; index < schedule.getNumberOfPeriods(); index++) {
			intercept += schedule.getPeriodLength(index);
		}
		intercept = 1.0/intercept;
		double slope = discountFactor /initialAnnuity -intercept;
		slope /=initialSwapRate;

		this.intercept  =intercept;
		this.slope		=slope;
		this.initialAnnuity = initialAnnuity;
	}

	/**
	 * Construct the annuity mapping.
	 *
	 * @param fixSchedule The schedule of the fix leg of the swap.
	 * @param floatSchedule The schedule of the float leg of the swap.
	 * @param model The model containing the curves.
	 * @param discountCurveName The discount curve.
	 */
	public SimplifiedLinearAnnuityMapping(final Schedule fixSchedule, final Schedule floatSchedule, final AnalyticModel model, final String discountCurveName) {
		super();

		double intercept = 0.0;
		for(int index = 0; index < fixSchedule.getNumberOfPeriods(); index++) {
			intercept += fixSchedule.getPeriodLength(index);
		}
		intercept = 1.0/intercept;

		final ForwardCurve forwardCurve = new ForwardCurveFromDiscountCurve(discountCurveName,
				model.getDiscountCurve(discountCurveName).getReferenceDate() == null ?  fixSchedule.getReferenceDate() :
					model.getDiscountCurve(discountCurveName).getReferenceDate(), "6M");

		final double initialAnnuity		= SwapAnnuity.getSwapAnnuity(fixSchedule.getFixing(0), fixSchedule, model.getDiscountCurve(discountCurveName), model);
		//		double initialSwapRate		= Swap.getForwardSwapRate(schedule, schedule, model.getForwardCurve(forwardCurveName), model);
		final double initialSwapRate		= Swap.getForwardSwapRate(fixSchedule, floatSchedule, forwardCurve, model);

		double slope = 1 /initialAnnuity -intercept; //model.getDiscountCurve(discountCurveName).getValue(schedule.getPeriodStart(0)) /model.getDiscountCurve(discountCurveName).getValue(schedule.getFixing(0))
		slope /=initialSwapRate;

		this.intercept  =intercept;
		this.slope		=slope;
		this.initialAnnuity = initialAnnuity;
	}

	@Override
	public double getValue(final double swapRate) {
		return (swapRate *slope +intercept) *initialAnnuity;
	}

	@Override
	public double getFirstDerivative(final double swapRate) {
		return slope *initialAnnuity;
	}

	@Override
	public double getSecondDerivative(final double swapRate) {
		return 0;
	}

}
