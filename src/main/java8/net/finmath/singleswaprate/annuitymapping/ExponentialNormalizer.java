package net.finmath.singleswaprate.annuitymapping;

import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurveFromDiscountCurve;
import net.finmath.marketdata.products.Swap;
import net.finmath.singleswaprate.model.VolatilityCubeModel;
import net.finmath.singleswaprate.products.NormalizingDummyProduct;
import net.finmath.time.Schedule;

/**
 * An exponential normalizing function following
 * \[
 * 	c e^{-(x / S)^2}
 * \]
 * where S is the swap rate and c is some scaling factor.
 *
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public class ExponentialNormalizer implements NormalizingFunction {

	private final double initialSwapRate;
	private final double scale;

	/**
	 * Create the exponential normalizer from information of the product. The constructor assumes a period of 6M for the forward curve.
	 *
	 * @param fixSchedule The fix schedule of the product.
	 * @param floatSchedule The float schedule of the product.
	 * @param discountCurveName The name of the discount curve.
	 * @param forwardCurveName The name of the forward curve.
	 * @param volatilityCubeName The name of the volatility cube.
	 * @param model The model for context.
	 */
	public ExponentialNormalizer(final Schedule fixSchedule, final Schedule floatSchedule, final String discountCurveName, final String forwardCurveName, final String volatilityCubeName, final VolatilityCubeModel model) {
		super();
		final ForwardCurve forwardCurve = new ForwardCurveFromDiscountCurve(discountCurveName,
				model.getDiscountCurve(discountCurveName).getReferenceDate(), "6M");
		this.initialSwapRate = Swap.getForwardSwapRate(fixSchedule, floatSchedule, forwardCurve, model);
		final NormalizingDummyProduct unscaledDummy = new NormalizingDummyProduct(fixSchedule, floatSchedule, discountCurveName, forwardCurveName, volatilityCubeName, new ExponentialNormalizer(initialSwapRate, 1));

		this.scale = 1 / unscaledDummy.getValue(fixSchedule.getFixing(0), model);
	}

	/**
	 * Create the exponential normalizer with parameters.
	 *
	 * @param initialSwapRate The par swap rate.
	 * @param scale The scale.
	 */
	public ExponentialNormalizer(final double initialSwapRate, final double scale) {
		super();
		this.initialSwapRate = initialSwapRate;
		this.scale = scale;
	}

	@Override
	public double getValue(final double swapRate) {
		final double exponent = - swapRate * swapRate / initialSwapRate / initialSwapRate;
		return scale * Math.exp(exponent);
	}

	@Override
	public double getFirstDerivative(final double swapRate) {
		final double factor = -2 * swapRate / initialSwapRate / initialSwapRate;
		return factor * getValue(swapRate);
	}

	@Override
	public double getSecondDerivative(final double swapRate) {
		final double factor = 2 / initialSwapRate / initialSwapRate;
		return (factor * factor * swapRate * swapRate - factor) * getValue(swapRate);
	}

}
