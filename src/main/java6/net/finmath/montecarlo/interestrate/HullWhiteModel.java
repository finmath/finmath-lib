/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2004
 */
package net.finmath.montecarlo.interestrate;

import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.marketdata.model.curves.DiscountCurveFromForwardCurve;
import net.finmath.marketdata.model.curves.DiscountCurveInterface;
import net.finmath.marketdata.model.curves.ForwardCurveInterface;
import net.finmath.montecarlo.interestrate.modelplugins.ShortRateVolailityModelInterface;
import net.finmath.montecarlo.model.AbstractModel;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * Implements a Hull-White model with time dependent mean reversion speed and time dependent short rate volatility.
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
 * The class provides the exact Euler step for the joint distribution of
 * \( (r,N) \), where \( r \) denotes the short rate and \( N \) denotes the
 * numeraire, following the scheme in <a href="http://ssrn.com/abstract=2737091">ssrn.com/abstract=2737091</a>.
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
 * (where \( \{ T_{i} \} \) is the forward rates tenor time discretization (note that this is the <b>normal</b> volatility, not the <b>log-normal</b> volatility)
 * (see <a href="http://ssrn.com/abstract=2737091">ssrn.com/abstract=2737091</a> for details on the derivation).
 * Hence, we interpret both, short rate mean reversion speed and short rate volatility as part of the <i>volatility model</i>.
 * 
 * The mean reversion speed and the short rate volatility have to be provided to this class via an object implementing
 * {@link net.finmath.montecarlo.interestrate.modelplugins.ShortRateVolailityModelInterface}. 
 * 
 * @see net.finmath.montecarlo.interestrate.modelplugins.ShortRateVolailityModelInterface
 * @see <a href="http://ssrn.com/abstract=2737091">ssrn.com/abstract=2737091</a>
 * 
 * @author Christian Fries
 * @version 1.4
 */
public class HullWhiteModel extends AbstractModel implements LIBORModelInterface {

	private final TimeDiscretizationInterface		liborPeriodDiscretization;

	private String							forwardCurveName;
	private AnalyticModelInterface			curveModel;

	private ForwardCurveInterface			forwardRateCurve;
	private DiscountCurveInterface			discountCurve;
	private DiscountCurveInterface			discountCurveFromForwardCurve;

	private final ShortRateVolailityModelInterface volatilityModel;

	/**
	 * Creates a Hull-White model which implements <code>LIBORMarketModelInterface</code>.
	 * 
	 * @param liborPeriodDiscretization The forward rate discretization to be used in the <code>getLIBOR</code> method.
	 * @param analyticModel The analytic model to be used (currently not used, may be null).
	 * @param forwardRateCurve The forward curve to be used (currently not used, - the model uses disocuntCurve only.
	 * @param discountCurve The disocuntCurve (currently also used to determine the forward curve).
	 * @param volatilityModel The volatility model specifying mean reversion and instantaneous volatility of the short rate.
	 * @param properties A map specifying model properties (currently not used, may be null).
	 */
	public HullWhiteModel(
			TimeDiscretizationInterface			liborPeriodDiscretization,
			AnalyticModelInterface				analyticModel,
			ForwardCurveInterface				forwardRateCurve,
			DiscountCurveInterface				discountCurve,
			ShortRateVolailityModelInterface	volatilityModel,
			Map<String, ?>						properties
			) {

		this.liborPeriodDiscretization	= liborPeriodDiscretization;
		this.curveModel					= analyticModel;
		this.forwardRateCurve	= forwardRateCurve;
		this.discountCurve		= discountCurve;
		this.volatilityModel	= volatilityModel;

		this.discountCurveFromForwardCurve = new DiscountCurveFromForwardCurve(forwardRateCurve);
	}

	@Override
	public int getNumberOfComponents() {
		return 2;
	}

	@Override
	public RandomVariableInterface applyStateSpaceTransform(int componentIndex, RandomVariableInterface randomVariable) {
		return randomVariable;
	}

	@Override
	public RandomVariableInterface applyStateSpaceTransformInverse(int componentIndex, RandomVariableInterface randomVariable) {
		return randomVariable;
	}

	@Override
	public RandomVariableInterface[] getInitialState() {
		// Initial value is zero - BrownianMotionInterface serves as a factory here.
		RandomVariableInterface zero = getProcess().getStochasticDriver().getRandomVariableForConstant(0.0);
		return new RandomVariableInterface[] { zero, zero };
	}

	@Override
	public RandomVariableInterface getNumeraire(double time) throws CalculationException {
		if(time == getTime(0)) {
			// Initial value of numeraire is one - BrownianMotionInterface serves as a factory here.
			RandomVariableInterface one = getProcess().getStochasticDriver().getRandomVariableForConstant(1.0);
			return one;
		}

		int timeIndex = getProcess().getTimeIndex(time);
		if(timeIndex < 0) {
			/*
			 * time is not part of the time discretization.
			 */

			// Find the time index prior to the current time (note: if time does not match a discretization point, we get a negative value, such that -index is next point).
			int previousTimeIndex = getProcess().getTimeIndex(time);
			if(previousTimeIndex < 0) {
				previousTimeIndex = -previousTimeIndex-1;
			}
			previousTimeIndex--;
			double previousTime = getProcess().getTime(previousTimeIndex);
			double nextTime = getProcess().getTime(previousTimeIndex+1);

			// Log-linear interpolation
			return getNumeraire(previousTime).log().mult(nextTime-time)
					.add(getNumeraire(nextTime).log().mult(time-previousTime))
					.div(nextTime-previousTime).exp();
		}

		RandomVariableInterface logNum = getProcessValue(timeIndex, 1).add(0.5*getV(0,time));
		RandomVariableInterface numeraire = logNum.exp().div(discountCurveFromForwardCurve.getDiscountFactor(curveModel, time));

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

		int timeIndexVolatility = volatilityModel.getTimeDiscretization().getTimeIndex(time);
		if(timeIndexVolatility < 0) {
			timeIndexVolatility = -timeIndexVolatility-2;
		}
		double meanReversion = volatilityModel.getMeanReversion(timeIndexVolatility);
		double meanReversionEffective = meanReversion*getB(time,timeNext)/(timeNext-time);

		RandomVariableInterface driftShortRate		= realizationAtTimeIndex[0].mult(-meanReversionEffective);
		RandomVariableInterface driftLogNumeraire	= realizationAtTimeIndex[0].mult(getB(time,timeNext)/(timeNext-time));

		return new RandomVariableInterface[] { driftShortRate, driftLogNumeraire };
	}

	@Override
	public RandomVariableInterface[] getFactorLoading(int timeIndex, int componentIndex, RandomVariableInterface[] realizationAtTimeIndex) {
		double time = getProcess().getTime(timeIndex);
		double timeNext = getProcess().getTime(timeIndex+1);

		int timeIndexVolatility = volatilityModel.getTimeDiscretization().getTimeIndex(time);
		if(timeIndexVolatility < 0) {
			timeIndexVolatility = -timeIndexVolatility-2;
		}

		double meanReversion = volatilityModel.getMeanReversion(timeIndexVolatility);
		double scaling = Math.sqrt((1.0-Math.exp(-2.0 * meanReversion * (timeNext-time)))/(2.0 * meanReversion * (timeNext-time)));

		double volatilityEffective = scaling * volatilityModel.getVolatility(timeIndexVolatility);

		double factorLoading1, factorLoading2;
		if(componentIndex == 0) {
			// Factor loadings for the short rate driver.
			factorLoading1 = volatilityEffective;
			factorLoading2 = 0.0;
		}
		else if(componentIndex == 1) {
			// Factor loadings for the numeraire driver.
			double volatilityLogNumeraire = Math.sqrt(getV(time,timeNext) / (timeNext-time));
			double rho = (getDV(time,timeNext) / (timeNext-time)) / (volatilityEffective * volatilityLogNumeraire);
			factorLoading1 = volatilityLogNumeraire * rho;
			factorLoading2 = volatilityLogNumeraire * Math.sqrt(1.0-rho*rho);
		}
		else {
			throw new IllegalArgumentException();
		}

		RandomVariableInterface factorLoading1RV = getProcess().getStochasticDriver().getRandomVariableForConstant(factorLoading1);
		RandomVariableInterface factorLoading2RV = getProcess().getStochasticDriver().getRandomVariableForConstant(factorLoading2);

		return new RandomVariableInterface[] { factorLoading1RV, factorLoading2RV };
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.model.AbstractModelInterface#getRandomVariableForConstant(double)
	 */
	@Override
	public RandomVariableInterface getRandomVariableForConstant(double value) {
		return getProcess().getStochasticDriver().getRandomVariableForConstant(value);
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
	public LIBORModelInterface getCloneWithModifiedData(Map<String, Object> dataModified) throws CalculationException {
		throw new UnsupportedOperationException();
	}

	private RandomVariableInterface getShortRate(int timeIndex) throws CalculationException {
		double time = getProcess().getTime(timeIndex);
		double timePrev = timeIndex > 0 ? getProcess().getTime(timeIndex-1) : time;
		double timeNext = getProcess().getTime(timeIndex+1);

		double zeroRate = -Math.log(discountCurveFromForwardCurve.getDiscountFactor(curveModel, timeNext)/discountCurveFromForwardCurve.getDiscountFactor(curveModel, time)) / (timeNext-time);

		double alpha = zeroRate + getDV(0, time);

		RandomVariableInterface value = getProcess().getProcessValue(timeIndex, 0);
		value = value.add(alpha);

		return value;
	}

	private RandomVariableInterface getZeroCouponBond(double time, double maturity) throws CalculationException {
		int timeIndex = getProcess().getTimeIndex(time);
		if(timeIndex < 0) {
			int timeIndexLo = -timeIndex-1-1;
			double timeLo = getProcess().getTime(timeIndexLo);
			return getZeroCouponBond(timeLo, maturity).mult(getShortRate(timeIndexLo).mult(time-timeLo).exp());
		}
		RandomVariableInterface shortRate = getShortRate(timeIndex);
		double A = getA(time, maturity);
		double B = getB(time, maturity);
		return shortRate.mult(-B).exp().mult(A);
	}

	/**
	 * This is the shift alpha of the process, which essentially represents
	 * the integrated drift of the short rate (without the interest rate curve related part).
	 * 
	 * @param timeIndex Time index associated with the time discretization obtained from <code>getProcess</code>
	 * @return The integrated drift (integrating from 0 to getTime(timeIndex)).
	 */
	private double getIntegratedDriftAdjustment(int timeIndex) {
		double integratedDriftAdjustment = 0;
		for(int i=1; i<=timeIndex; i++) {
			double t = getProcess().getTime(i-1);
			double t2 = getProcess().getTime(i);

			int timeIndexVolatilityModel = volatilityModel.getTimeDiscretization().getTimeIndex(t);
			if(timeIndexVolatilityModel < 0)
			 {
				timeIndexVolatilityModel = -timeIndexVolatilityModel-2;	// Get timeIndex corresponding to previous point
			}
			double meanReversion = volatilityModel.getMeanReversion(timeIndexVolatilityModel);

			integratedDriftAdjustment += getShortRateConditionalVariance(0, t) * getB(t,t2)/(t2-t) * (t2-t) - integratedDriftAdjustment * meanReversion * (t2-t) * getB(t,t2)/(t2-t);
		}
		return integratedDriftAdjustment;
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
	private double getA(double time, double maturity) {
		int timeIndex = getProcess().getTimeIndex(time);
		double timeStep = getProcess().getTimeDiscretization().getTimeStep(timeIndex);

		double dt = timeStep;
		double zeroRate = -Math.log(discountCurveFromForwardCurve.getDiscountFactor(curveModel, time+dt)/discountCurveFromForwardCurve.getDiscountFactor(curveModel, time)) / dt;

		double B = getB(time,maturity);

		double lnA = Math.log(discountCurveFromForwardCurve.getDiscountFactor(curveModel, maturity)/discountCurveFromForwardCurve.getDiscountFactor(curveModel, time))
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
	private double getMRTime(double time, double maturity) {
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
			double meanReversion = volatilityModel.getMeanReversion(timeIndex-1);
			integral += meanReversion*(timeNext-timePrev);
			timePrev = timeNext;
		}
		timeNext = maturity;
		double meanReversion = volatilityModel.getMeanReversion(timeIndexEnd);
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
	private double getB(double time, double maturity) {
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
			double meanReversion = volatilityModel.getMeanReversion(timeIndex-1);
			integral += (Math.exp(-getMRTime(timeNext,maturity)) - Math.exp(-getMRTime(timePrev,maturity)))/meanReversion;
			timePrev = timeNext;
		}
		double meanReversion = volatilityModel.getMeanReversion(timeIndexEnd);
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
	private double getV(double time, double maturity) {
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
			double meanReversion = volatilityModel.getMeanReversion(timeIndex-1);
			double volatility = volatilityModel.getVolatility(timeIndex-1);
			integral += volatility * volatility * (timeNext-timePrev)/(meanReversion*meanReversion);
			integral -= volatility * volatility * 2 * (Math.exp(- getMRTime(timeNext,maturity))-Math.exp(- getMRTime(timePrev,maturity))) / (meanReversion*meanReversion*meanReversion);
			integral += volatility * volatility * (Math.exp(- 2 * getMRTime(timeNext,maturity))-Math.exp(- 2 * getMRTime(timePrev,maturity))) / (2 * meanReversion*meanReversion*meanReversion);
			timePrev = timeNext;
		}
		timeNext = maturity;
		double meanReversion = volatilityModel.getMeanReversion(timeIndexEnd);
		double volatility = volatilityModel.getVolatility(timeIndexEnd);
		integral += volatility * volatility * (timeNext-timePrev)/(meanReversion*meanReversion);
		integral -= volatility * volatility * 2 * (Math.exp(- getMRTime(timeNext,maturity))-Math.exp(- getMRTime(timePrev,maturity))) / (meanReversion*meanReversion*meanReversion);
		integral += volatility * volatility * (Math.exp(- 2 * getMRTime(timeNext,maturity))-Math.exp(- 2 * getMRTime(timePrev,maturity))) / (2 * meanReversion*meanReversion*meanReversion);

		return integral;
	}

	private double getDV(double time, double maturity) {
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
			double meanReversion = volatilityModel.getMeanReversion(timeIndex-1);
			double volatility = volatilityModel.getVolatility(timeIndex-1);
			integral += volatility * volatility * (Math.exp(- getMRTime(timeNext,maturity))-Math.exp(- getMRTime(timePrev,maturity))) / (meanReversion*meanReversion);
			integral -= volatility * volatility * (Math.exp(- 2 * getMRTime(timeNext,maturity))-Math.exp(- 2 * getMRTime(timePrev,maturity))) / (2 * meanReversion*meanReversion);
			timePrev = timeNext;
		}
		timeNext = maturity;
		double meanReversion = volatilityModel.getMeanReversion(timeIndexEnd);
		double volatility = volatilityModel.getVolatility(timeIndexEnd);
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
	public double getShortRateConditionalVariance(double time, double maturity) {
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
			double meanReversion = volatilityModel.getMeanReversion(timeIndex-1);
			double volatility = volatilityModel.getVolatility(timeIndex-1);
			integral += volatility * volatility * (Math.exp(-2 * getMRTime(timeNext,maturity))-Math.exp(-2 * getMRTime(timePrev,maturity))) / (2*meanReversion);
			timePrev = timeNext;
		}
		timeNext = maturity;
		double meanReversion = volatilityModel.getMeanReversion(timeIndexEnd);
		double volatility = volatilityModel.getVolatility(timeIndexEnd);
		integral += volatility * volatility * (Math.exp(-2 * getMRTime(timeNext,maturity))-Math.exp(-2 * getMRTime(timePrev,maturity))) / (2*meanReversion);

		return integral;
	}

	public double getIntegratedBondSquaredVolatility(double time, double maturity) {
		return getShortRateConditionalVariance(0, time) * getB(time,maturity) * getB(time,maturity);
	}
}


