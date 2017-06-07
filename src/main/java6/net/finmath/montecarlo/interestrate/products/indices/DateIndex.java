/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 14.06.2015
 */

package net.finmath.montecarlo.interestrate.products.indices;

import java.util.Set;

import org.joda.time.LocalDate;

import net.finmath.exception.CalculationException;
import net.finmath.math.stochastic.RandomVariableInterface;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;

/**
 * An index whose value is a function of the fixing date, for example the DAY, MONTH or NUMBER_OF_DAYS_IN_MONTH.
 * This index is useful in building date based interpolation formulas using other indices.
 * 
 * @author Christian Fries
 */
public class DateIndex extends AbstractIndex {

	private static final long serialVersionUID = 7457336500162149869L;

	public enum DateIndexType {
		DAY,
		MONTH,
		YEAR,
		NUMBER_OF_DAYS_IN_MONTH
	}

	private DateIndexType dateIndexType;
	
	/**
	 * Construct a date index.
	 * 
	 * @param name Name of this index.
	 * @param currency Currency (if any - in natural situations this index is a scalar).
	 * @param dateIndexType The date index type.
	 */
	public DateIndex(String name, String currency, DateIndexType dateIndexType) {
		super(name, currency);
		this.dateIndexType = dateIndexType;
	}

	/**
	 * Construct a date index.
	 * 
	 * @param name Name of this index.
	 * @param dateIndexType The date index type.
	 */
	public DateIndex(String name, DateIndexType dateIndexType) {
		super(name);
		this.dateIndexType = dateIndexType;
	}

	@Override
	public RandomVariableInterface getValue(double fixingTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {
		LocalDate referenceDate = model.getModel().getForwardRateCurve().getReferenceDate()
				.plusDays((int)Math.round(fixingTime*365));
		
		double value = 0;
		switch(dateIndexType) {
		case DAY:
			value = referenceDate.getDayOfMonth();
			break;
		case MONTH:
			value = referenceDate.getMonthOfYear();
			break;
		case YEAR:
			value = referenceDate.getYear();
			break;
		case NUMBER_OF_DAYS_IN_MONTH:
			value = referenceDate.dayOfMonth().getMaximumValue();
			break;
		}

		return model.getRandomVariableForConstant(value);
	}

	@Override
	public Set<String> queryUnderlyings() {
		return null;
	}

}
