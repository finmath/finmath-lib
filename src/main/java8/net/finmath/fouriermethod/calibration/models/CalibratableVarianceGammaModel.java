package net.finmath.fouriermethod.calibration.models;

import net.finmath.fouriermethod.calibration.ScalarParameterInformation;
import net.finmath.fouriermethod.calibration.ScalarParameterInformationImplementation;
import net.finmath.fouriermethod.calibration.Unconstrained;
import net.finmath.fouriermethod.models.CharacteristicFunctionModel;
import net.finmath.fouriermethod.models.VarianceGammaModel;
import net.finmath.modelling.ModelDescriptor;
import net.finmath.modelling.descriptor.VarianceGammaModelDescriptor;

/**
 * This class is creates new instances of VarianceGammaModel and communicates with the optimization algorithm.
 *
 * This class provides clones of herself: in such a way the information concerning constraints is not lost.
 *
 * The method getCharacteristicFunction is then passed to the FFT pricing routine.
 *
 * @author Alessandro Gnoatto
 */
public class CalibratableVarianceGammaModel implements CalibratableProcess {
	private final VarianceGammaModelDescriptor descriptor;

	private final ScalarParameterInformation sigmaInfo;
	private final ScalarParameterInformation thetaInfo;
	private final ScalarParameterInformation nuInfo;
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
	 * @param descriptor The model descriptor for the Variance Gamma model.
	 */
	public CalibratableVarianceGammaModel(VarianceGammaModelDescriptor descriptor) {
		super();
		this.descriptor = descriptor;
		this.sigmaInfo = new ScalarParameterInformationImplementation(true, new Unconstrained());
		this.thetaInfo = new ScalarParameterInformationImplementation(true, new Unconstrained());
		this.nuInfo = new ScalarParameterInformationImplementation(true, new Unconstrained());
		this.parameterUpperBounds = extractUpperBounds();
		this.parameterLowerBounds = extractLowerBounds();
	}

	/**
	 *
	 * @param descriptor The model descriptor for the Variance Gamma model.
	 * @param sigmaInfo A constraint for the parameter sigma.
	 * @param thetaInfo A constraint for the parameter theta.
	 * @param nuInfo A constraint for the parameter nu.
	 */
	public CalibratableVarianceGammaModel(VarianceGammaModelDescriptor descriptor, ScalarParameterInformation sigmaInfo,
			ScalarParameterInformation thetaInfo, ScalarParameterInformation nuInfo) {
		this.descriptor = descriptor;
		this.sigmaInfo = sigmaInfo;
		this.thetaInfo = thetaInfo;
		this.nuInfo = nuInfo;
		this.parameterUpperBounds = extractUpperBounds();
		this.parameterLowerBounds = extractLowerBounds();
	}

	@Override
	public CalibratableProcess getCloneForModifiedParameters(double[] parameters) {

		//If the parameters are to be calibrated we update the value, otherwise we use the stored one.
		final double sigma = sigmaInfo.getIsParameterToCalibrate() == true ? sigmaInfo.getConstraint().apply(parameters[0]) : descriptor.getSigma();
		final double theta = thetaInfo.getIsParameterToCalibrate() == true ? thetaInfo.getConstraint().apply(parameters[1]) : descriptor.getTheta();
		final double nu = nuInfo.getIsParameterToCalibrate() == true ? nuInfo.getConstraint().apply(parameters[2]) : descriptor.getNu();

		final VarianceGammaModelDescriptor newDescriptor = new VarianceGammaModelDescriptor(descriptor.getReferenceDate(),
				descriptor.getInitialValue(),descriptor.getDiscountCurveForForwardRate(), descriptor.getDiscountCurveForDiscountRate(),
				sigma, theta, nu);

		return new CalibratableVarianceGammaModel(newDescriptor,sigmaInfo, thetaInfo, nuInfo);
	}

	@Override
	public ModelDescriptor getModelDescriptor() {
		return descriptor;
	}

	@Override
	public CharacteristicFunctionModel getCharacteristicFunctionModel() {
		return new VarianceGammaModel(null, descriptor.getInitialValue(),descriptor.getDiscountCurveForForwardRate(),
				descriptor.getDiscountCurveForDiscountRate(), descriptor.getSigma(), descriptor.getTheta(), descriptor.getNu());
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
		final double[] upperBounds = new double[3];
		final double threshold = 1E6;
		upperBounds[0] = sigmaInfo.getConstraint().getUpperBound() > threshold ? threshold : sigmaInfo.getConstraint().getUpperBound();
		upperBounds[1] = thetaInfo.getConstraint().getUpperBound() > threshold ? threshold : thetaInfo.getConstraint().getUpperBound();
		upperBounds[2] = nuInfo.getConstraint().getUpperBound() > threshold ? threshold : nuInfo.getConstraint().getUpperBound();

		return upperBounds;
	}

	private double[] extractLowerBounds() {
		final double[] upperBounds = new double[3];
		final double threshold = -1E6;
		upperBounds[0] = sigmaInfo.getConstraint().getLowerBound() < threshold ? threshold : sigmaInfo.getConstraint().getLowerBound();
		upperBounds[1] = thetaInfo.getConstraint().getLowerBound() < threshold ? threshold : thetaInfo.getConstraint().getLowerBound();
		upperBounds[2] = nuInfo.getConstraint().getLowerBound() < threshold ? threshold : nuInfo.getConstraint().getLowerBound();

		return upperBounds;
	}
}
