/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2006
 */
package net.finmath.montecarlo.interestrate.modelplugins;

import net.finmath.montecarlo.RandomVariable;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * @author Christian Fries
 *
 */
public abstract class AbstractLIBORCovarianceModel {
    TimeDiscretizationInterface	timeDiscretization;
    TimeDiscretizationInterface	liborPeriodDiscretization;
	int				numberOfFactors;
	
    // You cannot instanciate the class empty
    @SuppressWarnings("unused")
    private AbstractLIBORCovarianceModel() {        
    }
    
	/**
	 * @param timeDiscretization The vector of simulation time discretization points.
	 * @param liborPeriodDiscretization The vector of tenor discretization points.
	 */
	public AbstractLIBORCovarianceModel(TimeDiscretizationInterface timeDiscretization, TimeDiscretizationInterface liborPeriodDiscretization, int numberOfFactors) {
		super();
		this.timeDiscretization			= timeDiscretization;
		this.liborPeriodDiscretization	= liborPeriodDiscretization;
		this.numberOfFactors			= numberOfFactors;
	}

	public abstract	RandomVariableInterface	getFactorLoading(int timeIndex, int factor, int component);
	public abstract RandomVariableInterface	getFactorLoadingPseudoInverse(int timeIndex, int component, int factor);

	/**
	 * Returns the covariance calculated from factor loadings.
	 * 
	 * @param timeIndex
	 * @param component1
	 * @param component2
	 * @return the covariance.
	 */
	public RandomVariableInterface getCovariance(int timeIndex, int component1, int component2) {
		RandomVariable covariance = new RandomVariable(0.0, 0.0);
		
		for(int factorIndex=0; factorIndex<this.getNumberOfFactors(); factorIndex++) {
			RandomVariableInterface factorLoadingOfComponent1 = getFactorLoading(timeIndex, factorIndex, component1);
			RandomVariableInterface factorLoadingOfComponent2 = getFactorLoading(timeIndex, factorIndex, component2);

			covariance.addProduct(factorLoadingOfComponent1,factorLoadingOfComponent2);
		}

		return covariance;
	}


	/**
	 * @return the timeDiscretization
	 */
	public TimeDiscretizationInterface getTimeDiscretization() {
		return timeDiscretization;
	}

	/**
	 * @param timeDiscretization the timeDiscretization to set
	 * @deprecated Will be an immutable object soon.
	 */
	public void setTimeDiscretization(TimeDiscretizationInterface timeDiscretization) {
		this.timeDiscretization = timeDiscretization;
	}

	/**
	 * @return the liborPeriodDiscretization
	 */
	public TimeDiscretizationInterface getLiborPeriodDiscretization() {
		return liborPeriodDiscretization;
	}

	/**
	 * @deprecated Will be an immutable object soon.
	 * @param liborPeriodDiscretization the liborPeriodDiscretization to set
	 */
	public void setLiborPeriodDiscretization(TimeDiscretizationInterface liborPeriodDiscretization) {
		this.liborPeriodDiscretization = liborPeriodDiscretization;
	}

	/**
	 * @return the numberOfFactors
	 */
	public int getNumberOfFactors() {
		return numberOfFactors;
	}

	/**
	 * @param numberOfFactors the numberOfFactors to set
	 * @deprecated Will be an immutable object soon.
	 */
	public void setNumberOfFactors(int numberOfFactors) {
		this.numberOfFactors = numberOfFactors;
	}
}
