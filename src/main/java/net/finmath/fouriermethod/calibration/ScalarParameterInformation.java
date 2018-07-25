package net.finmath.fouriermethod.calibration;

/**
 * This class tells us if a parameter has to be calibrated and if it is constrained.
 * @author Alessandro Gnoatto
 *
 */
public class ScalarParameterInformation implements ScalarParameterInformationInterface{

	private final boolean isParameterToCalibrate;
	private final ScalarConstraintInterface constraint;

	/**
	 * Full constructor.
	 * @param isParameterToCalibrate
	 * @param constraint
	 */
	public ScalarParameterInformation(boolean isParameterToCalibrate, ScalarConstraintInterface constraint) {
		super();
		this.isParameterToCalibrate = isParameterToCalibrate;
		this.constraint = constraint;
	}

	/**
	 * Constructs an unconstrained parameter.
	 * @param isParameterToCalibrate
	 */
	public ScalarParameterInformation(boolean isParameterToCalibrate) {
		super();
		this.isParameterToCalibrate = isParameterToCalibrate;
		this.constraint = new Unconstrained();
	}

	/**
	 * Constructs a parameter that needs to be calibrated.
	 * @param constraint
	 */
	public ScalarParameterInformation(ScalarConstraintInterface constraint) {
		super();
		this.isParameterToCalibrate = true;
		this.constraint = constraint;
	}

	@Override
	public boolean getIsParameterToCalibrate() {
		return isParameterToCalibrate;
	}

	@Override
	public ScalarConstraintInterface getConstraint() {
		return constraint;
	}
}
