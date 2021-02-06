/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 08.08.2005
 */
package net.finmath.montecarlo.interestrate.models.covariance;

import java.util.Map;

import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFromArrayFactory;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;

/**
 * Implements the volatility model
 * \[
 * 	\sigma_{i}(t_{j}) = ( a + b (T_{i}-t_{j}) ) exp(-c (T_{i}-t_{j})) + d \text{.}
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
 * 	\left( \sigma^{\text{Black}}_{i}(t_{k}) \right)^2 = \frac{1}{t_{k}} \sum_{j=0}^{k-1} \left( ( a + b (T_{i}-t_{j}) ) exp(-c (T_{i}-t_{j})) + d \right)^{2} (t_{j+1}-t_{j})
 * \]
 * i.e., the instantaneous volatility is given by the picewise constant approximation of the function
 * \[
 * 	\sigma_{i}(t) = ( a + b (T_{i}-t) ) exp(-c (T_{i}-t)) + d
 * \]
 * on the time discretization \( \{ t_{j} \} \). For the exact integration of this formula see {@link LIBORVolatilityModelFourParameterExponentialFormIntegrated}.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class LIBORVolatilityModelFourParameterExponentialForm extends LIBORVolatilityModel {

	private static final long serialVersionUID = -7371483471144264848L;

	private final RandomVariableFactory	randomVariableFactory;

	private final RandomVariable a;
	private final RandomVariable b;
	private final RandomVariable c;
	private final RandomVariable d;

	private boolean isCalibrateable = false;

	// A lazy init cache
	private transient RandomVariable[][] volatility;
	private transient Object volatilityLazyInitLock = new Object();

	/**
	 * Creates the volatility model &sigma;<sub>i</sub>(t<sub>j</sub>) = ( a + b * (T<sub>i</sub>-t<sub>j</sub>) ) * exp(-c (T<sub>i</sub>-t<sub>j</sub>)) + d
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
	public LIBORVolatilityModelFourParameterExponentialForm(final RandomVariableFactory randomVariableFactory, final TimeDiscretization timeDiscretization, final TimeDiscretization liborPeriodDiscretization, final RandomVariable a, final RandomVariable b, final RandomVariable c, final RandomVariable d, final boolean isCalibrateable) {
		super(timeDiscretization, liborPeriodDiscretization);
		this.randomVariableFactory = randomVariableFactory;
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
		this.isCalibrateable = isCalibrateable;
	}

	/**
	 * Creates the volatility model &sigma;<sub>i</sub>(t<sub>j</sub>) = ( a + b * (T<sub>i</sub>-t<sub>j</sub>) ) * exp(-c (T<sub>i</sub>-t<sub>j</sub>)) + d
	 *
	 * @param timeDiscretization The simulation time discretization t<sub>j</sub>.
	 * @param liborPeriodDiscretization The period time discretization T<sub>i</sub>.
	 * @param a The parameter a: an initial volatility level.
	 * @param b The parameter b: the slope at the short end (shortly before maturity).
	 * @param c The parameter c: exponential decay of the volatility in time-to-maturity.
	 * @param d The parameter d: if c &gt; 0 this is the very long term volatility level.
	 * @param isCalibrateable Set this to true, if the parameters are available for calibration.
	 */
	public LIBORVolatilityModelFourParameterExponentialForm(final TimeDiscretization timeDiscretization, final TimeDiscretization liborPeriodDiscretization, final RandomVariable a, final RandomVariable b, final RandomVariable c, final RandomVariable d, final boolean isCalibrateable) {
		this(new RandomVariableFromArrayFactory(), timeDiscretization, liborPeriodDiscretization, a, b, c, d, isCalibrateable);
	}

	/**
	 * Creates the volatility model &sigma;<sub>i</sub>(t<sub>j</sub>) = ( a + b * (T<sub>i</sub>-t<sub>j</sub>) ) * exp(-c (T<sub>i</sub>-t<sub>j</sub>)) + d
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
	public LIBORVolatilityModelFourParameterExponentialForm(final RandomVariableFactory randomVariableFactory, final TimeDiscretization timeDiscretization, final TimeDiscretization liborPeriodDiscretization, final double a, final double b, final double c, final double d, final boolean isCalibrateable) {
		super(timeDiscretization, liborPeriodDiscretization);
		this.randomVariableFactory = randomVariableFactory;
		this.a = randomVariableFactory.createRandomVariable(a);
		this.b = randomVariableFactory.createRandomVariable(b);
		this.c = randomVariableFactory.createRandomVariable(c);
		this.d = randomVariableFactory.createRandomVariable(d);
		this.isCalibrateable = isCalibrateable;
	}

	/**
	 * Creates the volatility model &sigma;<sub>i</sub>(t<sub>j</sub>) = ( a + b * (T<sub>i</sub>-t<sub>j</sub>) ) * exp(-c (T<sub>i</sub>-t<sub>j</sub>)) + d
	 *
	 * @param timeDiscretization The simulation time discretization t<sub>j</sub>.
	 * @param liborPeriodDiscretization The period time discretization T<sub>i</sub>.
	 * @param a The parameter a: an initial volatility level.
	 * @param b The parameter b: the slope at the short end (shortly before maturity).
	 * @param c The parameter c: exponential decay of the volatility in time-to-maturity.
	 * @param d The parameter d: if c &gt; 0 this is the very long term volatility level.
	 * @param isCalibrateable Set this to true, if the parameters are available for calibration.
	 */
	public LIBORVolatilityModelFourParameterExponentialForm(final TimeDiscretization timeDiscretization, final TimeDiscretization liborPeriodDiscretization, final double a, final double b, final double c, final double d, final boolean isCalibrateable) {
		this(new RandomVariableFromArrayFactory(), timeDiscretization, liborPeriodDiscretization, a, b, c, d, isCalibrateable);
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
	public LIBORVolatilityModelFourParameterExponentialForm getCloneWithModifiedParameter(final RandomVariable[] parameter) {
		if(!isCalibrateable) {
			return this;
		}

		return new LIBORVolatilityModelFourParameterExponentialForm(
				randomVariableFactory,
				getTimeDiscretization(),
				getLiborPeriodDiscretization(),
				parameter[0],
				parameter[1],
				parameter[2],
				parameter[3],
				isCalibrateable
				);
	}

	@Override
	public RandomVariable getVolatility(final int timeIndex, final int liborIndex) {

		synchronized (volatilityLazyInitLock) {
			if(volatility == null) {
				volatility = new RandomVariable[getTimeDiscretization().getNumberOfTimeSteps()][getLiborPeriodDiscretization().getNumberOfTimeSteps()];
			}

			if(volatility[timeIndex][liborIndex] == null) {
				final double time             = getTimeDiscretization().getTime(timeIndex);
				final double maturity         = getLiborPeriodDiscretization().getTime(liborIndex);
				final double timeToMaturity   = maturity-time;

				RandomVariable volatilityInstanteaneous;
				if(timeToMaturity <= 0)
				{
					volatilityInstanteaneous = randomVariableFactory.createRandomVariable(0.0);   // This forward rate is already fixed, no volatility
				}
				else
				{
					volatilityInstanteaneous = (a.addProduct(b, timeToMaturity)).mult(c.mult(-timeToMaturity).exp()).add(d);
				}

				volatility[timeIndex][liborIndex] = volatilityInstanteaneous;
			}

			return volatility[timeIndex][liborIndex];
		}
	}

	@Override
	public Object clone() {
		return new LIBORVolatilityModelFourParameterExponentialForm(
				randomVariableFactory,
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
		RandomVariableFactory randomVariableFactory = this.randomVariableFactory;
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

			if(dataModified.getOrDefault("a", a) instanceof RandomVariable) {
				a = randomVariableFactory.createRandomVariable(((RandomVariable)dataModified.getOrDefault("a", a)).doubleValue());
			}else {
				a = randomVariableFactory.createRandomVariable((double)dataModified.get("a"));
			}
			if(dataModified.getOrDefault("b", b) instanceof RandomVariable) {
				b = randomVariableFactory.createRandomVariable(((RandomVariable)dataModified.getOrDefault("b", b)).doubleValue());
			}else {
				b = randomVariableFactory.createRandomVariable((double)dataModified.get("b"));
			}
			if(dataModified.getOrDefault("c", c) instanceof RandomVariable) {
				c = randomVariableFactory.createRandomVariable(((RandomVariable)dataModified.getOrDefault("c", c)).doubleValue());
			}else {
				c = randomVariableFactory.createRandomVariable((double)dataModified.get("c"));
			}
			if(dataModified.getOrDefault("d", d) instanceof RandomVariable) {
				d = randomVariableFactory.createRandomVariable(((RandomVariable)dataModified.getOrDefault("d", d)).doubleValue());
			}else {
				d = randomVariableFactory.createRandomVariable((double)dataModified.get("d"));
			}
		}

		final LIBORVolatilityModel newModel = new LIBORVolatilityModelFourParameterExponentialForm(randomVariableFactory, timeDiscretization, liborPeriodDiscretization, a, b, c, d, isCalibrateable);
		return newModel;
	}

	/*
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();

		// Init transient fields
		volatilityLazyInitLock = new Object();
	}
	 */
}
