/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2004
 */
package net.finmath.montecarlo.interestrate.models;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.DiscountCurveFromForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFromArrayFactory;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.interestrate.LIBORMarketModel;
import net.finmath.montecarlo.interestrate.LIBORModel;
import net.finmath.montecarlo.interestrate.models.covariance.ShortRateVolatilityModel;
import net.finmath.montecarlo.model.AbstractProcessModel;
import net.finmath.montecarlo.process.MonteCarloProcess;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;

/**
 * Implements a Hull-White model with time dependent mean reversion speed and time dependent short rate volatility.
 *
 * <i>
 * Note: This implementation is for illustrative purposes.
 * For a numerically equivalent, more efficient implementation see {@link net.finmath.montecarlo.interestrate.models.HullWhiteModel}.
 * Please use {@link net.finmath.montecarlo.interestrate.models.HullWhiteModel} for real applications.
 * </i>
 *
 * <p>
 * <b>Model Dynamics</b>
 * </p>
 *
 * The Hull-While model assumes the following dynamic for the short rate:
 * \[ d r(t) = ( \theta(t) - a(t) r(t) ) d t + \sigma(t) d W(t) \text{,} \quad r(t_{0}) = r_{0} \text{,} \]
 * where the function \( \theta \) determines the calibration to the initial forward curve,
 * \( a \) is the mean reversion and \( \sigma \) is the instantaneous volatility.
 *
 * The dynamic above is under the equivalent martingale measure corresponding to the numeraire
 * \[ N(t) = \exp\left( \int_0^t r(\tau) \mathrm{d}\tau \right) \text{.} \]
 *
 * The main task of this class is to provide the risk-neutral drift and the volatility to the numerical scheme (given the volatility model), simulating
 * \( r(t_{i}) \). The class then also provides and the corresponding numeraire and forward rates (LIBORs).
 *
 * <p>
 * <b>Time Discrete Model</b>
 * </p>
 *
 * Assuming piecewise constant coefficients (mean reversion speed \( a \) and short
 * rate volatility \( \sigma \) the class specifies the drift and factor loadings as
 * piecewise constant functions for an Euler-scheme.
 * The class provides the exact Euler step for the short rate r.
 *
 * More specifically (assuming a constant mean reversion speed \( a \) for a moment), considering
 * \[ \Delta \bar{r}(t_{i}) = \frac{1}{t_{i+1}-t_{i}} \int_{t_{i}}^{t_{i+1}} d r(t) \]
 * we find from
 * \[ \exp(-a t) \ \left( \mathrm{d} \left( \exp(a t) r(t) \right) \right) \ = \ a r(t) + \mathrm{d} r(t) \ = \ \theta(t) \mathrm{d}t + \sigma(t) \mathrm{d}W(t) \]
 * that
 * \[ \exp(a t_{i+1}) r(t_{i+1}) - \exp(a t_{i}) r(t_{i}) \ = \ \int_{t_{i}}^{t_{i+1}} \left[ \exp(a t) \theta(t) \mathrm{d}t + \exp(a t) \sigma(t) \mathrm{d}W(t) \right] \]
 * that is
 * \[ r(t_{i+1}) - r(t_{i}) \ = \ -(1-\exp(-a (t_{i+1}-t_{i})) r(t_{i}) + \int_{t_{i}}^{t_{i+1}} \left[ \exp(-a (t_{i+1}-t)) \theta(t) \mathrm{d}t + \exp(-a (t_{i+1}-t)) \sigma(t) \mathrm{d}W(t) \right] \]
 *
 * Assuming piecewise constant \( \sigma \) and \( \theta \), being constant over \( (t_{i},t_{i}+\Delta t_{i}) \), we thus find
 * \[ r(t_{i+1}) - r(t_{i}) \ = \ \frac{1-\exp(-a \Delta t_{i})}{a \Delta t_{i}} \left( ( \theta(t_{i}) - a \bar{r}(t_{i})) \Delta t_{i} \right) + \sqrt{\frac{1-\exp(-2 a \Delta t_{i})}{2 a \Delta t_{i}}} \sigma(t_{i}) \Delta W(t_{i}) \] .
 *
 * In other words, the Euler scheme is exact if the mean reversion \( a \) is replaced by the effective mean reversion
 * \( \frac{1-\exp(-a \Delta t_{i})}{a \Delta t_{i}} a \) and the volatility is replaced by the
 * effective volatility \( \sqrt{\frac{1-\exp(-2 a \Delta t_{i})}{2 a \Delta t_{i}}} \sigma(t_{i}) \).
 *
 * In the calculations above the mean reversion speed is treated as a constants, but it is straight
 * forward to see that the same holds for piecewise constant mean reversion speeds, replacing
 * the expression \( a \ t \) by \( \int_{0}^t a(s) \mathrm{d}s \).
 *
 * <p>
 * <b>Calibration</b>
 * </p>
 *
 * The drift of the short rate is calibrated to the given forward curve using
 * \[ \theta(t) = \frac{\partial}{\partial T} f(0,t) + a(t) f(0,t) + \phi(t) \text{,} \]
 * where the function \( f \) denotes the instantanenous forward rate and
 * \( \phi(t) = \frac{1}{2} a \sigma^{2}(t) B(t)^{2} + \sigma^{2}(t) B(t) \frac{\partial}{\partial t} B(t) \) with \( B(t) = \frac{1-\exp(-a t)}{a} \).
 *
 * <p>
 * <b>Volatility Model</b>
 * </p>
 *
 * The Hull-White model is essentially equivalent to LIBOR Market Model where the forward rate <b>normal</b> volatility \( \sigma(t,T) \) is
 * given by
 * \[  \sigma(t,T_{i}) \ = \ (1 + L_{i}(t) (T_{i+1}-T_{i})) \sigma(t) \exp(-a (T_{i}-t)) \frac{1-\exp(-a (T_{i+1}-T_{i}))}{a (T_{i+1}-T_{i})} \]
 * (where \( \{ T_{i} \} \) is the forward rates tenor time discretization (note that this is the <b>normal</b> volatility, not the <b>log-normal</b> volatility).
 * Hence, we interpret both, short rate mean reversion speed and short rate volatility as part of the <i>volatility model</i>.
 *
 * The mean reversion speed and the short rate volatility have to be provided to this class via an object implementing
 * {@link net.finmath.montecarlo.interestrate.models.covariance.ShortRateVolatilityModel}.
 *
 *
 * @see net.finmath.montecarlo.interestrate.models.covariance.ShortRateVolatilityModel
 * @see net.finmath.montecarlo.interestrate.models.HullWhiteModel
 *
 * @author Christian Fries
 * @version 1.2
 */
public class HullWhiteModelWithDirectSimulation extends AbstractProcessModel implements LIBORModel {

	private final TimeDiscretization		liborPeriodDiscretization;

	private String							forwardCurveName;
	private final AnalyticModel			analyticModel;

	private final ForwardCurve			forwardRateCurve;
	private final DiscountCurve			discountCurve;
	private final DiscountCurve			discountCurveFromForwardCurve;

	private final RandomVariableFactory	randomVariableFactory = new RandomVariableFromArrayFactory();

	// Cache for the numeraires, needs to be invalidated if process changes
	private final ConcurrentHashMap<Integer, RandomVariable>	numeraires;
	private MonteCarloProcess									numerairesProcess = null;

	private final ShortRateVolatilityModel volatilityModel;

	// Initialized lazily using process time discretization
	private RandomVariable[] initialState;

	/**
	 * Creates a Hull-White model which implements <code>LIBORMarketModel</code>.
	 *
	 * @param liborPeriodDiscretization The forward rate discretization to be used in the <code>getLIBOR</code> method.
	 * @param analyticModel The analytic model to be used (currently not used, may be null).
	 * @param forwardRateCurve The forward curve to be used (currently not used, - the model uses disocuntCurve only.
	 * @param discountCurve The disocuntCurve (currently also used to determine the forward curve).
	 * @param volatilityModel The volatility model specifying mean reversion and instantaneous volatility of the short rate.
	 * @param properties A map specifying model properties (currently not used, may be null).
	 */
	public HullWhiteModelWithDirectSimulation(
			final TimeDiscretization			liborPeriodDiscretization,
			final AnalyticModel				analyticModel,
			final ForwardCurve				forwardRateCurve,
			final DiscountCurve				discountCurve,
			final ShortRateVolatilityModel	volatilityModel,
			final Map<String, ?>						properties
			) {

		this.liborPeriodDiscretization	= liborPeriodDiscretization;
		this.analyticModel				= analyticModel;
		this.forwardRateCurve	= forwardRateCurve;
		this.discountCurve		= discountCurve;
		this.volatilityModel	= volatilityModel;

		discountCurveFromForwardCurve = new DiscountCurveFromForwardCurve(forwardRateCurve);

		numeraires = new ConcurrentHashMap<>();
	}

	@Override
	public int getNumberOfComponents() {
		return 1;
	}

	@Override
	public int getNumberOfFactors()
	{
		return 1;
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
	public RandomVariable[] getInitialState(MonteCarloProcess process) {
		if(initialState == null) {
			final double dt = process.getTimeDiscretization().getTimeStep(0);
			initialState = new RandomVariable[] { new RandomVariableFromDoubleArray(Math.log(discountCurveFromForwardCurve.getDiscountFactor(0.0)/discountCurveFromForwardCurve.getDiscountFactor(dt))/dt) };
		}

		return initialState;
	}

	@Override
	public RandomVariable getNumeraire(final MonteCarloProcess process, final double time) throws CalculationException {
		if(time < 0) {
			return randomVariableFactory.createRandomVariable(discountCurve.getDiscountFactor(analyticModel, time));
		}

		if(time == process.getTime(0)) {
			// Initial value of numeraire is one - BrownianMotion serves as a factory here.
			final RandomVariable one = randomVariableFactory.createRandomVariable(1.0);
			return one;
		}

		final int timeIndex = process.getTimeIndex(time);
		if(timeIndex < 0) {
			/*
			 * time is not part of the time discretization.
			 */

			// Find the time index prior to the current time (note: if time does not match a discretization point, we get a negative value, such that -index is next point).
			int previousTimeIndex = process.getTimeIndex(time);
			if(previousTimeIndex < 0) {
				previousTimeIndex = -previousTimeIndex-1;
			}
			previousTimeIndex--;
			final double previousTime = process.getTime(previousTimeIndex);

			// Get value of short rate for period from previousTime to time.
			final RandomVariable rate = getShortRate(process, previousTimeIndex);

			// Piecewise constant rate for the increment
			final RandomVariable integratedRate = rate.mult(time-previousTime);

			return getNumeraire(process, previousTime).mult(integratedRate.exp());
		}

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
			final RandomVariable zero = process.getStochasticDriver().getRandomVariableForConstant(0.0);
			RandomVariable integratedRate = zero;
			// Add r(t_{i}) (t_{i+1}-t_{i}) for i = 0 to previousTimeIndex-1
			for(int i=0; i<timeIndex; i++) {
				final RandomVariable rate = getShortRate(process, i);
				final double dt = process.getTimeDiscretization().getTimeStep(i);
				//			double dt = getB(process.getTimeDiscretization().getTime(i),process.getTimeDiscretization().getTime(i+1));
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
			final double deterministicNumeraireAdjustment = numeraire.invert().getAverage() / discountCurve.getDiscountFactor(analyticModel, time);
			numeraire = numeraire.mult(deterministicNumeraireAdjustment);
		}

		return numeraire;
	}

	@Override
	public RandomVariable[] getDrift(final MonteCarloProcess process, final int timeIndex, final RandomVariable[] realizationAtTimeIndex, final RandomVariable[] realizationPredictor) {

		final double time = process.getTime(timeIndex);
		final double timeNext = process.getTime(timeIndex+1);

		final double t0 = time;
		final double t1 = timeNext;
		final double t2 = timeIndex< process.getTimeDiscretization().getNumberOfTimes()-2 ? process.getTime(timeIndex+2) : t1 + process.getTimeDiscretization().getTimeStep(timeIndex);

		final double df0 = discountCurveFromForwardCurve.getDiscountFactor(t0);
		final double df1 = discountCurveFromForwardCurve.getDiscountFactor(t1);
		final double df2 = discountCurveFromForwardCurve.getDiscountFactor(t2);

		final double forward = time > 0 ? - Math.log(df1/df0) / (t1-t0) : getInitialState(process)[0].get(0);
		final double forwardNext = - Math.log(df2/df1) / (t2-t1);
		final double forwardChange = (forwardNext-forward) / ((t1-t0));

		int timeIndexVolatility = volatilityModel.getTimeDiscretization().getTimeIndex(time);
		if(timeIndexVolatility < 0) {
			timeIndexVolatility = -timeIndexVolatility-2;
		}
		final double meanReversion = volatilityModel.getMeanReversion(timeIndexVolatility).doubleValue();
		final double meanReversionEffective = meanReversion*getB(time,timeNext)/(timeNext-time);

		//		double phi = getShortRateConditionalVariance(0, timeNext) * getB(time,timeNext)/(timeNext-time);
		final double phi = (getDV(0, timeNext) - Math.exp(-meanReversion * (timeNext-time)) *  getDV(0, time)) / (timeNext-time);

		/*
		 * The +meanReversionEffective * forwardPrev removes the previous forward from the mean-reversion part.
		 * The +forwardChange updates the forward to the next period.
		 */
		final double theta = forwardChange + meanReversionEffective * forward + phi;

		return new RandomVariable[] { realizationAtTimeIndex[0].mult(-meanReversionEffective).add(theta) };
	}

	@Override
	public RandomVariable[] getFactorLoading(final MonteCarloProcess process, final int timeIndex, final int componentIndex, final RandomVariable[] realizationAtTimeIndex) {
		final double time = process.getTime(timeIndex);
		final double timeNext = process.getTime(timeIndex+1);

		int timeIndexVolatility = volatilityModel.getTimeDiscretization().getTimeIndex(time);
		if(timeIndexVolatility < 0) {
			timeIndexVolatility = -timeIndexVolatility-2;
		}

		final double meanReversion = volatilityModel.getMeanReversion(timeIndexVolatility).doubleValue();
		final double volatility = volatilityModel.getVolatility(timeIndexVolatility).doubleValue();
		final double scaling = Math.sqrt((1.0-Math.exp(-2.0 * meanReversion * (timeNext-time)))/(2.0 * meanReversion * (timeNext-time)));
		final double volatilityEffective = scaling * volatility;

		return new RandomVariable[] { new RandomVariableFromDoubleArray(volatilityEffective) };
	}

	@Override
	public RandomVariable getRandomVariableForConstant(final double value) {
		return randomVariableFactory.createRandomVariable(value);
	}

	@Override
	public RandomVariable getForwardRate(final MonteCarloProcess process, final double time, final double periodStart, final double periodEnd) throws CalculationException
	{
		return getZeroCouponBond(process, time, periodStart).div(getZeroCouponBond(process, time, periodEnd)).sub(1.0).div(periodEnd-periodStart);
	}

	@Override
	public RandomVariable getLIBOR(final MonteCarloProcess process, final int timeIndex, final int liborIndex) throws CalculationException {
		return getZeroCouponBond(process, process.getTime(timeIndex), getLiborPeriod(liborIndex)).div(getZeroCouponBond(process, process.getTime(timeIndex), getLiborPeriod(liborIndex+1))).sub(1.0).div(getLiborPeriodDiscretization().getTimeStep(liborIndex));
	}

	@Override
	public TimeDiscretization getLiborPeriodDiscretization() {
		return liborPeriodDiscretization;
	}

	@Override
	public int getNumberOfLibors() {
		return liborPeriodDiscretization.getNumberOfTimeSteps();
	}

	@Override
	public double getLiborPeriod(final int timeIndex) {
		return liborPeriodDiscretization.getTime(timeIndex);
	}

	@Override
	public int getLiborPeriodIndex(final double time) {
		return liborPeriodDiscretization.getTimeIndex(time);
	}

	@Override
	public AnalyticModel getAnalyticModel() {
		return analyticModel;
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
	public LIBORMarketModel getCloneWithModifiedData(final Map<String, Object> dataModified) {
		throw new UnsupportedOperationException();
	}

	private RandomVariable getShortRate(final MonteCarloProcess process, final int timeIndex) throws CalculationException {
		final RandomVariable value = process.getProcessValue(timeIndex, 0);
		return value;
	}

	private RandomVariable getZeroCouponBond(final MonteCarloProcess process, final double time, final double maturity) throws CalculationException {
		final int timeIndex = process.getTimeIndex(time);
		final RandomVariable shortRate = getShortRate(process, timeIndex);
		final double A = getA(process, time, maturity);
		final double B = getB(time, maturity);
		return shortRate.mult(-B).exp().mult(A);
	}

	/**
	 * Returns A(t,T) where
	 * \( A(t,T) = P(T)/P(t) \cdot exp(B(t,T) \cdot f(0,t) - \frac{1}{2} \phi(0,t) * B(t,T)^{2} ) \)
	 * and
	 * \( \phi(t,T) \) is the value calculated from integrating \( ( \sigma(s) exp(-\int_{s}^{T} a(\tau) \mathrm{d}\tau ) )^{2} \) with respect to s from t to T
	 * in <code>getShortRateConditionalVariance</code>.
	 *
	 * @param time The parameter t.
	 * @param maturity The parameter T.
	 * @return The value A(t,T).
	 */
	private double getA(final MonteCarloProcess process, final double time, final double maturity) {
		final int timeIndex = process.getTimeIndex(time);
		final double timeStep = process.getTimeDiscretization().getTimeStep(timeIndex);

		final double dt = timeStep;
		final double zeroRate = -Math.log(discountCurveFromForwardCurve.getDiscountFactor(time+dt)/discountCurveFromForwardCurve.getDiscountFactor(time)) / dt;

		final double B = getB(time,maturity);

		final double lnA = Math.log(discountCurveFromForwardCurve.getDiscountFactor(maturity)/discountCurveFromForwardCurve.getDiscountFactor(time))
				+ B * zeroRate - 0.5 * getShortRateConditionalVariance(0,time) * B * B;

		return Math.exp(lnA);
	}

	/**
	 * Calculates \( \int_{t}^{T} a(s) \mathrm{d}s \), where \( a \) is the mean reversion parameter.
	 *
	 * @param time The parameter t.
	 * @param maturity The parameter T.
	 * @return The value of \( \int_{t}^{T} a(s) \mathrm{d}s \).
	 */
	private double getMRTime(final double time, final double maturity) {
		int timeIndexStart = volatilityModel.getTimeDiscretization().getTimeIndex(time);
		if(timeIndexStart < 0)
		{
			timeIndexStart = -timeIndexStart-1;	// Get timeIndex corresponding to next point
		}

		int timeIndexEnd =volatilityModel.getTimeDiscretization().getTimeIndex(maturity);
		if(timeIndexEnd < 0)
		{
			timeIndexEnd = -timeIndexEnd-2;	// Get timeIndex corresponding to previous point
		}

		double integral = 0.0;
		double timePrev = time;
		double timeNext;
		for(int timeIndex=timeIndexStart+1; timeIndex<=timeIndexEnd; timeIndex++) {
			timeNext = volatilityModel.getTimeDiscretization().getTime(timeIndex);
			final double meanReversion = volatilityModel.getMeanReversion(timeIndex-1).doubleValue();
			integral += meanReversion*(timeNext-timePrev);
			timePrev = timeNext;
		}
		timeNext = maturity;
		final double meanReversion = volatilityModel.getMeanReversion(timeIndexEnd).doubleValue();
		integral += meanReversion*(timeNext-timePrev);

		return integral;
	}

	/**
	 * Calculates \( B(t,T) = \int_{t}^{T} \exp(-\int_{s}^{T} a(\tau) \mathrm{d}\tau) \mathrm{d}s \), where a is the mean reversion parameter.
	 * For a constant \( a \) this results in \( \frac{1-\exp(-a (T-t)}{a} \), but the method also supports piecewise constant \( a \)'s.
	 *
	 * @param time The parameter t.
	 * @param maturity The parameter T.
	 * @return The value of B(t,T).
	 */
	private double getB(final double time, final double maturity) {
		int timeIndexStart = volatilityModel.getTimeDiscretization().getTimeIndex(time);
		if(timeIndexStart < 0)
		{
			timeIndexStart = -timeIndexStart-1;	// Get timeIndex corresponding to next point
		}

		int timeIndexEnd =volatilityModel.getTimeDiscretization().getTimeIndex(maturity);
		if(timeIndexEnd < 0)
		{
			timeIndexEnd = -timeIndexEnd-2;	// Get timeIndex corresponding to previous point
		}

		double integral = 0.0;
		double timePrev = time;
		double timeNext;
		for(int timeIndex=timeIndexStart+1; timeIndex<=timeIndexEnd; timeIndex++) {
			timeNext = volatilityModel.getTimeDiscretization().getTime(timeIndex);
			final double meanReversion = volatilityModel.getMeanReversion(timeIndex-1).doubleValue();
			integral += (Math.exp(-getMRTime(timeNext,maturity)) - Math.exp(-getMRTime(timePrev,maturity)))/meanReversion;
			timePrev = timeNext;
		}
		final double meanReversion = volatilityModel.getMeanReversion(timeIndexEnd).doubleValue();
		timeNext = maturity;
		integral += (Math.exp(-getMRTime(timeNext,maturity)) - Math.exp(-getMRTime(timePrev,maturity)))/meanReversion;

		return integral;
	}

	/**
	 * Calculates the drift adjustment for the log numeraire, that is
	 * \(
	 * \int_{t}^{T} \sigma^{2}(s) B(s,T)^{2} \mathrm{d}s
	 * \) where \( B(t,T) = \int_{t}^{T} \exp(-\int_{s}^{T} a(\tau) \mathrm{d}\tau) \mathrm{d}s \).
	 *
	 * @param time The parameter t in \( \int_{t}^{T} \sigma^{2}(s) B(s,T)^{2} \mathrm{d}s \)
	 * @param maturity The parameter T in \( \int_{t}^{T} \sigma^{2}(s) B(s,T)^{2} \mathrm{d}s \)
	 * @return The integral \( \int_{t}^{T} \sigma^{2}(s) B(s,T)^{2} \mathrm{d}s \).
	 */
	private double getV(final double time, final double maturity) {
		if(time==maturity) {
			return 0;
		}
		int timeIndexStart = volatilityModel.getTimeDiscretization().getTimeIndex(time);
		if(timeIndexStart < 0)
		{
			timeIndexStart = -timeIndexStart-1;	// Get timeIndex corresponding to next point
		}

		int timeIndexEnd =volatilityModel.getTimeDiscretization().getTimeIndex(maturity);
		if(timeIndexEnd < 0)
		{
			timeIndexEnd = -timeIndexEnd-2;	// Get timeIndex corresponding to previous point
		}

		double integral = 0.0;
		double timePrev = time;
		double timeNext;
		for(int timeIndex=timeIndexStart+1; timeIndex<=timeIndexEnd; timeIndex++) {
			timeNext = volatilityModel.getTimeDiscretization().getTime(timeIndex);
			final double meanReversion = volatilityModel.getMeanReversion(timeIndex-1).doubleValue();
			final double volatility = volatilityModel.getVolatility(timeIndex-1).doubleValue();
			integral += volatility * volatility * (timeNext-timePrev)/(meanReversion*meanReversion);
			integral -= volatility * volatility * 2 * (Math.exp(- getMRTime(timeNext,maturity))-Math.exp(- getMRTime(timePrev,maturity))) / (meanReversion*meanReversion*meanReversion);
			integral += volatility * volatility * (Math.exp(- 2 * getMRTime(timeNext,maturity))-Math.exp(- 2 * getMRTime(timePrev,maturity))) / (2 * meanReversion*meanReversion*meanReversion);
			timePrev = timeNext;
		}
		timeNext = maturity;
		final double meanReversion = volatilityModel.getMeanReversion(timeIndexEnd).doubleValue();
		final double volatility = volatilityModel.getVolatility(timeIndexEnd).doubleValue();
		integral += volatility * volatility * (timeNext-timePrev)/(meanReversion*meanReversion);
		integral -= volatility * volatility * 2 * (Math.exp(- getMRTime(timeNext,maturity))-Math.exp(- getMRTime(timePrev,maturity))) / (meanReversion*meanReversion*meanReversion);
		integral += volatility * volatility * (Math.exp(- 2 * getMRTime(timeNext,maturity))-Math.exp(- 2 * getMRTime(timePrev,maturity))) / (2 * meanReversion*meanReversion*meanReversion);

		return integral;
	}

	private double getDV(final double time, final double maturity) {
		if(time==maturity) {
			return 0;
		}
		int timeIndexStart = volatilityModel.getTimeDiscretization().getTimeIndex(time);
		if(timeIndexStart < 0)
		{
			timeIndexStart = -timeIndexStart-1;	// Get timeIndex corresponding to next point
		}

		int timeIndexEnd =volatilityModel.getTimeDiscretization().getTimeIndex(maturity);
		if(timeIndexEnd < 0)
		{
			timeIndexEnd = -timeIndexEnd-2;	// Get timeIndex corresponding to previous point
		}

		double integral = 0.0;
		double timePrev = time;
		double timeNext;
		for(int timeIndex=timeIndexStart+1; timeIndex<=timeIndexEnd; timeIndex++) {
			timeNext = volatilityModel.getTimeDiscretization().getTime(timeIndex);
			final double meanReversion = volatilityModel.getMeanReversion(timeIndex-1).doubleValue();
			final double volatility = volatilityModel.getVolatility(timeIndex-1).doubleValue();
			integral += volatility * volatility * (Math.exp(- getMRTime(timeNext,maturity))-Math.exp(- getMRTime(timePrev,maturity))) / (meanReversion*meanReversion);
			integral -= volatility * volatility * (Math.exp(- 2 * getMRTime(timeNext,maturity))-Math.exp(- 2 * getMRTime(timePrev,maturity))) / (2 * meanReversion*meanReversion);
			timePrev = timeNext;
		}
		timeNext = maturity;
		final double meanReversion = volatilityModel.getMeanReversion(timeIndexEnd).doubleValue();
		final double volatility = volatilityModel.getVolatility(timeIndexEnd).doubleValue();
		integral += volatility * volatility * (Math.exp(- getMRTime(timeNext,maturity))-Math.exp(- getMRTime(timePrev,maturity))) / (meanReversion*meanReversion);
		integral -= volatility * volatility * (Math.exp(- 2 * getMRTime(timeNext,maturity))-Math.exp(- 2 * getMRTime(timePrev,maturity))) / (2 * meanReversion*meanReversion);

		return integral;
	}

	/**
	 * Calculates the variance \( \mathop{Var}(r(t) \vert r(s) ) \), that is
	 * \(
	 * \int_{s}^{t} \sigma^{2}(\tau) \exp(-2 \cdot \int_{\tau}^{t} a(u) \mathrm{d}u ) \ \mathrm{d}\tau
	 * \) where \( a \) is the meanReversion and \( \sigma \) is the short rate instantaneous volatility.
	 *
	 * @param time The parameter s in \( \int_{s}^{t} \sigma^{2}(\tau) \exp(-2 \cdot \int_{\tau}^{t} a(u) \mathrm{d}u ) \ \mathrm{d}\tau \)
	 * @param maturity The parameter t in \( \int_{s}^{t} \sigma^{2}(\tau) \exp(-2 \cdot \int_{\tau}^{t} a(u) \mathrm{d}u ) \ \mathrm{d}\tau \)
	 * @return The conditional variance of the short rate, \( \mathop{Var}(r(t) \vert r(s) ) \).
	 */
	public double getShortRateConditionalVariance(final double time, final double maturity) {
		int timeIndexStart = volatilityModel.getTimeDiscretization().getTimeIndex(time);
		if(timeIndexStart < 0)
		{
			timeIndexStart = -timeIndexStart-1;	// Get timeIndex corresponding to next point
		}

		int timeIndexEnd =volatilityModel.getTimeDiscretization().getTimeIndex(maturity);
		if(timeIndexEnd < 0)
		{
			timeIndexEnd = -timeIndexEnd-2;	// Get timeIndex corresponding to previous point
		}

		double integral = 0.0;
		double timePrev = time;
		double timeNext;
		for(int timeIndex=timeIndexStart+1; timeIndex<=timeIndexEnd; timeIndex++) {
			timeNext = volatilityModel.getTimeDiscretization().getTime(timeIndex);
			final double meanReversion = volatilityModel.getMeanReversion(timeIndex-1).doubleValue();
			final double volatility = volatilityModel.getVolatility(timeIndex-1).doubleValue();
			integral += volatility * volatility * (Math.exp(-2 * getMRTime(timeNext,maturity))-Math.exp(-2 * getMRTime(timePrev,maturity))) / (2*meanReversion);
			timePrev = timeNext;
		}
		timeNext = maturity;
		final double meanReversion = volatilityModel.getMeanReversion(timeIndexEnd).doubleValue();
		final double volatility = volatilityModel.getVolatility(timeIndexEnd).doubleValue();
		integral += volatility * volatility * (Math.exp(-2 * getMRTime(timeNext,maturity))-Math.exp(-2 * getMRTime(timePrev,maturity))) / (2*meanReversion);

		return integral;
	}

	public double getIntegratedBondSquaredVolatility(final double time, final double maturity) {
		return getShortRateConditionalVariance(0, time) * getB(time,maturity) * getB(time,maturity);
	}

	@Override
	public Map<String, RandomVariable> getModelParameters() {
		// TODO Add implementation
		throw new UnsupportedOperationException();
	}
}

