/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 28.02.2015
 */

package net.finmath.montecarlo.interestrate.products;

import java.time.LocalDateTime;
import java.time.LocalTime;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.models.FundingCapacity;
import net.finmath.montecarlo.interestrate.products.indices.AbstractIndex;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
import net.finmath.time.FloatingpointDate;
import net.finmath.time.Schedule;

/**
 * @author Christian Fries
 *
 * @version 1.0
 */
public class SwapLegWithFundingProvider extends AbstractTermStructureMonteCarloProduct {

	private final Schedule legSchedule;
	private final double[] notionals;
	private final AbstractIndex index;
	private final double[] spreads;

	private final FundingCapacity fundingCapacity;

	/**
	 * Creates a swap leg. The swap leg is build from elementary components.
	 *
	 * @param legSchedule ScheduleFromPeriods of the leg.
	 * @param notionals An array of notionals for each period in the schedule.
	 * @param index The index.
	 * @param spreads Fixed spreads on the forward or fix rate.
	 * @param fundingCapacity A funding capacity monitor.
	 */
	public SwapLegWithFundingProvider(final Schedule legSchedule, final double[] notionals, final AbstractIndex index, final double[] spreads, FundingCapacity fundingCapacity) {
		super();
		if(legSchedule.getNumberOfPeriods() != notionals.length) {
			throw new IllegalArgumentException("Number of notionals ("+notionals.length+") must match number of periods ("+legSchedule.getNumberOfPeriods()+").");
		}

		this.legSchedule = legSchedule;
		this.notionals = notionals;
		this.index =  index;
		this.spreads = spreads;

		this.fundingCapacity = fundingCapacity;
	}

	@Override
	public RandomVariable getValue(final double evaluationTime, final TermStructureMonteCarloSimulationModel model) throws CalculationException {

		final LocalDateTime referenceDate = LocalDateTime.of(legSchedule.getReferenceDate(), LocalTime.of(0, 0));

		double productToModelTimeOffset = 0;
		try {
			if(referenceDate != null && model.getReferenceDate() != null) {
				productToModelTimeOffset = FloatingpointDate.getFloatingPointDateFromDate(model.getReferenceDate(), referenceDate);
			}
		}
		catch(final UnsupportedOperationException e) {
			// @TODO Models that do not provide a reference date will become disfunctional in future releases.
		}

		RandomVariable values = new Scalar(0.0);

		for(int periodIndex=0; periodIndex<legSchedule.getNumberOfPeriods(); periodIndex++) {

			final net.finmath.time.Period period = legSchedule.getPeriod(periodIndex);

			final double periodStart	= legSchedule.getPeriodStart(periodIndex);
			final double periodEnd	= legSchedule.getPeriodEnd(periodIndex);
			final double fixingDate	= legSchedule.getFixing(periodIndex);
			final double paymentDate	= legSchedule.getPayment(periodIndex);
			final double periodLength	= legSchedule.getPeriodLength(periodIndex);

			/*
			 * We do not count empty periods.
			 * Since empty periods are an indication for a ill-specified
			 * product, it might be reasonable to throw an illegal argument exception instead.
			 */
			if(periodLength == 0) {
				continue;
			}

			final double			notionalAtPeriodStart	= notionals[periodIndex];
			final RandomVariable	numeraire				= model.getNumeraire(productToModelTimeOffset + paymentDate);

			RandomVariable payoff = index.getValue(productToModelTimeOffset + fixingDate, model).add(spreads[periodIndex]).mult(periodLength);
			payoff = payoff.mult(notionalAtPeriodStart);

			final RandomVariable survivalProbility = fundingCapacity.getDefaultFactors(productToModelTimeOffset + periodEnd, payoff).getSurvivalProbability();
			payoff = payoff.mult(survivalProbility);

			payoff = payoff.div(numeraire);
			values = values.add(payoff);
		}

		final RandomVariable	numeraireAtEval			= model.getNumeraire(evaluationTime);
		values = values.mult(numeraireAtEval);

		return values;
	}
}
