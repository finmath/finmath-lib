package net.finmath.fouriermethod.calibration;

/**
 * This class tells us if a parameter has to be calibrated and if it is constrained.
 *
 * @author Alessandro Gnoatto
 */
public class ScalarParameterInformationImplementation implements ScalarParameterInformation{

	private final boolean isParameterToCalibrate;
	private final ScalarConstraint constraint;

	/**
	 * Constructs a parameter.
	 *
	 * @param isParameterToCalibrate If true, the parameter will be varied during calibration.
	 * @param constraint A constraint for the parameter.
	 */
	public ScalarParameterInformationImplementation(final boolean isParameterToCalibrate, final ScalarConstraint constraint) {
		super();
		this.isParameterToCalibrate = isParameterToCalibrate;
		this.constraint = constraint;
	}

	/**
	 * Constructs an unconstrained parameter.
	 *
	 * @param isParameterToCalibrate If true, the parameter will be varied during calibration.
	 */
	public ScalarParameterInformationImplementation(final boolean isParameterToCalibrate) {
		super();
		this.isParameterToCalibrate = isParameterToCalibrate;
		constraint = new Unconstrained();
	}

	/**
	 * Constructs a parameter that needs to be calibrated.
	 * @param constraint A constraint for the parameter.
	 */
	public ScalarParameterInformationImplementation(final ScalarConstraint constraint) {
		super();
		isParameterToCalibrate = true;
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
