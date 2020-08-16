package net.finmath.singleswaprate.products;

import java.util.function.DoubleUnaryOperator;

import net.finmath.functions.AnalyticFormulas;
import net.finmath.integration.RealIntegral;
import net.finmath.integration.SimpsonRealIntegrator;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurveFromDiscountCurve;
import net.finmath.marketdata.model.volatilities.VolatilitySurface;
import net.finmath.marketdata.model.volatilities.VolatilitySurface.QuotingConvention;
import net.finmath.marketdata.products.Swap;
import net.finmath.singleswaprate.annuitymapping.AnnuityMapping;
import net.finmath.singleswaprate.model.VolatilityCubeModel;
import net.finmath.time.Schedule;

/**
 * An abstract class providing valuation methods for single swap rate products.
 * Extending classes may use the valuation via replication of this class by implementing the methods:
 * <ul>
 * <li>payoffFunction</li>
 * <li>hedgeWeight</li>
 * <li>singularAddon</li>
 * <li>buildAnnuityMapping</li>
 * </ul>
 *
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public abstract class AbstractSingleSwapRateProduct extends AbstractAnalyticVolatilityCubeProduct {

	private final Schedule fixSchedule;
	private final Schedule floatSchedule;
	private final String discountCurveName;
	private final String forwardCurveName;
	private final String volatilityCubeName;

	private final QuotingConvention quotingConvention = VolatilitySurface.QuotingConvention.VOLATILITYNORMAL;

	private double lowerBound = -0.15;
	private double upperBound =  0.15;
	private int numberOfEvaluationPoints = 500;

	/**
	 * Create the single swap rate product.
	 *
	 * @param fixSchedule The fix schedule of the swap.
	 * @param floatSchedule The float schedule of the swap.
	 * @param discountCurveName The name of the discount curve.
	 * @param forwardCurveName The name of the forward curve.
	 * @param volatilityCubeName The name of the volatility cube.
	 */
	public AbstractSingleSwapRateProduct(final Schedule fixSchedule, final Schedule floatSchedule, final String discountCurveName, final String forwardCurveName,
			final String volatilityCubeName) {
		super();
		this.fixSchedule = fixSchedule;
		this.floatSchedule = floatSchedule;
		this.discountCurveName = discountCurveName;
		this.forwardCurveName = forwardCurveName;
		this.volatilityCubeName = volatilityCubeName;
	}

	/**
	 * Set the parameters for replication.
	 *
	 * @param lowerBound The lowest strike the replication may use.
	 * @param upperBound The largest strike the replication may use.
	 * @param numberOfEvaluationPoints The number of points the replication may evaluate.
	 */
	public void setIntegrationParameters(final double lowerBound, final double upperBound, final int numberOfEvaluationPoints) {
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
		this.numberOfEvaluationPoints = numberOfEvaluationPoints;
	}

	/**
	 * @return The lowest strike the replication may use.
	 */
	public double getIntegrationLowerBound() {
		return lowerBound;
	}

	/**
	 * @return The largest strike the replication may use.
	 */
	public double getIntegrationUpperBound() {
		return upperBound;
	}

	/**
	 * @return The number of points the replication may evaluate.
	 */
	public int getIntegrationNumberOfEvaluationPoints() {
		return numberOfEvaluationPoints;
	}

	//can be overridden for better performance
	@Override
	public double getValue(final double evaluationTime, final VolatilityCubeModel model) {
		return getValue(evaluationTime, null, model);
	}

	// allows to provide annuity mapping to optimize code
	/**
	 * Return the valuation of the product using the given model.
	 * The model has to implement the modes of {@link VolatilityCubeModel}.
	 * This method allows to specify an annuity mapping to be used during the evaluation.
	 * This may be done to safe time, when many evaluations at different time, or of products with the same underlying swap rate, have to be done at once.
	 * Otherwise each call of <code>getValue</code> will first instantiate an individual annuity mapping. Which may take long for more elaborate implementations.
	 *
	 * @param evaluationTime The evaluation time as double. Cash flows prior and including this time are not considered.
	 * @param annuityMapping The annuity mapping to be used during valuation. If left null a new mapping will be generated.
	 * @param model The model under which the product is valued.
	 * @return The value of the product using the given model.
	 */
	public double getValue(final double evaluationTime, final AnnuityMapping annuityMapping, final VolatilityCubeModel model) {

		if(evaluationTime > getFixSchedule().getPeriodStart(0)) {
			throw new IllegalArgumentException("This framework is not set up to evaluate the product "
					+this.getClass()+" at a time larger than the start of the first period ("+getFixSchedule().getPeriodStart(0)+"). Requested time was "+evaluationTime);
		}

		double forwardSwapRate;
		ForwardCurve forwardCurve;

		if(getForwardCurveName() == null) {
			forwardCurve = new ForwardCurveFromDiscountCurve("From"+getDiscountCurveName(), getDiscountCurveName(), getFixSchedule().getReferenceDate(), "6M");
		} else {
			forwardCurve = model.getForwardCurve(getForwardCurveName());
		}
		forwardSwapRate	= Swap.getForwardSwapRate(getFixSchedule(), getFloatSchedule(), forwardCurve, model);

		// check if there is an annuity mapping provided, otherwise get new one.
		final AnnuityMapping internalAnnuityMapping = annuityMapping == null ? buildAnnuityMapping(model) : annuityMapping;

		double	receiverLeg = 0.0;
		double	payerLeg 	= 0.0;

		// check whether cube supports given lower bound
		final double lowerBound = getVolatilityCubeName() == null ? this.lowerBound :
			Math.max(this.lowerBound, model.getVolatilityCube(getVolatilityCubeName()).getLowestStrike(model));

		// Numerical integration
		final RealIntegral receiverIntegral = new SimpsonRealIntegrator(lowerBound, forwardSwapRate, numberOfEvaluationPoints);
		final RealIntegral    payerIntegral = new SimpsonRealIntegrator(forwardSwapRate, upperBound, numberOfEvaluationPoints);

		final DoubleUnaryOperator  receiverIntegrand = x -> (hedgeWeight(x, internalAnnuityMapping, model) * valuePut(x,model, forwardSwapRate));
		final DoubleUnaryOperator 	payerIntegrand = x -> (hedgeWeight(x, internalAnnuityMapping, model) * valueCall(x,model, forwardSwapRate));

		receiverLeg = receiverIntegral.integrate(receiverIntegrand);
		payerLeg	= payerIntegral.integrate(payerIntegrand);

		double value = ((payoffFunction(forwardSwapRate, internalAnnuityMapping, model)) + receiverLeg + payerLeg
				+ singularAddon(forwardSwapRate, internalAnnuityMapping, model));
		if(evaluationTime != getFixSchedule().getPeriodStart(0)) {
			value *= model.getDiscountCurve(getDiscountCurveName()).getDiscountFactor(model, getFixSchedule().getPeriodStart(0))
					/ model.getDiscountCurve(getDiscountCurveName()).getDiscountFactor(model, evaluationTime);
		}
		return value;
	}

	//to be implemented by individual products
	/**
	 * Payoff function of the product. Dependent only on the swap rate.
	 *
	 * @param swapRate The swap rate.
	 * @param annuityMapping The annuity mapping to use.
	 * @param model The model for context.
	 * @return The payoff of the product.
	 */
	protected abstract double payoffFunction(double swapRate, AnnuityMapping annuityMapping, VolatilityCubeModel model);

	/**
	 * Essentially the second derivative of the payoff function.
	 * The hedgeweight determines the weight of the puts and calls under the integral when replicating.
	 *
	 * @param swapRate The swap rate.
	 * @param annuityMapping The annuity mapping to use.
	 * @param model The model for context.
	 * @return The weight during replication.
	 */
	protected abstract double hedgeWeight(double swapRate, AnnuityMapping annuityMapping, VolatilityCubeModel model);

	/**
	 * As some products have a portion of their weight in a singular point,
	 * this is portion is split off from the <code>hedgeweight</code> and added after the integration.
	 *
	 * @param swapRate The swap rate.
	 * @param annuityMapping The annuity mapping to use.
	 * @param model The model for context.
	 * @return The singular addon.
	 */
	protected abstract double singularAddon(double swapRate, AnnuityMapping annuityMapping, VolatilityCubeModel model);

	/**
	 * Since most annuity mappings require data from models to be created, but models are only provided at execution of <code>getValue</code>,
	 * the product needs to dynamically be able to build its annuity mapping.
	 * This method may be left to return <code>null</code>, if the product requires no annuity mapping or is intended to always receive an annuity mapping for evaluation.
	 *
	 * @param model The model for context.
	 * @return The annuity mapping.
	 */
	protected abstract AnnuityMapping buildAnnuityMapping(VolatilityCubeModel model);

	//for internal use in the integration
	/**
	 * Value of a put option on the swap rate.
	 *
	 * @param optionStrike The strike of the option.
	 * @param model The model for context.
	 * @param swapRate The swap rate.
	 * @return The value of a put.
	 */
	protected double valuePut(final double optionStrike, final VolatilityCubeModel model, final double swapRate){
		return valueCall(optionStrike, model, swapRate) - (swapRate-optionStrike);
	}

	/**
	 * Value of a call option on the swap rate.
	 *
	 * @param optionStrike The strike of the option.
	 * @param model The model for context.
	 * @param swapRate The swap rate.
	 * @return The value of a call.
	 */
	protected double valueCall(final double optionStrike, final VolatilityCubeModel model, final double swapRate){
		final double optionMaturity 	= getFixSchedule().getFixing(0);
		final double termination 		= getFixSchedule().getPayment(getFixSchedule().getNumberOfPeriods()-1);
		final double volatility = model.getVolatilityCube(getVolatilityCubeName()).getValue(model, termination, optionMaturity, optionStrike, quotingConvention);
		final double value = AnalyticFormulas.bachelierOptionValue(swapRate, volatility, optionMaturity, optionStrike, 1.0);
		return value;
	}

	/**
	 * @return the fixSchedule
	 */
	public Schedule getFixSchedule() {
		return fixSchedule;
	}

	/**
	 * @return the floatSchedule
	 */
	public Schedule getFloatSchedule() {
		return floatSchedule;
	}

	/**
	 * @return the discountCurveName
	 */
	public String getDiscountCurveName() {
		return discountCurveName;
	}

	/**
	 * @return the forwardCurveName
	 */
	public String getForwardCurveName() {
		return forwardCurveName;
	}

	/**
	 * @return the volatilityCubeName
	 */
	public String getVolatilityCubeName() {
		return volatilityCubeName;
	}

}
