/*
 * Created on 23.01.2004.
 *
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 */
package net.finmath.functions;

import net.finmath.optimizer.GoldenSectionSearch;
import net.finmath.stochastic.RandomVariable;

/**
 * This class implements some functions as static class methods related to the Bachelier model.
 * 
 * There are different variants of the Bachelier model, depending on if the volatility of the stock
 * or the volatility of the forward are assumed to be constant.
 * 
 * @see net.finmath.m
 *
 * @author Christian Fries
 * @version 1.10
 * @date 27.04.2012
 */
public class BachelierModel {

	// Suppress default constructor for non-instantiability
	private BachelierModel() {
		// This constructor will never be invoked
	}

	/**
	 * Calculates the option value of a call, i.e., the payoff max(S(T)-K,0), where S follows a
	 * normal process with numeraire scaled volatility, i.e., a Homogeneous Bachelier model
	 * \[
	 * 	\mathrm{d} S(t) = r S(t) \mathrm{d} t + \sigma exp(r t) \mathrm{d}W(t)
	 * \]
	 * Considering the numeraire \( N(t) = exp( r t ) \), this implies that \( F(t) = S(t)/N(t) \) follows
	 * \[
	 * 	\mathrm{d} F(t) = \sigma \mathrm{d}W(t) \text{.}
	 * \]
	 *
	 * @param forward The forward of the underlying \( F = S(0) \exp(r T) \).
	 * @param volatility The Bachelier volatility \( \sigma \).
	 * @param optionMaturity The option maturity T.
	 * @param optionStrike The option strike K.
	 * @param payoffUnit The payoff unit (e.g., the discount factor)
	 * @return Returns the value of a European call option under the Bachelier model.
	 */
	public static double bachelierHomogeneousOptionValue(
			final double forward,
			final double volatility,
			final double optionMaturity,
			final double optionStrike,
			final double payoffUnit)
	{
		double volatilityBachelier = volatility / payoffUnit;
		
		if(optionMaturity < 0) {
			return 0;
		}
		else if(forward == optionStrike) {
			return volatilityBachelier * Math.sqrt(optionMaturity / Math.PI / 2.0) * payoffUnit;
		}
		else
		{
			// Calculate analytic value
			final double dPlus = (forward - optionStrike) / (volatilityBachelier * Math.sqrt(optionMaturity));

			final double valueAnalytic = ((forward - optionStrike) * NormalDistribution.cumulativeDistribution(dPlus)
					+ volatilityBachelier * Math.sqrt(optionMaturity) * NormalDistribution.density(dPlus)) * payoffUnit;

			return valueAnalytic;
		}
	}

	/**
	 * Calculates the option value of a call, i.e., the payoff max(S(T)-K,0), where S follows a
	 * normal process with numeraire scaled volatility, i.e., a Homogeneous Bachelier model
	 * \[
	 * 	\mathrm{d} S(t) = r S(t) \mathrm{d} t + \sigma exp(r t) \mathrm{d}W(t)
	 * \]
	 * Considering the numeraire \( N(t) = exp( r t ) \), this implies that \( F(t) = S(t)/N(t) \) follows
	 * \[
	 * 	\mathrm{d} F(t) = \sigma \mathrm{d}W(t) \text{.}
	 * \]
	 *
	 * @param forward The forward of the underlying \( F = S(0) \exp(r T) \).
	 * @param volatility The Bachelier volatility \( \sigma \).
	 * @param optionMaturity The option maturity T.
	 * @param optionStrike The option strike.
	 * @param payoffUnit The payoff unit (e.g., the discount factor)
	 * @return Returns the value of a European call option under the Bachelier model.
	 */
	public static RandomVariable bachelierHomogeneousOptionValue(
			final RandomVariable forward,
			final RandomVariable volatility,
			final double optionMaturity,
			final double optionStrike,
			final RandomVariable payoffUnit)
	{
		if(optionMaturity < 0) {
			return forward.mult(0.0);
		}
		else
		{
			final RandomVariable volatilityBachelier = volatility.div(payoffUnit);
			final RandomVariable integratedVolatility = volatilityBachelier.mult(Math.sqrt(optionMaturity));
			final RandomVariable dPlus	= forward.sub(optionStrike).div(integratedVolatility);

			final RandomVariable valueAnalytic = dPlus.apply(NormalDistribution::cumulativeDistribution).mult(forward.sub(optionStrike))
					.add(dPlus.apply(NormalDistribution::density).mult(integratedVolatility)).mult(payoffUnit);

			return valueAnalytic;
		}
	}

	/**
	 * Calculates the Bachelier option implied volatility of a call, i.e., the payoff
	 * <p><i>max(S(T)-K,0)</i></p>, where <i>S</i> follows a normal process with numeraire scaled volatility.
	 *
	 * @param forward The forward of the underlying.
	 * @param optionMaturity The option maturity T.
	 * @param optionStrike The option strike. If the option strike is &le; 0.0 the method returns the value of the forward contract paying S(T)-K in T.
	 * @param payoffUnit The payoff unit (e.g., the discount factor)
	 * @param optionValue The option value.
	 * @return Returns the implied volatility of a European call option under the Bachelier model.
	 */
	public static double bachelierHomogeneousOptionImpliedVolatility(
			final double forward,
			final double optionMaturity,
			final double optionStrike,
			final double payoffUnit,
			final double optionValue)
	{
		if(forward == optionStrike) {
			return optionValue / Math.sqrt(optionMaturity / Math.PI / 2.0) / payoffUnit;
		}

		// Limit the maximum number of iterations, to ensure this calculation returns fast, e.g. in cases when there is no such thing as an implied vol
		// TODO An exception should be thrown, when there is no implied volatility for the given value.
		final int		maxIterations	= 100;
		final double	maxAccuracy		= 0.0;

		// Calculate an lower and upper bound for the volatility
		final double volatilityLowerBound = 0.0;
		final double volatilityUpperBound = Math.sqrt(2 * Math.PI * Math.E) * (optionValue / payoffUnit + Math.abs(forward-optionStrike)) / Math.sqrt(optionMaturity);

		// Solve for implied volatility
		final GoldenSectionSearch solver = new GoldenSectionSearch(volatilityLowerBound, volatilityUpperBound);
		while(solver.getAccuracy() > maxAccuracy && !solver.isDone() && solver.getNumberOfIterations() < maxIterations) {
			final double volatility = solver.getNextPoint();

			final double valueAnalytic	= bachelierHomogeneousOptionValue(forward, volatility, optionMaturity, optionStrike, payoffUnit);

			final double error = valueAnalytic - optionValue;

			solver.setValue(error*error);
		}

		return solver.getBestPoint();
	}

	/**
	 * Calculates the option delta dV(0)/dS(0) of a call option, i.e., the payoff V(T)=max(S(T)-K,0), where S follows a
	 * normal process with numeraire scaled volatility, i.e., a Homogeneous Bachelier model
	 * \[
	 * 	\mathrm{d} S(t) = r S(t) \mathrm{d} t + \sigma exp(r t) \mathrm{d}W(t)
	 * \]
	 * Considering the numeraire \( N(t) = exp( r t ) \), this implies that \( F(t) = S(t)/N(t) \) follows
	 * \[
	 * 	\mathrm{d} F(t) = \sigma \mathrm{d}W(t) \text{.}
	 * \]
	 *
	 * @param forward The forward of the underlying \( F = S(0) \exp(r T) \).
	 * @param volatility The Bachelier volatility \( \sigma \).
	 * @param optionMaturity The option maturity T.
	 * @param optionStrike The option strike K.
	 * @param payoffUnit The payoff unit (e.g., the discount factor)
	 * @return Returns the value of the option delta (dV/dS(0)) of a European call option under the Bachelier model.
	 */
	public static double bachelierHomogeneousOptionDelta(
			final double forward,
			final double volatility,
			final double optionMaturity,
			final double optionStrike,
			final double payoffUnit)
	{
		double volatilityBachelier = volatility / payoffUnit;

		if(optionMaturity < 0) {
			return 0;
		}
		else if(forward == optionStrike) {
			return 1.0 / 2.0;
		}
		else
		{
			// Calculate analytic value
			final double dPlus = (forward - optionStrike) / (volatilityBachelier * Math.sqrt(optionMaturity));

			final double deltaAnalytic = NormalDistribution.cumulativeDistribution(dPlus);

			return deltaAnalytic;
		}
	}

	/**
	 * Calculates the vega of a call, i.e., the payoff max(S(T)-K,0) P, where S follows a
	 * normal process with numeraire scaled volatility, i.e., a Homogeneous Bachelier model
	 * \[
	 * 	\mathrm{d} S(t) = r S(t) \mathrm{d} t + \sigma exp(r t) \mathrm{d}W(t)
	 * \]
	 * Considering the numeraire \( N(t) = exp( r t ) \), this implies that \( F(t) = S(t)/N(t) \) follows
	 * \[
	 * 	\mathrm{d} F(t) = \sigma \mathrm{d}W(t) \text{.}
	 * \]
	 *
	 * @param forward The forward of the underlying \( F = S(0) \exp(r T) \).
	 * @param volatility The Bachelier volatility \( \sigma \).
	 * @param optionMaturity The option maturity T.
	 * @param optionStrike The option strike.
	 * @param payoffUnit The payoff unit (e.g., the discount factor)
	 * @return Returns the vega of a European call option under the Bachelier model.
	 */
	public static double bachelierHomogeneousOptionVega(
			final double forward,
			final double volatility,
			final double optionMaturity,
			final double optionStrike,
			final double payoffUnit)
	{
		double volatilityBachelier = volatility / payoffUnit;

		if(optionMaturity < 0) {
			return 0;
		}
		else if(forward == optionStrike) {

			return Math.sqrt(optionMaturity / (Math.PI * 2.0)) * payoffUnit;
		}
		else
		{
			// Calculate analytic value
			final double dPlus = (forward - optionStrike) / (volatilityBachelier * Math.sqrt(optionMaturity));

			final double vegaAnalytic = Math.sqrt(optionMaturity) * NormalDistribution.density(dPlus) * payoffUnit;

			return vegaAnalytic * volatilityBachelier / volatility;
		}
	}

	/**
	 * Calculates the option value of a call, i.e., the payoff max(S(T)-K,0), where S follows a
	 * normal process with numeraire scaled volatility, i.e., a Homogeneous Bachelier model
	 * \[
	 * 	\mathrm{d} S(t) = r S(t) \mathrm{d} t + \sigma exp(r t) \mathrm{d}W(t)
	 * \]
	 * Considering the numeraire \( N(t) = exp( r t ) \), this implies that \( F(t) = S(t)/N(t) \) follows
	 * \[
	 * 	\mathrm{d} F(t) = \sigma \mathrm{d}W(t) \text{.}
	 * \]
	 *
	 * @param forward The forward of the underlying \( F = S(0) \exp(r T) \).
	 * @param volatility The Bachelier volatility \( \sigma \).
	 * @param optionMaturity The option maturity T.
	 * @param optionStrike The option strike K.
	 * @param payoffUnit The payoff unit (e.g., the discount factor)
	 * @return Returns the value of a European call option under the Bachelier model.
	 */
	public static double bachelierInhomogeneousOptionValue(
			final double forward,
			final double volatility,
			final double optionMaturity,
			final double optionStrike,
			final double payoffUnit)
	{
		double volatilityBachelier = volatility / payoffUnit * Math.sqrt((1 - payoffUnit*payoffUnit)/(-2.0*Math.log(payoffUnit)));
		
		if(optionMaturity < 0) {
			return 0;
		}
		else if(forward == optionStrike) {
			return volatilityBachelier * Math.sqrt(optionMaturity / Math.PI / 2.0) * payoffUnit;
		}
		else
		{
			// Calculate analytic value
			final double dPlus = (forward - optionStrike) / (volatilityBachelier * Math.sqrt(optionMaturity));

			final double valueAnalytic = ((forward - optionStrike) * NormalDistribution.cumulativeDistribution(dPlus)
					+ volatilityBachelier * Math.sqrt(optionMaturity) * NormalDistribution.density(dPlus)) * payoffUnit;

			return valueAnalytic;
		}
	}

	/**
	 * Calculates the option value of a call, i.e., the payoff max(S(T)-K,0), where S follows a
	 * normal process with numeraire scaled volatility, i.e., a Homogeneous Bachelier model
	 * \[
	 * 	\mathrm{d} S(t) = r S(t) \mathrm{d} t + \sigma exp(r t) \mathrm{d}W(t)
	 * \]
	 * Considering the numeraire \( N(t) = exp( r t ) \), this implies that \( F(t) = S(t)/N(t) \) follows
	 * \[
	 * 	\mathrm{d} F(t) = \sigma \mathrm{d}W(t) \text{.}
	 * \]
	 *
	 * @param forward The forward of the underlying \( F = S(0) \exp(r T) \).
	 * @param volatility The Bachelier volatility \( \sigma \).
	 * @param optionMaturity The option maturity T.
	 * @param optionStrike The option strike.
	 * @param payoffUnit The payoff unit (e.g., the discount factor)
	 * @return Returns the value of a European call option under the Bachelier model.
	 */
	public static RandomVariable bachelierInhomogeneousOptionValue(
			final RandomVariable forward,
			final RandomVariable volatility,
			final double optionMaturity,
			final double optionStrike,
			final RandomVariable payoffUnit)
	{
		if(optionMaturity < 0) {
			return forward.mult(0.0);
		}
		else
		{
//			double volatilityBachelier = volatility / payoffUnit * Math.sqrt((1 - payoffUnit*payoffUnit)/(2*Math.log(payoffUnit)));
			final RandomVariable volatilityBachelier = volatility.div(payoffUnit);
			final RandomVariable integratedVolatility = volatilityBachelier.mult(Math.sqrt(optionMaturity));
			final RandomVariable dPlus	= forward.sub(optionStrike).div(integratedVolatility);

			final RandomVariable valueAnalytic = dPlus.apply(NormalDistribution::cumulativeDistribution).mult(forward.sub(optionStrike))
					.add(dPlus.apply(NormalDistribution::density).mult(integratedVolatility)).mult(payoffUnit);

			return valueAnalytic;
		}
	}

	/**
	 * Calculates the Bachelier option implied volatility of a call, i.e., the payoff
	 * <p><i>max(S(T)-K,0)</i></p>, where <i>S</i> follows a normal process with numeraire scaled volatility.
	 *
	 * @param forward The forward of the underlying.
	 * @param optionMaturity The option maturity T.
	 * @param optionStrike The option strike. If the option strike is &le; 0.0 the method returns the value of the forward contract paying S(T)-K in T.
	 * @param payoffUnit The payoff unit (e.g., the discount factor)
	 * @param optionValue The option value.
	 * @return Returns the implied volatility of a European call option under the Bachelier model.
	 */
	public static double bachelierInhomogeneousOptionImpliedVolatility(
			final double forward,
			final double optionMaturity,
			final double optionStrike,
			final double payoffUnit,
			final double optionValue)
	{
		if(forward == optionStrike) {
			return optionValue / Math.sqrt(optionMaturity / Math.PI / 2.0) / payoffUnit;
		}

		// Limit the maximum number of iterations, to ensure this calculation returns fast, e.g. in cases when there is no such thing as an implied vol
		// TODO An exception should be thrown, when there is no implied volatility for the given value.
		final int		maxIterations	= 100;
		final double	maxAccuracy		= 0.0;

		// Calculate an lower and upper bound for the volatility
		final double volatilityLowerBound = 0.0;
		final double volatilityUpperBound = Math.sqrt(2 * Math.PI * Math.E) * (optionValue / payoffUnit + Math.abs(forward-optionStrike)) / Math.sqrt(optionMaturity);

		// Solve for implied volatility
		final GoldenSectionSearch solver = new GoldenSectionSearch(volatilityLowerBound, volatilityUpperBound);
		while(solver.getAccuracy() > maxAccuracy && !solver.isDone() && solver.getNumberOfIterations() < maxIterations) {
			final double volatility = solver.getNextPoint();

			final double valueAnalytic	= bachelierHomogeneousOptionValue(forward, volatility, optionMaturity, optionStrike, payoffUnit);

			final double error = valueAnalytic - optionValue;

			solver.setValue(error*error);
		}

		return solver.getBestPoint();
	}

	/**
	 * Calculates the option delta dV(0)/dS(0) of a call option, i.e., the payoff V(T)=max(S(T)-K,0), where S follows a
	 * normal process with numeraire scaled volatility, i.e., a Homogeneous Bachelier model
	 * \[
	 * 	\mathrm{d} S(t) = r S(t) \mathrm{d} t + \sigma exp(r t) \mathrm{d}W(t)
	 * \]
	 * Considering the numeraire \( N(t) = exp( r t ) \), this implies that \( F(t) = S(t)/N(t) \) follows
	 * \[
	 * 	\mathrm{d} F(t) = \sigma \mathrm{d}W(t) \text{.}
	 * \]
	 *
	 * @param forward The forward of the underlying \( F = S(0) \exp(r T) \).
	 * @param volatility The Bachelier volatility \( \sigma \).
	 * @param optionMaturity The option maturity T.
	 * @param optionStrike The option strike K.
	 * @param payoffUnit The payoff unit (e.g., the discount factor)
	 * @return Returns the value of the option delta (dV/dS(0)) of a European call option under the Bachelier model.
	 */
	public static double bachelierInhomogeneousOptionDelta(
			final double forward,
			final double volatility,
			final double optionMaturity,
			final double optionStrike,
			final double payoffUnit)
	{
		double volatilityBachelier = volatility / payoffUnit * Math.sqrt((1 - payoffUnit*payoffUnit)/(-2.0*Math.log(payoffUnit)));

		if(optionMaturity < 0) {
			return 0;
		}
		else if(forward == optionStrike) {
			return 1.0 / 2.0;
		}
		else
		{
			// Calculate analytic value
			final double dPlus = (forward - optionStrike) / (volatilityBachelier * Math.sqrt(optionMaturity));

			final double deltaAnalytic = NormalDistribution.cumulativeDistribution(dPlus);

			return deltaAnalytic;
		}
	}

	/**
	 * Calculates the vega of a call, i.e., the payoff max(S(T)-K,0) P, where S follows a
	 * normal process with numeraire scaled volatility, i.e., a Homogeneous Bachelier model
	 * \[
	 * 	\mathrm{d} S(t) = r S(t) \mathrm{d} t + \sigma exp(r t) \mathrm{d}W(t)
	 * \]
	 * Considering the numeraire \( N(t) = exp( r t ) \), this implies that \( F(t) = S(t)/N(t) \) follows
	 * \[
	 * 	\mathrm{d} F(t) = \sigma \mathrm{d}W(t) \text{.}
	 * \]
	 *
	 * @param forward The forward of the underlying \( F = S(0) \exp(r T) \).
	 * @param volatility The Bachelier volatility \( \sigma \).
	 * @param optionMaturity The option maturity T.
	 * @param optionStrike The option strike.
	 * @param payoffUnit The payoff unit (e.g., the discount factor)
	 * @return Returns the vega of a European call option under the Bachelier model.
	 */
	public static double bachelierInhomogeneousOptionVega(
			final double forward,
			final double volatility,
			final double optionMaturity,
			final double optionStrike,
			final double payoffUnit)
	{
		double volatilityBachelier = volatility / payoffUnit * Math.sqrt((1 - payoffUnit*payoffUnit)/(-2.0*Math.log(payoffUnit)));

		if(optionMaturity < 0) {
			return 0;
		}
		else if(forward == optionStrike) {

			return Math.sqrt(optionMaturity / (Math.PI * 2.0)) * payoffUnit;
		}
		else
		{
			// Calculate analytic value
			final double dPlus = (forward - optionStrike) / (volatilityBachelier * Math.sqrt(optionMaturity));

			final double vegaAnalytic = Math.sqrt(optionMaturity) * NormalDistribution.density(dPlus) * payoffUnit;

			return vegaAnalytic * volatilityBachelier/volatility;
		}
	}
}
