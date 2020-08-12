package net.finmath.singleswaprate.annuitymapping;

import java.time.LocalDate;

import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurveFromDiscountCurve;
import net.finmath.marketdata.products.Swap;
import net.finmath.singleswaprate.model.VolatilityCubeModel;
import net.finmath.singleswaprate.model.volatilities.ScaledVolatilityCube;
import net.finmath.singleswaprate.model.volatilities.VolatilityCube;
import net.finmath.time.Schedule;

/**
 * Implements an annuity mapping following Vladimir Piterbarg's approach.
 *
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public class MultiPiterbargAnnuityMapping implements AnnuityMapping {

	private final BasicPiterbargAnnuityMapping basicMapping;

	private final double iborOisDecorrelation;
	private final double oisSwapRate;
	private final double iborSwapRate;

	/**
	 * Create the annuity mapping. When used without strike the volatilities are taken out of the cube at par swap rate.
	 *
	 * @param fixSchedule Fix leg schedule of the swap.
	 * @param floatSchedule Float leg schedule of the swap.
	 * @param model The model containing curve and cube.
	 * @param discountCurveName The name of the discount curve.
	 * @param forwardCurveName The name of the forward curve.
	 * @param volatilityCubeName The name of the volatility cube.
	 */
	public MultiPiterbargAnnuityMapping(final Schedule fixSchedule, final Schedule floatSchedule, final VolatilityCubeModel model,
			final String discountCurveName, final String forwardCurveName, final String volatilityCubeName) {
		this(
				fixSchedule,
				floatSchedule,
				Swap.getForwardSwapRate(fixSchedule, floatSchedule, model.getForwardCurve(forwardCurveName), model),
				model,
				discountCurveName,
				forwardCurveName,
				volatilityCubeName,
				0,0,-1);
	}


	/**
	 * Create the annuity mapping.
	 *
	 * @param fixSchedule Fix leg schedule of the swap.
	 * @param floatSchedule Float leg schedule of the swap.
	 * @param strike The strike of the product this annuity mapping is being created for.
	 * @param model The model containing curve and cube.
	 * @param discountCurveName The name of the discount curve.
	 * @param forwardCurveName The name of the forward curve.
	 * @param volatilityCubeName The name of the volatility cube.
	 * @param lowerBound The lowest strike the Piterbarg annuity mapping may use during replication, when normalizing.
	 * @param upperBound The maximum strike the Piterbarg annuity mapping may use during replication, when normalizing.
	 * @param numberOfEvaluationPoints The number of points the replication may evaluate Piterbarg annuity mapping is normalizing.
	 */
	public MultiPiterbargAnnuityMapping(final Schedule fixSchedule, final Schedule floatSchedule, final double strike,
			final VolatilityCubeModel model, final String discountCurveName, final String forwardCurveName, final String volatilityCubeName,
			final double lowerBound, final double upperBound, final int numberOfEvaluationPoints) {
		super();

		//create a new volatility cube out of the given one, scaled by the iborOisDecorrelation
		final VolatilityCube baseCube = model.getVolatilityCube(volatilityCubeName);
		final LocalDate referenceDate = baseCube.getReferenceDate();
		this.iborOisDecorrelation = model.getVolatilityCube(volatilityCubeName).getIborOisDecorrelation();
		final VolatilityCube tempCube = new ScaledVolatilityCube("MultiPiterbargCubeFrom"+volatilityCubeName, referenceDate, volatilityCubeName,
				iborOisDecorrelation, baseCube.getCorrelationDecay());
		final VolatilityCubeModel tempModel = model.addVolatilityCube(tempCube);

		final String offsetCode = getOffsetCode(forwardCurveName);
		final ForwardCurve forwardFromDiscount = new ForwardCurveFromDiscountCurve(discountCurveName, referenceDate, offsetCode);

		this.basicMapping = new BasicPiterbargAnnuityMapping(fixSchedule, floatSchedule, strike, tempModel, discountCurveName, tempCube.getName(),
				lowerBound, upperBound, numberOfEvaluationPoints);
		this.iborSwapRate = Swap.getForwardSwapRate(fixSchedule, floatSchedule, model.getForwardCurve(forwardCurveName), tempModel);
		this.oisSwapRate = Swap.getForwardSwapRate(fixSchedule, floatSchedule, forwardFromDiscount, tempModel);
	}

	@Override
	public double getValue(final double swapRate) {
		return basicMapping.getValue(oisSwapRate + iborOisDecorrelation * (swapRate - iborSwapRate));
	}

	@Override
	public double getFirstDerivative(final double swapRate) {
		return basicMapping.getFirstDerivative(oisSwapRate + iborOisDecorrelation * (swapRate - iborSwapRate)) * iborOisDecorrelation;
	}

	@Override
	public double getSecondDerivative(final double swapRate) {
		return basicMapping.getSecondDerivative(oisSwapRate + iborOisDecorrelation * (swapRate - iborSwapRate)) * iborOisDecorrelation * iborOisDecorrelation;
	}

	// gets the offset code from from the forward curve name
	private static String getOffsetCode(final String forwardCurveName) {
		final String[] splits = forwardCurveName.split("(?<=\\D)(?=\\d)");
		String offsetCode = splits[splits.length-1];
		if(offsetCode != null && offsetCode.length() > 0 && offsetCode.charAt(offsetCode.length() - 1) == ')') {
			offsetCode = offsetCode.substring(0, offsetCode.length() - 1);
		}
		return offsetCode;
	}

}
