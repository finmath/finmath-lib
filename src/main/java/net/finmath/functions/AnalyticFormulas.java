/*
 * Created on 23.01.2004.
 *
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 */
package net.finmath.functions;

import java.util.Calendar;

import org.apache.commons.math3.analysis.UnivariateFunction;

import net.finmath.rootfinder.NewtonsMethod;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * This class implements some functions as static class methods.
 * 
 * It provides functions like
 * <ul>
 * 	<li>the Black-Scholes formula,
 * 	<li>the inverse of the Back-Scholes formula with respect to (implied) volatility,
 * 	<li>the corresponding functions (versions) for caplets and swaptions,
 * 	<li>some convexity adjustments.
 * </ul>
 * 
 * @author Christian Fries
 * @version 1.9
 * @date 27.04.2012
 */
public class AnalyticFormulas {
	
	// Suppress default constructor for non-instantiability
	private AnalyticFormulas() {
		// This constructor will never be invoked
	}
	
	/**
	 * Calculates the Black-Scholes option value of a call, i.e., the payoff max(S(T)-K,0) P, where S follows a log-normal process with constant log-volatility.
	 * 
	 * The method also handles cases where the forward and/or option strike is negative
	 * and some limit cases where the forward and/or the option strike is zero.
	 * 
	 * @param forward The forward of the underlying.
	 * @param volatility The Black-Scholes volatility.
	 * @param optionMaturity The option maturity T.
	 * @param optionStrike The option strike. If the option strike is &le; 0.0 the method returns the value of the forward contract paying S(T)-K in T.
	 * @param payoffUnit The payoff unit (e.g., the discount factor)
	 * @return Returns the value of a European call option under the Black-Scholes model.
	 */
	public static double blackScholesGeneralizedOptionValue(
			double forward,
			double volatility,
			double optionMaturity,
			double optionStrike,
			double payoffUnit)
	{
		if(optionMaturity < 0) {
			return 0;
		}
		else if(forward < 0) {
			// We use max(X,0) = X + max(-X,0) 
			return (forward - optionStrike) * payoffUnit + blackScholesGeneralizedOptionValue(-forward, volatility, optionMaturity, -optionStrike, payoffUnit);
		}
		else if((forward == 0) || (optionStrike <= 0.0) || (volatility <= 0.0) || (optionMaturity <= 0.0))
		{	
			// Limit case (where dPlus = +/- infty)
			return Math.max(forward - optionStrike,0) * payoffUnit;
		}
		else
		{	
			// Calculate analytic value
			double dPlus = (Math.log(forward / optionStrike) + 0.5 * volatility * volatility * optionMaturity) / (volatility * Math.sqrt(optionMaturity));
			double dMinus = dPlus - volatility * Math.sqrt(optionMaturity);

			double valueAnalytic = (forward * NormalDistribution.cumulativeDistribution(dPlus) - optionStrike * NormalDistribution.cumulativeDistribution(dMinus)) * payoffUnit;
			
			return valueAnalytic;
		}
	}
	
	/**
	 * Calculates the Black-Scholes option value of a call, i.e., the payoff max(S(T)-K,0) P, where S follows a log-normal process with constant log-volatility.
	 * 
	 * The model specific quantities are considered to be random variable, i.e.,
	 * the function may calculate an per-path valuation in a single call.
	 * 
	 * @param forward The forward of the underlying.
	 * @param volatility The Black-Scholes volatility.
	 * @param optionMaturity The option maturity T.
	 * @param optionStrike The option strike. If the option strike is &le; 0.0 the method returns the value of the forward contract paying S(T)-K in T.
	 * @param payoffUnit The payoff unit (e.g., the discount factor)
	 * @return Returns the value of a European call option under the Black-Scholes model.
	 */
	public static RandomVariableInterface blackScholesGeneralizedOptionValue(
			RandomVariableInterface forward,
			RandomVariableInterface volatility,
			double optionMaturity,
			double optionStrike,
			RandomVariableInterface payoffUnit)
	{
		if(optionMaturity < 0) {
			return forward.mult(0.0);
		}
		else
		{	
			RandomVariableInterface dPlus	= forward.div(optionStrike).log().add(volatility.squared().mult(0.5 * optionMaturity)).div(volatility).div(Math.sqrt(optionMaturity));
			RandomVariableInterface dMinus	= dPlus.sub(volatility.mult(Math.sqrt(optionMaturity)));
			
			UnivariateFunction cumulativeNormal = new UnivariateFunction() {
				public double value(double x) { return NormalDistribution.cumulativeDistribution(x); }
			};
			
			RandomVariableInterface valueAnalytic = dPlus.apply(cumulativeNormal).mult(forward).sub(dMinus.apply(cumulativeNormal).mult(optionStrike)).mult(payoffUnit);
			
			return valueAnalytic;
		}
	}

	/**
	 * Calculates the Black-Scholes option value of a call, i.e., the payoff max(S(T)-K,0), where S follows a log-normal process with constant log-volatility.
	 * 
	 * @param initialStockValue The spot value of the underlying.
	 * @param riskFreeRate The risk free rate r (df = exp(-r T)).
	 * @param volatility The Black-Scholes volatility.
	 * @param optionMaturity The option maturity T.
	 * @param optionStrike The option strike. If the option strike is &le; 0.0 the method returns the value of the forward contract paying S(T)-K in T.
	 * @return Returns the value of a European call option under the Black-Scholes model.
	 */
	public static double blackScholesOptionValue(
			double initialStockValue,
			double riskFreeRate,
			double volatility,
			double optionMaturity,
			double optionStrike)
	{
		return blackScholesGeneralizedOptionValue(
				initialStockValue * Math.exp(riskFreeRate * optionMaturity),	// forward
				volatility,
				optionMaturity,
				optionStrike,
				Math.exp(-riskFreeRate * optionMaturity)						// payoff unit
				);
	}
	
	/**
     * Calculates the Black-Scholes option value of an atm call option.
     * 
     * @param volatility The Black-Scholes volatility.
     * @param optionMaturity The option maturity T.
	 * @param forward The forward, i.e., the expectation of the index under the measure associated with payoff unit.
	 * @param payoffUnit The payoff unit, i.e., the discount factor or the anuity associated with the payoff.
	 * @return Returns the value of a European at-the-money call option under the Black-Scholes model
	 */
	public static double blackScholesATMOptionValue(
			double volatility,
			double optionMaturity,
			double forward,
			double payoffUnit)
	{
		if(optionMaturity < 0) return 0.0;

		// Calculate analytic value
		double dPlus = 0.5 * volatility * Math.sqrt(optionMaturity);
		double dMinus = -dPlus;
		
		double valueAnalytic = (NormalDistribution.cumulativeDistribution(dPlus) - NormalDistribution.cumulativeDistribution(dMinus)) * forward * payoffUnit;
		
		return valueAnalytic;
	}

	/**
	 * Calculates the delta of a call option under a Black-Scholes model
	 * 
	 * The method also handles cases where the forward and/or option strike is negative
	 * and some limit cases where the forward or the option strike is zero.
	 * In the case forward = option strike = 0 the method returns 1.0.
	 * 
	 * @param initialStockValue The initial value of the underlying, i.e., the spot.
	 * @param riskFreeRate The risk free rate of the bank account numerarie.
     * @param volatility The Black-Scholes volatility.
     * @param optionMaturity The option maturity T.
	 * @param optionStrike The option strike.
	 * @return The delta of the option
	 */
	public static double blackScholesOptionDelta(
			double initialStockValue,
			double riskFreeRate,
			double volatility,
			double optionMaturity,
			double optionStrike)
	{
		if(optionMaturity < 0) {
			return 0;
		}
		else if(initialStockValue < 0) {
			// We use Indicator(S>K) = 1 - Indicator(-S>-K)
			return 1 - blackScholesOptionDelta(-initialStockValue, riskFreeRate, volatility, optionMaturity, -optionStrike);
		}
		else if(initialStockValue == 0)
		{
			// Limit case (where dPlus = +/- infty)
			if(optionStrike < 0)		return 1.0;					// dPlus = +infinity
			else if(optionStrike > 0)	return 0.0;					// dPlus = -infinity
			else						return 1.0;					// Matter of definition of continuity of the payoff function
		}
		else if((optionStrike <= 0.0) || (volatility <= 0.0) || (optionMaturity <= 0.0))		// (and initialStockValue > 0)
		{	
			// The Black-Scholes model does not consider it being an option
			return 1.0;
		}
		else
		{	
			// Calculate delta
			double dPlus = (Math.log(initialStockValue / optionStrike) + (riskFreeRate + 0.5 * volatility * volatility) * optionMaturity) / (volatility * Math.sqrt(optionMaturity));
			
			double delta = NormalDistribution.cumulativeDistribution(dPlus);
			
			return delta;
		}
	}

	/**
	 * Calculates the delta of a call option under a Black-Scholes model
	 * 
	 * The method also handles cases where the forward and/or option strike is negative
	 * and some limit cases where the forward or the option strike is zero.
	 * In the case forward = option strike = 0 the method returns 1.0.
	 * 
	 * @param initialStockValue The initial value of the underlying, i.e., the spot.
	 * @param riskFreeRate The risk free rate of the bank account numerarie.
     * @param volatility The Black-Scholes volatility.
     * @param optionMaturity The option maturity T.
	 * @param optionStrike The option strike.
	 * @return The delta of the option
	 */
	public static RandomVariableInterface blackScholesOptionDelta(
			RandomVariableInterface initialStockValue,
			RandomVariableInterface riskFreeRate,
			RandomVariableInterface volatility,
			double optionMaturity,
			double optionStrike)
	{
		if(optionMaturity < 0) {
			return initialStockValue.mult(0.0);
		}
		else
		{	
			// Calculate delta
			RandomVariableInterface dPlus	= initialStockValue.div(optionStrike).log().add(volatility.squared().mult(0.5).add(riskFreeRate).mult(optionMaturity)).div(volatility).div(Math.sqrt(optionMaturity));
			
			UnivariateFunction cummulativeNormal = new UnivariateFunction() {
				public double value(double x) { return NormalDistribution.cumulativeDistribution(x); }
			};
			RandomVariableInterface delta = dPlus.apply(cummulativeNormal);
			
			return delta;
		}
	}

	/**
	 * This static method calculated the gamma of a call option under a Black-Scholes model
	 * 
	 * @param initialStockValue The initial value of the underlying, i.e., the spot.
	 * @param riskFreeRate The risk free rate of the bank account numerarie.
     * @param volatility The Black-Scholes volatility.
     * @param optionMaturity The option maturity T.
	 * @param optionStrike The option strike.
	 * @return The gamma of the option
	 */
	public static double blackScholesOptionGamma(
			double initialStockValue,
			double riskFreeRate,
			double volatility,
			double optionMaturity,
			double optionStrike)
	{
		if(optionStrike <= 0.0 || optionMaturity <= 0.0)
		{	
			// The Black-Scholes model does not consider it being an option
			return 0.0;
		}
		else
		{	
			// Calculate gamma
			double dPlus = (Math.log(initialStockValue / optionStrike) + (riskFreeRate + 0.5 * volatility * volatility) * optionMaturity) / (volatility * Math.sqrt(optionMaturity));
			
			double gamma = Math.exp(-0.5*dPlus*dPlus) / (Math.sqrt(2.0 * Math.PI * optionMaturity) * initialStockValue * volatility);
			
			return gamma;
		}
	}
	
	/**
	 * This static method calculated the gamma of a call option under a Black-Scholes model
	 * 
	 * @param initialStockValue The initial value of the underlying, i.e., the spot.
	 * @param riskFreeRate The risk free rate of the bank account numerarie.
     * @param volatility The Black-Scholes volatility.
     * @param optionMaturity The option maturity T.
	 * @param optionStrike The option strike.
	 * @return The gamma of the option
	 */
	public static RandomVariableInterface blackScholesOptionGamma(
			RandomVariableInterface initialStockValue,
			RandomVariableInterface riskFreeRate,
			RandomVariableInterface volatility,
			double optionMaturity,
			double optionStrike)
	{
		if(optionStrike <= 0.0 || optionMaturity <= 0.0)
		{	
			// The Black-Scholes model does not consider it being an option
			return initialStockValue.mult(0.0);
		}
		else
		{	
			// Calculate gamma
			RandomVariableInterface dPlus	= initialStockValue.div(optionStrike).log().add(volatility.squared().mult(0.5).add(riskFreeRate).mult(optionMaturity)).div(volatility).div(Math.sqrt(optionMaturity));
			
			RandomVariableInterface gamma	= dPlus.squared().mult(-0.5).exp().div(initialStockValue.mult(volatility).mult(Math.sqrt(2.0 * Math.PI * optionMaturity)));
			
			return gamma;
		}
	}

	/**
	 * This static method calculated the vega of a call option under a Black-Scholes model
	 * 
	 * @param initialStockValue The initial value of the underlying, i.e., the spot.
	 * @param riskFreeRate The risk free rate of the bank account numerarie.
     * @param volatility The Black-Scholes volatility.
     * @param optionMaturity The option maturity T.
	 * @param optionStrike The option strike.
	 * @return The vega of the option
	 */
	public static double blackScholesOptionVega(
			double initialStockValue,
			double riskFreeRate,
			double volatility,
			double optionMaturity,
			double optionStrike)
	{
		if(optionStrike <= 0.0 || optionMaturity <= 0.0)
		{	
			// The Black-Scholes model does not consider it being an option
			return 0.0;
		}
		else
		{
			// Calculate vega
			double dPlus = (Math.log(initialStockValue / optionStrike) + (riskFreeRate + 0.5 * volatility * volatility) * optionMaturity) / (volatility * Math.sqrt(optionMaturity));
			
			double vega = Math.exp(-0.5*dPlus*dPlus) / Math.sqrt(2.0 * Math.PI) * initialStockValue * Math.sqrt(optionMaturity);
			
			return vega;
		}
	}

	/**
     * Calculates the Black-Scholes option implied volatility of a call, i.e., the payoff
     * <p><i>max(S(T)-K,0)</i></p>, where <i>S</i> follows a log-normal process with constant log-volatility.
     *
	 * @param forward The forward of the underlying.
	 * @param optionMaturity The option maturity T.
	 * @param optionStrike The option strike. If the option strike is &le; 0.0 the method returns the value of the forward contract paying S(T)-K in T.
	 * @param payoffUnit The payoff unit (e.g., the discount factor)
     * @param optionValue The option value.
     * @return Returns the implied volatility of a European call option under the Black-Scholes model.
     */
	public static double blackScholesOptionImpliedVolatility(
			double forward,
			double optionMaturity,
			double optionStrike,
			double payoffUnit,
			double optionValue)
	{
		// Limit the maximum number of iterations, to ensure this calculation returns fast, e.g. in cases when there is no such thing as an implied vol
		// TODO: An exception should be thrown, when there is no implied volatility for the given value.
		int		maxIterations	= 500;
		double	maxAccuracy		= 1E-15;
		
		if(optionStrike <= 0.0)
		{	
			// Actually it is not an option
			return 0.0;
		}
		else
		{
			// Calculate an lower and upper bound for the volatility
			double p = NormalDistribution.inverseCumulativeDistribution((optionValue/payoffUnit+optionStrike)/(forward+optionStrike)) / Math.sqrt(optionMaturity);
			double q = 2.0 * Math.abs(Math.log(forward/optionStrike)) / optionMaturity;

			double volatilityLowerBound = p + Math.sqrt(Math.max(p * p - q, 0.0));
			double volatilityUpperBound = p + Math.sqrt(         p * p + q      );

			// If strike is close to forward the two bounds are close to the analytic solution
			if(Math.abs(volatilityLowerBound - volatilityUpperBound) < maxAccuracy) return (volatilityLowerBound+volatilityUpperBound) / 2.0;

			// Solve for implied volatility
			NewtonsMethod solver = new NewtonsMethod(0.5*(volatilityLowerBound+volatilityUpperBound) /* guess */);
			while(solver.getAccuracy() > maxAccuracy && !solver.isDone() && solver.getNumberOfIterations() < maxIterations) {
				double volatility = solver.getNextPoint();

				// Calculate analytic value
				double dPlus                = (Math.log(forward / optionStrike) + 0.5 * volatility * volatility * optionMaturity) / (volatility * Math.sqrt(optionMaturity));
				double dMinus               = dPlus - volatility * Math.sqrt(optionMaturity);				
				double valueAnalytic		= (forward * NormalDistribution.cumulativeDistribution(dPlus) - optionStrike * NormalDistribution.cumulativeDistribution(dMinus)) * payoffUnit;
				double derivativeAnalytic	= forward * Math.sqrt(optionMaturity) * Math.exp(-0.5*dPlus*dPlus) / Math.sqrt(2.0*Math.PI) * payoffUnit;

				double error = valueAnalytic - optionValue;

				solver.setValueAndDerivative(error,derivativeAnalytic);
			}

			return solver.getBestPoint();
		}
	}

	/**
     * Calculates the Black-Scholes option value of a digital call option.
     * 
	 * @param initialStockValue The initial value of the underlying, i.e., the spot.
	 * @param riskFreeRate The risk free rate of the bank account numerarie.
     * @param volatility The Black-Scholes volatility.
     * @param optionMaturity The option maturity T.
	 * @param optionStrike The option strike.
	 * @return Returns the value of a European call option under the Black-Scholes model
	 */
	public static double blackScholesDigitalOptionValue(
			double initialStockValue,
			double riskFreeRate,
			double volatility,
			double optionMaturity,
			double optionStrike)
	{
		if(optionStrike <= 0.0)
		{
			// The Black-Scholes model does not consider it being an option
			return 1.0;
		}
		else
		{	
			// Calculate analytic value
			double dPlus = (Math.log(initialStockValue / optionStrike) + (riskFreeRate + 0.5 * volatility * volatility) * optionMaturity) / (volatility * Math.sqrt(optionMaturity));
			double dMinus = dPlus - volatility * Math.sqrt(optionMaturity);
			
			double valueAnalytic = Math.exp(- riskFreeRate * optionMaturity) * NormalDistribution.cumulativeDistribution(dMinus);
			
			return valueAnalytic;
		}
	}

	/**
	 * Calculates the delta of a digital option under a Black-Scholes model
	 * 
	 * @param initialStockValue The initial value of the underlying, i.e., the spot.
	 * @param riskFreeRate The risk free rate of the bank account numerarie.
     * @param volatility The Black-Scholes volatility.
     * @param optionMaturity The option maturity T.
	 * @param optionStrike The option strike.
	 * @return The delta of the digital option
	 */
	public static double blackScholesDigitalOptionDelta(
			double initialStockValue,
			double riskFreeRate,
			double volatility,
			double optionMaturity,
			double optionStrike)
	{
		if(optionStrike <= 0.0 || optionMaturity <= 0.0)
		{	
			// The Black-Scholes model does not consider it being an option
			return 0.0;
		}
		else
		{	
			// Calculate delta
			double dPlus = (Math.log(initialStockValue / optionStrike) + (riskFreeRate + 0.5 * volatility * volatility) * optionMaturity) / (volatility * Math.sqrt(optionMaturity));
			double dMinus = dPlus - volatility * Math.sqrt(optionMaturity);
			
			double delta = Math.exp(-0.5*dMinus*dMinus) / (Math.sqrt(2.0 * Math.PI * optionMaturity) * initialStockValue * volatility);
			
			return delta;
		}
	}

    /**
     * Calculate the value of a caplet assuming the Black'76 model.
     * 
     * @param forward The forward (spot).
     * @param volatility The Black'76 volatility.
     * @param optionMaturity The option maturity
     * @param optionStrike The option strike.
     * @param periodLength The period length of the underlying forward rate.
     * @param discountFactor The discount factor corresponding to the payment date (option maturity + period length).
     * @return Returns the value of a caplet under the Black'76 model
	 */
	public static double blackModelCapletValue(
			double forward,
			double volatility,
			double optionMaturity,
			double optionStrike,
			double periodLength,
			double discountFactor)
	{
		// May be interpreted as a special version of the Black-Scholes Formula
		return AnalyticFormulas.blackScholesGeneralizedOptionValue(forward, volatility, optionMaturity, optionStrike, periodLength * discountFactor);
	}

	/**
	 * Calculate the value of a digital caplet assuming the Black'76 model.
	 * 
	 * @param forward The forward (spot).
	 * @param volatility The Black'76 volatility.
	 * @param periodLength The period length of the underlying forward rate.
	 * @param discountFactor The discount factor corresponding to the payment date (option maturity + period length).
	 * @param optionMaturity The option maturity
	 * @param optionStrike The option strike.
	 * @return Returns the price of a digital caplet under the Black'76 model
	 */
	public static double blackModelDgitialCapletValue(
			double forward,
			double volatility,
			double periodLength,
			double discountFactor,
			double optionMaturity,
			double optionStrike)
	{
		// May be interpreted as a special version of the Black-Scholes Formula
		return AnalyticFormulas.blackScholesDigitalOptionValue(forward, 0.0, volatility, optionMaturity, optionStrike) * periodLength * discountFactor;
	}

    /**
     * Calculate the value of a swaption assuming the Black'76 model.
     * 
     * @param forwardSwaprate The forward (spot)
     * @param volatility The Black'76 volatility.
     * @param optionMaturity The option maturity.
     * @param optionStrike The option strike.
     * @param swapAnnuity The swap annuity corresponding to the underlying swap.
     * @return Returns the value of a Swaption under the Black'76 model
     */
    public static double blackModelSwaptionValue(
            double forwardSwaprate,
            double volatility,
            double optionMaturity,
            double optionStrike,
            double swapAnnuity)
    {
        // May be interpreted as a special version of the Black-Scholes Formula
        return AnalyticFormulas.blackScholesGeneralizedOptionValue(forwardSwaprate, volatility, optionMaturity, optionStrike, swapAnnuity);
    }

    /**
     * Calculate the value of a CMS option using the Black-Scholes model for the swap rate together with
     * the Hunt-Kennedy convexity adjustment.
     * 
     * @param forwardSwaprate The forward swap rate
     * @param volatility Volatility of the log of the swap rate
     * @param swapAnnuity The swap annuity
     * @param optionMaturity The option maturity
     * @param swapMaturity The swap maturity
     * @param payoffUnit The payoff unit, e.g., the discount factor corresponding to the payment date
     * @param optionStrike The option strike
     * @return Value of the CMS option
     */
    public static double huntKennedyCMSOptionValue(
    		double forwardSwaprate,
    		double volatility,
    		double swapAnnuity,
    		double optionMaturity,
    		double swapMaturity,
    		double payoffUnit,
    		double optionStrike)
    {
    	double a = 1.0/swapMaturity;
    	double b = (payoffUnit / swapAnnuity - a) / forwardSwaprate;
    	double convexityAdjustment = Math.exp(volatility*volatility*optionMaturity);

    	double valueUnadjusted	= blackModelSwaptionValue(forwardSwaprate, volatility, optionMaturity, optionStrike, swapAnnuity);
    	double valueAdjusted	= blackModelSwaptionValue(forwardSwaprate * convexityAdjustment, volatility, optionMaturity, optionStrike, swapAnnuity); 

    	return a * valueUnadjusted + b * forwardSwaprate * valueAdjusted;
    }
    
    /**
     * Calculate the value of a CMS strike using the Black-Scholes model for the swap rate together with
     * the Hunt-Kennedy convexity adjustment.
     * 
     * @param forwardSwaprate The forward swap rate
     * @param volatility Volatility of the log of the swap rate
     * @param swapAnnuity The swap annuity
     * @param optionMaturity The option maturity
     * @param swapMaturity The swap maturity
     * @param payoffUnit The payoff unit, e.g., the discount factor corresponding to the payment date
     * @param optionStrike The option strike
     * @return Value of the CMS strike
     */
    public static double huntKennedyCMSFloorValue(
		double forwardSwaprate,
		double volatility,
		double swapAnnuity,
		double optionMaturity,
		double swapMaturity,
		double payoffUnit,
		double optionStrike)
    {
    	double huntKennedyCMSOptionValue = huntKennedyCMSOptionValue(forwardSwaprate, volatility, swapAnnuity, optionMaturity, swapMaturity, payoffUnit, optionStrike);
    	
    	// A floor is an option plus the strike (max(X,K) = max(X-K,0) + K)
    	return huntKennedyCMSOptionValue + optionStrike * payoffUnit;
    }

    /**
     * Calculate the adjusted forward swaprate corresponding to a change of payoff unit from the given swapAnnuity to the given payoffUnit
     * using the Black-Scholes model for the swap rate together with the Hunt-Kennedy convexity adjustment.
     * 
     * @param forwardSwaprate The forward swap rate
     * @param volatility Volatility of the log of the swap rate
     * @param swapAnnuity The swap annuity
     * @param optionMaturity The option maturity
     * @param swapMaturity The swap maturity
     * @param payoffUnit The payoff unit, e.g., the discount factor corresponding to the payment date
     * @return Convexity adjusted forward rate
     */
    public static double huntKennedyCMSAdjustedRate(
		double forwardSwaprate,
		double volatility,
		double swapAnnuity,
		double optionMaturity,
		double swapMaturity,
		double payoffUnit)
    {
    	double a = 1.0/swapMaturity;
    	double b = (payoffUnit / swapAnnuity - a) / forwardSwaprate;
    	double convexityAdjustment = Math.exp(volatility*volatility*optionMaturity);
    	
    	double rateUnadjusted	= forwardSwaprate;
    	double rateAdjusted		= forwardSwaprate * convexityAdjustment; 

    	return (a * rateUnadjusted + b * forwardSwaprate * rateAdjusted) * swapAnnuity / payoffUnit;
    }

    /**
     * Re-implementation of the Excel PRICE function (a rather primitive bond price formula).
     * The re-implementation is not exact, because this function does not consider daycount conventions.
     * 
     * @param settlementDate Valuation date.
     * @param maturityDate Maturity date of the bond.
     * @param coupon Coupon payment.
     * @param yield Yield (discount factor, using frequency: 1/(1 + yield/frequency).
     * @param redemption Redemption (notional repayment).
     * @param frequency Frequency (1,2,4).
     * @return price Clean price.
     */
    public static double price(
    		java.util.Date settlementDate,
    		java.util.Date maturityDate,
    		double coupon,
    		double yield,
    		double redemption,
    		int frequency)
    {
    	double price = 0.0;

    	if(maturityDate.after(settlementDate)) {
    		price += redemption;
    	}

    	Calendar paymentDate = Calendar.getInstance();
    	paymentDate.setTime(maturityDate);
    	while(paymentDate.after(settlementDate)) {
    		price += coupon;
    		
    		// Disocunt back
            price /= 1.0 + yield / frequency;
    		paymentDate.add(Calendar.MONTH, -12/frequency);
    	}

    	Calendar periodEndDate = (Calendar)paymentDate.clone();
    	periodEndDate.add(Calendar.MONTH, +12/frequency);

    	// Accrue running period    	
    	double accrualPeriod = (paymentDate.getTimeInMillis() - settlementDate.getTime()) / (periodEndDate.getTimeInMillis() - paymentDate.getTimeInMillis());
    	price *= Math.pow(1.0 + yield / frequency, accrualPeriod);
    	price -= coupon * accrualPeriod;
    	
    	return price;
    }

    /**
     * Re-implementation of the Excel PRICE function (a rather primitive bond price formula).
     * The re-implementation is not exact, because this function does not consider daycount conventions.
     * We assume we have (int)timeToMaturity/frequency future periods and the running period has
     * an accrual period of timeToMaturity - frequency * ((int)timeToMaturity/frequency).
     * 
     * @param timeToMaturity The time to maturity.
     * @param coupon Coupon payment.
     * @param yield Yield (discount factor, using frequency: 1/(1 + yield/frequency).
     * @param redemption Redemption (notional repayment).
     * @param frequency Frequency (1,2,4).
     * @return price Clean price.
     */
    public static double price(
    		double timeToMaturity,
    		double coupon,
    		double yield,
    		double redemption,
    		int frequency)
    {
    	double price = 0.0;

    	if(timeToMaturity > 0) {
    		price += redemption;
    	}

    	double paymentTime = timeToMaturity;
    	while(paymentTime > 0) {
    		price += coupon;
    		
    		// Discount back
    		price = price / (1.0 + yield / frequency);
    		paymentTime -= 1.0 / frequency;
    	}

    	// Accrue running period
    	double accrualPeriod = 0.0- paymentTime;
    	price *= Math.pow(1.0 + yield / frequency, accrualPeriod);
    	price -= coupon * accrualPeriod;
    	
    	return price;
    }
}