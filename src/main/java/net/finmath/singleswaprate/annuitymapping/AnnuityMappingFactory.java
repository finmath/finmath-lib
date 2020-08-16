package net.finmath.singleswaprate.annuitymapping;

import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurveFromDiscountCurve;
import net.finmath.marketdata.products.Swap;
import net.finmath.singleswaprate.annuitymapping.AnnuityMapping.AnnuityMappingType;
import net.finmath.singleswaprate.model.VolatilityCubeModel;
import net.finmath.time.Schedule;
import net.finmath.time.SchedulePrototype;

/**
 * Provides factories to build annuity mappings from uniform input.
 *
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public class AnnuityMappingFactory {

	private final Schedule fixSchedule;
	private final Schedule floatSchedule;

	private final String discountCurveName;
	private final String forwardCurveName;
	private final String volatilityCubeName;

	private final double strike;

	private final double lowerBound;
	private final double upperBound;
	private final int numberOfEvaluationPoints;

	/**
	 * Build an annuity mapping.
	 *
	 * @param strike The strike to get the proper volatilities from the cube.
	 * @param fixSchedule Fix leg schedule of the swap.
	 * @param floatSchedule Float leg schedule of the swap.
	 * @param discountCurveName The name of the discount curve.
	 * @param forwardCurveName The name of the forward curve.
	 * @param volatilityCubeName The name of the volatility cube.
	 * @param type The desired type of annuity mapping.
	 * @param model The model containing curves and cube.
	 * @return The annuity mapping.
	 */
	public static AnnuityMapping buildAnnuityMapping(final double strike, final Schedule fixSchedule, final Schedule floatSchedule,
			final String discountCurveName, final String forwardCurveName, final String volatilityCubeName, final AnnuityMappingType type, final VolatilityCubeModel model) {
		final AnnuityMappingFactory factory = new AnnuityMappingFactory(fixSchedule, floatSchedule, discountCurveName, forwardCurveName, volatilityCubeName,
				strike, 0, 0, -1);
		return factory.build(type, model);
	}

	/**
	 * Build an annuity mapping.
	 *
	 * @param strike The strike to get the proper volatilities from the cube.
	 * @param fixSchedule Fix leg schedule of the swap.
	 * @param floatSchedule Float leg schedule of the swap.
	 * @param discountCurveName The name of the discount curve.
	 * @param forwardCurveName The name of the forward curve.
	 * @param volatilityCubeName The name of the volatility cube.
	 * @param type The desired type of annuity mapping.
	 * @param model The model containing curves and cube.
	 * @param lowerBound The lowest strike the Piterbarg annuity mapping may use during replication, when normalizing.
	 * @param upperBound The maximum strike the Piterbarg annuity mapping may use during replication, when normalizing.
	 * @param numberOfEvaluationPoints The number of points the replication may evaluate Piterbarg annuity mapping is normalizing.
	 * @return The annuity mapping.
	 */
	public static AnnuityMapping buildAnnuityMapping(final double strike, final Schedule fixSchedule, final Schedule floatSchedule,
			final String discountCurveName, final String forwardCurveName, final String volatilityCubeName, final AnnuityMappingType type, final VolatilityCubeModel model,
			final double lowerBound, final double upperBound, final int numberOfEvaluationPoints) {
		final AnnuityMappingFactory factory = new AnnuityMappingFactory(fixSchedule, floatSchedule, discountCurveName, forwardCurveName, volatilityCubeName,
				strike, lowerBound, upperBound, numberOfEvaluationPoints);
		return factory.build(type, model);
	}


	/**
	 * Create the factory.
	 *
	 * @param fixSchedule Fix leg schedule of the swap.
	 * @param floatSchedule Float leg schedule of the swap.
	 * @param discountCurveName The name of the discount curve.
	 * @param forwardCurveName The name of the forward curve.
	 * @param volatilityCubeName The name of the volatility cube.
	 */
	public AnnuityMappingFactory(final Schedule fixSchedule, final Schedule floatSchedule,
			final String discountCurveName, final String forwardCurveName, final String volatilityCubeName) {
		this(fixSchedule, floatSchedule, discountCurveName, forwardCurveName, volatilityCubeName, Double.NaN, 0, 0, -1);
	}

	/**
	 * Create the factory.
	 *
	 * @param fixSchedule Fix leg schedule of the swap.
	 * @param floatSchedule Float leg schedule of the swap.
	 * @param discountCurveName The name of the discount curve.
	 * @param forwardCurveName The name of the forward curve.
	 * @param volatilityCubeName The name of the volatility cube.
	 * @param strike The strike to get the proper volatilities from the cube.
	 * @param lowerBound The lowest strike the Piterbarg annuity mapping may use during replication, when normalizing.
	 * @param upperBound The maximum strike the Piterbarg annuity mapping may use during replication, when normalizing.
	 * @param numberOfEvaluationPoints The number of points the replication may evaluate Piterbarg annuity mapping is normalizing.
	 */
	public AnnuityMappingFactory(final Schedule fixSchedule, final Schedule floatSchedule,
			final String discountCurveName, final String forwardCurveName, final String volatilityCubeName, final double strike,
			final double lowerBound, final double upperBound, final int numberOfEvaluationPoints) {
		super();
		this.fixSchedule = fixSchedule;
		this.floatSchedule = floatSchedule;
		this.discountCurveName = discountCurveName;
		this.forwardCurveName = forwardCurveName;
		this.volatilityCubeName = volatilityCubeName;
		this.strike = strike;
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
		this.numberOfEvaluationPoints = numberOfEvaluationPoints;
	}


	/**
	 * Build the annuity mapping.
	 *
	 * @param type The desired type of annuity mapping.
	 * @param model The model containing curves and cube.
	 * @return The annuity mapping.
	 */
	public AnnuityMapping build(final AnnuityMappingType type, final VolatilityCubeModel model) {

		double strike = this.strike;
		ForwardCurve forwardCurve;
		if(Double.isNaN(strike)) {
			if(forwardCurveName == null || forwardCurveName.isEmpty()) {
				forwardCurve = new ForwardCurveFromDiscountCurve(discountCurveName, fixSchedule.getReferenceDate(),
						SchedulePrototype.getOffsetCodeFromSchedule(fixSchedule));
			} else {
				forwardCurve = model.getForwardCurve(forwardCurveName);
			}
			strike = Swap.getForwardSwapRate(fixSchedule, floatSchedule, forwardCurve, model);
		}

		AnnuityMapping annuityMapping;
		switch(type) {
		case MULTIPITERBARG: annuityMapping = new MultiPiterbargAnnuityMapping(fixSchedule, floatSchedule, strike,
				model, discountCurveName, forwardCurveName, volatilityCubeName, lowerBound, upperBound, numberOfEvaluationPoints); break;
		case BASICPITERBARG: annuityMapping = new BasicPiterbargAnnuityMapping(fixSchedule, floatSchedule, strike, model, discountCurveName,
				volatilityCubeName, lowerBound, upperBound, numberOfEvaluationPoints); break;
		case SIMPLIFIEDLINEAR: annuityMapping = new SimplifiedLinearAnnuityMapping(fixSchedule, floatSchedule, model, discountCurveName); break;
		default: annuityMapping = new BasicPiterbargAnnuityMapping(fixSchedule, floatSchedule, strike, model, discountCurveName, volatilityCubeName,
				0, 0, -1); break;
		}

		return annuityMapping;
	}
}
