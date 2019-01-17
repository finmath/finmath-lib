/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2004
 */
package net.finmath.montecarlo.interestrate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.marketdata.model.curves.DiscountCurveInterface;
import net.finmath.marketdata.model.curves.ForwardCurveInterface;
import net.finmath.montecarlo.interestrate.modelplugins.TermStructureCovarianceModelInterface;
import net.finmath.montecarlo.interestrate.modelplugins.TermStructureCovarianceModelParametric;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.montecarlo.model.AbstractModel;
import net.finmath.montecarlo.process.AbstractProcessInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationInterface;

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
 * See {@link net.finmath.montecarlo.model.AbstractModelInterface} for details on the implemented interface
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
 * @see net.finmath.montecarlo.process.AbstractProcessInterface The interface for numerical schemes.
 * @see net.finmath.montecarlo.model.AbstractModelInterface The interface for models provinding parameters to numerical schemes.
 * @see <a href="https://papers.ssrn.com/sol3/papers.cfm?abstract_id=2884699">https://papers.ssrn.com/sol3/papers.cfm?abstract_id=2884699</a>
 */
public class LIBORMarketModelWithTenorRefinement extends AbstractModel implements TermStructureModelInterface {

	public enum Driftapproximation	{ EULER, LINE_INTEGRAL, PREDICTOR_CORRECTOR }

	private final TimeDiscretizationInterface[]		liborPeriodDiscretizations;
	private final Integer[]							numberOfDiscretizationIntervalls;

	private String							forwardCurveName;
	private AnalyticModelInterface			curveModel;

	private ForwardCurveInterface			forwardRateCurve;
	private DiscountCurveInterface			discountCurve;

	private TermStructureCovarianceModelInterface	covarianceModel;

	// Cache for the numeraires, needs to be invalidated if process changes
	private final ConcurrentHashMap<Integer, RandomVariableInterface>	numeraires;
	private AbstractProcessInterface									numerairesProcess = null;

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
	 * @param numberOfDiscretizationIntervalls A vector of number of periods to be taken from the liborPeriodDiscretizations.
	 * @param analyticModel The associated analytic model of this model (containing the associated market data objects like curve).
	 * @param forwardRateCurve The initial values for the forward rates.
	 * @param discountCurve The discount curve to use. This will create an LMM model with a deterministic zero-spread discounting adjustment.
	 * @param covarianceModel The covariance model to use.
	 * @param calibrationProducts The vector of calibration items (a union of a product, target value and weight) for the objective function sum weight(i) * (modelValue(i)-targetValue(i).
	 * @param properties Key value map specifying properties like <code>measure</code> and <code>stateSpace</code>.
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	public LIBORMarketModelWithTenorRefinement(
			TimeDiscretizationInterface[]		liborPeriodDiscretizations,
			Integer[]							numberOfDiscretizationIntervalls,
			AnalyticModelInterface				analyticModel,
			ForwardCurveInterface				forwardRateCurve,
			DiscountCurveInterface				discountCurve,
			TermStructureCovarianceModelInterface	covarianceModel,
			CalibrationProduct[]					calibrationProducts,
			Map<String, ?>						properties
			) throws CalculationException {

		Map<String,Object> calibrationParameters = null;
		if(properties != null && properties.containsKey("calibrationParameters")) {
			calibrationParameters	= (Map<String,Object>)properties.get("calibrationParameters");
		}

		this.liborPeriodDiscretizations	= liborPeriodDiscretizations;
		this.numberOfDiscretizationIntervalls = numberOfDiscretizationIntervalls;
		this.curveModel					= analyticModel;
		this.forwardRateCurve	= forwardRateCurve;
		this.discountCurve		= discountCurve;
		this.covarianceModel	= covarianceModel;

		// Perform calibration, if data is given
		if(calibrationProducts != null && calibrationProducts.length > 0) {
			TermStructureCovarianceModelParametric covarianceModelParametric = null;
			try {
				covarianceModelParametric = (TermStructureCovarianceModelParametric)covarianceModel;
			}
			catch(Exception e) {
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
	 * @return The numeraire at the specified time as <code>RandomVariableInterface</code>
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	@Override
	public RandomVariableInterface getNumeraire(double time) throws CalculationException {
		int timeIndex = liborPeriodDiscretizations[0].getTimeIndex(time);
		TimeDiscretizationInterface liborPeriodDiscretization = liborPeriodDiscretizations[0];
		if(timeIndex < 0) {
			// Interpolation of Numeraire: log linear interpolation.
			int upperIndex = -timeIndex-1;
			int lowerIndex = upperIndex-1;
			if(lowerIndex < 0) {
				throw new IllegalArgumentException("Numeraire requested for time " + time + ". Unsupported");
			}

			double alpha = (time-liborPeriodDiscretization.getTime(lowerIndex)) / (liborPeriodDiscretization.getTime(upperIndex) - liborPeriodDiscretization.getTime(lowerIndex));
			RandomVariableInterface numeraire = getNumeraire(liborPeriodDiscretization.getTime(upperIndex)).log().mult(alpha).add(getNumeraire(liborPeriodDiscretization.getTime(lowerIndex)).log().mult(1.0-alpha)).exp();

			/*
			 * Adjust for discounting, i.e. funding or collateralization
			 */
			if(discountCurve != null) {
				// This includes a control for zero bonds
				double deterministicNumeraireAdjustment = numeraire.invert().getAverage() / discountCurve.getDiscountFactor(curveModel, time);
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
		if(getProcess() != numerairesProcess) {
			numeraires.clear();
			numerairesProcess = getProcess();
		}

		/*
		 * Check if numeraire is part of the cache
		 */
		RandomVariableInterface numeraire = numeraires.get(timeIndex);
		if(numeraire == null) {
			/*
			 * Calculate the numeraire for timeIndex
			 */
			if(timeIndex == 0) {
				numeraire = getProcess().getStochasticDriver().getRandomVariableForConstant(1.0);
			}
			else {
				// Initialize to previous numeraire
				numeraire = getNumeraire(liborPeriodDiscretizations[0].getTime(timeIndex-1));

				double periodStart	= liborPeriodDiscretizations[0].getTime(timeIndex-1);
				double periodEnd	= liborPeriodDiscretizations[0].getTime(timeIndex);
				RandomVariableInterface libor = getLIBOR(periodStart, periodStart, periodEnd);

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
			double deterministicNumeraireAdjustment = numeraire.invert().getAverage() / discountCurve.getDiscountFactor(curveModel, time);
			numeraire = numeraire.mult(deterministicNumeraireAdjustment);
		}

		return numeraire;
	}

	@Override
	public RandomVariableInterface[] getInitialState() {
		RandomVariableInterface[] initialStateRandomVariable = new RandomVariableInterface[getNumberOfComponents()];
		for(int componentIndex=0; componentIndex<getNumberOfComponents(); componentIndex++) {
			initialStateRandomVariable[componentIndex] = getProcess().getStochasticDriver().getRandomVariableForConstant(0.0);
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
	 * @see net.finmath.montecarlo.interestrate.LIBORMarketModelWithTenorRefinement#getNumeraire(double) The calculation of the drift is consistent with the calculation of the numeraire in <code>getNumeraire</code>.
	 * @see net.finmath.montecarlo.interestrate.LIBORMarketModelWithTenorRefinement#getFactorLoading(int, int, RandomVariableInterface[]) The factor loading \( \lambda_{j,k} \).
	 *
	 * @param timeIndex Time index <i>i</i> for which the drift should be returned <i>&mu;(t<sub>i</sub>)</i>.
	 * @param realizationAtTimeIndex Time current forward rate vector at time index <i>i</i> which should be used in the calculation.
	 * @return The drift vector &mu;(t<sub>i</sub>) as <code>RandomVariable[]</code>
	 */
	@Override
	public RandomVariableInterface[] getDrift(int timeIndex, RandomVariableInterface[] realizationAtTimeIndex, RandomVariableInterface[] realizationPredictor) {

		double	time				= getTime(timeIndex);
		double	timeStep			= getTimeDiscretization().getTimeStep(timeIndex);
		double	timeNext			= getTime(timeIndex+1);

		RandomVariableInterface		zero	= getProcess().getStochasticDriver().getRandomVariableForConstant(0.0);

		// Allocate drift vector and initialize to zero (will be used to sum up drift components)
		RandomVariableInterface[]	drift = new RandomVariableInterface[getNumberOfComponents()];
		for(int componentIndex=0; componentIndex<getNumberOfComponents(); componentIndex++) {
			drift[componentIndex] = null;
		}

		RandomVariableInterface[]	variances	= new RandomVariableInterface[getNumberOfComponents()];
		for(int componentIndex=0; componentIndex<getNumberOfComponents(); componentIndex++) {
			variances[componentIndex] = zero;
		}

		RandomVariableInterface[]	covarianceFactorSums	= new RandomVariableInterface[getNumberOfFactors()];
		for(int factorIndex=0; factorIndex<getNumberOfFactors(); factorIndex++) {
			covarianceFactorSums[factorIndex] = zero;
		}

		/*
		 * Standard HJM drift part of log-forward-bond
		 */
		TimeDiscretizationInterface liborPeriodDiscretization = getLiborPeriodDiscretization(timeNext);
		// Calculate drift for the component componentIndex (starting at firstLiborIndex, others are zero)
		for(int componentIndex=0; componentIndex<liborPeriodDiscretization.getNumberOfTimeSteps(); componentIndex++) {
			drift[componentIndex] = zero;

			double						periodStart		= liborPeriodDiscretization.getTime(componentIndex);
			double						periodLength	= liborPeriodDiscretization.getTimeStep(componentIndex);
			double						periodEnd		= periodStart + periodLength;
			double						tenorTime		= covarianceModel.getScaledTenorTime(periodStart, periodEnd);

			// @todo Document that factorLoading componentIndexing is on time discretization of t+1 for interval (t,t+1)
			RandomVariableInterface[]	factorLoading   	= getFactorLoading(timeIndex, componentIndex, realizationAtTimeIndex);
			double weight = getWeightForTenorRefinement(periodStart,periodStart,periodStart,periodEnd);
			for(int factorIndex=0; factorIndex<getNumberOfFactors(); factorIndex++) {
				drift[componentIndex] = drift[componentIndex].addProduct(covarianceFactorSums[factorIndex].addProduct(factorLoading[factorIndex], weight),factorLoading[factorIndex]);
				variances[componentIndex] = variances[componentIndex].addProduct(factorLoading[factorIndex], factorLoading[factorIndex]);
				covarianceFactorSums[factorIndex] = covarianceFactorSums[factorIndex].addProduct(factorLoading[factorIndex],tenorTime);
			}
		}

		/*
		 * Change of tenor discretization - impact on log-forward-bond
		 */
		TimeDiscretizationInterface liborPeriodDiscretizationPrevious = getLiborPeriodDiscretization(time);
		for(int componentIndex=0; componentIndex<liborPeriodDiscretization.getNumberOfTimeSteps(); componentIndex++) {
			double						periodStart		= liborPeriodDiscretization.getTime(componentIndex);
			double						periodLength	= liborPeriodDiscretization.getTimeStep(componentIndex);
			double						periodEnd		= periodStart + periodLength;

			double						periodStartPrevious		= liborPeriodDiscretizationPrevious.getTime(componentIndex);
			double						periodLengthPrevious	= liborPeriodDiscretizationPrevious.getTimeStep(componentIndex);
			double						periodEndPrevious		= periodStartPrevious + periodLengthPrevious;

			if(periodStartPrevious == periodStart && periodEndPrevious == periodEnd) {
				continue;
			}

			RandomVariableInterface		stateVariablePrevious	= getStateVariable(timeIndex, periodStartPrevious,	periodEndPrevious);
			RandomVariableInterface		stateVariable			= getStateVariable(timeIndex, periodStart, 			periodEnd);

			if(Double.isNaN(stateVariable.getAverage()) || Double.isNaN(stateVariablePrevious.getAverage())) {
				throw new IllegalArgumentException();
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
			double						periodStart		= liborPeriodDiscretization.getTime(componentIndex);
			double						periodLength	= liborPeriodDiscretization.getTimeStep(componentIndex);
			double						periodEnd		= periodStart + periodLength;

			double						periodStartPrevious		= liborPeriodDiscretizationPrevious.getTime(componentIndex);
			double						periodLengthPrevious	= liborPeriodDiscretizationPrevious.getTimeStep(componentIndex);
			double						periodEndPrevious		= periodStartPrevious + periodLengthPrevious;

			if(periodStartPrevious == periodStart && periodEndPrevious == periodEnd) {
				continue;
			}

			RandomVariableInterface		stateVariablePrevious	= getIntegratedVariance(timeIndex, periodStartPrevious, periodEndPrevious);
			RandomVariableInterface		stateVariable			= getIntegratedVariance(timeIndex, periodStart, 		periodEnd);

			if(Double.isNaN(stateVariable.getAverage()) || Double.isNaN(stateVariablePrevious.getAverage())) {
				throw new IllegalArgumentException();
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
	private RandomVariableInterface getIntegratedVariance(int timeIndex, double periodStart, double periodEnd) {
		TimeDiscretizationInterface liborPeriodTiscretization = getLiborPeriodDiscretization(getTime(timeIndex));

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

		RandomVariableInterface integratedVariance = null;
		try {
			integratedVariance = getProcess().getProcessValue(timeIndex, getNumberOfLibors()+periodStartIndex);
		} catch (CalculationException e) {
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
	private double getWeightForTenorRefinement(double periodStartPrevious, double periodEndPrevious, double periodStart, double periodEnd) {
		TimeDiscretizationInterface numeriareDiscretization = liborPeriodDiscretizations[0];

		int periodStartPreviousIndex = numeriareDiscretization.getTimeIndex(periodStartPrevious);
		int periodEndPreviousIndex = numeriareDiscretization.getTimeIndex(periodEndPrevious);
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
			double deltaT = covarianceModel.getScaledTenorTime(numeriareDiscretization.getTime(periodIndex), numeriareDiscretization.getTime(periodIndex+1));
			double deltaTSum = covarianceModel.getScaledTenorTime(periodStartPrevious, numeriareDiscretization.getTime(periodIndex+1));
			weight1 +=  deltaT * deltaTSum;
		}

		double weight2 = 0.0;
		for(int periodIndex = periodStartIndex; periodIndex<periodEndIndex; periodIndex++) {
			double deltaT = covarianceModel.getScaledTenorTime(numeriareDiscretization.getTime(periodIndex), numeriareDiscretization.getTime(periodIndex+1));
			double deltaTSum = covarianceModel.getScaledTenorTime(periodStartPrevious, numeriareDiscretization.getTime(periodIndex+1));
			weight2 +=  deltaT * deltaTSum;
		}

		if(weight1 > 0) {
			return weight2 / covarianceModel.getScaledTenorTime(periodStart, periodEnd) - weight1 / covarianceModel.getScaledTenorTime(periodStartPrevious, periodEndPrevious);
		} else {
			return weight2 / covarianceModel.getScaledTenorTime(periodStart, periodEnd);
		}
	}

	@Override
	public	RandomVariableInterface[]	getFactorLoading(int timeIndex, int componentIndex, RandomVariableInterface[] realizationAtTimeIndex)
	{
		RandomVariableInterface zero = getProcess().getStochasticDriver().getRandomVariableForConstant(0.0);

		if(componentIndex < getNumberOfLibors()) {
			TimeDiscretizationInterface liborPeriodDiscretization = getLiborPeriodDiscretization(getTime(timeIndex));
			TimeDiscretizationInterface liborPeriodDiscretizationNext = getLiborPeriodDiscretization(getTime(timeIndex+1));
			double						periodStart	= liborPeriodDiscretizationNext.getTime(componentIndex);
			double						periodEnd	= liborPeriodDiscretizationNext.getTime(componentIndex+1);
			RandomVariableInterface[] factorLoadingVector = covarianceModel.getFactorLoading(getTime(timeIndex), periodStart,  periodEnd, liborPeriodDiscretization, realizationAtTimeIndex, this);

			return factorLoadingVector;
		}
		else {
			RandomVariableInterface[] zeros = new RandomVariableInterface[getProcess().getStochasticDriver().getNumberOfFactors()];
			Arrays.fill(zeros, zero);
			return zeros;
		}

	}

	@Override
	public RandomVariableInterface applyStateSpaceTransform(int componentIndex, RandomVariableInterface randomVariable) {
		return randomVariable;
	}

	@Override
	public RandomVariableInterface applyStateSpaceTransformInverse(int componentIndex, RandomVariableInterface randomVariable) {
		return randomVariable;
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.model.AbstractModelInterface#getRandomVariableForConstant(double)
	 */
	@Override
	public RandomVariableInterface getRandomVariableForConstant(double value) {
		return getProcess().getStochasticDriver().getRandomVariableForConstant(value);
	}

	private TimeDiscretizationInterface getLiborPeriodDiscretization(double time) {
		ArrayList<Double> tenorTimes = new ArrayList<>();
		double firstTime	= liborPeriodDiscretizations[0].getTime(liborPeriodDiscretizations[0].getTimeIndexNearestLessOrEqual(time));
		double lastTime		= firstTime;
		tenorTimes.add(firstTime);
		for(int discretizationLevelIndex = 0; discretizationLevelIndex<liborPeriodDiscretizations.length; discretizationLevelIndex++) {
			int tentorIntervallStartIndex = liborPeriodDiscretizations[discretizationLevelIndex].getTimeIndexNearestLessOrEqual(lastTime)+1;
			for(int tenorIntervall=0; tenorIntervall<numberOfDiscretizationIntervalls[discretizationLevelIndex]; tenorIntervall++) {
				if(tentorIntervallStartIndex+tenorIntervall >= liborPeriodDiscretizations[discretizationLevelIndex].getNumberOfTimes()) {
					break;
				}
				lastTime = liborPeriodDiscretizations[discretizationLevelIndex].getTime(tentorIntervallStartIndex+tenorIntervall);
				// round to liborPeriodDiscretizations[0]
				lastTime = liborPeriodDiscretizations[0].getTime(liborPeriodDiscretizations[0].getTimeIndexNearestLessOrEqual(lastTime));
				tenorTimes.add(lastTime);
			}
		}

		return new TimeDiscretization(tenorTimes);
	}

	public RandomVariableInterface getStateVariableForPeriod(TimeDiscretizationInterface liborPeriodDiscretization, RandomVariableInterface[] stateVariables, double periodStart, double periodEnd) {

		int periodStartIndex = liborPeriodDiscretization.getTimeIndex(periodStart);
		int periodEndIndex = liborPeriodDiscretization.getTimeIndex(periodEnd);

		RandomVariableInterface stateVariableSum = this.getProcess().getStochasticDriver().getRandomVariableForConstant(0.0);

		if(periodStartIndex < 0) {
			periodStartIndex = -periodStartIndex-1;
			if(periodStartIndex >= liborPeriodDiscretization.getNumberOfTimes()) {
				throw new IllegalArgumentException();
			}
			RandomVariableInterface stateVariable = stateVariables[periodStartIndex-1];
			double shortPeriodEnd = liborPeriodDiscretization.getTime(periodStartIndex);
			double tenorRefinementWeight = getWeightForTenorRefinement(liborPeriodDiscretization.getTime(periodStartIndex-1), shortPeriodEnd, periodStart, shortPeriodEnd);
			RandomVariableInterface integratedVariance = stateVariables[getNumberOfLibors()+periodStartIndex-1];

			double tenor = covarianceModel.getScaledTenorTime(periodStart, shortPeriodEnd);
			stateVariableSum = stateVariableSum.addProduct(stateVariable.addProduct(integratedVariance, tenorRefinementWeight), tenor);
		}

		if(periodEndIndex < 0) {
			periodEndIndex = -periodEndIndex-1;
			RandomVariableInterface stateVariable = stateVariables[periodEndIndex-1];
			double shortPeriodStart = liborPeriodDiscretization.getTime(periodEndIndex-1);
			double tenorRefinementWeight = getWeightForTenorRefinement(shortPeriodStart, liborPeriodDiscretization.getTime(periodEndIndex), shortPeriodStart, periodEnd);
			RandomVariableInterface integratedVariance = stateVariables[getNumberOfLibors()+periodEndIndex-1];

			double tenor = covarianceModel.getScaledTenorTime(shortPeriodStart, periodEnd);
			stateVariableSum = stateVariableSum.addProduct(stateVariable.addProduct(integratedVariance, tenorRefinementWeight), tenor);
			periodEndIndex--;
		}

		for(int periodIndex = periodStartIndex; periodIndex<periodEndIndex; periodIndex++) {
			RandomVariableInterface stateVariable = stateVariables[periodIndex];

			double tenor = covarianceModel.getScaledTenorTime(liborPeriodDiscretization.getTime(periodIndex), liborPeriodDiscretization.getTime(periodIndex+1));
			stateVariableSum = stateVariableSum.addProduct(stateVariable, tenor);
		}
		double tenor = covarianceModel.getScaledTenorTime(periodStart, periodEnd);
		stateVariableSum = stateVariableSum.div(tenor);

		return stateVariableSum;
	}

	public RandomVariableInterface getLIBORForStateVariable(TimeDiscretizationInterface liborPeriodDiscretization, RandomVariableInterface[] stateVariables, double periodStart, double periodEnd) {
		RandomVariableInterface stateVariable = getStateVariableForPeriod(liborPeriodDiscretization, stateVariables, periodStart, periodEnd);
		stateVariable = stateVariable.mult(periodEnd-periodStart).add(Math.log(1+forwardRateCurve.getForward(null, periodStart)*(periodEnd-periodStart)));
		RandomVariableInterface libor = stateVariable.exp().sub(1.0).div(periodEnd-periodStart);

		return null;//libor;
	}

	public RandomVariableInterface getStateVariable(int timeIndex, double periodStart, double periodEnd)
	{
		// @TODO: Make getLiborPeriodDiscretization to use timeIndex
		double time = this.getTimeDiscretization().getTime(timeIndex);
		TimeDiscretizationInterface liborPeriodDiscretization = this.getLiborPeriodDiscretization(time);
		//		return getStateVariableForPeriod(liborPeriodDiscretization, stateVariables, periodStart, periodEnd);

		int periodStartIndex = liborPeriodDiscretization.getTimeIndex(periodStart);
		int periodEndIndex = liborPeriodDiscretization.getTimeIndex(periodEnd);

		RandomVariableInterface stateVariableSum = null;
		try {
			stateVariableSum = this.getProcess().getStochasticDriver().getRandomVariableForConstant(0.0);

			if(periodStartIndex < 0) {
				periodStartIndex = -periodStartIndex-1;
				if(periodStartIndex >= liborPeriodDiscretization.getNumberOfTimes()) {
					throw new IllegalArgumentException();
				}
				RandomVariableInterface stateVariable = getProcessValue(timeIndex, periodStartIndex-1);
				double shortPeriodEnd = liborPeriodDiscretization.getTime(periodStartIndex);
				double tenorRefinementWeight = getWeightForTenorRefinement(liborPeriodDiscretization.getTime(periodStartIndex-1), shortPeriodEnd, periodStart, shortPeriodEnd);
				RandomVariableInterface integratedVariance = getIntegratedVariance(timeIndex, liborPeriodDiscretization.getTime(periodStartIndex-1), liborPeriodDiscretization.getTime(periodStartIndex));

				stateVariableSum = stateVariableSum.addProduct(stateVariable.addProduct(integratedVariance, tenorRefinementWeight), covarianceModel.getScaledTenorTime(periodStart, shortPeriodEnd));
			}

			if(periodEndIndex < 0) {
				periodEndIndex = -periodEndIndex-1;
				RandomVariableInterface stateVariable = getProcessValue(timeIndex, periodEndIndex-1);
				double shortPeriodStart = liborPeriodDiscretization.getTime(periodEndIndex-1);
				double tenorRefinementWeight = getWeightForTenorRefinement(shortPeriodStart, liborPeriodDiscretization.getTime(periodEndIndex), shortPeriodStart, periodEnd);
				RandomVariableInterface integratedVariance = getIntegratedVariance(timeIndex, liborPeriodDiscretization.getTime(periodEndIndex-1), liborPeriodDiscretization.getTime(periodEndIndex));

				stateVariableSum = stateVariableSum.addProduct(stateVariable.addProduct(integratedVariance, tenorRefinementWeight), covarianceModel.getScaledTenorTime(shortPeriodStart,periodEnd));
				periodEndIndex--;
			}

			for(int periodIndex = periodStartIndex; periodIndex<periodEndIndex; periodIndex++) {
				RandomVariableInterface stateVariable = getProcessValue(timeIndex, periodIndex);

				stateVariableSum = stateVariableSum.addProduct(stateVariable, covarianceModel.getScaledTenorTime(liborPeriodDiscretization.getTime(periodIndex), liborPeriodDiscretization.getTime(periodIndex+1)));
			}
			stateVariableSum = stateVariableSum.div(covarianceModel.getScaledTenorTime(periodStart,periodEnd));
		} catch (CalculationException e) {
		}

		return stateVariableSum;
	}


	@Override
	public RandomVariableInterface getLIBOR(double time, double periodStart, double periodEnd) {
		int timeIndex = getProcess().getTimeIndex(time);
		// @TODO Improve interpolation in simulation time here, if required.
		if(timeIndex < 0) {
			timeIndex = -timeIndex-1-1;
		}

		return getLIBOR(timeIndex, periodStart, periodEnd);
	}

	public RandomVariableInterface getLIBOR(int timeIndex, double periodStart, double periodEnd)
	{
		RandomVariableInterface stateVariable = getStateVariable(timeIndex, periodStart, periodEnd);
		double initialValue = Math.log(1+forwardRateCurve.getForward(curveModel, periodStart)*(forwardRateCurve.getPaymentOffset(periodStart))) / forwardRateCurve.getPaymentOffset(periodStart);
		double tenorTime = covarianceModel.getScaledTenorTime(periodStart, periodEnd);

		stateVariable = stateVariable.mult(tenorTime).add(initialValue*(periodEnd-periodStart));
		RandomVariableInterface libor = stateVariable.exp().sub(1.0).div(periodEnd-periodStart);

		return libor;
	}

	@Override
	public int getNumberOfComponents() {
		return 2 * this.getLiborPeriodDiscretization(0.0).getNumberOfTimeSteps();
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
	public AnalyticModelInterface getAnalyticModel() {
		return curveModel;
	}

	@Override
	public DiscountCurveInterface getDiscountCurve() {
		return discountCurve;
	}

	@Override
	public ForwardCurveInterface getForwardRateCurve() {
		return forwardRateCurve;
	}

	@Override
	public TermStructureModelInterface getCloneWithModifiedData(Map<String, Object> dataModified) throws CalculationException {
		CalibrationProduct[] calibrationItems = null;
		Map<String, ?> properties = null;

		TermStructureCovarianceModelInterface covarianceModel = this.covarianceModel;
		if(dataModified.containsKey("covarianceModel")) {
			covarianceModel = (TermStructureCovarianceModelInterface)dataModified.get("covarianceModel");
		}

		return new LIBORMarketModelWithTenorRefinement(liborPeriodDiscretizations, numberOfDiscretizationIntervalls, curveModel, forwardRateCurve, discountCurve, covarianceModel, calibrationItems, properties);
	}

	/**
	 * Returns the term structure covariance model.
	 *
	 * @return the term structure covariance model.
	 */
	public TermStructureCovarianceModelInterface getCovarianceModel() {
		return covarianceModel;
	}
}

