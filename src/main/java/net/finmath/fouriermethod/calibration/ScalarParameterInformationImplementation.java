package net.finmath.fouriermethod.calibration;

/**
 * This class tells us if a parameter has to be calibrated and if it is constrained.
 * @author Alessandro Gnoatto
 *
 */
public class ScalarParameterInformationImplementation implements ScalarParameterInformation{

	private final boolean isParameterToCalibrate;
	private final ScalarConstraint constraint;

	/**
	 * Full constructor.
	 * @param isParameterToCalibrate
	 * @param constraint
	 */
	public ScalarParameterInformationImplementation(boolean isParameterToCalibrate, ScalarConstraint constraint) {
		super();
		this.isParameterToCalibrate = isParameterToCalibrate;
		this.constraint = constraint;
	}

	/**
	 * Constructs an unconstrained parameter.
	 * @param isParameterToCalibrate
	 */
	public ScalarParameterInformationImplementation(boolean isParameterToCalibrate) {
		super();
		this.isParameterToCalibrate = isParameterToCalibrate;
		this.constraint = new Unconstrained();
	}

	/**
	 * Constructs a parameter that needs to be calibrated.
	 * @param constraint
	 */
	public ScalarParameterInformationImplementation(ScalarConstraint constraint) {
		super();
		this.isParameterToCalibrate = true;
		this.constraint = constraint;
	}

	@Override
	public boolean getIsParameterToCalibrate() {
		return isParameterToCalibrate;
	}

	@Override
	public ScalarConstraint getConstraint() {
		return constraint;
	}
}
