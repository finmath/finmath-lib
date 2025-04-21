package net.finmath.optimizer.parameter;

import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;

public class ParameterTransfromSimpleBounded implements ParameterTransfrom {

	private final RandomVariable parameterLowerBound[];
	private final RandomVariable parameterUpperBound[];

	public ParameterTransfromSimpleBounded(RandomVariable[] parameterLowerBound, RandomVariable[] parameterUpperBound) {
		super();
		this.parameterLowerBound = parameterLowerBound;
		this.parameterUpperBound = parameterUpperBound;
	}

	public ParameterTransfromSimpleBounded(double[] parameterLowerBound, double[] parameterUpperBound) {
		this(Scalar.arrayOf(parameterLowerBound), Scalar.arrayOf(parameterUpperBound));
	}

	@Override
	public RandomVariable[] getModelParametersFrom(RandomVariable[] optimizerParameters) {
		final RandomVariable[] parametersTransformed = new RandomVariable[optimizerParameters.length];
		// From (-infty, infty) to (0,infty)
		for(int i=0; i<optimizerParameters.length; i++) {
			parametersTransformed[i] = optimizerParameters[i].mult(-1).exp();
		}
		// If parameter should be monotone
		for(int i=1; i<optimizerParameters.length; i++) {
			//			parametersTransformed[i] = parametersTransformed[i-1].add(parametersTransformed[i]);
		}
		// From (0,infty) to (a,b)
		for(int i=0; i<optimizerParameters.length; i++) {
			parametersTransformed[i] = parametersTransformed[i].mult(-1).exp().mult(parameterUpperBound[i].sub(parameterLowerBound[i])).add(parameterLowerBound[i]);
		}
		return parametersTransformed;
	}

	@Override
	public RandomVariable[] getOptimizerParametersFrom(RandomVariable[] modelParameters) {
		final RandomVariable[] parameters = new RandomVariable[modelParameters.length];
		// From (a,b) to (0,infty)
		for(int i=0; i<modelParameters.length; i++) {
			parameters[i] = modelParameters[i].sub(parameterLowerBound[i]).div(parameterUpperBound[i].sub(parameterLowerBound[i])).log().mult(-1);
		}
		// If parameter should be monotone
		for(int i=parameters.length-1; i>=1; i--) {
			//			parameters[i] = parameters[i] - parameters[i-1];
		}
		// From (0,infty) to (-infty, infty)
		for(int i=0; i<parameters.length; i++) {
			parameters[i] = parameters[i].log().mult(-1);
		}
		return parameters;
	}
}
