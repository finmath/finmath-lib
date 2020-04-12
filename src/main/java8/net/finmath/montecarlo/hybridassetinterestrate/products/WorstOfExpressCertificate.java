/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 04.04.2015
 */

package net.finmath.montecarlo.hybridassetinterestrate.products;

import net.finmath.exception.CalculationException;
import net.finmath.modelling.Model;
import net.finmath.modelling.Product;
import net.finmath.montecarlo.hybridassetinterestrate.HybridAssetLIBORModelMonteCarloSimulation;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;

/**
 * @author Christian Fries
 * @version 1.0
 */
public class WorstOfExpressCertificate implements Product {

	private final double maturity;
	private final double[] strikeLevels;
	private final double[] exerciseDates;
	private final double[] triggerPerformanceLevel;
	private final double[] redemption;
	private final double redemptionFinal;

	public WorstOfExpressCertificate(final double maturity, final double[] baseLevels,
			final double[] exerciseDates, final double[] triggerLevels,
			final double[] redemption, final double redemptionFinal) {
		super();
		this.maturity = maturity;
		strikeLevels = baseLevels;
		this.exerciseDates = exerciseDates;
		triggerPerformanceLevel = triggerLevels;
		this.redemption = redemption;
		this.redemptionFinal = redemptionFinal;
	}

	/* (non-Javadoc)
	 * @see net.finmath.modelling.Product#getValue(double, net.finmath.modelling.Model)
	 */
	@Override
	public Object getValue(final double evaluationTime, final Model model) {
		return null;
	}

	public double getValue(final double evaluationTime, final HybridAssetLIBORModelMonteCarloSimulation model) throws CalculationException {

		final RandomVariable zero				= model.getRandomVariableForConstant(0.0);
		RandomVariable values				= model.getRandomVariableForConstant(0.0);
		RandomVariable exerciseIndicator	= model.getRandomVariableForConstant(1.0);

		for(int triggerIndex=0; triggerIndex<exerciseDates.length; triggerIndex++) {

			// get worst performance
			final RandomVariable worstPerformance = getWorstPerformance(model, exerciseDates[triggerIndex], strikeLevels);

			// exercise if worstPerformance >= triggerPerformanceLevel[triggerIndex]
			final RandomVariable trigger = worstPerformance.sub(triggerPerformanceLevel[triggerIndex]);

			RandomVariable payment = exerciseIndicator.mult(redemption[triggerIndex]);
			payment = payment.div(model.getNumeraire(exerciseDates[triggerIndex]));

			// if trigger >= 0 we have a payment and set the exerciseIndicator to 0.
			values = values.add(trigger.choose(payment, new Scalar(0.0)));
			exerciseIndicator = trigger.choose(zero, exerciseIndicator);
		}

		/*
		 * final redemption
		 */

		final RandomVariable worstPerformance = getWorstPerformance(model, maturity, strikeLevels);
		RandomVariable payment = exerciseIndicator.mult(worstPerformance.mult(redemptionFinal));

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
	private static RandomVariable getWorstPerformance(final HybridAssetLIBORModelMonteCarloSimulation model, final double exerciseDate, final double[] baseLevels) throws CalculationException {
		RandomVariable worstPerformance = null;
		for(int assetIndex=0; assetIndex<baseLevels.length; assetIndex++) {
			final RandomVariable underlying = model.getAssetValue(exerciseDate, assetIndex);
			final RandomVariable performance = underlying.div(baseLevels[assetIndex]);
			worstPerformance = worstPerformance != null ? worstPerformance.cap(performance) : performance;
		}

		return worstPerformance;
	}
}
