/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 26.11.2012
 */
package net.finmath.marketdata2.products;

import net.finmath.marketdata2.model.AnalyticModel;
import net.finmath.marketdata2.model.AnalyticModelFromCurvesAndVols;
import net.finmath.marketdata2.model.curves.Curve;
import net.finmath.marketdata2.model.curves.DiscountCurveFromForwardCurve;
import net.finmath.marketdata2.model.curves.DiscountCurveInterface;
import net.finmath.marketdata2.model.curves.ForwardCurveInterface;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.RegularSchedule;
import net.finmath.time.Schedule;
import net.finmath.time.TimeDiscretization;

/**
 * Implements the valuation of a swap annuity using curves (discount curve).
 * Support for day counting is limited to the capabilities of
 * <code>TimeDiscretization</code>.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class SwapAnnuity extends AbstractAnalyticProduct implements AnalyticProduct {

	private final Schedule	schedule;
	private final String			discountCurveName;

	/**
	 * Creates a swap annuity for a given schedule and discount curve.
	 *
	 * @param schedule TenorFromArray of the swap annuity.
	 * @param discountCurveName Name of the discount curve for the swap annuity.
	 */
	public SwapAnnuity(final Schedule schedule, final String discountCurveName) {
		super();
		this.schedule = schedule;
		this.discountCurveName = discountCurveName;
	}

	@Override
	public RandomVariable getValue(final double evaluationTime, final AnalyticModel model) {
		final DiscountCurveInterface discountCurve = (DiscountCurveInterface) model.getCurve(discountCurveName);

		return getSwapAnnuity(evaluationTime, schedule, discountCurve, model);
	}

	/**
	 * Function to calculate an (idealized) swap annuity for a given schedule and discount curve.
	 *
	 * @param tenor The schedule discretization, i.e., the period start and end dates. End dates are considered payment dates and start of the next period.
	 * @param discountCurve The discount curve.
	 * @return The swap annuity.
	 */
	public static RandomVariable getSwapAnnuity(final TimeDiscretization tenor, final DiscountCurveInterface discountCurve) {
		return getSwapAnnuity(new RegularSchedule(tenor), discountCurve);
	}

	/**
	 * Function to calculate an (idealized) single curve swap annuity for a given schedule and forward curve.
	 * The discount curve used to calculate the annuity is calculated from the forward curve using classical
	 * single curve interpretations of forwards and a default period length. The may be a crude approximation.
	 *
	 * @param tenor The schedule discretization, i.e., the period start and end dates. End dates are considered payment dates and start of the next period.
	 * @param forwardCurve The forward curve.
	 * @return The swap annuity.
	 */
	public static RandomVariable getSwapAnnuity(final TimeDiscretization tenor, final ForwardCurveInterface forwardCurve) {
		return getSwapAnnuity(new RegularSchedule(tenor), forwardCurve);
	}

	/**
	 * Function to calculate an (idealized) swap annuity for a given schedule and discount curve.
	 *
	 * Note: This method will consider evaluationTime being 0, see {@link net.finmath.marketdata2.products.SwapAnnuity#getSwapAnnuity(double, Schedule, DiscountCurveInterface, AnalyticModel)}.
	 *
	 * @param schedule The schedule discretization, i.e., the period start and end dates. End dates are considered payment dates and start of the next period.
	 * @param discountCurve The discount curve.
	 * @return The swap annuity.
	 */
	public static RandomVariable getSwapAnnuity(final Schedule schedule, final DiscountCurveInterface discountCurve) {
		final double evaluationTime = 0.0;	// Consider only payment time > 0
		return getSwapAnnuity(evaluationTime, schedule, discountCurve, null);
	}

	/**
	 * Function to calculate an (idealized) single curve swap annuity for a given schedule and forward curve.
	 * The discount curve used to calculate the annuity is calculated from the forward curve using classical
	 * single curve interpretations of forwards and a default period length. The may be a crude approximation.
	 *
	 * Note: This method will consider evaluationTime being 0, see {@link net.finmath.marketdata2.products.SwapAnnuity#getSwapAnnuity(double, Schedule, DiscountCurveInterface, AnalyticModel)}.
	 *
	 * @param schedule The schedule discretization, i.e., the period start and end dates. End dates are considered payment dates and start of the next period.
	 * @param forwardCurve The forward curve.
	 * @return The swap annuity.
	 */
	public static RandomVariable getSwapAnnuity(final Schedule schedule, final ForwardCurveInterface forwardCurve) {
		final DiscountCurveInterface discountCurve = new DiscountCurveFromForwardCurve(forwardCurve.getName());
		final double evaluationTime = 0.0;	// Consider only payment time > 0
		return getSwapAnnuity(evaluationTime, schedule, discountCurve, new AnalyticModelFromCurvesAndVols( new Curve[] {forwardCurve, discountCurve} ));
	}

	/**
	 * Function to calculate an (idealized) swap annuity for a given schedule and discount curve.
	 *
	 * Note that, the value returned is divided by the discount factor at evaluation.
	 * This matters, if the discount factor at evaluationTime is not equal to 1.0.
	 *
	 * @param evaluationTime The evaluation time as double. Cash flows prior and including this time are not considered.
	 * @param schedule The schedule discretization, i.e., the period start and end dates. End dates are considered payment dates and start of the next period.
	 * @param discountCurve The discount curve.
	 * @param model The model, needed only in case the discount curve evaluation depends on an additional curve.
	 * @return The swap annuity.
	 */
	public static RandomVariable getSwapAnnuity(final double evaluationTime, final Schedule schedule, final DiscountCurveInterface discountCurve, final AnalyticModel model) {
		RandomVariable value = new RandomVariableFromDoubleArray(0.0);
		for(int periodIndex=0; periodIndex<schedule.getNumberOfPeriods(); periodIndex++) {
			final double paymentDate		= schedule.getPayment(periodIndex);
			if(paymentDate <= evaluationTime) {
				continue;
			}

			final double periodLength		= schedule.getPeriodLength(periodIndex);
			final RandomVariable discountFactor	= discountCurve.getDiscountFactor(model, paymentDate);
			value = discountFactor.mult(periodLength).add(value);
		}
		return value.div(discountCurve.getDiscountFactor(model, evaluationTime));
	}

	@Override
	public String toString() {
		return "SwapAnnuity [schedule=" + schedule + ", discountCurveName="
				+ discountCurveName + "]";
	}
}
