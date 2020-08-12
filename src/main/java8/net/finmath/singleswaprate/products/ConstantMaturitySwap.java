package net.finmath.singleswaprate.products;

import net.finmath.singleswaprate.annuitymapping.AnnuityMapping;
import net.finmath.singleswaprate.annuitymapping.AnnuityMapping.AnnuityMappingType;
import net.finmath.singleswaprate.annuitymapping.AnnuityMappingFactory;
import net.finmath.singleswaprate.model.VolatilityCubeModel;
import net.finmath.time.Schedule;

/**
 * A constant maturity swap.
 *
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public class ConstantMaturitySwap extends AbstractSingleSwapRateProduct {

	private final AnnuityMappingType annuityMappingType;

	/**
	 * Create the single swap rate product.
	 *
	 * @param fixSchedule The fix schedule of the swap.
	 * @param floatSchedule The float schedule of the swap.
	 * @param discountCurveName The name of the discount curve.
	 * @param forwardCurveName The name of the forward curve.
	 * @param volatilityCubeName The name of the volatility cube.
	 * @param annuityMappingType The type of annuity mapping to be used for evaluation.
	 */
	public ConstantMaturitySwap(final Schedule fixSchedule, final Schedule floatSchedule, final String discountCurveName, final String forwardCurveName,
			final String volatilityCubeName, final AnnuityMappingType annuityMappingType) {
		super(fixSchedule, floatSchedule, discountCurveName, forwardCurveName, volatilityCubeName);
		this.annuityMappingType = annuityMappingType;
	}

	@Override
	protected double payoffFunction(final double swapRate, final AnnuityMapping annuityMapping, final VolatilityCubeModel model) {
		return swapRate * annuityMapping.getValue(swapRate);
	}

	@Override
	protected double hedgeWeight(final double swapRate, final AnnuityMapping annuityMapping, final VolatilityCubeModel model) {
		return 2 *annuityMapping.getFirstDerivative(swapRate) + swapRate *annuityMapping.getSecondDerivative(swapRate);
	}

	@Override
	protected double singularAddon(final double swapRate, final AnnuityMapping annuityMapping, final VolatilityCubeModel model) {
		return 0;
	}

	@Override
	protected AnnuityMapping buildAnnuityMapping( final VolatilityCubeModel model) {

		final AnnuityMappingFactory factory = new AnnuityMappingFactory(getFixSchedule(), getFloatSchedule(), getDiscountCurveName(), getForwardCurveName(), getVolatilityCubeName());
		return factory.build(annuityMappingType, model);
	}

	/**
	 * Analytic approximation of a CMS value.
	 *
	 * @param swaprate The underlying swap rate.
	 * @param volatility The volatility of the swap rate.
	 * @param swapAnnuity The annuity of the swap.
	 * @param swapFixing The fixing time of the swap.
	 * @param swapMaturity The maturity time of the swap.
	 * @param payoffUnit The discount factor to be used.
	 *
	 * @return The value of the cms.
	 */
	public static double analyticApproximation(
			final double swaprate,
			final double volatility,
			final double swapAnnuity,
			final double swapFixing,
			final double swapMaturity,
			final double payoffUnit)
	{

		final double a = 1.0/(swapMaturity-swapFixing);
		final double b = (payoffUnit / swapAnnuity - a) / swaprate;
		final double convexityAdjustment = (volatility*volatility*swapFixing);

		final double valueAdjusted	= swaprate + convexityAdjustment;

		return (a * valueAdjusted + b * valueAdjusted * valueAdjusted + b * convexityAdjustment) / (a + b * valueAdjusted);
	}

}
