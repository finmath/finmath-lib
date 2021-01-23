/*
 * Created on 15.09.2006
 *
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 */
package net.finmath.montecarlo.interestrate.products.indices;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.FloatingpointDate;
import net.finmath.time.businessdaycalendar.BusinessdayCalendar;
import net.finmath.time.businessdaycalendar.BusinessdayCalendar.DateRollConvention;

/**
 * A (floating) forward rate index for a given period start offset (offset from fixing) and period length.
 *
 * @author Christian Fries
 * @version 1.1
 */
public class LIBORIndex extends AbstractIndex {

	private static final long serialVersionUID = 1L;

	private final String paymentOffsetCode;
	private final BusinessdayCalendar paymentBusinessdayCalendar;
	private final BusinessdayCalendar.DateRollConvention paymentDateRollConvention;

	private final double periodStartOffset;
	private final double periodLength;



	public LIBORIndex(final String name, final String currency, final String paymentOffsetCode,
			final BusinessdayCalendar paymentBusinessdayCalendar, final DateRollConvention paymentDateRollConvention) {
		super(name, currency);
		this.paymentOffsetCode = paymentOffsetCode;
		this.paymentBusinessdayCalendar = paymentBusinessdayCalendar;
		this.paymentDateRollConvention = paymentDateRollConvention;
		periodStartOffset = 0.0;
		periodLength = Double.NaN;
	}

	/**
	 * Creates a forward rate index for a given period start offset (offset from fixing) and period length.
	 *
	 * @param name The name of an index. Used to map an index on a curve.
	 * @param periodStartOffset An offset added to the fixing to define the period start.
	 * @param periodLength The period length
	 */
	public LIBORIndex(final String name, final double periodStartOffset, final double periodLength) {
		super(name, null);
		paymentOffsetCode = null;
		paymentBusinessdayCalendar = null;
		paymentDateRollConvention = null;
		this.periodStartOffset = periodStartOffset;
		this.periodLength = periodLength;
	}

	/**
	 * Creates a forward rate index for a given period start offset (offset from fixing) and period length.
	 *
	 * @param periodStartOffset An offset added to the fixing to define the period start.
	 * @param periodLength The period length
	 */
	public LIBORIndex(final double periodStartOffset, final double periodLength) {
		this(null, periodStartOffset, periodLength);
	}

	@Override
	public RandomVariable getValue(final double evaluationTime, final TermStructureMonteCarloSimulationModel model) throws CalculationException {

		// Check if model provides this index
		if(getName() != null && model.getModel().getForwardRateCurve().getName() != null) {
			// Check if analytic adjustment would be possible
			if(!model.getModel().getForwardRateCurve().getName().equals(getName()) && (model.getModel().getAnalyticModel() != null && model.getModel().getAnalyticModel().getForwardCurve(getName()) == null)) {
				throw new IllegalArgumentException("No curve for index " + getName() + " found in model.");
			}
		}

		// If evaluationTime < 0 take fixing from curve (note: this is a fall-back, fixing should be provided by product, if possible).
		if(evaluationTime < 0) {
			return model.getRandomVariableForConstant(model.getModel().getForwardRateCurve().getForward(model.getModel().getAnalyticModel(), evaluationTime+periodStartOffset));
		}

		/*
		 * The periodLength may be a given float or (more exact) derived from the rolling convetions.
		 */
		final double periodLength = getPeriodLength(model, evaluationTime+periodStartOffset);

		/*
		 * Fetch forward rate from model
		 */
		RandomVariable forwardRate = model.getForwardRate(evaluationTime, evaluationTime+periodStartOffset, evaluationTime+periodStartOffset+periodLength);

		if(getName() != null && !model.getModel().getForwardRateCurve().getName().equals(getName())) {
			// Perform a multiplicative adjustment on the forward bonds
			final AnalyticModel analyticModel = model.getModel().getAnalyticModel();
			if(analyticModel == null) {
				throw new IllegalArgumentException("Index " + getName() + " does not aggree with model curve " + model.getModel().getForwardRateCurve().getName() + " and requires analytic model for adjustment. The analyticModel is null.");
			}
			final ForwardCurve indexForwardCurve = analyticModel.getForwardCurve(getName());
			final ForwardCurve modelForwardCurve = model.getModel().getForwardRateCurve();
			final double adjustment = (1.0 + indexForwardCurve.getForward(analyticModel, evaluationTime+periodStartOffset, periodLength) * periodLength) / (1.0 + modelForwardCurve.getForward(analyticModel, evaluationTime+periodStartOffset, periodLength) * periodLength);
			forwardRate = forwardRate.mult(periodLength).add(1.0).mult(adjustment).sub(1.0).div(periodLength);
		}

		return forwardRate;
	}

	/**
	 * Returns the periodStartOffset as an act/365 daycount.
	 *
	 * @return the periodStartOffset
	 */
	public double getPeriodStartOffset() {
		return periodStartOffset;
	}

	public double getPeriodLength(final TermStructureMonteCarloSimulationModel model, final double fixingTime) {
		if(paymentOffsetCode != null) {
			final LocalDateTime referenceDate = model.getReferenceDate();
			final LocalDateTime fixingDate = FloatingpointDate.getDateFromFloatingPointDate(referenceDate, fixingTime);
			final LocalDate paymentDate = paymentBusinessdayCalendar.getAdjustedDate(fixingDate.toLocalDate(), paymentOffsetCode, paymentDateRollConvention);
			final double paymentTime = FloatingpointDate.getFloatingPointDateFromDate(referenceDate, LocalDateTime.of(paymentDate, fixingDate.toLocalTime()));

			return paymentTime - fixingTime;
		}
		else {
			return periodLength;
		}
	}

	/**
	 * Returns the tenor encoded as an pseudo act/365 daycount fraction.
	 *
	 * @return the periodLength The tenor as an act/365 daycount fraction.
	 */
	public double getPeriodLength() {

		return periodLength;
	}

	@Override
	public Set<String> queryUnderlyings() {
		final Set<String> underlyingNames = new HashSet<>();
		underlyingNames.add(getName());
		return underlyingNames;
	}

	@Override
	public String toString() {
		return "LIBORIndex [periodStartOffset=" + periodStartOffset
				+ ", periodLength=" + periodLength + ", toString()="
				+ super.toString() + "]";
	}
}
