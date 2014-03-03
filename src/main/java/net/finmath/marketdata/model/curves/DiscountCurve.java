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
 * and getValue(x) = 0 for all x &lt; 0.
 * 
 * @author Christian Fries
 */
public class DiscountCurve extends Curve implements Serializable, DiscountCurveInterface {

	private static final long serialVersionUID = -4126228588123963885L;

	/**
	 * Create an empty discount curve using default interpolation and extrapolation methods.
	 * 
	 * @param name The name of this discount curve.
	 */
	private DiscountCurve(String name) {
		super(name, null, InterpolationMethod.LINEAR, ExtrapolationMethod.CONSTANT, InterpolationEntity.LOG_OF_VALUE_PER_TIME);
	}

	/**
	 * Create an empty discount curve using given interpolation and extrapolation methods.
	 *
	 * @param name The name of this discount curve.
	 * @param interpolationMethod The interpolation method used for the curve.
	 * @param extrapolationMethod The extrapolation method used for the curve.
	 * @param interpolationEntity The entity interpolated/extrapolated.
	 */
	private DiscountCurve(String name, InterpolationMethod interpolationMethod,
			ExtrapolationMethod extrapolationMethod, InterpolationEntity interpolationEntity){

		super(name, null, interpolationMethod, extrapolationMethod, interpolationEntity);
	}

	/**
	 * Create a discount curve from given times and given discount factors using default interpolation and extrapolation methods.
	 * 
	 * @param name The name of this discount curve.
	 * @param times Array of times as doubles.
	 * @param givenDiscountFactors Array of corresponding discount factors.
	 * @return A new discount factor object.
	 */
	public static DiscountCurve createDiscountCurveFromDiscountFactors(String name, double[] times, double[] givenDiscountFactors) {
		DiscountCurve discountFactors = new DiscountCurve(name);

		for(int timeIndex=0; timeIndex<times.length;timeIndex++) {
			discountFactors.addDiscountFactor(times[timeIndex], givenDiscountFactors[timeIndex]);
		}

		return discountFactors;
	}

	/**
	 * Create a discount curve from given times and given zero rates using default interpolation and extrapolation methods.
	 * 
	 * @param name The name of this discount curve.
	 * @param times Array of times as doubles.
	 * @param givenZeroRates Array of corresponding zero rates.
	 * @return A new discount factor object.
	 */
	public static DiscountCurve createDiscountCurveFromZeroRates(String name, double[] times, double[] givenZeroRates) {
		double[] givenDiscountFactors = new double[givenZeroRates.length];

		for(int timeIndex=0; timeIndex<times.length;timeIndex++) {
			givenDiscountFactors[timeIndex] = Math.exp(givenZeroRates[timeIndex] * times[timeIndex]);
		}

		return createDiscountCurveFromDiscountFactors(name, times, givenDiscountFactors);
	}

	/**
	 * Create a discount curve from given time discretization and forward rates.
	 * This function is provided for "single interest rate curve" frameworks.
	 * 
	 * @param name The name of this discount curve.
	 * @param tenor Time discretization for the forward rates
	 * @param forwardRates Array of forward rates.
	 * @return A new discount factor object.
	 */
	public static DiscountCurveInterface createDiscountFactorsFromForwardRates(String name, TimeDiscretizationInterface tenor, double[] forwardRates) {
		DiscountCurve discountFactors = new DiscountCurve(name);

		double df = 1.0;
		for(int timeIndex=0; timeIndex<tenor.getNumberOfTimeSteps();timeIndex++) {
			df /= 1.0 + forwardRates[timeIndex] * tenor.getTimeStep(timeIndex);
			discountFactors.addDiscountFactor(tenor.getTime(timeIndex+1), df);
		}

		return discountFactors;
	}

	/**
	 * Create a discount curve from given times and given discount factors using given interpolation and extrapolation methods.
	 *
	 * @param name The name of this discount curve.
	 * @param times Array of times as doubles.
	 * @param givenDiscountFactors Array of corresponding discount factors.
	 * @param interpolationMethod The interpolation method used for the curve.
	 * @param extrapolationMethod The extrapolation method used for the curve.
	 * @param interpolationEntity The entity interpolated/extrapolated.
	 * @return A new discount factor object.
	 */
	public static DiscountCurve createDiscountCurveFromDiscountFactors(String name, double[] times, double[] givenDiscountFactors,
			InterpolationMethod interpolationMethod, ExtrapolationMethod extrapolationMethod, InterpolationEntity interpolationEntity) {

		DiscountCurve discountFactors = new DiscountCurve(name, interpolationMethod, extrapolationMethod, interpolationEntity);

		for(int timeIndex=0; timeIndex<times.length;timeIndex++) {
			discountFactors.addDiscountFactor(times[timeIndex], givenDiscountFactors[timeIndex]);
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

	private void addDiscountFactor(double maturity, double discountFactor) {
		boolean isParameter = (maturity > 0);

		this.addPoint(maturity, discountFactor, isParameter);
	}

	public String toString() {
		return super.toString();
	}

	public DiscountCurveInterface getCloneForModifiedData(double time, double newValue) throws CloneNotSupportedException {
		int timeIndex = this.getTimeIndex(time);

		double[] parameterOfCurve = getParameter();
		parameterOfCurve[timeIndex] = newValue;

		DiscountCurve newDiscountCurve = (DiscountCurve)getCloneForParameter(parameterOfCurve);

		return newDiscountCurve;

	}
}
