package net.finmath.singleswaprate.annuitymapping;

/**
 * Constant normalizer returning the value one.
 *
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public class ConstantNormalizer implements NormalizingFunction {

	@Override
	public double getValue(final double swapRate) {
		return 1;
	}

	@Override
	public double getFirstDerivative(final double swapRate) {
		return 0;
	}

	@Override
	public double getSecondDerivative(final double swapRate) {
		return 0;
	}

}
