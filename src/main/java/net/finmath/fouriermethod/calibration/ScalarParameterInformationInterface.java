package net.finmath.fouriermethod.calibration;

/**
 * An interface representing a scalar parameter.
 * 
 * @author Alessandro Gnoatto
 *
 */
public interface ScalarParameterInformationInterface extends ParameterInformationInterface{

	/**
	 * boolean flag for parameters that need to be calibrated.
	 * @return true if the parameter must be calibrated.
	 */
	public boolean getIsParameterToCalibrate();

	/**
	 * Returns the constraint.
	 * @return the constraint.
	 */
	public ScalarConstraintInterface getConstraint();

}
