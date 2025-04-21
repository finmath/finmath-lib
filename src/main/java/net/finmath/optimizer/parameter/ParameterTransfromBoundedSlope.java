package net.finmath.optimizer.parameter;

import net.finmath.stochastic.RandomVariable;

public class ParameterTransfromBoundedSlope implements ParameterTransfrom {

	private final RandomVariable parameterLowerBound[];
	private final RandomVariable parameterUpperBound[];

	private final RandomVariable parameterSlopeLowerBound[];
	private final RandomVariable parameterSlopeUpperBound[];

	public ParameterTransfromBoundedSlope(RandomVariable[] parameterLowerBound, RandomVariable[] parameterUpperBound,
			RandomVariable[] parameterSlopeLowerBound, RandomVariable[] parameterSlopeUpperBound) {
		super();
		this.parameterLowerBound = parameterLowerBound;
		this.parameterUpperBound = parameterUpperBound;
		this.parameterSlopeLowerBound = parameterSlopeLowerBound;
		this.parameterSlopeUpperBound = parameterSlopeUpperBound;
	}

	@Override
	public RandomVariable[] getModelParametersFrom(RandomVariable[] optimizerParameters) {
		RandomVariable[] modelParameters = new RandomVariable[optimizerParameters.length];
		// From (-infty, infty) to (0,infty)
		for(int i=0; i<optimizerParameters.length; i++) {
			modelParameters[i] = optimizerParameters[i].mult(-1).exp();
		}
		// From (0,infty) to (a,b)
		for(int i=0; i<optimizerParameters.length; i++) {
			RandomVariable lowerBound = parameterLowerBound[i];
			RandomVariable upperBound = parameterUpperBound[i];
			if(i > 0) {
				if(parameterSlopeLowerBound != null) {
					lowerBound = lowerBound.floor(modelParameters[i-1].add(parameterSlopeLowerBound[i]));
				}
				if(parameterSlopeUpperBound != null) {
					upperBound = upperBound.cap(modelParameters[i-1].add(parameterSlopeUpperBound[i]));
				}
			}
			modelParameters[i] = modelParameters[i].mult(-1).exp().mult(upperBound.sub(lowerBound)).add(lowerBound);
		}
		return modelParameters;
	}

	@Override
	public RandomVariable[] getOptimizerParametersFrom(RandomVariable[] modelParameters) {
		final RandomVariable[] optimizerParameters = new RandomVariable[modelParameters.length];
		// From (a,b) to (0,infty)
		for(int i=0; i<modelParameters.length; i++) {
			RandomVariable lowerBound = parameterLowerBound[i];
			RandomVariable upperBound = parameterUpperBound[i];
			if(i > 0) {
				if(parameterSlopeLowerBound != null) {
					lowerBound = lowerBound.floor(modelParameters[i-1].add(parameterSlopeLowerBound[i]));
				}
				if(parameterSlopeUpperBound != null) {
					upperBound = upperBound.cap(modelParameters[i-1].add(parameterSlopeUpperBound[i]));
				}	
			}

			optimizerParameters[i] = modelParameters[i].sub(lowerBound).div(upperBound.sub(lowerBound)).log().mult(-1);
		}
		// From (0,infty) to (-infty, infty)
		for(int i=0; i<optimizerParameters.length; i++) {
			optimizerParameters[i] = optimizerParameters[i].log().mult(-1);
		}
		return optimizerParameters;
	}
}
