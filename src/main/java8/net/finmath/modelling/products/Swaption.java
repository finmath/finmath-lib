/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 31.03.2019
 */

package net.finmath.modelling.products;

/**
 * A market interface for all swaption implementations and a holder for some product specific definitions.
 *
 * @author Christian Fries
 */
public interface Swaption {

	/**
	 * Swaptions specific value units, like swaption implied volatilities.
	 *
	 * @author Christian Fries
	 */
	enum ValueUnit {
		/** Returns the value of the swaption. **/
		VALUE,
		/** Returns the Black-Scholes implied integrated variance, i.e., <i>&sigma;<sup>2</sup> T</i>. **/
		INTEGRATEDVARIANCELOGNORMAL,
		/**
		 * Returns the Black-Scholes implied integrated variance, i.e., <i>&sigma;<sup>2</sup> T</i>.
		 * @deprecated Use INTEGRATEDVARIANCELOGNORMAL instead.
		 */
		INTEGRATEDLOGNORMALVARIANCE,
		/** Returns the Black-Scholes implied volatility, i.e., <i>&sigma;</i>. **/
		VOLATILITYLOGNORMAL,
		/** Returns the Bachelier implied integrated variance, i.e., <i>&sigma;<sup>2</sup> T</i>. **/
		INTEGRATEDVARIANCENORMAL,
		/**
		 * Returns the Bachelier implied integrated variance, i.e., <i>&sigma;<sup>2</sup> T</i>.
		 * @deprecated Use INTEGRATEDVARIANCENORMAL instead.
		 */
		INTEGRATEDNORMALVARIANCE,
		/** Returns the Bachelier implied volatility, i.e., <i>&sigma;</i>. **/
		VOLATILITYNORMAL,
		/**
		 * Returns the Black-Scholes implied integrated variance, i.e., <i>&sigma;<sup>2</sup> T</i>.
		 * @deprecated Use INTEGRATEDVARIANCELOGNORMAL instead
		 */
		INTEGRATEDVARIANCE,
		/**
		 * Returns the Black-Scholes implied volatility, i.e., <i>&sigma;</i>.
		 * @deprecated Use VOLATILITYLOGNORMAL instead
		 */
		VOLATILITY,

		/**
		 * The Bachelier implied volatility, assuming an ATM option.
		 * The value is obtained by multiplying VALUE with 1.0 / Math.sqrt(optionMaturity / Math.PI / 2.0) / annuity.
		 */
		VOLATILITYNORMALATM

	}
}

