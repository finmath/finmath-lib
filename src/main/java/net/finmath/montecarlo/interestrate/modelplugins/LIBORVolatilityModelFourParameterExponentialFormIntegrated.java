/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 08.08.2005
 */
package net.finmath.montecarlo.interestrate.modelplugins;

import net.finmath.marketdata.model.volatilities.CapletVolatilitiesParametric;
import net.finmath.marketdata.model.volatilities.VolatilitySurfaceInterface.QuotingConvention;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

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
 */
public class LIBORVolatilityModelFourParameterExponentialFormIntegrated extends LIBORVolatilityModel {

	private static final long serialVersionUID = -1613728266481870311L;

	private double a;
	private double b;
	private double c;
	private double d;

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
	public LIBORVolatilityModelFourParameterExponentialFormIntegrated(TimeDiscretizationInterface timeDiscretization, TimeDiscretizationInterface liborPeriodDiscretization, double a, double b, double c, double d, boolean isCalibrateable) {
		super(timeDiscretization, liborPeriodDiscretization);
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
		this.isCalibrateable = isCalibrateable;
		cap = new CapletVolatilitiesParametric("", null, a, b, c, d);
	}


	@Override
	public double[] getParameter() {
		if(!isCalibrateable) return null;

		double[] parameter = new double[4];
		parameter[0] = a;
		parameter[1] = b;
		parameter[2] = c;
		parameter[3] = d;

		return parameter;
	}

	@Override
	public LIBORVolatilityModelFourParameterExponentialFormIntegrated getCloneWithModifiedParameter(double[] parameter) {
		if(!isCalibrateable) return this;

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
	public RandomVariableInterface getVolatility(int timeIndex, int liborIndex) {
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

		return new RandomVariable(Math.sqrt(varianceInstantaneous));
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
}
