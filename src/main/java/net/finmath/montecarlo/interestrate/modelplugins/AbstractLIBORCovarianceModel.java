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
 * A base class and interface description for the instantaneous covariance of
 * an forward rate interest rate model.
 * 
 * @author Christian Fries
 */
public abstract class AbstractLIBORCovarianceModel {
	
    private	TimeDiscretizationInterface		timeDiscretization;
    private TimeDiscretizationInterface		liborPeriodDiscretization;
	private	int								numberOfFactors;

	/**
	 * Constructor consuming time discretizations, which are handled by the super class.
	 * 
	 * @param timeDiscretization The vector of simulation time discretization points.
	 * @param liborPeriodDiscretization The vector of tenor discretization points.
	 * @param numberOfFactors The number of factors to use (a factor reduction is performed)
	 */
	public AbstractLIBORCovarianceModel(TimeDiscretizationInterface timeDiscretization, TimeDiscretizationInterface liborPeriodDiscretization, int numberOfFactors) {
		super();
		this.timeDiscretization			= timeDiscretization;
		this.liborPeriodDiscretization	= liborPeriodDiscretization;
		this.numberOfFactors			= numberOfFactors;
	}

	/**
	 * Return the factor loading for a given time and component index.
	 * The factor loading is the vector <i>f<sub>i</sub></i> such that the scalar product <br>
	 * <i>f<sub>j</sub>f<sub>k</sub> = f<sub>j,1</sub>f<sub>k,1</sub> + ... + f<sub>j,m</sub>f<sub>k,m</sub></i> <br>
	 * is the instantaneous covariance of the component <i>j</i> and <i>k</i>.
	 * 
	 * With respect to simulation time <i>t</i>, this method uses a piece wise constant interpolation, i.e.,
	 * it calculates <i>t_<sub>i</sub></i> such that <i>t_<sub>i</sub></i> is the largest point in <code>getTimeDiscretization</code>
	 * such that <i>t_<sub>i</sub> &le; t </i>.
	 * 
	 * @param time The time <i>t</i> at which factor loading is requested.
	 * @param component The index of the component  <i>i</i>.
	 * @param realizationAtTimeIndex The realization of the stochastic process (may be used to implement local volatility/covariance/correlation models).
	 * @return The factor loading <i>f<sub>i</sub>(t)</i>.
	 */
	public	RandomVariableInterface[]	getFactorLoading(double time, int component, RandomVariableInterface[] realizationAtTimeIndex) {
		int timeIndex = timeDiscretization.getTimeIndex(time);
		if(timeIndex < 0) timeIndex = Math.abs(timeIndex)-2;
		return getFactorLoading(timeIndex, component, realizationAtTimeIndex);
	}
	
	/**
	 * Return the factor loading for a given time index and component index.
	 * The factor loading is the vector <i>f<sub>i</sub></i> such that the scalar product <br>
	 * <i>f<sub>j</sub>f<sub>k</sub> = f<sub>j,1</sub>f<sub>k,1</sub> + ... + f<sub>j,m</sub>f<sub>k,m</sub></i> <br>
	 * is the instantaneous covariance of the component <i>j</i> and <i>k</i>.
	 * 
	 * @param timeIndex The time index at which factor loading is requested.
	 * @param component The index of the component  <i>i</i>.
	 * @param realizationAtTimeIndex The realization of the stochastic process (may be used to implement local volatility/covariance/correlation models).
	 * @return The factor loading <i>f<sub>i</sub>(t)</i>.
	 */
	public abstract	RandomVariableInterface[]	getFactorLoading(int timeIndex, int component, RandomVariableInterface[] realizationAtTimeIndex);

	/**
	 * Returns the pseudo inverse of the factor matrix.
	 * 
	 * @param timeIndex The time index at which factor loading inverse is requested.
	 * @param factor The index of the factor <i>j</i>.
	 * @param component The index of the component  <i>i</i>.
	 * @param realizationAtTimeIndex The realization of the stochastic process (may be used to implement local volatility/covariance/correlation models).
	 * @return The entry of the pseudo-inverse of the factor loading matrix.
	 */
	public abstract RandomVariableInterface	getFactorLoadingPseudoInverse(int timeIndex, int component, int factor, RandomVariableInterface[] realizationAtTimeIndex);

	/**
	 * Returns the instantaneous covariance calculated from factor loadings.
	 * 
	 * @param time The time <i>t</i> at which covariance is requested.
	 * @param component1 Index of component <i>i</i>.
	 * @param component2  Index of component <i>j</i>.
	 * @param realizationAtTimeIndex The realization of the stochastic process.
	 * @return The instantaneous covariance between component <i>i</i> and  <i>j</i>.
	 */
	public RandomVariableInterface getCovariance(double time, int component1, int component2, RandomVariableInterface[] realizationAtTimeIndex) {
		int timeIndex = timeDiscretization.getTimeIndex(time);
		if(timeIndex < 0) timeIndex = Math.abs(timeIndex)-2;

		return getCovariance(timeIndex, component1, component2, realizationAtTimeIndex);
	}

	/**
	 * Returns the instantaneous covariance calculated from factor loadings.
	 * 
	 * @param timeIndex The time index at which covariance is requested.
	 * @param component1 Index of component <i>i</i>.
	 * @param component2  Index of component <i>j</i>.
	 * @param realizationAtTimeIndex The realization of the stochastic process.
	 * @return The instantaneous covariance between component <i>i</i> and  <i>j</i>.
	 */
	public RandomVariableInterface getCovariance(int timeIndex, int component1, int component2, RandomVariableInterface[] realizationAtTimeIndex) {
		RandomVariableInterface covariance = new RandomVariable(0.0, 0.0);
		
		RandomVariableInterface[] factorLoadingOfComponent1 = getFactorLoading(timeIndex, component1, realizationAtTimeIndex);
		RandomVariableInterface[] factorLoadingOfComponent2 = getFactorLoading(timeIndex, component2, realizationAtTimeIndex);
		for(int factorIndex=0; factorIndex<this.getNumberOfFactors(); factorIndex++) {
            covariance = covariance.addProduct(factorLoadingOfComponent1[factorIndex],factorLoadingOfComponent2[factorIndex]);
		}

		return covariance;
	}


	/**
	 * The simulation time discretization associated with this model.
	 * 
	 * @return the timeDiscretization
	 */
	public TimeDiscretizationInterface getTimeDiscretization() {
		return timeDiscretization;
	}

	/**
	 * The forward rate time discretization associated with this model (defines the components).
	 * 
	 * @return the forward rate time discretization associated with this model.
	 */
	public TimeDiscretizationInterface getLiborPeriodDiscretization() {
		return liborPeriodDiscretization;
	}

	/**
	 * @return the numberOfFactors
	 */
	public int getNumberOfFactors() {
		return numberOfFactors;
	}
}
