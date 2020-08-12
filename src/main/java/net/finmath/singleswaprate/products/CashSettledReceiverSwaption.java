package net.finmath.singleswaprate.products;

import net.finmath.singleswaprate.annuitymapping.AnnuityMapping;
import net.finmath.singleswaprate.annuitymapping.AnnuityMapping.AnnuityMappingType;
import net.finmath.singleswaprate.annuitymapping.AnnuityMappingFactory;
import net.finmath.singleswaprate.model.VolatilityCubeModel;
import net.finmath.time.Schedule;

/**
 * A European cash settled receiver swaption.
 *
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public class CashSettledReceiverSwaption extends AbstractSingleSwapRateProduct {

	private final double strike;
	private final AnnuityMappingType annuityMappingType;

	/**
	 * Create the product.
	 *
	 * @param fixSchedule The fix schedule of the swap.
	 * @param floatSchedule The float schedule of the swap.
	 * @param strike The strike of the option.
	 * @param discountCurveName The name of the discount curve.
	 * @param forwardCurveName The name of the forward curve.
	 * @param volatilityCubeName The name of the volatility cube.
	 * @param annuityMappingType The type of annuity mapping to be used for evaluation.
	 */
	public CashSettledReceiverSwaption(final Schedule fixSchedule, final Schedule floatSchedule, final double strike, final String discountCurveName, final String forwardCurveName,
			final String volatilityCubeName, final AnnuityMappingType annuityMappingType) {
		super(fixSchedule, floatSchedule, discountCurveName, forwardCurveName, volatilityCubeName);
		this.strike = strike;
		this.annuityMappingType = annuityMappingType;
	}

	/**
	 * Create the product with custom replication settings.
	 *
	 * @param fixSchedule The fix schedule of the swap.
	 * @param floatSchedule The float schedule of the swap.
	 * @param strike The strike of the option.
	 * @param discountCurveName The name of the discount curve.
	 * @param forwardCurveName The name of the forward curve.
	 * @param volatilityCubeName The name of the volatility cube.
	 * @param annuityMappingType The type of annuity mapping to be used for evaluation.
	 * @param replicationLowerBound The lowest strike the replication may use.
	 * @param replicationUpperBound The largest strike the replication may use.
	 * @param replicationNumberOfEvaluationPoints The number of points the replication may evaluate.
	 */
	public CashSettledReceiverSwaption(final Schedule fixSchedule, final Schedule floatSchedule, final double strike, final String discountCurveName,
			final String forwardCurveName, final String volatilityCubeName, final AnnuityMappingType annuityMappingType,
			final double replicationLowerBound, final double replicationUpperBound, final int replicationNumberOfEvaluationPoints) {
		super(fixSchedule, floatSchedule, discountCurveName, forwardCurveName, volatilityCubeName);
		this.strike = strike;
		this.annuityMappingType = annuityMappingType;

		setIntegrationParameters(replicationLowerBound, replicationUpperBound, replicationNumberOfEvaluationPoints);
	}

	@Override
	protected double payoffFunction(final double swapRate, final AnnuityMapping annuityMapping, final VolatilityCubeModel model) {
		double value = Math.max(strike -swapRate, 0.0);
		value *= annuityMapping.getValue(swapRate) *cashFunction(swapRate);
		return value;
	}

	//Not full integrand; singular part is being split of to be added after integration
	@Override
	protected double hedgeWeight(final double swapRate, final AnnuityMapping annuityMapping, final VolatilityCubeModel model) {

		if(!(swapRate < strike)) {
			return 0;
		}

		double value = annuityMapping.getSecondDerivative(swapRate) * cashFunction(swapRate);
		value += 2 *annuityMapping.getFirstDerivative(swapRate) *cashFunctionFirstDerivative(swapRate);
		value += annuityMapping.getValue(swapRate) *cashFunctionSecondDerivative(swapRate);
		value *= strike -swapRate;

		value -= (annuityMapping.getFirstDerivative(swapRate) *cashFunction(swapRate) +annuityMapping.getValue(swapRate)
		*cashFunctionFirstDerivative(swapRate)) *2;
		return value;
	}

	//added to the integral over the hedge weight
	@Override
	protected double singularAddon(final double swapRate, final AnnuityMapping annuityMapping, final VolatilityCubeModel model) {
		double value = annuityMapping.getValue(swapRate) * cashFunction(swapRate);
		if(swapRate < strike) {
			value *= valueCall(strike, model, swapRate);
		} else {
			value *= valuePut(strike, model, swapRate);
		}
		return value;
	}

	@Override
	protected AnnuityMapping buildAnnuityMapping( final VolatilityCubeModel model) {

		final AnnuityMappingFactory factory = new AnnuityMappingFactory(getFixSchedule(), getFloatSchedule(), getDiscountCurveName(), getForwardCurveName(), getVolatilityCubeName(),
				strike, getIntegrationLowerBound(), getIntegrationUpperBound(), getIntegrationNumberOfEvaluationPoints());
		return factory.build(annuityMappingType, model);
	}

	/**
	 * The annuity cash function for equidistant tenors. This replaces the annuity compared to the physically settled swaption.
	 *
	 * @param swapRate The swap rate.
	 * @return The value of the annuity cash function.
	 */
	private double cashFunction(final double swapRate) {

		final int numberOfPeriods = getFixSchedule().getNumberOfPeriods();
		double periodLength = 0.0;
		for(int index = 0; index < numberOfPeriods; index++) {
			periodLength += getFixSchedule().getPeriodLength(index);
		}
		periodLength /= getFixSchedule().getNumberOfPeriods();

		if(swapRate == 0.0) {
			return numberOfPeriods * periodLength;
		} else {
			return (1 - Math.pow(1 + periodLength * swapRate, - numberOfPeriods)) / swapRate;
		}
	}

	/**
	 * The first derivative of the annuity cash function.
	 *
	 * @param swapRate The swap rate.
	 * @return The first derivative of the annuity cash function.
	 */
	private double cashFunctionFirstDerivative(final double swapRate){

		final int numberOfPeriods = getFixSchedule().getNumberOfPeriods();
		double periodLength = 0.0;
		for(int index = 0; index < getFixSchedule().getNumberOfPeriods(); index++) {
			periodLength += getFixSchedule().getPeriodLength(index);
		}
		periodLength /= getFixSchedule().getNumberOfPeriods();

		if(swapRate == 0.0) {
			return - (numberOfPeriods +1) *numberOfPeriods /2 /periodLength /periodLength;
		} else {
			double value = Math.pow(periodLength * swapRate + 1, - numberOfPeriods - 1);
			value *= numberOfPeriods * periodLength / swapRate;
			value -= cashFunction(swapRate) /swapRate;
			return value;
		}
	}

	/**
	 * The second derivative of the annuity cash function.
	 *
	 * @param swapRate The swap rate.
	 * @return The second derivative of the annuity cash function.
	 */
	private double cashFunctionSecondDerivative(final double swapRate) {

		final int numberOfPeriods = getFixSchedule().getNumberOfPeriods();
		double periodLength = 0.0;
		for(int index = 0; index < numberOfPeriods; index++) {
			periodLength += getFixSchedule().getPeriodLength(index);
		}
		periodLength /= numberOfPeriods;

		double value;
		if(swapRate == 0.0) {
			value  = numberOfPeriods * (numberOfPeriods +1) * (numberOfPeriods +2);
			value *= periodLength * periodLength * periodLength /3;
		} else {
			value  = Math.pow(periodLength * swapRate + 1, - numberOfPeriods - 2);
			value *= - (numberOfPeriods +1) * numberOfPeriods * periodLength * periodLength / swapRate;
			value -= cashFunctionFirstDerivative(swapRate) *2 /swapRate;
		}
		return value;
	}

}
