/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 29.02.2008
 */
package net.finmath.montecarlo.process;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.BrownianMotionInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * The interface for a process (numerical scheme) of a stochastic process <i>X</i> where
 * <i>X = f(Y)</i> and <br>
 * \[
 * dY_{j} = \mu_{j} dt + \lambda_{1,j} dW_{1} + \ldots + \lambda_{m,j} dW_{m}
 * \]
 * 
 * The parameters are provided by a model implementing {@link net.finmath.montecarlo.model.AbstractModelInterface}:
 * <ul>
 * <li>The value of <i>Y(0)</i> is provided by the method {@link net.finmath.montecarlo.model.AbstractModelInterface#getInitialState}.
 * <li>The value of &mu; is provided by the method {@link net.finmath.montecarlo.model.AbstractModelInterface#getDrift}.
 * <li>The value &lambda;<sub>j</sub> is provided by the method {@link net.finmath.montecarlo.model.AbstractModelInterface#getFactorLoading}.
 * <li>The function <i>f</i> is provided by the method {@link net.finmath.montecarlo.model.AbstractModelInterface#applyStateSpaceTransform}.
 * </ul>
 * Here, &mu; and &lambda;<sub>j</sub> may depend on <i>X</i>, which allows to implement stochastic drifts (like in a LIBOR market model)
 * of local volatility models.
 * 
 * @author Christian Fries
 * @see net.finmath.montecarlo.model.AbstractModelInterface The definition of the model.
 */
public interface AbstractProcessInterface {

    /**
     * This method returns the realization of a component of the process at a certain time index.
     * 
     * @param timeIndex Time index at which the process should be observed
     * @param component Component index of the process
     * @return The process component realizations (given as <code>RandomVariable</code>)
     * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
     */
    RandomVariableInterface getProcessValue(int timeIndex, int component) throws CalculationException;

    /**
     * This method returns the weights of a weighted Monte Carlo method (the probability density).
     * 
     * @param timeIndex Time index at which the process should be observed
     * @return A vector of positive weights which sums up to one
     * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
     */
    RandomVariableInterface getMonteCarloWeights(int timeIndex) throws CalculationException;

    /**
     * @return Returns the numberOfComponents.
     */
    int getNumberOfComponents();

    /**
     * @return Returns the numberOfPaths.
     */
    int getNumberOfPaths();

    /**
     * @return Returns the numberOfFactors.
     */
    int getNumberOfFactors();

    /**
     * @return Returns the timeDiscretization.
     */
    TimeDiscretizationInterface getTimeDiscretization();

    /**
     * @param timeIndex Time index.
     * @return Returns the time for a given time index.
     */
    double getTime(int timeIndex);

    /**
     * Returns the time index for a given simulation time.
     * @param time The given simulation time.
     * @return Returns the time index for a given time
     */
    int getTimeIndex(double time);

	/**
     * @return Returns the brownian motion used to generate this process
     */
    BrownianMotionInterface getBrownianMotion();

	/**
	 * Create and return a clone of this process. The clone is not tied to any model, but has the same
	 * process specification, that is, if the model is the same, it would generate the same paths.
	 * 
	 * @return Clone of the process
	 */
    AbstractProcessInterface clone();

}