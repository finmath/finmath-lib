/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2004
 */
package net.finmath.montecarlo.interestrate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.marketdata.model.curves.DiscountCurveFromForwardCurve;
import net.finmath.marketdata.model.curves.DiscountCurveInterface;
import net.finmath.marketdata.model.curves.ForwardCurveInterface;
import net.finmath.math.stochastic.RandomVariableInterface;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.montecarlo.model.AbstractModel;
import net.finmath.montecarlo.process.AbstractProcessInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * Implements a Hull-White model with constant coefficients.
 * 
 * <i>
 * A more general implementation of the Hull-White model can be found in {@link net.finmath.montecarlo.interestrate.HullWhiteModel}.
 * For details and documentation please see {@link net.finmath.montecarlo.interestrate.HullWhiteModel} for real applications.
 * </i>
 * 
 * @author Christian Fries
 */
public class HullWhiteModelWithConstantCoeff extends AbstractModel implements LIBORModelInterface {

	private final TimeDiscretizationInterface		liborPeriodDiscretization;

	private String							forwardCurveName;
	private AnalyticModelInterface			curveModel;

	private ForwardCurveInterface			forwardRateCurve;
	private DiscountCurveInterface			discountCurve;
	private DiscountCurveInterface			discountCurveFromForwardCurve;

	private final double meanReversion;
	private final double volatility;

	// Cache for the numeraires, needs to be invalidated if process changes
	private final ConcurrentHashMap<Integer, RandomVariableInterface>	numeraires;
	private AbstractProcessInterface									numerairesProcess = null;

	
	// Initialized lazily using process time discretization
	private RandomVariableInterface[] initialState;

	/**
	 * Creates a Hull-White model which implements <code>LIBORMarketModelInterface</code>.
	 * 
	 * @param liborPeriodDiscretization The forward rate discretization to be used in the <code>getLIBOR</code> method.
	 * @param analyticModel The analytic model to be used (currently not used, may be null).
	 * @param forwardRateCurve The forward curve to be used (currently not used, - the model uses disocuntCurve only.
	 * @param discountCurve The disocuntCurve (currently also used to determine the forward curve).
	 * @param meanReversion The mean reversion speed parameter a.
	 * @param volatility The short rate volatility \( \sigma \).
	 * @param properties A map specifying model properties (currently not used, may be null).
	 */
	public HullWhiteModelWithConstantCoeff(
			TimeDiscretizationInterface			liborPeriodDiscretization,
			AnalyticModelInterface				analyticModel,
			ForwardCurveInterface				forwardRateCurve,
			DiscountCurveInterface				discountCurve,
			double 								meanReversion,
			double								volatility,
			Map<String, ?>						properties
			) {

		this.liborPeriodDiscretization	= liborPeriodDiscretization;
		this.curveModel					= analyticModel;
		this.forwardRateCurve	= forwardRateCurve;
		this.discountCurve		= discountCurve;
		this.meanReversion		= meanReversion;
		this.volatility			= volatility;

		this.discountCurveFromForwardCurve = new DiscountCurveFromForwardCurve(forwardRateCurve);

		numeraires = new ConcurrentHashMap<Integer, RandomVariableInterface>();
	}

	@Override
	public int getNumberOfComponents() {
		return 1;
	}

	@Override
	public RandomVariableInterface applyStateSpaceTransform(int componentIndex, RandomVariableInterface randomVariable) {
		return randomVariable;
	}

	@Override
	public RandomVariableInterface[] getInitialState() {
		if(initialState == null) {
			double dt = getProcess().getTimeDiscretization().getTimeStep(0);
			//liborPeriodDiscretization.getTimeStep(0);
			initialState = new RandomVariableInterface[] { new RandomVariable(Math.log(discountCurveFromForwardCurve.getDiscountFactor(0.0)/discountCurveFromForwardCurve.getDiscountFactor(dt))/dt) };
		}

		return initialState;
	}

	@Override
	public RandomVariableInterface getNumeraire(double time) throws CalculationException {
		if(time == getTime(0)) return new RandomVariable(1.0);

		int timeIndex = getProcess().getTimeIndex(time);
		if(timeIndex < 0) {
			/*
			 * time is not part of the time discretization.
			 */

			// Find the time index prior to the current time (note: if time does not match a discretization point, we get a negative value, such that -index is next point).
			int previousTimeIndex = getProcess().getTimeIndex(time);
			if(previousTimeIndex < 0) previousTimeIndex = -previousTimeIndex-1;
			previousTimeIndex--;
			double previousTime = getProcess().getTime(previousTimeIndex);

			// Get value of short rate for period from previousTime to time.
			RandomVariableInterface value = getShortRate(previousTimeIndex);

			// Piecewise constant rate for the increment
			RandomVariableInterface integratedRate = value.mult(time-previousTime);

			return getNumeraire(previousTime).mult(integratedRate.exp());
		}

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
			RandomVariableInterface zero = getProcess().getStochasticDriver().getRandomVariableForConstant(0.0);
			RandomVariableInterface integratedRate = zero;
		// Add r(t_{i}) (t_{i+1}-t_{i}) for i = 0 to previousTimeIndex-1
		for(int i=0; i<timeIndex; i++) {
				RandomVariableInterface rate = getShortRate(i);
				double dt = getProcess().getTimeDiscretization().getTimeStep(i);
				//			double dt = getB(getProcess().getTimeDiscretization().getTime(i),getProcess().getTimeDiscretization().getTime(i+1));
				integratedRate = integratedRate.addProduct(rate, dt);

			numeraire = integratedRate.exp();
			numeraires.put(i+1, numeraire);
		}
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
	public RandomVariableInterface[] getDrift(int timeIndex, RandomVariableInterface[] realizationAtTimeIndex, RandomVariableInterface[] realizationPredictor) {

		double time = getProcess().getTime(timeIndex);
		double timeNext = getProcess().getTime(timeIndex+1);

		double t0 = time;
		double t1 = timeNext;
		double t2 = timeIndex< getProcess().getTimeDiscretization().getNumberOfTimes()-2 ? getProcess().getTime(timeIndex+2) : t1 + getProcess().getTimeDiscretization().getTimeStep(timeIndex);

		double df0 = discountCurveFromForwardCurve.getDiscountFactor(t0);
		double df1 = discountCurveFromForwardCurve.getDiscountFactor(t1);
		double df2 = discountCurveFromForwardCurve.getDiscountFactor(t2);

		double forward = time > 0 ? - Math.log(df1/df0) / (t1-t0) : getInitialState()[0].get(0);
		double forwardNext = - Math.log(df2/df1) / (t2-t1);
		double forwardChange = (forwardNext-forward) / ((t1-t0));

		double meanReversionEffective = meanReversion*getB(time,timeNext)/(timeNext-time);

		double shortRateVariance = getShortRateConditionalVariance(0, time);

		/*
		 * The +meanReversionEffective * forwardPrev removes the previous forward from the mean-reversion part.
		 * The +forwardChange updates the forward to the next period.
		 */
		double theta = forwardChange + meanReversionEffective * forward + shortRateVariance*getB(time,t1)/(t1-time);

		return new RandomVariableInterface[] { realizationAtTimeIndex[0].mult(-meanReversionEffective).add(theta) };
	}

	@Override
	public RandomVariableInterface[] getFactorLoading(int timeIndex, int componentIndex, RandomVariableInterface[] realizationAtTimeIndex) {
		double time = getProcess().getTime(timeIndex);
		double timeNext = getProcess().getTime(timeIndex+1);

		double scaling = Math.sqrt((1.0-Math.exp(-2.0 * meanReversion * (timeNext-time)))/(2.0 * meanReversion * (timeNext-time)));
		double volatilityEffective = scaling*volatility;

		return new RandomVariableInterface[] { new RandomVariable(volatilityEffective) };
	}

	@Override
	public RandomVariableInterface getLIBOR(double time, double periodStart, double periodEnd) throws CalculationException
	{
		return getZeroCouponBond(time, periodStart).div(getZeroCouponBond(time, periodEnd)).sub(1.0).div(periodEnd-periodStart);
	}

	@Override
	public RandomVariableInterface getLIBOR(int timeIndex, int liborIndex) throws CalculationException {
		return getZeroCouponBond(getProcess().getTime(timeIndex), getLiborPeriod(liborIndex)).div(getZeroCouponBond(getProcess().getTime(timeIndex), getLiborPeriod(liborIndex+1))).sub(1.0).div(getLiborPeriodDiscretization().getTimeStep(liborIndex));
	}

	@Override
	public TimeDiscretizationInterface getLiborPeriodDiscretization() {
		return liborPeriodDiscretization;
	}

	@Override
	public int getNumberOfLibors() {
		return liborPeriodDiscretization.getNumberOfTimeSteps();
	}

	@Override
	public double getLiborPeriod(int timeIndex) {
		return liborPeriodDiscretization.getTime(timeIndex);
	}

	@Override
	public int getLiborPeriodIndex(double time) {
		return liborPeriodDiscretization.getTimeIndex(time);
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
	public LIBORMarketModelInterface getCloneWithModifiedData(Map<String, Object> dataModified) throws CalculationException {
		throw new UnsupportedOperationException();
	}

	private RandomVariableInterface getShortRate(int timeIndex) throws CalculationException {
		RandomVariableInterface value = getProcess().getProcessValue(timeIndex, 0);
		return value;
	}

	private RandomVariableInterface getZeroCouponBond(double time, double maturity) throws CalculationException {
		RandomVariableInterface shortRate = getShortRate(getProcess().getTimeIndex(time));
		return shortRate.mult(-getB(time,maturity)).exp().mult(getA(time, maturity));
	}

	/**
	 * Returns A(t,T) where
	 * \( A(t,T) = P(T)/P(t) \cdot exp(B(t,T) \cdot f(0,t) - \frac{1}{2} \phi(0,t) * B(t,T)^{2} ) \)
	 * and
	 * \( \phi(t,T) \) is the value calculated from integrating \( ( \sigma(s) B(s,T) )^{2} \) with respect to s from t to T
	 * in <code>getShortRateConditionalVariance</code>.
	 * 
	 * @param time The parameter t.
	 * @param maturity The parameter T.
	 * @return The value A(t,T).
	 */
	private double getA(double time, double maturity) {
		int timeIndex = getProcess().getTimeIndex(time);
		double timeStep = getProcess().getTimeDiscretization().getTimeStep(timeIndex);

		double dt = timeStep;
		double zeroRate = -Math.log(discountCurveFromForwardCurve.getDiscountFactor(time+dt)/discountCurveFromForwardCurve.getDiscountFactor(time)) / dt;

		double B = getB(time,maturity);

		double lnA = Math.log(discountCurveFromForwardCurve.getDiscountFactor(maturity)/discountCurveFromForwardCurve.getDiscountFactor(time))
				+ B * zeroRate - 0.5 * getShortRateConditionalVariance(0,time) * B * B;

		return Math.exp(lnA);
	}

	/**
	 * Calculates \( B(t,T) = \int_{t}^{T} \exp(-\int_{s}^{T} a(\tau) \mathrm{d}\tau) \mathrm{d}s \), where a is the mean reversion parameter.
	 * For a constant \( a \) this results in \( \frac{1-\exp(-a (T-t)}{a} \), but the method also supports piecewise constant \( a \)'s.
	 * 
	 * @param time The parameter t.
	 * @param maturity The parameter T.
	 * @return The value of B(t,T).
	 */
	private double getB(double time, double maturity) {
		return (1-Math.exp(-meanReversion * (maturity-time)))/meanReversion;
	}

	/**
	 * Calculates the variance \( \mathop{Var}(r(t) \vert r(s) ) \), that is
	 * \(
	 * \int_{s}^{t} \sigma^{2}(\tau) \exp(-2 \cdot a \cdot (t-\tau)) \ \mathrm{d}\tau
	 * \) where \( a \) is the meanReversion and \( \sigma \) is the short rate instantaneous volatility.
	 * 
	 * @param time The parameter s in \( \int_{s}^{t} \sigma^{2}(\tau) \exp(-2 \cdot a \cdot (t-\tau)) \ \mathrm{d}\tau \)
	 * @param maturity The parameter t in \( \int_{s}^{t} \sigma^{2}(\tau) \exp(-2 \cdot a \cdot (t-\tau)) \ \mathrm{d}\tau \)
	 * @return The integrated square volatility.
	 */
	public double getShortRateConditionalVariance(double time, double maturity) {
		return volatility*volatility * (1 - Math.exp(-2*meanReversion*(maturity-time))) / (2*meanReversion);

	}

	public double getIntegratedBondSquaredVolatility(double time, double maturity) {
		return getShortRateConditionalVariance(0, time) * getB(time,maturity) * getB(time,maturity);
	}
}


