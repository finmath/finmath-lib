/*
 * Created on 22.11.2009
 */
package net.finmath.montecarlo.interestrate.products.indices;

import java.util.Set;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.products.components.AbstractProductComponent;
import net.finmath.stochastic.RandomVariable;

/**
 * A trigger index.
 *
 * Provides the function trigger(t) &ge; 0.0 ? indexIfTriggerIsPositive(t) : indexIfTriggerIsNegative(t).
 *
 * @author Christian Fries
 * @version 1.1
 */
public class TriggerIndex extends AbstractIndex {

	private static final long serialVersionUID = 2329303879116802448L;

	private final AbstractProductComponent trigger;
	private final AbstractProductComponent indexIfTriggerIsPositive;
	private final AbstractProductComponent indexIfTriggerIsNegative;


	/**
	 * Creates the function trigger(t) &ge; 0.0 ? indexIfTriggerIsPositive(t) : indexIfTriggerIsNegative(t)
	 *
	 * @param trigger An index whose value determines which of the following indices is taken.
	 * @param indexIfTriggerIsPositive An index representing the result if trigger is non-negative (&ge; 0).
	 * @param indexIfTriggerIsNegative An index representing the result if trigger is negative (&lt; 0)
	 */
	public TriggerIndex(final AbstractProductComponent trigger, final AbstractProductComponent indexIfTriggerIsPositive, final AbstractProductComponent indexIfTriggerIsNegative) {
		super();
		this.trigger = trigger;
		this.indexIfTriggerIsPositive = indexIfTriggerIsPositive;
		this.indexIfTriggerIsNegative = indexIfTriggerIsNegative;
	}

	@Override
	public RandomVariable getValue(final double evaluationTime, final TermStructureMonteCarloSimulationModel model) throws CalculationException {
		final RandomVariable valueTrigger				= trigger.getValue(evaluationTime, model);
		final RandomVariable valueIfTriggerNonNegative	= indexIfTriggerIsPositive.getValue(evaluationTime, model);
		final RandomVariable valueIfTriggerIsNegative	= indexIfTriggerIsNegative.getValue(evaluationTime, model);
		return valueTrigger.choose(valueIfTriggerNonNegative, valueIfTriggerIsNegative);
	}

	@Override
	public Set<String> queryUnderlyings() {
		Set<String> underlyingNames			= trigger.queryUnderlyings();
		final Set<String>	underlyingNamesPositive	= indexIfTriggerIsPositive.queryUnderlyings();
		if(underlyingNamesPositive != null) {
			if(underlyingNames != null) {
				underlyingNames.addAll(underlyingNamesPositive);
			} else {
				underlyingNames = underlyingNamesPositive;
			}
		}
		final Set<String>	underlyingNamesNegative	= indexIfTriggerIsNegative.queryUnderlyings();
		if(underlyingNamesNegative != null) {
			if(underlyingNames != null) {
				underlyingNames.addAll(underlyingNamesNegative);
			} else {
				underlyingNames = underlyingNamesNegative;
			}
		}
		return underlyingNames;
	}

	@Override
	public String toString() {
		return "TriggerIndex [trigger=" + trigger
				+ ", indexIfTriggerIsPositive=" + indexIfTriggerIsPositive
				+ ", indexIfTriggerIsNegative=" + indexIfTriggerIsNegative
				+ ", toString()=" + super.toString() + "]";
	}
}
