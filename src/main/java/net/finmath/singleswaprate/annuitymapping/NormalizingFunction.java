package net.finmath.singleswaprate.annuitymapping;

/**
 * Interface for a normalizing function which is to ensure the no-arbitrage requirements of a Piterbarg annuity mapping.
 *
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public interface NormalizingFunction {

	/**
	 * Return the value of the normalizing function for the given swap rate.
	 *
	 * @param swapRate The desired swap rate
	 * @return The normalizing factor at the given swap rate.
	 */
	double getValue(double swapRate);

	/**
	 * Return the first derivative of the normalizing function at the given swap rate.
	 *
	 * @param swapRate The desired swap rate.
	 * @return The first derivative of the normalizing function at the given swap rate.
	 */
	double getFirstDerivative(double swapRate);

	/**
	 * Return the second derivative of the normalizing function at the given swap rate.
	 *
	 * @param swapRate The desired swap rate.
	 * @return The second derivative of the normalizing function at the given swap rate.
	 */
	double getSecondDerivative(double swapRate);

}
