package net.finmath.montecarlo;

import net.finmath.stochastic.RandomVariable;

public interface RandomVariableFactory {

	RandomVariable createRandomVariable(double value);

	RandomVariable createRandomVariable(double time, double value);

	RandomVariable createRandomVariable(double time, double[] values);

	RandomVariable[] createRandomVariableArray(double[] values);

	RandomVariable[][] createRandomVariableMatrix(double[][] values);

}