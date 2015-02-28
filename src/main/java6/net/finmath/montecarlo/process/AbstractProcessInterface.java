/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 29.02.2008
 */
package net.finmath.montecarlo.process;

import net.finmath.montecarlo.BrownianMotionInterface;

/**
 * The interface for a process (numerical scheme) of a stochastic process <i>X</i> where
 * <i>X = f(Y)</i> and Y is an Itô process<br>
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
public interface AbstractProcessInterface extends ProcessInterface {

    /**
     * @return Returns the numberOfPaths.
     */
    int getNumberOfPaths();

    /**
     * @return Returns the numberOfFactors.
     */
    int getNumberOfFactors();

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