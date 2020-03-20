/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 30.08.2014
 */

package net.finmath.marketdata.model.volatilities;

import java.time.LocalDate;

import net.finmath.functions.AnalyticFormulas;
import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.ForwardCurve;

/**
 * A parametric caplet volatility surface created form the four parameter model
 * for the instantaneous <i>displaced</i> forward rate lognormal volatility given by
 * \( \sigma(t) = (a + b t) \exp(- c t) + d \).
 *
 * In other words, the Black volatility of the <i>displaced</i> rate for maturity T is given by
 * \[ \sqrt{ \frac{1}{T} \int_0^T ((a + b t) \exp(- c t) + d)^2 dt } \].
 *
 * The displacement may be either set as a fixed parameter (isDisplacementCalibrateable = false) or
 * as a free parameter (isDisplacementCalibrateable = true). This will alter the behavior of
 * the getCloneForParameter method which either requires a double[4] or a double[5] argument.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class CapletVolatilitiesParametricDisplacedFourParameterAnalytic extends AbstractVolatilitySurfaceParametric {

	private final double timeScaling;
	private final double a,b,c,d;
	private final double displacement;
	private final boolean isDisplacementCalibrateable;

	/**
	 * Create a model with parameters a,b,c,d defining a displaced lognormal volatility surface.
	 *
	 * @param name The name of this volatility surface.
	 * @param referenceDate The reference date for this volatility surface, i.e., the date which defined t=0.
	 * @param forwardCurve The underlying forward curve.
	 * @param discountCurve The associated discount curve.
	 * @param displacement The displacement for the forward rate.
	 * @param isDisplacementCalibrateable Boolean specifying if the displacement parameter is considered a free parameter of the model.
	 * @param a The parameter a
	 * @param b The parameter b
	 * @param c The parameter c
	 * @param d The parameter d
	 * @param timeScaling A scaling factor applied to t when converting from global double time to the parametric function argument t.
	 */
	public CapletVolatilitiesParametricDisplacedFourParameterAnalytic(final String name, final LocalDate referenceDate,
			final ForwardCurve forwardCurve,
			final DiscountCurve discountCurve,
			final double displacement, final boolean isDisplacementCalibrateable,
			final double a, final double b, final double c, final double d, final double timeScaling) {
		super(name, referenceDate, forwardCurve, discountCurve, QuotingConvention.VOLATILITYLOGNORMAL, null);
		this.timeScaling = timeScaling;
		this.displacement = displacement;
		this.isDisplacementCalibrateable = isDisplacementCalibrateable;
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
	}

	@Override
	public double getValue(final double maturity, final double strike, final QuotingConvention quotingConvention) {
		return getValue(null, maturity, strike, quotingConvention);
	}

	@Override
	public double getValue(final AnalyticModel model, final double maturity, final double strike, final QuotingConvention quotingConvention) {
		if(maturity == 0) {
			return 0;
		}

		final double T = maturity * timeScaling;

		/*
		 * Integral of the square of the instantaneous volatility function
		 * ((a + b * T) * Math.exp(- c * T) + d);
		 */
		double integratedVariance;
		if(c != 0) {
			/*
			 * http://www.wolframalpha.com/input/?i=integrate+%28%28a+%2B+b+*+t%29+*+exp%28-+c+*+t%29+%2B+d%29%5E2+from+0+to+T
			 * integral_0^T ((a+b t) exp(-(c t))+d)^2  dt = 1/4 ((e^(-2 c T) (-2 a^2 c^2-2 a b c (2 c T+1)+b^2 (-(2 c T (c T+1)+1))))/c^3+(2 a^2 c^2+2 a b c+b^2)/c^3-(8 d e^(-c T) (a c+b c T+b))/c^2+(8 d (a c+b))/c^2+4 d^2 T)
			 */
			integratedVariance = a*a*T*((1-Math.exp(-2*c*T))/(2*c*T))
					+ a*b*T*T*(((1 - Math.exp(-2*c*T))/(2*c*T) - Math.exp(-2*c*T))/(c*T))
					+ 2*a*d*T*((1-Math.exp(-c*T))/(c*T))
					+ b*b*T*T*T*(((((1-Math.exp(-2*c*T))/(2*c*T)-Math.exp(-2*c*T))/(T*c)-Math.exp(-2*c*T)))/(2*c*T))
					+ 2*b*d*T*T*(((1-Math.exp(-c*T))-T*c*Math.exp(-c*T))/(c*c*T*T))
					+ d*d*T;
		}
		else {
			/*
			 * http://www.wolframalpha.com/input/?i=expand+%28integrate+%28%28a+%2B+b+*+t%29+%2B+d%29%5E2+from+0+to+T%29
			 */
			integratedVariance = a*a*T + a*b*T*T + 2*a*d*T + (b*b*T*T*T)/3.0 + b*d*T*T + d*d*T;
		}

		final double value = Math.sqrt(integratedVariance/maturity);

		if(getDiscountCurve() == null) {
			throw new IllegalArgumentException("Missing discount curve. Conversion of QuotingConvention requires forward curve and discount curve to be set.");
		}
		if(getForwardCurve() == null) {
			throw new IllegalArgumentException("Missing forward curve. Conversion of QuotingConvention requires forward curve and discount curve to be set.");
		}

		final double periodStart = maturity;
		final double periodEnd = periodStart + getForwardCurve().getPaymentOffset(periodStart);

		final double forward = getForwardCurve().getForward(model, periodStart);

		double daycountFraction;
		if(getDaycountConvention() != null) {
			final LocalDate startDate = getReferenceDate().plusDays((int)Math.round(periodStart*365));
			final LocalDate endDate   = getReferenceDate().plusDays((int)Math.round(periodEnd*365));
			daycountFraction = getDaycountConvention().getDaycountFraction(startDate, endDate);
		}
		else {
			daycountFraction = getForwardCurve().getPaymentOffset(periodStart);
		}

		final double payoffUnit = getDiscountCurve().getDiscountFactor(maturity+getForwardCurve().getPaymentOffset(maturity)) * daycountFraction;

		final double valuePrice = AnalyticFormulas.blackScholesGeneralizedOptionValue(forward+displacement, value, maturity, strike+displacement, payoffUnit);

		return convertFromTo(model, maturity, strike, valuePrice, QuotingConvention.PRICE, quotingConvention);
	}

	@Override
	public double[] getParameter() {
		double[] parameter;
		if(isDisplacementCalibrateable) {
			parameter = new double[5];
			parameter[0] = displacement;
			parameter[1] = a;
			parameter[2] = b;
			parameter[3] = c;
			parameter[4] = d;

			return parameter;
		}
		else {
			parameter = new double[4];
			parameter[0] = a;
			parameter[1] = b;
			parameter[2] = c;
			parameter[3] = d;

			return parameter;
		}
	}

	@Override
	public void setParameter(final double[] parameter) {
		throw new UnsupportedOperationException("This class is immutable.");
	}

	@Override
	public AbstractVolatilitySurfaceParametric getCloneForParameter(final double[] value) throws CloneNotSupportedException {
		if(isDisplacementCalibrateable) {
			return new CapletVolatilitiesParametricDisplacedFourParameterAnalytic(getName(), getReferenceDate(), getForwardCurve(), getDiscountCurve(), value[0], isDisplacementCalibrateable, value[1], value[2], value[3], value[4], timeScaling);
		}
		else {
			return new CapletVolatilitiesParametricDisplacedFourParameterAnalytic(getName(), getReferenceDate(), getForwardCurve(), getDiscountCurve(), displacement, isDisplacementCalibrateable, value[0], value[1], value[2], value[3], timeScaling);
		}
	}

}
