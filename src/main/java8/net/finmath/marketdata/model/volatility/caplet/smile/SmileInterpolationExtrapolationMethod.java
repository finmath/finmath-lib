package net.finmath.marketdata.model.volatility.caplet.smile;

/**
 * Interface for a Smile inter and extrapolation.
 */
public interface SmileInterpolationExtrapolationMethod {

	double calculateInterpolatedExtrapolatedSmileVolatility(double strike, int rowIndex);

}
