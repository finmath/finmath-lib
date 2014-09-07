/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 30.08.2014
 */

package net.finmath.marketdata.model.volatilities;

import java.util.Calendar;

import net.finmath.marketdata.model.AnalyticModelInterface;

/**
 * A parametric caplet volatility surface created form the four parameter model
 * for the instantaneous forward rate volatility given by
 * \( \sigma(t) = (a + b t) \exp(- c t) + d \).
 * 
 * In other words, the Black volatility for maturity T is given by
 * \[ \sqrt{ \frac{1}{T} \int_0^T ((a + b t) \exp(- c t) + d)^2 dt } \].
 * 
 * @author Christian Fries
 */
public class CapletVolatilitiesParametric extends AbstractVolatilitySurface {

	private final double timeScaling;
	private final double a,b,c,d;

	/**
	 * Create a model with parameters a,b,c,d.
	 * 
	 * @param name The name of this volatility surface.
	 * @param referenceDate The reference date for this volatility surface, i.e., the date which defined t=0.
	 * @param a The parameter a
	 * @param b The parameter b
	 * @param c The parameter c
	 * @param d The parameter d
	 * @param timeScaling A scaling factor applied to t when converting from global double time to the parametric function argument t.
	 */
	public CapletVolatilitiesParametric(String name, Calendar referenceDate, double a, double b, double c, double d, double timeScaling) {
		super(name, referenceDate);
		this.timeScaling = timeScaling;
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
		this.quotingConvention = QuotingConvention.VOLATILITYLOGNORMAL;
	}

	/**
	 * Create a model with parameters a,b,c,d.
	 * 
	 * @param name The name of this volatility surface.
	 * @param referenceDate The reference date for this volatility surface, i.e., the date which defined t=0.
	 * @param a The parameter a
	 * @param b The parameter b
	 * @param c The parameter c
	 * @param d The parameter d
	 */
	public CapletVolatilitiesParametric(String name, Calendar referenceDate, double a, double b, double c, double d) {
		this(name, referenceDate, a, b, c, d, 1.0);
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

		double T = maturity * timeScaling;

		/*
		 * Integral of the square of the instantaneous volatility function
		 * ((a + b * T) * Math.exp(- c * T) + d);
		 */
		double integratedVariance;
		if(c != 0) {
			/*
			 * http://www.wolframalpha.com/input/?i=expand+%28integrate+%28%28a+%2B+b+*+t%29+*+exp%28-+c+*+t%29+%2B+d%29%5E2+from+0+to+T%29
			 * integral_0^T ((a+b t) exp(-(c t))+d)^2  dt = 1/4 ((e^(-2 c T) (-2 a*a c^2-2 a b c (2 c T+1)+b^2 (-(2 c T (c T+1)+1))))/c^3+(2 a*a c^2+2 a b c+b^2)/c^3-(8 d e^(-c T) (a c+b c T+b))/c^2+(8 d (a c+b))/c^2+4 d*d T)
			 */
			integratedVariance = a*a*T*((1-Math.exp(-2*c*T))/(2*c*T))
					+ a*b*T*T*(((1 - Math.exp(-2*c*T))/(2*c*T) - Math.exp(-2*c*T))/(c*T))
					+ 2*a*d*T*((1-Math.exp(-c*T))/c)
					+ b*b*T*T*T*(((((1-Math.exp(-2*c*T))/(2*c*T)-Math.exp(-2*c*T))/(T*c)-Math.exp(-2*c*T)))/(2*c*T))
					+ 2*b*d*T*T*(((1-Math.exp(-c*T))-T*c*Math.exp(-c*T))/(c*c*T*T))
					+ d*d*T;
		}
		else {
			/*
			 * http://www.wolframalpha.com/input/?i=expand+%28integrate+%28%28a+%2B+b+*+t%29+%2B+d%29%5E2+from+0+to+T%29
			 * integral_0^T ((a+b t) exp(-(c t))+d)^2  dt = 1/4 ((e^(-2 c T) (-2 a*a c^2-2 a b c (2 c T+1)+b^2 (-(2 c T (c T+1)+1))))/c^3+(2 a*a c^2+2 a b c+b^2)/c^3-(8 d e^(-c T) (a c+b c T+b))/c^2+(8 d (a c+b))/c^2+4 d*d T)
			 */
			integratedVariance = a*a*T + a*b*T*T + 2*a*d*T + (b*b*T*T*T)/3 + b*d*T*T + d*d*T;
		}

		double value = Math.sqrt(integratedVariance/maturity);
		return convertFromTo(model, maturity, strike, value, this.quotingConvention, quotingConvention);
	}
}
