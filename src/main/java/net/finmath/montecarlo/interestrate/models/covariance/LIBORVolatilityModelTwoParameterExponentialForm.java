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
 * Implements the volatility model &sigma;<sub>i</sub>(t<sub>j</sub>) = a * exp(-b (T<sub>i</sub>-t<sub>j</sub>))
 *
 * @author Christian Fries
 * @version 1.0
 */
public class LIBORVolatilityModelTwoParameterExponentialForm extends LIBORVolatilityModel {

	private static final long serialVersionUID = 8398006103722351360L;

	private final RandomVariableFactory	abstractRandomVariableFactory;

	private RandomVariable a;
	private RandomVariable b;

	private boolean isCalibrateable = false;

	// A lazy init cache
	private transient RandomVariable[][] volatility;
	private Object volatilityLazyInitLock = new Object();

	/**
	 * Creates the volatility model &sigma;<sub>i</sub>(t<sub>j</sub>) = a * exp(-b (T<sub>i</sub>-t<sub>j</sub>))
	 *
	 * @param abstractRandomVariableFactory The random variable factor used to construct random variables from the parameters.
	 * @param timeDiscretization The simulation time discretization t<sub>j</sub>.
	 * @param liborPeriodDiscretization The period time discretization T<sub>i</sub>.
	 * @param a The parameter a: an initial volatility level.
	 * @param b The parameter b: exponential decay of the volatility.
	 * @param isCalibrateable Set this to true, if the parameters are available for calibration.
	 */
	public LIBORVolatilityModelTwoParameterExponentialForm(RandomVariableFactory abstractRandomVariableFactory, TimeDiscretization timeDiscretization, TimeDiscretization liborPeriodDiscretization, RandomVariable a, RandomVariable b, boolean isCalibrateable) {
		super(timeDiscretization, liborPeriodDiscretization);
		this.abstractRandomVariableFactory = abstractRandomVariableFactory;
		this.a = a;
		this.b = b;
		this.isCalibrateable = isCalibrateable;
	}

	/**
	 * Creates the volatility model &sigma;<sub>i</sub>(t<sub>j</sub>) = a * exp(-b (T<sub>i</sub>-t<sub>j</sub>))
	 *
	 * @param abstractRandomVariableFactory The random variable factor used to construct random variables from the parameters.
	 * @param timeDiscretization The simulation time discretization t<sub>j</sub>.
	 * @param liborPeriodDiscretization The period time discretization T<sub>i</sub>.
	 * @param a The parameter a: an initial volatility level.
	 * @param b The parameter b: exponential decay of the volatility.
	 * @param isCalibrateable Set this to true, if the parameters are available for calibration.
	 */
	public LIBORVolatilityModelTwoParameterExponentialForm(RandomVariableFactory abstractRandomVariableFactory, TimeDiscretization timeDiscretization, TimeDiscretization liborPeriodDiscretization, double a, double b, boolean isCalibrateable) {
		this(abstractRandomVariableFactory, timeDiscretization, liborPeriodDiscretization, abstractRandomVariableFactory.createRandomVariable(a), abstractRandomVariableFactory.createRandomVariable(b), isCalibrateable);
	}

	/**
	 * Creates the volatility model &sigma;<sub>i</sub>(t<sub>j</sub>) = a * exp(-b (T<sub>i</sub>-t<sub>j</sub>))
	 *
	 * @param timeDiscretization The simulation time discretization t<sub>j</sub>.
	 * @param liborPeriodDiscretization The period time discretization T<sub>i</sub>.
	 * @param a The parameter a: an initial volatility level.
	 * @param b The parameter b: exponential decay of the volatility.
	 */
	public LIBORVolatilityModelTwoParameterExponentialForm(TimeDiscretization timeDiscretization, TimeDiscretization liborPeriodDiscretization, double a, double b) {
		this(new RandomVariableFromArrayFactory(), timeDiscretization, liborPeriodDiscretization, a, b, true);
	}


	@Override
	public RandomVariable[] getParameter() {
		if(!isCalibrateable) {
			return null;
		}

		RandomVariable[] parameter = new RandomVariable[2];
		parameter[0] = a;
		parameter[1] = b;

		return parameter;
	}

	@Override
	public LIBORVolatilityModelTwoParameterExponentialForm getCloneWithModifiedParameter(RandomVariable[] parameter) {
		if(!isCalibrateable) {
			return this;
		}

		return new LIBORVolatilityModelTwoParameterExponentialForm(
				abstractRandomVariableFactory,
				getTimeDiscretization(),
				getLiborPeriodDiscretization(),
				parameter[0],
				parameter[1],
				isCalibrateable
				);
	}

	@Override
	public RandomVariable getVolatility(int timeIndex, int liborIndex) {

		synchronized (volatilityLazyInitLock) {
			if(volatility == null) {
				volatility = new RandomVariable[getTimeDiscretization().getNumberOfTimeSteps()][getLiborPeriodDiscretization().getNumberOfTimeSteps()];
			}

			if(volatility[timeIndex][liborIndex] == null) {
				double time             = getTimeDiscretization().getTime(timeIndex);
				double maturity         = getLiborPeriodDiscretization().getTime(liborIndex);
				double timeToMaturity   = maturity-time;

				RandomVariable volatilityInstanteaneous;
				if(timeToMaturity <= 0)
				{
					volatilityInstanteaneous = abstractRandomVariableFactory.createRandomVariable(0.0);   // This forward rate is already fixed, no volatility
				}
				else
				{
					volatilityInstanteaneous = a.mult(b.mult(-timeToMaturity).exp());
				}

				volatility[timeIndex][liborIndex] = volatilityInstanteaneous;
			}

			return volatility[timeIndex][liborIndex];
		}
	}
	@Override
	public Object clone() {
		return new LIBORVolatilityModelTwoParameterExponentialForm(
				abstractRandomVariableFactory,
				getTimeDiscretization(),
				getLiborPeriodDiscretization(),
				a,
				b,
				isCalibrateable
				);
	}

	@Override
	public LIBORVolatilityModel getCloneWithModifiedData(Map<String, Object> dataModified) {
		// TODO Auto-generated method stub
		return null;
	}
}
