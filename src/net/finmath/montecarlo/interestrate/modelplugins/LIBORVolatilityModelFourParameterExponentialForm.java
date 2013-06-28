/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 08.08.2005
 */
package net.finmath.montecarlo.interestrate.modelplugins;

import net.finmath.montecarlo.RandomVariable;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * Implements the volatility model sigma_{i}(t_{j}} = ( a + b * (T_{j}-t_{i}) ) * exp(-c (T_{j}-t_{i})) + d
 * 
 * @author Christian Fries
 */
public class LIBORVolatilityModelFourParameterExponentialForm extends LIBORVolatilityModel {

    private double a;
    private double b;
    private double c;
    private double d;
    
    private boolean isCalibrateable = false;

    /**
     * Creates the volatility model &sigma;_{i}(t_{j}} = ( a + b * (T_{j}-t_{i}) ) * exp(-c (T_{j}-t_{i})) + d
     * 
     * @param timeDiscretization The simulation time discretization t<sub>i</sub>.
     * @param liborPeriodDiscretization The period time discretization T<sub>j</sub>.
     * @param a The parameter a: an initial volatility level.
     * @param b The parameter b.
     * @param c The parameter c: exponential decay of the volatility.
     * @param d The parameter d: if c > 0 this is the very long term volatlity level.
     * @param isCalibrateable Set this to true, if the parameters are available for calibration.
     */
    public LIBORVolatilityModelFourParameterExponentialForm(TimeDiscretizationInterface timeDiscretization, TimeDiscretizationInterface liborPeriodDiscretization, double a, double b, double c, double d, boolean isCalibrateable) {
        super(timeDiscretization, liborPeriodDiscretization);
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
        this.isCalibrateable = isCalibrateable;
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
	public void setParameter(double[] parameter) {
		if(!isCalibrateable) return;

		this.a = parameter[0];
        this.b = parameter[1];
        this.c = parameter[2];
        this.d = parameter[3];
	}

    /* (non-Javadoc)
     * @see net.finmath.montecarlo.interestrate.modelplugins.LIBORVolatilityModel#getVolatility(int, int)
     */
    @Override
    public RandomVariableInterface getVolatility(int timeIndex, int liborIndex) {
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
            volatilityInstanteaneous = (a + b * timeToMaturity) * Math.exp(-c * timeToMaturity) + d;
        }
        if(volatilityInstanteaneous < 0.0) volatilityInstanteaneous = Math.max(volatilityInstanteaneous,0.0);

        return new RandomVariable(getTimeDiscretization().getTime(timeIndex),volatilityInstanteaneous);
    }

	@Override
	public Object clone() {
		return new LIBORVolatilityModelFourParameterExponentialForm(
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
