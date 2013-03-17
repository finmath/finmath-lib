/*
 * Created on 28.03.2008
 *
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 */
package net.finmath.montecarlo.model;

import net.finmath.stochastic.ImmutableRandomVariableInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * @author Christian Fries
 */
public interface AbstractModelInterface {

	/**
	 * Returns the time discretization of the model parameters. It is not necessary that this time discretization agrees
	 * with the discretization of the implementation.
	 * 
	 * @return The time discretization
	 */
	public abstract TimeDiscretizationInterface getTimeDiscretization();
	
    /**
     * @return The number of components
     */
    public abstract int getNumberOfComponents();

    /**
     * Returns the initial value of the state variable of the process, not to be
     * confused with the initial value of the model (which is the state space transform
     * applied to this state value.
     * 
     * @return The initial value of the state variable of the process.
     */
    public abstract ImmutableRandomVariableInterface[] getInitialState();

    /**
     * @param timeIndex The time index (related to the model times discretization).
     * @param realizationAtTimeIndex The given realization at timeIndex
     * @param realizationPredictor The given realization at <code>timeIndex+1</code> or null of no predictor is available.
     * @return The (average) drift from timeIndex to timeIndex+1
     */
    public abstract RandomVariableInterface[] getDrift(int timeIndex, ImmutableRandomVariableInterface[] realizationAtTimeIndex, ImmutableRandomVariableInterface[] realizationPredictor);

    /**
     * @param timeIndex The time index (related to the model times discretization).
     * @param componentIndex The index of the component.
     * @param realizationAtTimeIndex The given realization at timeIndex.
     * @param realizationPredictor The given realization at <code>timeIndex+1</code> or null of no predictor is available.
     * @return The (average) drift from timeIndex to timeIndex+1
     */
    public abstract RandomVariableInterface getDrift(int timeIndex, int componentIndex, ImmutableRandomVariableInterface[] realizationAtTimeIndex, ImmutableRandomVariableInterface[] realizationPredictor);
    
    /**
     * This method has to be implemented to return the factor loadings, i.e.
     * the coeffient lamba(i,j) such that <br>
     * dS(j) = (...) dt + S(j) * (lambda(1,j) dW(1) + ... + lambda(m,j) dW(m)) <br>
     * in an m-factor model. Here j denotes index of the component of the resulting
     * log-normal process and i denotes the index of the factor.
     * 
     * @param timeIndex The time index (related to the model times discretization).
     * @param factorIndex The index of the driving factor.
     * @param componentIndex The index of the driven component.
     * @return The factor loading for given factor and component.
     */
    public abstract RandomVariableInterface getFactorLoading(int timeIndex, int factorIndex, int componentIndex);
    
    public void applyStateSpaceTransform(RandomVariableInterface randomVariable);
}