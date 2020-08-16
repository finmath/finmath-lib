package net.finmath.singleswaprate.products;

import net.finmath.singleswaprate.annuitymapping.AnnuityMapping;
import net.finmath.singleswaprate.annuitymapping.AnnuityMapping.AnnuityMappingType;
import net.finmath.singleswaprate.annuitymapping.AnnuityMappingFactory;
import net.finmath.singleswaprate.model.VolatilityCubeModel;
import net.finmath.time.Schedule;

/**
 * A dummy product that only evaluates the value of a {@link AnnuityMapping}. This can be used to find the expectation correction factor in Piterbarg annuity mapping.
 *
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public class AnnuityDummyProduct extends AbstractSingleSwapRateProduct {

	private final AnnuityMappingType annuityMappingType;
	private final AnnuityMapping annuityMapping;

	/**
	 * Create the dummy product with the annuity mapping specified by type.
	 * The mapping will be generated at execution of <code>getValue</code>.
	 *
	 * @param fixSchedule The fix schedule of the swap.
	 * @param floatSchedule The float schedule of the swap.
	 * @param discountCurveName The name of the discount curve.
	 * @param forwardCurveName The name of the forward curve.
	 * @param volatilityCubeName The name of the volatility cube.
	 * @param annuityMappingType The type of the annuity mapping.
	 */
	public AnnuityDummyProduct(final Schedule fixSchedule, final Schedule floatSchedule, final String discountCurveName, final String forwardCurveName,
			final String volatilityCubeName, final AnnuityMappingType annuityMappingType) {
		super(fixSchedule, floatSchedule, discountCurveName, forwardCurveName, volatilityCubeName);
		this.annuityMappingType = annuityMappingType;
		this.annuityMapping = null;
	}

	/**
	 * Create the dummy product for the given annuity mapping.
	 *
	 * @param fixSchedule The fix schedule of the swap.
	 * @param floatSchedule The float schedule of the swap.
	 * @param discountCurveName The name of the discount curve.
	 * @param forwardCurveName The name of the forward curve.
	 * @param volatilityCubeName The name of the volatility cube.
	 * @param annuityMapping The annuity mapping.
	 */
	public AnnuityDummyProduct(final Schedule fixSchedule, final Schedule floatSchedule, final String discountCurveName, final String forwardCurveName,
			final String volatilityCubeName, final AnnuityMapping annuityMapping) {
		super(fixSchedule, floatSchedule, discountCurveName, forwardCurveName, volatilityCubeName);
		this.annuityMapping = annuityMapping;
		this.annuityMappingType = null;
	}

	@Override
	protected double payoffFunction(final double swapRate, final AnnuityMapping annuityMapping,
			final VolatilityCubeModel model) {
		return annuityMapping.getValue(swapRate);
	}

	@Override
	protected double hedgeWeight(final double swapRate, final AnnuityMapping annuityMapping,
			final VolatilityCubeModel model) {
		return annuityMapping.getSecondDerivative(swapRate);
	}

	@Override
	protected double singularAddon(final double swapRate, final AnnuityMapping annuityMapping,
			final VolatilityCubeModel model) {
		return 0;
	}

	@Override
	protected AnnuityMapping buildAnnuityMapping(final VolatilityCubeModel model) {

		if(annuityMapping != null) {
			return annuityMapping;
		}

		final AnnuityMappingFactory factory = new AnnuityMappingFactory(getFixSchedule(), getFloatSchedule(), getDiscountCurveName(), getForwardCurveName(), getVolatilityCubeName());
		return factory.build(annuityMappingType, model);
	}

}
