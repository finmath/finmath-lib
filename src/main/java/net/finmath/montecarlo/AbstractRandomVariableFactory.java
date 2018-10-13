/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2004
 */
package net.finmath.montecarlo;

import java.io.Serializable;

import net.finmath.stochastic.RandomVariableInterface;

/**
 *
 * @author Christian Fries
 * @version 1.0
 */
public abstract class AbstractRandomVariableFactory implements Serializable{

	private static final long serialVersionUID = -4412332958142580025L;

	public RandomVariableInterface createRandomVariable(double value) {
		return createRandomVariable(Double.NEGATIVE_INFINITY, value);
	}

	public abstract RandomVariableInterface createRandomVariable(double time, double value);

	public abstract RandomVariableInterface createRandomVariable(double time, double[] values);
}
