/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 08.08.2005
 */
package net.finmath.montecarlo.interestrate.modelplugins;

import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.time.TimeDiscretization;

/**
 * Implements the volatility model &sigma;<sub>i</sub>(t<sub>j</sub>) = a * exp(-b (T<sub>i</sub>-t<sub>j</sub>))
 *
 * @author Christian Fries
 * @version 1.0
 */
public class LIBORVolatilityModelTwoParameterExponentialForm extends LIBORVolatilityModel {

	private static final long serialVersionUID = 8398006103722351360L;

	private double a;
	private double b;

	private boolean isCalibrateable = false;

	/**
	 * Creates the volatility model &sigma;<sub>i</sub>(t<sub>j</sub>) = a * exp(-b (T<sub>i</sub>-t<sub>j</sub>))
	 *
	 * @param timeDiscretizationFromArray The simulation time discretization t<sub>j</sub>.
	 * @param liborPeriodDiscretization The period time discretization T<sub>i</sub>.
	 * @param a The parameter a: an initial volatility level.
	 * @param b The parameter b: exponential decay of the volatility.
	 */
	public LIBORVolatilityModelTwoParameterExponentialForm(TimeDiscretization timeDiscretization, TimeDiscretization liborPeriodDiscretization, double a, double b) {
		this(timeDiscretization, liborPeriodDiscretization, a, b, true);
	}

	/**
	 * Creates the volatility model &sigma;<sub>i</sub>(t<sub>j</sub>) = a * exp(-b (T<sub>i</sub>-t<sub>j</sub>))
	 *
	 * @param timeDiscretizationFromArray The simulation time discretization t<sub>j</sub>.
	 * @param liborPeriodDiscretization The period time discretization T<sub>i</sub>.
	 * @param a The parameter a: an initial volatility level.
	 * @param b The parameter b: exponential decay of the volatility.
	 * @param isCalibrateable Set this to true, if the parameters are available for calibration.
	 */
	public LIBORVolatilityModelTwoParameterExponentialForm(TimeDiscretization timeDiscretization, TimeDiscretization liborPeriodDiscretization, double a, double b, boolean isCalibrateable) {
		super(timeDiscretization, liborPeriodDiscretization);
		this.a = a;
		this.b = b;
		this.isCalibrateable = isCalibrateable;
	}

	@Override
	public double[] getParameter() {
		if(!isCalibrateable) {
			return null;
		}

		double[] parameter = new double[2];
		parameter[0] = a;
		parameter[1] = b;

		return parameter;
	}

	@Override
	public LIBORVolatilityModelTwoParameterExponentialForm getCloneWithModifiedParameter(double[] parameter) {
		if(!isCalibrateable) {
			return this;
		}

		return new LIBORVolatilityModelTwoParameterExponentialForm(
				getTimeDiscretization(),
				getLiborPeriodDiscretization(),
				a,
				b,
				isCalibrateable
				);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.modelplugins.LIBORVolatilityModel#getVolatility(int, int)
	 */
	@Override
	public RandomVariableFromDoubleArray getVolatility(int timeIndex, int liborIndex) {
		// Create a very simple volatility model here
		double time             = getTimeDiscretization().getTime(timeIndex);
		double maturity         = getLiborPeriodDiscretization().getTime(liborIndex);
		double timeToMaturity   = maturity-time;

		double volatilityInstanteaneous;
		if(timeToMaturity <= 0)
		{
			volatilityInstanteaneous = 0;   // This forward rate is already fixed, no volatility
		}
		else
		{
			volatilityInstanteaneous = a * Math.exp(-b * timeToMaturity);
		}

		return new RandomVariableFromDoubleArray(getTimeDiscretization().getTime(timeIndex), volatilityInstanteaneous);
	}

	@Override
	public Object clone() {
		return new LIBORVolatilityModelTwoParameterExponentialForm(
				getTimeDiscretization(),
				getLiborPeriodDiscretization(),
				a,
				b,
				isCalibrateable
				);
	}
}
