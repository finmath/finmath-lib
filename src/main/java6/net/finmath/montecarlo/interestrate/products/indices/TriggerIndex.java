/*
 * Created on 22.11.2009
 */
package net.finmath.montecarlo.interestrate.products.indices;

import java.util.Set;

import net.finmath.exception.CalculationException;
import net.finmath.math.stochastic.RandomVariableInterface;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.products.components.AbstractProductComponent;

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

	private AbstractProductComponent trigger;
	private AbstractProductComponent indexIfTriggerIsPositive;
	private AbstractProductComponent indexIfTriggerIsNegative;


	/**
	 * Creates the function trigger(t) &ge; 0.0 ? indexIfTriggerIsPositive(t) : indexIfTriggerIsNegative(t)
	 * 
	 * @param trigger An index whose value determines which of the following indices is taken.
	 * @param indexIfTriggerIsPositive An index representing the result if trigger is non-negative (&ge; 0).
	 * @param indexIfTriggerIsNegative An index representing the result if trigger is negative (&lt; 0)
	 */
	public TriggerIndex(AbstractProductComponent trigger, AbstractProductComponent indexIfTriggerIsPositive, AbstractProductComponent indexIfTriggerIsNegative) {
		super();
		this.trigger = trigger;
		this.indexIfTriggerIsPositive = indexIfTriggerIsPositive;
		this.indexIfTriggerIsNegative = indexIfTriggerIsNegative;
	}

	@Override
	public RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {
		RandomVariableInterface valueTrigger				= trigger.getValue(evaluationTime, model);
		RandomVariableInterface valueIfTriggerNonNegative	= indexIfTriggerIsPositive.getValue(evaluationTime, model);
		RandomVariableInterface valueIfTriggerIsNegative	= indexIfTriggerIsNegative.getValue(evaluationTime, model);
		return valueTrigger.barrier(valueTrigger, valueIfTriggerNonNegative, valueIfTriggerIsNegative);
	}

	@Override
	public Set<String> queryUnderlyings() {
		Set<String> underlyingNames			= trigger.queryUnderlyings();
		Set<String>	underlyingNamesPositive	= indexIfTriggerIsPositive.queryUnderlyings();
		if(underlyingNamesPositive != null) {
			if(underlyingNames != null)	underlyingNames.addAll(underlyingNamesPositive);
			else						underlyingNames = underlyingNamesPositive;
		}
		Set<String>	underlyingNamesNegative	= indexIfTriggerIsNegative.queryUnderlyings();
		if(underlyingNamesNegative != null) {
			if(underlyingNames != null)	underlyingNames.addAll(underlyingNamesNegative);
			else						underlyingNames = underlyingNamesNegative;
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
