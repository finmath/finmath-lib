/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 30.08.2014
 */

package net.finmath.marketdata.model.volatilities;

import java.util.Calendar;

import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * A parametric caplet volatility surface created form the
 * picewise constant (numerical integration) of the four parameter model
 * for the instantaneous forward rate volatility given by
 * \( \sigma(t) = (a + b t) \exp(- c t) + d \).
 * 
 * In other words, the Black volatility for maturity T is given by
 * \[ \sqrt{ \frac{1}{t_{n}} \sum_{i=0}^{n-1} ((a + b t_{i}) \exp(- c t_{i}) + d)^2 (t_{i+1}-t_{i}) } \],
 * where \( t_{i} \) is given time discretization.
 * 
 * @author Christian Fries
 */
public class CapletVolatilitiesParametricFourParameterPicewiseConstant extends AbstractVolatilitySurface {

	private final double a,b,c,d;
	private final TimeDiscretizationInterface timeDiscretization;

	/**
	 * Create a model with parameters a,b,c,d.
	 * 
	 * @param name The name of this volatility surface.
	 * @param referenceDate The reference date for this volatility surface, i.e., the date which defined t=0.
	 * @param a The parameter a
	 * @param b The parameter b
	 * @param c The parameter c
	 * @param d The parameter d
	 * @param timeDiscretization The timeDiscretization used in numerical integration.
	 */
	public CapletVolatilitiesParametricFourParameterPicewiseConstant(String name, Calendar referenceDate, double a, double b, double c, double d, TimeDiscretizationInterface timeDiscretization) {
		super(name, referenceDate);
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
		this.timeDiscretization = timeDiscretization;
		this.quotingConvention = QuotingConvention.VOLATILITYLOGNORMAL;
	}

	/* (non-Javadoc)
	 * @see net.finmath.marketdata.model.volatilities.VolatilitySurfaceInterface#getValue(double, double, net.finmath.marketdata.model.volatilities.VolatilitySurfaceInterface.QuotingConvention)
	 */
	@Override
	public double getValue(double maturity, double strike, QuotingConvention quotingConvention) {
		return getValue(null, maturity, strike, quotingConvention);
	}

	/* (non-Javadoc)
	 * @see net.finmath.marketdata.model.volatilities.VolatilitySurfaceInterface#getValue(net.finmath.marketdata.model.AnalyticModelInterface, double, double, net.finmath.marketdata.model.volatilities.VolatilitySurfaceInterface.QuotingConvention)
	 */
	@Override
	public double getValue(AnalyticModelInterface model, double maturity, double strike, QuotingConvention quotingConvention) {
		if(maturity == 0) return 0;

		/*
		 * Integral of the square of the instantaneous volatility function
		 * ((a + b * T) * Math.exp(- c * T) + d);
		 */
		double integratedVariance = 0.0;
		for(int timeIndex = 0; timeIndex < timeDiscretization.getNumberOfTimeSteps(); timeIndex++) {
			double time = timeDiscretization.getTime(timeIndex);
			if(time > maturity) break;

			double timeStep = timeDiscretization.getTimeStep(timeIndex);
			double instantaneousVolatility = (a + b * time) * Math.exp(-c * time) + d;
			
			integratedVariance += instantaneousVolatility*instantaneousVolatility * Math.min(maturity-time, timeStep);
		}

		double value = Math.sqrt(integratedVariance/maturity);
		return convertFromTo(model, maturity, strike, value, this.quotingConvention, quotingConvention);
	}
}
