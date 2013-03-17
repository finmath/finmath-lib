/*
 * Created on 09.02.2004
 */
package net.finmath.montecarlo.interestrate.products;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.ImmutableRandomVariableInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * Implements the pricing of a Caplet using a given <code>AbstractLIBORMarketModel</code>.
 * 
 * @author Christian Fries
 * @version 1.0
 */
public class Caplet extends AbstractLIBORMonteCarloProduct {
	private double	maturity;
	private double	periodLength;
	private double	strike;
	private boolean	isFloorlet = false;

	/**
	 * @param maturity The fixing date given as double. The payment is at the period end.
     * @param periodLength The length of the libor period.
	 * @param strike The strike given as double.
	 */
	public Caplet(double maturity, double periodLength, double strike) {
		super();
        this.maturity = maturity;
        this.periodLength = periodLength;
		this.strike = strike;
	}

	/**
	 * Makes the caplet a floorlet.
	 * 
	 * @param isFloorlet
	 */
	public Caplet setFloorlet(boolean isFloorlet) {
		this.isFloorlet = isFloorlet;

		return this;
	}
	
    /**
     * This method returns the value random variable of the product within the specified model, evaluated at a given evalutationTime.
     * Note: For a lattice this is often the value conditional to evalutationTime, for a Monte-Carlo simulation this is the (sum of) value discounted to evaluation time.
     * Cashflows prior evaluationTime are not considered.
     * 
     * @param evaluationTime The time on which this products value should be observed.
     * @param model The model used to price the product.
     * @return The random variable representing the value of the product discounted to evaluation time
     * @throws CalculationException 
     */
    @Override
    public RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {        
		// This is on the LIBOR discretization
		double	paymentDate	= maturity+periodLength;
				
		// Get random variables
		RandomVariableInterface				libor					= model.getLIBOR(maturity, maturity, maturity+periodLength);
		ImmutableRandomVariableInterface	numeraire				= model.getNumeraire(paymentDate);
		ImmutableRandomVariableInterface	monteCarloProbabilities	= model.getMonteCarloWeights(model.getTimeIndex(paymentDate));
	
		RandomVariableInterface values = libor;		
		if(!isFloorlet)	values.sub(strike).floor(0.0).mult(periodLength);
		else			values.sub(strike).cap(0.0).mult(-1.0 * periodLength);

		values.div(numeraire).mult(monteCarloProbabilities);

		ImmutableRandomVariableInterface	numeraireAtZero						= model.getNumeraire(evaluationTime);		
		ImmutableRandomVariableInterface	monteCarloProbabilitiesAtZero		= model.getMonteCarloWeights(evaluationTime);		
		values.mult(numeraireAtZero).div(monteCarloProbabilitiesAtZero);

		return values;
	}
}
