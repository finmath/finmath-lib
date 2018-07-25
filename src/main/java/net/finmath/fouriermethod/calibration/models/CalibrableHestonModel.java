package net.finmath.fouriermethod.calibration.models;

import net.finmath.fouriermethod.calibration.ScalarParameterInformation;
import net.finmath.fouriermethod.calibration.ScalarParameterInformationInterface;
import net.finmath.fouriermethod.calibration.Unconstrained;
import net.finmath.fouriermethod.models.HestonModel;
import net.finmath.modelling.ModelDescriptor;
import net.finmath.modelling.descriptor.HestonModelDescriptor;

/**
 * This class is creates new instances of HestonModel and communicates with the optimization algorithm.
 * 
 * This class provides clones of herself: in such a way the information concerning constraints is not lost.
 * 
 * The method getCharacteristicFunction is then passed to the FFT pricing routine.
 *
 * @author Alessandro Gnoatto
 * 
 */
public class CalibrableHestonModel implements  CalibrableProcessInterface {
	private final HestonModelDescriptor descriptor;

	private final ScalarParameterInformationInterface volatilityInfo;
	private final ScalarParameterInformationInterface thetaInfo;
	private final ScalarParameterInformationInterface kappaInfo;
	private final ScalarParameterInformationInterface xiInfo;
	private final ScalarParameterInformationInterface rhoInfo;
	private final boolean applyFellerConstraint;

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
	 * @param descriptor
	 */
	public CalibrableHestonModel(HestonModelDescriptor descriptor) {
		super();
		this.descriptor = descriptor;
		this.volatilityInfo = new ScalarParameterInformation(true, new Unconstrained());
		this.thetaInfo =new ScalarParameterInformation(true, new Unconstrained());
		this.kappaInfo = new ScalarParameterInformation(true, new Unconstrained());
		this.xiInfo = new ScalarParameterInformation(true, new Unconstrained());
		this.rhoInfo = new ScalarParameterInformation(true, new Unconstrained());
		this.applyFellerConstraint = false;
		this.parameterUpperBounds = extractUpperBounds();
		this.parameterLowerBounds = extractLowerBounds();
	}

	/**
	 * This constructor allows for the specification of constraints.
	 * This is very liberal since we can impose different types of constraints.
	 * The choice on the parameters to be applied is left to the user.
	 * This implies that he user could create Heston models which are not admissible in the sense of Duffie Filipovic and Schachermayer (2003).
	 * For example, it is up to the user to impose constraints such that the product of kappa and theta is positive.
	 * 
	 * @param descriptor
	 * @param volatilityConstraint
	 * @param thetaConstraint
	 * @param kappaConstraint
	 * @param xiConstraint
	 * @param rhoConstraint
	 * @param applyFellerConstraint
	 */
	public CalibrableHestonModel(HestonModelDescriptor descriptor, ScalarParameterInformationInterface volatilityConstraint,
			ScalarParameterInformationInterface thetaConstraint, ScalarParameterInformationInterface kappaConstraint, ScalarParameterInformationInterface xiConstraint,
			ScalarParameterInformationInterface rhoConstraint, boolean applyFellerConstraint) {
		this.descriptor = descriptor;
		this.volatilityInfo = volatilityConstraint;
		this.thetaInfo = thetaConstraint;
		this.kappaInfo = kappaConstraint;
		this.xiInfo = xiConstraint;
		this.rhoInfo = rhoConstraint;
		this.applyFellerConstraint = applyFellerConstraint;
		this.parameterUpperBounds = extractUpperBounds();
		this.parameterLowerBounds = extractLowerBounds();
	}

	@Override
	public CalibrableHestonModel getCloneForModifiedParameters(double[] parameters) {

		//If the parameters are to be calibrated we update the value, otherwise we use the stored one.
		double volatility = volatilityInfo.getIsParameterToCalibrate() == true ? volatilityInfo.getConstraint().applyConstraint(parameters[0]) : descriptor.getVolatility();
		double theta = thetaInfo.getIsParameterToCalibrate() == true ? thetaInfo.getConstraint().applyConstraint(parameters[1]) : descriptor.getTheta();
		double kappa = kappaInfo.getIsParameterToCalibrate() == true ? kappaInfo.getConstraint().applyConstraint(parameters[2]) : descriptor.getKappa();
		double xi = xiInfo.getIsParameterToCalibrate() == true ? xiInfo.getConstraint().applyConstraint(parameters[3]) : descriptor.getXi();
		double rho = rhoInfo.getIsParameterToCalibrate() == true ? rhoInfo.getConstraint().applyConstraint(parameters[4]) : descriptor.getRho();

		if(applyFellerConstraint && 2*kappa*theta < xi*xi) {
			//bump long term volatility so that the Feller test is satisfied.
			theta = xi*xi / (2 * kappa) + 1E-9;
		}else {
			//nothing to do;
		}

		HestonModelDescriptor newDescriptor = new HestonModelDescriptor(descriptor.getReferenceDate(),
				descriptor.getInitialValue(),descriptor.getDiscountCurveForForwardRate(), descriptor.getDiscountCurveForForwardRate(),
				volatility, theta, kappa, xi, rho);

		return new CalibrableHestonModel(newDescriptor,this.volatilityInfo,this.thetaInfo,this.kappaInfo,this.xiInfo,this.rhoInfo,this.applyFellerConstraint);
	}

	@Override
	public ModelDescriptor getModelDescriptor() {
		return descriptor;
	}

	@Override
	public HestonModel getCharacteristiFunction() {
		return new HestonModel(descriptor.getInitialValue(),descriptor.getDiscountCurveForForwardRate(),
				descriptor.getVolatility(),descriptor.getDiscountCurveForForwardRate(),
				descriptor.getTheta(),descriptor.getKappa(),descriptor.getXi(),descriptor.getRho());
	}

	@Override
	public double[] getParameterUpperBounds() {
		return parameterUpperBounds;
	}

	@Override
	public double[] getParameterLowerBounds() {
		return parameterLowerBounds;
	}

	private double[] extractUpperBounds() {
		double[] upperBounds = new double[5];
		double threshold = 1E6;
		upperBounds[0] = volatilityInfo.getConstraint().getUpperBound() > threshold ? threshold : volatilityInfo.getConstraint().getUpperBound();
		upperBounds[1] = thetaInfo.getConstraint().getUpperBound() > threshold ? threshold : thetaInfo.getConstraint().getUpperBound();
		upperBounds[2] = kappaInfo.getConstraint().getUpperBound() > threshold ? threshold : kappaInfo.getConstraint().getUpperBound();
		upperBounds[3] = xiInfo.getConstraint().getUpperBound() > threshold ? threshold : xiInfo.getConstraint().getUpperBound();
		upperBounds[4] = rhoInfo.getConstraint().getUpperBound() > threshold ? threshold : rhoInfo.getConstraint().getUpperBound();

		return upperBounds;
	}

	private double[] extractLowerBounds() {
		double[] upperBounds = new double[5];
		double threshold = -1E6;
		upperBounds[0] = volatilityInfo.getConstraint().getLowerBound() < threshold ? threshold : volatilityInfo.getConstraint().getLowerBound();
		upperBounds[1] = thetaInfo.getConstraint().getLowerBound() < threshold ? threshold : thetaInfo.getConstraint().getLowerBound();
		upperBounds[2] = kappaInfo.getConstraint().getLowerBound() < threshold ? threshold : kappaInfo.getConstraint().getLowerBound();
		upperBounds[3] = xiInfo.getConstraint().getLowerBound() < threshold ? threshold : xiInfo.getConstraint().getLowerBound();
		upperBounds[4] = rhoInfo.getConstraint().getLowerBound() < threshold ? threshold : rhoInfo.getConstraint().getLowerBound();

		return upperBounds;
	}

}
