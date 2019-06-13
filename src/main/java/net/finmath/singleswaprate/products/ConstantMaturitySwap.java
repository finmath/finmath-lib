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
	public ConstantMaturitySwap(Schedule fixSchedule, Schedule floatSchedule, String discountCurveName, String forwardCurveName,
			String volatilityCubeName, AnnuityMappingType annuityMappingType) {
		super(fixSchedule, floatSchedule, discountCurveName, forwardCurveName, volatilityCubeName);
		this.annuityMappingType = annuityMappingType;
	}

	@Override
	protected double payoffFunction(double swapRate, AnnuityMapping annuityMapping, VolatilityCubeModel model) {
		return swapRate * annuityMapping.getValue(swapRate);
	}

	@Override
	protected double hedgeWeight(double swapRate, AnnuityMapping annuityMapping, VolatilityCubeModel model) {
		return 2 *annuityMapping.getFirstDerivative(swapRate) + swapRate *annuityMapping.getSecondDerivative(swapRate);
	}

	@Override
	protected double singularAddon(double swapRate, AnnuityMapping annuityMapping, VolatilityCubeModel model) {
		return 0;
	}

	@Override
	protected AnnuityMapping buildAnnuityMapping( VolatilityCubeModel model) {

		AnnuityMappingFactory factory = new AnnuityMappingFactory(getFixSchedule(), getFloatSchedule(), getDiscountCurveName(), getForwardCurveName(), getVolatilityCubeName());
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
			double swaprate,
			double volatility,
			double swapAnnuity,
			double swapFixing,
			double swapMaturity,
			double payoffUnit)
	{

		double a = 1.0/(swapMaturity-swapFixing);
		double b = (payoffUnit / swapAnnuity - a) / swaprate;
		double convexityAdjustment = (volatility*volatility*swapFixing);

		double valueAdjusted	= swaprate + convexityAdjustment;

		return (a * valueAdjusted + b * valueAdjusted * valueAdjusted + b * convexityAdjustment) / (a + b * valueAdjusted);
	}

}
