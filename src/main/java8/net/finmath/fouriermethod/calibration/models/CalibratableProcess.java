package net.finmath.fouriermethod.calibration.models;

import net.finmath.fouriermethod.models.CharacteristicFunctionModel;
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
public interface CalibratableProcess {
	/**
	 * Calibration substitutes in the model the parameters of the process with calibrated ones.
	 * Market observables such as the initial stock value should not be changed.
	 *
	 * @param parameters The new parameters.
	 * @return a clone of the original model with modified parameters.
	 */
	CalibratableProcess getCloneForModifiedParameters(double[] parameters);

	/**
	 * Every class implementing this interface must contain a ModelDescriptor from which we can create some concrete model.
	 *
	 * @return The descriptor for this model.
	 */
	ModelDescriptor getModelDescriptor();

	/**
	 * Directly returns the characteristic function.
	 *
	 * @return the characteristic function
	 */
	CharacteristicFunctionModel getCharacteristicFunctionModel();

	/**
	 * Extracts parameter lower bounds for the optimizer factory.
	 *
	 * @return parameter lower bounds for the optimizer factory.
	 */
	double[] getParameterLowerBounds();

	/**
	 * Extracts parameter upper bounds for the optimizer factory.
	 *
	 * @return parameter upper bounds for the optimizer factory.
	 */
	double[] getParameterUpperBounds();

}
