/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2004
 */
package net.finmath.montecarlo.interestrate.products;

import net.finmath.exception.CalculationException;
import net.finmath.math.functions.AnalyticFormulas;
import net.finmath.math.stochastic.RandomVariableInterface;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;

/**
 * Implements the pricing of a Caplet using a given <code>AbstractLIBORMarketModel</code>.
 * 
 * @author Christian Fries
 * @version 1.0
 */
public class Caplet extends AbstractLIBORMonteCarloProduct {

	public enum ValueUnit {
		VALUE,
		/*
		 * @deprecated Use INTEGRATEDLOGNORMALVARIANCE
		 */
		INTEGRATEDVARIANCE,
		/*
		 * @deprecated Use LOGNORMALVOLATILITY
		 */
		VOLATILITY,
		INTEGRATEDLOGNORMALVARIANCE,
		LOGNORMALVOLATILITY,
		INTEGRATEDNORMALVARIANCE,
		NORMALVOLATILITY
	}

	private final double	maturity;
	private final double	periodLength;
	private final double	strike;
	private final double	daycountFraction;
	private final boolean	isFloorlet;
	private final ValueUnit					valueUnit;

	/**
	 * Create a caplet or a floorlet.
	 * 
	 * A caplet pays \( max(L-K,0) * daycountFraction \) at maturity+periodLength
	 * where L is fixed at maturity.
	 * 
	 * A floorlet pays \( -min(L-K,0) * daycountFraction \) at maturity+periodLength
	 * where L is fixed at maturity.
	 * 
	 * @param maturity The fixing date given as double. The payment is at the period end.
	 * @param periodLength The length of the forward rate period.
	 * @param strike The strike given as double.
	 * @param daycountFraction The daycount fraction used in the payout function.
	 * @param isFloorlet If true, this object will represent a floorlet, otherwise a caplet.
	 * @param valueUnit The unit of the value returned by the <code>getValue</code> method.
	 */
	public Caplet(double maturity, double periodLength, double strike, double daycountFraction, boolean isFloorlet, ValueUnit valueUnit) {
		super();
		this.maturity = maturity;
		this.periodLength = periodLength;
		this.strike = strike;
		this.daycountFraction = daycountFraction;
		this.isFloorlet = isFloorlet;
		this.valueUnit = valueUnit;
	}
	
	/**
	 * Create a caplet or a floorlet.
	 * 
	 * A caplet pays \( max(L-K,0) * daycountFraction \) at maturity+periodLength
	 * where L is fixed at maturity.
	 * 
	 * A floorlet pays \( -min(L-K,0) * daycountFraction \) at maturity+periodLength
	 * where L is fixed at maturity.
	 * 
	 * This simplified constructor uses daycountFraction = periodLength.
	 * 
	 * @param maturity The fixing date given as double. The payment is at the period end.
	 * @param periodLength The length of the forward rate period in ACT/365 convention.
	 * @param strike The strike given as double.
	 * @param isFloorlet If true, this object will represent a floorlet, otherwise a caplet.
	 */
	public Caplet(double maturity, double periodLength, double strike, boolean isFloorlet) {
		this(maturity, periodLength, strike, periodLength, isFloorlet, ValueUnit.VALUE);
	}

	/**
	 * Create a caplet.
	 * 
	 * A caplet pays \( max(L-K,0) * periodLength \) at maturity+periodLength
	 * where L is fixed at maturity.
	 * 
	 * @param maturity The fixing date given as double. The payment is at the period end.
	 * @param periodLength The length of the forward rate period.
	 * @param strike The strike given as double.
	 */
	public Caplet(double maturity, double periodLength, double strike) {
		this(maturity, periodLength, strike, false);
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
		// This is on the LIBOR discretization
		double	paymentDate	= maturity+periodLength;
				
		// Get random variables
		RandomVariableInterface	libor					= model.getLIBOR(maturity, maturity, maturity+periodLength);
		RandomVariableInterface	numeraire				= model.getNumeraire(paymentDate);
		RandomVariableInterface	monteCarloProbabilities	= model.getMonteCarloWeights(paymentDate);
	
		/*
		 * Calculate the payoff, which is
		 *    max(L-K,0) * periodLength         for caplet or
		 *   -min(L-K,0) * periodLength         for floorlet.
		 */
		RandomVariableInterface values = libor;		
		if(!isFloorlet)	values = values.sub(strike).floor(0.0).mult(daycountFraction);
		else			values = values.sub(strike).cap(0.0).mult(-1.0 * daycountFraction);

		values = values.div(numeraire).mult(monteCarloProbabilities);

		RandomVariableInterface	numeraireAtValuationTime				= model.getNumeraire(evaluationTime);		
		RandomVariableInterface	monteCarloProbabilitiesAtValuationTime	= model.getMonteCarloWeights(evaluationTime);		
		values = values.mult(numeraireAtValuationTime).div(monteCarloProbabilitiesAtValuationTime);

		if(valueUnit == ValueUnit.VALUE) {
			return values;
		}
		else if(valueUnit == ValueUnit.LOGNORMALVOLATILITY || valueUnit == ValueUnit.VOLATILITY) {
			/*
			 * This calculation makes sense only if the value is an unconditional one.
			 */
			double forward = libor.div(numeraire).mult(monteCarloProbabilities).mult(numeraireAtValuationTime).div(monteCarloProbabilitiesAtValuationTime).getAverage();
			double optionMaturity = maturity-evaluationTime;
			double optionStrike = strike;
			double payoffUnit = daycountFraction;
			return model.getRandomVariableForConstant(AnalyticFormulas.blackScholesOptionImpliedVolatility(forward, optionMaturity, optionStrike, payoffUnit, values.getAverage()));
		}
		else if(valueUnit == ValueUnit.NORMALVOLATILITY) {
			/*
			 * This calculation makes sense only if the value is an unconditional one.
			 */
			double forward = libor.div(numeraire).mult(monteCarloProbabilities).mult(numeraireAtValuationTime).div(monteCarloProbabilitiesAtValuationTime).getAverage();
			double optionMaturity = maturity-evaluationTime;
			double optionStrike = strike;
			double payoffUnit = daycountFraction;
			return model.getRandomVariableForConstant(AnalyticFormulas.bachelierOptionImpliedVolatility(forward, optionMaturity, optionStrike, payoffUnit, values.getAverage()));
		}
		else {
			throw new IllegalArgumentException("Value unit " + valueUnit + " unsupported.");
		}
	}
}
