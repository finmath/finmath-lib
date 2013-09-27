/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2005
 */
package net.finmath.marketdata.model.curves;

import java.io.Serializable;

import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * A container for discount factors. The discount curve is based on the {@link net.finmath.marketdata.model.curves.Curve} class.
 * It thus features all interpolation and extrapolation methods and interpolation entities
 * as {@link net.finmath.marketdata.model.curves.Curve}.
 * In addition the discount curve has the property that getValue(0) = 1
 * and getValue(x) = 0 for all x < 0.
 * 
 * @author Christian Fries
 */
public class DiscountCurve extends Curve implements Serializable, DiscountCurveInterface {

    private static final long serialVersionUID = -4126228588123963885L;

    private double[] parameter;
    
    private DiscountCurve(String name) {
    	super(name, InterpolationMethod.LINEAR, ExtrapolationMethod.CONSTANT, InterpolationEntity.LOG_OF_VALUE_PER_TIME);
    }

    public static DiscountCurve createDiscountCurveFromDiscountFactors(String name, double[] times, double[] givenDiscountFactors) {
		DiscountCurve discountFactors = new DiscountCurve(name);

		for(int timeIndex=0; timeIndex<times.length;timeIndex++) {
			discountFactors.addDiscountFactor(times[timeIndex], givenDiscountFactors[timeIndex]);
		}
		
		return discountFactors;
	}

	public static DiscountCurveInterface createDiscountFactorsFromForwardRates(String name, TimeDiscretizationInterface tenor, double[] forwardRates) {
		DiscountCurve discountFactors = new DiscountCurve(name);
//		discountFactors.addDiscountFactor(0.0, 1.0);

		double df = 1.0;
		for(int timeIndex=0; timeIndex<tenor.getNumberOfTimeSteps();timeIndex++) {
			df /= 1.0 + forwardRates[timeIndex] * tenor.getTimeStep(timeIndex);
			discountFactors.addDiscountFactor(tenor.getTime(timeIndex+1), df);
		}
		
		return discountFactors;
	}

	/* (non-Javadoc)
	 * @see net.finmath.marketdata.model.curves.DiscountCurveInterface#getDiscountFactor(double)
	 */
	@Override
	public double getDiscountFactor(double maturity)
	{
		return getDiscountFactor(null, maturity);
	}

	/* (non-Javadoc)
	 * @see net.finmath.marketdata.model.curves.DiscountCurveInterface#getDiscountFactor(double)
	 */
	@Override
    public double getDiscountFactor(AnalyticModelInterface model, double maturity)
	{
		// Check conventions
		if(maturity <  0)	return 0.0;
		if(maturity == 0)	return 1.0;
		
		return getValue(model, maturity);
	}

	
	/**
	 * Returns the zero rate for a given maturity, i.e., -ln(df(T)) / T where T is the given maturity and df(T) is
	 * the discount factor at time $T$.
	 * 
	 * @param maturity The given maturity.
	 * @return The zero rate.
	 */
	public double getZeroRate(double maturity)
	{
		if(maturity == 0) return this.getZeroRate(1.0E-14);
		
		return -Math.log(getDiscountFactor(null, maturity))/maturity;
	}

	/**
	 * Returns the zero rates for a given vector maturities.
	 * 
	 * @param maturities The given maturities.
	 * @return The zero rates.
	 */
	public double[] getZeroRates(double[] maturities)
	{
		double[] values = new double[maturities.length];

		for(int i=0; i<maturities.length; i++) values[i] = getZeroRate(maturities[i]);
		
		return values;
	}
	
	protected void addDiscountFactor(double maturity, double discountFactor) {
		this.addPoint(maturity, discountFactor);
	}
	
	public String toString() {
		return super.toString();
	}

	/* (non-Javadoc)
	 * @see net.finmath.marketdata.calibration.UnconstrainedParameterVectorInterface#getParameter()
	 */
    @Override
    public double[] getParameter() {
    	double[] parameterOfCurve = super.getParameter();
  
    	// Allocate local parameter storage
    	if(parameter == null || parameter.length != parameterOfCurve.length-1) parameter = new double[parameterOfCurve.length-0*1];

    	// Special parameter transformation for discount factors. Discount factors are constrained to be in (0,1).
    	for(int i=0;i < parameter.length; i++) parameter[i] = -Math.log(-Math.log(parameterOfCurve[i+0*1]));

    	return parameter;
    }

	/* (non-Javadoc)
	 * @see net.finmath.marketdata.calibration.UnconstrainedParameterVectorInterface#setParameter(double[])
	 */
    @Override
    public void setParameter(double[] parameter) {
    	double[] parameterOfCurve = super.getParameter();

    	// Special parameter transformation for discount factors. Discount factors are contrained to be in (0,1).
    	parameterOfCurve[0] = 1.0;
    	for(int i=0;i < parameter.length; i++) parameterOfCurve[i+0*1] = Math.exp(-Math.exp(-parameter[i]));
    	
    	super.setParameter(parameterOfCurve);
    }

    public DiscountCurveInterface getCloneForModifiedData(double time, double newValue) {
    	int timeIndex = this.getTimeIndex(time);

    	double[] parameterOfCurve = getParameter();
    	parameterOfCurve[timeIndex] = newValue;

    	DiscountCurve newDiscountCurve = (DiscountCurve)getCloneForParameter(parameterOfCurve);
    	
    	return newDiscountCurve;
    	
    }
}
