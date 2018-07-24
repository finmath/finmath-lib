package net.finmath.fouriermethod.calibration.models;

import net.finmath.fouriermethod.models.ProcessCharacteristicFunctionInterface;
import net.finmath.modelling.ModelDescriptor;

/**
 * Every class implementing this interface communicates with the calibration routine by providing
 * clones of the model with changed parameters. A model descriptor is stored to infer parameters which are not
 * calibrated such as market observables.
 * 
 * We are decorating every characteristic function with the getCloneForModifiedParameters withouth touching
 * the existing classes providing the computation of the characteristic function.
 * 
 * Suitable specifications of getCloneForModifiedParameters can be employed to introduce e.g. non-linear constraints.
 * E.g. it is possible to force the Feller condition in the Heston model by providing a suitable implementation of
 * this method.
 * 
 * @author Alessandro Gnoatto
 *
 */
public interface CalibrableProcessInterface {
	
	/**
	 * Calibration substitutes in the model the parameters of the process with calibrated ones.
	 * Market observables such as the initial stock value should not be changed.
	 * 
	 * @param parameters The set of parameters to be used for the new model clone.
	 * @return a clone of the original model with modified parameters.
	 */
	ProcessCharacteristicFunctionInterface getCloneForModifiedParameters(double[] parameters);
	
	/**
	 * Every class implementing this interface must contain a ModelDescriptor from which we can create
	 * some concrete model.
	 * 
	 * @return The model descriptor.
	 */
	ModelDescriptor getModelDescriptor();

}
