package net.finmath.fouriermethod.calibration;

/**
 * A class applying a bound constraint to a parameter.
 *
 * @author Alessandro Gnoatto
 *
 */
public class BoundConstraint implements ScalarConstraint {
	private final double lowerBound;
	private final double upperBound;

	public BoundConstraint(final double lowerBound, final double upperBound) {
		super();
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
	}

	/**
	 * Return the lower bound.
	 * @return the lower bound.
	 */
	@Override
	public double getLowerBound() {
		return lowerBound;
	}

	/**
	 * Return the upper bound.
	 * @return the upper bound.
	 */
	@Override
	public double getUpperBound() {
		return upperBound;
	}

	@Override
	public double apply(final double parameterToTest) {

		if(parameterToTest > upperBound || parameterToTest < lowerBound) {
			final double u = 1.0/(Math.exp(parameterToTest)+1.0); //maps R to  to [0,1];

			return lowerBound + u*(upperBound - lowerBound); //maps from [0,1] to [lowerBound, upperBound]

		}else {
			return parameterToTest;
		}
	}

}
