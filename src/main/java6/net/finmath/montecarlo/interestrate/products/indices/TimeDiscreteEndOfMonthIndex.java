/*
 * Created on 31.01.2015
 *
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 */
package net.finmath.montecarlo.interestrate.products.indices;

import java.util.Set;

import org.joda.time.LocalDate;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.daycount.DayCountConvention_ACT_365;

/**
 * An index which maps is evaluation point to a fixed discrete point, the end of the month,
 * then takes the value of a given base index at this point.
 * 
 * @author Christian Fries
 */
public class TimeDiscreteEndOfMonthIndex extends AbstractIndex {

	private static final long serialVersionUID = -490057583438933158L;
	private final AbstractIndex baseIndex;
	private final int fixingOffsetMonths;

	/**
	 * Creates a time discrete index.
	 * 
	 * @param name The name of this index. Used to map an index on a curve.
	 * @param baseIndex The base index (will be referenced at end of month points).
	 * @param fixingOffsetMonths A given fixing offset in month.
	 */
	public TimeDiscreteEndOfMonthIndex(String name, AbstractIndex baseIndex, int fixingOffsetMonths) {
		super(name);
		this.baseIndex = baseIndex;
		this.fixingOffsetMonths = fixingOffsetMonths;
	}

	@Override
	public RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {
		LocalDate referenceDate = model.getModel().getForwardRateCurve().getReferenceDate();
		LocalDate cal = referenceDate.plusDays((int)Math.round(evaluationTime * 365))
				.withDayOfMonth(1)
				.plusMonths(1);
		cal = cal.withDayOfMonth(cal.dayOfMonth().getMaximumValue());
		double time = (new DayCountConvention_ACT_365()).getDaycountFraction(referenceDate, cal);

		return baseIndex.getValue(time, model);
	}

	@Override
	public Set<String> queryUnderlyings() {
		return baseIndex.queryUnderlyings();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "TimeDiscreteEndOfMonthIndex [baseIndex=" + baseIndex + ", fixingOffsetMonths=" + fixingOffsetMonths + "]";
	}
}