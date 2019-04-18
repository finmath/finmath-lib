/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 08.08.2005
 */
package net.finmath.montecarlo.interestrate.models.covariance;

import java.util.Map;

import net.finmath.marketdata.model.volatilities.CapletVolatilitiesParametric;
import net.finmath.marketdata.model.volatilities.VolatilitySurface.QuotingConvention;
import net.finmath.montecarlo.AbstractRandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
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
 * @version 1.0
 */
public class LIBORVolatilityModelFourParameterExponentialFormIntegrated extends LIBORVolatilityModel {

	private static final long serialVersionUID = -1613728266481870311L;

	private RandomVariable a;
	private RandomVariable b;
	private RandomVariable c;
	private RandomVariable d;

	private boolean isCalibrateable = false;

	private transient CapletVolatilitiesParametric cap;

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
	public LIBORVolatilityModelFourParameterExponentialFormIntegrated(TimeDiscretization timeDiscretization, TimeDiscretization liborPeriodDiscretization, RandomVariable a, RandomVariable b, RandomVariable c, RandomVariable d, boolean isCalibrateable) {
		super(timeDiscretization, liborPeriodDiscretization);
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
		this.isCalibrateable = isCalibrateable;
		cap = new CapletVolatilitiesParametric("", null, a.doubleValue(), b.doubleValue(), c.doubleValue(), d.doubleValue());
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
	public LIBORVolatilityModelFourParameterExponentialFormIntegrated(TimeDiscretization timeDiscretization, TimeDiscretization liborPeriodDiscretization, double a, double b, double c, double d, boolean isCalibrateable) {
		super(timeDiscretization, liborPeriodDiscretization);
		this.a = new Scalar(a);
		this.b = new Scalar(b);
		this.c = new Scalar(c);
		this.d = new Scalar(d);
		this.isCalibrateable = isCalibrateable;
		cap = new CapletVolatilitiesParametric("", null, a, b, c, d);
	}


	@Override
	public RandomVariable[] getParameter() {
		if(!isCalibrateable) {
			return null;
		}

		RandomVariable[] parameter = new RandomVariable[4];
		parameter[0] = a;
		parameter[1] = b;
		parameter[2] = c;
		parameter[3] = d;

		return parameter;
	}

	@Override
	public LIBORVolatilityModelFourParameterExponentialFormIntegrated getCloneWithModifiedParameter(RandomVariable[] parameter) {
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
	public RandomVariable getVolatility(int timeIndex, int liborIndex) {
		// Create a very simple volatility model here
		double timeStart		= getTimeDiscretization().getTime(timeIndex);
		double timeEnd			= getTimeDiscretization().getTime(timeIndex+1);
		double maturity			= getLiborPeriodDiscretization().getTime(liborIndex);

		double volStart	= cap.getValue(maturity-timeStart, 0, QuotingConvention.VOLATILITYLOGNORMAL);
		double volEnd	= cap.getValue(maturity-timeEnd, 0, QuotingConvention.VOLATILITYLOGNORMAL);

		double varianceInstantaneous = (
				volStart*volStart*(maturity-timeStart)
				-
				volEnd*volEnd*(maturity-timeEnd)
				)/(timeEnd-timeStart);

		varianceInstantaneous = Math.max(varianceInstantaneous, 0.0);

		return new RandomVariableFromDoubleArray(Math.sqrt(varianceInstantaneous));
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
	public LIBORVolatilityModel getCloneWithModifiedData(Map<String, Object> dataModified) {
		AbstractRandomVariableFactory randomVariableFactory = null;
		TimeDiscretization timeDiscretization = this.getTimeDiscretization();
		TimeDiscretization liborPeriodDiscretization = this.getLiborPeriodDiscretization();
		RandomVariable a = this.a;
		RandomVariable b = this.b;
		RandomVariable c = this.c;
		RandomVariable d = this.d;
		boolean isCalibrateable = this.isCalibrateable;

		if(dataModified != null) {
			// Explicitly passed covarianceModel has priority
			randomVariableFactory = (AbstractRandomVariableFactory)dataModified.getOrDefault("randomVariableFactory", randomVariableFactory);
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

		LIBORVolatilityModel newModel = new LIBORVolatilityModelFourParameterExponentialFormIntegrated(timeDiscretization, liborPeriodDiscretization, a, b, c, d, isCalibrateable);
		return newModel;
	}
}
