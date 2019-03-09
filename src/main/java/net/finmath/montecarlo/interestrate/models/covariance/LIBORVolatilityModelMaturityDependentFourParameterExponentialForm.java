/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 08.08.2005
 */
package net.finmath.montecarlo.interestrate.models.covariance;

import java.util.Arrays;
import java.util.Map;

import net.finmath.montecarlo.AbstractRandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
import net.finmath.time.TimeDiscretization;

/**
 * @author Christian Fries
 * @version 1.0
 */
public class LIBORVolatilityModelMaturityDependentFourParameterExponentialForm extends LIBORVolatilityModel {

	private static final long serialVersionUID = 1412665163004646789L;

	private final AbstractRandomVariableFactory	randomVariableFactory;

	private RandomVariable[] a;
	private RandomVariable[] b;
	private RandomVariable[] c;
	private RandomVariable[] d;

	public LIBORVolatilityModelMaturityDependentFourParameterExponentialForm(
			TimeDiscretization timeDiscretization,
			TimeDiscretization liborPeriodDiscretization,
			RandomVariable[] parameterA,
			RandomVariable[] parameterB,
			RandomVariable[] parameterC,
			RandomVariable[] parameterD) {
		super(timeDiscretization, liborPeriodDiscretization);
		this.randomVariableFactory = new RandomVariableFactory();
		a = parameterA;
		b = parameterB;
		c = parameterC;
		d = parameterD;
	}

	/**
	 * @param randomVariableFactory The random variable factor used to construct random variables from the parameters.
	 * @param timeDiscretization The simulation time discretization t<sub>j</sub>.
	 * @param liborPeriodDiscretization The period time discretization T<sub>i</sub>.
	 * @param a The parameter a: an initial volatility level.
	 * @param b The parameter b: the slope at the short end (shortly before maturity).
	 * @param c The parameter c: exponential decay of the volatility in time-to-maturity.
	 * @param d The parameter d: if c &gt; 0 this is the very long term volatility level.
	 */
	public LIBORVolatilityModelMaturityDependentFourParameterExponentialForm(
			AbstractRandomVariableFactory randomVariableFactory,
			TimeDiscretization timeDiscretization,
			TimeDiscretization liborPeriodDiscretization,
			double[] a, double[] b, double[] c, double[] d) {
		super(timeDiscretization, liborPeriodDiscretization);
		this.randomVariableFactory = randomVariableFactory;
		this.a = randomVariableFactory.createRandomVariableArray(a);
		this.b = randomVariableFactory.createRandomVariableArray(b);
		this.c = randomVariableFactory.createRandomVariableArray(c);
		this.d = randomVariableFactory.createRandomVariableArray(d);
	}

	/**
	 * @param randomVariableFactory The random variable factor used to construct random variables from the parameters.
	 * @param timeDiscretization The simulation time discretization t<sub>j</sub>.
	 * @param liborPeriodDiscretization The period time discretization T<sub>i</sub>.
	 * @param a The parameter a: an initial volatility level.
	 * @param b The parameter b: the slope at the short end (shortly before maturity).
	 * @param c The parameter c: exponential decay of the volatility in time-to-maturity.
	 * @param d The parameter d: if c &gt; 0 this is the very long term volatility level.
	 */
	public LIBORVolatilityModelMaturityDependentFourParameterExponentialForm(
			AbstractRandomVariableFactory randomVariableFactory,
			TimeDiscretization timeDiscretization,
			TimeDiscretization liborPeriodDiscretization,
			double a, double b, double c, double d) {
		super(timeDiscretization, liborPeriodDiscretization);
		this.randomVariableFactory = randomVariableFactory;
		this.a = new RandomVariable[liborPeriodDiscretization.getNumberOfTimeSteps()];	Arrays.fill(this.a, randomVariableFactory.createRandomVariable(a));
		this.b = new RandomVariable[liborPeriodDiscretization.getNumberOfTimeSteps()];	Arrays.fill(this.b, randomVariableFactory.createRandomVariable(b));
		this.c = new RandomVariable[liborPeriodDiscretization.getNumberOfTimeSteps()];	Arrays.fill(this.c, randomVariableFactory.createRandomVariable(c));
		this.d = new RandomVariable[liborPeriodDiscretization.getNumberOfTimeSteps()];	Arrays.fill(this.d, randomVariableFactory.createRandomVariable(d));
	}

	/**
	 * @param timeDiscretization The simulation time discretization t<sub>j</sub>.
	 * @param liborPeriodDiscretization The period time discretization T<sub>i</sub>.
	 * @param a The parameter a: an initial volatility level.
	 * @param b The parameter b: the slope at the short end (shortly before maturity).
	 * @param c The parameter c: exponential decay of the volatility in time-to-maturity.
	 * @param d The parameter d: if c &gt; 0 this is the very long term volatility level.
	 */
	public LIBORVolatilityModelMaturityDependentFourParameterExponentialForm(TimeDiscretization timeDiscretization,
			TimeDiscretization liborPeriodDiscretization, double a, double b, double c, double d) {
		this(new RandomVariableFactory(), timeDiscretization, liborPeriodDiscretization, a, b, c, d);
	}

	/**
	 * @param timeDiscretization The simulation time discretization t<sub>j</sub>.
	 * @param liborPeriodDiscretization The period time discretization T<sub>i</sub>.
	 * @param a The parameter a: an initial volatility level.
	 * @param b The parameter b: the slope at the short end (shortly before maturity).
	 * @param c The parameter c: exponential decay of the volatility in time-to-maturity.
	 * @param d The parameter d: if c &gt; 0 this is the very long term volatility level.
	 */
	public LIBORVolatilityModelMaturityDependentFourParameterExponentialForm(TimeDiscretization timeDiscretization,
			TimeDiscretization liborPeriodDiscretization, double[] a, double[] b, double[] c, double[] d) {
		this(new RandomVariableFactory(), timeDiscretization, liborPeriodDiscretization, a, b, c, d);
	}

	@Override
	public RandomVariable[] getParameter() {
		RandomVariable[] parameter = new RandomVariable[a.length+b.length+c.length+d.length];
		System.arraycopy(a, 0, parameter, 0, a.length);
		System.arraycopy(b, 0, parameter, a.length, b.length);
		System.arraycopy(c, 0, parameter, a.length+b.length, c.length);
		System.arraycopy(d, 0, parameter, a.length+b.length+c.length, d.length);

		return parameter;
	}

	@Override
	public LIBORVolatilityModelMaturityDependentFourParameterExponentialForm getCloneWithModifiedParameter(RandomVariable[] parameter) {
		RandomVariable[] parameterA = new RandomVariable[a.length];
		RandomVariable[] parameterB = new RandomVariable[b.length];
		RandomVariable[] parameterC = new RandomVariable[c.length];
		RandomVariable[] parameterD = new RandomVariable[d.length];

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
	 * @see net.finmath.montecarlo.interestrate.models.covariance.LIBORVolatilityModel#getVolatility(int, int)
	 */
	@Override
	public RandomVariable getVolatility(int timeIndex, int liborIndex) {
		// Create a very simple volatility model here
		double time             = getTimeDiscretization().getTime(timeIndex);
		double maturity         = getLiborPeriodDiscretization().getTime(liborIndex);
		double timeToMaturity   = maturity-time;

		RandomVariable volatilityInstanteaneous;
		if(timeToMaturity <= 0)
		{
			volatilityInstanteaneous = new Scalar(0.0);   // This forward rate is already fixed, no volatility
		}
		else
		{
			volatilityInstanteaneous = a[liborIndex].addProduct(b[liborIndex], timeToMaturity).mult(c[liborIndex].mult(-timeToMaturity).exp()).add(d[liborIndex]);
		}

		return volatilityInstanteaneous;
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

	@Override
	public LIBORVolatilityModel getCloneWithModifiedData(Map<String, Object> dataModified) {
		AbstractRandomVariableFactory randomVariableFactory = this.randomVariableFactory;
		TimeDiscretization timeDiscretization = this.getTimeDiscretization();
		TimeDiscretization liborPeriodDiscretization = this.getLiborPeriodDiscretization();
		double[] a = Arrays.stream(this.a).mapToDouble(x -> x.doubleValue()).toArray();
		double[] b = Arrays.stream(this.b).mapToDouble(x -> x.doubleValue()).toArray();
		double[] c = Arrays.stream(this.c).mapToDouble(x -> x.doubleValue()).toArray();
		double[] d = Arrays.stream(this.d).mapToDouble(x -> x.doubleValue()).toArray();
		
		if(dataModified != null) {
			// Explicitly passed covarianceModel has priority
			randomVariableFactory = (AbstractRandomVariableFactory)dataModified.getOrDefault("randomVariableFactory", randomVariableFactory);
			timeDiscretization = (TimeDiscretization)dataModified.getOrDefault("timeDiscretization", timeDiscretization);
			liborPeriodDiscretization = (TimeDiscretization)dataModified.getOrDefault("liborPeriodDiscretization", liborPeriodDiscretization);

			
			if(dataModified.getOrDefault("a", a) instanceof RandomVariable[]) {
				a = Arrays.stream((RandomVariable[])dataModified.getOrDefault("a", a)).mapToDouble(param -> param.doubleValue()).toArray();
			}else {
				a = (double[])dataModified.get("a");
			}
			if(dataModified.getOrDefault("b", b) instanceof RandomVariable[]) {
				b = Arrays.stream((RandomVariable[])dataModified.getOrDefault("b", b)).mapToDouble(param -> param.doubleValue()).toArray();
			}else {
				b = (double[])dataModified.get("b");
			}
			if(dataModified.getOrDefault("c", c) instanceof RandomVariable[]) {
				c = Arrays.stream((RandomVariable[])dataModified.getOrDefault("c", c)).mapToDouble(param -> param.doubleValue()).toArray();
			}else {
				c = (double[])dataModified.get("c");
			}
			if(dataModified.getOrDefault("d", d) instanceof RandomVariable[]) {
				d = Arrays.stream((RandomVariable[])dataModified.getOrDefault("d", d)).mapToDouble(param -> param.doubleValue()).toArray();
			}else {
				d = (double[])dataModified.get("d");
			}
		}
		
		LIBORVolatilityModel newModel = new LIBORVolatilityModelMaturityDependentFourParameterExponentialForm(randomVariableFactory, timeDiscretization, liborPeriodDiscretization, a, b, c, d);
		return newModel;
	}
}
