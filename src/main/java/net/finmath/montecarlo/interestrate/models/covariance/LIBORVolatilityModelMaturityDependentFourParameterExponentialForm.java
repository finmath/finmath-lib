/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 08.08.2005
 */
package net.finmath.montecarlo.interestrate.models.covariance;

import java.util.Arrays;
import java.util.Map;
import java.util.function.ToDoubleFunction;

import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFromArrayFactory;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
import net.finmath.time.TimeDiscretization;

/**
 * @author Christian Fries
 * @version 1.0
 */
public class LIBORVolatilityModelMaturityDependentFourParameterExponentialForm extends LIBORVolatilityModel {

	private static final long serialVersionUID = 1412665163004646789L;

	private final RandomVariableFactory	abstractRandomVariableFactory;

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
		abstractRandomVariableFactory = new RandomVariableFromArrayFactory();
		a = parameterA;
		b = parameterB;
		c = parameterC;
		d = parameterD;
	}

	/**
	 * @param abstractRandomVariableFactory The random variable factor used to construct random variables from the parameters.
	 * @param timeDiscretization The simulation time discretization t<sub>j</sub>.
	 * @param liborPeriodDiscretization The period time discretization T<sub>i</sub>.
	 * @param a The parameter a: an initial volatility level.
	 * @param b The parameter b: the slope at the short end (shortly before maturity).
	 * @param c The parameter c: exponential decay of the volatility in time-to-maturity.
	 * @param d The parameter d: if c &gt; 0 this is the very long term volatility level.
	 */
	public LIBORVolatilityModelMaturityDependentFourParameterExponentialForm(
			RandomVariableFactory abstractRandomVariableFactory,
			TimeDiscretization timeDiscretization,
			TimeDiscretization liborPeriodDiscretization,
			double[] a, double[] b, double[] c, double[] d) {
		super(timeDiscretization, liborPeriodDiscretization);
		this.abstractRandomVariableFactory = abstractRandomVariableFactory;
		this.a = abstractRandomVariableFactory.createRandomVariableArray(a);
		this.b = abstractRandomVariableFactory.createRandomVariableArray(b);
		this.c = abstractRandomVariableFactory.createRandomVariableArray(c);
		this.d = abstractRandomVariableFactory.createRandomVariableArray(d);
	}

	/**
	 * @param abstractRandomVariableFactory The random variable factor used to construct random variables from the parameters.
	 * @param timeDiscretization The simulation time discretization t<sub>j</sub>.
	 * @param liborPeriodDiscretization The period time discretization T<sub>i</sub>.
	 * @param a The parameter a: an initial volatility level.
	 * @param b The parameter b: the slope at the short end (shortly before maturity).
	 * @param c The parameter c: exponential decay of the volatility in time-to-maturity.
	 * @param d The parameter d: if c &gt; 0 this is the very long term volatility level.
	 */
	public LIBORVolatilityModelMaturityDependentFourParameterExponentialForm(
			RandomVariableFactory abstractRandomVariableFactory,
			TimeDiscretization timeDiscretization,
			TimeDiscretization liborPeriodDiscretization,
			double a, double b, double c, double d) {
		super(timeDiscretization, liborPeriodDiscretization);
		this.abstractRandomVariableFactory = abstractRandomVariableFactory;
		this.a = new RandomVariable[liborPeriodDiscretization.getNumberOfTimeSteps()];	Arrays.fill(this.a, abstractRandomVariableFactory.createRandomVariable(a));
		this.b = new RandomVariable[liborPeriodDiscretization.getNumberOfTimeSteps()];	Arrays.fill(this.b, abstractRandomVariableFactory.createRandomVariable(b));
		this.c = new RandomVariable[liborPeriodDiscretization.getNumberOfTimeSteps()];	Arrays.fill(this.c, abstractRandomVariableFactory.createRandomVariable(c));
		this.d = new RandomVariable[liborPeriodDiscretization.getNumberOfTimeSteps()];	Arrays.fill(this.d, abstractRandomVariableFactory.createRandomVariable(d));
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
		this(new RandomVariableFromArrayFactory(), timeDiscretization, liborPeriodDiscretization, a, b, c, d);
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
		this(new RandomVariableFromArrayFactory(), timeDiscretization, liborPeriodDiscretization, a, b, c, d);
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
		RandomVariableFactory abstractRandomVariableFactory = this.abstractRandomVariableFactory;
		TimeDiscretization timeDiscretization = this.getTimeDiscretization();
		TimeDiscretization liborPeriodDiscretization = this.getLiborPeriodDiscretization();
		double[] a = Arrays.stream(this.a).mapToDouble(new ToDoubleFunction<RandomVariable>() {
			@Override
			public double applyAsDouble(RandomVariable x) {
				return x.doubleValue();
			}
		}).toArray();
		double[] b = Arrays.stream(this.b).mapToDouble(new ToDoubleFunction<RandomVariable>() {
			@Override
			public double applyAsDouble(RandomVariable x) {
				return x.doubleValue();
			}
		}).toArray();
		double[] c = Arrays.stream(this.c).mapToDouble(new ToDoubleFunction<RandomVariable>() {
			@Override
			public double applyAsDouble(RandomVariable x) {
				return x.doubleValue();
			}
		}).toArray();
		double[] d = Arrays.stream(this.d).mapToDouble(new ToDoubleFunction<RandomVariable>() {
			@Override
			public double applyAsDouble(RandomVariable x) {
				return x.doubleValue();
			}
		}).toArray();

		if(dataModified != null) {
			// Explicitly passed covarianceModel has priority
			abstractRandomVariableFactory = (RandomVariableFactory)dataModified.getOrDefault("randomVariableFactory", abstractRandomVariableFactory);
			timeDiscretization = (TimeDiscretization)dataModified.getOrDefault("timeDiscretization", timeDiscretization);
			liborPeriodDiscretization = (TimeDiscretization)dataModified.getOrDefault("liborPeriodDiscretization", liborPeriodDiscretization);


			if(dataModified.getOrDefault("a", a) instanceof RandomVariable[]) {
				a = Arrays.stream((RandomVariable[])dataModified.getOrDefault("a", a)).mapToDouble(new ToDoubleFunction<RandomVariable>() {
					@Override
					public double applyAsDouble(RandomVariable param) {
						return param.doubleValue();
					}
				}).toArray();
			}else {
				a = (double[])dataModified.get("a");
			}
			if(dataModified.getOrDefault("b", b) instanceof RandomVariable[]) {
				b = Arrays.stream((RandomVariable[])dataModified.getOrDefault("b", b)).mapToDouble(new ToDoubleFunction<RandomVariable>() {
					@Override
					public double applyAsDouble(RandomVariable param) {
						return param.doubleValue();
					}
				}).toArray();
			}else {
				b = (double[])dataModified.get("b");
			}
			if(dataModified.getOrDefault("c", c) instanceof RandomVariable[]) {
				c = Arrays.stream((RandomVariable[])dataModified.getOrDefault("c", c)).mapToDouble(new ToDoubleFunction<RandomVariable>() {
					@Override
					public double applyAsDouble(RandomVariable param) {
						return param.doubleValue();
					}
				}).toArray();
			}else {
				c = (double[])dataModified.get("c");
			}
			if(dataModified.getOrDefault("d", d) instanceof RandomVariable[]) {
				d = Arrays.stream((RandomVariable[])dataModified.getOrDefault("d", d)).mapToDouble(new ToDoubleFunction<RandomVariable>() {
					@Override
					public double applyAsDouble(RandomVariable param) {
						return param.doubleValue();
					}
				}).toArray();
			}else {
				d = (double[])dataModified.get("d");
			}
		}

		LIBORVolatilityModel newModel = new LIBORVolatilityModelMaturityDependentFourParameterExponentialForm(abstractRandomVariableFactory, timeDiscretization, liborPeriodDiscretization, a, b, c, d);
		return newModel;
	}
}
