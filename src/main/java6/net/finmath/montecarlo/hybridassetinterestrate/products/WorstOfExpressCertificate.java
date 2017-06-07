/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 04.04.2015
 */

package net.finmath.montecarlo.hybridassetinterestrate.products;

import net.finmath.exception.CalculationException;
import net.finmath.math.stochastic.RandomVariableInterface;
import net.finmath.modelling.ModelInterface;
import net.finmath.modelling.ProductInterface;
import net.finmath.montecarlo.hybridassetinterestrate.HybridAssetLIBORModelMonteCarloSimulationInterface;

/**
 * @author Christian Fries
 */
public class WorstOfExpressCertificate implements ProductInterface {

	final double maturity;	
	final double[] strikeLevels;
	final double[] exerciseDates;
	final double[] triggerPerformanceLevel;
	final double[] redemption;
	final double redemptionFinal;

	public WorstOfExpressCertificate(double maturity, double[] baseLevels,
			double[] exerciseDates, double[] triggerLevels,
			double[] redemption, double redemptionFinal) {
		super();
		this.maturity = maturity;
		this.strikeLevels = baseLevels;
		this.exerciseDates = exerciseDates;
		this.triggerPerformanceLevel = triggerLevels;
		this.redemption = redemption;
		this.redemptionFinal = redemptionFinal;
	}

	/* (non-Javadoc)
	 * @see net.finmath.modelling.ProductInterface#getValue(double, net.finmath.modelling.ModelInterface)
	 */
	@Override
	public Object getValue(double evaluationTime, ModelInterface model) {
		return null;
	}
	
	public double getValue(double evaluationTime, HybridAssetLIBORModelMonteCarloSimulationInterface model) throws CalculationException {
		
		RandomVariableInterface zero				= model.getRandomVariableForConstant(0.0);
		RandomVariableInterface values				= model.getRandomVariableForConstant(0.0);
		RandomVariableInterface exerciseIndicator	= model.getRandomVariableForConstant(1.0);

		for(int triggerIndex=0; triggerIndex<exerciseDates.length; triggerIndex++) {

			// get worst performance
			RandomVariableInterface worstPerformance = getWorstPerformance(model, exerciseDates[triggerIndex], strikeLevels);
			
			// exercise if worstPerformance >= triggerPerformanceLevel[triggerIndex]
			RandomVariableInterface trigger = worstPerformance.sub(triggerPerformanceLevel[triggerIndex]);
			
			RandomVariableInterface payment = exerciseIndicator.mult(redemption[triggerIndex]);
			payment = payment.div(model.getNumeraire(exerciseDates[triggerIndex]));
			
			// if trigger >= 0 we have a payment and set the exerciseIndicator to 0.
			values = values.add(trigger.barrier(trigger, payment, 0.0));
			exerciseIndicator = exerciseIndicator.barrier(trigger, zero, exerciseIndicator);				
		}
		
		/*
		 * final redemption
		 */

		RandomVariableInterface worstPerformance = getWorstPerformance(model, maturity, strikeLevels);
		RandomVariableInterface payment = exerciseIndicator.mult(worstPerformance.mult(redemptionFinal));

		payment = payment.div(model.getNumeraire(maturity));
		values = values.add(payment);
		
		/*
		 * numeraire at evaluationTime
		 */
		values = values.mult(model.getNumeraire(evaluationTime));
		return values.getAverage();
	}

	/**
	 * @param model
	 * @param exerciseDate
	 * @param baseLevels
	 * @return
	 * @throws CalculationException
	 */
	private static RandomVariableInterface getWorstPerformance(HybridAssetLIBORModelMonteCarloSimulationInterface model, double exerciseDate, double[] baseLevels) throws CalculationException {
		RandomVariableInterface worstPerformance = null;
		for(int assetIndex=0; assetIndex<baseLevels.length; assetIndex++) {
			RandomVariableInterface underlying = model.getAssetValue(exerciseDate, assetIndex);
			RandomVariableInterface performance = underlying.div(baseLevels[assetIndex]);
			worstPerformance = worstPerformance != null ? worstPerformance.cap(performance) : performance;
		}
		
		return worstPerformance;
	}
}
