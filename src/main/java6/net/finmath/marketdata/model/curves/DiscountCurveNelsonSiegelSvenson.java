/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2005
 */
package net.finmath.marketdata.model.curves;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Calendar;

import org.apache.commons.math3.util.FastMath;

import net.finmath.marketdata.model.AnalyticModelInterface;

/**
 * Implementation of a discount factor curve given by a Nelson-Siegel-Svensson (NSS) parameterization.
 * In the NSS parameterization the zero rate \( r(T) \) is given by
 * 
 * \[ r(T) = \beta_0 + \beta_1 \frac{1-x_0}{T/\tau_0} + \beta_2 ( \frac{1-x_0}{T/\tau_0} - x_0) + \beta_3 ( \frac{1-x_2}{T/\tau_1} - x_1) \]
 * 
 * where \( x_0 = \exp(-T/\tau_0) \) and \( x_1 = \exp(-T/\tau_1) \).
 * 
 * The sub-family of curve with \( \beta_3 = 0 \) is called Nelson-Siegel parameterization.
 * 
 * @author Christian Fries
 */
public class DiscountCurveNelsonSiegelSvenson extends AbstractCurve implements Serializable, DiscountCurveInterface {

	private static final long serialVersionUID = 8024640795839972709L;

	private final double	timeScaling;
	private final double[]	parameter;

	public DiscountCurveNelsonSiegelSvenson(String name, Calendar referenceDate, double[] parameter, double timeScaling) {
		super(name, referenceDate);
		this.timeScaling = timeScaling;

		this.parameter = parameter.clone();
	}

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
		// Change time scale
		maturity *= timeScaling;
		
		double beta1	= parameter[0];
		double beta2	= parameter[1];
		double beta3	= parameter[2];
		double beta4	= parameter[3];
		double tau1		= parameter[4];
		double tau2		= parameter[5];

		double x1 = tau1 > 0 ? FastMath.exp(-maturity/tau1) : 0.0;
		double x2 = tau2 > 0 ? FastMath.exp(-maturity/tau2) : 0.0;

		double y1 = tau1 > 0 ? (maturity > 0.0 ? (1.0-x1)/maturity*tau1 : 1.0) : 0.0;
		double y2 = tau1 > 0 ? (maturity > 0.0 ? (1.0-x2)/maturity*tau2 : 1.0) : 0.0;

		double zeroRate = beta1 + beta2 * y1 + beta3 * (y1-x1) + beta4 * (y2-x2);

		return Math.exp(- zeroRate * maturity);
	}

	@Override
	public double getValue(AnalyticModelInterface model, double time) {
		return getDiscountFactor(model, time);
	}

	@Override
	public CurveBuilderInterface getCloneBuilder() throws CloneNotSupportedException {
		return null;
	}

	@Override
	public double[] getParameter() {
		return parameter;
	}

	@Override
	@Deprecated
	public void setParameter(double[] parameter) {
		throw new UnsupportedOperationException();

	}

	public double getTimeScaling() {
		return timeScaling;
	}

	@Override
	public DiscountCurveNelsonSiegelSvenson clone() throws CloneNotSupportedException {
		return (DiscountCurveNelsonSiegelSvenson)super.clone();
	}

	@Override
	public DiscountCurveNelsonSiegelSvenson getCloneForParameter(double[] value) throws CloneNotSupportedException {
		return new DiscountCurveNelsonSiegelSvenson(getName(), getReferenceDate(), value, timeScaling);
	}

	@Override
	public String toString() {
		return "DiscountCurveNelsonSiegelSvenson [timeScaling=" + timeScaling
				+ ", parameter=" + Arrays.toString(parameter) + ", toString()="
				+ super.toString() + "]";
	}
}
