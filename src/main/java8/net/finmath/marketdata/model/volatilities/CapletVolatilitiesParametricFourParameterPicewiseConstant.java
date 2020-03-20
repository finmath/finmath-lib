/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 30.08.2014
 */

package net.finmath.marketdata.model.volatilities;

import java.time.LocalDate;

import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.time.TimeDiscretization;

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
 * @version 1.0
 */
public class CapletVolatilitiesParametricFourParameterPicewiseConstant extends AbstractVolatilitySurfaceParametric {

	private final double a,b,c,d;
	private final TimeDiscretization timeDiscretization;

	/**
	 * Create a model with parameters a,b,c,d.
	 *
	 * @param name The name of this volatility surface.
	 * @param referenceDate The reference date for this volatility surface, i.e., the date which defined t=0.
	 * @param a The parameter a
	 * @param b The parameter b
	 * @param c The parameter c
	 * @param d The parameter d
	 * @param timeDiscretization The timeDiscretizationFromArray used in numerical integration.
	 */
	public CapletVolatilitiesParametricFourParameterPicewiseConstant(final String name, final LocalDate referenceDate, final double a, final double b, final double c, final double d, final TimeDiscretization timeDiscretization) {
		super(name, referenceDate, null, null, QuotingConvention.VOLATILITYLOGNORMAL, null);
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
		this.timeDiscretization = timeDiscretization;
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
		if(maturity == 0) {
			return 0;
		}

		/*
		 * Integral of the square of the instantaneous volatility function
		 * ((a + b * T) * Math.exp(- c * T) + d);
		 */
		double integratedVariance = 0.0;
		for(int timeIndex = 0; timeIndex < timeDiscretization.getNumberOfTimeSteps(); timeIndex++) {
			final double time = timeDiscretization.getTime(timeIndex);
			if(time > maturity) {
				break;
			}

			final double timeStep = timeDiscretization.getTimeStep(timeIndex);
			final double instantaneousVolatility = (a + b * (maturity-time)) * Math.exp(-c * (maturity-time)) + d;

			integratedVariance += instantaneousVolatility*instantaneousVolatility * Math.min(maturity-time, timeStep);
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
		return new CapletVolatilitiesParametricFourParameterPicewiseConstant(getName(), getReferenceDate(), value[0], value[1], value[2], value[3], timeDiscretization);
	}
}
