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
import net.finmath.montecarlo.interestrate.products.components.AbstractProductComponent;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.FloatingpointDate;
import net.finmath.time.businessdaycalendar.BusinessdayCalendar;

/**
 * A time-lagged index paying index(t+fixingOffset)
 *
 * @author Christian Fries
 * @version 1.0
 */
public class LaggedIndex extends AbstractIndex {

	private static final long serialVersionUID = 4899043672016395530L;

	private final AbstractProductComponent	index;

	private final String fixingOffsetCode;
	private final BusinessdayCalendar paymentBusinessdayCalendar;

	private final double					fixingOffset;

	public LaggedIndex(final AbstractProductComponent index, final String fixingOffsetCode,
			final BusinessdayCalendar paymentBusinessdayCalendar) {
		this.index = index;
		this.fixingOffsetCode = fixingOffsetCode;
		this.paymentBusinessdayCalendar = paymentBusinessdayCalendar;
		fixingOffset = 0;
	}

	/**
	 * Creates a time-lagged index paying index(t+fixingOffset).
	 *
	 * @param index An index.
	 * @param fixingOffset Offset added to the fixing (evaluation time) of this index to fix the underlying index.
	 */
	public LaggedIndex(final AbstractProductComponent index, final double fixingOffset) {
		super();
		this.index			= index;
		fixingOffsetCode = null;
		paymentBusinessdayCalendar = null;
		this.fixingOffset	= fixingOffset;
	}

	@Override
	public Set<String> queryUnderlyings() {
		return index.queryUnderlyings();
	}

	@Override
	public RandomVariable getValue(final double evaluationTime, final TermStructureMonteCarloSimulationModel model) throws CalculationException {
		if(fixingOffsetCode != null) {
			final LocalDate startDate = FloatingpointDate.getDateFromFloatingPointDate(model.getReferenceDate().toLocalDate(), evaluationTime);
			final LocalDate endDate =  paymentBusinessdayCalendar.getDateFromDateAndOffsetCode(startDate, fixingOffsetCode);
			final double fixingOffset = FloatingpointDate.getFloatingPointDateFromDate(model.getReferenceDate().toLocalDate(), endDate);

			return index.getValue(evaluationTime + fixingOffset, model);
		}
		else {
			return index.getValue(evaluationTime + fixingOffset, model);
		}
	}
}
