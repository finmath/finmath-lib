/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 23.03.2014
 */

package net.finmath.fouriermethod.calibration.models;

import net.finmath.fouriermethod.models.HestonModel;
import net.finmath.fouriermethod.models.ProcessCharacteristicFunctionInterface;
import net.finmath.modelling.ModelDescriptor;
import net.finmath.modelling.descriptor.HestonModelDescriptor;

/**
 * This class is creates new instances of HestonModel and communicates with the optimization algorithm.
 *
 * @author Alessandro Gnoatto
 */
public class CalibrableHestonModel implements  CalibrableProcessInterface {
	private final HestonModelDescriptor descriptor;

	public CalibrableHestonModel(HestonModelDescriptor descriptor) {
		super();
		this.descriptor = descriptor;
	}

	@Override
	public ProcessCharacteristicFunctionInterface getCloneForModifiedParameters(double[] parameters) {
		double volatility = parameters[0];
		double theta = parameters[1];
		double kappa = parameters[2];
		double xi = parameters[3];
		double rho = parameters[4];
		return new HestonModel(descriptor.getInitialValue(),
				descriptor.getDiscountCurveForForwardRate(),
				volatility, descriptor.getDiscountCurveForForwardRate(), theta, kappa, xi, rho);
	}

	@Override
	public ModelDescriptor getModelDescriptor() {
		return descriptor;
	}

}
