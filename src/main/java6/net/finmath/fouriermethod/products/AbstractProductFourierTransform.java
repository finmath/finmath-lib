/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 23.03.2014
 */

package net.finmath.fouriermethod.products;

import net.finmath.compatibility.java.util.function.DoubleUnaryOperator;
import net.finmath.fouriermethod.CharacteristicFunctionInterface;
import net.finmath.fouriermethod.models.ProcessCharacteristicFunctionInterface;
import net.finmath.integration.RealIntegralInterface;
import net.finmath.integration.SimpsonRealIntegrator;

import org.apache.commons.math3.complex.Complex;

/**
 * @author Christian Fries
 *
 */
public abstract class AbstractProductFourierTransform implements CharacteristicFunctionInterface {

	/**
	 * This method returns the value random variable of the product within the specified model, evaluated at a given evalutationTime.
	 * Note: For a lattice this is often the value conditional to evalutationTime, for a Monte-Carlo simulation this is the (sum of) value discounted to evaluation time.
	 * Cashflows prior evaluationTime are not considered.
	 * 
	 * @param model The model used to price the product.
	 * @return The random variable representing the value of the product discounted to evaluation time
	 */
	public double getValue(ProcessCharacteristicFunctionInterface model) {

		final CharacteristicFunctionInterface modelCF   = model.apply(getMaturity());
        final CharacteristicFunctionInterface productCF = this;

		final double lineOfIntegration = 0.5 * getIntegrationDomainImagUpperBound()+getIntegrationDomainImagLowerBound();
		DoubleUnaryOperator integrand = new DoubleUnaryOperator() {
            @Override
            public double applyAsDouble(double real) {
                Complex z = new Complex(real,lineOfIntegration);
                return modelCF.apply(z.negate()).multiply(productCF.apply(z)).getReal();
            }
        };

		RealIntegralInterface integrator = new SimpsonRealIntegrator(-100.0, 100.0, 20000, true);

		return integrator.integrate(integrand) / 2.0 / Math.PI;
	}

	/**
	 * Return the maturity of the associated payoff.
	 * 
	 * @return The maturity of the associated payoff.
	 */
	public abstract double getMaturity();
	
	/**
	 * Return the lower bound of the imaginary part of the domain where
	 * the characteristic function can be integrated.
	 * 
	 * @return the lower bound of the imaginary part of the domain of integration.
	 */
	public abstract double getIntegrationDomainImagLowerBound();
	
	/**
	 * Return the upper bound of the imaginary part of the domain where
	 * the characteristic function can be integrated.
	 * 
	 * @return the upper bound of the imaginary part of the domain of integration.
	 */
	public abstract double getIntegrationDomainImagUpperBound();
}
