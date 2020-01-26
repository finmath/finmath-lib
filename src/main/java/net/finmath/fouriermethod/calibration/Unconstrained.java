package net.finmath.fouriermethod.calibration;

/**
 * Absence of constraints.
 *
 * @author Alessandro Gnoatto
 *
 */
public class Unconstrained extends BoundConstraint{

	public Unconstrained() {
		super(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
	}

	@Override
	public double apply(final double parameterToTest) {
		return parameterToTest;
	}

}
