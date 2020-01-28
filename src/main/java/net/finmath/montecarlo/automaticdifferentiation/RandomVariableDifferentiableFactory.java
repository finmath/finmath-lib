package net.finmath.montecarlo.automaticdifferentiation;

import net.finmath.stochastic.RandomVariable;

public interface RandomVariableDifferentiableFactory {

	RandomVariableDifferentiable createRandomVariable(double value);

	RandomVariableDifferentiable createRandomVariable(double time, double value);

	RandomVariableDifferentiable createRandomVariable(double time, double[] values);

	RandomVariable createRandomVariableNonDifferentiable(double time, double value);

	RandomVariable createRandomVariableNonDifferentiable(double time, double[] values);

}