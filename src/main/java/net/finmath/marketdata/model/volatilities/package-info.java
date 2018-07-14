/**
 * 		Provides interface specification and implementation of volatility surfaces, e.g.,
 * 		interest rate volatility surfaces like (implied) caplet volatilities and swaption
 * 		volatilities.
 * 		Volatility surfaces are mappings (t,K) &rarr; f(t,K), usually given by a discrete
 * 		set of points and an interpolation and extrapolation method or a functional form
 * 		(like the SABR model).
 *
 *
 * @author Christian Fries
 */
package net.finmath.marketdata.model.volatilities;
