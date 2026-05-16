/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2004
 */
package net.finmath.montecarlo.interestrate.models;

import java.util.ArrayList;
import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.functions.AnalyticFormulas;
import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.DiscountCurveFromForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.model.volatilities.SwaptionMarketData;
import net.finmath.marketdata.products.Swap;
import net.finmath.marketdata.products.SwapAnnuity;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFromArrayFactory;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.interestrate.CalibrationProduct;
import net.finmath.montecarlo.interestrate.LIBORMarketModel;
import net.finmath.montecarlo.interestrate.models.covariance.AbstractLIBORCovarianceModelParametric;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCovarianceModel;
import net.finmath.montecarlo.interestrate.products.AbstractTermStructureMonteCarloProduct;
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
 * Implements a basic LIBOR market model with some drift approximation methods.
 * <br>
 * The class implements different measure(drift) / numeraire pairs (terminal and spot).
 * <br>
 * The class specifies a LIBOR market model in its log-normal formulation, that is
 * <i>L<sub>j</sub> = exp(Y<sub>j</sub>) </i> where
 * <br>
 * <i>dY<sub>j</sub> = &mu;<sub>j</sub> dt + &lambda;<sub>1,j</sub> dW<sub>1</sub> + ... + &lambda;<sub>m,j</sub> dW<sub>m</sub></i>
 * <br>
 * see {@link net.finmath.montecarlo.model.ProcessModel} for details on the implemented interface.
 * <br>
 * The model uses an <code>AbstractLIBORCovarianceModel</code> for the specification of <i>(&lambda;<sub>1,j</sub>,...,&lambda;<sub>m,j</sub>)</i> as a covariance model,
 * which may have the ability to calibrate to swaptions.
 *
 * @see net.finmath.montecarlo.interestrate.models.covariance.AbstractLIBORCovarianceModel
 *
 * @author Christian Fries
 * @version 1.1
 */
public class LIBORMarketModelStandard extends AbstractProcessModel implements LIBORMarketModel {

	private static final boolean isUseAnalyticApproximation;
	static {
		// Default value is true;
		isUseAnalyticApproximation = Boolean.parseBoolean(System.getProperty("net.finmath.montecarlo.interestrate.LIBORMarketModelStandard.isUseAnalyticApproximation","true"));
	}

	public enum Driftapproximation	{ EULER, LINE_INTEGRAL, PREDICTOR_CORRECTOR }

	public enum Measure				{ SPOT, TERMINAL }

	private final TimeDiscretization		liborPeriodDiscretization;

	private String							forwardCurveName;
	private AnalyticModel			curveModel;

	private final ForwardCurve			forwardRateCurve;
	private DiscountCurve			discountCurve;

	private final RandomVariableFactory	randomVariableFactory = new RandomVariableFromArrayFactory();

	private LIBORCovarianceModel	covarianceModel;

	private SwaptionMarketData		swaptionMarketData;

	private Driftapproximation	driftApproximationMethod	= Driftapproximation.EULER;
	private Measure				measure						= Measure.SPOT;

	// This is a cache of the integrated covariance.
	private double[][][]	integratedLIBORCovariance;

	/**
	 * Creates a LIBOR Market Model for given covariance.
	 *
	 * @param liborPeriodDiscretization The discretization of the interest rate curve into forward rates (tenor structure).
	 * @param forwardRateCurve The initial values for the forward rates.
	 * @param covarianceModel The covariance model to use.
	 */
	public LIBORMarketModelStandard(
			final TimeDiscretization		liborPeriodDiscretization,
			final ForwardCurve			forwardRateCurve,
			final LIBORCovarianceModel	covarianceModel
			) {
		this.liborPeriodDiscretization	= liborPeriodDiscretization;
		this.forwardRateCurve			= forwardRateCurve;
		this.covarianceModel			= covarianceModel;
	}

	/**
	 * Creates a LIBOR Market Model for given covariance.
	 *
	 * @param liborPeriodDiscretization The discretization of the interest rate curve into forward rates (tenor structure).
	 * @param forwardRateCurve The initial values for the forward rates.
	 * @param discountCurve The discount curve to use. This will create an LMM model with a deterministic zero-spread discounting adjustment.
	 * @param covarianceModel The covariance model to use.
	 */
	public LIBORMarketModelStandard(
			final TimeDiscretization		liborPeriodDiscretization,
			final ForwardCurve			forwardRateCurve,
			final DiscountCurve			discountCurve,
			final LIBORCovarianceModel	covarianceModel
			) {
		this.liborPeriodDiscretization	= liborPeriodDiscretization;
		this.forwardRateCurve			= forwardRateCurve;
		this.discountCurve				= discountCurve;
		this.covarianceModel			= covarianceModel;
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
	public LIBORMarketModelStandard(
			final TimeDiscretization			liborPeriodDiscretization,
			final ForwardCurve				forwardRateCurve,
			final LIBORCovarianceModel		covarianceModel,
			final SwaptionMarketData			swaptionMarketData
			) throws CalculationException {
		this(liborPeriodDiscretization, forwardRateCurve, null, covarianceModel, getCalibrationItems(liborPeriodDiscretization, forwardRateCurve, swaptionMarketData));
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
	public LIBORMarketModelStandard(
			final TimeDiscretization			liborPeriodDiscretization,
			final ForwardCurve				forwardRateCurve,
			final DiscountCurve				discountCurve,
			final LIBORCovarianceModel		covarianceModel,
			final SwaptionMarketData			swaptionMarketData
			) throws CalculationException {
		this(liborPeriodDiscretization, forwardRateCurve, discountCurve, covarianceModel, getCalibrationItems(liborPeriodDiscretization, forwardRateCurve, swaptionMarketData));
	}



	/**
	 * Creates a LIBOR Market Model for given covariance.
	 *
	 * @param liborPeriodDiscretization The discretization of the interest rate curve into forward rates (tenor structure).
	 * @param forwardRateCurve The initial values for the forward rates.
	 * @param discountCurve The discount curve to use. This will create an LMM model with a deterministic zero-spread discounting adjustment.
	 * @param covarianceModel The covariance model to use.
	 * @param calibrationProducts The vector of calibration items (a union of a product, target value and weight) for the objective function sum weight(i) * (modelValue(i)-targetValue(i).
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	public LIBORMarketModelStandard(
			final TimeDiscretization				liborPeriodDiscretization,
			final ForwardCurve					forwardRateCurve,
			final DiscountCurve					discountCurve,
			final LIBORCovarianceModel			covarianceModel,
			final CalibrationProduct[]						calibrationProducts
			) throws CalculationException {
		this.liborPeriodDiscretization	= liborPeriodDiscretization;

		final double[] times = new double[liborPeriodDiscretization.getNumberOfTimeSteps()];
		for(int i=0; i<times.length; i++) {
			times[i] = liborPeriodDiscretization.getTime(i);
		}

		AbstractLIBORCovarianceModelParametric covarianceModelParametric = null;
		try {
			covarianceModelParametric = (AbstractLIBORCovarianceModelParametric)covarianceModel;
		}
		catch(final Exception e) {
			throw new ClassCastException("Calibration is currently restricted to parametric covariance models (AbstractLIBORCovarianceModelParametric).");
		}

		this.forwardRateCurve	= forwardRateCurve;
		this.discountCurve		= discountCurve;

		this.covarianceModel    = covarianceModelParametric.getCloneCalibrated(this, calibrationProducts, null);
	}

	private static CalibrationProduct[] getCalibrationItems(final TimeDiscretization liborPeriodDiscretization, final ForwardCurve forwardCurve, final SwaptionMarketData swaptionMarketData) {
		if(swaptionMarketData == null) {
			return null;
		}

		final TimeDiscretization	optionMaturities		= swaptionMarketData.getOptionMaturities();
		final TimeDiscretization	tenor					= swaptionMarketData.getTenor();
		final double						swapPeriodLength		= swaptionMarketData.getSwapPeriodLength();

		final ArrayList<CalibrationProduct> calibrationProducts = new ArrayList<>();
		for(int exerciseIndex=0; exerciseIndex<=optionMaturities.getNumberOfTimeSteps(); exerciseIndex++) {
			for(int tenorIndex=0; tenorIndex<=tenor.getNumberOfTimeSteps()-exerciseIndex; tenorIndex++) {

				// Create a swaption
				final double exerciseDate	= optionMaturities.getTime(exerciseIndex);
				final double swapLength	= tenor.getTime(tenorIndex);

				if(liborPeriodDiscretization.getTimeIndex(exerciseDate) < 0) {
					continue;
				}
				if(liborPeriodDiscretization.getTimeIndex(exerciseDate+swapLength) <= liborPeriodDiscretization.getTimeIndex(exerciseDate)) {
					continue;
				}

				final int numberOfPeriods = (int)(swapLength / swapPeriodLength);

				final double[] fixingDates      = new double[numberOfPeriods];
				final double[] paymentDates     = new double[numberOfPeriods];
				final double[] swapTenorTimes   = new double[numberOfPeriods+1];

				for(int periodStartIndex=0; periodStartIndex<numberOfPeriods; periodStartIndex++) {
					fixingDates[periodStartIndex] = exerciseDate + periodStartIndex * swapPeriodLength;
					paymentDates[periodStartIndex] = exerciseDate + (periodStartIndex+1) * swapPeriodLength;
					swapTenorTimes[periodStartIndex] = exerciseDate + periodStartIndex * swapPeriodLength;
				}
				swapTenorTimes[numberOfPeriods] = exerciseDate + numberOfPeriods * swapPeriodLength;


				// Swaptions swap rate
				final Schedule swapTenor = new RegularSchedule(new TimeDiscretizationFromArray(swapTenorTimes));
				final double swaprate = Swap.getForwardSwapRate(swapTenor, swapTenor, forwardCurve, null);

				// Set swap rates for each period
				final double[] swaprates        = new double[numberOfPeriods];
				for(int periodStartIndex=0; periodStartIndex<numberOfPeriods; periodStartIndex++) {
					swaprates[periodStartIndex] = swaprate;
				}

				if(isUseAnalyticApproximation) {
					final AbstractTermStructureMonteCarloProduct swaption = new SwaptionAnalyticApproximation(swaprate, swapTenorTimes, SwaptionAnalyticApproximation.ValueUnit.VOLATILITYLOGNORMAL);
					final double impliedVolatility = swaptionMarketData.getVolatility(exerciseDate, swapLength, swaptionMarketData.getSwapPeriodLength(), swaprate);

					calibrationProducts.add(new CalibrationProduct(swaption, impliedVolatility, 1.0));
				}
				else {
					final AbstractTermStructureMonteCarloProduct swaption = new SwaptionSimple(swaprate, swapTenorTimes, SwaptionSimple.ValueUnit.VALUE);

					final double forwardSwaprate		= Swap.getForwardSwapRate(swapTenor, swapTenor, forwardCurve);
					final double swapAnnuity 			= SwapAnnuity.getSwapAnnuity(swapTenor, forwardCurve);
					final double impliedVolatility	= swaptionMarketData.getVolatility(exerciseDate, swapLength, swaptionMarketData.getSwapPeriodLength(), swaprate);

					final double targetValue = AnalyticFormulas.blackModelSwaptionValue(forwardSwaprate, impliedVolatility, exerciseDate, swaprate, swapAnnuity);

					calibrationProducts.add(new CalibrationProduct(swaption, targetValue, 1.0));
				}
			}
		}

		return calibrationProducts.toArray(new CalibrationProduct[calibrationProducts.size()]);
	}

	/**
	 * Return the numeraire at a given time.
	 * The numeraire is provided for interpolated points. If requested on points which are not
	 * part of the tenor discretization, the numeraire uses a linear interpolation of the reciprocal
	 * value. See ISBN 0470047224 for details.
	 *
	 * @param time Time time <i>t</i> for which the numeraire should be returned <i>N(t)</i>.
	 * @return The numeraire at the specified time as <code>RandomVariableFromDoubleArray</code>
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	@Override
	public RandomVariable getNumeraire(final MonteCarloProcess process, double time) throws CalculationException {
		final int timeIndex = getLiborPeriodIndex(time);

		if(timeIndex < 0) {
			// Interpolation of Numeraire: linear interpolation of the reciprocal.
			final int lowerIndex = -timeIndex -1;
			final int upperIndex = -timeIndex;
			final double alpha = (time-getLiborPeriod(lowerIndex)) / (getLiborPeriod(upperIndex) - getLiborPeriod(lowerIndex));
			return getNumeraire(process, getLiborPeriod(upperIndex)).invert().mult(alpha).add(getNumeraire(process, getLiborPeriod(lowerIndex)).invert().mult(1.0-alpha)).invert();
		}

		// Calculate the numeraire, when time is part of liborPeriodDiscretization

		// Get the start of the product
		int firstLiborIndex		= getLiborPeriodIndex(time);
		if(firstLiborIndex < 0) {
			throw new CalculationException("Simulation time discretization not part of forward rate tenor discretization.");
		}

		// Get the end of the product
		int lastLiborIndex 	= liborPeriodDiscretization.getNumberOfTimeSteps()-1;

		if(measure == Measure.SPOT) {
			// Spot measure
			firstLiborIndex	= 0;
			lastLiborIndex	= getLiborPeriodIndex(time)-1;
		}

		/*
		 * Calculation of the numeraire
		 */

		// Initialize to 1.0
		RandomVariable numeraire = new RandomVariableFromDoubleArray(time, 1.0);

		// The product
		for(int liborIndex = firstLiborIndex; liborIndex<=lastLiborIndex; liborIndex++) {
			final RandomVariable libor = getLIBOR(process, process.getTimeIndex(Math.min(time,liborPeriodDiscretization.getTime(liborIndex))), liborIndex);

			final double periodLength = liborPeriodDiscretization.getTimeStep(liborIndex);

			if(measure == Measure.SPOT) {
				numeraire = numeraire.accrue(libor, periodLength);
			}
			else {
				numeraire = numeraire.discount(libor, periodLength);
			}
		}

		/*
		 * Adjust for discounting
		 */
		if(discountCurve != null) {
			final DiscountCurve discountcountCurveFromForwardPerformance = new DiscountCurveFromForwardCurve(forwardRateCurve);
			final double deterministicNumeraireAdjustment = discountcountCurveFromForwardPerformance.getDiscountFactor(time) / discountCurve.getDiscountFactor(time);
			numeraire = numeraire.mult(deterministicNumeraireAdjustment);
		}
		return numeraire;
	}

	@Override
	public RandomVariable[] getInitialState(MonteCarloProcess process) {
		final double[] liborInitialStates = new double[liborPeriodDiscretization.getNumberOfTimeSteps()];
		for(int timeIndex=0; timeIndex<liborPeriodDiscretization.getNumberOfTimeSteps(); timeIndex++) {
			final double rate = forwardRateCurve.getForward(null, liborPeriodDiscretization.getTime(timeIndex));
			liborInitialStates[timeIndex] = Math.log(rate);
		}

		final RandomVariable[] initialStateRandomVariable = new RandomVariable[getNumberOfComponents()];
		for(int componentIndex=0; componentIndex<getNumberOfComponents(); componentIndex++) {
			initialStateRandomVariable[componentIndex] = new RandomVariableFromDoubleArray(liborInitialStates[componentIndex]);
		}
		return initialStateRandomVariable;
	}

	/**
	 * Return the complete vector of the drift for the time index timeIndex, given that current state is realizationAtTimeIndex.
	 * Note: The random variable returned is a defensive copy and may be modified.
	 * The drift will be zero for rates being already fixed.
	 *
	 * @see net.finmath.montecarlo.interestrate.models.LIBORMarketModelStandard#getNumeraire(MonteCarloProcess, double) The calculation of the drift is consistent with the calculation of the numeraire in <code>getNumeraire</code>.
	 *
	 * @param process The discretization process generating this model. The process provides call backs for TimeDiscretization and allows calls to getProcessValue for timeIndices less or equal the given one.
	 * @param timeIndex Time index <i>i</i> for which the drift should be returned <i>&mu;(t<sub>i</sub>)</i>.
	 * @param realizationAtTimeIndex Time current forward rate vector at time index <i>i</i> which should be used in the calculation.
	 * @return The drift vector &mu;(t<sub>i</sub>) as <code>RandomVariableFromDoubleArray[]</code>
	 */
	@Override
	public RandomVariable[] getDrift(final MonteCarloProcess process, final int timeIndex, final RandomVariable[] realizationAtTimeIndex, final RandomVariable[] realizationPredictor) {
		final double	time				= process.getTime(timeIndex);
		int		firstLiborIndex		= this.getLiborPeriodIndex(time)+1;
		if(firstLiborIndex<0) {
			firstLiborIndex = -firstLiborIndex-1 + 1;
		}

		// Allocate drift vector and initialize to zero (will be used to sum up drift components)
		final RandomVariable[]	drift = new RandomVariable[getNumberOfComponents()];
		final RandomVariable[][]	covarianceFactorSums	= new RandomVariable[getNumberOfComponents()][getNumberOfFactors()];
		for(int componentIndex=firstLiborIndex; componentIndex<getNumberOfComponents(); componentIndex++) {
			drift[componentIndex] = new RandomVariableFromDoubleArray(0.0);
		}

		// Calculate drift for the component componentIndex (starting at firstLiborIndex, others are zero)
		for(int componentIndex=firstLiborIndex; componentIndex<getNumberOfComponents(); componentIndex++) {
			final double						periodLength		= liborPeriodDiscretization.getTimeStep(componentIndex);
			final RandomVariable libor = realizationAtTimeIndex[componentIndex];
			final RandomVariable oneStepMeasureTransform = libor.discount(libor, periodLength).mult(periodLength);

			//oneStepMeasureTransform = oneStepMeasureTransform.mult(libor);

			final RandomVariable[]	factorLoading   	= getFactorLoading(process, timeIndex, componentIndex, realizationAtTimeIndex);
			final RandomVariable[]   covarianceFactors   = new RandomVariable[getNumberOfFactors()];
			for(int factorIndex=0; factorIndex<getNumberOfFactors(); factorIndex++) {
				covarianceFactors[factorIndex] = factorLoading[factorIndex].mult(oneStepMeasureTransform);
				covarianceFactorSums[componentIndex][factorIndex] = covarianceFactors[factorIndex];
				if(componentIndex > firstLiborIndex) {
					covarianceFactorSums[componentIndex][factorIndex] = covarianceFactorSums[componentIndex][factorIndex].add(covarianceFactorSums[componentIndex-1][factorIndex]);
				}
			}
			for(int factorIndex=0; factorIndex<getNumberOfFactors(); factorIndex++) {
				drift[componentIndex] = drift[componentIndex].addProduct(covarianceFactorSums[componentIndex][factorIndex], factorLoading[factorIndex]);
			}
		}

		// Above is the drift for the spot measure: a simple conversion makes it the drift of the terminal measure.
		if(measure == Measure.TERMINAL) {
			for(int componentIndex=firstLiborIndex; componentIndex<getNumberOfComponents(); componentIndex++) {
				drift[componentIndex] = drift[componentIndex].sub(drift[getNumberOfComponents()-1]);
			}
		}

		// Drift adjustment for log-coordinate in each component
		for(int componentIndex=firstLiborIndex; componentIndex<getNumberOfComponents(); componentIndex++) {
			final RandomVariable		variance		= covarianceModel.getCovariance(timeIndex, componentIndex, componentIndex, realizationAtTimeIndex);
			drift[componentIndex] = drift[componentIndex].addProduct(variance, -0.5);
		}

		return drift;
	}

	@Override
	public	RandomVariable[]	getFactorLoading(final MonteCarloProcess process, final int timeIndex, final int componentIndex, final RandomVariable[] realizationAtTimeIndex)
	{
		// TODO timeDiscretization may be different. Use time instead
		return covarianceModel.getFactorLoading(timeIndex, componentIndex, realizationAtTimeIndex);
	}

	@Override
	public RandomVariable applyStateSpaceTransform(final MonteCarloProcess process, final int timeIndex, final int componentIndex, final RandomVariable randomVariable) {
		return randomVariable.exp();
	}

	@Override
	public RandomVariable applyStateSpaceTransformInverse(final MonteCarloProcess process, final int timeIndex, final int componentIndex, final RandomVariable randomVariable) {
		return randomVariable.log();
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.model.ProcessModel#getRandomVariableForConstant(double)
	 */
	@Override
	public RandomVariable getRandomVariableForConstant(final double value) {
		return randomVariableFactory.createRandomVariable(value);
	}

	/**
	 * @return Returns the driftApproximationMethod.
	 */
	public Driftapproximation getDriftApproximationMethod() {
		return driftApproximationMethod;
	}

	@Override
	public RandomVariable getForwardRate(final MonteCarloProcess process, double time, final double periodStart, final double periodEnd) throws CalculationException
	{
		final int periodStartIndex    = getLiborPeriodIndex(periodStart);
		final int periodEndIndex      = getLiborPeriodIndex(periodEnd);

		// The forward rates are provided on fractional tenor discretization points using linear interpolation. See ISBN 0470047224.

		// Interpolation on tenor, consistent with interpolation on numeraire (log-linear): interpolate end date
		if(periodEndIndex < 0) {
			final int		previousEndIndex	= (-periodEndIndex-1)-1;
			final double	previousEndTime		= getLiborPeriod(previousEndIndex);
			final double	nextEndTime			= getLiborPeriod(previousEndIndex+1);
			final RandomVariable liborLongPeriod		= getForwardRate(process, time, periodStart, nextEndTime);
			final RandomVariable	liborShortPeriod	= getForwardRate(process, time, previousEndTime, nextEndTime);

			// Interpolate libor from periodStart to periodEnd on periodEnd
			RandomVariable libor = liborLongPeriod.mult(nextEndTime-periodStart).add(1.0)
					.div(
							liborShortPeriod.mult(nextEndTime-previousEndTime).add(1.0).log().mult((nextEndTime-periodEnd)/(nextEndTime-previousEndTime)).exp()
							).sub(1.0).div(periodEnd-periodStart);

			// Analytic adjustment for the interpolation
			// @TODO reference to AnalyticModelFromCuvesAndVols must not be null
			// @TODO This adjustment only applies if the corresponding adjustment in getNumeraire is enabled
			final double analyticLibor				= getForwardRateCurve().getForward(getAnalyticModel(), previousEndTime, periodEnd-previousEndTime);
			final double analyticLiborShortPeriod		= getForwardRateCurve().getForward(getAnalyticModel(), previousEndTime, nextEndTime-previousEndTime);
			final double analyticInterpolatedOnePlusLiborDt		= (1 + analyticLiborShortPeriod * (nextEndTime-previousEndTime)) / Math.exp(Math.log(1 + analyticLiborShortPeriod * (nextEndTime-previousEndTime)) * (nextEndTime-periodEnd)/(nextEndTime-previousEndTime));
			final double analyticOnePlusLiborDt					= (1 + analyticLibor * (periodEnd-previousEndTime));
			final double adjustment = analyticOnePlusLiborDt / analyticInterpolatedOnePlusLiborDt;
			libor = libor.mult(periodEnd-periodStart).add(1.0).mult(adjustment).sub(1.0).div(periodEnd-periodStart);
			return libor;
		}

		// Interpolation on tenor, consistent with interpolation on numeraire (log-linear): interpolate start date
		if(periodStartIndex < 0) {
			final int		previousStartIndex	= (-periodStartIndex-1)-1;
			final double	previousStartTime	= getLiborPeriod(previousStartIndex);
			final double	nextStartTime		= getLiborPeriod(previousStartIndex+1);
			final RandomVariable liborLongPeriod		= getForwardRate(process, time, previousStartTime, periodEnd);
			final RandomVariable	liborShortPeriod	= getForwardRate(process, time, previousStartTime, nextStartTime);

			RandomVariable libor = liborLongPeriod.mult(periodEnd-previousStartTime).add(1.0)
					.div(
							liborShortPeriod.mult(nextStartTime-previousStartTime).add(1.0).log().mult((periodStart-previousStartTime)/(nextStartTime-previousStartTime)).exp()
							).sub(1.0).div(periodEnd-periodStart);

			// Analytic adjustment for the interpolation
			// @TODO reference to AnalyticModelFromCuvesAndVols must not be null
			// @TODO This adjustment only applies if the corresponding adjustment in getNumeraire is enabled
			final double analyticLibor				= getForwardRateCurve().getForward(getAnalyticModel(), previousStartTime, nextStartTime-periodStart);
			final double analyticLiborShortPeriod		= getForwardRateCurve().getForward(getAnalyticModel(), previousStartTime, nextStartTime-previousStartTime);
			final double analyticInterpolatedOnePlusLiborDt		= (1 + analyticLiborShortPeriod * (nextStartTime-previousStartTime)) / Math.exp(Math.log(1 + analyticLiborShortPeriod * (nextStartTime-previousStartTime)) * (nextStartTime-periodStart)/(nextStartTime-previousStartTime));
			final double analyticOnePlusLiborDt					= (1 + analyticLibor * (periodStart-previousStartTime));
			final double adjustment = analyticOnePlusLiborDt / analyticInterpolatedOnePlusLiborDt;
			libor = libor.mult(periodEnd-periodStart).add(1.0).div(adjustment).sub(1.0).div(periodEnd-periodStart);
			return libor;
		}

		if(periodStartIndex < 0 || periodEndIndex < 0) {
			throw new AssertionError("LIBOR requested outside libor discretization points and interpolation was not performed.");
		}

		// If time is beyond fixing, use the fixing time.
		time = Math.min(time, periodStart);
		int timeIndex           = process.getTimeIndex(time);

		// If time is not part of the discretization, use the latest available point.
		if(timeIndex < 0) {
			timeIndex = -timeIndex-2;
			//			double timeStep = getTimeDiscretization().getTimeStep(timeIndex);
			//			return getLIBOR(getTime(timeIndex), periodStart, periodEnd).mult((getTime(timeIndex+1)-time)/timeStep).add(getLIBOR(getTime(timeIndex+1), periodStart, periodEnd).mult((time-getTime(timeIndex))/timeStep));
		}

		// If this is a model primitive then return it
		if(periodStartIndex+1==periodEndIndex) {
			return getLIBOR(process, timeIndex, periodStartIndex);
		}

		// The requested LIBOR is not a model primitive. We need to calculate it (slow!)
		RandomVariable accrualAccount = getRandomVariableForConstant(1.0);

		// Calculate the value of the forward bond
		for(int periodIndex = periodStartIndex; periodIndex<periodEndIndex; periodIndex++)
		{
			final double subPeriodLength = getLiborPeriod(periodIndex+1) - getLiborPeriod(periodIndex);
			final RandomVariable liborOverSubPeriod = getLIBOR(process, timeIndex, periodIndex);

			accrualAccount = accrualAccount.accrue(liborOverSubPeriod, subPeriodLength);
		}

		final RandomVariable libor = accrualAccount.sub(1.0).div(periodEnd - periodStart);

		return libor;
	}

	@Override
	public RandomVariable getLIBOR(final MonteCarloProcess process, final int timeIndex, final int liborIndex) throws CalculationException
	{
		// This method is delegating to getProcessValue of process
		return process.getProcessValue(timeIndex, liborIndex);
	}

	/**
	 * This method is just a synonym to getNumberOfLibors
	 * @return The number of components
	 */
	@Override
	public int getNumberOfComponents() {
		return getNumberOfLibors();
	}

	@Override
	public int getNumberOfFactors()
	{
		return covarianceModel.getNumberOfFactors();
	}

	@Override
	public int getNumberOfLibors()
	{
		// This is just a synonym to number of components
		return liborPeriodDiscretization.getNumberOfTimeSteps();
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.LIBORMarketModel#getLiborPeriod(int)
	 */
	@Override
	public double getLiborPeriod(final int timeIndex) {
		if(timeIndex >= liborPeriodDiscretization.getNumberOfTimes()) {
			throw new ArrayIndexOutOfBoundsException("Index for LIBOR period discretization out of bounds.");
		}
		return liborPeriodDiscretization.getTime(timeIndex);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.LIBORMarketModel#getLiborPeriodIndex(double)
	 */
	@Override
	public int getLiborPeriodIndex(final double time) {
		return liborPeriodDiscretization.getTimeIndex(time);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.LIBORMarketModel#getLiborPeriodDiscretization()
	 */
	@Override
	public TimeDiscretization getLiborPeriodDiscretization() {
		return liborPeriodDiscretization;
	}

	/**
	 * Alternative implementation for the drift. For experimental purposes.
	 *
	 * @param timeIndex
	 * @param componentIndex
	 * @param realizationAtTimeIndex
	 * @param realizationPredictor
	 * @return
	 */
	private RandomVariable getDrift(final MonteCarloProcess process, final int timeIndex, final int componentIndex, final RandomVariable[] realizationAtTimeIndex, final RandomVariable[] realizationPredictor) {

		// Check if this LIBOR is already fixed
		if(process.getTime(timeIndex) >= this.getLiborPeriod(componentIndex)) {
			return null;
		}

		/*
		 * We implemented several different methods to calculate the drift
		 */
		if(driftApproximationMethod == Driftapproximation.PREDICTOR_CORRECTOR && realizationPredictor != null) {
			RandomVariable drift					= getDriftEuler(process, timeIndex, componentIndex, realizationAtTimeIndex);
			final RandomVariable driftEulerWithPredictor	= getDriftEuler(process, timeIndex, componentIndex, realizationPredictor);
			drift = drift.add(driftEulerWithPredictor).div(2.0);

			return drift;
		}
		else if(driftApproximationMethod == Driftapproximation.LINE_INTEGRAL && realizationPredictor != null) {
			return getDriftLineIntegral(process, timeIndex, componentIndex, realizationAtTimeIndex, realizationPredictor);
		}
		else {
			return getDriftEuler(process, timeIndex, componentIndex, realizationAtTimeIndex);
		}
	}

	protected RandomVariable getDriftEuler(final MonteCarloProcess process, final int timeIndex, final int componentIndex, final RandomVariable[] liborVectorStart) {
		// The following is the drift of the LIBOR component
		final double	time					= process.getTime(timeIndex);

		// Initialize to 0.0
		RandomVariable drift = new RandomVariableFromDoubleArray(time, 0.0);

		// Get the start and end of the summation (start is the LIBOR after the current LIBOR component, end is the last LIBOR)
		int firstLiborIndex, lastLiborIndex;
		switch(measure) {
		case SPOT:
			// Spot measure
			firstLiborIndex	= this.getLiborPeriodIndex(time)+1;
			if(firstLiborIndex<0) {
				firstLiborIndex = -firstLiborIndex-1 + 1;
			}
			lastLiborIndex	= componentIndex;
			break;
		case TERMINAL:
		default:
			firstLiborIndex	= componentIndex+1;
			lastLiborIndex 	= liborPeriodDiscretization.getNumberOfTimeSteps()-1;
			break;
		}

		// The sum
		for(int liborIndex = firstLiborIndex; liborIndex<=lastLiborIndex; liborIndex++) {
			final double						periodLength	= liborPeriodDiscretization.getTimeStep(liborIndex);
			RandomVariable		covariance		= covarianceModel.getCovariance(timeIndex, componentIndex, liborIndex, null);
			final RandomVariable libor			= liborVectorStart[liborIndex];
			covariance = covariance.mult(periodLength).mult(libor).discount(libor, periodLength);
			drift = drift.add(covariance);
		}
		if(measure == Measure.TERMINAL) {
			drift = drift.mult(-1.0);
		}

		// Drift adjustment for log-coordinate
		final RandomVariable		variance		= covarianceModel.getCovariance(timeIndex, componentIndex, componentIndex, null);
		drift = drift.addProduct(variance, -0.5);

		return drift;
	}

	private RandomVariable getDriftLineIntegral(final MonteCarloProcess process, final int timeIndex, final int componentIndex, final RandomVariable[] liborVectorStart, final RandomVariable[] liborVectorEnd) {
		// The following is the dirft of the LIBOR component

		final double	time				= process.getTime(timeIndex);

		// Check if this LIBOR is already fixed
		if(process.getTime(timeIndex) >= this.getLiborPeriod(componentIndex)) {
			return null;
		}

		// Initialize to 0.0
		RandomVariable drift = new RandomVariableFromDoubleArray(time, 0.0);

		// Get the start and end of the summation (start is the LIBOR after the current LIBOR component, end is the last LIBOR)
		int firstLiborIndex, lastLiborIndex;
		switch(measure) {
		case SPOT:
			// Spot measure
			firstLiborIndex	= this.getLiborPeriodIndex(time)+1;
			if(firstLiborIndex<0) {
				firstLiborIndex = -firstLiborIndex-1 + 1;
			}
			lastLiborIndex	= componentIndex;
			break;
		case TERMINAL:
		default:
			firstLiborIndex	= componentIndex+1;
			lastLiborIndex 	= liborPeriodDiscretization.getNumberOfTimeSteps()-1;
			break;
		}

		// The sum
		for(int liborIndex = firstLiborIndex; liborIndex<=lastLiborIndex; liborIndex++) {

			final double periodLength = liborPeriodDiscretization.getTimeStep(liborIndex);
			final RandomVariable	covariance	= covarianceModel.getCovariance(timeIndex, componentIndex, liborIndex, null);

			/*
			 * We calculate
			 * driftTerm = covariance * log( (1 + periodLength * liborVectorEnd[liborIndex]) / (1 + periodLength * liborVectorStart[liborIndex]) )
			 *            / log(liborVectorEnd[liborIndex] / liborVectorStart[liborIndex])
			 */

			RandomVariable driftTerm = new RandomVariableFromDoubleArray(1.0);
			driftTerm = driftTerm.accrue(liborVectorEnd[liborIndex], periodLength);
			driftTerm = driftTerm.discount(liborVectorStart[liborIndex], periodLength);
			driftTerm = driftTerm.log();
			driftTerm = driftTerm.mult(covariance);
			driftTerm = driftTerm.div(liborVectorEnd[liborIndex].div(liborVectorStart[liborIndex]).log());

			drift = drift.sub(driftTerm);
		}

		return drift;
	}

	/**
	 * @return Returns the measure.
	 */
	public Measure getMeasure() {
		return measure;
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.LIBORMarketModel#getIntegratedLIBORCovariance()
	 */
	@Override
	public double[][][] getIntegratedLIBORCovariance(TimeDiscretization simulationTimeDiscretization) {
		if(integratedLIBORCovariance != null) {
			return integratedLIBORCovariance;
		}

		final TimeDiscretization liborPeriodDiscretization = getLiborPeriodDiscretization();

		integratedLIBORCovariance = new double[simulationTimeDiscretization.getNumberOfTimeSteps()][liborPeriodDiscretization.getNumberOfTimeSteps()][liborPeriodDiscretization.getNumberOfTimeSteps()];
		for(int componentIndex1 = 0; componentIndex1 < liborPeriodDiscretization.getNumberOfTimeSteps(); componentIndex1++) {
			// Sum the libor cross terms (use symmetry)
			for(int componentIndex2 = componentIndex1; componentIndex2 < liborPeriodDiscretization.getNumberOfTimeSteps(); componentIndex2++) {
				double integratedLIBORCovarianceValue = 0.0;
				for(int timeIndex = 0; timeIndex < simulationTimeDiscretization.getNumberOfTimeSteps(); timeIndex++) {
					final double dt = simulationTimeDiscretization.getTimeStep(timeIndex);
					final RandomVariable[] factorLoadingOfComponent1 = getCovarianceModel().getFactorLoading(simulationTimeDiscretization.getTime(timeIndex), liborPeriodDiscretization.getTime(componentIndex1), null);
					final RandomVariable[] factorLoadingOfComponent2 = getCovarianceModel().getFactorLoading(simulationTimeDiscretization.getTime(timeIndex), liborPeriodDiscretization.getTime(componentIndex2), null);
					for(int factorIndex = 0; factorIndex < getNumberOfFactors(); factorIndex++) {
						integratedLIBORCovarianceValue += factorLoadingOfComponent1[factorIndex].get(0) * factorLoadingOfComponent2[factorIndex].get(0) * dt;
					}
					integratedLIBORCovariance[timeIndex][componentIndex1][componentIndex2] = integratedLIBORCovarianceValue;
				}
			}
		}

		return integratedLIBORCovariance;
	}

	@Override
	public Object clone() {
		return new LIBORMarketModelStandard(liborPeriodDiscretization, forwardRateCurve, covarianceModel);
	}

	/**
	 * @param driftApproximationMethod The driftApproximationMethod to set.
	 */
	public void setDriftApproximationMethod(final Driftapproximation driftApproximationMethod) {
		this.driftApproximationMethod = driftApproximationMethod;
	}

	/**
	 * @param measure The measure to set.
	 */
	public void setMeasure(final Measure measure) {
		this.measure = measure;
	}

	@Override
	public AnalyticModel getAnalyticModel() {
		return curveModel;
	}

	@Override
	public DiscountCurve getDiscountCurve() {
		if(discountCurve == null) {
			final DiscountCurve discountCurveFromForwardCurve = new DiscountCurveFromForwardCurve(getForwardRateCurve());
			return discountCurveFromForwardCurve;
		}

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
	public SwaptionMarketData getSwaptionMarketData() {
		return swaptionMarketData;
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.LIBORMarketModel#getCovarianceModel()
	 */
	@Override
	public LIBORCovarianceModel getCovarianceModel() {
		return covarianceModel;
	}

	/**
	 * @param covarianceModel A covariance model
	 * @return A new <code>LIBORMarketModelStandard</code> using the specified covariance model.
	 */
	@Override
	public LIBORMarketModelStandard getCloneWithModifiedCovarianceModel(final LIBORCovarianceModel covarianceModel) {
		final LIBORMarketModelStandard model = (LIBORMarketModelStandard)this.clone();
		model.covarianceModel = covarianceModel;
		return model;
	}

	@Override
	public LIBORMarketModelStandard getCloneWithModifiedData(final Map<String, Object> dataModified) throws CalculationException {
		TimeDiscretization		liborPeriodDiscretization	= this.liborPeriodDiscretization;
		final AnalyticModel			analyticModel				= curveModel;
		ForwardCurve			forwardRateCurve			= this.forwardRateCurve;
		LIBORCovarianceModel	covarianceModel				= this.covarianceModel;
		SwaptionMarketData		swaptionMarketData			= null;

		if(dataModified.containsKey("liborPeriodDiscretization")) {
			liborPeriodDiscretization = (TimeDiscretization)dataModified.get("liborPeriodDiscretization");
		}
		if(dataModified.containsKey("forwardRateCurve")) {
			forwardRateCurve = (ForwardCurve)dataModified.get("forwardRateCurve");
		}
		if(dataModified.containsKey("forwardRateShift")) {
			throw new RuntimeException("Forward rate shift clone currently disabled.");
		}
		if(dataModified.containsKey("covarianceModel")) {
			covarianceModel = (LIBORCovarianceModel)dataModified.get("covarianceModel");
		}
		if(dataModified.containsKey("swaptionMarketData")) {
			swaptionMarketData = (SwaptionMarketData)dataModified.get("swaptionMarketData");
		}

		if(swaptionMarketData == null) {
			return new LIBORMarketModelStandard(liborPeriodDiscretization, forwardRateCurve, covarianceModel);
		}
		else {
			return new LIBORMarketModelStandard(liborPeriodDiscretization, forwardRateCurve, covarianceModel, swaptionMarketData);

		}
	}

	@Override
	public Map<String, RandomVariable> getModelParameters() {
		// TODO Add implementation
		throw new UnsupportedOperationException();
	}
}

