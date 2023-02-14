/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 21.07.2019
 */
package net.finmath.montecarlo.interestrate.models.covariance;

import java.util.Map;

import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
import net.finmath.time.TimeDiscretization;

/**
 * Implements the volatility model
 * \[
 * 	\sigma_{i}(t_{j}) = \sqrt{ \frac{1}{t_{j+1}-t_{j}} \int_{t_{j}}^{t_{j+1}} \left( ( a + b (T_{i}-t) ) exp(-c (T_{i}-t)) + d \right)^{2} \ \mathrm{d}t } \text{.}
 * \]
 *
 * The parameters here have some interpretation:
 * <ul>
 * <li>The parameter a: an initial volatility level.</li>
 * <li>The parameter b: the slope at the short end (shortly before maturity).</li>
 * <li>The parameter c: exponential decay of the volatility in time-to-maturity.</li>
 * <li>The parameter d: if c &gt; 0 this is the very long term volatility level.</li>
 * </ul>
 *
 * Note that this model results in a terminal (Black 76) volatility which is given
 * by
 * \[
 * 	\left( \sigma^{\text{Black}}_{i}(t_{k}) \right)^2 = \frac{1}{t_{k} \int_{0}^{t_{k}} \left( ( a + b (T_{i}-t) ) exp(-c (T_{i}-t)) + d \right)^{2} \ \mathrm{d}t \text{.}
 * \]
 *
 * @author Christian Fries
 * @version 1.1
 */
public class LIBORVolatilityModelFourParameterExponentialFormIntegrated extends LIBORVolatilityModel {

	private static final long serialVersionUID = -1613728266481870311L;

	private final double[] coeffTaylorE1 = new double[] { 1, 1.0/2.0, 1.0/6.0, 1.0/24.0, 1.0/120.0 };
	private final double[] coeffTaylorE2 = new double[] { 1, 2.0/3.0, 1.0/4.0, 1.0/15.0, 1.0/72.0 };
	private final double[] coeffTaylorE3 = new double[] { 1, 3.0/4.0, 3.0/10.0, 1.0/12.0, 1.0/56.0 };

	private final double[] coeffTaylorE17 = new double[] { 1, 1.0/2.0, 1.0/6.0, 1.0/24.0, 1.0/120.0, 1.0/720.0, 1.0/5040.0 };
	private final double[] coeffTaylorE27 = new double[] { 1, 2.0/3.0, 1.0/4.0, 1.0/15.0, 1.0/72.0, 1.0/420.0, 1.0/2880.0 };
	private final double[] coeffTaylorE37 = new double[] { 1, 3.0/4.0, 3.0/10.0, 1.0/12.0, 1.0/56.0, 1.0/320.0, 1.0/2160.0 };

	private RandomVariableFactory randomVariableFactory;

	private final RandomVariable a;
	private final RandomVariable b;
	private final RandomVariable c;
	private final RandomVariable d;

	private boolean isCalibrateable = false;

	/**
	 * Creates the volatility model
	 * \[
	 * 	\sigma_{i}(t_{j}) = \sqrt{ \frac{1}{t_{j+1}-t_{j}} \int_{t_{j}}^{t_{j+1}} \left( ( a + b (T_{i}-t) ) \exp(-c (T_{i}-t)) + d \right)^{2} \ \mathrm{d}t } \text{.}
	 * \]
	 *
	 * @param randomVariableFactory The random variable factor used to construct random variables from the parameters.
	 * @param timeDiscretization The simulation time discretization t<sub>j</sub>.
	 * @param liborPeriodDiscretization The period time discretization T<sub>i</sub>.
	 * @param a The parameter a: an initial volatility level.
	 * @param b The parameter b: the slope at the short end (shortly before maturity).
	 * @param c The parameter c: exponential decay of the volatility in time-to-maturity.
	 * @param d The parameter d: if c &gt; 0 this is the very long term volatility level.
	 * @param isCalibrateable Set this to true, if the parameters are available for calibration.
	 */
	public LIBORVolatilityModelFourParameterExponentialFormIntegrated(final RandomVariableFactory randomVariableFactory, final TimeDiscretization timeDiscretization, final TimeDiscretization liborPeriodDiscretization, final double a, final double b, final double c, final double d, final boolean isCalibrateable) {
		super(timeDiscretization, liborPeriodDiscretization);
		this.randomVariableFactory = randomVariableFactory;
		this.a = randomVariableFactory.createRandomVariable(a);
		this.b = randomVariableFactory.createRandomVariable(b);
		this.c = randomVariableFactory.createRandomVariable(c);
		this.d = randomVariableFactory.createRandomVariable(d);
		this.isCalibrateable = isCalibrateable;
	}

	/**
	 * Creates the volatility model
	 * \[
	 * 	\sigma_{i}(t_{j}) = \sqrt{ \frac{1}{t_{j+1}-t_{j}} \int_{t_{j}}^{t_{j+1}} \left( ( a + b (T_{i}-t) ) \exp(-c (T_{i}-t)) + d \right)^{2} \ \mathrm{d}t } \text{.}
	 * \]
	 *
	 * @param timeDiscretization The simulation time discretization t<sub>j</sub>.
	 * @param liborPeriodDiscretization The period time discretization T<sub>i</sub>.
	 * @param a The parameter a: an initial volatility level.
	 * @param b The parameter b: the slope at the short end (shortly before maturity).
	 * @param c The parameter c: exponential decay of the volatility in time-to-maturity.
	 * @param d The parameter d: if c &gt; 0 this is the very long term volatility level.
	 * @param isCalibrateable Set this to true, if the parameters are available for calibration.
	 */
	public LIBORVolatilityModelFourParameterExponentialFormIntegrated(final TimeDiscretization timeDiscretization, final TimeDiscretization liborPeriodDiscretization, final RandomVariable a, final RandomVariable b, final RandomVariable c, final RandomVariable d, final boolean isCalibrateable) {
		super(timeDiscretization, liborPeriodDiscretization);
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
		this.isCalibrateable = isCalibrateable;
	}

	/**
	 * Creates the volatility model
	 * \[
	 * 	\sigma_{i}(t_{j}) = \sqrt{ \frac{1}{t_{j+1}-t_{j}} \int_{t_{j}}^{t_{j+1}} \left( ( a + b (T_{i}-t) ) \exp(-c (T_{i}-t)) + d \right)^{2} \ \mathrm{d}t } \text{.}
	 * \]
	 *
	 * @param timeDiscretization The simulation time discretization t<sub>j</sub>.
	 * @param liborPeriodDiscretization The period time discretization T<sub>i</sub>.
	 * @param a The parameter a: an initial volatility level.
	 * @param b The parameter b: the slope at the short end (shortly before maturity).
	 * @param c The parameter c: exponential decay of the volatility in time-to-maturity.
	 * @param d The parameter d: if c &gt; 0 this is the very long term volatility level.
	 * @param isCalibrateable Set this to true, if the parameters are available for calibration.
	 */
	public LIBORVolatilityModelFourParameterExponentialFormIntegrated(final TimeDiscretization timeDiscretization, final TimeDiscretization liborPeriodDiscretization, final double a, final double b, final double c, final double d, final boolean isCalibrateable) {
		super(timeDiscretization, liborPeriodDiscretization);
		this.a = new Scalar(a);
		this.b = new Scalar(b);
		this.c = new Scalar(c);
		this.d = new Scalar(d);
		this.isCalibrateable = isCalibrateable;
	}


	@Override
	public RandomVariable[] getParameter() {
		if(!isCalibrateable) {
			return null;
		}

		final RandomVariable[] parameter = new RandomVariable[4];
		parameter[0] = a;
		parameter[1] = b;
		parameter[2] = c;
		parameter[3] = d;

		return parameter;
	}

	@Override
	public LIBORVolatilityModelFourParameterExponentialFormIntegrated getCloneWithModifiedParameter(final RandomVariable[] parameter) {
		if(!isCalibrateable) {
			return this;
		}

		return new LIBORVolatilityModelFourParameterExponentialFormIntegrated(
				super.getTimeDiscretization(),
				super.getLiborPeriodDiscretization(),
				parameter[0],
				parameter[1],
				parameter[2],
				parameter[3],
				isCalibrateable
				);
	}

	@Override
	public RandomVariable getVolatility(final int timeIndex, final int liborIndex) {
		// Create a very simple volatility model here
		final double timeStart		= getTimeDiscretization().getTime(timeIndex);
		final double timeEnd			= getTimeDiscretization().getTime(timeIndex+1);
		final double maturity			= getLiborPeriodDiscretization().getTime(liborIndex);

		if(maturity-timeStart <= 0) {
			return new Scalar(0.0);
		}

		final RandomVariable varianceInstantaneous = getIntegratedVariance(maturity-timeStart).sub(getIntegratedVariance(maturity-timeEnd)).div(timeEnd-timeStart);

		return varianceInstantaneous.sqrt();
	}

	private RandomVariable getIntegratedVariance(final double maturity) {

		if(maturity == 0) {
			return new Scalar(0.0);
		}

		/*
		 * Integral of the square of the instantaneous volatility function
		 * ((a + b * T) * Math.exp(- c * T) + d);
		 */

		/*
		 * http://www.wolframalpha.com/input/?i=integrate+%28%28a+%2B+b+*+t%29+*+exp%28-+c+*+t%29+%2B+d%29%5E2+from+0+to+T
		 * integral_0^T ((a+b t) exp(-(c t))+d)^2  dt = 1/4 ((e^(-2 c T) (-2 a^2 c^2-2 a b c (2 c T+1)+b^2 (-(2 c T (c T+1)+1))))/c^3+(2 a^2 c^2+2 a b c+b^2)/c^3-(8 d e^(-c T) (a c+b c T+b))/c^2+(8 d (a c+b))/c^2+4 d^2 T)
		 */
		final RandomVariable aaT = a.squared().mult(maturity);
		final RandomVariable abTT = a.mult(b).mult(maturity*maturity);
		final RandomVariable ad2T = a.mult(d).mult(2.0*maturity);
		final RandomVariable bbTTT = b.squared().mult(maturity*maturity*maturity/3.0);
		final RandomVariable bdTT = b.mult(d).mult(maturity*maturity);
		final RandomVariable ddT = d.squared().mult(maturity);

		final RandomVariable mcT = c.mult(-maturity);
		final RandomVariable mcT2 = mcT.mult(2.0);

		RandomVariable expA1 = mcT.expm1().div(mcT);
		RandomVariable expA2 = mcT.sub(expA1.log()).expm1().div(mcT).mult(expA1).mult(2.0);

		RandomVariable expB1 = mcT2.expm1().div(mcT2);
		RandomVariable expB2 = mcT2.sub(expB1.log()).expm1().div(mcT2).mult(expB1).mult(2.0);
		RandomVariable expB3 = mcT2.sub(expB2.log()).expm1().div(mcT2).mult(expB2).mult(3.0);

		// Ensure that c is cut off from 0 (the term (exp(-x)-1)/x will have cancelations)

		// 1 1/2 1/6  1/24  1/120 1/720 1/5040
		// 1 2/3 1/4  1/15  1/72  1/420 1/2880
		// 1 3/4 3/10 1/12	1/56  1/320 1/2160

		final RandomVariable pA1 = polynom(mcT, coeffTaylorE1);
		final RandomVariable pA2 = polynom(mcT, coeffTaylorE2);

		final RandomVariable pB1 = polynom(mcT2, coeffTaylorE1);
		final RandomVariable pB2 = polynom(mcT2, coeffTaylorE2);
		final RandomVariable pB3 = polynom(mcT2, coeffTaylorE3);

		final RandomVariable cCutOff1 = mcT.abs().sub(1E-12).choose(new Scalar(1.0), new Scalar(-1.0));
		final RandomVariable cCutOff2 = mcT.abs().sub(1E-2).choose(new Scalar(1.0), new Scalar(-1.0));
		final RandomVariable cCutOff3 = cCutOff2;

		expA1 = cCutOff1.choose(expA1, pA1);
		expA2 = cCutOff2.choose(expA2, pA2);
		expB1 = cCutOff1.choose(expB1, pB1);
		expB2 = cCutOff2.choose(expB2, pB2);
		expB3 = cCutOff3.choose(expB3, pB3);

		/*
			integratedVariance = a*a*T*((1-Math.exp(-2*c*T))/(2*c*T))
					+ a*b*T*T*(((1 - Math.exp(-2*c*T))/(2*c*T) - Math.exp(-2*c*T))/(c*T))
					+ 2*a*d*T*((1-Math.exp(-c*T))/(c*T))
					+ b*b*T*T*T*(((((1-Math.exp(-2*c*T))/(2*c*T)-Math.exp(-2*c*T))/(T*c)-Math.exp(-2*c*T)))/(2*c*T))
					+ 2*b*d*T*T*(((1-Math.exp(-c*T))-T*c*Math.exp(-c*T))/(c*c*T*T))
					+ d*d*T;
		 */

		RandomVariable integratedVariance = aaT.mult(expB1);
		integratedVariance = integratedVariance.add( abTT.mult(expB2) );
		integratedVariance = integratedVariance.add( ad2T.mult(expA1) );
		integratedVariance = integratedVariance.add( bbTTT.mult(expB3) );
		integratedVariance = integratedVariance.add( bdTT.mult(expA2) );
		integratedVariance = integratedVariance.add( ddT );

		return integratedVariance;
	}

	private RandomVariable polynom(final RandomVariable x, final double[] coeff) {
		RandomVariable p = x.mult(coeff[coeff.length-1]).add(coeff[coeff.length-2]);

		for(int i=coeff.length-3; i >= 0; i--) {
			p = p.mult(x).add(coeff[i]);
		}
		return p;
	}

	@Override
	public Object clone() {
		return new LIBORVolatilityModelFourParameterExponentialFormIntegrated(
				super.getTimeDiscretization(),
				super.getLiborPeriodDiscretization(),
				a,
				b,
				c,
				d,
				isCalibrateable
				);
	}

	@Override
	public LIBORVolatilityModel getCloneWithModifiedData(final Map<String, Object> dataModified) {
		RandomVariableFactory randomVariableFactory = null;
		TimeDiscretization timeDiscretization = this.getTimeDiscretization();
		TimeDiscretization liborPeriodDiscretization = this.getLiborPeriodDiscretization();
		RandomVariable a = this.a;
		RandomVariable b = this.b;
		RandomVariable c = this.c;
		RandomVariable d = this.d;
		boolean isCalibrateable = this.isCalibrateable;

		if(dataModified != null) {
			// Explicitly passed covarianceModel has priority
			randomVariableFactory = (RandomVariableFactory)dataModified.getOrDefault("randomVariableFactory", randomVariableFactory);
			timeDiscretization = (TimeDiscretization)dataModified.getOrDefault("timeDiscretization", timeDiscretization);
			liborPeriodDiscretization = (TimeDiscretization)dataModified.getOrDefault("liborPeriodDiscretization", liborPeriodDiscretization);
			isCalibrateable = (boolean)dataModified.getOrDefault("isCalibrateable", isCalibrateable);

			if(dataModified.containsKey("randomVariableFactory")) {
				a = randomVariableFactory.createRandomVariable(a.doubleValue());
				b = randomVariableFactory.createRandomVariable(b.doubleValue());
				c = randomVariableFactory.createRandomVariable(c.doubleValue());
				d = randomVariableFactory.createRandomVariable(d.doubleValue());
			}

			if(dataModified.getOrDefault("a", a) instanceof RandomVariable) {
				a = ((RandomVariable)dataModified.getOrDefault("a", a));
			}else if(randomVariableFactory != null){
				a = randomVariableFactory.createRandomVariable((double)dataModified.get("a"));
			}else {
				a = new Scalar((double)dataModified.get("a"));
			}
			if(dataModified.getOrDefault("b", b) instanceof RandomVariable) {
				b = randomVariableFactory.createRandomVariable(((RandomVariable)dataModified.getOrDefault("b", b)).doubleValue());
			}else if(randomVariableFactory != null){
				b = randomVariableFactory.createRandomVariable((double)dataModified.get("b"));
			}else {
				b = new Scalar((double)dataModified.get("b"));
			}
			if(dataModified.getOrDefault("c", c) instanceof RandomVariable) {
				c = randomVariableFactory.createRandomVariable(((RandomVariable)dataModified.getOrDefault("c", c)).doubleValue());
			}else if(randomVariableFactory != null){
				c = randomVariableFactory.createRandomVariable((double)dataModified.get("c"));
			}else {
				c = new Scalar((double)dataModified.get("c"));
			}
			if(dataModified.getOrDefault("d", d) instanceof RandomVariable) {
				d = randomVariableFactory.createRandomVariable(((RandomVariable)dataModified.getOrDefault("d", d)).doubleValue());
			}else if(randomVariableFactory != null){
				d = randomVariableFactory.createRandomVariable((double)dataModified.get("d"));
			}else {
				d = new Scalar((double)dataModified.get("d"));
			}
		}

		final LIBORVolatilityModel newModel = new LIBORVolatilityModelFourParameterExponentialFormIntegrated(timeDiscretization, liborPeriodDiscretization, a, b, c, d, isCalibrateable);
		return newModel;
	}
}
