/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 30.08.2014
 */

package net.finmath.marketdata.model.volatilities;

import java.time.LocalDate;
import java.util.function.DoubleUnaryOperator;

import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.ForwardCurve;

/**
 * A parametric caplet volatility surface created form the four parameter model
 * for the instantaneous forward rate lognormal volatility given by
 * \( \sigma(t) = (a + b t) \exp(- c t) + d \).
 *
 * In other words, the Black volatility for maturity T is given by
 * \[ \sqrt{ \frac{1}{T} \int_0^T ((a + b t) \exp(- c t) + d)^2 dt } \].
 *
 * Note: quoting convention of the functional form is LOGNORMAL, but container may
 * provide data in other conventions.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class CapletVolatilitiesParametric extends AbstractVolatilitySurfaceParametric {

	private final double timeScaling;
	private final double a,b,c,d;

	/**
	 * Create a model with parameters a,b,c,d defining a lognormal volatility surface.
	 *
	 * @param name The name of this volatility surface.
	 * @param referenceDate The reference date for this volatility surface, i.e., the date which defined t=0.
	 * @param forwardCurve The underlying forward curve.
	 * @param discountCurve The associated discount curve.
	 * @param a The parameter a
	 * @param b The parameter b
	 * @param c The parameter c
	 * @param d The parameter d
	 * @param timeScaling A scaling factor applied to t when converting from global double time to the parametric function argument t.
	 * @param quotingConvention The quoting convention reflected by the parametetric form (e.g. lognormal or normal).
	 */
	public CapletVolatilitiesParametric(
			final String name,
			final LocalDate referenceDate,
			final ForwardCurve forwardCurve,
			final DiscountCurve discountCurve,
			final double a, final double b, final double c, final double d, final double timeScaling, final QuotingConvention quotingConvention) {
		super(name, referenceDate, forwardCurve, discountCurve, quotingConvention, null);
		this.timeScaling = timeScaling;
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
	}

	/**
	 * Create a model with parameters a,b,c,d defining a lognormal volatility surface.
	 *
	 * @param name The name of this volatility surface.
	 * @param referenceDate The reference date for this volatility surface, i.e., the date which defined t=0.
	 * @param forwardCurve The underlying forward curve.
	 * @param discountCurve The associated discount curve.
	 * @param a The parameter a
	 * @param b The parameter b
	 * @param c The parameter c
	 * @param d The parameter d
	 * @param timeScaling A scaling factor applied to t when converting from global double time to the parametric function argument t.
	 */
	public CapletVolatilitiesParametric(final String name, final LocalDate referenceDate,
			final ForwardCurve forwardCurve,
			final DiscountCurve discountCurve,
			final double a, final double b, final double c, final double d, final double timeScaling) {
		super(name, referenceDate, forwardCurve, discountCurve, QuotingConvention.VOLATILITYLOGNORMAL, null);
		this.timeScaling = timeScaling;
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
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
	 * @param timeScaling A scaling factor applied to t when converting from global double time to the parametric function argument t.
	 */
	public CapletVolatilitiesParametric(final String name, final LocalDate referenceDate,
			final double a, final double b, final double c, final double d, final double timeScaling) {
		this(name, referenceDate, null, null, a, b, c, d, timeScaling);
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
	public CapletVolatilitiesParametric(final String name, final LocalDate referenceDate, final double a, final double b, final double c, final double d) {
		this(name, referenceDate, a, b, c, d, 1.0);
	}

	/* (non-Javadoc)
	 * @see net.finmath.marketdata.model.volatilities.VolatilitySurfaceInterface#getValue(double, double, net.finmath.marketdata.model.volatilities.VolatilitySurfaceInterface.QuotingConvention)
	 */
	@Override
	public double getValue(final double maturity, final double strike, final QuotingConvention quotingConvention) {
		return getValue(null, maturity, strike, quotingConvention);
	}

	/* (non-Javadoc)
	 * @see net.finmath.marketdata.model.volatilities.VolatilitySurfaceInterface#getValue(net.finmath.marketdata.model.AnalyticModelInterface, double, double, net.finmath.marketdata.model.volatilities.VolatilitySurfaceInterface.QuotingConvention)
	 */
	@Override
	public double getValue(final AnalyticModel model, final double maturity, final double strike, final QuotingConvention quotingConvention) {
		if(maturity <= 0) {
			return 0;
		}

		final double T = maturity * timeScaling;

		/*
		 * Integral of the square of the instantaneous volatility function
		 * ((a + b * T) * Math.exp(- c * T) + d);
		 */
		double integratedVariance;
		if(Math.abs(c*T) > 1E-5) {
			final double u = Math.exp(-c*T);
			final double u2 = Math.exp(-2*c*T);

			final DoubleUnaryOperator umxlogu = x -> (u-x)/Math.log(u);
			final DoubleUnaryOperator u2mxlogu2 = x -> (u2-x)/Math.log(u2);

			final double expA1 = umxlogu.applyAsDouble(1.0);
			final double expA2 = umxlogu.applyAsDouble(expA1) * 2.0;
			final double expB1 = u2mxlogu2.applyAsDouble(1.0);
			final double expB2 = u2mxlogu2.applyAsDouble(expB1) * 2.0;
			final double expB3 = u2mxlogu2.applyAsDouble(expB2) * 3.0;

			/*
			 * http://www.wolframalpha.com/input/?i=integrate+%28%28a+%2B+b+*+t%29+*+exp%28-+c+*+t%29+%2B+d%29%5E2+from+0+to+T
			 * integral_0^T ((a+b t) exp(-(c t))+d)^2  dt = 1/4 ((e^(-2 c T) (-2 a^2 c^2-2 a b c (2 c T+1)+b^2 (-(2 c T (c T+1)+1))))/c^3+(2 a^2 c^2+2 a b c+b^2)/c^3-(8 d e^(-c T) (a c+b c T+b))/c^2+(8 d (a c+b))/c^2+4 d^2 T)
			 */
			integratedVariance = a*a*T*expB1
					+ a*b*T*T*expB2
					+ 2.0*a*d*T*expA1
					+ b*b*T*T*T*expB3/3.0
					+ b*d*T*T*expA2
					+ d*d*T;
		}
		else {
			// Treat c as c = 0

			/*
			 * http://www.wolframalpha.com/input/?i=expand+%28integrate+%28%28a+%2B+b+*+t%29+%2B+d%29%5E2+from+0+to+T%29
			 */
			integratedVariance = a*a*T + a*b*T*T + 2*a*d*T + (b*b*T*T*T)/3.0 + b*d*T*T + d*d*T;
		}

		final double value = Math.sqrt(integratedVariance/maturity);
		return convertFromTo(model, maturity, strike, value, this.getQuotingConvention(), quotingConvention);
	}

	@Override
	public double[] getParameter() {
		final double[] parameter = new double[4];
		parameter[0] = a;
		parameter[1] = b;
		parameter[2] = c;
		parameter[3] = d;

		return parameter;
	}

	@Override
	public void setParameter(final double[] parameter) {
		throw new UnsupportedOperationException("This class is immutable.");
	}

	@Override
	public AbstractVolatilitySurfaceParametric getCloneForParameter(final double[] value) throws CloneNotSupportedException {
		return new CapletVolatilitiesParametric(getName(), getReferenceDate(), getForwardCurve(), getDiscountCurve(), value[0], value[1], value[2], value[3], timeScaling, getQuotingConvention());
	}

}
