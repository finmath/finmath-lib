package net.finmath.marketdata.model.curves;


import java.io.Serializable;
import java.util.Date;

import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.ZoneId;

import net.finmath.marketdata.model.AnalyticModelInterface;

/**
 * Hazard Curve
 * 
 * @author Alessandro Gnoatto
 */
public class HazardCurve extends Curve implements Serializable, HazardCurveInterface{


	private static final long serialVersionUID = 8538289748469149026L;
	
	/**
	 * Create an empty discount curve using default interpolation and extrapolation methods.
	 * 
	 * @param name The name of this hazard curve.
	 */
	private HazardCurve(String name){
		super(name,null,InterpolationMethod.LINEAR,ExtrapolationMethod.CONSTANT,InterpolationEntity.LOG_OF_VALUE_PER_TIME);
	}
	
	/**
	 * Create an empty hazard curve using given interpolation and extrapolation methods.
	 *
	 * @param name The name of this hazard curve.
	 * @param interpolationMethod The interpolation method used for the curve.
	 * @param extrapolationMethod The extrapolation method used for the curve.
	 * @param interpolationEntity The entity interpolated/extrapolated.
	 */
	private HazardCurve(String name, InterpolationMethod interpolationMethod,
			ExtrapolationMethod extrapolationMethod, InterpolationEntity interpolationEntity){

		super(name, null, interpolationMethod, extrapolationMethod, interpolationEntity);
	}
	
	/**
	 * Create an empty hazard curve using given interpolation and extrapolation methods.
	 *
	 * @param name The name of this hazard curve.
	 * @param referenceDate The reference date for this curve, i.e., the date which defined t=0.
	 * @param interpolationMethod The interpolation method used for the curve.
	 * @param extrapolationMethod The extrapolation method used for the curve.
	 * @param interpolationEntity The entity interpolated/extrapolated.
	 */
	private HazardCurve(String name, LocalDate referenceDate, InterpolationMethod interpolationMethod,
			ExtrapolationMethod extrapolationMethod, InterpolationEntity interpolationEntity){

		super(name, referenceDate, interpolationMethod, extrapolationMethod, interpolationEntity);
	}
	
	
	/**
	 * Create a hazard curve from given times and given survival probabilities using given interpolation and extrapolation methods.
	 *
	 * @param name The name of this hazard curve.
	 * @param referenceDate The reference date for this curve, i.e., the date which defined t=0.
	 * @param times Array of times as doubles.
	 * @param givenSurvivalProbabilities Array of corresponding survival probabilities.
	 * @param isParameter Array of booleans specifying whether this point is served "as as parameter", e.g., whether it is calibrates (e.g. using CalibratedCurves).
	 * @param interpolationMethod The interpolation method used for the curve.
	 * @param extrapolationMethod The extrapolation method used for the curve.
	 * @param interpolationEntity The entity interpolated/extrapolated.
	 * @return A new hazard curve object.
	 */
	public static HazardCurve createHazardCurveFromSurvivalProbabilities(
			String name, LocalDate referenceDate,
			double[] times, double[] givenSurvivalProbabilities, boolean[] isParameter,
			InterpolationMethod interpolationMethod, ExtrapolationMethod extrapolationMethod, InterpolationEntity interpolationEntity) {
		
		//We check that all input survival probabilities are in the range [0,1]
		for( int i = 0; i < givenSurvivalProbabilities.length; i++){
			if(givenSurvivalProbabilities[i] < 0 || givenSurvivalProbabilities[i] > 1)
				throw new IllegalArgumentException("Survival Probabilities must be between 0 and 1");
		}
		
		HazardCurve survivalProbabilities = new HazardCurve(name, referenceDate, interpolationMethod, extrapolationMethod, interpolationEntity);

		for(int timeIndex=0; timeIndex<times.length;timeIndex++) {
			survivalProbabilities.addSurvivalProbability(times[timeIndex], givenSurvivalProbabilities[timeIndex], isParameter != null && isParameter[timeIndex]);
		}

		return survivalProbabilities;
	}
	
	/**
	 * Create a hazard curve from given times and given survival probabilities using given interpolation and extrapolation methods.
	 *
	 * @param name The name of this hazard curve.
	 * @param times Array of times as doubles.
	 * @param givenSurvivalProbabilities Array of corresponding survival probabilities.
	 * @param isParameter Array of booleans specifying whether this point is served "as as parameter", e.g., whether it is calibrates (e.g. using CalibratedCurves).
	 * @param interpolationMethod The interpolation method used for the curve.
	 * @param extrapolationMethod The extrapolation method used for the curve.
	 * @param interpolationEntity The entity interpolated/extrapolated.
	 * @return A new hazard curve object.
	 */
	public static HazardCurve createHazardCurveFromSurvivalProbabilities(
			String name,
			double[] times,
			double[] givenSurvivalProbabilities,
			boolean[] isParameter,
			InterpolationMethod interpolationMethod, ExtrapolationMethod extrapolationMethod, InterpolationEntity interpolationEntity) {
		return createHazardCurveFromSurvivalProbabilities(name, null, times, givenSurvivalProbabilities, isParameter, interpolationMethod, extrapolationMethod, interpolationEntity);
	}
	
	/**
	 * Create a hazard curve from given times and given survival probabilities using given interpolation and extrapolation methods.
	 *
	 * @param name The name of this hazard curve.
	 * @param times Array of times as doubles.
	 * @param givenSurvivalProbabilities Array of corresponding survival probabilities.
	 * @param interpolationMethod The interpolation method used for the curve.
	 * @param extrapolationMethod The extrapolation method used for the curve.
	 * @param interpolationEntity The entity interpolated/extrapolated.
	 * @return A new discount factor object.
	 */
	public static HazardCurve createHazardCurveFromSurvivalProbabilities(
			String name,
			double[] times,
			double[] givenSurvivalProbabilities,
			InterpolationMethod interpolationMethod, ExtrapolationMethod extrapolationMethod, InterpolationEntity interpolationEntity) {
		
		boolean[] isParameter = new boolean[times.length];
		
		for(int timeIndex=0; timeIndex<times.length;timeIndex++) {
			isParameter[timeIndex] = times[timeIndex] > 0;
		}
		
		return createHazardCurveFromSurvivalProbabilities(name, times, givenSurvivalProbabilities, isParameter, interpolationMethod, extrapolationMethod, interpolationEntity);
	}
	
	/**
	 * Create a hazard curve from given times and given discount factors using default interpolation and extrapolation methods.
	 * 
	 * @param name The name of this hazard curve.
	 * @param times Array of times as doubles.
	 * @param givenSurvivalProbabilities Array of corresponding survival probabilities.
	 * @return A new discount factor object.
	 */
	public static HazardCurve createHazardCurveFromSurvivalProbabilities(String name, double[] times, double[] givenSurvivalProbabilities){
		HazardCurve survivalProbabilities = new HazardCurve(name);
		
		for(int timeIndex=0; timeIndex<times.length;timeIndex++) {
			survivalProbabilities.addSurvivalProbability(times[timeIndex], givenSurvivalProbabilities[timeIndex], times[timeIndex] > 0);
		}

		return survivalProbabilities;
	}
	
	/**
	 * Create a hazard curve from given times and given hazard rates using given interpolation and extrapolation methods.
	 * The discount factor is determined by 
	 * <code>
	 * 		givenSurvivalProbabilities[timeIndex] = givenSurvivalProbabilities[timeIndex-1] * Math.exp(- givenHazardRates[timeIndex] * (times[timeIndex]-times[timeIndex-1]));
	 * </code>
	 *
	 * @param name The name of this hazard curve.
	 * @param referenceDate The reference date for this curve, i.e., the date which defined t=0.
	 * @param times Array of times as doubles.
	 * @param givenHazardRates Array of corresponding hazard rates.
	 * @param isParameter Array of booleans specifying whether this point is served "as as parameter", e.g., whether it is calibrates (e.g. using CalibratedCurves).
	 * @param interpolationMethod The interpolation method used for the curve.
	 * @param extrapolationMethod The extrapolation method used for the curve.
	 * @param interpolationEntity The entity interpolated/extrapolated.
	 * @return A new discount factor object.
	 */
	public static HazardCurve createHazardCurveFromHazardRate(
			String name, LocalDate referenceDate,
			double[] times, double[] givenHazardRates, boolean[] isParameter,
			InterpolationMethod interpolationMethod, ExtrapolationMethod extrapolationMethod, InterpolationEntity interpolationEntity){
		
		double[] givenSurvivalProbabilities = new double[givenHazardRates.length];
		
		if(givenHazardRates[0]<0)
			throw new IllegalArgumentException("First hazard rate is not positive");
		
		//initialize the term structure
		givenSurvivalProbabilities[0] = Math.exp(- givenHazardRates[0] * times[0]);
				
		/*
		 * Construct the hazard curve by numerically integrating the hazard rates.
		 * At each step check if the input hazard rate is positive.
		 */
		for(int timeIndex=1; timeIndex<times.length;timeIndex++) {
			
			if(givenHazardRates[timeIndex]<0)
				throw new IllegalArgumentException("The " + timeIndex + "-th hazard rate is not positive");
			
			givenSurvivalProbabilities[timeIndex] = givenSurvivalProbabilities[timeIndex-1] * Math.exp(- givenHazardRates[timeIndex] * (times[timeIndex]-times[timeIndex-1]));
		}
		
		return createHazardCurveFromSurvivalProbabilities(name, referenceDate, times, givenSurvivalProbabilities, isParameter, interpolationMethod, extrapolationMethod, interpolationEntity);
	}
		
	/**
	 * Create a hazard curve from given times and given zero rates using given interpolation and extrapolation methods.
	 * The discount factor is determined by 
	 * <code>
	 * 		givenSurvivalProbabilities[timeIndex] = givenSurvivalProbabilities[timeIndex-1] * Math.exp(- givenHazardRates[timeIndex] * (times[timeIndex]-times[timeIndex-1]));
	 * </code>
	 *
	 * @param name The name of this discount curve.
	 * @param referenceDate The reference date for this curve, i.e., the date which defined t=0.
	 * @param times Array of times as doubles.
	 * @param givenHazardRates Array of corresponding survival probabilities.
	 * @param isParameter Array of booleans specifying whether this point is served "as as parameter", e.g., whether it is calibrates (e.g. using CalibratedCurves).
	 * @param interpolationMethod The interpolation method used for the curve.
	 * @param extrapolationMethod The extrapolation method used for the curve.
	 * @param interpolationEntity The entity interpolated/extrapolated.
	 * @return A new discount factor object.
	 */
	public static HazardCurve createHazardCurveFromHazardRate(
			String name, Date referenceDate,
			double[] times, double[] givenHazardRates, boolean[] isParameter,
			InterpolationMethod interpolationMethod, ExtrapolationMethod extrapolationMethod, InterpolationEntity interpolationEntity) {
		
		LocalDate referenceDataAsLocalDate = Instant.ofEpochMilli(referenceDate.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
		return createHazardCurveFromHazardRate(name, referenceDataAsLocalDate, times, givenHazardRates, isParameter, interpolationMethod, extrapolationMethod, interpolationEntity);
	}
		
	/**
	 * Create a discount curve from given times and given zero rates using given interpolation and extrapolation methods.
	 * The discount factor is determined by 
	 * <code>
	 * 		givenSurvivalProbabilities[timeIndex] = givenSurvivalProbabilities[timeIndex-1] * Math.exp(- givenHazardRates[timeIndex] * (times[timeIndex]-times[timeIndex-1]));
	 * </code>
	 *
	 * @param name The name of this discount curve.
	 * @param referenceDate The reference date for this curve, i.e., the date which defined t=0.
	 * @param times Array of times as doubles.
	 * @param givenHazardRates Array of corresponding zero rates.
	 * @param interpolationMethod The interpolation method used for the curve.
	 * @param extrapolationMethod The extrapolation method used for the curve.
	 * @param interpolationEntity The entity interpolated/extrapolated.
	 * @return A new discount factor object.
	 */
	public static HazardCurve createHazardCurveFromHazardRate(
			String name, LocalDate referenceDate,
			double[] times, double[] givenHazardRates,
			InterpolationMethod interpolationMethod, ExtrapolationMethod extrapolationMethod, InterpolationEntity interpolationEntity){
		
		boolean[] isParameter = new boolean[givenHazardRates.length];
		
		double[] givenSurvivalProbabilities = new double[givenHazardRates.length];
		
		if(givenHazardRates[0]<0)
			throw new IllegalArgumentException("First hazard rate is not positive");
		
		//initialize the term structure
		givenSurvivalProbabilities[0] = Math.exp(- givenHazardRates[0] * times[0]);
				
		/*
		 * Construct the hazard curve by numerically integrating the hazard rates.
		 * At each step check if the input hazard rate is positive.
		 */
		for(int timeIndex=1; timeIndex<times.length;timeIndex++) {
			
			if(givenHazardRates[timeIndex]<0)
				throw new IllegalArgumentException("The " + timeIndex + "-th hazard rate is not positive");
			
			givenSurvivalProbabilities[timeIndex] = givenSurvivalProbabilities[timeIndex-1] * Math.exp(- givenHazardRates[timeIndex] * (times[timeIndex]-times[timeIndex-1]));
		}
		
		
		return createHazardCurveFromSurvivalProbabilities(name, referenceDate, times, givenSurvivalProbabilities, isParameter, interpolationMethod, extrapolationMethod, interpolationEntity);
	}
	
	/**
	 * Create a discount curve from given times and given zero rates using default interpolation and extrapolation methods.
	 * The discount factor is determined by 
	 * <code>
	 * 		givenSurvivalProbabilities[timeIndex] = givenSurvivalProbabilities[timeIndex-1] * Math.exp(- givenHazardRates[timeIndex] * (times[timeIndex]-times[timeIndex-1]));
	 * </code>
	 * 
	 * @param name The name of this discount curve.
	 * @param times Array of times as doubles.
	 * @param givenHazardRates Array of corresponding zero rates.
	 * @return A new discount factor object.
	 */
	public static HazardCurve createHazardCurveFromHazardRate(String name, double[] times, double[] givenHazardRates){
		
		double[] givenSurvivalProbabilities = new double[givenHazardRates.length];
		
		if(givenHazardRates[0]<0)
			throw new IllegalArgumentException("First hazard rate is not positive");
		
		//initialize the term structure
		givenSurvivalProbabilities[0] = Math.exp(- givenHazardRates[0] * times[0]);
				
		/*
		 * Construct the hazard curve by numerically integrating the hazard rates.
		 * At each step check if the input hazard rate is positive.
		 */
		for(int timeIndex=1; timeIndex<times.length;timeIndex++) {
			
			if(givenHazardRates[timeIndex]<0)
				throw new IllegalArgumentException("The " + timeIndex + "-th hazard rate is not positive");
			
			givenSurvivalProbabilities[timeIndex] = givenSurvivalProbabilities[timeIndex-1] * Math.exp(- givenHazardRates[timeIndex] * (times[timeIndex]-times[timeIndex-1]));
		}
				
		return createHazardCurveFromSurvivalProbabilities(name, times, givenSurvivalProbabilities);
	}
	
	@Override
	public double getSurvivalProbability(AnalyticModelInterface model, double maturity) {
		
		double candidateSurvivalProbability = super.getValue(model, maturity);
		
		/*
		 * A probability must be in the interval [0,1] this guarantees also that hazard rates are positive.
		 * In this way the optimizer should always be able to continue without applying bogus probabilities to the CDS instrument.
		 */
		if(candidateSurvivalProbability >= 1){
			return 1;
		}else if (candidateSurvivalProbability <= 0){
			return 1E-20;
		}else{
			return candidateSurvivalProbability;
		}

	}
	
	
	public double getHazardRate(AnalyticModelInterface model, double maturity){
		if(maturity == 0) return 1.0E-14;

		return -Math.log(getSurvivalProbability(model, maturity))/maturity;		
		
	}
	
	
	protected void addSurvivalProbability(double maturity, double survivalProbability, boolean isParameter) {
		this.addPoint(maturity, survivalProbability, isParameter);
	}
	
	@Override
	public double getValue(AnalyticModelInterface model, double time){
		
		double myHazard = -Math.abs(this.getHazardRate(model, time));
		
		return Math.exp(myHazard*time);
	}
	
	@Override
	public String toString() {
		return super.toString();
	}	
}
