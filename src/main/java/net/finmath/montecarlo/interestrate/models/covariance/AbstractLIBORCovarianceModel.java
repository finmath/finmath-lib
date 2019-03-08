/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2006
 */
package net.finmath.montecarlo.interestrate.models.covariance;

import java.io.Serializable;
import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;

/**
 * A base class and interface description for the instantaneous covariance of
 * an forward rate interest rate model.
 *
 * @author Christian Fries
 * @version 1.0
 */
public abstract class AbstractLIBORCovarianceModel implements Serializable, LIBORCovarianceModel {

	private static final long serialVersionUID = 5364544247367259329L;

	private	TimeDiscretization		timeDiscretization;
	private TimeDiscretization		liborPeriodDiscretization;
	private	int								numberOfFactors;

	/**
	 * Constructor consuming time discretizations, which are handled by the super class.
	 *
	 * @param timeDiscretization The vector of simulation time discretization points.
	 * @param liborPeriodDiscretization The vector of tenor discretization points.
	 * @param numberOfFactors The number of factors to use (a factor reduction is performed)
	 */
	public AbstractLIBORCovarianceModel(TimeDiscretization timeDiscretization, TimeDiscretization liborPeriodDiscretization, int numberOfFactors) {
		super();
		this.timeDiscretization			= timeDiscretization;
		this.liborPeriodDiscretization	= liborPeriodDiscretization;
		this.numberOfFactors			= numberOfFactors;
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.models.covariance.LIBORCovarianceModel#getFactorLoading(double, double, net.finmath.stochastic.RandomVariable[])
	 */
	@Override
	public	RandomVariable[]	getFactorLoading(double time, double component, RandomVariable[] realizationAtTimeIndex) {
		int componentIndex = liborPeriodDiscretization.getTimeIndex(component);
		if(componentIndex < 0) {
			componentIndex = -componentIndex - 2;
		}
		return getFactorLoading(time, componentIndex, realizationAtTimeIndex);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.models.covariance.LIBORCovarianceModel#getFactorLoading(double, int, net.finmath.stochastic.RandomVariable[])
	 */
	@Override
	public	RandomVariable[]	getFactorLoading(double time, int component, RandomVariable[] realizationAtTimeIndex) {
		int timeIndex = timeDiscretization.getTimeIndex(time);
		if(timeIndex < 0) {
			timeIndex = -timeIndex - 2;
		}
		return getFactorLoading(timeIndex, component, realizationAtTimeIndex);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.models.covariance.LIBORCovarianceModel#getFactorLoading(int, int, net.finmath.stochastic.RandomVariable[])
	 */
	@Override
	public abstract	RandomVariable[]	getFactorLoading(int timeIndex, int component, RandomVariable[] realizationAtTimeIndex);

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.models.covariance.LIBORCovarianceModel#getFactorLoadingPseudoInverse(int, int, int, net.finmath.stochastic.RandomVariable[])
	 */
	@Override
	public abstract RandomVariable	getFactorLoadingPseudoInverse(int timeIndex, int component, int factor, RandomVariable[] realizationAtTimeIndex);

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.models.covariance.LIBORCovarianceModel#getCovariance(double, int, int, net.finmath.stochastic.RandomVariable[])
	 */
	@Override
	public RandomVariable getCovariance(double time, int component1, int component2, RandomVariable[] realizationAtTimeIndex) {
		int timeIndex = timeDiscretization.getTimeIndex(time);
		if(timeIndex < 0) {
			timeIndex = Math.abs(timeIndex)-2;
		}

		return getCovariance(timeIndex, component1, component2, realizationAtTimeIndex);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.models.covariance.LIBORCovarianceModel#getCovariance(int, int, int, net.finmath.stochastic.RandomVariable[])
	 */
	@Override
	public RandomVariable getCovariance(int timeIndex, int component1, int component2, RandomVariable[] realizationAtTimeIndex) {

		RandomVariable[] factorLoadingOfComponent1 = getFactorLoading(timeIndex, component1, realizationAtTimeIndex);
		RandomVariable[] factorLoadingOfComponent2 = getFactorLoading(timeIndex, component2, realizationAtTimeIndex);

		// Multiply first factor loading (this avoids that we have to init covariance to 0).
		RandomVariable covariance = factorLoadingOfComponent1[0].mult(factorLoadingOfComponent2[0]);

		// Add others, if any
		for(int factorIndex=1; factorIndex<this.getNumberOfFactors(); factorIndex++) {
			covariance = covariance.addProduct(factorLoadingOfComponent1[factorIndex],factorLoadingOfComponent2[factorIndex]);
		}

		return covariance;
	}


	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.models.covariance.LIBORCovarianceModel#getTimeDiscretization()
	 */
	@Override
	public TimeDiscretization getTimeDiscretization() {
		return timeDiscretization;
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.models.covariance.LIBORCovarianceModel#getLiborPeriodDiscretization()
	 */
	@Override
	public TimeDiscretization getLiborPeriodDiscretization() {
		return liborPeriodDiscretization;
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.models.covariance.LIBORCovarianceModel#getNumberOfFactors()
	 */
	@Override
	public int getNumberOfFactors() {
		return numberOfFactors;
	}
	
	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.models.covariance.LIBORCovarianceModel#getCloneWithModifiedData()
	 */
	@Override
	public abstract AbstractLIBORCovarianceModelParametric getCloneWithModifiedData(Map<String, Object> dataModified) throws CalculationException;
}
