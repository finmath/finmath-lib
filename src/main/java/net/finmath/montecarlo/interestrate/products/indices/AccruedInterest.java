/*
 * Created on 06.12.2009
 *
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 */
package net.finmath.montecarlo.interestrate.products.indices;

import java.util.Calendar;
import java.util.Set;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.daycount.DayCountConventionInterface;

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
 */
public class AccruedInterest extends AbstractIndex {

	private static final long serialVersionUID = 147619920344514766L;

	private final Calendar	referenceDate;

	private final Calendar	periodStartDate;
	private final Calendar	periodEndDate;

	private final AbstractIndex					index;
	private final Double						indexFixingTime;
	private final DayCountConventionInterface	daycountConvention;
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
	 * @param daycountConvention The daycount convention.
	 * @param isNegativeAccruedInterest If true, the class represents the coupon payment minus the accrued interest, i.e., \( I(t_{0}) \cdot \frac{\max(\text{dcf}(T_{start},t),0)}{\text{dcf}(T_{start},T_{end})} \).
	 */
	public AccruedInterest(String name,
			String currency,
			Calendar referenceDate, Calendar periodStartDate,
			Calendar periodEndDate, AbstractIndex index, Double indexFixingTime,
			DayCountConventionInterface daycountConvention, boolean isNegativeAccruedInterest) {
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
	public RandomVariableInterface getValue(double fixingTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {
		double daycountFraction	= daycountConvention.getDaycountFraction(periodStartDate, getModelDate(fixingTime));
		double daycountPeriod	= daycountConvention.getDaycountFraction(periodStartDate, periodEndDate);
		daycountFraction = Math.min(Math.max(daycountFraction, 0.0), daycountPeriod);
		if(isNegativeAccruedInterest) daycountFraction = daycountPeriod - daycountFraction;

		return index.getValue(indexFixingTime, model).mult(daycountFraction);
	}

	@Override
	public Set<String> queryUnderlyings() {
		return index.queryUnderlyings();
	}

	private Calendar getModelDate(double fixingTime) {
		Calendar modelDate = (Calendar)referenceDate.clone();
		modelDate.add(Calendar.DAY_OF_YEAR, (int)(fixingTime*365.0));
		return modelDate;
	}
}
