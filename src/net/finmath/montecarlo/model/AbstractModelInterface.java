/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 28.03.2008
 */
package net.finmath.montecarlo.model;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.process.AbstractProcessInterface;
import net.finmath.stochastic.ImmutableRandomVariableInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * The interface for a model of a stochastic process <i>X</i> where
 * <i>X = f(Y)</i> and <br>
 * <i>dY<sub>j</sub> = &mu;<sub>j</sub> dt + &lambda;<sub>1,j</sub> dW<sub>1</sub> + ... + &lambda;<sub>m,j</sub> dW<sub>m</sub></i> <br>
 * 
 * getInitialState
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
 */
public interface AbstractModelInterface {

	/**
	 * Returns the time discretization of the model parameters. It is not necessary that this time discretization agrees
	 * with the discretization of the implementation.
	 * 
	 * @return The time discretization
	 */
    TimeDiscretizationInterface getTimeDiscretization();
	
    /**
     * Returns the number of components
     * 
     * @return The number of components
     */
    int getNumberOfComponents();

    /**
     * Applied the state space transform <i>f<sub>i</sub></i> to the given state random variable
     * such that <i>Y<sub>i</sub> &rarr; f<sub>i</sub>(Y<sub>i</sub>) =: X<sub>i</sub></i>.
     * 
     * @param componentIndex The component index <i>i</i>.
     * @param randomVariableInterface The state random variable <i>Y<sub>i</sub></i>.
     */
    RandomVariableInterface applyStateSpaceTransform(int componentIndex, RandomVariableInterface randomVariableInterface);

    /**
     * Returns the initial value of the state variable of the process, not to be
     * confused with the initial value of the model (which is the state space transform
     * applied to this state value.
     * 
     * @return The initial value of the state variable of the process.
     */
    ImmutableRandomVariableInterface[] getInitialState();

	/**
	 * Return the numeraire at a given time index.
	 * Note: The random variable returned is a defensive copy and may be modified.
	 * 
	 * @param time The time <i>t</i> for which the numeraire <i>N(t)</i> should be returned.
	 * @return The numeraire at the specified time as <code>RandomVariable</code>
	 * @throws net.finmath.exception.CalculationException
	 */
    RandomVariableInterface getNumeraire(double time) throws CalculationException;

    /**
     * @param timeIndex The time index (related to the model times discretization).
     * @param realizationAtTimeIndex The given realization at timeIndex
     * @param realizationPredictor The given realization at <code>timeIndex+1</code> or null of no predictor is available.
     * @return The (average) drift from timeIndex to timeIndex+1
     */
    RandomVariableInterface[] getDrift(int timeIndex, ImmutableRandomVariableInterface[] realizationAtTimeIndex, ImmutableRandomVariableInterface[] realizationPredictor);

    /**
     * @param timeIndex The time index (related to the model times discretization).
     * @param componentIndex The index of the component.
     * @param realizationAtTimeIndex The given realization at timeIndex.
     * @param realizationPredictor The given realization at <code>timeIndex+1</code> or null of no predictor is available.
     * @return The (average) drift from timeIndex to timeIndex+1
     */
    RandomVariableInterface getDrift(int timeIndex, int componentIndex, ImmutableRandomVariableInterface[] realizationAtTimeIndex, ImmutableRandomVariableInterface[] realizationPredictor);
    
    /**
     * Returns the number of factors
     * 
     * @return The number of factors
     */
    int getNumberOfFactors();

    /**
     * This method has to be implemented to return the factor loadings, i.e.
     * the coeffient vector <br>
     * <i>lamba(j) =  (lambda(1,j), ..., lambda(m,j))</i> such that <br>
     * <i>dS(j) = (...) dt + S(j) * (lambda(1,j) dW(1) + ... + lambda(m,j) dW(m))</i> <br>
     * in an m-factor model. Here j denotes index of the component of the resulting
     * log-normal process and i denotes the index of the factor.
     * 
     * @param timeIndex The time index (related to the model times discretization).
     * @param componentIndex The index of the driven component.
     * @param realizationAtTimeIndex The realization of S at the time corresponding to timeIndex (in order to implement local and stochastic volatlity models).
     * @return The factor loading for given factor and component.
     */
    RandomVariableInterface[] getFactorLoading(int timeIndex, int componentIndex, ImmutableRandomVariableInterface[] realizationAtTimeIndex);

    /**
     * Set the numerical scheme used to generate the stochastic process.
     * 
     * The model needs the numerical scheme to calculate, e.g., the numeraire.
     * 
     * @param process The process.
     */
    void setProcess(AbstractProcessInterface process);

    /**
     * Get the numerical scheme used to generate the stochastic process.
     * 
     * The model needs the numerical scheme to calculate, e.g., the numeraire.
     * 
     * @return the process
     */
    AbstractProcessInterface getProcess();

}