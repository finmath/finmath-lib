/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 08.11.2014
 */

package net.finmath.montecarlo.interestrate.products.components;

import java.util.Set;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.products.indices.AbstractIndex;
import net.finmath.montecarlo.interestrate.products.indices.AnalyticModelIndex;
import net.finmath.stochastic.RandomVariable;

/**
 * Implementation of a general accrual account.
 *
 * This class is an improved version of the {@link net.finmath.montecarlo.interestrate.products.MoneyMarketAccount} except that it
 * allow the use of a general index for accrual and also supports past fixings.
 *
 * @author Christian Fries
 *
 * @version 1.0
 */
public class AccrualAccount extends AbstractProductComponent {

	private static final long serialVersionUID = 188297603697240319L;

	private AnalyticModelIndex	pastFixings		= null;
	private final AbstractIndex		accrualIndex;
	private final double				accrualPeriod;

	/**
	 * Create an accrual account.
	 *
	 * @param currency The currency of this account.
	 * @param pastFixings An analytic model index which is used for past fixings, i.e., all calls to getValue with evaluationTime &lt; 0 are delegated to this index.
	 * @param accrualIndex The accrual index.
	 * @param accrualPeriod The accrual period.
	 */
	public AccrualAccount(final String currency, final AnalyticModelIndex pastFixings, final AbstractIndex accrualIndex, final double accrualPeriod) {
		super(currency);
		this.pastFixings	= pastFixings;
		this.accrualIndex	= accrualIndex;
		this.accrualPeriod	= accrualPeriod;
	}

	@Override
	public Set<String> queryUnderlyings() {
		return accrualIndex.queryUnderlyings();
	}

	@Override
	public RandomVariable getValue(final double evaluationTime, final TermStructureMonteCarloSimulationModel model) throws CalculationException {
		if(evaluationTime <= 0) {
			return pastFixings.getValue(evaluationTime, model);
		}

		RandomVariable value = pastFixings.getValue(0.0, model);

		// Loop over accrual periods
		for(double time=0.0; time<evaluationTime; time += accrualPeriod) {
			// Get the forward fixed at the beginning of the period
			final RandomVariable	forwardRate				= accrualIndex.getValue(time, model);
			final double					currentAccrualPeriod	= Math.min(accrualPeriod , evaluationTime-time);

			// Accrue the value using the current forward rate
			value = value.accrue(forwardRate, currentAccrualPeriod);
		}

		return value;
	}
}
