package net.finmath.fouriermethod.calibration;

/**
 * Negativity constraint for calibration parameters.
 *
 * @author Alessandro Gnoatto
 *
 */
public class NegativityConstraint extends BoundConstraint {

	public NegativityConstraint() {
		super(Double.NEGATIVE_INFINITY, 0.0);
	}

	@Override
	public double apply(final double parameterToTest) {
		return -Math.abs(parameterToTest);
	}

}
