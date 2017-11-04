/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 19.02.2004
 */
package net.finmath.montecarlo.interestrate.products;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * This class implements the valuation of a Flexi Cap (aka Auto Cap).
 * If <code>maximumNumberOfExercises = fixingDates.length</code> then this is a Cap.
 * The payoff of the product is <i>L(T<sub>i</sub>) - K<sub>i</sub></i> if <i>L(T<sub>i</sub>) - K<sub>i</sub> &gt; 0</i> and the number of
 * <i>j &lt; i</i> such that <i>L(T<sub>j</sub>) - K<sub>j</sub></i> if <i>L(T<sub>j</sub>) - K<sub>j</sub> &gt; 0</i>
 * is less than <code>maximumNumberOfExercises</code>, where <i>T<sub>i</sub></i> is an
 * element of <code>fixingDates</code>.
 * 
 * @author Christian Fries
 * @version 1.0
 */
public class FlexiCap extends AbstractLIBORMonteCarloProduct {
	
	private final double[]	fixingDates;					// Vector of fixing dates (must be sorted)
	private final double[]	paymentDates;					// Vector of payment dates (same length as fixing dates)
	private final double[]	strikes;						// Vector of strikes
	private final int		maximumNumberOfExercises;		// The maximum number of exercises
	
	/**
	 * Create a Flexi Cap (aka Auto Cap).
	 * If <code>maximumNumberOfExercises = fixingDates.length</code> then this is a Cap.
	 * 
	 * @param fixingDates Vector of fixing dates
	 * @param paymentDates Vector of payment dates (must have same length as fixing dates)
	 * @param strikes Vector of strikes (must have same length as fixing dates)
	 * @param maximumNumberOfExercises Maximum number of exercises.
	 */
	public FlexiCap(
			double[]	fixingDates,
			double[]	paymentDates,
			double[]	strikes,
			int			maximumNumberOfExercises) {
		super();
		this.fixingDates				= fixingDates;
		this.paymentDates				= paymentDates;
		this.strikes					= strikes;
		this.maximumNumberOfExercises	= maximumNumberOfExercises;
	}
	
    /**
     * This method returns the value random variable of the product within the specified model, evaluated at a given evalutationTime.
     * Note: For a lattice this is often the value conditional to evalutationTime, for a Monte-Carlo simulation this is the (sum of) value discounted to evaluation time.
     * Cashflows prior evaluationTime are not considered.
     * 
     * @param evaluationTime The time on which this products value should be observed.
     * @param model The model used to price the product.
     * @return The random variable representing the value of the product discounted to evaluation time
     * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
     */
    @Override
    public RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {

    	// Allocate accumulator for values
        RandomVariableInterface values = new RandomVariable(0.0);

        /*
		 * We go forward in time and on each path we remember the number of exercises so far.
		 */
        RandomVariableInterface numberOfExcercises = new RandomVariable(maximumNumberOfExercises-0.5);
		
		for(int period=0; period<fixingDates.length; period++)
		{
			double fixingDate	= fixingDates[period];
			double paymentDate	= paymentDates[period];
			
			// evaluationTime > fixingDate is allowed. Negative fixing date is allowed too (but likely not supported by the model)
			if(evaluationTime > paymentDate) continue;
			
			double strike	 	= strikes[period];
			double periodLength	= paymentDate - fixingDate;
			
			// Get random variables
			RandomVariableInterface	libor					= model.getLIBOR(fixingDate, fixingDate, paymentDate);
			RandomVariableInterface	numeraire				= model.getNumeraire(paymentDate);
			RandomVariableInterface	monteCarloProbabilities	= model.getMonteCarloWeights(model.getTimeIndex(paymentDate));

			// Calculate payout
			RandomVariableInterface payoff = libor.sub(strike).mult(periodLength);
			RandomVariableInterface indicator = new RandomVariable(1.0).barrier(payoff, new RandomVariable(1.0), new RandomVariable(0.0));
			indicator = indicator.barrier(numberOfExcercises, indicator, 0.0);
			
			payoff = payoff.div(numeraire).mult(monteCarloProbabilities);

			// Accumulate numeraire relative values
			values = values.addProduct(indicator, payoff);

			// Update exercise counter
            numberOfExcercises = numberOfExcercises.sub(indicator);
		}

		RandomVariableInterface	numeraireAtEvaluationTime				= model.getNumeraire(evaluationTime);
		RandomVariableInterface	monteCarloProbabilitiesAtEvaluationTime	= model.getMonteCarloWeights(evaluationTime);
		values = values.mult(numeraireAtEvaluationTime).div(monteCarloProbabilitiesAtEvaluationTime);
				
		return values;	
	}
	
	/**
	 * @return Returns the strikes.
	 */
	public double[] getStrikes() {
		return strikes;
	}
}
