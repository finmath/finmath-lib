/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 23.03.2014
 */

package net.finmath.math.integration;

import net.finmath.compatibility.java.util.function.DoubleUnaryOperator;

/**
 * Interface for real integral. An integral is a map which
 * maps a DoubleUnaryOperator to a double.
 * 
 * This is a functional interface.
 * 
 * @author Christian Fries
 */
public interface RealIntegralInterface {

	double integrate(DoubleUnaryOperator integrand);
	
}
