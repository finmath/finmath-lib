/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2004
 */
package net.finmath.montecarlo.interestrate.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFromArrayFactory;
import net.finmath.montecarlo.interestrate.CalibrationProduct;
import net.finmath.montecarlo.interestrate.TermStructureModel;
import net.finmath.montecarlo.interestrate.models.covariance.TermStructureCovarianceModel;
import net.finmath.montecarlo.interestrate.models.covariance.TermStructureCovarianceModelParametric;
import net.finmath.montecarlo.model.AbstractProcessModel;
import net.finmath.montecarlo.process.MonteCarloProcess;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * Implements a discretized Heath-Jarrow-Morton model / LIBOR market model with dynamic tenor refinement, see
 * <a href="https://papers.ssrn.com/sol3/papers.cfm?abstract_id=2884699">https://papers.ssrn.com/sol3/papers.cfm?abstract_id=2884699</a>.
 *
 * <br><br>
 * In its default case the class specifies a multi-factor LIBOR market model, that is
 * \( L_{j} = \frac{1}{T_{j+1}-T_{j}} ( exp(Y_{j}) - 1 ) \), where
 * \[
 * 		dY_{j} = \mu_{j} dt + \lambda_{1,j} dW_{1} + \ldots + \lambda_{m,j} dW_{m}
 * \]
 * <br>
 * The model uses an <code>AbstractLIBORCovarianceModel</code> for the specification of
 * <i>(&lambda;<sub>1,j</sub>,...,&lambda;<sub>m,j</sub>)</i> as a covariance model.
 * See {@link net.finmath.montecarlo.model.ProcessModel} for details on the implemented interface
 * <br><br>
 * The model uses an <code>AbstractLIBORCovarianceModel</code> as a covariance model.
 * If the covariance model is of type <code>AbstractLIBORCovarianceModelParametric</code>
 * a calibration to swaptions can be performed.
 * <br>
 * Note that &lambda; may still depend on <i>L</i> (through a local volatility model).
 * <br>
 * The simulation is performed under spot measure, that is, the numeraire
 * 	is \( N(T_{i}) = \prod_{j=0}^{i-1} (1 + L(T_{j},T_{j+1};T_{j}) (T_{j+1}-T_{j})) \).
 *
 * The map <code>properties</code> allows to configure the model. The following keys may be used:
 * <ul>
 * 		<li>
 * 			<code>liborCap</code>: An optional <code>Double</code> value applied as a cap to the LIBOR rates.
 * 			May be used to limit the simulated valued to prevent values attaining POSITIVE_INFINITY and
 * 			numerical problems. To disable the cap, set <code>liborCap</code> to <code>Double.POSITIVE_INFINITY</code>.
 *		</li>
 * </ul>
 * <br>
 *
 * The main task of this class is to calculate the risk-neutral drift and the
 * corresponding numeraire given the covariance model.
 *
 * The calibration of the covariance structure is not part of this class.
 *
 * @author Christian Fries
 * @version 1.2
 * @see net.finmath.montecarlo.process.MonteCarloProcess The interface for numerical schemes.
 * @see net.finmath.montecarlo.model.ProcessModel The interface for models provinding parameters to numerical schemes.
 * @see <a href="https://papers.ssrn.com/sol3/papers.cfm?abstract_id=2884699">https://papers.ssrn.com/sol3/papers.cfm?abstract_id=2884699</a>
 */
public class LIBORMarketModelWithTenorRefinement extends AbstractProcessModel implements TermStructureModel {

	public enum Driftapproximation	{ EULER, LINE_INTEGRAL, PREDICTOR_CORRECTOR }

	private final TimeDiscretization[]		liborPeriodDiscretizations;
	private final Integer[]							numberOfDiscretizationIntervals;

	private String							forwardCurveName;
	private final AnalyticModel			curveModel;

	private final ForwardCurve			forwardRateCurve;
	private final DiscountCurve			discountCurve;

	private final RandomVariableFactory	randomVariableFactory = new RandomVariableFromArrayFactory();

	private TermStructureCovarianceModel	covarianceModel;

	// Cache for the numeraires, needs to be invalidated if process changes
	private final ConcurrentHashMap<Integer, RandomVariable>	numeraires;
	private MonteCarloProcess									numerairesProcess = null;

	/**
	 * Creates a model for given covariance.
	 *
	 * Creates a discretized Heath-Jarrow-Morton model / LIBOR market model with dynamic tenor refinement, see
	 * <a href="https://papers.ssrn.com/sol3/papers.cfm?abstract_id=2884699">https://papers.ssrn.com/sol3/papers.cfm?abstract_id=2884699</a>.
	 * <br>
	 * If calibrationItems in non-empty and the covariance model is a parametric model,
	 * the covariance will be replaced by a calibrate version of the same model, i.e.,
	 * the LIBOR Market Model will be calibrated.
	 * <br>
	 * The map <code>properties</code> allows to configure the model. The following keys may be used:
	 * <ul>
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
	 * @param liborPeriodDiscretizations A vector of tenor discretizations of the interest rate curve into forward rates (tenor structure), finest first.
	 * @param numberOfDiscretizationIntervals A vector of number of periods to be taken from the liborPeriodDiscretizations.
	 * @param analyticModel The associated analytic model of this model (containing the associated market data objects like curve).
	 * @param forwardRateCurve The initial values for the forward rates.
	 * @param discountCurve The discount curve to use. This will create an LMM model with a deterministic zero-spread discounting adjustment.
	 * @param covarianceModel The covariance model to use.
	 * @param calibrationProducts The vector of calibration items (a union of a product, target value and weight) for the objective function sum weight(i) * (modelValue(i)-targetValue(i).
	 * @param properties Key value map specifying properties like <code>measure</code> and <code>stateSpace</code>.
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	public LIBORMarketModelWithTenorRefinement(
			final TimeDiscretization[]		liborPeriodDiscretizations,
			final Integer[]							numberOfDiscretizationIntervals,
			final AnalyticModel				analyticModel,
			final ForwardCurve				forwardRateCurve,
			final DiscountCurve				discountCurve,
			final TermStructureCovarianceModel	covarianceModel,
			final CalibrationProduct[]					calibrationProducts,
			final Map<String, ?>						properties
			) throws CalculationException {

		Map<String,Object> calibrationParameters = null;
		if(properties != null && properties.containsKey("calibrationParameters")) {
			calibrationParameters	= (Map<String,Object>)properties.get("calibrationParameters");
		}

		this.liborPeriodDiscretizations	= liborPeriodDiscretizations;
		this.numberOfDiscretizationIntervals = numberOfDiscretizationIntervals;
		curveModel					= analyticModel;
		this.forwardRateCurve	= forwardRateCurve;
		this.discountCurve		= discountCurve;
		this.covarianceModel	= covarianceModel;

		// Perform calibration, if data is given
		if(calibrationProducts != null && calibrationProducts.length > 0) {
			TermStructureCovarianceModelParametric covarianceModelParametric = null;
			try {
				covarianceModelParametric = (TermStructureCovarianceModelParametric)covarianceModel;
			}
			catch(final Exception e) {
				throw new ClassCastException("Calibration is currently restricted to parametric covariance models (TermStructureCovarianceModelParametricInterface).");
			}

			this.covarianceModel    = covarianceModelParametric.getCloneCalibrated(this, calibrationProducts, calibrationParameters);
		}

		numeraires = new ConcurrentHashMap<>();
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
	public RandomVariable getNumeraire(final MonteCarloProcess process, final double time) throws CalculationException {
		final int timeIndex = liborPeriodDiscretizations[0].getTimeIndex(time);
		final TimeDiscretization liborPeriodDiscretization = liborPeriodDiscretizations[0];
		if(timeIndex < 0) {
			// Interpolation of Numeraire: log linear interpolation.
			final int upperIndex = -timeIndex-1;
			final int lowerIndex = upperIndex-1;
			if(lowerIndex < 0) {
				throw new IllegalArgumentException("Numeraire requested for time " + time + ". Unsupported");
			}

			final double alpha = (time-liborPeriodDiscretization.getTime(lowerIndex)) / (liborPeriodDiscretization.getTime(upperIndex) - liborPeriodDiscretization.getTime(lowerIndex));
			RandomVariable numeraire = getNumeraire(process, liborPeriodDiscretization.getTime(upperIndex)).log().mult(alpha).add(getNumeraire(process, liborPeriodDiscretization.getTime(lowerIndex)).log().mult(1.0-alpha)).exp();

			/*
			 * Adjust for discounting, i.e. funding or collateralization
			 */
			if(discountCurve != null) {
				// This includes a control for zero bonds
				final double deterministicNumeraireAdjustment = numeraire.invert().getAverage() / discountCurve.getDiscountFactor(curveModel, time);
				numeraire = numeraire.mult(deterministicNumeraireAdjustment);
			}

			return numeraire;
		}

		/*
		 * Calculate the numeraire, when time is part of liborPeriodDiscretization
		 */

		/*
		 * Check if numeraire cache is values (i.e. process did not change)
		 */
		if(process != numerairesProcess) {
			numeraires.clear();
			numerairesProcess = process;
		}

		/*
		 * Check if numeraire is part of the cache
		 */
		RandomVariable numeraire = numeraires.get(timeIndex);
		if(numeraire == null) {
			/*
			 * Calculate the numeraire for timeIndex
			 */
			if(timeIndex == 0) {
				numeraire = process.getStochasticDriver().getRandomVariableForConstant(1.0);
			}
			else {
				// Initialize to previous numeraire
				numeraire = getNumeraire(process, liborPeriodDiscretizations[0].getTime(timeIndex-1));

				final double periodStart	= liborPeriodDiscretizations[0].getTime(timeIndex-1);
				final double periodEnd	= liborPeriodDiscretizations[0].getTime(timeIndex);
				final RandomVariable libor = getForwardRate(process, periodStart, periodStart, periodEnd);

				numeraire = numeraire.accrue(libor, periodEnd-periodStart);
			}

			// Cache the numeraire
			numeraires.put(timeIndex, numeraire);
		}

		/*
		 * Adjust for discounting, i.e. funding or collateralization
		 */
		if(discountCurve != null) {
			// This includes a control for zero bonds
			final double deterministicNumeraireAdjustment = numeraire.invert().getAverage() / discountCurve.getDiscountFactor(curveModel, time);
			numeraire = numeraire.mult(deterministicNumeraireAdjustment);
		}

		return numeraire;
	}

	@Override
	public RandomVariable[] getInitialState(MonteCarloProcess process) {
		final RandomVariable[] initialStateRandomVariable = new RandomVariable[getNumberOfComponents()];
		for(int componentIndex=0; componentIndex<getNumberOfComponents(); componentIndex++) {
			initialStateRandomVariable[componentIndex] = process.getStochasticDriver().getRandomVariableForConstant(0.0);
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
	 * @see net.finmath.montecarlo.interestrate.models.LIBORMarketModelWithTenorRefinement#getNumeraire(MonteCarloProcess, double) The calculation of the drift is consistent with the calculation of the numeraire in <code>getNumeraire</code>.
	 * @see net.finmath.montecarlo.interestrate.models.LIBORMarketModelWithTenorRefinement#getFactorLoading(MonteCarloProcess, int, int, RandomVariable[]) The factor loading \( \lambda_{j,k} \).
	 *
	 * @param process The discretization process generating this model. The process provides call backs for TimeDiscretization and allows calls to getProcessValue for timeIndices less or equal the given one.
	 * @param timeIndex Time index <i>i</i> for which the drift should be returned <i>&mu;(t<sub>i</sub>)</i>.
	 * @param realizationAtTimeIndex Time current forward rate vector at time index <i>i</i> which should be used in the calculation.
	 * @return The drift vector &mu;(t<sub>i</sub>) as <code>RandomVariableFromDoubleArray[]</code>
	 */
	@Override
	public RandomVariable[] getDrift(final MonteCarloProcess process, final int timeIndex, final RandomVariable[] realizationAtTimeIndex, final RandomVariable[] realizationPredictor) {

		final double	time				= process.getTime(timeIndex);
		final double	timeStep			= process.getTimeDiscretization().getTimeStep(timeIndex);
		final double	timeNext			= process.getTime(timeIndex+1);

		final RandomVariable		zero	= process.getStochasticDriver().getRandomVariableForConstant(0.0);

		// Allocate drift vector and initialize to zero (will be used to sum up drift components)
		final RandomVariable[]	drift = new RandomVariable[getNumberOfComponents()];
		for(int componentIndex=0; componentIndex<getNumberOfComponents(); componentIndex++) {
			drift[componentIndex] = null;
		}

		final RandomVariable[]	variances	= new RandomVariable[getNumberOfComponents()];
		for(int componentIndex=0; componentIndex<getNumberOfComponents(); componentIndex++) {
			variances[componentIndex] = zero;
		}

		final RandomVariable[]	covarianceFactorSums	= new RandomVariable[getNumberOfFactors()];
		for(int factorIndex=0; factorIndex<covarianceFactorSums.length; factorIndex++) {
			covarianceFactorSums[factorIndex] = zero;
		}

		/*
		 * Standard HJM drift part of log-forward-bond
		 */
		final TimeDiscretization liborPeriodDiscretization = getLiborPeriodDiscretization(timeNext);
		// Calculate drift for the component componentIndex (starting at firstLiborIndex, others are zero)
		for(int componentIndex=0; componentIndex<liborPeriodDiscretization.getNumberOfTimeSteps(); componentIndex++) {
			drift[componentIndex] = zero;

			final double						periodStart		= liborPeriodDiscretization.getTime(componentIndex);
			final double						periodLength	= liborPeriodDiscretization.getTimeStep(componentIndex);
			final double						periodEnd		= periodStart + periodLength;
			final double						tenorTime		= covarianceModel.getScaledTenorTime(periodStart, periodEnd);

			// @TODO Document that factorLoading componentIndexing is on time discretization of t+1 for interval (t,t+1)
			final RandomVariable[]	factorLoading   	= getFactorLoading(process, timeIndex, componentIndex, realizationAtTimeIndex);
			final double weight = getWeightForTenorRefinement(periodStart,periodStart,periodStart,periodEnd);
			for(int factorIndex=0; factorIndex<factorLoading.length; factorIndex++) {
				drift[componentIndex] = drift[componentIndex].addProduct(covarianceFactorSums[factorIndex].addProduct(factorLoading[factorIndex], weight),factorLoading[factorIndex]);
				variances[componentIndex] = variances[componentIndex].addProduct(factorLoading[factorIndex], factorLoading[factorIndex]);
				covarianceFactorSums[factorIndex] = covarianceFactorSums[factorIndex].addProduct(factorLoading[factorIndex],tenorTime);
			}
		}

		/*
		 * Change of tenor discretization - impact on log-forward-bond
		 */
		final TimeDiscretization liborPeriodDiscretizationPrevious = getLiborPeriodDiscretization(time);
		for(int componentIndex=0; componentIndex<liborPeriodDiscretization.getNumberOfTimeSteps(); componentIndex++) {
			final double						periodStart		= liborPeriodDiscretization.getTime(componentIndex);
			final double						periodLength	= liborPeriodDiscretization.getTimeStep(componentIndex);
			final double						periodEnd		= periodStart + periodLength;

			final double						periodStartPrevious		= liborPeriodDiscretizationPrevious.getTime(componentIndex);
			final double						periodLengthPrevious	= liborPeriodDiscretizationPrevious.getTimeStep(componentIndex);
			final double						periodEndPrevious		= periodStartPrevious + periodLengthPrevious;

			if(periodStartPrevious == periodStart && periodEndPrevious == periodEnd) {
				continue;
			}

			final RandomVariable		stateVariablePrevious	= getStateVariable(process, timeIndex, periodStartPrevious,	periodEndPrevious);
			final RandomVariable		stateVariable			= getStateVariable(process, timeIndex, periodStart, 			periodEnd);

			if(Double.isNaN(stateVariable.getAverage()) || Double.isNaN(stateVariablePrevious.getAverage())) {
				throw new IllegalArgumentException("The model parameters resulted in a state variable becoming NaN (not a number).");
			}

			// Shift in indexing and/or tenor refinement
			drift[componentIndex] = drift[componentIndex].add(stateVariable.sub(stateVariablePrevious).div(timeStep));
		}

		/*
		 * Integrated variance - drift part
		 */
		for(int componentIndex=0; componentIndex<liborPeriodDiscretization.getNumberOfTimeSteps(); componentIndex++) {
			drift[getNumberOfLibors()+componentIndex] = variances[componentIndex];
		}

		/*
		 * Change of tenor discretization - impact on integrated variance
		 */
		for(int componentIndex=0; componentIndex<liborPeriodDiscretization.getNumberOfTimeSteps(); componentIndex++) {
			final double						periodStart		= liborPeriodDiscretization.getTime(componentIndex);
			final double						periodLength	= liborPeriodDiscretization.getTimeStep(componentIndex);
			final double						periodEnd		= periodStart + periodLength;

			final double						periodStartPrevious		= liborPeriodDiscretizationPrevious.getTime(componentIndex);
			final double						periodLengthPrevious	= liborPeriodDiscretizationPrevious.getTimeStep(componentIndex);
			final double						periodEndPrevious		= periodStartPrevious + periodLengthPrevious;

			if(periodStartPrevious == periodStart && periodEndPrevious == periodEnd) {
				continue;
			}

			final RandomVariable		stateVariablePrevious	= getIntegratedVariance(process, timeIndex, periodStartPrevious, periodEndPrevious);
			final RandomVariable		stateVariable			= getIntegratedVariance(process, timeIndex, periodStart, 		periodEnd);

			if(Double.isNaN(stateVariable.getAverage()) || Double.isNaN(stateVariablePrevious.getAverage())) {
				throw new IllegalArgumentException("The model parameters resulted in a state variable becoming NaN (not a number).");
			}

			// Shift in indexing
			drift[getNumberOfLibors()+componentIndex] = drift[getNumberOfLibors()+componentIndex].add(stateVariable.sub(stateVariablePrevious).div(timeStep));
		}

		return drift;
	}


	/**
	 * @param timeIndex
	 * @param periodStart
	 * @param periodEnd
	 * @return
	 */
	private RandomVariable getIntegratedVariance(final MonteCarloProcess process, final int timeIndex, final double periodStart, final double periodEnd) {
		final TimeDiscretization liborPeriodTiscretization = getLiborPeriodDiscretization(process.getTime(timeIndex));

		int periodStartIndex = liborPeriodTiscretization.getTimeIndex(periodStart);
		int perirodEndIndex = liborPeriodTiscretization.getTimeIndex(periodEnd);

		if(periodStartIndex < 0) {
			periodStartIndex = -periodStartIndex-1-1;
		}
		if(perirodEndIndex < 0) {
			perirodEndIndex = -perirodEndIndex-1;
		}

		if(perirodEndIndex != periodStartIndex+1) {
			throw new IllegalArgumentException();
		}

		RandomVariable integratedVariance = null;
		try {
			integratedVariance = process.getProcessValue(timeIndex, getNumberOfLibors()+periodStartIndex);
		} catch (final CalculationException e) {
		}

		return integratedVariance;
	}

	/**
	 * @param periodStartPrevious
	 * @param periodEndPrevious
	 * @param periodStart
	 * @param periodEnd
	 * @return
	 */
	private double getWeightForTenorRefinement(final double periodStartPrevious, final double periodEndPrevious, final double periodStart, final double periodEnd) {
		final TimeDiscretization numeriareDiscretization = liborPeriodDiscretizations[0];

		final int periodStartPreviousIndex = numeriareDiscretization.getTimeIndex(periodStartPrevious);
		final int periodEndPreviousIndex = numeriareDiscretization.getTimeIndex(periodEndPrevious);
		int periodStartIndex = numeriareDiscretization.getTimeIndex(periodStart);
		int periodEndIndex = numeriareDiscretization.getTimeIndex(periodEnd);

		/// @TODO Need to improve LIBOR interpolation if required
		if(periodStartIndex < 0) {
			periodStartIndex = -periodStartIndex-1;
		}
		if(periodEndIndex < 0) {
			periodEndIndex = -periodEndIndex-1-1;
		}

		double weight1 = 0.0;
		for(int periodIndex = periodStartPreviousIndex; periodIndex<periodEndPreviousIndex; periodIndex++) {
			final double deltaT = covarianceModel.getScaledTenorTime(numeriareDiscretization.getTime(periodIndex), numeriareDiscretization.getTime(periodIndex+1));
			final double deltaTSum = covarianceModel.getScaledTenorTime(periodStartPrevious, numeriareDiscretization.getTime(periodIndex+1));
			weight1 +=  deltaT * deltaTSum;
		}

		double weight2 = 0.0;
		for(int periodIndex = periodStartIndex; periodIndex<periodEndIndex; periodIndex++) {
			final double deltaT = covarianceModel.getScaledTenorTime(numeriareDiscretization.getTime(periodIndex), numeriareDiscretization.getTime(periodIndex+1));
			final double deltaTSum = covarianceModel.getScaledTenorTime(periodStartPrevious, numeriareDiscretization.getTime(periodIndex+1));
			weight2 +=  deltaT * deltaTSum;
		}

		if(weight1 > 0) {
			return weight2 / covarianceModel.getScaledTenorTime(periodStart, periodEnd) - weight1 / covarianceModel.getScaledTenorTime(periodStartPrevious, periodEndPrevious);
		} else {
			return weight2 / covarianceModel.getScaledTenorTime(periodStart, periodEnd);
		}
	}

	@Override
	public	RandomVariable[]	getFactorLoading(final MonteCarloProcess process, final int timeIndex, final int componentIndex, final RandomVariable[] realizationAtTimeIndex)
	{
		final RandomVariable zero = process.getStochasticDriver().getRandomVariableForConstant(0.0);

		if(componentIndex < getNumberOfLibors()) {
			final TimeDiscretization liborPeriodDiscretization = getLiborPeriodDiscretization(process.getTime(timeIndex));
			final TimeDiscretization liborPeriodDiscretizationNext = getLiborPeriodDiscretization(process.getTime(timeIndex+1));
			final double						periodStart	= liborPeriodDiscretizationNext.getTime(componentIndex);
			final double						periodEnd	= liborPeriodDiscretizationNext.getTime(componentIndex+1);
			final RandomVariable[] factorLoadingVector = covarianceModel.getFactorLoading(process.getTime(timeIndex), periodStart,  periodEnd, liborPeriodDiscretization, realizationAtTimeIndex, this);

			return factorLoadingVector;
		}
		else {
			final RandomVariable[] zeros = new RandomVariable[process.getStochasticDriver().getNumberOfFactors()];
			Arrays.fill(zeros, zero);
			return zeros;
		}

	}

	@Override
	public RandomVariable applyStateSpaceTransform(final MonteCarloProcess process, final int timeIndex, final int componentIndex, final RandomVariable randomVariable) {
		return randomVariable;
	}

	@Override
	public RandomVariable applyStateSpaceTransformInverse(final MonteCarloProcess process, final int timeIndex, final int componentIndex, final RandomVariable randomVariable) {
		return randomVariable;
	}

	@Override
	public RandomVariable getRandomVariableForConstant(final double value) {
		return randomVariableFactory.createRandomVariable(value);
	}

	private TimeDiscretization getLiborPeriodDiscretization(final double time) {
		final ArrayList<Double> tenorTimes = new ArrayList<>();
		final double firstTime	= liborPeriodDiscretizations[0].getTime(liborPeriodDiscretizations[0].getTimeIndexNearestLessOrEqual(time));
		double lastTime		= firstTime;
		tenorTimes.add(firstTime);
		for(int discretizationLevelIndex = 0; discretizationLevelIndex<liborPeriodDiscretizations.length; discretizationLevelIndex++) {
			final int tentorIntervalStartIndex = liborPeriodDiscretizations[discretizationLevelIndex].getTimeIndexNearestLessOrEqual(lastTime)+1;
			for(int tenorInterval=0; tenorInterval<numberOfDiscretizationIntervals[discretizationLevelIndex]; tenorInterval++) {
				if(tentorIntervalStartIndex+tenorInterval >= liborPeriodDiscretizations[discretizationLevelIndex].getNumberOfTimes()) {
					break;
				}
				lastTime = liborPeriodDiscretizations[discretizationLevelIndex].getTime(tentorIntervalStartIndex+tenorInterval);
				// round to liborPeriodDiscretizations[0]
				lastTime = liborPeriodDiscretizations[0].getTime(liborPeriodDiscretizations[0].getTimeIndexNearestLessOrEqual(lastTime));
				tenorTimes.add(lastTime);
			}
		}

		return new TimeDiscretizationFromArray(tenorTimes);
	}

	public RandomVariable getStateVariableForPeriod(final TimeDiscretization liborPeriodDiscretization, final RandomVariable[] stateVariables, final double periodStart, final double periodEnd) {

		int periodStartIndex = liborPeriodDiscretization.getTimeIndex(periodStart);
		int periodEndIndex = liborPeriodDiscretization.getTimeIndex(periodEnd);

		RandomVariable stateVariableSum = randomVariableFactory.createRandomVariable(0.0);

		if(periodStartIndex < 0) {
			periodStartIndex = -periodStartIndex-1;
			if(periodStartIndex >= liborPeriodDiscretization.getNumberOfTimes()) {
				throw new IllegalArgumentException();
			}
			final RandomVariable stateVariable = stateVariables[periodStartIndex-1];
			final double shortPeriodEnd = liborPeriodDiscretization.getTime(periodStartIndex);
			final double tenorRefinementWeight = getWeightForTenorRefinement(liborPeriodDiscretization.getTime(periodStartIndex-1), shortPeriodEnd, periodStart, shortPeriodEnd);
			final RandomVariable integratedVariance = stateVariables[getNumberOfLibors()+periodStartIndex-1];

			final double tenor = covarianceModel.getScaledTenorTime(periodStart, shortPeriodEnd);
			stateVariableSum = stateVariableSum.addProduct(stateVariable.addProduct(integratedVariance, tenorRefinementWeight), tenor);
		}

		if(periodEndIndex < 0) {
			periodEndIndex = -periodEndIndex-1;
			final RandomVariable stateVariable = stateVariables[periodEndIndex-1];
			final double shortPeriodStart = liborPeriodDiscretization.getTime(periodEndIndex-1);
			final double tenorRefinementWeight = getWeightForTenorRefinement(shortPeriodStart, liborPeriodDiscretization.getTime(periodEndIndex), shortPeriodStart, periodEnd);
			final RandomVariable integratedVariance = stateVariables[getNumberOfLibors()+periodEndIndex-1];

			final double tenor = covarianceModel.getScaledTenorTime(shortPeriodStart, periodEnd);
			stateVariableSum = stateVariableSum.addProduct(stateVariable.addProduct(integratedVariance, tenorRefinementWeight), tenor);
			periodEndIndex--;
		}

		for(int periodIndex = periodStartIndex; periodIndex<periodEndIndex; periodIndex++) {
			final RandomVariable stateVariable = stateVariables[periodIndex];

			final double tenor = covarianceModel.getScaledTenorTime(liborPeriodDiscretization.getTime(periodIndex), liborPeriodDiscretization.getTime(periodIndex+1));
			stateVariableSum = stateVariableSum.addProduct(stateVariable, tenor);
		}
		final double tenor = covarianceModel.getScaledTenorTime(periodStart, periodEnd);
		stateVariableSum = stateVariableSum.div(tenor);

		return stateVariableSum;
	}

	public RandomVariable getLIBORForStateVariable(final TimeDiscretization liborPeriodDiscretization, final RandomVariable[] stateVariables, final double periodStart, final double periodEnd) {
		RandomVariable stateVariable = getStateVariableForPeriod(liborPeriodDiscretization, stateVariables, periodStart, periodEnd);
		stateVariable = stateVariable.mult(periodEnd-periodStart).add(Math.log(1+forwardRateCurve.getForward(null, periodStart)*(periodEnd-periodStart)));
		final RandomVariable libor = stateVariable.exp().sub(1.0).div(periodEnd-periodStart);

		return null;//libor;
	}

	public RandomVariable getStateVariable(final MonteCarloProcess process, final int timeIndex, final double periodStart, final double periodEnd)
	{
		// @TODO Make getLiborPeriodDiscretization to use timeIndex
		final double time = process.getTimeDiscretization().getTime(timeIndex);
		final TimeDiscretization liborPeriodDiscretization = this.getLiborPeriodDiscretization(time);
		//		return getStateVariableForPeriod(liborPeriodDiscretization, stateVariables, periodStart, periodEnd);

		int periodStartIndex = liborPeriodDiscretization.getTimeIndex(periodStart);
		int periodEndIndex = liborPeriodDiscretization.getTimeIndex(periodEnd);

		RandomVariable stateVariableSum = null;
		try {
			stateVariableSum = process.getStochasticDriver().getRandomVariableForConstant(0.0);

			if(periodStartIndex < 0) {
				periodStartIndex = -periodStartIndex-1;
				if(periodStartIndex >= liborPeriodDiscretization.getNumberOfTimes()) {
					throw new IllegalArgumentException();
				}
				final RandomVariable stateVariable = process.getProcessValue(timeIndex, periodStartIndex-1);
				final double shortPeriodEnd = liborPeriodDiscretization.getTime(periodStartIndex);
				final double tenorRefinementWeight = getWeightForTenorRefinement(liborPeriodDiscretization.getTime(periodStartIndex-1), shortPeriodEnd, periodStart, shortPeriodEnd);
				final RandomVariable integratedVariance = getIntegratedVariance(process, timeIndex, liborPeriodDiscretization.getTime(periodStartIndex-1), liborPeriodDiscretization.getTime(periodStartIndex));

				stateVariableSum = stateVariableSum.addProduct(stateVariable.addProduct(integratedVariance, tenorRefinementWeight), covarianceModel.getScaledTenorTime(periodStart, shortPeriodEnd));
			}

			if(periodEndIndex < 0) {
				periodEndIndex = -periodEndIndex-1;
				final RandomVariable stateVariable = process.getProcessValue(timeIndex, periodEndIndex-1);
				final double shortPeriodStart = liborPeriodDiscretization.getTime(periodEndIndex-1);
				final double tenorRefinementWeight = getWeightForTenorRefinement(shortPeriodStart, liborPeriodDiscretization.getTime(periodEndIndex), shortPeriodStart, periodEnd);
				final RandomVariable integratedVariance = getIntegratedVariance(process, timeIndex, liborPeriodDiscretization.getTime(periodEndIndex-1), liborPeriodDiscretization.getTime(periodEndIndex));

				stateVariableSum = stateVariableSum.addProduct(stateVariable.addProduct(integratedVariance, tenorRefinementWeight), covarianceModel.getScaledTenorTime(shortPeriodStart,periodEnd));
				periodEndIndex--;
			}

			for(int periodIndex = periodStartIndex; periodIndex<periodEndIndex; periodIndex++) {
				final RandomVariable stateVariable = process.getProcessValue(timeIndex, periodIndex);

				stateVariableSum = stateVariableSum.addProduct(stateVariable, covarianceModel.getScaledTenorTime(liborPeriodDiscretization.getTime(periodIndex), liborPeriodDiscretization.getTime(periodIndex+1)));
			}
			stateVariableSum = stateVariableSum.div(covarianceModel.getScaledTenorTime(periodStart,periodEnd));
		} catch (final CalculationException e) {
		}

		return stateVariableSum;
	}


	@Override
	public RandomVariable getForwardRate(final MonteCarloProcess process, final double time, final double periodStart, final double periodEnd) {
		int timeIndex = process.getTimeIndex(time);
		// @TODO Improve interpolation in simulation time here, if required.
		if(timeIndex < 0) {
			timeIndex = -timeIndex-1-1;
		}

		return getLIBOR(process, timeIndex, periodStart, periodEnd);
	}

	public RandomVariable getLIBOR(final MonteCarloProcess process, final int timeIndex, final double periodStart, final double periodEnd)
	{
		RandomVariable stateVariable = getStateVariable(process, timeIndex, periodStart, periodEnd);
		final double initialValue = Math.log(1+forwardRateCurve.getForward(curveModel, periodStart)*(forwardRateCurve.getPaymentOffset(periodStart))) / forwardRateCurve.getPaymentOffset(periodStart);
		final double tenorTime = covarianceModel.getScaledTenorTime(periodStart, periodEnd);

		stateVariable = stateVariable.mult(tenorTime).add(initialValue*(periodEnd-periodStart));
		final RandomVariable libor = stateVariable.exp().sub(1.0).div(periodEnd-periodStart);

		return libor;
	}

	@Override
	public int getNumberOfComponents() {
		return 2 * this.getLiborPeriodDiscretization(0.0).getNumberOfTimeSteps();
	}

	@Override
	public int getNumberOfFactors()
	{
		return covarianceModel.getNumberOfFactors();
	}

	public int getNumberOfLibors()
	{
		return this.getLiborPeriodDiscretization(0.0).getNumberOfTimeSteps();
	}

	@Override
	public Object clone() {
		throw new UnsupportedOperationException();
		/*
		try {
			Map<String, Object> properties = new HashMap<String, Object>();
			properties.put("measure",		measure.name());
			properties.put("stateSpace",	stateSpace.name());
			return new LIBORMarketModelWithTenorRefinement(getLiborPeriodDiscretization(), getForwardRateCurve(), getDiscountCurve(), covarianceModel, new CalibrationProduct[0], properties);
		} catch (CalculationException e) {
			return null;
		}
		 */
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

	@Override
	public TermStructureModel getCloneWithModifiedData(final Map<String, Object> dataModified) throws CalculationException {
		final CalibrationProduct[] calibrationItems = null;
		final Map<String, ?> properties = null;

		TermStructureCovarianceModel covarianceModel = this.covarianceModel;
		if(dataModified.containsKey("covarianceModel")) {
			covarianceModel = (TermStructureCovarianceModel)dataModified.get("covarianceModel");
		}

		return new LIBORMarketModelWithTenorRefinement(liborPeriodDiscretizations, numberOfDiscretizationIntervals, curveModel, forwardRateCurve, discountCurve, covarianceModel, calibrationItems, properties);
	}

	/**
	 * Returns the term structure covariance model.
	 *
	 * @return the term structure covariance model.
	 */
	public TermStructureCovarianceModel getCovarianceModel() {
		return covarianceModel;
	}
}

