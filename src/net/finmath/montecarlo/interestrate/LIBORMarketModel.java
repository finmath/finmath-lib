/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2004
 */
package net.finmath.montecarlo.interestrate;

import java.util.ArrayList;
import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.AbstractSwaptionMarketData;
import net.finmath.marketdata.model.curves.ForwardCurveInterface;
import net.finmath.marketdata.products.Swap;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.montecarlo.interestrate.modelplugins.AbstractLIBORCovarianceModel;
import net.finmath.montecarlo.interestrate.modelplugins.AbstractLIBORCovarianceModelParametric;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.montecarlo.interestrate.products.SwaptionAnalyticApproximation;
import net.finmath.montecarlo.model.AbstractModel;
import net.finmath.stochastic.ImmutableRandomVariableInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * Implements a basic LIBOR market model with some drift approximation methods.
 * 
 * The class implements different measure(drift) / numeraire pairs (terminal and spot).
 * 
 * The model uses an <code>AbstractLIBORCovarianceModel</code> as a covariance model, which has the ability to calibrate to swaptions.
 * 
 * @see AbstractLIBORCovarianceModel
 * 
 * @author Christian Fries
 * @version 1.0
 */
public class LIBORMarketModel extends AbstractModel implements LIBORMarketModelInterface {

	public enum Driftapproximation	{ EULER, LINE_INTEGRAL, PREDICTOR_CORRECTOR };
	public enum Measure				{ SPOT, TERMINAL };

	private TimeDiscretizationInterface		liborPeriodDiscretization;
	private ForwardCurveInterface			forwardRateCurve;
	private AbstractLIBORCovarianceModel	covarianceModel;

	private AbstractSwaptionMarketData		swaptionMarketData;

	private Driftapproximation	driftApproximationMethod	= Driftapproximation.EULER;
	private Measure				measure						= Measure.SPOT;

	// This is a cache of the integrated covariance.
    private double[][][]	integratedLIBORCovariance;

    public static class CalibrationItem {
		public AbstractLIBORMonteCarloProduct		calibrationProduct;
		public double								calibrationTargetValue;
		public double								calibrationWeight;

		public CalibrationItem(AbstractLIBORMonteCarloProduct calibrationProduct, double calibrationTargetValue, double calibrationWeight) {
			super();
			this.calibrationProduct		= calibrationProduct;
			this.calibrationTargetValue	= calibrationTargetValue;
			this.calibrationWeight		= calibrationWeight;
		}
    }

    /**
	 * Creates a LIBOR Market Model for given covariance.
	 * 
	 * @param liborPeriodDiscretization The discretization of the interest rate curve into forward rates (tenor structure).
	 * @param forwardRateCurve The initial values for the forward rates.
	 * @param covarianceModel The covariance model to use.
	 */
	public LIBORMarketModel(
			TimeDiscretizationInterface		liborPeriodDiscretization,
			ForwardCurveInterface			forwardRateCurve,
			AbstractLIBORCovarianceModel	covarianceModel
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
	 * @param covarianceModel The covariance model to use.
	 * @param swaptionMarketData The set of swaption values to calibrate to.
	 * @throws CalculationException 
	 */
	public LIBORMarketModel(
			TimeDiscretizationInterface			liborPeriodDiscretization,
			ForwardCurveInterface				forwardRateCurve,
			AbstractLIBORCovarianceModel		covarianceModel,
			AbstractSwaptionMarketData			swaptionMarketData
	) throws CalculationException {
			this(liborPeriodDiscretization, forwardRateCurve, covarianceModel, getCalibrationData(liborPeriodDiscretization, forwardRateCurve, swaptionMarketData));
	}



	/**
	 * Creates a LIBOR Market Model for given covariance.
	 * 
	 * @param liborPeriodDiscretization The discretization of the interest rate curve into forward rates (tenor structure).
	 * @param forwardRateCurve The initial values for the forward rates.
	 * @param covarianceModel The covariance model to use.
	 * @param calibrationItems The vector of calibration items (a union of a product, target value and weight) for the objective function sum weight(i) * (modelValue(i)-targetValue(i).
	 * @throws CalculationException 
	 */
	public LIBORMarketModel(
			TimeDiscretizationInterface				liborPeriodDiscretization,
			ForwardCurveInterface					forwardRateCurve,
			AbstractLIBORCovarianceModel			covarianceModel,
			CalibrationItem[]						calibrationItems
	) throws CalculationException {
		this.liborPeriodDiscretization	= liborPeriodDiscretization;

		double[] times = new double[liborPeriodDiscretization.getNumberOfTimeSteps()];
		for(int i=0; i<times.length; i++) times[i] = liborPeriodDiscretization.getTime(i);

		AbstractLIBORCovarianceModelParametric covarianceModelParametric = null;
		try {
			covarianceModelParametric = (AbstractLIBORCovarianceModelParametric)covarianceModel;
		}
		catch(Exception e) {
			throw new ClassCastException("Calibration is currently restricted to parametric covariance models (AbstractLIBORCovarianceModelParametric).");
		}

		this.forwardRateCurve	= forwardRateCurve;

		// @TODO Should be more elegant. Convert array for constructor
		AbstractLIBORMonteCarloProduct[]	calibrationProducts		= new AbstractLIBORMonteCarloProduct[calibrationItems.length];
		double[]							calibrationTargetValues	= new double[calibrationItems.length];
		double[]							calibrationWeights		= new double[calibrationItems.length];
		for(int i=0; i<calibrationTargetValues.length; i++) {
			calibrationProducts[i]		= calibrationItems[i].calibrationProduct;
			calibrationTargetValues[i]	= calibrationItems[i].calibrationTargetValue;
			calibrationWeights[i]		= calibrationItems[i].calibrationWeight;
		}

		this.covarianceModel    = covarianceModelParametric.getCloneCalibrated(this, calibrationProducts, calibrationTargetValues, calibrationWeights);
	}

	private static CalibrationItem[] getCalibrationData(TimeDiscretizationInterface liborPeriodDiscretization, ForwardCurveInterface forwardCurve, AbstractSwaptionMarketData swaptionMarketData) {
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

//	            System.out.println("Swaption " + exerciseDate + "/" + numberOfPeriods*swapPeriodLength);

				// Swaptions swap rate
				TimeDiscretizationInterface swapTenor = new TimeDiscretization(swapTenorTimes);
				double swaprate = Swap.getForwardSwapRate(swapTenor, swapTenor, forwardCurve, null);

				// Set swap rates for each period
				double[] swaprates        = new double[numberOfPeriods];
				for(int periodStartIndex=0; periodStartIndex<numberOfPeriods; periodStartIndex++) {
					swaprates[periodStartIndex] = swaprate;
				}

				SwaptionAnalyticApproximation swaptionAnalytic = new SwaptionAnalyticApproximation(swaprate, swapTenorTimes, SwaptionAnalyticApproximation.ValueUnit.VOLATILITY);

				double impliedVolatility = swaptionMarketData.getVolatility(exerciseDate, swapLength, swaptionMarketData.getSwapPeriodLength(), swaprate);
				
				calibrationItems.add(new CalibrationItem(swaptionAnalytic,
						impliedVolatility,
						1.0
						));
			}
		}

		return calibrationItems.toArray(new CalibrationItem[0]);
	}

	/**
	 * @param time
	 * @return The numeraire at the specified time as <code>RandomVariable</code>
	 * @throws CalculationException 
	 */
	public RandomVariableInterface getNumeraire(double time) throws CalculationException {
		return getNumeraire(getTimeIndex(time));
	}

	public RandomVariableInterface[] getInitialState() {
		double[] liborInitialValues = new double[liborPeriodDiscretization.getNumberOfTimeSteps()];
		for(int timeIndex=0; timeIndex<liborPeriodDiscretization.getNumberOfTimeSteps(); timeIndex++) {
			liborInitialValues[timeIndex] = forwardRateCurve.getForward(null, liborPeriodDiscretization.getTime(timeIndex));
		}

		RandomVariableInterface[] initialValueRandomVariable = new RandomVariableInterface[getNumberOfComponents()];
		for(int componentIndex=0; componentIndex<getNumberOfComponents(); componentIndex++) {
			initialValueRandomVariable[componentIndex] = new RandomVariable(0.0, Math.log(liborInitialValues[componentIndex]));
		}
		return initialValueRandomVariable;
	}

	/**
	 * Return the complete vector of the drift for the time index timeIndex, given that current state is realizationAtTimeIndex.
	 * Note: The random variable returned is a defensive copy and may be modified.
	 * The drift will be zero for rates being already fixed.
	 * 
	 * @see LIBORMarketModel#getNumeraire(int) The calculation of the drift is consistent with the calculation of the numeraire in <code>getNumeraire</code>.
	 * 
	 * @param timeIndex Time index <i>i</i> for which the drift should be returned <i>&mu;(t<sub>i</sub>)</i>.
	 * @param realizationAtTimeIndex Time current forward rate vector at time index <i>i</i> which should be used in the calculation.
	 * @return The drift vector &mu;(t<sub>i</sub>) as <code>RandomVariable[]</code>
	 * @throws CalculationException 
	 */
    public RandomVariableInterface[] getDrift(int timeIndex, ImmutableRandomVariableInterface[] realizationAtTimeIndex, ImmutableRandomVariableInterface[] realizationPredictor) {
		double	time				= getTime(timeIndex);
		int		firstLiborIndex		= this.getLiborPeriodIndex(time)+1;
		if(firstLiborIndex<0) firstLiborIndex = -firstLiborIndex-1 + 1;

		// Allocate drift vector and initialize to zero (will be used to sum up drift components)
    	RandomVariableInterface[]	drift = new RandomVariableInterface[getNumberOfComponents()];
    	RandomVariableInterface[][]	covarianceFactorSums	= new RandomVariableInterface[getNumberOfComponents()][getNumberOfFactors()];
    	for(int componentIndex=0; componentIndex<getNumberOfComponents(); componentIndex++) {
    		drift[componentIndex] = new RandomVariable(0.0, 0.0);
    	}
    	
    	// Calculate drift for the component componentIndex (starting at firstLiborIndex, others are zero)
    	for(int componentIndex=firstLiborIndex; componentIndex<getNumberOfComponents(); componentIndex++) {
			double						periodLength		= liborPeriodDiscretization.getTimeStep(componentIndex);
    		ImmutableRandomVariableInterface libor = realizationAtTimeIndex[componentIndex];
    		RandomVariableInterface oneStepMeasureTransform = libor.getMutableCopy().discount(libor, periodLength).mult(periodLength);

    		for(int factorIndex=0; factorIndex<getNumberOfFactors(); factorIndex++) {
    			RandomVariableInterface		covarianceFactor	= covarianceModel.getFactorLoading(timeIndex, factorIndex, componentIndex);
    			covarianceFactor.mult(oneStepMeasureTransform);
    			covarianceFactorSums[componentIndex][factorIndex] = covarianceFactor;
    			if(componentIndex > firstLiborIndex)
    				covarianceFactorSums[componentIndex][factorIndex].add(covarianceFactorSums[componentIndex-1][factorIndex]);
        	}
        }
    	// Above is the drift for the spot measure: a simple conversion makes it the drift of the terminal measure.
		if(measure == Measure.TERMINAL) {
	        for(int componentIndex=firstLiborIndex; componentIndex<getNumberOfComponents(); componentIndex++) {
	       		for(int factorIndex=0; factorIndex<getNumberOfFactors(); factorIndex++) {
	       			covarianceFactorSums[componentIndex][factorIndex].sub(covarianceFactorSums[getNumberOfComponents()-1][factorIndex]);
	        	}
	        }
		}
		for(int componentIndex=firstLiborIndex; componentIndex<getNumberOfComponents(); componentIndex++) {
       		for(int factorIndex=0; factorIndex<getNumberOfFactors(); factorIndex++) {
        		drift[componentIndex].addProduct(covarianceFactorSums[componentIndex][factorIndex], getFactorLoading(timeIndex, factorIndex, componentIndex));
        	}
        }

		// Drift adjustment for log-coordinate in each component
		for(int componentIndex=0; componentIndex<getNumberOfComponents(); componentIndex++) {
    		RandomVariableInterface		variance		= covarianceModel.getCovariance(timeIndex, componentIndex, componentIndex);
    		drift[componentIndex].addProduct(variance, -0.5);
        }

		return drift;
	}

    public RandomVariableInterface getDrift(int timeIndex, int componentIndex, ImmutableRandomVariableInterface[] realizationAtTimeIndex, ImmutableRandomVariableInterface[] realizationPredictor) {

		// Check if this LIBOR is already fixed
		if(getTime(timeIndex) >= this.getLiborPeriod(componentIndex)) {
			return null;
		}

		/*
		 * We implemented several different methods to calculate the drift
		 */
		if(driftApproximationMethod == Driftapproximation.PREDICTOR_CORRECTOR && realizationPredictor != null) {
			RandomVariableInterface drift					= getDriftEuler(timeIndex, componentIndex, realizationAtTimeIndex);
			RandomVariableInterface driftEulerWithPredictor	= getDriftEuler(timeIndex, componentIndex, realizationPredictor);
			drift.add(driftEulerWithPredictor).div(2.0);

			return drift;
		}
		else if(driftApproximationMethod == Driftapproximation.LINE_INTEGRAL && realizationPredictor != null) {
			return getDriftLineIntegral(timeIndex, componentIndex, realizationAtTimeIndex, realizationPredictor);		    
		}
		else {
			return getDriftEuler(timeIndex, componentIndex, realizationAtTimeIndex);
		}		
	}

	public	RandomVariableInterface	getFactorLoading(int timeIndex, int factor, int componentIndex)
	{
		return covarianceModel.getFactorLoading(timeIndex, factor, componentIndex);
	}

	@Override
	public void applyStateSpaceTransform(RandomVariableInterface randomVariable) {
		randomVariable.exp();
	}

	
	/**
	 * @return Returns the driftApproximationMethod.
	 */
	public Driftapproximation getDriftApproximationMethod() {
		return driftApproximationMethod;
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.LIBORMarketModelInterface#getLIBOR(int, int)
	 */
	@Override
	public RandomVariableInterface getLIBOR(int timeIndex, int liborIndex) throws CalculationException
	{
		// This method is just a psynonym - call getProcessValue of super class
		return getProcessValue(timeIndex, liborIndex);
	}

	/**
	 * This method is just a synonym to getNumberOfLibors
	 * @return The number of components
	 */
	public int getNumberOfComponents() {
		return getNumberOfLibors();
	}


	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.LIBORMarketModelInterface#getNumberOfLibors()
	 */
	@Override
	public int getNumberOfLibors()
	{
		// This is just a synonym to number of components
		return liborPeriodDiscretization.getNumberOfTimeSteps();
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.LIBORMarketModelInterface#getLiborPeriod(int)
	 */
	@Override
	public double getLiborPeriod(int timeIndex) {
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

	protected RandomVariableInterface getDriftEuler(int timeIndex, int componentIndex, ImmutableRandomVariableInterface[] liborVectorStart) {
		// The following is the drift of the LIBOR component
		double	time					= getTime(timeIndex);

		// Initialize to 0.0
		RandomVariableInterface drift = new RandomVariable(time, 0.0);

		// Get the start and end of the summation (start is the LIBOR after the current LIBOR component, end is the last LIBOR)
		int firstLiborIndex, lastLiborIndex;
		switch(measure) {
		case SPOT:
			// Spot measure
			firstLiborIndex	= this.getLiborPeriodIndex(time)+1;
			if(firstLiborIndex<0) firstLiborIndex = -firstLiborIndex-1 + 1;
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
			double						periodLength	= liborPeriodDiscretization.getTimeStep(liborIndex);
			RandomVariableInterface		covariance		= covarianceModel.getCovariance(timeIndex, componentIndex, liborIndex);
			ImmutableRandomVariableInterface		libor			= liborVectorStart[liborIndex];
			covariance.mult(periodLength).mult(libor).discount(libor, periodLength);
			drift.add(covariance);
		}
		if(measure == Measure.TERMINAL) {
			drift.mult(-1.0);
		}

		// Drift adjustment for log-coordinate
		RandomVariableInterface		variance		= covarianceModel.getCovariance(timeIndex, componentIndex, componentIndex);
		drift.addProduct(variance, -0.5);

		return drift;
	}

	private RandomVariableInterface getDriftLineIntegral(int timeIndex, int componentIndex, ImmutableRandomVariableInterface[] liborVectorStart, ImmutableRandomVariableInterface[] liborVectorEnd) {
		// The following is the dirft of the LIBOR component

		int		numberOfPaths		= getNumberOfPaths();

		double	time				= getTime(timeIndex);

		// Check if this LIBOR is already fixed
		if(getTime(timeIndex) >= this.getLiborPeriod(componentIndex)) {
			return null;
		}

		// Initialize to 0.0
		RandomVariable drift = new RandomVariable(time, 0.0);

		// Get the start and end of the summation (start is the LIBOR after the current LIBOR component, end is the last LIBOR)
		int firstLiborIndex, lastLiborIndex;
		switch(measure) {
		case SPOT:
			// Spot measure
			firstLiborIndex	= this.getLiborPeriodIndex(time)+1;
			if(firstLiborIndex<0) firstLiborIndex = -firstLiborIndex-1 + 1;
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

			double periodLength = liborPeriodDiscretization.getTimeStep(liborIndex);
			RandomVariableInterface	covariance	= covarianceModel.getCovariance(timeIndex, componentIndex, liborIndex);

			double[] driftValues = drift.getRealizations(numberOfPaths);
			for(int path=0; path<numberOfPaths; path++) {
				driftValues[path] -= covariance.get(path) * Math.log(
						(1 + periodLength * liborVectorEnd[liborIndex].get(path))
						/ (1 + periodLength * liborVectorStart[liborIndex].get(path))
				)
				/ Math.log(liborVectorEnd[liborIndex].get(path) / liborVectorStart[liborIndex].get(path));
			}
		}

		return drift;
	}

	/**
	 * @return Returns the measure.
	 */
	public Measure getMeasure() {
		return measure;
	}

	/**
	 * Return the numeraire at a given time index.
	 * Note: The random variable returned is a defensive copy and may be modified.
	 * <b>
	 * Note: This method does not interpolate the numeraire if it is requested outside the liborPeriodDiscretization
	 * </b>
	 * 
	 * @param timeIndex Time index <i>i</i> for which the numeraire should be returned <i>N(t<sub>i</sub>)</i>.
	 * @return The numeraire at the specified time as <code>RandomVariable</code>
	 * @throws CalculationException 
	 */
	public RandomVariableInterface getNumeraire(int timeIndex) throws CalculationException
	{
		double time = getTime(timeIndex);

		// Get the start of the product
		int firstLiborIndex		= this.getLiborPeriodIndex(time);
		//		while(liborPeriodDiscretization[firstLiborIndex] < time) firstLiborIndex++;

		// Get the end of the product
		int lastLiborIndex 	= liborPeriodDiscretization.getNumberOfTimeSteps()-1;

		if(measure == Measure.SPOT) {
			// Spot measure
			firstLiborIndex	= 0;
			lastLiborIndex	= this.getLiborPeriodIndex(time)-1;
			if(lastLiborIndex < -1) {
				System.out.println("Interpolation on Numeraire not supported.");
			}
		}

		/**
		 * Calculation of the numeraire
		 */

		// Initialize to 1.0
		RandomVariable numeraire = new RandomVariable(time, 1.0);

		// The product 
		for(int liborIndex = firstLiborIndex; liborIndex<=lastLiborIndex; liborIndex++) {
			RandomVariableInterface libor = getLIBOR(getTimeIndex(Math.min(time,liborPeriodDiscretization.getTime(liborIndex))), liborIndex);

			double periodLength = liborPeriodDiscretization.getTimeStep(liborIndex);

			if(measure == Measure.SPOT) {
				numeraire.accrue(libor, periodLength);
			}
			else {
				numeraire.discount(libor, periodLength);
			}
		}
		return numeraire;
	}

   public synchronized double[][][] getIntegratedLIBORCovariance() {
    	if(integratedLIBORCovariance != null) return integratedLIBORCovariance;

    	TimeDiscretizationInterface liborPeriodDiscretization = getLiborPeriodDiscretization();
    	TimeDiscretizationInterface simulationTimeDiscretization = getTimeDiscretization();

    	if(integratedLIBORCovariance != null) integratedLIBORCovariance = new double[liborPeriodDiscretization.getNumberOfTimeSteps()][liborPeriodDiscretization.getNumberOfTimeSteps()][simulationTimeDiscretization.getNumberOfTimeSteps()];
    	integratedLIBORCovariance = new double[liborPeriodDiscretization.getNumberOfTimeSteps()][liborPeriodDiscretization.getNumberOfTimeSteps()][simulationTimeDiscretization.getNumberOfTimeSteps()];
    	for(int componentIndex1 = 0; componentIndex1 < liborPeriodDiscretization.getNumberOfTimeSteps(); componentIndex1++) {
            // Sum the libor cross terms (use symmetry)
            for(int componentIndex2 = componentIndex1; componentIndex2 < liborPeriodDiscretization.getNumberOfTimeSteps(); componentIndex2++) {
            	double integratedLIBORCovarianceValue = 0.0;
                for(int timeIndex = 0; timeIndex < simulationTimeDiscretization.getNumberOfTimeSteps(); timeIndex++) {
                    double dt = getTime(timeIndex+1) - getTime(timeIndex);
                    for(int factorIndex = 0; factorIndex < getNumberOfFactors(); factorIndex++) {
                    	integratedLIBORCovarianceValue += getCovarianceModel().getFactorLoading(timeIndex, factorIndex, componentIndex1).get(0) * getCovarianceModel().getFactorLoading(timeIndex, factorIndex, componentIndex2).get(0) * dt;
                    }
                	integratedLIBORCovariance[componentIndex1][componentIndex2][timeIndex] = integratedLIBORCovarianceValue;
                }
            }
        }
        
        return integratedLIBORCovariance;
    }
	
	public Object clone() {
		return new LIBORMarketModel(liborPeriodDiscretization, forwardRateCurve, covarianceModel);
	}

	/**
	 * @param driftApproximationMethod The driftApproximationMethod to set.
	 */
	public void setDriftApproximationMethod(Driftapproximation driftApproximationMethod) {
		this.driftApproximationMethod = driftApproximationMethod;
	}

	/**
	 * @param measure The measure to set.
	 */
	public void setMeasure(Measure measure) {
		this.measure = measure;
	}
	
	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.LIBORMarketModelInterface#getForwardRateCurve()
	 */
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
	 * @param covarianceModel the covarianceModel to set
	 * @deprecated
	 */
	public void setCovarianceModel(AbstractLIBORCovarianceModel covarianceModel) {
		this.covarianceModel = covarianceModel;
	}

	/**
	 * @param covarianceModel A covariance model
	 * @return A new <code>LIBORMarketModel</code> using the specified covariance model.
	 */
	public LIBORMarketModel getCloneWithModifiedCovarianceModel(AbstractLIBORCovarianceModel covarianceModel) {
		LIBORMarketModel model = (LIBORMarketModel)this.clone();
		model.setCovarianceModel(covarianceModel);
		return model;
	}

    public LIBORMarketModel getCloneWithModifiedData(Map<String, Object> dataModified) throws CalculationException {
		TimeDiscretizationInterface		liborPeriodDiscretization	= this.liborPeriodDiscretization;
		ForwardCurveInterface			forwardRateCurve			= this.forwardRateCurve;
		AbstractLIBORCovarianceModel	covarianceModel				= this.covarianceModel;
		AbstractSwaptionMarketData		swaptionMarketData			= null;

		if(dataModified.containsKey("liborPeriodDiscretization")) {
    		liborPeriodDiscretization = (TimeDiscretizationInterface)dataModified.get("liborPeriodDiscretization");
    	}
    	if(dataModified.containsKey("forwardRateCurve")) {
    		forwardRateCurve = (ForwardCurveInterface)dataModified.get("forwardRateCurve");
    	}
    	if(dataModified.containsKey("forwardRateShift")) {
    		forwardRateCurve = (ForwardCurveInterface)dataModified.get("forwardRateCurve");
//    		throw new RuntimeException("Forward rate shift clone currently disabled.");
    	}
    	if(dataModified.containsKey("covarianceModel")) {
    		covarianceModel = (AbstractLIBORCovarianceModel)dataModified.get("covarianceModel");
    	}
    	if(dataModified.containsKey("swaptionMarketData")) {
    		swaptionMarketData = (AbstractSwaptionMarketData)dataModified.get("covarianceModel");
    	}
    	
    	if(swaptionMarketData == null) {
    		return new LIBORMarketModel(liborPeriodDiscretization, forwardRateCurve, covarianceModel);
    	}
    	else {
    		return new LIBORMarketModel(liborPeriodDiscretization, forwardRateCurve, covarianceModel, swaptionMarketData);
    		
    	}
    }
}


