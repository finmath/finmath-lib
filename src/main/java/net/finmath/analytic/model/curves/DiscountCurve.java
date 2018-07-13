/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2005
 */
package net.finmath.analytic.model.curves;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.stream.DoubleStream;

import net.finmath.analytic.model.AnalyticModelInterface;
import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
//import net.finmath.montecarlo.AbstractRandomVariableFactory;
//import net.finmath.montecarlo.interestrate.LIBORMarketModel;
//import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * Implementation of a discount factor curve based on {@link net.finmath.marketdata.model.curves.Curve}. The discount curve is based on the {@link net.finmath.marketdata.model.curves.Curve} class.
 * 
 * It thus features all interpolation and extrapolation methods and interpolation entities
 * as {@link net.finmath.marketdata.model.curves.Curve} and implements the {@link net.finmath.marketdata.model.curves.DiscountCurveInterface}.
 * 
 * Note that this version of the DiscountCurve will no longer make the
 * assumption that at t=0 its value is 1.0. Such a norming is not
 * necessary since valuation will always divide by the corresponding
 * discount factor at evaluation time. See the implementation of {@link net.finmath.marketdata.products.SwapLeg}
 * for an example.
 * 
 * @author Christian Fries
 * @see net.finmath.marketdata.products.SwapLeg
 * @see net.finmath.marketdata.model.curves.Curve
 */
public class DiscountCurve extends Curve implements Serializable, DiscountCurveInterface {

	private static final long serialVersionUID = -4126228588123963885L;

	/**
	 * Create an empty discount curve using default interpolation and extrapolation methods.
	 * 
	 * @param name The name of this discount curve.
	 */
	private DiscountCurve(String name) {
		super(name, null, InterpolationMethod.LINEAR, ExtrapolationMethod.CONSTANT, InterpolationEntity.LOG_OF_VALUE_PER_TIME);
	}

	/**
	 * Create an empty discount curve using given interpolation and extrapolation methods.
	 *
	 * @param name The name of this discount curve.
	 * @param interpolationMethod The interpolation method used for the curve.
	 * @param extrapolationMethod The extrapolation method used for the curve.
	 * @param interpolationEntity The entity interpolated/extrapolated.
	 */
	private DiscountCurve(String name, InterpolationMethod interpolationMethod,
			ExtrapolationMethod extrapolationMethod, InterpolationEntity interpolationEntity){

		super(name, null, interpolationMethod, extrapolationMethod, interpolationEntity);
	}


	/**
	 * Create an empty discount curve using given interpolation and extrapolation methods.
	 *
	 * @param name The name of this discount curve.
	 * @param referenceDate The reference date for this curve, i.e., the date which defined t=0.
	 * @param interpolationMethod The interpolation method used for the curve.
	 * @param extrapolationMethod The extrapolation method used for the curve.
	 * @param interpolationEntity The entity interpolated/extrapolated.
	 */
	private DiscountCurve(String name, LocalDate referenceDate, InterpolationMethod interpolationMethod,
			ExtrapolationMethod extrapolationMethod, InterpolationEntity interpolationEntity){

		super(name, referenceDate, interpolationMethod, extrapolationMethod, interpolationEntity);
	}

	/**
	 * Create a discount curve from given times and given discount factors using given interpolation and extrapolation methods.
	 *
	 * @param name The name of this discount curve.
	 * @param referenceDate The reference date for this curve, i.e., the date which defined t=0.
	 * @param times Array of times as doubles.
	 * @param givenDiscountFactors Array of corresponding discount factors.
	 * @param isParameter Array of booleans specifying whether this point is served "as as parameter", e.g., whether it is calibrates (e.g. using CalibratedCurves).
	 * @param interpolationMethod The interpolation method used for the curve.
	 * @param extrapolationMethod The extrapolation method used for the curve.
	 * @param interpolationEntity The entity interpolated/extrapolated.
	 * @return A new discount factor object.
	 */
	public static DiscountCurve createDiscountCurveFromDiscountFactors(
			String name, LocalDate referenceDate,
			double[] times, RandomVariableInterface[] givenDiscountFactors, boolean[] isParameter,
			InterpolationMethod interpolationMethod, ExtrapolationMethod extrapolationMethod, InterpolationEntity interpolationEntity) {

		DiscountCurve discountFactors = new DiscountCurve(name, referenceDate, interpolationMethod, extrapolationMethod, interpolationEntity);

		for(int timeIndex=0; timeIndex<times.length;timeIndex++) {
			discountFactors.addDiscountFactor(times[timeIndex], givenDiscountFactors[timeIndex], isParameter != null && isParameter[timeIndex]);
		}

		return discountFactors;
	}

	/**
	 * Create a discount curve from given times and given discount factors using given interpolation and extrapolation methods.
	 *
	 * @param name The name of this discount curve.
	 * @param times Array of times as doubles.
	 * @param givenDiscountFactors Array of corresponding discount factors.
	 * @param isParameter Array of booleans specifying whether this point is served "as as parameter", e.g., whether it is calibrates (e.g. using CalibratedCurves).
	 * @param interpolationMethod The interpolation method used for the curve.
	 * @param extrapolationMethod The extrapolation method used for the curve.
	 * @param interpolationEntity The entity interpolated/extrapolated.
	 * @return A new discount factor object.
	 */
	public static DiscountCurve createDiscountCurveFromDiscountFactors(
			String name,
			double[] times,
			RandomVariableInterface[] givenDiscountFactors,
			boolean[] isParameter,
			InterpolationMethod interpolationMethod, ExtrapolationMethod extrapolationMethod, InterpolationEntity interpolationEntity) {
		return createDiscountCurveFromDiscountFactors(name, null, times, givenDiscountFactors, isParameter, interpolationMethod, extrapolationMethod, interpolationEntity);
	}

	/**
	 * Create a discount curve from given times and given discount factors using given interpolation and extrapolation methods.
	 *
	 * @param name The name of this discount curve.
	 * @param times Array of times as doubles.
	 * @param givenDiscountFactors Array of corresponding discount factors.
	 * @param interpolationMethod The interpolation method used for the curve.
	 * @param extrapolationMethod The extrapolation method used for the curve.
	 * @param interpolationEntity The entity interpolated/extrapolated.
	 * @return A new discount factor object.
	 */
	public static DiscountCurve createDiscountCurveFromDiscountFactors(
			String name,
			double[] times,
			RandomVariableInterface[] givenDiscountFactors,
			InterpolationMethod interpolationMethod, ExtrapolationMethod extrapolationMethod, InterpolationEntity interpolationEntity) {
		boolean[] isParameter = new boolean[times.length];
		for(int timeIndex=0; timeIndex<times.length;timeIndex++) {
			isParameter[timeIndex] = times[timeIndex] > 0;
		}

		return createDiscountCurveFromDiscountFactors(name, times, givenDiscountFactors, isParameter, interpolationMethod, extrapolationMethod, interpolationEntity);
	}

	public static DiscountCurve createDiscountCurveFromDiscountFactors(
			String name,
			double[] times,
			double[] givenDiscountFactors,
			InterpolationMethod interpolationMethod, ExtrapolationMethod extrapolationMethod, InterpolationEntity interpolationEntity) {
		RandomVariableInterface[] givenDiscountFactorsAsRandomVariables = DoubleStream.of(givenDiscountFactors).mapToObj(x -> { return new RandomVariable(x); }).toArray(RandomVariableInterface[]::new);
		return createDiscountCurveFromDiscountFactors(name, times, givenDiscountFactorsAsRandomVariables, interpolationMethod, extrapolationMethod, interpolationEntity);
	}

	/**
	 * Create a discount curve from given times and given discount factors using default interpolation and extrapolation methods.
	 * 
	 * @param name The name of this discount curve.
	 * @param times Array of times as doubles.
	 * @param givenDiscountFactors Array of corresponding discount factors.
	 * @return A new discount factor object.
	 */
	public static DiscountCurve createDiscountCurveFromDiscountFactors(String name, double[] times, RandomVariableInterface[] givenDiscountFactors) {
		DiscountCurve discountFactors = new DiscountCurve(name);

		for(int timeIndex=0; timeIndex<times.length;timeIndex++) {
			discountFactors.addDiscountFactor(times[timeIndex], givenDiscountFactors[timeIndex], times[timeIndex] > 0);
		}

		return discountFactors;
	}

	public static DiscountCurve createDiscountCurveFromDiscountFactors(String name, double[] times, double[] givenDiscountFactors) {
		RandomVariableInterface[] givenDiscountFactorsAsRandomVariables = DoubleStream.of(givenDiscountFactors).mapToObj(x -> { return new RandomVariable(x); }).toArray(RandomVariableInterface[]::new);
		return createDiscountCurveFromDiscountFactors(name, times, givenDiscountFactorsAsRandomVariables);
	}

	/**
	 * Create a discount curve from given times and given zero rates using given interpolation and extrapolation methods.
	 * The discount factor is determined by 
	 * <code>
	 * 		givenDiscountFactors[timeIndex] = Math.exp(- givenZeroRates[timeIndex] * times[timeIndex]);
	 * </code>
	 *
	 * @param name The name of this discount curve.
	 * @param referenceDate The reference date for this curve, i.e., the date which defined t=0.
	 * @param times Array of times as doubles.
	 * @param givenZeroRates Array of corresponding zero rates.
	 * @param isParameter Array of booleans specifying whether this point is served "as as parameter", e.g., whether it is calibrates (e.g. using CalibratedCurves).
	 * @param interpolationMethod The interpolation method used for the curve.
	 * @param extrapolationMethod The extrapolation method used for the curve.
	 * @param interpolationEntity The entity interpolated/extrapolated.
	 * @return A new discount factor object.
	 */
	public static DiscountCurve createDiscountCurveFromZeroRates(
			String name, LocalDate referenceDate,
			double[] times, RandomVariableInterface[] givenZeroRates, boolean[] isParameter,
			InterpolationMethod interpolationMethod, ExtrapolationMethod extrapolationMethod, InterpolationEntity interpolationEntity) {

		RandomVariableInterface[] givenDiscountFactors = new RandomVariableInterface[givenZeroRates.length];

		for(int timeIndex=0; timeIndex<times.length;timeIndex++) {
			givenDiscountFactors[timeIndex] = givenZeroRates[timeIndex].mult(-times[timeIndex]).exp();
		}

		return createDiscountCurveFromDiscountFactors(name, referenceDate, times, givenDiscountFactors, isParameter, interpolationMethod, extrapolationMethod, interpolationEntity);
	}

	/**
	 * Create a discount curve from given times and given zero rates using given interpolation and extrapolation methods.
	 * The discount factor is determined by 
	 * <code>
	 * 		givenDiscountFactors[timeIndex] = Math.exp(- givenZeroRates[timeIndex] * times[timeIndex]);
	 * </code>
	 *
	 * @param name The name of this discount curve.
	 * @param referenceDate The reference date for this curve, i.e., the date which defined t=0.
	 * @param times Array of times as doubles.
	 * @param givenZeroRates Array of corresponding zero rates.
	 * @param isParameter Array of booleans specifying whether this point is served "as as parameter", e.g., whether it is calibrates (e.g. using CalibratedCurves).
	 * @param interpolationMethod The interpolation method used for the curve.
	 * @param extrapolationMethod The extrapolation method used for the curve.
	 * @param interpolationEntity The entity interpolated/extrapolated.
	 * @return A new discount factor object.
	 */
	public static DiscountCurve createDiscountCurveFromZeroRates(
			String name, Date referenceDate,
			double[] times, RandomVariableInterface[] givenZeroRates, boolean[] isParameter,
			InterpolationMethod interpolationMethod, ExtrapolationMethod extrapolationMethod, InterpolationEntity interpolationEntity) {

		return createDiscountCurveFromZeroRates(name, referenceDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(), times, givenZeroRates, isParameter, interpolationMethod, extrapolationMethod, interpolationEntity);
	}



	/**
	 * Create a discount curve from given times and given zero rates using given interpolation and extrapolation methods.
	 * The discount factor is determined by 
	 * <code>
	 * 		givenDiscountFactors[timeIndex] = Math.exp(- givenZeroRates[timeIndex] * times[timeIndex]);
	 * </code>
	 *
	 * @param name The name of this discount curve.
	 * @param referenceDate The reference date for this curve, i.e., the date which defined t=0.
	 * @param times Array of times as doubles.
	 * @param givenZeroRates Array of corresponding zero rates.
	 * @param interpolationMethod The interpolation method used for the curve.
	 * @param extrapolationMethod The extrapolation method used for the curve.
	 * @param interpolationEntity The entity interpolated/extrapolated.
	 * @return A new discount factor object.
	 */
	public static DiscountCurve createDiscountCurveFromZeroRates(
			String name, LocalDate referenceDate,
			double[] times, RandomVariableInterface[] givenZeroRates,
			InterpolationMethod interpolationMethod, ExtrapolationMethod extrapolationMethod, InterpolationEntity interpolationEntity) {

		RandomVariableInterface[] givenDiscountFactors = new RandomVariableInterface[givenZeroRates.length];
		boolean[] isParameter = new boolean[givenZeroRates.length];

		for(int timeIndex=0; timeIndex<times.length;timeIndex++) {
			givenDiscountFactors[timeIndex] = givenZeroRates[timeIndex].mult(-times[timeIndex]).exp();
			isParameter[timeIndex] = false;
		}

		return createDiscountCurveFromDiscountFactors(name, referenceDate, times, givenDiscountFactors, isParameter, interpolationMethod, extrapolationMethod, interpolationEntity);
	}

	/**
	 * Create a discount curve from given times and given zero rates using default interpolation and extrapolation methods.
	 * The discount factor is determined by 
	 * <code>
	 * 		givenDiscountFactors[timeIndex] = Math.exp(- givenZeroRates[timeIndex] * times[timeIndex]);
	 * </code>
	 * 
	 * @param name The name of this discount curve.
	 * @param times Array of times as doubles.
	 * @param givenZeroRates Array of corresponding zero rates.
	 * @return A new discount factor object.
	 */
	public static DiscountCurve createDiscountCurveFromZeroRates(String name, double[] times, RandomVariableInterface[] givenZeroRates) {
		RandomVariableInterface[] givenDiscountFactors = new RandomVariableInterface[givenZeroRates.length];

		for(int timeIndex=0; timeIndex<times.length;timeIndex++) {
			givenDiscountFactors[timeIndex] = givenZeroRates[timeIndex].mult(-times[timeIndex]).exp();
		}

		return createDiscountCurveFromDiscountFactors(name, times, givenDiscountFactors);
	}

	/**
	 * Create a discount curve from given times and given annualized zero rates using given interpolation and extrapolation methods.
	 * The discount factor is determined by 
	 * <code>
	 * 		givenDiscountFactors[timeIndex] = Math.pow(1.0 + givenAnnualizedZeroRates[timeIndex], -times[timeIndex]);
	 * </code>
	 *
	 * @param name The name of this discount curve.
	 * @param referenceDate The reference date for this curve, i.e., the date which defined t=0.
	 * @param times Array of times as doubles.
	 * @param givenAnnualizedZeroRates Array of corresponding zero rates.
	 * @param isParameter Array of booleans specifying whether this point is served "as as parameter", e.g., whether it is calibrates (e.g. using CalibratedCurves).
	 * @param interpolationMethod The interpolation method used for the curve.
	 * @param extrapolationMethod The extrapolation method used for the curve.
	 * @param interpolationEntity The entity interpolated/extrapolated.
	 * @return A new discount factor object.
	 */
	public static DiscountCurve createDiscountCurveFromAnnualizedZeroRates(
			String name, LocalDate referenceDate,
			double[] times, RandomVariableInterface[] givenAnnualizedZeroRates, boolean[] isParameter,
			InterpolationMethod interpolationMethod, ExtrapolationMethod extrapolationMethod, InterpolationEntity interpolationEntity) {

		RandomVariableInterface[] givenDiscountFactors = new RandomVariableInterface[givenAnnualizedZeroRates.length];

		for(int timeIndex=0; timeIndex<times.length;timeIndex++) {
			givenDiscountFactors[timeIndex] = givenAnnualizedZeroRates[timeIndex].add(1.0).pow(-times[timeIndex]);
		}

		return createDiscountCurveFromDiscountFactors(name, referenceDate, times, givenDiscountFactors, isParameter, interpolationMethod, extrapolationMethod, interpolationEntity);
	}

	/**
	 * Create a discount curve from given times and given annualized zero rates using given interpolation and extrapolation methods.
	 * The discount factor is determined by 
	 * <code>
	 * 		givenDiscountFactors[timeIndex] = Math.pow(1.0 + givenAnnualizedZeroRates[timeIndex], -times[timeIndex]);
	 * </code>
	 *
	 * @param name The name of this discount curve.
	 * @param referenceDate The reference date for this curve, i.e., the date which defined t=0.
	 * @param times Array of times as doubles.
	 * @param givenAnnualizedZeroRates Array of corresponding zero rates.
	 * @param interpolationMethod The interpolation method used for the curve.
	 * @param extrapolationMethod The extrapolation method used for the curve.
	 * @param interpolationEntity The entity interpolated/extrapolated.
	 * @return A new discount factor object.
	 */
	public static DiscountCurve createDiscountCurveFromAnnualizedZeroRates(
			String name, LocalDate referenceDate,
			double[] times, RandomVariableInterface[] givenAnnualizedZeroRates,
			InterpolationMethod interpolationMethod, ExtrapolationMethod extrapolationMethod, InterpolationEntity interpolationEntity) {

		RandomVariableInterface[] givenDiscountFactors = new RandomVariableInterface[givenAnnualizedZeroRates.length];
		boolean[] isParameter = new boolean[givenAnnualizedZeroRates.length];

		for(int timeIndex=0; timeIndex<times.length;timeIndex++) {
			givenDiscountFactors[timeIndex] = givenAnnualizedZeroRates[timeIndex].add(1.0).pow(-times[timeIndex]);
			isParameter[timeIndex] = false;
		}

		return createDiscountCurveFromDiscountFactors(name, referenceDate, times, givenDiscountFactors, isParameter, interpolationMethod, extrapolationMethod, interpolationEntity);
	}

	/**
	 * Create a discount curve from given time discretization and forward rates.
	 * This function is provided for "single interest rate curve" frameworks.
	 * 
	 * @param name The name of this discount curve.
	 * @param tenor Time discretization for the forward rates
	 * @param forwardRates Array of forward rates.
	 * @return A new discount factor object.
	 */
	public static DiscountCurveInterface createDiscountFactorsFromForwardRates(String name, TimeDiscretizationInterface tenor, RandomVariableInterface [] forwardRates) {
		DiscountCurve discountFactors = new DiscountCurve(name);
		RandomVariableInterface df = forwardRates[0].mult(tenor.getTimeStep(0)).add(1.0).invert();
		discountFactors.addDiscountFactor(tenor.getTime(1), df, tenor.getTime(1) > 0);
		for(int timeIndex=1; timeIndex<tenor.getNumberOfTimeSteps();timeIndex++) {
			df = df.div(forwardRates[timeIndex].mult(tenor.getTimeStep(timeIndex)).add(1.0));
			discountFactors.addDiscountFactor(tenor.getTime(timeIndex+1), df, tenor.getTime(timeIndex+1) > 0);
		}

		return discountFactors;
	}

	/**
	 * Create a discount curve from forwards given by a LIBORMonteCarloModel. If the model uses multiple curves, return its discount curve.
	 * 
	 * @param forwardCurveName      name of the forward curve.
	 * @param model                 Monte Carlo model providing the forwards.
	 * @param startTime             time at which the curve starts, i.e. zero time for the curve
	 * @return a discount curve from forwards given by a LIBORMonteCarloModel.
	 * @throws CalculationException Thrown if the model failed to provide the forward rates.
	 */
	public static DiscountCurveInterface createDiscountCurveFromMonteCarloLiborModel(String forwardCurveName,  LIBORModelMonteCarloSimulationInterface model, double startTime) throws CalculationException{
		// Check if the LMM uses a discount curve which is created from a forward curve
		if(model.getModel().getDiscountCurve()==null || model.getModel().getDiscountCurve().getName().toLowerCase().contains("DiscountCurveFromForwardCurve".toLowerCase())){
			return new DiscountCurveFromForwardCurve(ForwardCurve.createForwardCurveFromMonteCarloLiborModel(forwardCurveName, model, startTime));
		} else {
			// i.e. forward curve of Libor Model not OIS. In this case return the OIS curve.
			// Only at startTime 0!
			return (DiscountCurveInterface) model.getModel().getDiscountCurve();
		}

	}


	// INSERTED
	public static RandomVariableInterface[] createZeroRates(double time, double[] maturities, LIBORModelMonteCarloSimulationInterface model) throws CalculationException{

		// get time index of first libor fixing time after time
		int firstLiborIndex = model.getLiborPeriodDiscretization().getTimeIndexNearestGreaterOrEqual(time);
		int remainingLibors = model.getNumberOfLibors()-firstLiborIndex;
		RandomVariableInterface[] forwardRates;
		double[] liborTimes;
		int indexOffset;
		double periodStart;
		double periodEnd;
		if(model.getLiborPeriodDiscretization().getTime(firstLiborIndex)>time){
			periodStart = time;
			periodEnd   = model.getLiborPeriodDiscretization().getTime(firstLiborIndex);
			forwardRates = new RandomVariableInterface[remainingLibors+1];
			forwardRates[0] = model.getLIBOR(time, periodStart, periodEnd);
			indexOffset = 1;
			liborTimes = new double[forwardRates.length+1];
			liborTimes[0] = 0;
		} else {
			forwardRates = new RandomVariableInterface[remainingLibors];
			indexOffset = 0;
			liborTimes = new double[forwardRates.length+1];
		}
		for(int liborIndex=firstLiborIndex;liborIndex<model.getNumberOfLibors();liborIndex++){
			periodStart = model.getLiborPeriodDiscretization().getTime(liborIndex);
			periodEnd   = model.getLiborPeriodDiscretization().getTime(liborIndex+1);
			forwardRates[liborIndex-firstLiborIndex+indexOffset]=model.getLIBOR(time, periodStart, periodEnd);
		}

		for(int i=indexOffset;i<liborTimes.length;i++) {
			liborTimes[i]=model.getLiborPeriod(firstLiborIndex+i-indexOffset)-time;
		}
		DiscountCurve df = (DiscountCurve) createDiscountFactorsFromForwardRates("",new TimeDiscretization(liborTimes), forwardRates); 
		return df.getZeroRates(maturities);
	}


	/**
	 * Returns the zero rate for a given maturity, i.e., -ln(df(T)) / T where T is the given maturity and df(T) is
	 * the discount factor at time $T$.
	 * 
	 * @param maturity The given maturity.
	 * @return The zero rate.
	 */
	public RandomVariableInterface  getZeroRate(double maturity)
	{
		if(maturity == 0) {
			return this.getZeroRate(1.0E-14);
		}

		return getDiscountFactor(maturity).log().div(-maturity);
	}

	/**
	 * Returns the zero rates for a given vector maturities.
	 * 
	 * @param maturities The given maturities.
	 * @return The zero rates.
	 */
	public RandomVariableInterface[] getZeroRates(double[] maturities)
	{
		RandomVariableInterface[] values = new RandomVariableInterface [maturities.length];

		for(int i=0; i<maturities.length; i++) {
			values[i] = getZeroRate(maturities[i]);
		}

		return values;
	}

	protected void addDiscountFactor(double maturity, RandomVariableInterface discountFactor, boolean isParameter) {
		this.addPoint(maturity, discountFactor, isParameter);
	}

	@Override
	public String toString() {
		return "DiscountCurve [" + super.toString() + "]";
	}

	@Override
	public RandomVariableInterface getDiscountFactor(double maturity) {
		return getValue(null, maturity);
	}

	@Override
	public RandomVariableInterface getDiscountFactor(AnalyticModelInterface model, double maturity) {
		return getValue(model, maturity);
	}
}