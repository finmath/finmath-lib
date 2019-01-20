/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2004
 */
package net.finmath.montecarlo;

import java.io.Serializable;

import net.finmath.stochastic.RandomVariable;

/**
 *
 * @author Christian Fries
 * @version 1.0
 */
public abstract class AbstractRandomVariableFactory implements Serializable{

	private static final long serialVersionUID = -4412332958142580025L;

	public RandomVariable createRandomVariable(double value) {
		return createRandomVariable(Double.NEGATIVE_INFINITY, value);
	}

	public abstract RandomVariable createRandomVariable(double time, double value);

	public abstract RandomVariable createRandomVariable(double time, double[] values);

	public RandomVariable[] createRandomVariableArray(double[] values) {
		RandomVariable[] valuesAsRandomVariables = new RandomVariable[values.length];
		for(int i=0; i<values.length; i++) {
			valuesAsRandomVariables[i] = this.createRandomVariable(Double.NEGATIVE_INFINITY, values[i]);
		}
		return valuesAsRandomVariables;
	}

	public RandomVariable[][] createRandomVariableMatrix(double[][] values) {
		RandomVariable[][] valuesAsRandomVariables = new RandomVariable[values.length][];
		for(int i=0; i<values.length; i++) {
			valuesAsRandomVariables[i] = new RandomVariable[values[i].length];
			for(int j=0; j<values[i].length; j++) {
				valuesAsRandomVariables[i][j] = this.createRandomVariable(Double.NEGATIVE_INFINITY, values[i][j]);
			}
		}
		return valuesAsRandomVariables;
	}
}
