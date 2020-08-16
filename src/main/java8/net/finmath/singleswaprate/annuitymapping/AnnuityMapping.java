package net.finmath.singleswaprate.annuitymapping;


/**
 * An interface for calsses providing annuity mappings. An annuity mapping allows to treat swap annuity as a function of the swap rate. More precisely it is a function \( \alpha \)
 * such that
 * \[
 *		\alpha(x) = E^A [ \frac{A(0)}{A(T)} | S(T) = x ] \, .
 * \]
 *	Where A is the (froward) annuity and S is the swap rate at the given time.
 *
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public interface AnnuityMapping {

	/**
	 * Implemented types of annuity mappings.
	 *
	 * @author Christian Fries
	 * @author Roland Bachl
	 *
	 */
	enum AnnuityMappingType {
		BASICPITERBARG,
		SIMPLIFIEDLINEAR,
		MULTIPITERBARG,
	}

	/**
	 * Return the value the fraction of annuities take, when the realized swap rate meets the given swap rate.
	 *
	 * @param swapRate The swap rate at which to evaluate the annuity mapping.
	 * @return The value of the annuity mapping.
	 */
	double getValue(double swapRate);

	/**
	 * Return the first derivative of the annuity mapping for the given swap rate.
	 *
	 * @param swapRate The swap rate at which to evaluate the annuity mapping.
	 * @return The first derivative of the annuity mapping.
	 */
	double getFirstDerivative(double swapRate);

	/**
	 * Return the second derivative of the annuity mapping for the given swap rate.
	 *
	 * @param swapRate The swap rate at which to evaluate the annuity mapping.
	 * @return The second derivative of the annuity mapping.
	 */
	double getSecondDerivative(double swapRate);

}
