/*
 * Created on 06.12.2009
 *
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 */
package net.finmath.montecarlo.interestrate.products.indices;

import java.time.LocalDate;
import java.util.Set;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.daycount.DayCountConvention;

/**
 * An accrued interest index.
 *
 * For a given index I this class's getValue function calculates the value
 * \( I(t_{0}) \cdot \frac{\max(\text{dcf}(t,T_{end}),0)}{\text{dcf}(T_{start},T_{end})} \) \text{,}
 * where \( \text{dcf} \) is the given day count convention and T_{start} and T_{end} are the
 * period start and period end date respectively and \( t \) is the fixing date.
 *
 * The fixingDate is provided as an argument to the getValue method in terms of a ACT/365 day count fraction from
 * the given reference date.
 *
 * Note that the value returned is not numeraire adjusted, i.e., not discounted.
 *
 * Note that the index is fixed in \( t \). For
 *
 * @author Christian Fries
 * @version 1.0
 */
public class AccruedInterest extends AbstractIndex {

	private static final long serialVersionUID = 147619920344514766L;

	private final LocalDate	referenceDate;

	private final LocalDate	periodStartDate;
	private final LocalDate	periodEndDate;

	private final AbstractIndex					index;
	private final Double						indexFixingTime;
	private final DayCountConvention	daycountConvention;
	private final boolean						isNegativeAccruedInterest;

	/**
	 * Create an accrued interest index.
	 *
	 * @param name The name of the index.
	 * @param currency The payment currency.
	 * @param referenceDate The model reference date (corresponding to t=0).
	 * @param periodStartDate The period start date.
	 * @param periodEndDate The period end date.
	 * @param index The index.
	 * @param indexFixingTime The fixing time \( t_{0} \) of the index.
	 * @param daycountConvention The day count convention.
	 * @param isNegativeAccruedInterest If true, the class represents the coupon payment minus the accrued interest, i.e., \( I(t_{0}) \cdot \frac{\max(\text{dcf}(T_{start},t),0)}{\text{dcf}(T_{start},T_{end})} \).
	 */
	public AccruedInterest(final String name,
			final String currency,
			final LocalDate referenceDate, final LocalDate periodStartDate,
			final LocalDate periodEndDate, final AbstractIndex index, final Double indexFixingTime,
			final DayCountConvention daycountConvention, final boolean isNegativeAccruedInterest) {
		super(name, currency);
		this.referenceDate = referenceDate;
		this.periodStartDate = periodStartDate;
		this.periodEndDate = periodEndDate;
		this.index = index;
		this.indexFixingTime = indexFixingTime;
		this.daycountConvention = daycountConvention;
		this.isNegativeAccruedInterest = isNegativeAccruedInterest;
	}

	@Override
	public RandomVariable getValue(final double fixingTime, final TermStructureMonteCarloSimulationModel model) throws CalculationException {
		double daycountFraction	= daycountConvention.getDaycountFraction(periodStartDate, getModelDate(fixingTime));
		final double daycountPeriod	= daycountConvention.getDaycountFraction(periodStartDate, periodEndDate);
		daycountFraction = Math.min(Math.max(daycountFraction, 0.0), daycountPeriod);
		if(isNegativeAccruedInterest) {
			daycountFraction = daycountPeriod - daycountFraction;
		}

		return index.getValue(indexFixingTime, model).mult(daycountFraction);
	}

	@Override
	public Set<String> queryUnderlyings() {
		return index.queryUnderlyings();
	}

	private LocalDate getModelDate(final double fixingTime) {
		return referenceDate.plusDays(Math.round((float)(fixingTime*365.0)));
	}
}
