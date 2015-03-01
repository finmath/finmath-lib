/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2004
 */
package net.finmath.montecarlo.interestrate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.functions.AnalyticFormulas;
import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.marketdata.model.curves.DiscountCurveFromForwardCurve;
import net.finmath.marketdata.model.curves.DiscountCurveInterface;
import net.finmath.marketdata.model.curves.ForwardCurveInterface;
import net.finmath.marketdata.model.volatilities.AbstractSwaptionMarketData;
import net.finmath.marketdata.products.Swap;
import net.finmath.marketdata.products.SwapAnnuity;
import net.finmath.montecarlo.interestrate.modelplugins.AbstractLIBORCovarianceModel;
import net.finmath.montecarlo.interestrate.modelplugins.AbstractLIBORCovarianceModelParametric;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.montecarlo.interestrate.products.SwaptionAnalyticApproximation;
import net.finmath.montecarlo.interestrate.products.SwaptionSimple;
import net.finmath.montecarlo.model.AbstractModel;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.RegularSchedule;
import net.finmath.time.ScheduleInterface;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * Implements a (generalized) LIBOR market model with some drift approximation methods.
 * <br><br>
 * In its default case the class specifies a multi-factor LIBOR market model in its log-normal formulation, that is
 * <i>L<sub>j</sub> = exp(Y<sub>j</sub>) </i> where
 * \[
 * 		dY_{j} = \mu_{j} dt + \lambda_{1,j} dW_{1} + \ldots + \lambda_{m,j} dW_{m}
 * \]
 * <br>
 * The model uses an <code>AbstractLIBORCovarianceModel</code> for the specification of
 * <i>(&lambda;<sub>1,j</sub>,...,&lambda;<sub>m,j</sub>)</i> as a covariance model.
 * See {@link net.finmath.montecarlo.model.AbstractModelInterface} for details on the implemented interface
 * <br><br>
 * However, the class is more general:
 * <ul>
 * 	<li>
 * 		The model may be log-normal or normal specification with a given local volatility.
 *	</li>
 * 	<li>
 * 		The class implements different measure(drift) / numeraire pairs: terminal measure and spot measure.
 *	</li>
 * 	<li>
 * 		The class allows to configure a discounting curve (e.g.&nbsp;for "OIS discounting") using a simple deterministic zero spread.
 * 		In this case, the numeraire \( N(t) \) is adjusted by \( \exp( \int_0^t -\lambda(\tau) d\tau ) \). 
 *	</li>
 * </ul>
 * 
 * <br>
 * The class specifies a LIBOR market model, that is
 * <i>L<sub>j</sub> = f(Y<sub>j</sub>) </i> where
 * <ul>
 * 	<li>
 * 		<i>f</i> is <i>f(x) = exp(x)</i> (default, log-normal LIBOR Market Model) or
 * 	</li>
 * 	<li>
 * 		<i>f</i> is <i>f(x) = x</i> (normal model, used if <code>property.set("stateSpace","NORMAL"))</code>
 * 	</li>
 * </ul>
 * and
 * <br>
 * <i>dY<sub>j</sub> = &mu;<sub>j</sub> dt + &lambda;<sub>1,j</sub> dW<sub>1</sub> + ... + &lambda;<sub>m,j</sub> dW<sub>m</sub></i> <br>
 * <br>
 * see {@link net.finmath.montecarlo.model.AbstractModelInterface} for details on the implemented interface.
 * <br>
 * The model uses an <code>AbstractLIBORCovarianceModel</code> as a covariance model.
 * If the covariance model is of type <code>AbstractLIBORCovarianceModelParametric</code>
 * a calibration to swaptions can be performed.
 * <br>
 * Note that &lambda; may still depend on <i>L</i>, hence generating a log-normal dynamic for <i>L</i> even
 * if the stateSpace property has been set to NORMAL.
 * <br>
 *
 * The map <code>properties</code> allows to configure the model. The following keys may be used:
 * <ul>
 * 		<li>
 * 			<code>measure</code>: Possible values:
 * 			<ul>
 * 				<li>
 * 					<code>SPOT</code>: Simulate under spot measure. In this case, the single curve numeraire
 * 					is \( N(T_{i}) = \prod_{j=0}^{i-1} (1 + L(T_{j},T_{j+1};T_{j}) (T_{j+1}-T_{j})) \).
 * 				</li>
 * 				<li>
 * 					<code>TERMINAL</code>: Simulate under terminal measure. In this case, the single curve numeraire
 * 					is \( N(T_{i}) = P(T_{n};T_{i}) = \prod_{j=i}^{n-1} (1 + L(T_{j},T_{j+1};T_{i}) (T_{j+1}-T_{j}))^{-1} \).
 * 				</li>
 *			</ul>
 *		</li>
 * 		<li>
 * 			<code>stateSpace</code>: Possible values:
 * 			<ul>
 * 				<li>
 * 					<code>LOGNORMAL</code>: The state space transform is set to exp, i.e., <i>L = exp(Y)</i>. When the covariance model is deterministic, then this is the classical lognormal LIBOR market model. Note that the covariance model may still provide a local volatility function.
 * 				</li>
 * 				<li>
 * 					<code>NORMAL</code>: The state space transform is set to identity, i.e., <i>L = Y</i>. When the covariance model is deterministic, then this is a normal LIBOR market model. Note that the covariance model may still provide a local volatility function.
 * 				</li>
 *			</ul>
 *		</li>
 * </ul>
 * <br>
 * The main task of this class is to calculate the risk-neutral drift and the
 * corresponding numeraire given the covariance model.
 * 
 * The calibration of the covariance structure is not part of this class. For the calibration
 * of parametric models of the instantaneous covariance see
 * {@link net.finmath.montecarlo.interestrate.modelplugins.AbstractLIBORCovarianceModelParametric#getCloneCalibrated(LIBORMarketModelInterface, AbstractLIBORMonteCarloProduct[], double[], double[], Map)}.
 * 
 * @author Christian Fries
 * @version 1.2
 * @see net.finmath.montecarlo.process.AbstractProcessInterface The interface for numerical schemes.
 * @see net.finmath.montecarlo.model.AbstractModelInterface The interface for models provinding parameters to numerical schemes.
 * @see net.finmath.montecarlo.interestrate.modelplugins.AbstractLIBORCovarianceModel The abstract covariance model plug ins.
 * @see net.finmath.montecarlo.interestrate.modelplugins.AbstractLIBORCovarianceModelParametric A parametic covariance model including a generic calibration algorithm.
 */
public class LIBORMarketModel extends AbstractModel implements LIBORMarketModelInterface {

	public enum Driftapproximation	{ EULER, LINE_INTEGRAL, PREDICTOR_CORRECTOR }
	public enum Measure				{ SPOT, TERMINAL }
	public enum StateSpace			{ NORMAL, LOGNORMAL }

	private final TimeDiscretizationInterface		liborPeriodDiscretization;

	private String							forwardCurveName;
	private AnalyticModelInterface			curveModel;

	private ForwardCurveInterface			forwardRateCurve;
	private DiscountCurveInterface			discountCurve;

	private AbstractLIBORCovarianceModel	covarianceModel;

	private AbstractSwaptionMarketData		swaptionMarketData;

	private Driftapproximation	driftApproximationMethod	= Driftapproximation.EULER;
	private Measure				measure						= Measure.SPOT;
	private StateSpace			stateSpace					= StateSpace.LOGNORMAL;

	// This is a cache of the integrated covariance.
	private double[][][]	integratedLIBORCovariance;
	private final Object	integratedLIBORCovarianceLazyInitLock = new Object();

	public static class CalibrationItem {
		public final AbstractLIBORMonteCarloProduct		calibrationProduct;
		public final double								calibrationTargetValue;
		public final double								calibrationWeight;

		public CalibrationItem(AbstractLIBORMonteCarloProduct calibrationProduct, double calibrationTargetValue, double calibrationWeight) {
			super();
			this.calibrationProduct		= calibrationProduct;
			this.calibrationTargetValue	= calibrationTargetValue;
			this.calibrationWeight		= calibrationWeight;
		}

		@Override
		public String toString() {
			return "CalibrationItem [calibrationProduct=" + calibrationProduct
					+ ", calibrationTargetValue=" + calibrationTargetValue
					+ ", calibrationWeight=" + calibrationWeight + "]";
		}
	}

	/**
	 * Creates a LIBOR Market Model for given covariance.
	 * <br>
	 * If calibrationItems in non-empty and the covariance model is a parametric model,
	 * the covariance will be replaced by a calibrate version of the same model, i.e.,
	 * the LIBOR Market Model will be calibrated.
	 * <br>
	 * The map <code>properties</code> allows to configure the model. The following keys may be used:
	 * <ul>
	 * 		<li>
	 * 			<code>measure</code>: Possible values:
	 * 			<ul>
	 * 				<li>
	 * 					<code>SPOT</code> (<code>String</code>): Simulate under spot measure.
	 * 				</li>
	 * 				<li>
	 * 					<code>TERMINAL</code> (<code>String</code>): Simulate under terminal measure.
	 * 				</li>
	 *			</ul>
	 *		</li>
	 * 		<li>
	 * 			<code>stateSpace</code>: Possible values:
	 * 			<ul>
	 * 				<li>
	 * 					<code>LOGNORMAL</code> (<code>String</code>): Simulate <i>L = exp(Y)</i>.
	 * 				</li>
	 * 				<li>
	 * 					<code>NORMAL</code> (<code>String</code>): Simulate <i>L = Y</i>.
	 * 				</li>
	 *			</ul>
	 *		</li>
	 * 		<li>
	 * 			<code>calibrationParameters</code>: Possible values:
	 * 			<ul>
	 * 				<li>
	 * 					<code>Map&lt;String,Object&gt;</code> a parameter map with the following key/value pairs:
	 * 					<ul>
	 *				 		<li>
	 * 							<code>accuracy</code>: <code>Double</code> specifying the required solver accuracy.
	 * 						</li>
	 *				 		<li>
	 * 							<code>maxIterations</code>: <code>Integer</code> specifying the maximum iterations for the solver.
	 * 						</li>
	 *					</ul>
	 *				</li>
	 *			</ul>
	 *		</li>
	 * </ul>
	 * 
	 * @param liborPeriodDiscretization The discretization of the interest rate curve into forward rates (tenor structure).
	 * @param analyticModel The associated analytic model of this model (containing the associated market data objects like curve).
	 * @param forwardRateCurve The initial values for the forward rates.
	 * @param discountCurve The discount curve to use. This will create an LMM model with a deterministic zero-spread discounting adjustment.
	 * @param covarianceModel The covariance model to use.
	 * @param calibrationItems The vector of calibration items (a union of a product, target value and weight) for the objective function sum weight(i) * (modelValue(i)-targetValue(i).
	 * @param properties Key value map specifying properties like <code>measure</code> and <code>stateSpace</code>.
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	public LIBORMarketModel(
			TimeDiscretizationInterface			liborPeriodDiscretization,
			AnalyticModelInterface				analyticModel,
			ForwardCurveInterface				forwardRateCurve,
			DiscountCurveInterface				discountCurve,
			AbstractLIBORCovarianceModel		covarianceModel,
			CalibrationItem[]					calibrationItems,
			Map<String, ?>						properties
			) throws CalculationException {

		// Set some properties
		if(properties != null && properties.containsKey("measure"))					measure		= Measure.valueOf(((String)properties.get("measure")).toUpperCase());
		if(properties != null && properties.containsKey("stateSpace"))				stateSpace	= StateSpace.valueOf(((String)properties.get("stateSpace")).toUpperCase());

		Map<String,Object> calibrationParameters = null;
		if(properties != null && properties.containsKey("calibrationParameters"))	calibrationParameters	= (Map<String,Object>)properties.get("calibrationParameters");

		this.liborPeriodDiscretization	= liborPeriodDiscretization;
		this.curveModel					= analyticModel;
		this.forwardRateCurve	= forwardRateCurve;
		this.discountCurve		= discountCurve;
		this.covarianceModel	= covarianceModel;

		double[] times = new double[liborPeriodDiscretization.getNumberOfTimeSteps()];
		for(int i=0; i<times.length; i++) times[i] = liborPeriodDiscretization.getTime(i);

		// Perform calibration, if data is given
		if(calibrationItems != null && calibrationItems.length > 0) {
			AbstractLIBORCovarianceModelParametric covarianceModelParametric = null;
			try {
				covarianceModelParametric = (AbstractLIBORCovarianceModelParametric)covarianceModel;
			}
			catch(Exception e) {
				throw new ClassCastException("Calibration is currently restricted to parametric covariance models (AbstractLIBORCovarianceModelParametric).");
			}

			// @TODO Should be more elegant. Convert array for constructor
			AbstractLIBORMonteCarloProduct[]	calibrationProducts		= new AbstractLIBORMonteCarloProduct[calibrationItems.length];
			double[]							calibrationTargetValues	= new double[calibrationItems.length];
			double[]							calibrationWeights		= new double[calibrationItems.length];
			for(int i=0; i<calibrationTargetValues.length; i++) {
				calibrationProducts[i]		= calibrationItems[i].calibrationProduct;
				calibrationTargetValues[i]	= calibrationItems[i].calibrationTargetValue;
				calibrationWeights[i]		= calibrationItems[i].calibrationWeight;
			}

			this.covarianceModel    = covarianceModelParametric.getCloneCalibrated(this, calibrationProducts, calibrationTargetValues, calibrationWeights, calibrationParameters);
		}

	}

	/**
	 * Creates a LIBOR Market Model for given covariance.
	 * 
	 * @param liborPeriodDiscretization The discretization of the interest rate curve into forward rates (tenor structure).
	 * @param forwardRateCurve The initial values for the forward rates.
	 * @param covarianceModel The covariance model to use.
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	public LIBORMarketModel(
			TimeDiscretizationInterface		liborPeriodDiscretization,
			ForwardCurveInterface			forwardRateCurve,
			AbstractLIBORCovarianceModel	covarianceModel
			) throws CalculationException {
		this(liborPeriodDiscretization, forwardRateCurve, null, covarianceModel, new CalibrationItem[0], null);
	}

	/**
	 * Creates a LIBOR Market Model for given covariance.
	 * 
	 * @param liborPeriodDiscretization The discretization of the interest rate curve into forward rates (tenor structure).
	 * @param forwardRateCurve The initial values for the forward rates.
	 * @param discountCurve The discount curve to use. This will create an LMM model with a deterministic zero-spread discounting adjustment.
	 * @param covarianceModel The covariance model to use.
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	public LIBORMarketModel(
			TimeDiscretizationInterface		liborPeriodDiscretization,
			ForwardCurveInterface			forwardRateCurve,
			DiscountCurveInterface			discountCurve,
			AbstractLIBORCovarianceModel	covarianceModel
			) throws CalculationException {
		this(liborPeriodDiscretization, forwardRateCurve, discountCurve, covarianceModel, new CalibrationItem[0], null);
	}

	/**
	 * Creates a LIBOR Market Model using a given covariance model and calibrating this model
	 * to given swaption volatility data.
	 * 
	 * @param liborPeriodDiscretization The discretization of the interest rate curve into forward rates (tenor structure).
	 * @param forwardRateCurve The initial values for the forward rates.
	 * @param covarianceModel The covariance model to use.
	 * @param swaptionMarketData The set of swaption values to calibrate to.
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	public LIBORMarketModel(
			TimeDiscretizationInterface			liborPeriodDiscretization,
			ForwardCurveInterface				forwardRateCurve,
			AbstractLIBORCovarianceModel		covarianceModel,
			AbstractSwaptionMarketData			swaptionMarketData
			) throws CalculationException {
		this(liborPeriodDiscretization, forwardRateCurve, null, covarianceModel, swaptionMarketData, null);
	}

	/**
	 * Creates a LIBOR Market Model for given covariance.
	 * 
	 * @param liborPeriodDiscretization The discretization of the interest rate curve into forward rates (tenor structure).
	 * @param forwardRateCurve The initial values for the forward rates.
	 * @param discountCurve The discount curve to use. This will create an LMM model with a deterministic zero-spread discounting adjustment.
	 * @param covarianceModel The covariance model to use.
	 * @param swaptionMarketData The set of swaption values to calibrate to.
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	public LIBORMarketModel(
			TimeDiscretizationInterface			liborPeriodDiscretization,
			ForwardCurveInterface				forwardRateCurve,
			DiscountCurveInterface				discountCurve,
			AbstractLIBORCovarianceModel		covarianceModel,
			AbstractSwaptionMarketData			swaptionMarketData
			) throws CalculationException {
		this(
				liborPeriodDiscretization,
				forwardRateCurve,
				discountCurve,
				covarianceModel,
				swaptionMarketData,
				null
				);
	}

	/**
	 * Creates a LIBOR Market Model for given covariance.
	 * 
	 * @param liborPeriodDiscretization The discretization of the interest rate curve into forward rates (tenor structure).
	 * @param forwardRateCurve The initial values for the forward rates.
	 * @param discountCurve The discount curve to use. This will create an LMM model with a deterministic zero-spread discounting adjustment.
	 * @param covarianceModel The covariance model to use.
	 * @param swaptionMarketData The set of swaption values to calibrate to.
	 * @param properties Key value map specifying properties like <code>measure</code> and <code>stateSpace</code>.
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	public LIBORMarketModel(
			TimeDiscretizationInterface			liborPeriodDiscretization,
			ForwardCurveInterface				forwardRateCurve,
			DiscountCurveInterface				discountCurve,
			AbstractLIBORCovarianceModel		covarianceModel,
			AbstractSwaptionMarketData			swaptionMarketData,
			Map<String, ?>					properties
			) throws CalculationException {
		this(
				liborPeriodDiscretization,
				forwardRateCurve,
				discountCurve,
				covarianceModel,
				getCalibrationItems(
						liborPeriodDiscretization,
						forwardRateCurve,
						swaptionMarketData,
						// Condition under which we use analytic approximation
						(properties == null || properties.get("stateSpace") == null || ((String)properties.get("stateSpace")).toUpperCase().equals(StateSpace.LOGNORMAL.name()))
						&& AbstractLIBORCovarianceModelParametric.class.isAssignableFrom(covarianceModel.getClass())
						),
						properties
				);
	}



	/**
	 * Creates a LIBOR Market Model for given covariance.
	 * <br>
	 * If calibrationItems in non-empty and the covariance model is a parametric model,
	 * the covariance will be replaced by a calibrate version of the same model, i.e.,
	 * the LIBOR Market Model will be calibrated.
	 * <br>
	 * The map <code>properties</code> allows to configure the model. The following keys may be used:
	 * <ul>
	 * 		<li>
	 * 			<code>measure</code>: Possible values:
	 * 			<ul>
	 * 				<li>
	 * 					<code>SPOT</code> (<code>String</code>): Simulate under spot measure.
	 * 				</li>
	 * 				<li>
	 * 					<code>TERMINAL</code> (<code>String</code>): Simulate under terminal measure.
	 * 				</li>
	 *			</ul>
	 *		</li>
	 * 		<li>
	 * 			<code>stateSpace</code>: Possible values:
	 * 			<ul>
	 * 				<li>
	 * 					<code>LOGNORMAL</code> (<code>String</code>): Simulate <i>L = exp(Y)</i>.
	 * 				</li>
	 * 				<li>
	 * 					<code>NORMAL</code> (<code>String</code>): Simulate <i>L = Y</i>.
	 * 				</li>
	 *			</ul>
	 *		</li>
	 * 		<li>
	 * 			<code>calibrationParameters</code>: Possible values:
	 * 			<ul>
	 * 				<li>
	 * 					<code>Map&lt;String,Object&gt;</code> a parameter map with the following key/value pairs:
	 * 					<ul>
	 *				 		<li>
	 * 							<code>accuracy</code>: <code>Double</code> specifying the required solver accuracy.
	 * 						</li>
	 *				 		<li>
	 * 							<code>maxIterations</code>: <code>Integer</code> specifying the maximum iterations for the solver.
	 * 						</li>
	 *					</ul>
	 *				</li>
	 *			</ul>
	 *		</li>
	 * </ul>
	 * 
	 * @param liborPeriodDiscretization The discretization of the interest rate curve into forward rates (tenor structure).
	 * @param forwardRateCurve The initial values for the forward rates.
	 * @param discountCurve The discount curve to use. This will create an LMM model with a deterministic zero-spread discounting adjustment.
	 * @param covarianceModel The covariance model to use.
	 * @param calibrationItems The vector of calibration items (a union of a product, target value and weight) for the objective function sum weight(i) * (modelValue(i)-targetValue(i).
	 * @param properties Key value map specifying properties like <code>measure</code> and <code>stateSpace</code>.
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	public LIBORMarketModel(
			TimeDiscretizationInterface			liborPeriodDiscretization,
			ForwardCurveInterface				forwardRateCurve,
			DiscountCurveInterface				discountCurve,
			AbstractLIBORCovarianceModel		covarianceModel,
			CalibrationItem[]					calibrationItems,
			Map<String, ?>						properties
			) throws CalculationException {
		this(liborPeriodDiscretization, null, forwardRateCurve, discountCurve, covarianceModel, calibrationItems, properties);
	}

	private static CalibrationItem[] getCalibrationItems(TimeDiscretizationInterface liborPeriodDiscretization, ForwardCurveInterface forwardCurve, AbstractSwaptionMarketData swaptionMarketData, boolean isUseAnalyticApproximation) {
		if(swaptionMarketData == null) return null;

		TimeDiscretizationInterface	optionMaturities		= swaptionMarketData.getOptionMaturities();
		TimeDiscretizationInterface	tenor					= swaptionMarketData.getTenor();
		double						swapPeriodLength		= swaptionMarketData.getSwapPeriodLength();

		ArrayList<CalibrationItem> calibrationItems = new ArrayList<CalibrationItem>();
		for(int exerciseIndex=0; exerciseIndex<=optionMaturities.getNumberOfTimeSteps(); exerciseIndex++) {
			for(int tenorIndex=0; tenorIndex<=tenor.getNumberOfTimeSteps()-exerciseIndex; tenorIndex++) {

				// Create a swaption
				double exerciseDate	= optionMaturities.getTime(exerciseIndex);
				double swapLength	= tenor.getTime(tenorIndex);

				if(liborPeriodDiscretization.getTimeIndex(exerciseDate) < 0) continue;
				if(liborPeriodDiscretization.getTimeIndex(exerciseDate+swapLength) <= liborPeriodDiscretization.getTimeIndex(exerciseDate)) continue;

				int numberOfPeriods = (int)(swapLength / swapPeriodLength);

				double[] fixingDates      = new double[numberOfPeriods];
				double[] paymentDates     = new double[numberOfPeriods];
				double[] swapTenorTimes   = new double[numberOfPeriods+1];

				for(int periodStartIndex=0; periodStartIndex<numberOfPeriods; periodStartIndex++) {
					fixingDates[periodStartIndex] = exerciseDate + periodStartIndex * swapPeriodLength;
					paymentDates[periodStartIndex] = exerciseDate + (periodStartIndex+1) * swapPeriodLength;
					swapTenorTimes[periodStartIndex] = exerciseDate + periodStartIndex * swapPeriodLength;
				}
				swapTenorTimes[numberOfPeriods] = exerciseDate + numberOfPeriods * swapPeriodLength;


				// Swaptions swap rate
				ScheduleInterface swapTenor = new RegularSchedule(new TimeDiscretization(swapTenorTimes));
				double swaprate = Swap.getForwardSwapRate(swapTenor, swapTenor, forwardCurve, null);

				// Set swap rates for each period
				double[] swaprates        = new double[numberOfPeriods];
				for(int periodStartIndex=0; periodStartIndex<numberOfPeriods; periodStartIndex++) {
					swaprates[periodStartIndex] = swaprate;
				}

				if(isUseAnalyticApproximation) {
					AbstractLIBORMonteCarloProduct swaption = new SwaptionAnalyticApproximation(swaprate, swapTenorTimes, SwaptionAnalyticApproximation.ValueUnit.VOLATILITY);
					double impliedVolatility = swaptionMarketData.getVolatility(exerciseDate, swapLength, swaptionMarketData.getSwapPeriodLength(), swaprate);

					calibrationItems.add(new CalibrationItem(swaption, impliedVolatility, 1.0));
				}
				else {
					AbstractLIBORMonteCarloProduct swaption = new SwaptionSimple(swaprate, swapTenorTimes, SwaptionSimple.ValueUnit.VALUE);

					double forwardSwaprate		= Swap.getForwardSwapRate(swapTenor, swapTenor, forwardCurve);
					double swapAnnuity 			= SwapAnnuity.getSwapAnnuity(swapTenor, forwardCurve);
					double impliedVolatility	= swaptionMarketData.getVolatility(exerciseDate, swapLength, swaptionMarketData.getSwapPeriodLength(), swaprate);

					double targetValue = AnalyticFormulas.blackModelSwaptionValue(forwardSwaprate, impliedVolatility, exerciseDate, swaprate, swapAnnuity);

					calibrationItems.add(new CalibrationItem(swaption, targetValue, 1.0));
				}
			}
		}

		return calibrationItems.toArray(new CalibrationItem[calibrationItems.size()]);
	}

	/**
	 * Return the numeraire at a given time.
	 * The numeraire is provided for interpolated points. If requested on points which are not
	 * part of the tenor discretization, the numeraire uses a linear interpolation of the reciprocal
	 * value. See ISBN 0470047224 for details.
	 * 
	 * @param time Time time <i>t</i> for which the numeraire should be returned <i>N(t)</i>.
	 * @return The numeraire at the specified time as <code>RandomVariableInterface</code>
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	@Override
	public RandomVariableInterface getNumeraire(double time) throws CalculationException {
		int timeIndex = getLiborPeriodIndex(time);

		if(timeIndex < 0) {
			// Interpolation of Numeraire: log linear interpolation.
			int upperIndex = -timeIndex-1;
			int lowerIndex = upperIndex-1;
			if(lowerIndex < 0) throw new IllegalArgumentException("Numeraire requested for time " + time + ". Unsupported");

			double alpha = (time-getLiborPeriod(lowerIndex)) / (getLiborPeriod(upperIndex) - getLiborPeriod(lowerIndex));
			RandomVariableInterface numeraire = getNumeraire(getLiborPeriod(upperIndex)).log().mult(alpha).add(getNumeraire(getLiborPeriod(lowerIndex)).log().mult(1.0-alpha)).exp();

			double deterministicNumeraireAdjustment = (1 + forwardRateCurve.getForward(curveModel, getLiborPeriod(lowerIndex), time-getLiborPeriod(lowerIndex)) * (time-getLiborPeriod(lowerIndex)))/(1 + forwardRateCurve.getForward(curveModel, getLiborPeriod(lowerIndex), getLiborPeriod(upperIndex)-getLiborPeriod(lowerIndex)) * (time-getLiborPeriod(lowerIndex)));
			// @TODO This adjustment only applies if the corresponding adjustment in getLIBOR is enabled
//			numeraire = numeraire.mult(deterministicNumeraireAdjustment);
			return numeraire;
		}

		// Calculate the numeraire, when time is part of liborPeriodDiscretization

		// Get the start and end of the product
		int firstLiborIndex, lastLiborIndex;

		if(measure == Measure.TERMINAL) {
			firstLiborIndex	= getLiborPeriodIndex(time);
			if(firstLiborIndex < 0) {
				throw new CalculationException("Simulation time discretization not part of forward rate tenor discretization.");
			}

			lastLiborIndex 	= liborPeriodDiscretization.getNumberOfTimeSteps()-1;
		}
		else if(measure == Measure.SPOT) {
			// Spot measure
			firstLiborIndex	= 0;
			lastLiborIndex	= getLiborPeriodIndex(time)-1;
		}
		else {
			throw new CalculationException("Numeraire not implemented for specified measure.");
		}

		/*
		 * Calculation of the numeraire
		 */

		// Initialize to 1.0
		RandomVariableInterface numeraire = getProcess().getBrownianMotion().getRandomVariableForConstant(1.0);

		// The product 
		for(int liborIndex = firstLiborIndex; liborIndex<=lastLiborIndex; liborIndex++) {
			RandomVariableInterface libor = getLIBOR(getTimeIndex(Math.min(time,liborPeriodDiscretization.getTime(liborIndex))), liborIndex);

			double periodLength = liborPeriodDiscretization.getTimeStep(liborIndex);

			if(measure == Measure.SPOT) {
				numeraire = numeraire.accrue(libor, periodLength);
			}
			else {
				numeraire = numeraire.discount(libor, periodLength);
			}
		}

		/*
		 * Adjust for discounting, i.e. funding or collateralization
		 */
		if(discountCurve != null) {
			/*
			DiscountCurveInterface discountCurveFromForwardPerformance = new DiscountCurveFromForwardCurve(forwardRateCurve);
			double deterministicNumeraireAdjustment = discountCurveFromForwardPerformance.getDiscountFactor(time) / discountCurve.getDiscountFactor(time);
			*/

			// This includes a control for zero bonds
			double deterministicNumeraireAdjustment = numeraire.invert().getAverage() / discountCurve.getDiscountFactor(curveModel, time);
			numeraire = numeraire.mult(deterministicNumeraireAdjustment);
		}
		return numeraire;
	}

	@Override
	public RandomVariableInterface[] getInitialState() {
		double[] liborInitialStates = new double[liborPeriodDiscretization.getNumberOfTimeSteps()];
		for(int timeIndex=0; timeIndex<liborPeriodDiscretization.getNumberOfTimeSteps(); timeIndex++) {
			//			double rate = forwardRateCurve.getForward(curveModel, liborPeriodDiscretization.getTime(timeIndex), liborPeriodDiscretization.getTimeStep(timeIndex));
			double rate = forwardRateCurve.getForward(curveModel, liborPeriodDiscretization.getTime(timeIndex));
			liborInitialStates[timeIndex] = (stateSpace == StateSpace.LOGNORMAL) ? Math.log(Math.max(rate,0)) : rate;
		}

		RandomVariableInterface[] initialStateRandomVariable = new RandomVariableInterface[getNumberOfComponents()];
		for(int componentIndex=0; componentIndex<getNumberOfComponents(); componentIndex++) {
			initialStateRandomVariable[componentIndex] = getProcess().getBrownianMotion().getRandomVariableForConstant(liborInitialStates[componentIndex]);
		}
		return initialStateRandomVariable;
	}

	/**
	 * Return the complete vector of the drift for the time index timeIndex, given that current state is realizationAtTimeIndex.
	 * Note: The random variable returned is a defensive copy and may be modified.
	 * The drift will be zero for rates being already fixed.
	 * 
	 * @see net.finmath.montecarlo.interestrate.LIBORMarketModel#getNumeraire(double) The calculation of the drift is consistent with the calculation of the numeraire in <code>getNumeraire</code>.
	 * 
	 * @param timeIndex Time index <i>i</i> for which the drift should be returned <i>&mu;(t<sub>i</sub>)</i>.
	 * @param realizationAtTimeIndex Time current forward rate vector at time index <i>i</i> which should be used in the calculation.
	 * @return The drift vector &mu;(t<sub>i</sub>) as <code>RandomVariable[]</code>
	 */
	@Override
	public RandomVariableInterface[] getDrift(int timeIndex, RandomVariableInterface[] realizationAtTimeIndex, RandomVariableInterface[] realizationPredictor) {
		double	time				= getTime(timeIndex);
		int		firstLiborIndex		= this.getLiborPeriodIndex(time)+1;
		if(firstLiborIndex<0) firstLiborIndex = -firstLiborIndex-1 + 1;

		RandomVariableInterface		zero	= getProcess().getBrownianMotion().getRandomVariableForConstant(0.0);

		// Allocate drift vector and initialize to zero (will be used to sum up drift components)
		RandomVariableInterface[]	drift = new RandomVariableInterface[getNumberOfComponents()];
		for(int componentIndex=firstLiborIndex; componentIndex<getNumberOfComponents(); componentIndex++) {
			drift[componentIndex] = zero;
		}

		RandomVariableInterface[]	covarianceFactorSums	= new RandomVariableInterface[getNumberOfFactors()];
		for(int factorIndex=0; factorIndex<getNumberOfFactors(); factorIndex++) {
			covarianceFactorSums[factorIndex] = zero;
		}

		if(measure == Measure.SPOT) {
			// Calculate drift for the component componentIndex (starting at firstLiborIndex, others are zero)
			for(int componentIndex=firstLiborIndex; componentIndex<getNumberOfComponents(); componentIndex++) {
				double						periodLength	= liborPeriodDiscretization.getTimeStep(componentIndex);
				RandomVariableInterface		libor			= realizationAtTimeIndex[componentIndex];
				RandomVariableInterface		oneStepMeasureTransform = (getProcess().getBrownianMotion().getRandomVariableForConstant(periodLength)).discount(libor, periodLength);

				if(stateSpace == StateSpace.LOGNORMAL) oneStepMeasureTransform = oneStepMeasureTransform.mult(libor);

				RandomVariableInterface[]	factorLoading   	= getFactorLoading(timeIndex, componentIndex, realizationAtTimeIndex);
				for(int factorIndex=0; factorIndex<getNumberOfFactors(); factorIndex++) {
					covarianceFactorSums[factorIndex] = covarianceFactorSums[factorIndex].add(oneStepMeasureTransform.mult(factorLoading[factorIndex]));
					drift[componentIndex] = drift[componentIndex].addProduct(covarianceFactorSums[factorIndex], factorLoading[factorIndex]);
				}
			}
		}
		else if(measure == Measure.TERMINAL) {
			// Calculate drift for the component componentIndex (starting at firstLiborIndex, others are zero)
			for(int componentIndex=getNumberOfComponents()-1; componentIndex>=firstLiborIndex; componentIndex--) {
				double					periodLength	= liborPeriodDiscretization.getTimeStep(componentIndex);
				RandomVariableInterface libor			= realizationAtTimeIndex[componentIndex];
				RandomVariableInterface oneStepMeasureTransform = (getProcess().getBrownianMotion().getRandomVariableForConstant(periodLength)).discount(libor, periodLength);

				if(stateSpace == StateSpace.LOGNORMAL) oneStepMeasureTransform = oneStepMeasureTransform.mult(libor);

				RandomVariableInterface[]	factorLoading   	= getFactorLoading(timeIndex, componentIndex, realizationAtTimeIndex);
				for(int factorIndex=0; factorIndex<getNumberOfFactors(); factorIndex++) {
					drift[componentIndex] = drift[componentIndex].addProduct(covarianceFactorSums[factorIndex], factorLoading[factorIndex]);
					covarianceFactorSums[factorIndex] = covarianceFactorSums[factorIndex].sub(oneStepMeasureTransform.mult(factorLoading[factorIndex]));
				}
			}
		}

		if(stateSpace == StateSpace.LOGNORMAL) {
			// Drift adjustment for log-coordinate in each component
			for(int componentIndex=firstLiborIndex; componentIndex<getNumberOfComponents(); componentIndex++) {
				RandomVariableInterface		variance		= covarianceModel.getCovariance(getTime(timeIndex), componentIndex, componentIndex, realizationAtTimeIndex);
				drift[componentIndex] = drift[componentIndex].addProduct(variance, -0.5);
			}
		}

		return drift;
	}

	@Override
	public	RandomVariableInterface[]	getFactorLoading(int timeIndex, int componentIndex, RandomVariableInterface[] realizationAtTimeIndex)
	{
		return covarianceModel.getFactorLoading(getTime(timeIndex), getLiborPeriod(componentIndex), realizationAtTimeIndex);
	}

	@Override
	public RandomVariableInterface applyStateSpaceTransform(int componentIndex, RandomVariableInterface randomVariable) {
		if(stateSpace == StateSpace.LOGNORMAL)	return randomVariable.exp();
		else									return randomVariable;
	}


	/**
	 * @return Returns the driftApproximationMethod.
	 */
	public Driftapproximation getDriftApproximationMethod() {
		return driftApproximationMethod;
	}

	@Override
	public RandomVariableInterface getLIBOR(int timeIndex, int liborIndex) throws CalculationException
	{
		// This method is just a synonym - call getProcessValue of super class
		return getProcessValue(timeIndex, liborIndex);
	}

	@Override
	public int getNumberOfComponents() {
		return liborPeriodDiscretization.getNumberOfTimeSteps();
	}

	@Override
	public int getNumberOfLibors()
	{
		// This is just a synonym to number of components
		return getNumberOfComponents();
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.LIBORMarketModelInterface#getLiborPeriod(int)
	 */
	@Override
	public double getLiborPeriod(int timeIndex) {
		if(timeIndex >= liborPeriodDiscretization.getNumberOfTimes() || timeIndex < 0) {
			throw new ArrayIndexOutOfBoundsException("Index for LIBOR period discretization out of bounds: " + timeIndex + ".");
		}
		return liborPeriodDiscretization.getTime(timeIndex);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.LIBORMarketModelInterface#getLiborPeriodIndex(double)
	 */
	@Override
	public int getLiborPeriodIndex(double time) {
		return liborPeriodDiscretization.getTimeIndex(time);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.LIBORMarketModelInterface#getLiborPeriodDiscretization()
	 */
	@Override
	public TimeDiscretizationInterface getLiborPeriodDiscretization() {
		return liborPeriodDiscretization;
	}

	/**
	 * @return Returns the measure.
	 */
	public Measure getMeasure() {
		return measure;
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.LIBORMarketModelInterface#getIntegratedLIBORCovariance()
	 */
	@Override
	public double[][][] getIntegratedLIBORCovariance() {
		synchronized (integratedLIBORCovarianceLazyInitLock) {
			if(integratedLIBORCovariance == null) {
				TimeDiscretizationInterface liborPeriodDiscretization = getLiborPeriodDiscretization();
				TimeDiscretizationInterface simulationTimeDiscretization = getCovarianceModel().getTimeDiscretization();

				integratedLIBORCovariance = new double[simulationTimeDiscretization.getNumberOfTimeSteps()][liborPeriodDiscretization.getNumberOfTimeSteps()][liborPeriodDiscretization.getNumberOfTimeSteps()];
				for(int timeIndex = 0; timeIndex < simulationTimeDiscretization.getNumberOfTimeSteps(); timeIndex++) {
					double dt = simulationTimeDiscretization.getTime(timeIndex+1) - simulationTimeDiscretization.getTime(timeIndex);
					RandomVariableInterface[][] factorLoadings = new RandomVariableInterface[liborPeriodDiscretization.getNumberOfTimeSteps()][];
					// Prefetch factor loadings
					for(int componentIndex = 0; componentIndex < liborPeriodDiscretization.getNumberOfTimeSteps(); componentIndex++) {
						factorLoadings[componentIndex] = getCovarianceModel().getFactorLoading(timeIndex, componentIndex, null);
					}
					for(int componentIndex1 = 0; componentIndex1 < liborPeriodDiscretization.getNumberOfTimeSteps(); componentIndex1++) {
						RandomVariableInterface[] factorLoadingOfComponent1 = factorLoadings[componentIndex1];
						// Sum the libor cross terms (use symmetry)
						for(int componentIndex2 = componentIndex1; componentIndex2 < liborPeriodDiscretization.getNumberOfTimeSteps(); componentIndex2++) {
							double integratedLIBORCovarianceValue = 0.0;
							if(getLiborPeriod(componentIndex1) > getTime(timeIndex)) {
								RandomVariableInterface[] factorLoadingOfComponent2 = factorLoadings[componentIndex2];
								for(int factorIndex = 0; factorIndex < getNumberOfFactors(); factorIndex++) {
									integratedLIBORCovarianceValue += factorLoadingOfComponent1[factorIndex].get(0) * factorLoadingOfComponent2[factorIndex].get(0) * dt;
								}
							}
							integratedLIBORCovariance[timeIndex][componentIndex1][componentIndex2] = integratedLIBORCovarianceValue;
						}
					}
				}

				// Integrate over time (i.e. sum up).
				for(int timeIndex = 1; timeIndex < simulationTimeDiscretization.getNumberOfTimeSteps(); timeIndex++) {
					double[][] prevIntegratedLIBORCovariance = integratedLIBORCovariance[timeIndex-1];
					double[][] thisIntegratedLIBORCovariance = integratedLIBORCovariance[timeIndex];
					for(int componentIndex1 = 0; componentIndex1 < liborPeriodDiscretization.getNumberOfTimeSteps(); componentIndex1++) {
						for(int componentIndex2 = componentIndex1; componentIndex2 < liborPeriodDiscretization.getNumberOfTimeSteps(); componentIndex2++) {
							thisIntegratedLIBORCovariance[componentIndex1][componentIndex2] = prevIntegratedLIBORCovariance[componentIndex1][componentIndex2] + thisIntegratedLIBORCovariance[componentIndex1][componentIndex2];
							thisIntegratedLIBORCovariance[componentIndex2][componentIndex1] = thisIntegratedLIBORCovariance[componentIndex1][componentIndex2];
						}
					}
				}
			}
		}
		
		return integratedLIBORCovariance;
	}

	@Override
	public Object clone() {
		try {
			Map<String, Object> properties = new HashMap<String, Object>();
			properties.put("measure",		measure.name());
			properties.put("stateSpace",	stateSpace.name());
			return new LIBORMarketModel(liborPeriodDiscretization, forwardRateCurve, discountCurve, covarianceModel, new CalibrationItem[0], properties);
		} catch (CalculationException e) {
			return null;
		}
	}

	@Override
	public AnalyticModelInterface getAnalyticModel() {
		return curveModel;
	}

	@Override
	public DiscountCurveInterface getDiscountCurve() {
		if(discountCurve == null) {
			DiscountCurveInterface discountCurveFromForwardCurve = new DiscountCurveFromForwardCurve(getForwardRateCurve());
			return discountCurveFromForwardCurve;
		}

		return discountCurve;
	}

	@Override
	public ForwardCurveInterface getForwardRateCurve() {
		return forwardRateCurve;
	}

	/**
	 * Return the swaption market data used for calibration (if any, may be null).
	 * 
	 * @return The swaption market data used for calibration (if any, may be null).
	 */
	public AbstractSwaptionMarketData getSwaptionMarketData() {
		return swaptionMarketData;
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.LIBORMarketModelInterface#getCovarianceModel()
	 */
	@Override
	public AbstractLIBORCovarianceModel getCovarianceModel() {
		return covarianceModel;
	}

	/**
	 * @param covarianceModel A covariance model
	 * @return A new <code>LIBORMarketModel</code> using the specified covariance model.
	 */
	@Override
	public LIBORMarketModel getCloneWithModifiedCovarianceModel(AbstractLIBORCovarianceModel covarianceModel) {
		LIBORMarketModel model = (LIBORMarketModel)this.clone();
		model.covarianceModel = covarianceModel;
		return model;
	}

	@Override
	public LIBORMarketModel getCloneWithModifiedData(Map<String, Object> dataModified) throws CalculationException {
		TimeDiscretizationInterface		liborPeriodDiscretization	= this.liborPeriodDiscretization;
		AnalyticModelInterface			analyticModel				= this.curveModel;
		ForwardCurveInterface			forwardRateCurve			= this.forwardRateCurve;
		DiscountCurveInterface			discountCurve				= this.discountCurve;
		AbstractLIBORCovarianceModel	covarianceModel				= this.covarianceModel;
		AbstractSwaptionMarketData		swaptionMarketData			= null;		// No recalibration, unless new swaption data is specified
		Map<String, Object>				properties					= new HashMap<String, Object>();
		properties.put("measure",		measure.name());
		properties.put("stateSpace",	stateSpace.name());

		if(dataModified.containsKey("liborPeriodDiscretization")) {
			liborPeriodDiscretization = (TimeDiscretizationInterface)dataModified.get("liborPeriodDiscretization");
		}
		if(dataModified.containsKey("forwardRateCurve")) {
			forwardRateCurve = (ForwardCurveInterface)dataModified.get("forwardRateCurve");
		}
		if(dataModified.containsKey("discountCurve")) {
			discountCurve = (DiscountCurveInterface)dataModified.get("discountCurve");
		}
		if(dataModified.containsKey("forwardRateShift")) {
			throw new RuntimeException("Forward rate shift clone currently disabled.");
		}
		if(dataModified.containsKey("covarianceModel")) {
			covarianceModel = (AbstractLIBORCovarianceModel)dataModified.get("covarianceModel");
		}
		if(dataModified.containsKey("swaptionMarketData")) {
			swaptionMarketData = (AbstractSwaptionMarketData)dataModified.get("swaptionMarketData");
		}

		LIBORMarketModel newModel = new LIBORMarketModel(liborPeriodDiscretization, forwardRateCurve, discountCurve, covarianceModel, swaptionMarketData, properties);
		newModel.curveModel = analyticModel;
		return newModel;
	}

	@Override
	public String toString() {
		return "LIBORMarketModel [liborPeriodDiscretization="
				+ liborPeriodDiscretization + ", forwardCurveName="
				+ forwardCurveName + ", curveModel=" + curveModel
				+ ", forwardRateCurve=" + forwardRateCurve + ", discountCurve="
				+ discountCurve + ", covarianceModel=" + covarianceModel
				+ ", driftApproximationMethod=" + driftApproximationMethod
				+ ", measure=" + measure + ", stateSpace=" + stateSpace + "]";
	}
}


