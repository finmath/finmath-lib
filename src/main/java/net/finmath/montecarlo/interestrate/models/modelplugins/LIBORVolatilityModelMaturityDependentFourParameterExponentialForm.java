/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 08.08.2005
 */
package net.finmath.montecarlo.interestrate.models.modelplugins;

import java.util.Arrays;

import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;

/**
 * @author Christian Fries
 * @version 1.0
 */
public class LIBORVolatilityModelMaturityDependentFourParameterExponentialForm extends LIBORVolatilityModel {

	private static final long serialVersionUID = 1412665163004646789L;

	private double[] a;
	private double[] b;
	private double[] c;
	private double[] d;

	/**
	 * @param timeDiscretizationFromArray The simulation time discretization t<sub>j</sub>.
	 * @param liborPeriodDiscretization The period time discretization T<sub>i</sub>.
	 * @param a The parameter a: an initial volatility level.
	 * @param b The parameter b: the slope at the short end (shortly before maturity).
	 * @param c The parameter c: exponential decay of the volatility in time-to-maturity.
	 * @param d The parameter d: if c &gt; 0 this is the very long term volatility level.
	 */
	public LIBORVolatilityModelMaturityDependentFourParameterExponentialForm(TimeDiscretization timeDiscretization, TimeDiscretization liborPeriodDiscretization, double a, double b, double c, double d) {
		super(timeDiscretization, liborPeriodDiscretization);
		this.a = new double[liborPeriodDiscretization.getNumberOfTimeSteps()];	Arrays.fill(this.a, a);
		this.b = new double[liborPeriodDiscretization.getNumberOfTimeSteps()];	Arrays.fill(this.b, b);
		this.c = new double[liborPeriodDiscretization.getNumberOfTimeSteps()];	Arrays.fill(this.c, c);
		this.d = new double[liborPeriodDiscretization.getNumberOfTimeSteps()];	Arrays.fill(this.d, d);
	}

	/**
	 * @param timeDiscretizationFromArray The simulation time discretization t<sub>j</sub>.
	 * @param liborPeriodDiscretization The period time discretization T<sub>i</sub>.
	 * @param a The parameter a: an initial volatility level.
	 * @param b The parameter b: the slope at the short end (shortly before maturity).
	 * @param c The parameter c: exponential decay of the volatility in time-to-maturity.
	 * @param d The parameter d: if c &gt; 0 this is the very long term volatility level.
	 */
	public LIBORVolatilityModelMaturityDependentFourParameterExponentialForm(TimeDiscretization timeDiscretization, TimeDiscretization liborPeriodDiscretization, double[] a, double[] b, double[] c, double[] d) {
		super(timeDiscretization, liborPeriodDiscretization);
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
	}

	@Override
	public double[] getParameter() {
		double[] parameter = new double[a.length+b.length+c.length+d.length];
		System.arraycopy(a, 0, parameter, 0, a.length);
		System.arraycopy(b, 0, parameter, a.length, b.length);
		System.arraycopy(c, 0, parameter, a.length+b.length, c.length);
		System.arraycopy(d, 0, parameter, a.length+b.length+c.length, d.length);

		return parameter;
	}

	@Override
	public LIBORVolatilityModelMaturityDependentFourParameterExponentialForm getCloneWithModifiedParameter(double[] parameter) {
		double[] parameterA = new double[a.length];
		double[] parameterB = new double[b.length];
		double[] parameterC = new double[c.length];
		double[] parameterD = new double[d.length];

		System.arraycopy(parameter, 0, parameterA, 0, a.length);
		System.arraycopy(parameter, a.length, parameterA, 0, b.length);
		System.arraycopy(parameter, a.length+b.length, parameterA, 0, c.length);
		System.arraycopy(parameter, a.length+b.length+c.length, parameterA, 0, d.length);

		return new LIBORVolatilityModelMaturityDependentFourParameterExponentialForm(
				super.getTimeDiscretization(),
				super.getLiborPeriodDiscretization(),
				parameterA,
				parameterB,
				parameterC,
				parameterD
				);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.models.modelplugins.LIBORVolatilityModel#getVolatility(int, int)
	 */
	@Override
	public RandomVariable getVolatility(int timeIndex, int liborIndex) {
		// Create a very simple volatility model here
		double time             = getTimeDiscretization().getTime(timeIndex);
		double maturity         = getLiborPeriodDiscretization().getTime(liborIndex);
		double timeToMaturity   = maturity-time;

		double volatilityInstanteaneous;
		if(timeToMaturity <= 0)
		{
			volatilityInstanteaneous = 0.0;   // This forward rate is already fixed, no volatility
		}
		else
		{
			volatilityInstanteaneous = (a[liborIndex] + b[liborIndex] * timeToMaturity) * Math.exp(-c[liborIndex] * timeToMaturity) + d[liborIndex];
		}
		if(volatilityInstanteaneous < 0.0) {
			volatilityInstanteaneous = Math.max(volatilityInstanteaneous,0.0);
		}

		return new RandomVariableFromDoubleArray(getTimeDiscretization().getTime(timeIndex),volatilityInstanteaneous);
	}

	@Override
	public Object clone() {
		return new LIBORVolatilityModelMaturityDependentFourParameterExponentialForm(
				super.getTimeDiscretization(),
				super.getLiborPeriodDiscretization(),
				a,
				b,
				c,
				d
				);
	}
}
