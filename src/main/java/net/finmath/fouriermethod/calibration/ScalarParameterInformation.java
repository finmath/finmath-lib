package net.finmath.fouriermethod.calibration;

/**
 * An interface representing a scalar parameter.
 *
 * @author Alessandro Gnoatto
 */
public interface ScalarParameterInformation extends ParameterInformation{

	/**
	 * Boolean flag for parameters that need to be calibrated.
	 *
	 * @return true if the parameter must be calibrated.
	 */
	boolean getIsParameterToCalibrate();

	/**
	 * Returns the constraint.
	 *
	 * @return the constraint.
	 */
	ScalarConstraint getConstraint();

}
