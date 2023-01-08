/**
 * Provides interfaces and classes needed to generate interest rate models model (using numerical
 * algorithms from <code>net.finmath.montecarlo.process</code>.
 * 
 * The basic interface and classes provide a <code>TermStructureModel</code> which provides a
 * <i>forward rate</i> {@link net.finmath.montecarlo.interestrate.TermStructureModel#getForwardRate(net.finmath.montecarlo.process.MonteCarloProcess, double, double, double)}
 * and a
 * <i>numeraire</i> {@link net.finmath.montecarlo.interestrate.TermStructureModel#getNumeraire(net.finmath.montecarlo.process.MonteCarloProcess, double)}
 *
 * There is a legacy interface <code>LIBORModel</code> which provides the 
 * forward rates on a tenor time discretization under the name
 * {@link net.finmath.montecarlo.interestrate.LIBORModel#getLIBOR(net.finmath.montecarlo.process.MonteCarloProcess, int, int)}
 * 
 * @author Christian Fries
 */
package net.finmath.montecarlo.interestrate;
