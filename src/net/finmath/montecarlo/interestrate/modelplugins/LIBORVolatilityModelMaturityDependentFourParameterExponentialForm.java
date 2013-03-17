/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 08.08.2005
 */
package net.finmath.montecarlo.interestrate.modelplugins;

import java.util.Arrays;

import net.finmath.montecarlo.RandomVariable;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * @author fries
 */
public class LIBORVolatilityModelMaturityDependentFourParameterExponentialForm extends LIBORVolatilityModel {

    double a[];
    double b[];
    double c[];
    double d[];

    /**
     * @param timeDiscretization
     * @param liborPeriodDiscretization
     * @param a
     * @param b
     * @param c
     * @param d
     */
    public LIBORVolatilityModelMaturityDependentFourParameterExponentialForm(TimeDiscretizationInterface timeDiscretization, TimeDiscretizationInterface liborPeriodDiscretization, double a, double b, double c, double d) {
        super(timeDiscretization, liborPeriodDiscretization);
        this.a = new double[liborPeriodDiscretization.getNumberOfTimeSteps()];	Arrays.fill(this.a, a);
        this.b = new double[liborPeriodDiscretization.getNumberOfTimeSteps()];	Arrays.fill(this.b, b);
        this.c = new double[liborPeriodDiscretization.getNumberOfTimeSteps()];	Arrays.fill(this.c, c);
        this.d = new double[liborPeriodDiscretization.getNumberOfTimeSteps()];	Arrays.fill(this.d, d);
    }

    /**
     * @param timeDiscretization
     * @param liborPeriodDiscretization
     * @param a
     * @param b
     * @param c
     * @param d
     */
    public LIBORVolatilityModelMaturityDependentFourParameterExponentialForm(TimeDiscretizationInterface timeDiscretization, TimeDiscretizationInterface liborPeriodDiscretization, double[] a, double[] b, double[] c, double[] d) {
        super(timeDiscretization, liborPeriodDiscretization);
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
    }

	@Override
	public double[] getParameter() {
		double[] parameter = new double[4];
		parameter[0] = a[0];
		parameter[1] = b[0];
		parameter[2] = c[0];
		parameter[3] = d[0];

		return parameter;
	}

	@Override
	public void setParameter(double[] parameter) {
        Arrays.fill(this.a, parameter[0]);
        Arrays.fill(this.b, parameter[1]);
        Arrays.fill(this.c, parameter[2]);
        Arrays.fill(this.d, parameter[3]);
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
            volatilityInstanteaneous = (a[liborIndex] + b[liborIndex] * timeToMaturity) * Math.exp(-c[liborIndex] * timeToMaturity) + d[liborIndex];
        }
        if(volatilityInstanteaneous < 0.0) volatilityInstanteaneous = Math.max(volatilityInstanteaneous,0.0);

        return new RandomVariable(getTimeDiscretization().getTime(timeIndex),volatilityInstanteaneous);
    }

	public void setParameters(double[] a, double[] b, double[] c, double[] d) {
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
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
