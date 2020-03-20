/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 23.03.2014
 */

package net.finmath.fouriermethod.products;

import java.util.HashMap;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;

import org.apache.commons.math3.complex.Complex;

import net.finmath.exception.CalculationException;
import net.finmath.fouriermethod.CharacteristicFunction;
import net.finmath.fouriermethod.models.CharacteristicFunctionModel;
import net.finmath.integration.RealIntegral;
import net.finmath.integration.SimpsonRealIntegrator;
import net.finmath.modelling.Model;

/**
 * @author Christian Fries
 *
 * @version 1.0
 */
public abstract class AbstractFourierTransformProduct implements CharacteristicFunction, FourierTransformProduct {

	/* (non-Javadoc)
	 * @see net.finmath.fouriermethod.products.FourierTransformProduct#getValue(double, net.finmath.modelling.Model)
	 */
	@Override
	public Double getValue(final double evaluationTime, final Model model) {
		Double value = null;
		try {
			value = getValue((CharacteristicFunctionModel) model);
		} catch (final CalculationException e) {
		}

		return value;
	}

	/* (non-Javadoc)
	 * @see net.finmath.fouriermethod.products.FourierTransformProduct#getValues(double, net.finmath.modelling.Model)
	 */
	@Override
	public Map<String, Object> getValues(final double evaluationTime, final Model model) {
		final Map<String, Object>  result = new HashMap<>();

		try {
			final double value = getValue((CharacteristicFunctionModel) model);
			result.put("value", value);
		} catch (final CalculationException e) {
			result.put("exception", e);
		}

		return result;
	}

	/* (non-Javadoc)
	 * @see net.finmath.fouriermethod.products.FourierTransformProduct#getValue(net.finmath.fouriermethod.models.CharacteristicFunctionModel)
	 */
	@Override
	public double getValue(final CharacteristicFunctionModel model) throws CalculationException {

		final CharacteristicFunction modelCF = model.apply(getMaturity());

		final double lineOfIntegration = 0.5 * getIntegrationDomainImagUpperBound()+getIntegrationDomainImagLowerBound();
		final DoubleUnaryOperator integrand = new DoubleUnaryOperator() {
			@Override
			public double applyAsDouble(final double real) {
				final Complex z = new Complex(real,lineOfIntegration);
				return modelCF.apply(z.negate()).multiply(AbstractFourierTransformProduct.this.apply(z)).getReal();
			}
		};

		final RealIntegral integrator = new SimpsonRealIntegrator(-100.0, 100.0, 20000, true);

		return integrator.integrate(integrand) / 2.0 / Math.PI;
	}

	/* (non-Javadoc)
	 * @see net.finmath.fouriermethod.products.FourierTransformProduct#getMaturity()
	 */
	@Override
	public abstract double getMaturity();

	/* (non-Javadoc)
	 * @see net.finmath.fouriermethod.products.FourierTransformProduct#getIntegrationDomainImagLowerBound()
	 */
	@Override
	public abstract double getIntegrationDomainImagLowerBound();

	/* (non-Javadoc)
	 * @see net.finmath.fouriermethod.products.FourierTransformProduct#getIntegrationDomainImagUpperBound()
	 */
	@Override
	public abstract double getIntegrationDomainImagUpperBound();
}
