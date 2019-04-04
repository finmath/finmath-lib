/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2004
 */
package net.finmath.montecarlo.interestrate.models;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import net.finmath.exception.CalculationException;
import net.finmath.functions.AnalyticFormulas;
import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.DiscountCurveFromForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.model.volatilities.AbstractSwaptionMarketData;
import net.finmath.marketdata.products.Swap;
import net.finmath.marketdata.products.SwapAnnuity;
import net.finmath.montecarlo.AbstractRandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.interestrate.CalibrationProduct;
import net.finmath.montecarlo.interestrate.LIBORMarketModel;
import net.finmath.montecarlo.interestrate.models.covariance.AbstractLIBORCovarianceModelParametric;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCovarianceModel;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCovarianceModelCalibrateable;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.montecarlo.interestrate.products.SwaptionAnalyticApproximation;
import net.finmath.montecarlo.interestrate.products.SwaptionSimple;
import net.finmath.montecarlo.model.AbstractProcessModel;
import net.finmath.montecarlo.process.MonteCarloProcess;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.RegularSchedule;
import net.finmath.time.Schedule;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * Implements a (generalized) LIBOR market model with generic covariance structure (lognormal, normal, displaced or stochastic volatility)
 * with some drift approximation methods.
 * <br><br>
 * In its default case the class specifies a multi-factor LIBOR market model in its log-normal formulation, that is
 * <i>L<sub>j</sub> = exp(Y<sub>j</sub>) </i> where
 * \[
 * 		dY_{j} = \mu_{j} dt + \lambda_{1,j} dW_{1} + \ldots + \lambda_{m,j} dW_{m}
 * \]
 * <br>
 * The model uses an {@link net.finmath.montecarlo.interestrate.models.covariance.LIBORCovarianceModel} for the specification of
 * <i>(&lambda;<sub>1,j</sub>,...,&lambda;<sub>m,j</sub>)</i> as a covariance model.
 * See {@link net.finmath.montecarlo.model.ProcessModel} for details on the implemented interface
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
 * see {@link net.finmath.montecarlo.model.ProcessModel} for details on the implemented interface.
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
 * 		<li>
 * 			<code>liborCap</code>: An optional <code>Double</code> value applied as a cap to the LIBOR rates.
 * 			May be used to limit the simulated valued to prevent values attaining POSITIVE_INFINITY and
 * 			numerical problems. To disable the cap, set <code>liborCap</code> to <code>Double.POSITIVE_INFINITY</code>.
 *		</li>
 * </ul>
 * <br>
 * The main task of this class is to calculate the risk-neutral drift and the
 * corresponding numeraire given the covariance model.
 *
 * The calibration of the covariance structure is not part of this class. For the calibration
 * of parametric models of the instantaneous covariance see
 * {@link net.finmath.montecarlo.interestrate.models.covariance.AbstractLIBORCovarianceModelParametric#getCloneCalibrated(LIBORMarketModel, CalibrationProduct[], Map)}.
 *
 * @author Christian Fries
 * @version 1.2
 * @see net.finmath.montecarlo.process.MonteCarloProcess The interface for numerical schemes.
 * @see net.finmath.montecarlo.model.ProcessModel The interface for models provinding parameters to numerical schemes.
 * @see net.finmath.montecarlo.interestrate.models.covariance.AbstractLIBORCovarianceModel The abstract covariance model plug ins.
 * @see net.finmath.montecarlo.interestrate.models.covariance.AbstractLIBORCovarianceModelParametric A parametic covariance model including a generic calibration algorithm.
 */
public class LIBORMarketModelFromCovarianceModel extends AbstractProcessModel implements LIBORMarketModel, Serializable {

	private static final long serialVersionUID = 4166077559001066615L;

	public enum Measure				{ SPOT, TERMINAL }
	public enum StateSpace			{ NORMAL, LOGNORMAL }
	public enum Driftapproximation	{ EULER, LINE_INTEGRAL, PREDICTOR_CORRECTOR }
	public enum InterpolationMethod	{ LINEAR, LOG_LINEAR_UNCORRECTED, LOG_LINEAR_CORRECTED }

	private final TimeDiscretization		liborPeriodDiscretization;

	private String							forwardCurveName;
	private AnalyticModel			curveModel;

	private ForwardCurve			forwardRateCurve;
	private DiscountCurve			discountCurve;

	private final AbstractRandomVariableFactory	randomVariableFactory;
	private LIBORCovarianceModel	covarianceModel;

	private AbstractSwaptionMarketData		swaptionMarketData;

	private Driftapproximation	driftApproximationMethod	= Driftapproximation.EULER;
	private Measure				measure						= Measure.SPOT;
	private StateSpace			stateSpace					= StateSpace.LOGNORMAL;
	private InterpolationMethod interpolationMethod			= InterpolationMethod.LOG_LINEAR_UNCORRECTED;
	private double				liborCap					= 1E5;

	// This is a cache of the integrated covariance.
	private double[][][]		integratedLIBORCovariance;
	private transient Object	integratedLIBORCovarianceLazyInitLock = new Object();

	// Cache for the numeraires, needs to be invalidated if process changes - move out of the object (to process?)
	private transient MonteCarloProcess						numerairesProcess = null;
	private transient ConcurrentHashMap<Integer, RandomVariable>	numeraires = new ConcurrentHashMap<>();
	private transient ConcurrentHashMap<Double, RandomVariable>		numeraireDiscountFactorForwardRates = new ConcurrentHashMap<>();
	private transient ConcurrentHashMap<Double, RandomVariable>		numeraireDiscountFactors = new ConcurrentHashMap<>();
	private transient Vector<RandomVariable>						interpolationDriftAdjustmentsTerminal = new Vector<RandomVariable>();

	/**
	 * Creates a LIBOR Market Model for given covariance.
	 *
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
	 * 			<code>liborCap</code>: An optional <code>Double</code> value applied as a cap to the LIBOR rates.
	 * 			May be used to limit the simulated valued to prevent values attaining POSITIVE_INFINITY and
	 * 			numerical problems. To disable the cap, set <code>liborCap</code> to <code>Double.POSITIVE_INFINITY</code>.
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
	 * @param randomVariableFactory The random variable factory used to create the inital values of the model.
	 * @param covarianceModel The covariance model to use.
	 * @param properties Key value map specifying properties like <code>measure</code> and <code>stateSpace</code>.
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	public LIBORMarketModelFromCovarianceModel(
			TimeDiscretization			liborPeriodDiscretization,
			AnalyticModel			analyticModel,
			ForwardCurve			forwardRateCurve,
			DiscountCurve			discountCurve,
			AbstractRandomVariableFactory	randomVariableFactory,
			LIBORCovarianceModel			covarianceModel,
			Map<String, ?>					properties
			) throws CalculationException {

		// Set some properties
		if(properties != null && properties.containsKey("measure")) {
			measure		= Measure.valueOf(((String)properties.get("measure")).toUpperCase());
		}
		if(properties != null && properties.containsKey("stateSpace")) {
			stateSpace	= StateSpace.valueOf(((String)properties.get("stateSpace")).toUpperCase());
		}
		if(properties != null && properties.containsKey("interpolationMethod")) {
			interpolationMethod	= InterpolationMethod.valueOf(((String)properties.get("interpolationMethod")).toUpperCase());
		}
		if(properties != null && properties.containsKey("liborCap")) {
			liborCap	= (Double)properties.get("liborCap");
		}

		Map<String,Object> calibrationParameters = null;
		if(properties != null && properties.containsKey("calibrationParameters")) {
			calibrationParameters	= (Map<String,Object>)properties.get("calibrationParameters");
		}

		this.liborPeriodDiscretization	= liborPeriodDiscretization;
		curveModel					= analyticModel;
		this.forwardRateCurve		= forwardRateCurve;
		this.discountCurve			= discountCurve;
		this.randomVariableFactory	= randomVariableFactory;
		this.covarianceModel	= covarianceModel;
	}

	/**
	 * Creates a LIBOR Market Model for given covariance with a calibration (if calibration items are given).
	 * <br>
	 * If calibrationItems in non-empty and the covariance model is a parametric model,
	 * the covariance will be replaced by a calibrate version of the same model, i.e.,
	 * the LIBOR Market Model will be calibrated. Note: Calibration is not lazy.
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
	 * 			<code>liborCap</code>: An optional <code>Double</code> value applied as a cap to the LIBOR rates.
	 * 			May be used to limit the simulated valued to prevent values attaining POSITIVE_INFINITY and
	 * 			numerical problems. To disable the cap, set <code>liborCap</code> to <code>Double.POSITIVE_INFINITY</code>.
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
	 * @param randomVariableFactory The random variable factory used to create the inital values of the model.
	 * @param covarianceModel The covariance model to use.
	 * @param calibrationProducts The vector of calibration items (a union of a product, target value and weight) for the objective function sum weight(i) * (modelValue(i)-targetValue(i).
	 * @param properties Key value map specifying properties like <code>measure</code> and <code>stateSpace</code>.
	 * @return A new instance of LIBORMarketModelFromCovarianceModel, possibly calibrated.
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	public static LIBORMarketModelFromCovarianceModel of(
			TimeDiscretization			liborPeriodDiscretization,
			AnalyticModel			analyticModel,
			ForwardCurve			forwardRateCurve,
			DiscountCurve			discountCurve,
			AbstractRandomVariableFactory	randomVariableFactory,
			LIBORCovarianceModel			covarianceModel,
			CalibrationProduct[]			calibrationProducts,
			Map<String, ?>					properties
			) throws CalculationException {

		LIBORMarketModelFromCovarianceModel model = new LIBORMarketModelFromCovarianceModel(liborPeriodDiscretization, analyticModel, forwardRateCurve, discountCurve, randomVariableFactory, covarianceModel, properties);

		// Perform calibration, if data is given
		if(calibrationProducts != null && calibrationProducts.length > 0) {
			Map<String,Object> calibrationParameters = null;
			if(properties != null && properties.containsKey("calibrationParameters")) {
				calibrationParameters	= (Map<String,Object>)properties.get("calibrationParameters");
			}

			LIBORCovarianceModelCalibrateable covarianceModelParametric = null;
			try {
				covarianceModelParametric = (LIBORCovarianceModelCalibrateable)covarianceModel;
			}
			catch(Exception e) {
				throw new ClassCastException("Calibration restricted to covariance models implementing LIBORCovarianceModelCalibrateable.");
			}

			LIBORCovarianceModel covarianceModelCalibrated = covarianceModelParametric.getCloneCalibrated(model, calibrationProducts, calibrationParameters);

			LIBORMarketModelFromCovarianceModel modelCalibrated = model.getCloneWithModifiedCovarianceModel(covarianceModelCalibrated);

			return modelCalibrated;
		}
		else {
			return model;
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
	 * 			<code>liborCap</code>: An optional <code>Double</code> value applied as a cap to the LIBOR rates.
	 * 			May be used to limit the simulated valued to prevent values attaining POSITIVE_INFINITY and
	 * 			numerical problems. To disable the cap, set <code>liborCap</code> to <code>Double.POSITIVE_INFINITY</code>.
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
	 * @param randomVariableFactory The random variable factory used to create the inital values of the model.
	 * @param covarianceModel The covariance model to use.
	 * @param calibrationProducts The vector of calibration items (a union of a product, target value and weight) for the objective function sum weight(i) * (modelValue(i)-targetValue(i).
	 * @param properties Key value map specifying properties like <code>measure</code> and <code>stateSpace</code>.
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 * @deprecated Use LIBORMarketModelFromCovarianceModel.of() instead.
	 */
	@Deprecated
	public LIBORMarketModelFromCovarianceModel(
			TimeDiscretization			liborPeriodDiscretization,
			AnalyticModel			analyticModel,
			ForwardCurve			forwardRateCurve,
			DiscountCurve			discountCurve,
			AbstractRandomVariableFactory	randomVariableFactory,
			LIBORCovarianceModel			covarianceModel,
			CalibrationProduct[]			calibrationProducts,
			Map<String, ?>					properties
			) throws CalculationException {

		// Set some properties
		if(properties != null && properties.containsKey("measure")) {
			measure		= Measure.valueOf(((String)properties.get("measure")).toUpperCase());
		}
		if(properties != null && properties.containsKey("stateSpace")) {
			stateSpace	= StateSpace.valueOf(((String)properties.get("stateSpace")).toUpperCase());
		}
		if(properties != null && properties.containsKey("interpolationMethod")) {
			interpolationMethod	= InterpolationMethod.valueOf(((String)properties.get("interpolationMethod")).toUpperCase());
		}
		if(properties != null && properties.containsKey("liborCap")) {
			liborCap	= (Double)properties.get("liborCap");
		}

		Map<String,Object> calibrationParameters = null;
		if(properties != null && properties.containsKey("calibrationParameters")) {
			calibrationParameters	= (Map<String,Object>)properties.get("calibrationParameters");
		}

		this.liborPeriodDiscretization	= liborPeriodDiscretization;
		curveModel					= analyticModel;
		this.forwardRateCurve		= forwardRateCurve;
		this.discountCurve			= discountCurve;
		this.randomVariableFactory	= randomVariableFactory;

		// Perform calibration, if data is given
		if(calibrationProducts != null && calibrationProducts.length > 0) {
			LIBORCovarianceModelCalibrateable covarianceModelParametric = null;
			try {
				covarianceModelParametric = (LIBORCovarianceModelCalibrateable)covarianceModel;
			}
			catch(Exception e) {
				throw new ClassCastException("Calibration restricted to covariance models implementing LIBORCovarianceModelCalibrateable.");
			}

			this.covarianceModel    = covarianceModelParametric.getCloneCalibrated(this, calibrationProducts, calibrationParameters);
		}
		else {
			this.covarianceModel	= covarianceModel;
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
	 * 			<code>liborCap</code>: An optional <code>Double</code> value applied as a cap to the LIBOR rates.
	 * 			May be used to limit the simulated valued to prevent values attaining POSITIVE_INFINITY and
	 * 			numerical problems. To disable the cap, set <code>liborCap</code> to <code>Double.POSITIVE_INFINITY</code>.
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
	 * @deprecated Use LIBORMarketModelFromCovarianceModel.of() instead.
	 */
	@Deprecated
	public LIBORMarketModelFromCovarianceModel(
			TimeDiscretization			liborPeriodDiscretization,
			AnalyticModel				analyticModel,
			ForwardCurve				forwardRateCurve,
			DiscountCurve				discountCurve,
			LIBORCovarianceModel		covarianceModel,
			CalibrationProduct[]					calibrationItems,
			Map<String, ?>						properties
			) throws CalculationException {
		this(liborPeriodDiscretization, analyticModel, forwardRateCurve, discountCurve, new RandomVariableFactory(), covarianceModel, calibrationItems, properties);
	}

	/**
	 * Creates a LIBOR Market Model for given covariance.
	 *
	 * @param liborPeriodDiscretization The discretization of the interest rate curve into forward rates (tenor structure).
	 * @param forwardRateCurve The initial values for the forward rates.
	 * @param covarianceModel The covariance model to use.
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	public LIBORMarketModelFromCovarianceModel(
			TimeDiscretization		liborPeriodDiscretization,
			ForwardCurve			forwardRateCurve,
			LIBORCovarianceModel	covarianceModel
			) throws CalculationException {
		this(liborPeriodDiscretization, forwardRateCurve, new DiscountCurveFromForwardCurve(forwardRateCurve), covarianceModel, new CalibrationProduct[0], null);
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
	public LIBORMarketModelFromCovarianceModel(
			TimeDiscretization		liborPeriodDiscretization,
			ForwardCurve			forwardRateCurve,
			DiscountCurve			discountCurve,
			LIBORCovarianceModel	covarianceModel
			) throws CalculationException {
		this(liborPeriodDiscretization, forwardRateCurve, discountCurve, covarianceModel, new CalibrationProduct[0], null);
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
	public LIBORMarketModelFromCovarianceModel(
			TimeDiscretization			liborPeriodDiscretization,
			ForwardCurve				forwardRateCurve,
			LIBORCovarianceModel		covarianceModel,
			AbstractSwaptionMarketData			swaptionMarketData
			) throws CalculationException {
		this(liborPeriodDiscretization, forwardRateCurve, new DiscountCurveFromForwardCurve(forwardRateCurve), covarianceModel, swaptionMarketData, null);
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
	public LIBORMarketModelFromCovarianceModel(
			TimeDiscretization			liborPeriodDiscretization,
			ForwardCurve				forwardRateCurve,
			DiscountCurve				discountCurve,
			LIBORCovarianceModel		covarianceModel,
			AbstractSwaptionMarketData			swaptionMarketData
			) throws CalculationException {
		this(liborPeriodDiscretization, forwardRateCurve, discountCurve, covarianceModel, swaptionMarketData, null);
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
	public LIBORMarketModelFromCovarianceModel(
			TimeDiscretization			liborPeriodDiscretization,
			ForwardCurve				forwardRateCurve,
			DiscountCurve				discountCurve,
			LIBORCovarianceModel		covarianceModel,
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
	 * @deprecated Use LIBORMarketModelFromCovarianceModel.of() instead.
	 */
	@Deprecated
	public LIBORMarketModelFromCovarianceModel(
			TimeDiscretization			liborPeriodDiscretization,
			ForwardCurve				forwardRateCurve,
			DiscountCurve				discountCurve,
			LIBORCovarianceModel		covarianceModel,
			CalibrationProduct[]					calibrationItems,
			Map<String, ?>						properties
			) throws CalculationException {
		this(liborPeriodDiscretization, null, forwardRateCurve, discountCurve, covarianceModel, calibrationItems, properties);
	}

	private static CalibrationProduct[] getCalibrationItems(TimeDiscretization liborPeriodDiscretization, ForwardCurve forwardCurve, AbstractSwaptionMarketData swaptionMarketData, boolean isUseAnalyticApproximation) {
		if(swaptionMarketData == null) {
			return null;
		}

		TimeDiscretization	optionMaturities		= swaptionMarketData.getOptionMaturities();
		TimeDiscretization	tenor					= swaptionMarketData.getTenor();
		double						swapPeriodLength		= swaptionMarketData.getSwapPeriodLength();

		ArrayList<CalibrationProduct> calibrationProducts = new ArrayList<>();
		for(int exerciseIndex=0; exerciseIndex<=optionMaturities.getNumberOfTimeSteps(); exerciseIndex++) {
			for(int tenorIndex=0; tenorIndex<=tenor.getNumberOfTimeSteps()-exerciseIndex; tenorIndex++) {

				// Create a swaption
				double exerciseDate	= optionMaturities.getTime(exerciseIndex);
				double swapLength	= tenor.getTime(tenorIndex);

				if(liborPeriodDiscretization.getTimeIndex(exerciseDate) < 0) {
					continue;
				}
				if(liborPeriodDiscretization.getTimeIndex(exerciseDate+swapLength) <= liborPeriodDiscretization.getTimeIndex(exerciseDate)) {
					continue;
				}

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
				Schedule swapTenor = new RegularSchedule(new TimeDiscretizationFromArray(swapTenorTimes));
				double swaprate = Swap.getForwardSwapRate(swapTenor, swapTenor, forwardCurve, null);

				// Set swap rates for each period
				double[] swaprates        = new double[numberOfPeriods];
				for(int periodStartIndex=0; periodStartIndex<numberOfPeriods; periodStartIndex++) {
					swaprates[periodStartIndex] = swaprate;
				}

				if(isUseAnalyticApproximation) {
					AbstractLIBORMonteCarloProduct swaption = new SwaptionAnalyticApproximation(swaprate, swapTenorTimes, SwaptionAnalyticApproximation.ValueUnit.VOLATILITYLOGNORMAL);
					double impliedVolatility = swaptionMarketData.getVolatility(exerciseDate, swapLength, swaptionMarketData.getSwapPeriodLength(), swaprate);

					calibrationProducts.add(new CalibrationProduct(swaption, impliedVolatility, 1.0));
				}
				else {
					AbstractLIBORMonteCarloProduct swaption = new SwaptionSimple(swaprate, swapTenorTimes, SwaptionSimple.ValueUnit.VALUE);

					double forwardSwaprate		= Swap.getForwardSwapRate(swapTenor, swapTenor, forwardCurve);
					double swapAnnuity 			= SwapAnnuity.getSwapAnnuity(swapTenor, forwardCurve);
					double impliedVolatility	= swaptionMarketData.getVolatility(exerciseDate, swapLength, swaptionMarketData.getSwapPeriodLength(), swaprate);

					double targetValue = AnalyticFormulas.blackModelSwaptionValue(forwardSwaprate, impliedVolatility, exerciseDate, swaprate, swapAnnuity);

					calibrationProducts.add(new CalibrationProduct(swaption, targetValue, 1.0));
				}
			}
		}

		return calibrationProducts.toArray(new CalibrationProduct[calibrationProducts.size()]);
	}

	@Override
	public LocalDateTime getReferenceDate() {
		return forwardRateCurve.getReferenceDate() != null ? forwardRateCurve.getReferenceDate().atStartOfDay() : null;
	}

	/**
	 * Return the numeraire at a given time.
	 *
	 * The numeraire is provided for interpolated points. If requested on points which are not
	 * part of the tenor discretization, the numeraire uses a linear interpolation of the reciprocal
	 * value. See ISBN 0470047224 for details.
	 *
	 * @param time Time time <i>t</i> for which the numeraire should be returned <i>N(t)</i>.
	 * @return The numeraire at the specified time as <code>RandomVariable</code>
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	@Override
	public RandomVariable getNumeraire(double time) throws CalculationException {
		if(time < 0) {
			return randomVariableFactory.createRandomVariable(discountCurve.getDiscountFactor(curveModel, time));
		}

		RandomVariable numeraire = getNumerairetUnAdjusted(time);
		/*
		 * Adjust for discounting, i.e. funding or collateralization
		 */
		if (discountCurve != null) {
			RandomVariable deterministicNumeraireAdjustment = getNumeraireAdjustment(time);

			numeraire = numeraire.mult(numeraire.invert().getAverage()).div(deterministicNumeraireAdjustment);
		}
		return numeraire;
	}

	private RandomVariable getNumeraireAdjustment(double time) {
		boolean isInterpolateDiscountFactorsOnLiborPeriodDiscretization = true;

		TimeDiscretization timeDiscretizationForCurves = isInterpolateDiscountFactorsOnLiborPeriodDiscretization ? liborPeriodDiscretization : getProcess().getTimeDiscretization();

		int timeIndex = timeDiscretizationForCurves.getTimeIndex(time);
		if(timeIndex >= 0) {
			return getNumeraireAdjustment(timeIndex);
		}
		else {
			// Interpolation
			int timeIndexPrev = Math.min(-timeIndex-2, getLiborPeriodDiscretization().getNumberOfTimes()-2);
			int timeIndexNext = timeIndexPrev+1;
			double timePrev = timeDiscretizationForCurves.getTime(timeIndexPrev);
			double timeNext = timeDiscretizationForCurves.getTime(timeIndexNext);
			RandomVariable numeraireAdjustmentPrev = getNumeraireAdjustment(timeIndexPrev);
			RandomVariable numeraireAdjustmentNext = getNumeraireAdjustment(timeIndexNext);
			return numeraireAdjustmentPrev.mult(numeraireAdjustmentNext.div(numeraireAdjustmentPrev).pow((time-timePrev)/(timeNext-timePrev)));
		}
	}

	private RandomVariable getNumeraireAdjustment(int timeIndex) {
		boolean isInterpolateDiscountFactorsOnLiborPeriodDiscretization = true;

		TimeDiscretization timeDiscretizationForCurves = isInterpolateDiscountFactorsOnLiborPeriodDiscretization ? liborPeriodDiscretization : getProcess().getTimeDiscretization();
		double time = timeDiscretizationForCurves.getTime(timeIndex);

		synchronized(numeraireDiscountFactorForwardRates) {
			ensureCacheConsistency();

			RandomVariable deterministicNumeraireAdjustment = numeraireDiscountFactors.get(time);
			if(deterministicNumeraireAdjustment == null) {
				double dfInitial = discountCurve.getDiscountFactor(curveModel, timeDiscretizationForCurves.getTime(0));
				deterministicNumeraireAdjustment = randomVariableFactory.createRandomVariable(dfInitial);
				numeraireDiscountFactors.put(timeDiscretizationForCurves.getTime(0), deterministicNumeraireAdjustment);

				for(int i=0; i<timeDiscretizationForCurves.getNumberOfTimeSteps(); i++) {
					double dfPrev = discountCurve.getDiscountFactor(curveModel, timeDiscretizationForCurves.getTime(i));
					double dfNext = discountCurve.getDiscountFactor(curveModel, timeDiscretizationForCurves.getTime(i+1));
					double timeStep = timeDiscretizationForCurves.getTimeStep(i);
					double timeNext = timeDiscretizationForCurves.getTime(i+1);
					RandomVariable forwardRate = randomVariableFactory.createRandomVariable((dfPrev / dfNext - 1.0) / timeStep);
					numeraireDiscountFactorForwardRates.put(timeDiscretizationForCurves.getTime(i), forwardRate);
					deterministicNumeraireAdjustment = deterministicNumeraireAdjustment.discount(forwardRate, timeStep);
					numeraireDiscountFactors.put(timeNext, deterministicNumeraireAdjustment);
				}
				deterministicNumeraireAdjustment = numeraireDiscountFactors.get(time);
			}
			return deterministicNumeraireAdjustment;
		}
	}

	public RandomVariable getForwardDiscountBond(double time, double maturity) throws CalculationException {
		RandomVariable inverseForwardBondAsOfTime = getLIBOR(time, time, maturity).mult(maturity-time).add(1.0);
		RandomVariable inverseForwardBondAsOfZero = getLIBOR(0.0, time, maturity).mult(maturity-time).add(1.0);
		RandomVariable forwardDiscountBondAsOfZero = getNumeraireAdjustment(maturity).div(getNumeraireAdjustment(time));
		return forwardDiscountBondAsOfZero.mult(inverseForwardBondAsOfZero).div(inverseForwardBondAsOfTime);
	}

	private void ensureCacheConsistency() {
		/*
		 * Check if caches are valid (i.e. process did not change)
		 */
		if (getProcess() != numerairesProcess) {
			// Clear caches
			numeraires.clear();
			numeraireDiscountFactorForwardRates.clear();
			numeraireDiscountFactors.clear();
			numerairesProcess = getProcess();
			interpolationDriftAdjustmentsTerminal.clear();
		}
	}

	protected RandomVariable getNumerairetUnAdjusted(double time) throws CalculationException {
		/*
		 * Check if numeraire is on LIBOR time grid
		 */
		int liborTimeIndex = getLiborPeriodIndex(time);
		RandomVariable numeraireUnadjusted;
		if (liborTimeIndex < 0) {
			/*
			 * Interpolation of Numeraire: use already interpolated short Libor
			 */
			int upperIndex = -liborTimeIndex - 1;
			int lowerIndex = upperIndex - 1;
			if (lowerIndex < 0) {
				throw new IllegalArgumentException("Numeraire requested for time " + time + ". Unsupported");
			}
			if (measure == Measure.TERMINAL) {
				/*
				 * Due to time < T_{timeIndex+1} loop is needed.
				 */
				numeraireUnadjusted = getRandomVariableForConstant(1.0);
				for (int liborIndex = upperIndex; liborIndex <= liborPeriodDiscretization.getNumberOfTimeSteps() - 1; liborIndex++) {
					RandomVariable libor = getLIBOR(getTimeIndex(Math.min(time, liborPeriodDiscretization.getTime(liborIndex))), liborIndex);
					double periodLength = liborPeriodDiscretization.getTimeStep(liborIndex);
					numeraireUnadjusted = numeraireUnadjusted.discount(libor, periodLength);
				}
			}
			else if (measure == Measure.SPOT) {
				numeraireUnadjusted = getNumerairetUnAdjusted(getLiborPeriod(upperIndex));
			}
			else {
				throw new CalculationException("Numeraire not implemented for specified measure.");
			}

			/*
			 * Multiply with short period bond
			 */
			numeraireUnadjusted = numeraireUnadjusted.discount(getLIBOR(time, time, getLiborPeriod(upperIndex)), getLiborPeriod(upperIndex) - time);

			return numeraireUnadjusted;
		}
		else {
			/*
			 * Calculate the numeraire, when time is part of liborPeriodDiscretization
			 */

			return getNumerairetUnAdjustedAtLIBORIndex(liborTimeIndex);
		}
	}

	protected RandomVariable getNumerairetUnAdjustedAtLIBORIndex(int liborTimeIndex) throws CalculationException {
		/*
		 * synchronize lazy init cache
		 */
		synchronized(numeraires) {
			/*
			 * Check if numeraire cache is valid (i.e. process did not change)
			 */
			ensureCacheConsistency();

			/*
			 * Check if numeraire is part of the cache
			 */
			RandomVariable numeraireUnadjusted = numeraires.get(liborTimeIndex);
			if (numeraireUnadjusted == null) {
				if (measure == Measure.TERMINAL) {
					int timeIndex = getTimeIndex(liborPeriodDiscretization.getTime(liborTimeIndex));
					if(timeIndex < 0) {
						timeIndex = -timeIndex -1;
					}

					// Initialize to 1.0
					numeraireUnadjusted = getRandomVariableForConstant(1.0);

					/*
					 * Due to time < T_{timeIndex+1} loop is needed.
					 */
					for (int liborIndex = liborTimeIndex; liborIndex <= liborPeriodDiscretization.getNumberOfTimeSteps() - 1; liborIndex++) {
						RandomVariable libor = getLIBOR(timeIndex, liborIndex);
						double periodLength = liborPeriodDiscretization.getTimeStep(liborIndex);
						numeraireUnadjusted = numeraireUnadjusted.discount(libor, periodLength);
					}
				}
				else if (measure == Measure.SPOT) {
					/*
					 * If numeraire is not N(0), multiply (1 + L(Ti-1)*dt) on N(Ti-1)
					 */
					if (liborTimeIndex != 0) {
						int timeIndex = getTimeIndex(liborPeriodDiscretization.getTime(liborTimeIndex-1));
						if(timeIndex < 0) {
							timeIndex = -timeIndex -1;
						}

						double periodLength = liborPeriodDiscretization.getTimeStep(liborTimeIndex - 1);
						RandomVariable libor = getLIBOR(timeIndex, liborTimeIndex - 1);
						numeraireUnadjusted = getNumerairetUnAdjustedAtLIBORIndex(liborTimeIndex - 1).accrue(libor, periodLength);
					}
					else {
						numeraireUnadjusted = getRandomVariableForConstant(1.0);
					}
				} else {
					throw new CalculationException("Numeraire not implemented for specified measure.");
				}
				numeraires.put(liborTimeIndex, numeraireUnadjusted);
			}
			return numeraireUnadjusted;
		}
	}

	public Map<Double, RandomVariable> getNumeraireAdjustments() {
		return Collections.unmodifiableMap(numeraireDiscountFactorForwardRates);
	}

	@Override
	public RandomVariable[] getInitialState() {
		double[] liborInitialStates = new double[liborPeriodDiscretization.getNumberOfTimeSteps()];
		for(int timeIndex=0; timeIndex<liborPeriodDiscretization.getNumberOfTimeSteps(); timeIndex++) {
			double rate = forwardRateCurve.getForward(curveModel, liborPeriodDiscretization.getTime(timeIndex), liborPeriodDiscretization.getTimeStep(timeIndex));
			liborInitialStates[timeIndex] = (stateSpace == StateSpace.LOGNORMAL) ? Math.log(Math.max(rate,0)) : rate;
		}

		RandomVariable[] initialStateRandomVariable = new RandomVariable[getNumberOfComponents()];
		for(int componentIndex=0; componentIndex<getNumberOfComponents(); componentIndex++) {
			initialStateRandomVariable[componentIndex] = getRandomVariableForConstant(liborInitialStates[componentIndex]);
		}
		return initialStateRandomVariable;
	}

	/**
	 * Return the complete vector of the drift for the time index timeIndex, given that current state is realizationAtTimeIndex.
	 * The drift will be zero for rates being already fixed.
	 *
	 * The method currently provides the drift for either <code>Measure.SPOT</code> or <code>Measure.TERMINAL</code> - depending how the
	 * model object was constructed. For <code>Measure.TERMINAL</code> the j-th entry of the return value is the random variable
	 * \[
	 * \mu_{j}^{\mathbb{Q}^{P(T_{n})}}(t) \ = \ - \mathop{\sum_{l\geq j+1}}_{l\leq n-1} \frac{\delta_{l}}{1+\delta_{l} L_{l}(t)} (\lambda_{j}(t) \cdot \lambda_{l}(t))
	 * \]
	 * and for <code>Measure.SPOT</code> the j-th entry of the return value is the random variable
	 * \[
	 * \mu_{j}^{\mathbb{Q}^{N}}(t) \ = \ \sum_{m(t) &lt; l\leq j} \frac{\delta_{l}}{1+\delta_{l} L_{l}(t)} (\lambda_{j}(t) \cdot \lambda_{l}(t))
	 * \]
	 * where \( \lambda_{j} \) is the vector for factor loadings for the j-th component of the stochastic process (that is, the diffusion part is
	 * \( \sum_{k=1}^m \lambda_{j,k} \mathrm{d}W_{k} \)).
	 *
	 * Note: The scalar product of the factor loadings determines the instantaneous covariance. If the model is written in log-coordinates (using exp as a state space transform), we find
	 * \(\lambda_{j} \cdot \lambda_{l} = \sum_{k=1}^m \lambda_{j,k} \lambda_{l,k} = \sigma_{j} \sigma_{l} \rho_{j,l} \).
	 * If the model is written without a state space transformation (in its orignial coordinates) then \(\lambda_{j} \cdot \lambda_{l} = \sum_{k=1}^m \lambda_{j,k} \lambda_{l,k} = L_{j} L_{l} \sigma_{j} \sigma_{l} \rho_{j,l} \).
	 *
	 *
	 * @see net.finmath.montecarlo.interestrate.models.LIBORMarketModelFromCovarianceModel#getNumeraire(double) The calculation of the drift is consistent with the calculation of the numeraire in <code>getNumeraire</code>.
	 * @see net.finmath.montecarlo.interestrate.models.LIBORMarketModelFromCovarianceModel#getFactorLoading(int, int, RandomVariable[]) The factor loading \( \lambda_{j,k} \).
	 *
	 * @param timeIndex Time index <i>i</i> for which the drift should be returned <i>&mu;(t<sub>i</sub>)</i>.
	 * @param realizationAtTimeIndex Time current forward rate vector at time index <i>i</i> which should be used in the calculation.
	 * @return The drift vector &mu;(t<sub>i</sub>) as <code>RandomVariableFromDoubleArray[]</code>
	 */
	@Override
	public RandomVariable[] getDrift(int timeIndex, RandomVariable[] realizationAtTimeIndex, RandomVariable[] realizationPredictor) {
		double	time				= getTime(timeIndex);
		int		firstLiborIndex		= this.getLiborPeriodIndex(time)+1;
		if(firstLiborIndex<0) {
			firstLiborIndex = -firstLiborIndex-1 + 1;
		}

		RandomVariable		zero	= getRandomVariableForConstant(0.0);

		// Allocate drift vector and initialize to zero (will be used to sum up drift components)
		RandomVariable[]	drift = new RandomVariable[getNumberOfComponents()];
		for(int componentIndex=firstLiborIndex; componentIndex<getNumberOfComponents(); componentIndex++) {
			drift[componentIndex] = zero;
		}

		RandomVariable[]	covarianceFactorSums	= new RandomVariable[getNumberOfFactors()];
		for(int factorIndex=0; factorIndex<getNumberOfFactors(); factorIndex++) {
			covarianceFactorSums[factorIndex] = zero;
		}

		if(measure == Measure.SPOT) {
			// Calculate drift for the component componentIndex (starting at firstLiborIndex, others are zero)
			for(int componentIndex=firstLiborIndex; componentIndex<getNumberOfComponents(); componentIndex++) {
				double						periodLength	= liborPeriodDiscretization.getTimeStep(componentIndex);
				RandomVariable		libor			= realizationAtTimeIndex[componentIndex];
				RandomVariable		oneStepMeasureTransform = getRandomVariableForConstant(periodLength).discount(libor, periodLength);

				if(stateSpace == StateSpace.LOGNORMAL) {
					// The drift has an additional forward rate factor
					oneStepMeasureTransform = oneStepMeasureTransform.mult(libor);
				}

				RandomVariable[]	factorLoading   	= getFactorLoading(timeIndex, componentIndex, realizationAtTimeIndex);
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
				RandomVariable libor			= realizationAtTimeIndex[componentIndex];
				RandomVariable oneStepMeasureTransform = getRandomVariableForConstant(periodLength).discount(libor, periodLength);

				if(stateSpace == StateSpace.LOGNORMAL) {
					oneStepMeasureTransform = oneStepMeasureTransform.mult(libor);
				}

				RandomVariable[]	factorLoading   	= getFactorLoading(timeIndex, componentIndex, realizationAtTimeIndex);
				for(int factorIndex=0; factorIndex<getNumberOfFactors(); factorIndex++) {
					drift[componentIndex] = drift[componentIndex].addProduct(covarianceFactorSums[factorIndex], factorLoading[factorIndex]);
					covarianceFactorSums[factorIndex] = covarianceFactorSums[factorIndex].sub(oneStepMeasureTransform.mult(factorLoading[factorIndex]));
				}
			}
		}

		if(stateSpace == StateSpace.LOGNORMAL) {
			// Drift adjustment for log-coordinate in each component
			for(int componentIndex=firstLiborIndex; componentIndex<getNumberOfComponents(); componentIndex++) {
				RandomVariable		variance		= covarianceModel.getCovariance(getTime(timeIndex), componentIndex, componentIndex, realizationAtTimeIndex);
				drift[componentIndex] = drift[componentIndex].addProduct(variance, -0.5);
			}
		}

		return drift;
	}

	@Override
	public	RandomVariable[]	getFactorLoading(int timeIndex, int componentIndex, RandomVariable[] realizationAtTimeIndex)
	{
		return covarianceModel.getFactorLoading(getTime(timeIndex), getLiborPeriod(componentIndex), realizationAtTimeIndex);
	}

	@Override
	public RandomVariable applyStateSpaceTransform(int componentIndex, RandomVariable randomVariable) {
		RandomVariable value = randomVariable;

		if(stateSpace == StateSpace.LOGNORMAL) {
			value = value.exp();
		}

		if(!Double.isInfinite(liborCap)) {
			value = value.cap(liborCap);
		}

		return value;
	}

	@Override
	public RandomVariable applyStateSpaceTransformInverse(int componentIndex, RandomVariable randomVariable) {
		RandomVariable value = randomVariable;

		if(stateSpace == StateSpace.LOGNORMAL) {
			value = value.log();
		}

		return value;
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.model.ProcessModel#getRandomVariableForConstant(double)
	 */
	@Override
	public RandomVariable getRandomVariableForConstant(double value) {
		return randomVariableFactory.createRandomVariable(value);
	}

	/**
	 * @return Returns the driftApproximationMethod.
	 */
	public Driftapproximation getDriftApproximationMethod() {
		return driftApproximationMethod;
	}

	@Override
	public RandomVariable getLIBOR(double time, double periodStart, double periodEnd) throws CalculationException
	{
		int periodStartIndex    = getLiborPeriodIndex(periodStart);
		int periodEndIndex      = getLiborPeriodIndex(periodEnd);

		// If time is beyond fixing, use the fixing time.
		time = Math.min(time, periodStart);
		int timeIndex           = getTimeIndex(time);
		// If time is not part of the discretization, use the nearest available point.
		if(timeIndex < 0) {
			timeIndex = -timeIndex-2;
			if(time-getTime(timeIndex) > getTime(timeIndex+1)-time) {
				timeIndex++;
			}
		}

		// The forward rates are provided on fractional tenor discretization points using linear interpolation. See ISBN 0470047224.

		// Interpolation on tenor using interpolationMethod
		if(periodEndIndex < 0) {
			int		previousEndIndex	= (-periodEndIndex-1)-1;
			double	nextEndTime			= getLiborPeriod(previousEndIndex+1);
			// Interpolate libor from periodStart to periodEnd on periodEnd
			RandomVariable onePlusLongLIBORdt         = getLIBOR(time, periodStart, nextEndTime).mult(nextEndTime - periodStart).add(1.0);
			RandomVariable onePlusInterpolatedLIBORDt = getOnePlusInterpolatedLIBORDt(timeIndex, periodEnd, previousEndIndex);
			return onePlusLongLIBORdt.div(onePlusInterpolatedLIBORDt).sub(1.0).div(periodEnd - periodStart);
		}

		// Interpolation on tenor using interpolationMethod
		if(periodStartIndex < 0) {
			int	previousStartIndex   = (-periodStartIndex-1)-1;
			double prevStartTime	 = getLiborPeriod(previousStartIndex);
			double nextStartTime	 = getLiborPeriod(previousStartIndex+1);
			if(nextStartTime > periodEnd) {
				throw new AssertionError("Interpolation not possible.");
			}
			if(nextStartTime == periodEnd) {
				return getOnePlusInterpolatedLIBORDt(timeIndex, periodStart, previousStartIndex).sub(1.0).div(periodEnd - periodStart);
			}
			//			RandomVariable onePlusLongLIBORdt         = getLIBOR(Math.min(prevStartTime, time), nextStartTime, periodEnd).mult(periodEnd - nextStartTime).add(1.0);
			RandomVariable onePlusLongLIBORdt         = getLIBOR(time, nextStartTime, periodEnd).mult(periodEnd - nextStartTime).add(1.0);
			RandomVariable onePlusInterpolatedLIBORDt = getOnePlusInterpolatedLIBORDt(timeIndex, periodStart, previousStartIndex);
			return onePlusLongLIBORdt.mult(onePlusInterpolatedLIBORDt).sub(1.0).div(periodEnd - periodStart);
		}

		if(periodStartIndex < 0 || periodEndIndex < 0) {
			throw new AssertionError("LIBOR requested outside libor discretization points and interpolation was not performed.");
		}

		// If this is a model primitive then return it
		if(periodStartIndex+1==periodEndIndex) {
			return getLIBOR(timeIndex, periodStartIndex);
		}

		// The requested LIBOR is not a model primitive. We need to calculate it (slow!)
		RandomVariable accrualAccount = randomVariableFactory.createRandomVariable(1.0);

		// Calculate the value of the forward bond
		for(int periodIndex = periodStartIndex; periodIndex<periodEndIndex; periodIndex++)
		{
			double subPeriodLength = getLiborPeriod(periodIndex+1) - getLiborPeriod(periodIndex);
			RandomVariable liborOverSubPeriod = getLIBOR(timeIndex, periodIndex);

			accrualAccount = accrualAccount == null ? liborOverSubPeriod.mult(subPeriodLength).add(1.0) : accrualAccount.accrue(liborOverSubPeriod, subPeriodLength);
		}

		RandomVariable libor = accrualAccount.sub(1.0).div(periodEnd - periodStart);

		return libor;
	}

	@Override
	public RandomVariable getLIBOR(int timeIndex, int liborIndex) throws CalculationException
	{
		// This method is just a synonym - call getProcessValue of super class
		return getProcessValue(timeIndex, liborIndex);
	}

	/**
	 * Implement the interpolation of the forward rate in tenor time.
	 * The method provides the forward rate \( F(t_{i}, S, T_{j+1}) \) where \( S \in [T_{j}, T_{j+1}] \).
	 *
	 * @param timeIndex The time index associated with the simulation time. The index i in \( t_{i} \).
	 * @param periodStartTime The period start time S (on which we interpolate).
	 * @param liborPeriodIndex The period index j for which \( S \in [T_{j}, T_{j+1}] \) (to avoid another lookup).
	 * @return The interpolated forward rate.
	 * @throws CalculationException Thrown if valuation failed.
	 */
	private RandomVariable getOnePlusInterpolatedLIBORDt(int timeIndex, double periodStartTime, int liborPeriodIndex) throws CalculationException
	{
		double tenorPeriodStartTime       = getLiborPeriod(liborPeriodIndex);
		double tenorPeriodEndTime         = getLiborPeriod(liborPeriodIndex + 1);
		double tenorDt                    = tenorPeriodEndTime - tenorPeriodStartTime;
		if(tenorPeriodStartTime < getTime(timeIndex)) {
			//Fixed at Long LIBOR period Start.
			timeIndex  = Math.min(timeIndex, getTimeIndex(tenorPeriodStartTime));
		}
		RandomVariable onePlusLongLIBORDt = getLIBOR(timeIndex , liborPeriodIndex).mult(tenorDt).add(1.0);

		double smallDt                    = tenorPeriodEndTime - periodStartTime;
		double alpha                      = smallDt / tenorDt;

		RandomVariable onePlusInterpolatedLIBORDt;
		switch(interpolationMethod)
		{
		case LINEAR:
			onePlusInterpolatedLIBORDt = onePlusLongLIBORDt.mult(alpha).add(1 - alpha);
			break;
		case LOG_LINEAR_UNCORRECTED:
			onePlusInterpolatedLIBORDt = onePlusLongLIBORDt.log().mult(alpha).exp();
			break;
		case LOG_LINEAR_CORRECTED:
			double adjustmentCoefficient     = 0.5 * smallDt * (tenorPeriodStartTime - periodStartTime);
			RandomVariable adjustment        = getInterpolationDriftAdjustment(timeIndex, liborPeriodIndex);
			adjustment = adjustment.mult(adjustmentCoefficient);
			onePlusInterpolatedLIBORDt = onePlusLongLIBORDt.log().mult(alpha).sub(adjustment).exp();
			break;
		default: throw new IllegalArgumentException("Method for enum " + interpolationMethod.name() + " not implemented!");
		}

		{
			// Analytic adjustment for the interpolation
			// @TODO reference to AnalyticModelFromCuvesAndVols must not be null
			// @TODO This adjustment only applies if the corresponding adjustment in getNumeraire is enabled
			double analyticOnePlusLongLIBORDt   = 1 + getForwardRateCurve().getForward(getAnalyticModel(), tenorPeriodStartTime, tenorDt)
					* tenorDt;
			double analyticOnePlusShortLIBORDt	= 1 + getForwardRateCurve().getForward(getAnalyticModel(), periodStartTime, smallDt)
					* smallDt;
			double analyticOnePlusInterpolatedLIBORDt;
			switch(interpolationMethod)
			{
			case LINEAR:
				analyticOnePlusInterpolatedLIBORDt = analyticOnePlusLongLIBORDt * alpha + (1-alpha);
				break;
			case LOG_LINEAR_UNCORRECTED:
			case LOG_LINEAR_CORRECTED:
				analyticOnePlusInterpolatedLIBORDt = Math.exp(Math.log(analyticOnePlusLongLIBORDt) * alpha);
				break;
			default: throw new IllegalArgumentException("Method for enum " + interpolationMethod.name() + " not implemented!");
			}
			onePlusInterpolatedLIBORDt = onePlusInterpolatedLIBORDt.mult(analyticOnePlusShortLIBORDt / analyticOnePlusInterpolatedLIBORDt);
		}

		return onePlusInterpolatedLIBORDt;
	}

	/**
	 *
	 * @param evaluationTimeIndex
	 * @param liborIndex
	 * @return
	 * @throws CalculationException
	 */
	private RandomVariable getInterpolationDriftAdjustment(int evaluationTimeIndex, int liborIndex) throws CalculationException
	{
		switch(interpolationMethod)
		{
		case LINEAR:
		case LOG_LINEAR_UNCORRECTED:
			return null;

		case LOG_LINEAR_CORRECTED:
			double tenorPeriodStartTime  = getLiborPeriod(liborIndex);
			int    tenorPeriodStartIndex = getTimeIndex(tenorPeriodStartTime);
			if(tenorPeriodStartIndex < 0)
			{
				tenorPeriodStartIndex = - tenorPeriodStartIndex - 2;
			}
			// Lazy init of interpolationDriftAdjustmentsTerminal
			if(evaluationTimeIndex == tenorPeriodStartIndex) {
				synchronized(interpolationDriftAdjustmentsTerminal) {
					// Invalidate cache if process has changed
					ensureCacheConsistency();

					// Check if value is cached
					if(interpolationDriftAdjustmentsTerminal.size() <= liborIndex) {
						interpolationDriftAdjustmentsTerminal.setSize(getNumberOfLibors());
					}

					RandomVariable interpolationDriftAdjustment = interpolationDriftAdjustmentsTerminal.get(liborIndex);
					if(interpolationDriftAdjustment == null) {
						;
					}
					{
						interpolationDriftAdjustment = getInterpolationDriftAdjustmentEvaluated(evaluationTimeIndex, liborIndex);
						interpolationDriftAdjustmentsTerminal.set(liborIndex, interpolationDriftAdjustment);
					}

					return interpolationDriftAdjustment;
				}
			}
			else {
				return getInterpolationDriftAdjustmentEvaluated(evaluationTimeIndex, liborIndex);
			}
		default: throw new IllegalArgumentException("Method for enum " + interpolationMethod.name() + " not implemented!");
		}
	}

	private RandomVariable getInterpolationDriftAdjustmentEvaluated(int evaluationTimeIndex, int liborIndex) throws CalculationException
	{

		double tenorPeriodStartTime  = getLiborPeriod(liborIndex);
		double tenorPeriodEndTime    = getLiborPeriod(liborIndex + 1);
		double tenorDt               = tenorPeriodEndTime - tenorPeriodStartTime;

		RandomVariable driftAdjustment = getRandomVariableForConstant(0.0);

		//integral approximation with trapezoid method.
		RandomVariable previousIntegrand = getRandomVariableForConstant(0.0);
		{
			RandomVariable[] realizationsAtTimeIndex = new RandomVariable[getNumberOfLibors()];
			for(int liborIndexForRealization = 0; liborIndexForRealization < getNumberOfLibors(); liborIndexForRealization++)
			{
				realizationsAtTimeIndex[liborIndexForRealization] = getLIBOR(0, liborIndexForRealization);
			}
			RandomVariable[] factorLoading = getFactorLoading(0, liborIndex, realizationsAtTimeIndex);
			//o_{Li}(t)
			for(RandomVariable oneFactor : factorLoading)
			{
				previousIntegrand = previousIntegrand.add(oneFactor.squared());
			}
			previousIntegrand = previousIntegrand.div( (realizationsAtTimeIndex[liborIndex].mult(tenorDt).add(1.0)).squared() );
			if(stateSpace == StateSpace.LOGNORMAL)
			{
				previousIntegrand = previousIntegrand.mult( realizationsAtTimeIndex[liborIndex].squared() );
			}
		}

		for(int sumTimeIndex = 1; sumTimeIndex <= evaluationTimeIndex; sumTimeIndex++)
		{
			RandomVariable[] realizationsAtTimeIndex = new RandomVariable[getNumberOfLibors()];
			for(int liborIndexForRealization = 0; liborIndexForRealization < getNumberOfLibors(); liborIndexForRealization++)
			{
				int evaluationTimeIndexForRealizations = Math.min(sumTimeIndex, getTimeIndex(getLiborPeriod(liborIndexForRealization)));
				if(evaluationTimeIndexForRealizations < 0)
				{
					evaluationTimeIndexForRealizations = - evaluationTimeIndexForRealizations - 2;
				}
				realizationsAtTimeIndex[liborIndexForRealization] = getLIBOR(evaluationTimeIndexForRealizations, liborIndexForRealization);
			}
			RandomVariable[] factorLoading = getFactorLoading(sumTimeIndex, liborIndex, realizationsAtTimeIndex);
			//o_{Li}(t)
			RandomVariable   integrand = getRandomVariableForConstant(0.0);
			for ( RandomVariable oneFactor: factorLoading)
			{
				integrand = integrand.add(oneFactor.squared());
			}
			integrand = integrand.div( (realizationsAtTimeIndex[liborIndex].mult(tenorDt).add(1.0)).squared() );
			if(stateSpace == StateSpace.LOGNORMAL)
			{
				integrand = integrand.mult( realizationsAtTimeIndex[liborIndex].squared() );
			}
			double integralDt = 0.5 * (getTime(sumTimeIndex) - getTime(sumTimeIndex - 1));
			driftAdjustment = driftAdjustment.add( (integrand.add(previousIntegrand)).mult(integralDt) );
			previousIntegrand = integrand;
		}

		return driftAdjustment;

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

	@Override
	public double getLiborPeriod(int timeIndex) {
		if(timeIndex >= liborPeriodDiscretization.getNumberOfTimes() || timeIndex < 0) {
			throw new ArrayIndexOutOfBoundsException("Index for LIBOR period discretization out of bounds: " + timeIndex + ".");
		}
		return liborPeriodDiscretization.getTime(timeIndex);
	}

	@Override
	public int getLiborPeriodIndex(double time) {
		return liborPeriodDiscretization.getTimeIndex(time);
	}

	@Override
	public TimeDiscretization getLiborPeriodDiscretization() {
		return liborPeriodDiscretization;
	}

	/**
	 * @return Returns the LIBOR rates interpolation method. See {@link InterpolationMethod}.
	 */
	public InterpolationMethod getInterpolationMethod() {
		return interpolationMethod;
	}

	/**
	 * @return Returns the measure. See {@link Measure}.
	 */
	public Measure getMeasure() {
		return measure;
	}

	@Override
	public double[][][] getIntegratedLIBORCovariance() {
		synchronized (integratedLIBORCovarianceLazyInitLock) {
			if(integratedLIBORCovariance == null) {
				TimeDiscretization liborPeriodDiscretization = getLiborPeriodDiscretization();
				TimeDiscretization simulationTimeDiscretization = getTimeDiscretization();

				integratedLIBORCovariance = new double[simulationTimeDiscretization.getNumberOfTimeSteps()][liborPeriodDiscretization.getNumberOfTimeSteps()][liborPeriodDiscretization.getNumberOfTimeSteps()];
				for(int timeIndex = 0; timeIndex < simulationTimeDiscretization.getNumberOfTimeSteps(); timeIndex++) {
					double dt = simulationTimeDiscretization.getTime(timeIndex+1) - simulationTimeDiscretization.getTime(timeIndex);
					RandomVariable[][] factorLoadings = new RandomVariable[liborPeriodDiscretization.getNumberOfTimeSteps()][];
					// Prefetch factor loadings
					for(int componentIndex = 0; componentIndex < liborPeriodDiscretization.getNumberOfTimeSteps(); componentIndex++) {
						factorLoadings[componentIndex] = getFactorLoading(timeIndex, componentIndex, null);
					}
					for(int componentIndex1 = 0; componentIndex1 < liborPeriodDiscretization.getNumberOfTimeSteps(); componentIndex1++) {
						RandomVariable[] factorLoadingOfComponent1 = factorLoadings[componentIndex1];
						// Sum the libor cross terms (use symmetry)
						for(int componentIndex2 = componentIndex1; componentIndex2 < liborPeriodDiscretization.getNumberOfTimeSteps(); componentIndex2++) {
							double integratedLIBORCovarianceValue = 0.0;
							if(getLiborPeriod(componentIndex1) > getTime(timeIndex)) {
								RandomVariable[] factorLoadingOfComponent2 = factorLoadings[componentIndex2];
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
			Map<String, Object> properties = new HashMap<>();
			properties.put("measure",		measure.name());
			properties.put("stateSpace",	stateSpace.name());
			return new LIBORMarketModelFromCovarianceModel(getLiborPeriodDiscretization(), getAnalyticModel(), getForwardRateCurve(), getDiscountCurve(), randomVariableFactory, covarianceModel, new CalibrationProduct[0], properties);
		} catch (CalculationException e) {
			return null;
		}
	}

	@Override
	public AnalyticModel getAnalyticModel() {
		return curveModel;
	}

	@Override
	public DiscountCurve getDiscountCurve() {
		return discountCurve;
	}

	@Override
	public ForwardCurve getForwardRateCurve() {
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

	@Override
	public LIBORCovarianceModel getCovarianceModel() {
		return covarianceModel;
	}

	/**
	 * @param covarianceModel A covariance model
	 * @return A new <code>LIBORMarketModelFromCovarianceModel</code> using the specified covariance model.
	 */
	@Override
	public LIBORMarketModelFromCovarianceModel getCloneWithModifiedCovarianceModel(LIBORCovarianceModel covarianceModel) {
		LIBORMarketModelFromCovarianceModel model = (LIBORMarketModelFromCovarianceModel)this.clone();
		model.covarianceModel = covarianceModel;
		return model;
	}

	@Override
	public LIBORMarketModelFromCovarianceModel getCloneWithModifiedData(Map<String, Object> dataModified) throws CalculationException {
		AbstractRandomVariableFactory 	randomVariableFactory	= this.randomVariableFactory;
		TimeDiscretization		liborPeriodDiscretization	= this.liborPeriodDiscretization;
		AnalyticModel			analyticModel				= curveModel;
		ForwardCurve			forwardRateCurve			= this.forwardRateCurve;
		DiscountCurve			discountCurve				= this.discountCurve;
		LIBORCovarianceModel	covarianceModel				= this.covarianceModel;
		Map<String, Object>				properties					= new HashMap<>();
		properties.put("measure",		measure.name());
		properties.put("stateSpace",	stateSpace.name());

		if(dataModified != null) {
			randomVariableFactory = (AbstractRandomVariableFactory)dataModified.getOrDefault("randomVariableFactory", randomVariableFactory);
			liborPeriodDiscretization = (TimeDiscretization)dataModified.getOrDefault("liborPeriodDiscretization", liborPeriodDiscretization);
			analyticModel = (AnalyticModel)dataModified.getOrDefault("analyticModel", analyticModel);
			forwardRateCurve = (ForwardCurve)dataModified.getOrDefault("forwardRateCurve", forwardRateCurve);
			discountCurve = (DiscountCurve)dataModified.getOrDefault("discountCurve", discountCurve);
			covarianceModel = (LIBORCovarianceModel)dataModified.getOrDefault("covarianceModel", covarianceModel);
			swaptionMarketData = (AbstractSwaptionMarketData)dataModified.getOrDefault("swaptionMarketData", swaptionMarketData);

			if(dataModified.containsKey("swaptionMarketData")) {
				throw new RuntimeException("Swaption market data as input for getCloneWithModifiedData not supported.");
			}

			if(dataModified.containsKey("forwardRateShift")) {
				throw new RuntimeException("Forward rate shift clone currently disabled.");
			}
			if(dataModified != null && dataModified.containsKey("swaptionMarketData")) {
				swaptionMarketData = (AbstractSwaptionMarketData)dataModified.get("swaptionMarketData");
			}

		}

		LIBORMarketModelFromCovarianceModel newModel = LIBORMarketModelFromCovarianceModel.of(liborPeriodDiscretization, analyticModel, forwardRateCurve, discountCurve, randomVariableFactory, covarianceModel, null, properties);
		return newModel;
	}

	@Override
	public Map<String, RandomVariable> getModelParameters() {
		Map<String, RandomVariable> modelParameters = new TreeMap<>();

		// Add initial values
		for(int liborIndex=0; liborIndex<getLiborPeriodDiscretization().getNumberOfTimeSteps(); liborIndex++) {
			RandomVariable forward = null;
			try {
				forward = getLIBOR(0, liborIndex);
			}
			catch (CalculationException e) {}

			modelParameters.put("FORWARD("+getLiborPeriod(liborIndex) + "," + getLiborPeriod(liborIndex+1) + ")", forward);
		}

		// Add volatilities
		if(covarianceModel instanceof AbstractLIBORCovarianceModelParametric) {
			RandomVariable[] covarianceModelParameters = ((AbstractLIBORCovarianceModelParametric) covarianceModel).getParameter();

			for(int covarianceModelParameterIndex=0; covarianceModelParameterIndex<covarianceModelParameters.length; covarianceModelParameterIndex++) {
				modelParameters.put("COVARIANCEMODELPARAMETER("+ covarianceModelParameterIndex + ")", covarianceModelParameters[covarianceModelParameterIndex]);
			}
		}

		// Add numeraire adjustments
		// TODO: Trigger lazy init
		for(Entry<Double, RandomVariable> numeraireAdjustment : numeraireDiscountFactorForwardRates.entrySet()) {
			modelParameters.put("NUMERAIREADJUSTMENT("+ numeraireAdjustment.getKey() + ")", numeraireAdjustment.getValue());
		}

		return modelParameters;
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();

		/*
		 * Init transient fields
		 */
		integratedLIBORCovarianceLazyInitLock = new Object();
		numeraires = new ConcurrentHashMap<>();
		numeraireDiscountFactorForwardRates = new ConcurrentHashMap<>();
		numeraireDiscountFactors = new ConcurrentHashMap<>();
		interpolationDriftAdjustmentsTerminal = new Vector<RandomVariable>();
	}

	@Override
	public String toString() {
		return "LIBORMarketModelFromCovarianceModel [liborPeriodDiscretization="
				+ liborPeriodDiscretization + ", forwardCurveName="
				+ forwardCurveName + ", curveModel=" + curveModel
				+ ", forwardRateCurve=" + forwardRateCurve + ", discountCurve="
				+ discountCurve + ", covarianceModel=" + covarianceModel
				+ ", driftApproximationMethod=" + driftApproximationMethod
				+ ", measure=" + measure + ", stateSpace=" + stateSpace + "]";
	}
}

