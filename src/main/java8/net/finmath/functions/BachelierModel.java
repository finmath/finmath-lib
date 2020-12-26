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
 * <dl>
 * 	<dt>Bachelier model</dt>
 * 	<dd>the model for a forward F(t) following dF = sigma dW, valuing the option max(F(T)-K,0) * N(T)</dd>
 *
 * 	<dt>Homogeneous Bachelier model</dt>
 * 	<dd>the model for a stock S(t) following dS = rS dt + sigma exp(rt) dW, valuing the option max(S(T)-K,0)</dd>
 *
 * 	<dt>Inhomogeneous Bachelier model</dt>
 * 	<dd>the model for a stock S(t) following dS = rS dt + sigma dW, valuing the option max(S(T)-K,0)</dd>
 * </dl>
 *
 * The class {@link net.finmath.montecarlo.assetderivativevaluation.models.BachelierModel} is the Monte-Carlo
 * implementation of a <i>Homogeneous Bachelier model</i>.
 *
 * The class {@link net.finmath.montecarlo.assetderivativevaluation.models.InhomogenousBachelierModel} is the Monte-Carlo
 * implementation of a <i>Inhomogeneous Bachelier model</i>.
 *
 * @see net.finmath.montecarlo.assetderivativevaluation.models.BachelierModel
 * @see net.finmath.montecarlo.assetderivativevaluation.models.InhomogenousBachelierModel
 *
 * @author Christian Fries
 * @version 1.11
 * @date 27.04.2012
 */
public class BachelierModel {

	// Suppress default constructor for non-instantiability
	private BachelierModel() {
		// This constructor will never be invoked
	}

	/**
	 * Calculates the option value of a call, i.e., the payoff max(S(T)-K,0), where S follows a
	 * normal process with numeraire scaled volatility, i.e., a homogeneous Bachelier model
	 * \[
	 * 	\mathrm{d} S(t) = r S(t) \mathrm{d} t + \sigma exp(-r (T-t)) \mathrm{d}W(t)
	 * \]
	 *
	 * Considering the numeraire \( N(t) = exp(-r (T-t)) \), this implies that \( F(t) = S(t)/N(t) \) follows
	 * \[
	 * 	\mathrm{d} F(t) = \sigma \mathrm{d}W(t) \text{.}
	 * \]
	 *
	 * @param forward The forward of the underlying \( F(0) = S(0)/N(0) = S(0) \exp(r T) \).
	 * @param volatility The Bachelier volatility \( \sigma \) of the forward process.
	 * @param optionMaturity The option maturity T.
	 * @param optionStrike The option strike K.
	 * @param payoffUnit The payoff unit (e.g., the discount factor \( N(0)/N(T) = \exp(-r T) \))
	 * @return Returns the value of a European call option under the Bachelier model.
	 */
	public static double bachelierOptionValue(
			final double forward,
			final double volatility,
			final double optionMaturity,
			final double optionStrike,
			final double payoffUnit)
	{
		if(optionMaturity < 0) {
			return 0;
		}
		else if(forward == optionStrike) {
			return volatility * Math.sqrt(optionMaturity / Math.PI / 2.0) * payoffUnit;
		}
		else
		{
			// Calculate analytic value
			final double dPlus = (forward - optionStrike) / (volatility * Math.sqrt(optionMaturity));

			final double valueAnalytic = ((forward - optionStrike) * NormalDistribution.cumulativeDistribution(dPlus)
					+ volatility * Math.sqrt(optionMaturity) * NormalDistribution.density(dPlus)) * payoffUnit;

			return valueAnalytic;
		}
	}

	/**
	 * Calculates the option value of a call, i.e., the payoff max(S(T)-K,0), where S follows a
	 * normal process with numeraire scaled volatility, i.e., a homogeneous Bachelier model
	 * \[
	 * 	\mathrm{d} S(t) = r S(t) \mathrm{d} t + \sigma exp(-r (T-t)) \mathrm{d}W(t)
	 * \]
	 * Considering the numeraire \( N(t) = exp(-r (T-t)) \), this implies that \( F(t) = S(t)/N(t) \) follows
	 * \[
	 * 	\mathrm{d} F(t) = \sigma \mathrm{d}W(t) \text{.}
	 * \]
	 *
	 * @param forward The forward of the underlying \( F(0) = S(0)/N(0) = S(0) \exp(r T) \).
	 * @param volatility The Bachelier volatility \( \sigma \) of the forward process.
	 * @param optionMaturity The option maturity T.
	 * @param optionStrike The option strike K.
	 * @param payoffUnit The payoff unit (e.g., the discount factor \( N(0)/N(T) = exp(-r T) \))
	 * @return Returns the value of a European call option under the Bachelier model.
	 */
	public static RandomVariable bachelierOptionValue(
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
			final RandomVariable integratedVolatility = volatility.mult(Math.sqrt(optionMaturity));
			final RandomVariable dPlus	= forward.sub(optionStrike).div(integratedVolatility);

			final RandomVariable valueAnalytic = dPlus.apply(NormalDistribution::cumulativeDistribution).mult(forward.sub(optionStrike))
					.add(dPlus.apply(NormalDistribution::density).mult(integratedVolatility)).mult(payoffUnit);

			return valueAnalytic;
		}
	}

	/**
	 * Calculates the Bachelier option implied volatility of a call, i.e., the payoff max(S(T)-K,0), where S follows a
	 * normal process with numeraire scaled volatility, i.e., a homogeneous Bachelier model
	 * \[
	 * 	\mathrm{d} S(t) = r S(t) \mathrm{d} t + \sigma exp(-r (T-t)) \mathrm{d}W(t)
	 * \]
	 * Considering the numeraire \( N(t) = exp(-r (T-t)) \), this implies that \( F(t) = S(t)/N(t) \) follows
	 * \[
	 * 	\mathrm{d} F(t) = \sigma \mathrm{d}W(t) \text{.}
	 * \]
	 *
	 * @param forward The forward of the underlying.
	 * @param optionMaturity The option maturity T.
	 * @param optionStrike The option strike. If the option strike is &le; 0.0 the method returns the value of the forward contract paying S(T)-K in T.
	 * @param payoffUnit The payoff unit (e.g., the discount factor)
	 * @param optionValue The option value.
	 * @return Returns the implied volatility of a European call option under the Bachelier model.
	 */
	public static double bachelierOptionImpliedVolatility(
			final double forward,
			final double optionMaturity,
			final double optionStrike,
			final double payoffUnit,
			final double optionValue)
	{
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

			final double valueAnalytic	= bachelierOptionValue(forward, volatility, optionMaturity, optionStrike, payoffUnit);

			final double error = valueAnalytic - optionValue;

			solver.setValue(error*error);
		}

		return solver.getBestPoint();
	}

	/**
	 * Calculates the option delta dV(0)/dS(0) of a call option, i.e., the payoff V(T)=max(S(T)-K,0), where S follows a
	 * normal process with numeraire scaled volatility, i.e., a homogeneous Bachelier model
	 * \[
	 * 	\mathrm{d} S(t) = r S(t) \mathrm{d} t + \sigma exp(-r (T-t)) \mathrm{d}W(t)
	 * \]
	 * Considering the numeraire \( N(t) = exp(-r (T-t)) \), this implies that \( F(t) = S(t)/N(t) \) follows
	 * \[
	 * 	\mathrm{d} F(t) = \sigma \mathrm{d}W(t) \text{.}
	 * \]
	 *
	 * @param forward The forward of the underlying \( F(0) = S(0)/N(0) = S(0) \exp(r T) \).
	 * @param volatility The Bachelier volatility \( \sigma \) of the forward process.
	 * @param optionMaturity The option maturity T.
	 * @param optionStrike The option strike K.
	 * @param payoffUnit The payoff unit (e.g., the discount factor \( N(0)/N(T) = exp(-r T) \))
	 * @return Returns the value of the option delta (dV/dS(0)) of a European call option under the Bachelier model.
	 */
	public static double bachelierOptionDelta(
			final double forward,
			final double volatility,
			final double optionMaturity,
			final double optionStrike,
			final double payoffUnit)
	{
		if(optionMaturity < 0) {
			return 0;
		}
		else if(forward == optionStrike) {
			return 1.0 / 2.0;
		}
		else
		{
			// Calculate analytic value
			final double dPlus = (forward - optionStrike) / (volatility * Math.sqrt(optionMaturity));

			final double deltaAnalytic = NormalDistribution.cumulativeDistribution(dPlus);

			return deltaAnalytic;
		}
	}

	/**
	 * Calculates the option delta dV(0)/dS(0) of a digital call option, i.e., the payoff V(T)=indicator(S(T)-K &gt; 0), where S follows a
	 * normal process with numeraire scaled volatility, i.e., a homogeneous Bachelier model
	 * \[
	 * 	\mathrm{d} S(t) = r S(t) \mathrm{d} t + \sigma exp(-r (T-t)) \mathrm{d}W(t)
	 * \]
	 * Considering the numeraire \( N(t) = exp(-r (T-t)) \), this implies that \( F(t) = S(t)/N(t) \) follows
	 * \[
	 * 	\mathrm{d} F(t) = \sigma \mathrm{d}W(t) \text{.}
	 * \]
	 *
	 * The delta reported is \[ \frac{1}{\sigma \sqrt{T}} \phi( \frac{F-K}{\sigma \sqrt{T}} ) \], where \( \phi \) is the density of the standard normal distribution.
	 *
	 * Note: The delta does not depend on the argument payoffUnit, due to \( \frac{\mathrm{d}F(0)}{\mathrm{d}S(0)} = \frac{1}{N(0)} \) being equal to 1 / payoffUnit.
	 *
	 * @param forward The forward of the underlying \( F(0) = S(0)/N(0) = S(0) \exp(r T) \).
	 * @param volatility The Bachelier volatility \( \sigma \) of the forward process.
	 * @param optionMaturity The option maturity T.
	 * @param optionStrike The option strike K.
	 * @param payoffUnit The payoff unit (e.g., the discount factor \( N(0)/N(T) = exp(-r T) \))
	 * @return Returns the value of the option delta (dV/dS(0)) of a European call option under the Bachelier model.
	 */
	public static double bachelierDigitalOptionDelta(
			final double forward,
			final double volatility,
			final double optionMaturity,
			final double optionStrike,
			final double payoffUnit)
	{
		if(optionMaturity < 0) {
			return 0;
		}
		else if(forward == optionStrike) {
			return 1.0 / Math.sqrt(2.0 * Math.PI) / (volatility * Math.sqrt(optionMaturity));
		}
		else
		{
			// Calculate analytic value
			final double dPlus = (forward - optionStrike) / (volatility * Math.sqrt(optionMaturity));

			final double deltaAnalytic = NormalDistribution.density(dPlus) / (volatility * Math.sqrt(optionMaturity));

			return deltaAnalytic;
		}
	}

	/**
	 * Calculates the option delta dV(0)/dS(0) of a call option, i.e., the payoff V(T)=max(S(T)-K,0), where S follows a
	 * normal process with numeraire scaled volatility, i.e., a homogeneous Bachelier model
	 * \[
	 * 	\mathrm{d} S(t) = r S(t) \mathrm{d} t + \sigma exp(-r (T-t)) \mathrm{d}W(t)
	 * \]
	 * Considering the numeraire \( N(t) = exp(-r (T-t)) \), this implies that \( F(t) = S(t)/N(t) \) follows
	 * \[
	 * 	\mathrm{d} F(t) = \sigma \mathrm{d}W(t) \text{.}
	 * \]
	 *
	 * @param forward The forward of the underlying \( F(0) = S(0)/N(0) = S(0) \exp(r T) \).
	 * @param volatility The Bachelier volatility \( \sigma \) of the forward process.
	 * @param optionMaturity The option maturity T.
	 * @param optionStrike The option strike K.
	 * @param payoffUnit The payoff unit (e.g., the discount factor \( N(0)/N(T) = exp(-r T) \))
	 * @return Returns the value of the option delta (dV/dS(0)) of a European call option under the Bachelier model.
	 */
	public static RandomVariable bachelierOptionDelta(
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
			final RandomVariable integratedVolatility = volatility.mult(Math.sqrt(optionMaturity));
			final RandomVariable dPlus	= forward.sub(optionStrike).div(integratedVolatility);

			final RandomVariable deltaAnalytic = dPlus.apply(NormalDistribution::cumulativeDistribution);

			return deltaAnalytic;
		}
	}

	/**
	 * Calculates the vega of a call, i.e., the payoff max(S(T)-K,0) P, where S follows a
	 * normal process with numeraire scaled volatility, i.e., a homogeneous Bachelier model
	 * \[
	 * 	\mathrm{d} S(t) = r S(t) \mathrm{d} t + \sigma exp(-r (T-t)) \mathrm{d}W(t)
	 * \]
	 * Considering the numeraire \( N(t) = exp(-r (T-t)) \), this implies that \( F(t) = S(t)/N(t) \) follows
	 * \[
	 * 	\mathrm{d} F(t) = \sigma \mathrm{d}W(t) \text{.}
	 * \]
	 *
	 * @param forward The forward of the underlying \( F(0) = S(0)/N(0) = S(0) \exp(r T) \).
	 * @param volatility The Bachelier volatility \( \sigma \) of the forward process.
	 * @param optionMaturity The option maturity T.
	 * @param optionStrike The option strike K.
	 * @param payoffUnit The payoff unit (e.g., the discount factor \( N(0)/N(T) = exp(-r T) \))
	 * @return Returns the vega of a European call option under the Bachelier model.
	 */
	public static double bachelierOptionVega(
			final double forward,
			final double volatility,
			final double optionMaturity,
			final double optionStrike,
			final double payoffUnit)
	{
		if(optionMaturity < 0) {
			return 0;
		}
		else if(forward == optionStrike) {

			return Math.sqrt(optionMaturity / (Math.PI * 2.0)) * payoffUnit;
		}
		else
		{
			// Calculate analytic value
			final double dPlus = (forward - optionStrike) / (volatility * Math.sqrt(optionMaturity));

			final double vegaAnalytic = Math.sqrt(optionMaturity) * NormalDistribution.density(dPlus) * payoffUnit;

			return vegaAnalytic;
		}
	}

	/**
	 * Calculates the vega of a call, i.e., the payoff max(S(T)-K,0) P, where S follows a
	 * normal process with numeraire scaled volatility, i.e., a homogeneous Bachelier model
	 * \[
	 * 	\mathrm{d} S(t) = r S(t) \mathrm{d} t + \sigma exp(-r (T-t)) \mathrm{d}W(t)
	 * \]
	 * Considering the numeraire \( N(t) = exp(-r (T-t)) \), this implies that \( F(t) = S(t)/N(t) \) follows
	 * \[
	 * 	\mathrm{d} F(t) = \sigma \mathrm{d}W(t) \text{.}
	 * \]
	 *
	 * @param forward The forward of the underlying \( F(0) = S(0)/N(0) = S(0) \exp(r T) \).
	 * @param volatility The Bachelier volatility \( \sigma \) of the forward process.
	 * @param optionMaturity The option maturity T.
	 * @param optionStrike The option strike K.
	 * @param payoffUnit The payoff unit (e.g., the discount factor \( N(0)/N(T) = exp(-r T) \))
	 * @return Returns the vega of a European call option under the Bachelier model.
	 */
	public static RandomVariable bachelierOptionVega(
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
			final RandomVariable integratedVolatility = volatility.mult(Math.sqrt(optionMaturity));
			final RandomVariable dPlus	= forward.sub(optionStrike).div(integratedVolatility);

			final RandomVariable vegaAnalytic = dPlus.apply(NormalDistribution::density).mult(payoffUnit).mult(Math.sqrt(optionMaturity));

			return vegaAnalytic;
		}
	}

	/**
	 * Calculates the option value of a call, i.e., the payoff max(S(T)-K,0), where S follows a
	 * normal process with numeraire scaled volatility, i.e., a homogeneous Bachelier model
	 * \[
	 * 	\mathrm{d} S(t) = r S(t) \mathrm{d} t + \sigma exp(rt) \mathrm{d}W(t)
	 * \]
	 *
	 * Considering the numeraire \( N(t) = exp(-r (T-t)) \), this implies that \( F(t) = S(t)/N(t) \) follows
	 * \[
	 * 	\mathrm{d} F(t) = \sigma / N(T) \mathrm{d}W(t) \text{.}
	 * \]
	 *
	 * @param forward The forward of the underlying \( F(0) = S(0)/N(0) = S(0) \exp(r T) \).
	 * @param volatility The Bachelier volatility \( \sigma \) of the forward process.
	 * @param optionMaturity The option maturity T.
	 * @param optionStrike The option strike K.
	 * @param payoffUnit The payoff unit (e.g., the discount factor \( N(0)/N(T) = exp(-r T) \))
	 * @return Returns the value of a European call option under the Bachelier model.
	 */
	public static double bachelierHomogeneousOptionValue(
			final double forward,
			final double volatility,
			final double optionMaturity,
			final double optionStrike,
			final double payoffUnit)
	{
		return bachelierOptionValue(forward, volatility/payoffUnit, optionMaturity, optionStrike, payoffUnit);
	}

	/**
	 * Calculates the option value of a call, i.e., the payoff max(S(T)-K,0), where S follows a
	 * normal process with numeraire scaled volatility, i.e., a homogeneous Bachelier model
	 * \[
	 * 	\mathrm{d} S(t) = r S(t) \mathrm{d} t + \sigma exp(rt) \mathrm{d}W(t)
	 * \]
	 *
	 * Considering the numeraire \( N(t) = exp(-r (T-t)) \), this implies that \( F(t) = S(t)/N(t) \) follows
	 * \[
	 * 	\mathrm{d} F(t) = \sigma / N(T) \mathrm{d}W(t) \text{.}
	 * \]
	 *
	 * @param forward The forward of the underlying \( F(0) = S(0)/N(0) = S(0) \exp(r T) \).
	 * @param volatility The Bachelier volatility \( \sigma \) of the forward process.
	 * @param optionMaturity The option maturity T.
	 * @param optionStrike The option strike K.
	 * @param payoffUnit The payoff unit (e.g., the discount factor \( N(0)/N(T) = exp(-r T) \))
	 * @return Returns the value of a European call option under the Bachelier model.
	 */
	public static RandomVariable bachelierHomogeneousOptionValue(
			final RandomVariable forward,
			final RandomVariable volatility,
			final double optionMaturity,
			final double optionStrike,
			final RandomVariable payoffUnit)
	{
		return bachelierOptionValue(forward, volatility.div(payoffUnit), optionMaturity, optionStrike, payoffUnit);
	}

	/**
	 * Calculates the Bachelier option implied volatility of a call, i.e., the payoff max(S(T)-K,0), where S follows a
	 * normal process with numeraire scaled volatility, i.e., a homogeneous Bachelier model
	 * \[
	 * 	\mathrm{d} S(t) = r S(t) \mathrm{d} t + \sigma exp(rt) \mathrm{d}W(t)
	 * \]
	 *
	 * Considering the numeraire \( N(t) = exp(-r (T-t)) \), this implies that \( F(t) = S(t)/N(t) \) follows
	 * \[
	 * 	\mathrm{d} F(t) = \sigma / N(T) \mathrm{d}W(t) \text{.}
	 * \]
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
		return bachelierOptionImpliedVolatility(forward, optionMaturity, optionStrike, payoffUnit, optionValue)*payoffUnit;
	}

	/**
	 * Calculates the option delta dV(0)/dS(0) of a call option, i.e., the payoff V(T)=max(S(T)-K,0), where S follows a
	 * normal process with numeraire scaled volatility, i.e., a homogeneous Bachelier model
	 * \[
	 * 	\mathrm{d} S(t) = r S(t) \mathrm{d} t + \sigma exp(rt) \mathrm{d}W(t)
	 * \]
	 *
	 * Considering the numeraire \( N(t) = exp(-r (T-t)) \), this implies that \( F(t) = S(t)/N(t) \) follows
	 * \[
	 * 	\mathrm{d} F(t) = \sigma / N(T) \mathrm{d}W(t) \text{.}
	 * \]
	 *
	 *
	 * @param forward The forward of the underlying \( F(0) = S(0)/N(0) = S(0) \exp(r T) \).
	 * @param volatility The Bachelier volatility \( \sigma \) of the forward process.
	 * @param optionMaturity The option maturity T.
	 * @param optionStrike The option strike K.
	 * @param payoffUnit The payoff unit (e.g., the discount factor \( N(0)/N(T) = exp(-r T) \))
	 * @return Returns the value of the option delta (dV/dS(0)) of a European call option under the Bachelier model.
	 */
	public static double bachelierHomogeneousOptionDelta(
			final double forward,
			final double volatility,
			final double optionMaturity,
			final double optionStrike,
			final double payoffUnit)
	{
		return bachelierOptionDelta(forward, volatility/payoffUnit, optionMaturity, optionStrike, payoffUnit);
	}

	/**
	 * Calculates the option delta dV(0)/dS(0) of a call option, i.e., the payoff V(T)=max(S(T)-K,0), where S follows a
	 * normal process with numeraire scaled volatility, i.e., a homogeneous Bachelier model
	 * \[
	 * 	\mathrm{d} S(t) = r S(t) \mathrm{d} t + \sigma exp(rt) \mathrm{d}W(t)
	 * \]
	 *
	 * Considering the numeraire \( N(t) = exp(-r (T-t)) \), this implies that \( F(t) = S(t)/N(t) \) follows
	 * \[
	 * 	\mathrm{d} F(t) = \sigma / N(T) \mathrm{d}W(t) \text{.}
	 * \]
	 *
	 *
	 * @param forward The forward of the underlying \( F(0) = S(0)/N(0) = S(0) \exp(r T) \).
	 * @param volatility The Bachelier volatility \( \sigma \) of the forward process.
	 * @param optionMaturity The option maturity T.
	 * @param optionStrike The option strike K.
	 * @param payoffUnit The payoff unit (e.g., the discount factor \( N(0)/N(T) = exp(-r T) \))
	 * @return Returns the value of the option delta (dV/dS(0)) of a European call option under the Bachelier model.
	 */
	public static RandomVariable bachelierHomogeneousOptionDelta(
			final RandomVariable forward,
			final RandomVariable volatility,
			final double optionMaturity,
			final double optionStrike,
			final RandomVariable payoffUnit)
	{
		return bachelierOptionDelta(forward, volatility.div(payoffUnit), optionMaturity, optionStrike, payoffUnit);
	}

	/**
	 * Calculates the vega of a call, i.e., the payoff max(S(T)-K,0) P, where S follows a
	 * normal process with numeraire scaled volatility, i.e., a homogeneous Bachelier model
	 * \[
	 * 	\mathrm{d} S(t) = r S(t) \mathrm{d} t + \sigma exp(rt) \mathrm{d}W(t)
	 * \]
	 *
	 * Considering the numeraire \( N(t) = exp(-r (T-t)) \), this implies that \( F(t) = S(t)/N(t) \) follows
	 * \[
	 * 	\mathrm{d} F(t) = \sigma / N(T) \mathrm{d}W(t) \text{.}
	 * \]
	 *
	 * @param forward The forward of the underlying \( F(0) = S(0)/N(0) = S(0) \exp(r T) \).
	 * @param volatility The Bachelier volatility \( \sigma \) of the forward process.
	 * @param optionMaturity The option maturity T.
	 * @param optionStrike The option strike K.
	 * @param payoffUnit The payoff unit (e.g., the discount factor \( N(0)/N(T) = exp(-r T) \))
	 * @return Returns the vega of a European call option under the Bachelier model.
	 */
	public static double bachelierHomogeneousOptionVega(
			final double forward,
			final double volatility,
			final double optionMaturity,
			final double optionStrike,
			final double payoffUnit)
	{
		return bachelierOptionVega(forward, volatility/payoffUnit, optionMaturity, optionStrike, payoffUnit)/payoffUnit;
	}

	/**
	 * Calculates the vega of a call, i.e., the payoff max(S(T)-K,0) P, where S follows a
	 * normal process with numeraire scaled volatility, i.e., a homogeneous Bachelier model
	 * \[
	 * 	\mathrm{d} S(t) = r S(t) \mathrm{d} t + \sigma exp(rt) \mathrm{d}W(t)
	 * \]
	 *
	 * Considering the numeraire \( N(t) = exp(-r (T-t)) \), this implies that \( F(t) = S(t)/N(t) \) follows
	 * \[
	 * 	\mathrm{d} F(t) = \sigma / N(T) \mathrm{d}W(t) \text{.}
	 * \]
	 *
	 * @param forward The forward of the underlying \( F(0) = S(0)/N(0) = S(0) \exp(r T) \).
	 * @param volatility The Bachelier volatility \( \sigma \) of the forward process.
	 * @param optionMaturity The option maturity T.
	 * @param optionStrike The option strike K.
	 * @param payoffUnit The payoff unit (e.g., the discount factor \( N(0)/N(T) = exp(-r T) \))
	 * @return Returns the vega of a European call option under the Bachelier model.
	 */
	public static RandomVariable bachelierHomogeneousOptionVega(
			final RandomVariable forward,
			final RandomVariable volatility,
			final double optionMaturity,
			final double optionStrike,
			final RandomVariable payoffUnit)
	{
		return bachelierOptionVega(forward, volatility.div(payoffUnit), optionMaturity, optionStrike, payoffUnit).div(payoffUnit);
	}

	/**
	 * Calculates the option value of a call, i.e., the payoff max(S(T)-K,0), where S follows a
	 * normal process with constant volatility, i.e., a inhomogeneous Bachelier model
	 * \[
	 * 	\mathrm{d} S(t) = r S(t) \mathrm{d} t + \sigma \mathrm{d}W(t)
	 * \]
	 * Considering the numeraire \( N(t) = exp(-r (T-t)) \), this implies that \( F(t) = S(t)/N(t) \) follows
	 * \[
	 * 	\mathrm{d} F(t) = \sigma exp(r (T-t)) \mathrm{d}W(t) \text{.}
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
		final double scaling = payoffUnit != 1 ? Math.sqrt((payoffUnit*payoffUnit-1)/(2.0*Math.log(payoffUnit))) : 1.0;
		final double volatilityEffective = volatility * scaling;

		return bachelierHomogeneousOptionValue(forward, volatilityEffective, optionMaturity, optionStrike, payoffUnit);
	}

	/**
	 * Calculates the option value of a call, i.e., the payoff max(S(T)-K,0), where S follows a
	 * normal process with constant volatility, i.e., a inhomogeneous Bachelier model
	 * \[
	 * 	\mathrm{d} S(t) = r S(t) \mathrm{d} t + \sigma \mathrm{d}W(t)
	 * \]
	 * Considering the numeraire \( N(t) = exp(-r (T-t)) \), this implies that \( F(t) = S(t)/N(t) \) follows
	 * \[
	 * 	\mathrm{d} F(t) = \sigma exp(r (T-t)) \mathrm{d}W(t) \text{.}
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
		// TODO The formula fails if payoffUnit == 1
		final RandomVariable volatilityEffective = volatility.mult(payoffUnit.squared().sub(1.0).div(payoffUnit.log().mult(2)).sqrt());

		return bachelierHomogeneousOptionValue(forward, volatilityEffective, optionMaturity, optionStrike, payoffUnit);
	}

	/**
	 * Calculates the Bachelier option implied volatility of a call, i.e., the payoff
	 * <p><i>max(S(T)-K,0)</i></p>, where <i>S</i> follows a
	 * normal process with constant volatility, i.e., a inhomogeneous Bachelier model
	 * \[
	 * 	\mathrm{d} S(t) = r S(t) \mathrm{d} t + \sigma \mathrm{d}W(t)
	 * \]
	 * Considering the numeraire \( N(t) = exp(-r (T-t)) \), this implies that \( F(t) = S(t)/N(t) \) follows
	 * \[
	 * 	\mathrm{d} F(t) = \sigma exp(r (T-t)) \mathrm{d}W(t) \text{.}
	 * \]
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
		final double volatilityEffective = bachelierHomogeneousOptionImpliedVolatility(forward, optionMaturity, optionStrike, payoffUnit, optionValue);

		final double scaling = payoffUnit != 1 ? Math.sqrt((payoffUnit*payoffUnit-1)/(2.0*Math.log(payoffUnit))) : 1.0;
		final double volatility = volatilityEffective / scaling;

		return volatility;
	}

	/**
	 * Calculates the option delta dV(0)/dS(0) of a call option, i.e., the payoff V(T)=max(S(T)-K,0), where S follows a
	 * normal process with constant volatility, i.e., a inhomogeneous Bachelier model
	 * \[
	 * 	\mathrm{d} S(t) = r S(t) \mathrm{d} t + \sigma \mathrm{d}W(t)
	 * \]
	 * Considering the numeraire \( N(t) = exp(-r (T-t)) \), this implies that \( F(t) = S(t)/N(t) \) follows
	 * \[
	 * 	\mathrm{d} F(t) = \sigma exp(r (T-t)) \mathrm{d}W(t) \text{.}
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
		final double scaling = payoffUnit != 1 ? Math.sqrt((payoffUnit*payoffUnit-1)/(2.0*Math.log(payoffUnit))) : 1.0;
		final double volatilityEffective = volatility * scaling;

		return bachelierHomogeneousOptionDelta(forward, volatilityEffective, optionMaturity, optionStrike, payoffUnit);
	}

	/**
	 * Calculates the option delta dV(0)/dS(0) of a call option, i.e., the payoff V(T)=max(S(T)-K,0), where S follows a
	 * normal process with constant volatility, i.e., a inhomogeneous Bachelier model
	 * \[
	 * 	\mathrm{d} S(t) = r S(t) \mathrm{d} t + \sigma \mathrm{d}W(t)
	 * \]
	 * Considering the numeraire \( N(t) = exp(-r (T-t)) \), this implies that \( F(t) = S(t)/N(t) \) follows
	 * \[
	 * 	\mathrm{d} F(t) = \sigma exp(r (T-t)) \mathrm{d}W(t) \text{.}
	 * \]
	 *
	 * This implies an effective "Bachelier" integrated variance, being (with \( s = 0 \)
	 * \[
	 * 	1/T \int_{0}^{T} \sigma^2 exp(2 r (T-t)) \mathrm{d}t \ = \ sigma^2 \frac{exp(2 r (T-0))-exp(2 r (T-T)}{2 r T}
	 * \]
	 *
	 * @param forward The forward of the underlying \( F = S(0) \exp(r T) \).
	 * @param volatility The Bachelier volatility \( \sigma \).
	 * @param optionMaturity The option maturity T.
	 * @param optionStrike The option strike K.
	 * @param payoffUnit The payoff unit (e.g., the discount factor)
	 * @return Returns the value of the option delta (dV/dS(0)) of a European call option under the Bachelier model.
	 */
	public static RandomVariable bachelierInhomogeneousOptionDelta(
			final RandomVariable forward,
			final RandomVariable volatility,
			final double optionMaturity,
			final double optionStrike,
			final RandomVariable payoffUnit)
	{
		// TODO The formula fails if payoffUnit == 1
		final RandomVariable volatilityEffective = volatility.mult(payoffUnit.squared().sub(1.0).div(payoffUnit.log().mult(2)).sqrt());

		return bachelierHomogeneousOptionDelta(forward, volatilityEffective, optionMaturity, optionStrike, payoffUnit);
	}

	/**
	 * Calculates the vega of a call, i.e., the payoff max(S(T)-K,0) P, where S follows a
	 * normal process with constant volatility, i.e., a Inhomogeneous Bachelier model
	 * \[
	 * 	\mathrm{d} S(t) = r S(t) \mathrm{d} t + \sigma \mathrm{d}W(t)
	 * \]
	 * Considering the numeraire \( N(t) = exp( r t ) \), this implies that \( F(t) = S(t)/N(t) \) follows
	 * \[
	 * 	\mathrm{d} F(t) = \sigma exp(-r t) \mathrm{d}W(t) \text{.}
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
		final double scaling = payoffUnit != 1 ? Math.sqrt((payoffUnit*payoffUnit-1)/(2.0*Math.log(payoffUnit))) : 1.0;
		final double volatilityEffective = volatility * scaling;

		final double vegaHomogenouse = bachelierHomogeneousOptionVega(forward, volatilityEffective, optionMaturity, optionStrike, payoffUnit);

		return vegaHomogenouse * scaling;
	}

	/**
	 * Calculates the vega of a call, i.e., the payoff max(S(T)-K,0) P, where S follows a
	 * normal process with constant volatility, i.e., a Inhomogeneous Bachelier model
	 * \[
	 * 	\mathrm{d} S(t) = r S(t) \mathrm{d} t + \sigma \mathrm{d}W(t)
	 * \]
	 * Considering the numeraire \( N(t) = exp( r t ) \), this implies that \( F(t) = S(t)/N(t) \) follows
	 * \[
	 * 	\mathrm{d} F(t) = \sigma exp(-r t) \mathrm{d}W(t) \text{.}
	 * \]
	 *
	 * @param forward The forward of the underlying \( F = S(0) \exp(r T) \).
	 * @param volatility The Bachelier volatility \( \sigma \).
	 * @param optionMaturity The option maturity T.
	 * @param optionStrike The option strike.
	 * @param payoffUnit The payoff unit (e.g., the discount factor)
	 * @return Returns the vega of a European call option under the Bachelier model.
	 */
	public static RandomVariable bachelierInhomogeneousOptionVega(
			final RandomVariable forward,
			final RandomVariable volatility,
			final double optionMaturity,
			final double optionStrike,
			final RandomVariable payoffUnit)
	{
		// TODO The formula fails if payoffUnit == 1
		final RandomVariable volatilityEffective = volatility.mult(payoffUnit.squared().sub(1.0).div(payoffUnit.log().mult(2)).sqrt());

		final RandomVariable vegaHomogenouse = bachelierHomogeneousOptionVega(forward, volatilityEffective, optionMaturity, optionStrike, payoffUnit);

		return vegaHomogenouse.mult(volatilityEffective).div(volatility);
	}
}
