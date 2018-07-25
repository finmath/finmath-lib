package net.finmath.fouriermethod.calibration;

/**
 * Base interface for scalar parameter constraints.
 * 
 * @author Alessandro Gnoatto
 *
 */
public interface ScalarConstraintInterface extends ConstraintInterface{

	/**
	 * Forces the parameter to respect a certain condition.
	 * @param parameterToTest
	 * @return the parameter after application of the constraint.
	 */
	double applyConstraint(double parameterToTest);

	/**
	 * Returns the lower bound, possibly given by Double.NEGATIVE_INFINITY.
	 * @return the lower bound.
	 */
	double getLowerBound();

	/**
	 * Returns the upper bound, possibly given by Double.POSITIVE_INFINITY.
	 * @return the upper bound.
	 */
	double getUpperBound();

}
