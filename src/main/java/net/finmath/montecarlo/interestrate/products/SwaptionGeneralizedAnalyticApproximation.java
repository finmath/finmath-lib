/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 17.05.2007
 * Created on 30.03.2014
 */
package net.finmath.montecarlo.interestrate.products;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import net.finmath.functions.AnalyticFormulas;
import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.AnalyticModelFromCurvesAndVols;
import net.finmath.marketdata.model.curves.Curve;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.DiscountCurveFromForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.montecarlo.interestrate.LIBORMarketModel;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.montecarlo.model.ProcessModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * This class implements an analytic swaption valuation formula under
 * a LIBOR market model. The algorithm implemented here is the
 * OIS discounting version of the algorithm described in
 * ISBN 0470047224 (see {@link net.finmath.montecarlo.interestrate.products.SwaptionSingleCurveAnalyticApproximation}).
 *
 * The approximation assumes that the forward rates (LIBOR) follow a
 * <b>normal model</b> or <b>log-normal model</b> and that the model provides the integrated
 * instantaneous covariance of the forward rates or the integrated
 * instantaneous covariance of the log-forward rates
 *
 * The getValue method calculates the approximated integrated instantaneous variance of the swap rate,
 * using the approximation
 * \[
 * 	\frac{d log(S(t))}{d log(L(t))} \approx \frac{d log(S(0))}{d log(L(0))} = : w.
 * \]
 *
 * Since \( L \) is a vector, \( w \) is a gradient (vector). The class then approximates
 * the Black volatility of a swaption via
 * \[
 * 	\sigma_S^{2} T := \sum_{i,j} w_{i} \gamma_{i,j} w_{j}
 * \]
 * where \( (\gamma_{i,j})_{i,j = 1,...,m} \) is the covariance matrix of the forward rates.
 *
 * The valuation can be performed in terms of value or implied Black volatility.
 *
 *
 * @author Christian Fries
 * @author Lorenzo Torricelli
 * @date 17.05.2017.
 * @version 1.0
 */
public class SwaptionGeneralizedAnalyticApproximation extends AbstractLIBORMonteCarloProduct implements net.finmath.modelling.products.Swaption {

	public enum StateSpace { //state space of the underlying LMM
		NORMAL,
		LOGNORMAL
	}

	public enum ValueUnit {
		/** Returns the value of the swaption **/
		VALUE,
		/** Returns the Black-Scholes implied integrated variance, i.e., <i>&sigma;<sup>2</sup> T</i> **/
		INTEGRATEDVARIANCE,
		/** Returns the Black-Scholes implied volatility, i.e., <i>&sigma;</i> **/
		VOLATILITY
	}

	private final double      swaprate;
	private final double[]    swapTenor;       // Vector of swap tenor (period start and end dates). Start of first period is the option maturity.
	private final ValueUnit   valueUnit;
	private final StateSpace  stateSpace;


	private Map<String, double[]>						cachedLogSwaprateDerivative;
	private WeakReference<TimeDiscretization>	cachedLogSwaprateDerivativeTimeDiscretization;
	private WeakReference<DiscountCurve>		cachedLogSwaprateDerivativeDiscountCurve;
	private WeakReference<ForwardCurve>		cachedLogSwaprateDerivativeForwardCurve;
	private final Object					cachedLogSwaprateDerivativeLock = new Object();

	private Map<String, double[]>						cachedSwaprateDerivative;
	private WeakReference<TimeDiscretization>	cachedSwaprateDerivativeTimeDiscretization;
	private WeakReference<DiscountCurve>		cachedSwaprateDerivativeDiscountCurve;
	private WeakReference<ForwardCurve>		cachedSwaprateDerivativeForwardCurve;
	private final Object					cachedSwaprateDerivativeLock = new Object();

	/**
	 * Create an analytic swaption approximation product for
	 * log normal forward rate model.
	 *
	 * Note: It is implicitly assumed that swapTenor[0] is the exercise date (no forward starting).
	 *
	 * @param swaprate The strike swap rate of the swaption.
	 * @param swapTenor The swap tenor in doubles.
	 * @param valueUnit The unit of the quantity returned by the getValues method.
	 * @param stateSpace The state space of the LMM (lognormal or normal)
	 */
	public SwaptionGeneralizedAnalyticApproximation(final double swaprate, final double[] swapTenor, final ValueUnit valueUnit, final StateSpace stateSpace) {
		super();
		this.swaprate	= swaprate;
		this.swapTenor	= swapTenor;
		this.valueUnit	= valueUnit;
		this.stateSpace	= stateSpace;
	}

	/**
	 * Create an analytic swaption approximation product for
	 * log normal forward rate model.
	 *
	 * Note: It is implicitly assumed that swapTenor.getTime(0) is the exercise date (no forward starting).
	 *
	 * @param swaprate The strike swap rate of the swaption.
	 * @param swapTenor The swap tenor in doubles.
	 * @param stateSpace The state space of the LMM (lognormal or normal)
	 */
	public SwaptionGeneralizedAnalyticApproximation(final double swaprate, final TimeDiscretization swapTenor, final StateSpace stateSpace) {
		this(swaprate, swapTenor.getAsDoubleArray(), ValueUnit.VALUE, stateSpace);
	}

	@Override
	public RandomVariable getValue(final double evaluationTime, final TermStructureMonteCarloSimulationModel model) {
		final ProcessModel modelBase = model.getModel();
		if(modelBase instanceof LIBORMarketModel) {
			return getValues(evaluationTime, model.getTimeDiscretization(), (LIBORMarketModel)modelBase);
		} else {
			throw new IllegalArgumentException("This product requires a simulation where the underlying model is of type LIBORMarketModel.");
		}
	}

	/**
	 * Calculates the approximated integrated instantaneous variance of the swap rate,
	 * using the approximation d S/d L (t) = d  S/d L (0).
	 *
	 * @param evaluationTime Time at which the product is evaluated.
	 * @param timeDiscretization The time discretization used for integrating the covariance.
	 * @param model A model implementing the LIBORModelMonteCarloSimulationModel
	 * @return Depending on the value of value unit, the method returns either
	 * the approximated integrated instantaneous variance of the swap rate (ValueUnit.INTEGRATEDVARIANCE)
	 * or the value using the Black formula (ValueUnit.VALUE).
	 * @TODO make initial values an arg and use evaluation time.
	 */
	public RandomVariable getValues(final double evaluationTime, final TimeDiscretization timeDiscretization, final LIBORMarketModel model) {
		if(evaluationTime > 0) {
			throw new RuntimeException("Forward start evaluation currently not supported.");
		}

		final double swapStart    = swapTenor[0];
		final double swapEnd      = swapTenor[swapTenor.length-1];

		final int swapStartIndex  = model.getLiborPeriodIndex(swapStart);
		final int swapEndIndex    = model.getLiborPeriodIndex(swapEnd);
		final int optionMaturityIndex = model.getCovarianceModel().getTimeDiscretization().getTimeIndex(swapStart)-1;
		double[]    swapCovarianceWeights;

		if(stateSpace==StateSpace.LOGNORMAL){  //Hash map types, so you can tell whether to return values or vols depending upon the string passed
			final Map<String, double[]>  logSwapRateDerivative  = getLogSwapRateDerivative(model.getLiborPeriodDiscretization(), model.getDiscountCurve(), model.getForwardRateCurve());
			swapCovarianceWeights  = logSwapRateDerivative.get("values");
		}
		else  //adding the normal case
		{
			final Map<String, double[]>  swapRateDerivative  = getSwapRateDerivative(model.getLiborPeriodDiscretization(), model.getAnalyticModel(), model.getDiscountCurve(), model.getForwardRateCurve());
			swapCovarianceWeights  = swapRateDerivative.get("values");
		}

		// Get the integrated libor covariance from the model
		final double[][]	integratedLIBORCovariance = model.getIntegratedLIBORCovariance(timeDiscretization)[optionMaturityIndex];

		// Calculate integrated swap rate covariance
		double integratedSwapRateVariance = 0.0;
		for(int componentIndex1 = swapStartIndex; componentIndex1 < swapEndIndex; componentIndex1++) {
			// Sum the libor cross terms (use symmetry)
			for(int componentIndex2 = componentIndex1+1; componentIndex2 < swapEndIndex; componentIndex2++) { //2*sum; symmetric summing
				integratedSwapRateVariance += 2.0 * swapCovarianceWeights[componentIndex1-swapStartIndex] * swapCovarianceWeights[componentIndex2-swapStartIndex] * integratedLIBORCovariance[componentIndex1][componentIndex2];
			}
			// Add diagonal term (libor variance term)
			integratedSwapRateVariance += swapCovarianceWeights[componentIndex1-swapStartIndex] * swapCovarianceWeights[componentIndex1-swapStartIndex] * integratedLIBORCovariance[componentIndex1][componentIndex1];
		}

		// Return integratedSwapRateVariance if requested
		if(valueUnit == ValueUnit.INTEGRATEDVARIANCE) {
			return new Scalar(integratedSwapRateVariance);
		}

		final double volatility		= Math.sqrt(integratedSwapRateVariance / swapStart);

		// Return integratedSwapRateVariance if requested
		if(valueUnit == ValueUnit.VOLATILITY) {
			return new Scalar(volatility);
		}

		// Use black formula for swaption to calculate the price
		final double parSwaprate	= net.finmath.marketdata.products.Swap.getForwardSwapRate(new TimeDiscretizationFromArray(swapTenor), new TimeDiscretizationFromArray(swapTenor), model.getForwardRateCurve(), model.getDiscountCurve());
		final double swapAnnuity	= net.finmath.marketdata.products.SwapAnnuity.getSwapAnnuity(new TimeDiscretizationFromArray(swapTenor), model.getDiscountCurve());

		final double optionMaturity	= swapStart;
		double valueSwaption;
		if(stateSpace==StateSpace.LOGNORMAL) {
			valueSwaption = AnalyticFormulas.blackModelSwaptionValue(parSwaprate, volatility, optionMaturity, swaprate, swapAnnuity);
		}
		else {
			valueSwaption = AnalyticFormulas.bachelierOptionValue(parSwaprate, volatility, optionMaturity, swaprate, swapAnnuity);
		}

		return new Scalar(valueSwaption);
	}

	/**
	 * This function calculate the partial derivative <i>d log(S) / d log(L<sub>k</sub>)</i> for
	 * a given swap rate with respect to a vector of forward rates (on a given forward rate tenor).
	 *
	 * It also returns some useful other quantities like the corresponding discount factors and swap annuities.
	 *
	 * @param liborPeriodDiscretization The libor period discretization.
	 * @param discountCurve The discount curve. If this parameter is null, the discount curve will be calculated from the forward curve.
	 * @param forwardCurve The forward curve.
	 * @return A map containing the partial derivatives (key "value"), the discount factors (key "discountFactors") and the annuities (key "annuities") as vectors of double[] (indexed by forward rate tenor index starting at swap start)
	 */

	public Map<String, double[]> getLogSwapRateDerivative(final TimeDiscretization liborPeriodDiscretization, DiscountCurve discountCurve, final ForwardCurve forwardCurve) {

		/*
		 * We cache the calculation of the log swaprate derivative. In a calibration this method might be called quite often with the same arguments.
		 */
		synchronized (cachedLogSwaprateDerivativeLock) {

			if(
					cachedLogSwaprateDerivative != null
					&& liborPeriodDiscretization == cachedLogSwaprateDerivativeTimeDiscretization.get()
					&& discountCurve == cachedLogSwaprateDerivativeDiscountCurve.get()
					&& forwardCurve == cachedLogSwaprateDerivativeForwardCurve.get()
					) {
				return cachedLogSwaprateDerivative;
			}
			cachedLogSwaprateDerivativeTimeDiscretization = new WeakReference<>(liborPeriodDiscretization);
			cachedLogSwaprateDerivativeDiscountCurve = new WeakReference<>(discountCurve);
			cachedLogSwaprateDerivativeForwardCurve = new WeakReference<>(forwardCurve);

			/*
			 * Small workaround for the case that the discount curve is not set. This part will be removed later.
			 */
			AnalyticModelFromCurvesAndVols model = null;
			if(discountCurve == null) {
				discountCurve	= new DiscountCurveFromForwardCurve(forwardCurve.getName());
				model					= new AnalyticModelFromCurvesAndVols(new Curve[] { forwardCurve, discountCurve });
			}

			final double swapStart    = swapTenor[0];
			final double swapEnd      = swapTenor[swapTenor.length-1];

			// Get the indices of the swap start and end on the forward rate tenor
			final int swapStartIndex  = liborPeriodDiscretization.getTimeIndex(swapStart);
			final int swapEndIndex    = liborPeriodDiscretization.getTimeIndex(swapEnd);

			// Precalculate forward rates and discount factors. Note: the swap contains swapEndIndex-swapStartIndex forward rates
			final double[] forwardRates       = new double[swapEndIndex-swapStartIndex+1];
			final double[] discountFactors    = new double[swapEndIndex-swapStartIndex+1];

			// Calculate discount factor at swap start
			discountFactors[0] = discountCurve.getDiscountFactor(model, swapStart);

			// Calculate discount factors for swap period ends (used for swap annuity)
			for(int liborPeriodIndex = swapStartIndex; liborPeriodIndex < swapEndIndex; liborPeriodIndex++) {
				final double libor = forwardCurve.getForward(null, liborPeriodDiscretization.getTime(liborPeriodIndex));

				forwardRates[liborPeriodIndex-swapStartIndex]       = libor;
				discountFactors[liborPeriodIndex-swapStartIndex+1]  = discountCurve.getDiscountFactor(model, liborPeriodDiscretization.getTime(liborPeriodIndex+1));
			}

			// Precalculate swap annuities
			final double[]    swapAnnuities   = new double[swapTenor.length-1]; //calculates the annuities value for EVERY libor period (must be used later in the weight calulation)
			double      swapAnnuity     = 0.0;
			//note: the final element of the loop, i.e. the final value of the local variable swap annuity is the total annuity discount factor of the swap
			for(int swapPeriodIndex = swapTenor.length-2; swapPeriodIndex >= 0; swapPeriodIndex--) {
				final int periodEndIndex = liborPeriodDiscretization.getTimeIndex(swapTenor[swapPeriodIndex+1]);
				swapAnnuity += discountFactors[periodEndIndex-swapStartIndex] * (swapTenor[swapPeriodIndex+1]-swapTenor[swapPeriodIndex]);
				swapAnnuities[swapPeriodIndex] = swapAnnuity;
			}

			// Precalculate weights: The formula is take from ISBN 0470047224.
			final double[] swapCovarianceWeights = new double[swapEndIndex-swapStartIndex];

			double valueFloatLeg = 0.0;
			for(int liborPeriodIndex = swapStartIndex; liborPeriodIndex < swapEndIndex; liborPeriodIndex++) { //loop on the Libor index
				final double liborPeriodLength = liborPeriodDiscretization.getTimeStep(liborPeriodIndex);
				valueFloatLeg += forwardRates[liborPeriodIndex-swapStartIndex] * discountFactors[liborPeriodIndex-swapStartIndex+1] * liborPeriodLength; //P(0, T_a)-P(0,T_b)
			}

			int swapPeriodIndex = 0;   //loop on the swap index
			double valueFloatLegUpToSwapStart = 0.0;
			for(int liborPeriodIndex = swapStartIndex; liborPeriodIndex < swapEndIndex; liborPeriodIndex++) {
				if(liborPeriodDiscretization.getTime(liborPeriodIndex) >= swapTenor[swapPeriodIndex+1]) {
					swapPeriodIndex++;
				}

				final double libor				= forwardRates[liborPeriodIndex-swapStartIndex];  //libor=L_k
				final double liborPeriodLength	= liborPeriodDiscretization.getTimeStep(liborPeriodIndex);

				valueFloatLegUpToSwapStart += forwardRates[liborPeriodIndex-swapStartIndex] * discountFactors[liborPeriodIndex-swapStartIndex+1] * liborPeriodLength;   //P(0, T_a)-P(0,T_k+1)

				final double discountFactorAtPeriodEnd = discountCurve.getDiscountFactor(model, liborPeriodDiscretization.getTime(liborPeriodIndex+1)); // P(0,T_k+1)
				final double derivativeFloatLeg	= (discountFactorAtPeriodEnd + valueFloatLegUpToSwapStart - valueFloatLeg) * liborPeriodLength / (1.0 + libor * liborPeriodLength) / valueFloatLeg; //=P(0,T_b)/P(0,T_a)-P(0,T_b)
				final double derivativeFixLeg		= - swapAnnuities[swapPeriodIndex] / swapAnnuity * liborPeriodLength / (1.0 + libor * liborPeriodLength); //second summand pag 379, with a minus

				swapCovarianceWeights[liborPeriodIndex-swapStartIndex] = (derivativeFloatLeg - derivativeFixLeg) * libor;    // and a minus here mathc the formula

			}

			// Return results
			final Map<String, double[]> results = new HashMap<>();
			results.put("values",			swapCovarianceWeights);
			results.put("discountFactors",	discountFactors);
			results.put("swapAnnuities",	swapAnnuities);

			cachedLogSwaprateDerivative = results;

			return results;
		}
	}


	/**
	 * Returns the derivative of the swap rate (associated with this swap) with respect to the forward rates  dS/dL_{i}.
	 *
	 * @param liborPeriodDiscretization Forward rate discretization.
	 * @param model The analytic model providing the context for discountCurve and forwardCurve.
	 * @param discountCurve The discount curve.
	 * @param forwardCurve The forward curve.
	 * @return A map containing the derivative of the swap rate with respect to the forward rates  dS/dL_{i} under the key "values".
	 */
	public Map<String, double[]> getSwapRateDerivative(final TimeDiscretization liborPeriodDiscretization, final AnalyticModel model, DiscountCurve discountCurve, final ForwardCurve forwardCurve) {

		/*
		 * We cache the calculation of the log swaprate derivative. In a calibration this method might be called quite often with the same arguments.
		 */
		synchronized (cachedSwaprateDerivativeLock) {

			if(
					cachedSwaprateDerivative != null
					&& liborPeriodDiscretization == cachedSwaprateDerivativeTimeDiscretization.get()
					&& discountCurve == cachedSwaprateDerivativeDiscountCurve.get()
					&& forwardCurve == cachedSwaprateDerivativeForwardCurve.get()
					) {
				return cachedSwaprateDerivative;
			}
			cachedSwaprateDerivativeTimeDiscretization = new WeakReference<>(liborPeriodDiscretization);
			cachedSwaprateDerivativeDiscountCurve = new WeakReference<>(discountCurve);
			cachedSwaprateDerivativeForwardCurve = new WeakReference<>(forwardCurve);

			/*
			 * Small workaround for the case that the discount curve is not set.  Obtain it from the forward curve
			 */
			if(discountCurve == null) {
				discountCurve	= new DiscountCurveFromForwardCurve(forwardCurve.getName());
			}

			final double swapStart    = swapTenor[0];
			final double swapEnd      = swapTenor[swapTenor.length-1];

			// Get the indices of the swap start and end on the forward rate tenor
			final int swapStartIndex  = liborPeriodDiscretization.getTimeIndex(swapStart);
			final int swapEndIndex    = liborPeriodDiscretization.getTimeIndex(swapEnd);

			// Precalculate forward rates and discount factors. Note: the swap contains swapEndIndex-swapStartIndex forward rates
			final double[] forwardRates       = new double[swapEndIndex-swapStartIndex+1];
			final double[] discountFactors    = new double[swapEndIndex-swapStartIndex+1];

			// Calculate discount factor at swap start
			discountFactors[0] = discountCurve.getDiscountFactor(model, swapStart);

			// Calculate discount factors for swap period ends (used for swap annuity)
			for(int liborPeriodIndex = swapStartIndex; liborPeriodIndex < swapEndIndex; liborPeriodIndex++) {
				final double libor = forwardCurve.getForward(model, liborPeriodDiscretization.getTime(liborPeriodIndex));

				forwardRates[liborPeriodIndex-swapStartIndex]       = libor;
				discountFactors[liborPeriodIndex-swapStartIndex+1]  = discountCurve.getDiscountFactor(model, liborPeriodDiscretization.getTime(liborPeriodIndex+1));
			}

			// Precalculate swap annuities
			final double[]    swapAnnuities   = new double[swapTenor.length-1];
			double      swapAnnuity     = 0.0;
			for(int swapPeriodIndex = swapTenor.length-2; swapPeriodIndex >= 0; swapPeriodIndex--) {  //Backward calculation
				final int periodEndIndex = liborPeriodDiscretization.getTimeIndex(swapTenor[swapPeriodIndex+1]);
				swapAnnuity += discountFactors[periodEndIndex-swapStartIndex] * (swapTenor[swapPeriodIndex+1]-swapTenor[swapPeriodIndex]); //P(0,T_i)*Delta_i
				swapAnnuities[swapPeriodIndex] = swapAnnuity;
			}

			// Precalculate weights: The formula is take from ISBN 0470047224
			final double[] swapCovarianceWeights = new double[swapEndIndex-swapStartIndex];

			double valueFloatLeg = 0.0;
			for(int liborPeriodIndex = swapStartIndex; liborPeriodIndex < swapEndIndex; liborPeriodIndex++) {
				final double liborPeriodLength = liborPeriodDiscretization.getTimeStep(liborPeriodIndex);
				valueFloatLeg += forwardRates[liborPeriodIndex-swapStartIndex] * discountFactors[liborPeriodIndex-swapStartIndex+1] * liborPeriodLength;
			}

			int swapPeriodIndex = 0;
			double valueFloatLegUpToSwapStart = 0.0;
			for(int liborPeriodIndex = swapStartIndex; liborPeriodIndex < swapEndIndex; liborPeriodIndex++) {
				if(liborPeriodDiscretization.getTime(liborPeriodIndex) >= swapTenor[swapPeriodIndex+1]) {
					swapPeriodIndex++;
				}

				final double libor				= forwardRates[liborPeriodIndex-swapStartIndex];
				final double liborPeriodLength	= liborPeriodDiscretization.getTimeStep(liborPeriodIndex);

				valueFloatLegUpToSwapStart += forwardRates[liborPeriodIndex-swapStartIndex] * discountFactors[liborPeriodIndex-swapStartIndex+1] * liborPeriodLength;

				final double discountFactorAtPeriodEnd = discountCurve.getDiscountFactor(model, liborPeriodDiscretization.getTime(liborPeriodIndex+1));
				final double derivativeFloatLeg	= (discountFactorAtPeriodEnd + valueFloatLegUpToSwapStart - valueFloatLeg) * liborPeriodLength / (1.0 + libor * liborPeriodLength) / swapAnnuity; //instead of /vlaueOfFloatingLeg
				final double derivativeFixLeg		= - swapAnnuities[swapPeriodIndex] / (swapAnnuity * swapAnnuity)* liborPeriodLength / (1.0 + libor * liborPeriodLength);  //instead  of /swapAnnuity

				swapCovarianceWeights[liborPeriodIndex-swapStartIndex] = (derivativeFloatLeg - valueFloatLeg*derivativeFixLeg) ; //no Libor multiplication factor
			}

			// Return results
			final Map<String, double[]> results = new HashMap<>();
			results.put("values",			swapCovarianceWeights);
			results.put("discountFactors",	discountFactors);
			results.put("swapAnnuities",	swapAnnuities);

			cachedSwaprateDerivative = results;

			return results;
		}
	}
}
