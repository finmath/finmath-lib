package net.finmath.fouriermethod.calibration.models;

import java.util.Arrays;

import net.finmath.fouriermethod.calibration.ScalarParameterInformation;
import net.finmath.fouriermethod.calibration.ScalarParameterInformationImplementation;
import net.finmath.fouriermethod.calibration.Unconstrained;
import net.finmath.fouriermethod.models.MertonModel;
import net.finmath.modelling.ModelDescriptor;
import net.finmath.modelling.descriptor.MertonModelDescriptor;

/**
 * This class is creates new instances of MertonModel and communicates with the optimization algorithm.
 *
 * This class provides clones of herself: in such a way the information concerning constraints is not lost.
 *
 * The method getCharacteristicFunction is then passed to the FFT pricing routine.
 *
 * @author Alessandro Gnoatto
 */
public class CalibratableMertonModel implements  CalibratableProcess{
	private final MertonModelDescriptor descriptor;

	private final ScalarParameterInformation volatilityInfo;
	private final ScalarParameterInformation jumpIntensityInfo;
	private final ScalarParameterInformation jumpSizeMeanInfo;
	private final ScalarParameterInformation jumpSizeStdDevInfo;

	/*
	 * Upper and lower bounds are collected here for convenience:
	 * such vectors are then passed to the factory of the optimization algorithm.
	 * In this way we guarantee consistency between the constraints in the model
	 * and the constraints in the optimizer factory.
	 */
	private final double[] parameterUpperBounds;
	private final double[] parameterLowerBounds;

	/**
	 * Basic constructor where all parameters are to be calibrated.
	 * All parameters are unconstrained.
	 *
	 * @param descriptor The model descriptor for the Merton model.
	 */
	public CalibratableMertonModel(final MertonModelDescriptor descriptor) {
		super();
		this.descriptor = descriptor;
		volatilityInfo = new ScalarParameterInformationImplementation(true, new Unconstrained());
		jumpIntensityInfo = new ScalarParameterInformationImplementation(true, new Unconstrained());
		jumpSizeMeanInfo = new ScalarParameterInformationImplementation(true, new Unconstrained());
		jumpSizeStdDevInfo = new ScalarParameterInformationImplementation(true, new Unconstrained());
		parameterUpperBounds = extractUpperBounds();
		parameterLowerBounds = extractLowerBounds();
	}

	/**
	 * This constructor allows for the specification of constraints.
	 * This is very liberal since we can impose different types of constraints.
	 * The choice on the parameters to be applied is left to the user.
	 * This implies that he user could create Merton models which are not meaningful.
	 *
	 * @param descriptor The model descriptor for the Merton model.
	 * @param volatilityInfo The volatility constraint.
	 * @param jumpIntensityInfo The constraint for the jump intensity parameter.
	 * @param jumpSizeMeanInfo The constraint for the jump size mean parameter.
	 * @param jumpSizeStdDevInfo The constraint for the jump standard deviation parameter.
	 */
	public CalibratableMertonModel(final MertonModelDescriptor descriptor, final ScalarParameterInformation volatilityInfo,
			final ScalarParameterInformation jumpIntensityInfo, final ScalarParameterInformation jumpSizeMeanInfo,
			final ScalarParameterInformation jumpSizeStdDevInfo) {
		super();
		this.descriptor = descriptor;
		this.volatilityInfo = volatilityInfo;
		this.jumpIntensityInfo = jumpIntensityInfo;
		this.jumpSizeMeanInfo = jumpSizeMeanInfo;
		this.jumpSizeStdDevInfo = jumpSizeStdDevInfo;
		parameterUpperBounds = extractUpperBounds();
		parameterLowerBounds = extractLowerBounds();
	}


	@Override
	public CalibratableProcess getCloneForModifiedParameters(final double[] parameters) {
		//If the parameters are to be calibrated we update the value, otherwise we use the stored one.
		final double volatility = volatilityInfo.getIsParameterToCalibrate() == true ? volatilityInfo.getConstraint().apply(parameters[0]) : descriptor.getVolatility();
		final double jumpIntensity = jumpIntensityInfo.getIsParameterToCalibrate() == true ? jumpIntensityInfo.getConstraint().apply(parameters[1]) : descriptor.getJumpIntensity();
		final double jumpSizeMean = jumpSizeMeanInfo.getIsParameterToCalibrate() == true ? jumpSizeMeanInfo.getConstraint().apply(parameters[2]) : descriptor.getJumpSizeMean();
		final double jumpSizeStdDev = jumpSizeStdDevInfo.getIsParameterToCalibrate() == true ? jumpSizeStdDevInfo.getConstraint().apply(parameters[3]) : descriptor.getJumpSizeStdDev();

		final MertonModelDescriptor newDescriptor = new MertonModelDescriptor(descriptor.getReferenceDate(),
				descriptor.getInitialValue(),descriptor.getDiscountCurveForForwardRate(),descriptor.getDiscountCurveForDiscountRate(),
				volatility,jumpIntensity,jumpSizeMean,jumpSizeStdDev);

		return new CalibratableMertonModel(newDescriptor,volatilityInfo,jumpIntensityInfo,jumpSizeMeanInfo,jumpSizeStdDevInfo);
	}

	@Override
	public ModelDescriptor getModelDescriptor() {
		return descriptor;
	}

	@Override
	public MertonModel getCharacteristicFunctionModel() {
		return new MertonModel(descriptor.getReferenceDate(),descriptor.getInitialValue(),descriptor.getDiscountCurveForForwardRate(),
				descriptor.getDiscountCurveForDiscountRate(),descriptor.getVolatility(),
				descriptor.getJumpIntensity(),descriptor.getJumpSizeMean(),descriptor.getJumpSizeStdDev());
	}

	@Override
	public double[] getParameterLowerBounds() {
		return parameterLowerBounds;
	}

	@Override
	public double[] getParameterUpperBounds() {
		return parameterUpperBounds;
	}

	private double[] extractUpperBounds() {
		final double[] upperBounds = new double[4];
		final double threshold = 1E6;
		upperBounds[0] = volatilityInfo.getConstraint().getUpperBound() > threshold ? threshold : volatilityInfo.getConstraint().getUpperBound();
		upperBounds[1] = jumpIntensityInfo.getConstraint().getUpperBound() > threshold ? threshold : jumpIntensityInfo.getConstraint().getUpperBound();
		upperBounds[2] = jumpSizeMeanInfo.getConstraint().getUpperBound() > threshold ? threshold : jumpSizeMeanInfo.getConstraint().getUpperBound();
		upperBounds[3] = jumpSizeStdDevInfo.getConstraint().getUpperBound() > threshold ? threshold : jumpSizeStdDevInfo.getConstraint().getUpperBound();

		return upperBounds;
	}

	private double[] extractLowerBounds() {
		final double[] upperBounds = new double[4];
		final double threshold = -1E6;
		upperBounds[0] = volatilityInfo.getConstraint().getLowerBound() < threshold ? threshold : volatilityInfo.getConstraint().getLowerBound();
		upperBounds[1] = jumpIntensityInfo.getConstraint().getLowerBound() < threshold ? threshold : jumpIntensityInfo.getConstraint().getLowerBound();
		upperBounds[2] = jumpSizeMeanInfo.getConstraint().getLowerBound() < threshold ? threshold : jumpSizeMeanInfo.getConstraint().getLowerBound();
		upperBounds[3] = jumpSizeStdDevInfo.getConstraint().getLowerBound() < threshold ? threshold : jumpSizeStdDevInfo.getConstraint().getLowerBound();

		return upperBounds;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "CalibratableMertonModel [descriptor=" + descriptor + ", volatilityInfo=" + volatilityInfo
				+ ", jumpIntensityInfo=" + jumpIntensityInfo + ", jumpSizeMeanInfo=" + jumpSizeMeanInfo
				+ ", jumpSizeStdDevInfo=" + jumpSizeStdDevInfo + ", parameterUpperBounds="
				+ Arrays.toString(parameterUpperBounds) + ", parameterLowerBounds="
				+ Arrays.toString(parameterLowerBounds) + "]";
	}

}
