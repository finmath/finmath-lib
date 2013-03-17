/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 15.02.2004
 */
package net.finmath.montecarlo.interestrate.products;

import net.finmath.exception.CalculationException;
import net.finmath.functions.AnalyticFormulas;
import net.finmath.marketdata.model.curves.ForwardCurveInterface;
import net.finmath.marketdata.products.Swap;
import net.finmath.marketdata.products.SwapAnnuity;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.ImmutableRandomVariableInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * Implements the pricing of a swaption under a AbstractLIBORMarketModel
 * 
 * @author Christian Fries
 * @version 1.1
 */
public class Swaption extends AbstractLIBORMonteCarloProduct {
	private double     exerciseDate;	// Exercise date
	private double[]   fixingDates;		// Vector of fixing dates (must be sorted)
	private double[]   paymentDates;	// Vector of payment dates (same length as fixing dates)
	private double[]   periodLengths;	// Vector of payment dates (same length as fixing dates)
	private double[]   swaprates;		// Vector of strikes
		
    /**
     * @param exerciseDate
     * @param fixingDates
     * @param paymentDates
     * @param periodLengths
     * @param swaprates
     */
    public Swaption(double exerciseDate, double[] fixingDates, double[] paymentDates, double[] periodLengths, double[] swaprates) {
        super();
        this.exerciseDate = exerciseDate;
        this.fixingDates = fixingDates;
        this.paymentDates = paymentDates;
        this.periodLengths = periodLengths;
        this.swaprates = swaprates;
    }

	/**
	 * @param fixingDates Vector of fixing dates
	 * @param paymentDates Vector of payment dates (must have same length as fixing dates)
	 * @param swaprates Vector of strikes (must have same length as fixing dates)
	 */
	public Swaption(
			double exerciseDate,
			double[] fixingDates,
			double[] paymentDates,
			double[] swaprates) {
		super();
		this.exerciseDate = exerciseDate;
		this.fixingDates = fixingDates;
		this.paymentDates = paymentDates;
        this.periodLengths = null;
		this.swaprates = swaprates;
	}

	/**
	 * Creates a swaption using a TimeDiscretization
	 * 
	 * @param exerciseDate Exercise date.
	 * @param swapTenor Vector of period start and end dates.
	 * @param swaprate Strike
	 */
	public Swaption(
			double				exerciseDate,
			TimeDiscretizationInterface	swapTenor,
			double				swaprate) {
		super();
		this.exerciseDate = exerciseDate;

		this.fixingDates	= new double[swapTenor.getNumberOfTimeSteps()];
		this.paymentDates	= new double[swapTenor.getNumberOfTimeSteps()];
		for(int periodIndex=0; periodIndex<fixingDates.length; periodIndex++) {
			fixingDates[periodIndex] = swapTenor.getTime(periodIndex);
			paymentDates[periodIndex] = swapTenor.getTime(periodIndex+1);
		}

		this.periodLengths = null;

        this.swaprates = new double[swapTenor.getNumberOfTimeSteps()];
        java.util.Arrays.fill(swaprates, swaprate);
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
		/*
		 * Calculate value of the swap at exercise date on each path (beware of perfect forsight - all rates are simulationTime=exerciseDate)
		 */
        RandomVariableInterface valueOfSwapAtExerciseDate	= new RandomVariable(fixingDates[fixingDates.length-1],0.0);
		
        // Calculate the value of the swap by working backward through all periods
		for(int period=fixingDates.length-1; period>=0; period--)
		{
			double fixingDate	= fixingDates[period];
			double paymentDate	= paymentDates[period];
			double swaprate		= swaprates[period];
			
			double periodLength	= periodLengths != null ? periodLengths[period] : paymentDate - fixingDate;
			
			// Get random variables - note that this is the rate at simulation time = exerciseDate
			RandomVariableInterface libor	= model.getLIBOR(exerciseDate, fixingDate, paymentDate);
			
            // Add payment received at end of period
			RandomVariableInterface payoff = libor.getMutableCopy().sub(swaprate).mult(periodLength);
			valueOfSwapAtExerciseDate.add(payoff);

			// Discount back to beginning of period
			valueOfSwapAtExerciseDate.discount(libor, paymentDate - fixingDate);
		}
        
        // If the exercise date is not the first periods start date, then discount back to the exercise date (calculate the forward starting swap)
		if(fixingDates[0] != exerciseDate) {
			ImmutableRandomVariableInterface libor	= model.getLIBOR(exerciseDate, exerciseDate, fixingDates[0]);			
			double periodLength	= fixingDates[0] - exerciseDate;

			// Discount back to beginning of period
			valueOfSwapAtExerciseDate.discount(libor, periodLength);
		}
		
		/*
		 * Calculate swaption value
		 */
		RandomVariableInterface values = valueOfSwapAtExerciseDate.floor(0.0);
        
		ImmutableRandomVariableInterface	numeraire				= model.getNumeraire(exerciseDate);
		ImmutableRandomVariableInterface	monteCarloProbabilities	= model.getMonteCarloWeights(model.getTimeIndex(exerciseDate));
		values.div(numeraire).mult(monteCarloProbabilities);

		ImmutableRandomVariableInterface	numeraireAtZero					= model.getNumeraire(evaluationTime);
		ImmutableRandomVariableInterface	monteCarloProbabilitiesAtZero	= model.getMonteCarloWeights(evaluationTime);
		values.mult(numeraireAtZero).div(monteCarloProbabilitiesAtZero);

		return values;
	}
    
    /**
     * This method returns the value of the product using a Black-Scholes model for the swap rate
     * The model is determined by a discount factor curve and a swap rate volatility.
     * 
     * @param forwardCurve The forward curve on which to value the swap.
     * @param swaprateVolatility The Black volatility.
     * @return Value of this product
     */
    public double getValue(ForwardCurveInterface forwardCurve, double swaprateVolatility) {
    	double swaprate = swaprates[0];
    	for(int periodIndex = 0; periodIndex<swaprates.length; periodIndex++) if(swaprates[periodIndex] != swaprate) throw new RuntimeException("Uneven swaprates not allows for analytical pricing.");
    	
    	double[] swapTenor = new double[fixingDates.length+1];
    	for(int periodIndex = 0; periodIndex<fixingDates.length; periodIndex++) swapTenor[periodIndex] = fixingDates[periodIndex];
    	swapTenor[swapTenor.length-1] = paymentDates[paymentDates.length-1];

    	double forwardSwapRate = Swap.getForwardSwapRate(new TimeDiscretization(swapTenor), new TimeDiscretization(swapTenor), forwardCurve);
    	double swapAnnuity = SwapAnnuity.getSwapAnnuity(new TimeDiscretization(swapTenor), forwardCurve);
    	
    	return AnalyticFormulas.blackModelSwaptionValue(forwardSwapRate, swaprateVolatility, exerciseDate, swaprate, swapAnnuity);
    }
}
