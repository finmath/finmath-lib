/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2004
 */
package net.finmath.montecarlo.interestrate.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.DiscountCurveFromForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.montecarlo.AbstractRandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.interestrate.CalibrationProduct;
import net.finmath.montecarlo.interestrate.LIBORModel;
import net.finmath.montecarlo.interestrate.ShortRateModel;
import net.finmath.montecarlo.interestrate.models.covariance.ShortRateVolatilityModelCalibrateable;
import net.finmath.montecarlo.interestrate.models.covariance.ShortRateVolatilityModel;
import net.finmath.montecarlo.interestrate.models.covariance.ShortRateVolatilityModelParametric;
import net.finmath.montecarlo.model.AbstractProcessModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
import net.finmath.time.TimeDiscretization;

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
 * {@link net.finmath.montecarlo.interestrate.models.covariance.ShortRateVolatilityModel}.
 *
 * @see net.finmath.montecarlo.interestrate.models.covariance.ShortRateVolatilityModel
 * @see <a href="http://ssrn.com/abstract=2737091">ssrn.com/abstract=2737091</a>
 *
 * @author Christian Fries
 * @version 1.4
 */
public class HullWhiteModel extends AbstractProcessModel implements ShortRateModel, LIBORModel, Serializable {

	private static final long serialVersionUID = 8677410149401310062L;

	private final TimeDiscretization		liborPeriodDiscretization;

	private String							forwardCurveName;
	private AnalyticModel			curveModel;

	private ForwardCurve			forwardRateCurve;
	private DiscountCurve			discountCurve;
	private DiscountCurve			discountCurveFromForwardCurve;

	private final AbstractRandomVariableFactory	randomVariableFactory;

	private final ShortRateVolatilityModel volatilityModel;

	private final Map<String, ?>			properties;

	private final List<RandomVariable> discountFactorCache = new ArrayList<>();
	private final List<RandomVariable> discountFactorForForwardCurveCache = new ArrayList<>();;

	/**
	 * Creates a Hull-White model which implements <code>LIBORMarketModel</code>.
	 *
	 * @param randomVariableFactory The factory to be used to construct random variables.
	 * @param liborPeriodDiscretization The forward rate discretization to be used in the <code>getLIBOR</code> method.
	 * @param analyticModel The analytic model to be used (currently not used, may be null).
	 * @param forwardRateCurve The forward curve to be used (currently not used, - the model uses disocuntCurve only.
	 * @param discountCurve The disocuntCurve (currently also used to determine the forward curve).
	 * @param volatilityModel The volatility model specifying mean reversion and instantaneous volatility of the short rate.
	 * @param properties A map specifying model properties (currently not used, may be null).
	 */
	public HullWhiteModel(
			AbstractRandomVariableFactory		randomVariableFactory,
			TimeDiscretization			liborPeriodDiscretization,
			AnalyticModel				analyticModel,
			ForwardCurve				forwardRateCurve,
			DiscountCurve				discountCurve,
			ShortRateVolatilityModel	volatilityModel,
			Map<String, ?>						properties
			) {

		this.randomVariableFactory		= randomVariableFactory;
		this.liborPeriodDiscretization	= liborPeriodDiscretization;
		this.curveModel					= analyticModel;
		this.forwardRateCurve	= forwardRateCurve;
		this.discountCurve		= discountCurve;
		this.volatilityModel	= volatilityModel;
		this.properties			= null;//properties;		// Note: if properties are stored, this may cause issues in serialization. Field will be removed.

		this.discountCurveFromForwardCurve = new DiscountCurveFromForwardCurve(forwardRateCurve);
	}

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
	public HullWhiteModel(
			TimeDiscretization			liborPeriodDiscretization,
			AnalyticModel				analyticModel,
			ForwardCurve				forwardRateCurve,
			DiscountCurve				discountCurve,
			ShortRateVolatilityModel	volatilityModel,
			Map<String, ?>						properties
			) {
		this(new RandomVariableFactory(), liborPeriodDiscretization, analyticModel, forwardRateCurve, discountCurve, volatilityModel, properties);
	}


	/**
	 * Creates a Hull-White model which implements <code>LIBORMarketModel</code>.
	 *
	 * @param randomVariableFactory The randomVariableFactory
	 * @param liborPeriodDiscretization The forward rate discretization to be used in the <code>getLIBOR</code> method.
	 * @param analyticModel The analytic model to be used (currently not used, may be null).
	 * @param forwardRateCurve The forward curve to be used (currently not used, - the model uses disocuntCurve only.
	 * @param discountCurve The disocuntCurve (currently also used to determine the forward curve).
	 * @param volatilityModel The volatility model specifying mean reversion and instantaneous volatility of the short rate.
	 * @param calibrationProducts The products to be used for calibration
	 * @param properties The calibration properties
	 * @return A (possibly calibrated) Hull White model.
	 * @throws CalculationException Thrown if calibration fails.
	 */
	public static HullWhiteModel of(
			AbstractRandomVariableFactory		randomVariableFactory,
			TimeDiscretization			liborPeriodDiscretization,
			AnalyticModel				analyticModel,
			ForwardCurve				forwardRateCurve,
			DiscountCurve				discountCurve,
			ShortRateVolatilityModel	volatilityModel,
			CalibrationProduct[]					calibrationProducts,
			Map<String, Object>					properties
			) throws CalculationException {

		HullWhiteModel model = new HullWhiteModel(randomVariableFactory, liborPeriodDiscretization, analyticModel, forwardRateCurve, discountCurve, volatilityModel, properties);

		// Perform calibration, if data is given
		if(calibrationProducts != null && calibrationProducts.length > 0) {
			ShortRateVolatilityModelCalibrateable volatilityModelParametric = null;
			try {
				volatilityModelParametric = (ShortRateVolatilityModelCalibrateable)volatilityModel;
			}
			catch(Exception e) {
				throw new ClassCastException("Calibration restricted to covariance models implementing HullWhiteModelCalibrateable.");
			}

			Map<String,Object> calibrationParameters = null;
			if(properties != null && properties.containsKey("calibrationParameters")) {
				calibrationParameters	= (Map<String,Object>)properties.get("calibrationParameters");
			}

			ShortRateVolatilityModelCalibrateable volatilityModelCalibrated = volatilityModelParametric.getCloneCalibrated(model, calibrationProducts, calibrationParameters);

			HullWhiteModel modelCalibrated = model.getCloneWithModifiedVolatilityModel(volatilityModelCalibrated);

			return modelCalibrated;
		}
		else {
			return model;
		}
	}

	@Override
	public LocalDateTime getReferenceDate() {
		return LocalDateTime.of(discountCurve.getReferenceDate(), LocalTime.of(0, 0));
	}

	@Override
	public int getNumberOfComponents() {
		return 2;
	}

	@Override
	public RandomVariable applyStateSpaceTransform(int componentIndex, RandomVariable randomVariable) {
		return randomVariable;
	}

	@Override
	public RandomVariable applyStateSpaceTransformInverse(int componentIndex, RandomVariable randomVariable) {
		return randomVariable;
	}

	@Override
	public RandomVariable[] getInitialState() {
		// Initial value is zero - BrownianMotion serves as a factory here.
		RandomVariable zero = getProcess().getStochasticDriver().getRandomVariableForConstant(0.0);
		return new RandomVariable[] { zero, zero };
	}

	@Override
	public RandomVariable getNumeraire(double time) throws CalculationException {
		if(time == getTime(0)) {
			// Initial value of numeraire is one - BrownianMotion serves as a factory here.
			RandomVariable one = getProcess().getStochasticDriver().getRandomVariableForConstant(1.0);
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

		RandomVariable logNum = getProcessValue(timeIndex, 1).add(getV(0,time).mult(0.5));
		RandomVariable discountFactorFromForwardCurve =  getDiscountFactorFromForwarCurve(timeIndex);
		RandomVariable numeraire = logNum.exp().div(discountFactorFromForwardCurve);

		/*
		 * Adjust for discounting, i.e. funding or collateralization
		 */
		if(discountCurve != null) {
			// This includes a control for zero bonds
			RandomVariable discountFactor =  getDiscountFactor(timeIndex);
			RandomVariable deterministicNumeraireAdjustment = numeraire.invert().average().div(discountFactor);
			numeraire = numeraire.mult(deterministicNumeraireAdjustment);
		}

		return numeraire;
	}

	@Override
	public RandomVariable[] getDrift(int timeIndex, RandomVariable[] realizationAtTimeIndex, RandomVariable[] realizationPredictor) {

		double time = getProcess().getTime(timeIndex);
		double timeNext = getProcess().getTime(timeIndex+1);

		int timeIndexVolatility = volatilityModel.getTimeDiscretization().getTimeIndex(time);
		if(timeIndexVolatility < 0) {
			timeIndexVolatility = -timeIndexVolatility-2;
		}
		RandomVariable meanReversion = volatilityModel.getMeanReversion(timeIndexVolatility);

		RandomVariable driftShortRate		= realizationAtTimeIndex[0].mult(meanReversion.mult(getB(time,timeNext).div(-1*(timeNext-time))));
		RandomVariable driftLogNumeraire	= realizationAtTimeIndex[0].mult(getB(time,timeNext).div(timeNext-time));

		return new RandomVariable[] { driftShortRate, driftLogNumeraire };
	}

	@Override
	public RandomVariable[] getFactorLoading(int timeIndex, int componentIndex, RandomVariable[] realizationAtTimeIndex) {
		double time = getProcess().getTime(timeIndex);
		double timeNext = getProcess().getTime(timeIndex+1);

		int timeIndexVolatility = volatilityModel.getTimeDiscretization().getTimeIndex(time);
		if(timeIndexVolatility < 0) {
			timeIndexVolatility = -timeIndexVolatility-2;
		}

		RandomVariable meanReversion = volatilityModel.getMeanReversion(timeIndexVolatility);
		RandomVariable meanReversionTimesTime = meanReversion.mult(-2.0 * (timeNext-time));
		// double scaling = Math.sqrt((1.0-Math.exp(-2.0 * meanReversion * (timeNext-time)))/(2.0 * meanReversion * (timeNext-time)));
		RandomVariable scaling = meanReversionTimesTime.exp().sub(1.0).div(meanReversionTimesTime).sqrt();

		RandomVariable volatilityEffective = scaling.mult(volatilityModel.getVolatility(timeIndexVolatility));

		RandomVariable factorLoading1, factorLoading2;
		if(componentIndex == 0) {
			// Factor loadings for the short rate driver.
			factorLoading1 = volatilityEffective;
			factorLoading2 = new Scalar(0.0);
		}
		else if(componentIndex == 1) {
			// Factor loadings for the numeraire driver.
			RandomVariable volatilityLogNumeraire = getV(time,timeNext).div(timeNext-time).sqrt();
			RandomVariable rho = getDV(time,timeNext).div(timeNext-time).div(volatilityEffective.mult(volatilityLogNumeraire));
			factorLoading1 = volatilityLogNumeraire.mult(rho);
			factorLoading2 = volatilityLogNumeraire.mult(rho.squared().sub(1).mult(-1).sqrt());
		}
		else {
			throw new IllegalArgumentException();
		}

		return new RandomVariable[] { factorLoading1, factorLoading2 };
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.model.ProcessModel#getRandomVariableForConstant(double)
	 */
	@Override
	public RandomVariable getRandomVariableForConstant(double value) {
		return getProcess().getStochasticDriver().getRandomVariableForConstant(value);
	}

	@Override
	public RandomVariable getLIBOR(double time, double periodStart, double periodEnd) throws CalculationException
	{
		return getZeroCouponBond(time, periodStart).div(getZeroCouponBond(time, periodEnd)).sub(1.0).div(periodEnd-periodStart);
	}

	@Override
	public RandomVariable getLIBOR(int timeIndex, int liborIndex) throws CalculationException {
		return getZeroCouponBond(getProcess().getTime(timeIndex), getLiborPeriod(liborIndex)).div(getZeroCouponBond(getProcess().getTime(timeIndex), getLiborPeriod(liborIndex+1))).sub(1.0).div(getLiborPeriodDiscretization().getTimeStep(liborIndex));
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
	public double getLiborPeriod(int timeIndex) {
		return liborPeriodDiscretization.getTime(timeIndex);
	}

	@Override
	public int getLiborPeriodIndex(double time) {
		return liborPeriodDiscretization.getTimeIndex(time);
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
	public LIBORModel getCloneWithModifiedData(Map<String, Object> dataModified) {
		throw new UnsupportedOperationException();
	}

	private RandomVariable getShortRate(int timeIndex) throws CalculationException {
		double time = getProcess().getTime(timeIndex);
		double timePrev = timeIndex > 0 ? getProcess().getTime(timeIndex-1) : time;
		double timeNext = getProcess().getTime(timeIndex+1);

		RandomVariable zeroRate = getDiscountFactorFromForwarCurve(time).div(getDiscountFactorFromForwarCurve(timeNext)).log().div(timeNext-time);

		RandomVariable alpha = getDV(0, time).add(zeroRate);

		RandomVariable value = getProcess().getProcessValue(timeIndex, 0);
		value = value.add(alpha);

		return value;
	}

	private RandomVariable getZeroCouponBond(double time, double maturity) throws CalculationException {
		int timeIndex = getProcess().getTimeIndex(time);
		if(timeIndex < 0) {
			int timeIndexLo = -timeIndex-1-1;
			double timeLo = getProcess().getTime(timeIndexLo);
			return getZeroCouponBond(timeLo, maturity).mult(getShortRate(timeIndexLo).mult(time-timeLo).exp());
		}
		RandomVariable shortRate = getShortRate(timeIndex);
		RandomVariable A = getA(time, maturity);
		RandomVariable B = getB(time, maturity);
		return shortRate.mult(B.mult(-1)).exp().mult(A);
	}

	/**
	 * This is the shift alpha of the process, which essentially represents
	 * the integrated drift of the short rate (without the interest rate curve related part).
	 *
	 * @param timeIndex Time index associated with the time discretization obtained from <code>getProcess</code>
	 * @return The integrated drift (integrating from 0 to getTime(timeIndex)).
	 */
	private RandomVariable getIntegratedDriftAdjustment(int timeIndex) {
		RandomVariable integratedDriftAdjustment = new Scalar(0.0);
		for(int i=1; i<=timeIndex; i++) {
			double t = getProcess().getTime(i-1);
			double t2 = getProcess().getTime(i);

			int timeIndexVolatilityModel = volatilityModel.getTimeDiscretization().getTimeIndex(t);
			if(timeIndexVolatilityModel < 0) {
				timeIndexVolatilityModel = -timeIndexVolatilityModel-2;	// Get timeIndex corresponding to previous point
			}
			RandomVariable meanReversion = volatilityModel.getMeanReversion(timeIndexVolatilityModel);

			integratedDriftAdjustment = integratedDriftAdjustment.add(getShortRateConditionalVariance(0, t).mult(getB(t,t2))).sub(integratedDriftAdjustment.mult(meanReversion.mult(getB(t,t2))));
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
	private RandomVariable getA(double time, double maturity) {
		int timeIndex = getProcess().getTimeIndex(time);
		double timeStep = getProcess().getTimeDiscretization().getTimeStep(timeIndex);

		RandomVariable zeroRate = getDiscountFactorFromForwarCurve(timeIndex).div(getDiscountFactorFromForwarCurve(timeIndex+1)).log().div(timeStep);

		RandomVariable forwardBond = getDiscountFactorFromForwarCurve(maturity).div(getDiscountFactorFromForwarCurve(time)).log();

		RandomVariable B = getB(time,maturity);

		RandomVariable lnA = B.mult(zeroRate).sub(B.squared().mult(getShortRateConditionalVariance(0,time).div(2))).add(forwardBond);

		return lnA.exp();
	}

	/**
	 * Calculates \( \int_{t}^{T} a(s) \mathrm{d}s \), where \( a \) is the mean reversion parameter.
	 *
	 * @param time The parameter t.
	 * @param maturity The parameter T.
	 * @return The value of \( \int_{t}^{T} a(s) \mathrm{d}s \).
	 */
	private RandomVariable getMRTime(double time, double maturity) {
		int timeIndexStart = volatilityModel.getTimeDiscretization().getTimeIndex(time);
		if(timeIndexStart < 0) {
			timeIndexStart = -timeIndexStart-1;	// Get timeIndex corresponding to next point
		}

		int timeIndexEnd =volatilityModel.getTimeDiscretization().getTimeIndex(maturity);
		if(timeIndexEnd < 0) {
			timeIndexEnd = -timeIndexEnd-2;	// Get timeIndex corresponding to previous point
		}

		RandomVariable integral = new Scalar(0.0);
		double timePrev = time;
		double timeNext;
		for(int timeIndex=timeIndexStart+1; timeIndex<=timeIndexEnd; timeIndex++) {
			timeNext = volatilityModel.getTimeDiscretization().getTime(timeIndex);
			RandomVariable meanReversion = volatilityModel.getMeanReversion(timeIndex-1);
			integral = integral.add(meanReversion.mult(timeNext-timePrev));
			timePrev = timeNext;
		}
		timeNext = maturity;
		RandomVariable meanReversion = volatilityModel.getMeanReversion(timeIndexEnd);
		integral = integral.add(meanReversion.mult(timeNext-timePrev));

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
	private RandomVariable getB(double time, double maturity) {
		int timeIndexStart = volatilityModel.getTimeDiscretization().getTimeIndex(time);
		if(timeIndexStart < 0) {
			timeIndexStart = -timeIndexStart-1;	// Get timeIndex corresponding to next point
		}

		int timeIndexEnd =volatilityModel.getTimeDiscretization().getTimeIndex(maturity);
		if(timeIndexEnd < 0) {
			timeIndexEnd = -timeIndexEnd-2;	// Get timeIndex corresponding to previous point
		}

		RandomVariable integral = new Scalar(0.0);
		double timePrev = time;
		double timeNext;
		for(int timeIndex=timeIndexStart+1; timeIndex<=timeIndexEnd; timeIndex++) {
			timeNext = volatilityModel.getTimeDiscretization().getTime(timeIndex);
			RandomVariable meanReversion = volatilityModel.getMeanReversion(timeIndex-1);
			integral = integral.add(
					getMRTime(timeNext,maturity).mult(-1.0).exp().sub(
							getMRTime(timePrev,maturity).mult(-1.0).exp()).div(meanReversion));
			timePrev = timeNext;
		}
		RandomVariable meanReversion = volatilityModel.getMeanReversion(timeIndexEnd);
		timeNext = maturity;
		integral = integral.add(
				getMRTime(timeNext,maturity).mult(-1.0).exp().sub(
						getMRTime(timePrev,maturity).mult(-1.0).exp()).div(meanReversion));

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
	private RandomVariable getV(double time, double maturity) {
		if(time ==  maturity) {
			return new Scalar(0.0);
		}
		int timeIndexStart = volatilityModel.getTimeDiscretization().getTimeIndex(time);
		if(timeIndexStart < 0) {
			timeIndexStart = -timeIndexStart-1;	// Get timeIndex corresponding to next point
		}

		int timeIndexEnd =volatilityModel.getTimeDiscretization().getTimeIndex(maturity);
		if(timeIndexEnd < 0) {
			timeIndexEnd = -timeIndexEnd-2;	// Get timeIndex corresponding to previous point
		}

		RandomVariable integral = new Scalar(0.0);
		double timePrev = time;
		double timeNext;
		RandomVariable expMRTimePrev = getMRTime(timePrev,maturity).mult(-1).exp();
		for(int timeIndex=timeIndexStart+1; timeIndex<=timeIndexEnd; timeIndex++) {
			timeNext = volatilityModel.getTimeDiscretization().getTime(timeIndex);
			RandomVariable meanReversion = volatilityModel.getMeanReversion(timeIndex-1);
			RandomVariable volatility = volatilityModel.getVolatility(timeIndex-1);
			RandomVariable volatilityPerMeanReversionSquared = volatility.squared().div(meanReversion.squared());
			RandomVariable expMRTimeNext = getMRTime(timeNext,maturity).mult(-1).exp();
			integral = integral.add(volatilityPerMeanReversionSquared.mult(
					expMRTimeNext.sub(expMRTimePrev).mult(-2).div(meanReversion)
					.add( expMRTimeNext.squared().sub(expMRTimePrev.squared()).div(meanReversion).div(2.0))
					.add(timeNext-timePrev)
					));
			timePrev = timeNext;
			expMRTimePrev = expMRTimeNext;
		}
		timeNext = maturity;
		RandomVariable meanReversion = volatilityModel.getMeanReversion(timeIndexEnd);
		RandomVariable volatility = volatilityModel.getVolatility(timeIndexEnd);
		RandomVariable volatilityPerMeanReversionSquared = volatility.squared().div(meanReversion.squared());
		RandomVariable expMRTimeNext = getMRTime(timeNext,maturity).mult(-1).exp();
		integral = integral.add(volatilityPerMeanReversionSquared.mult(
				expMRTimeNext.sub(expMRTimePrev).mult(-2).div(meanReversion)
				.add( expMRTimeNext.squared().sub(expMRTimePrev.squared()).div(meanReversion).div(2.0))
				.add(timeNext-timePrev)
				));

		return integral;
	}

	private RandomVariable getDV(double time, double maturity) {
		if(time==maturity) {
			return new Scalar(0.0);
		}
		int timeIndexStart = volatilityModel.getTimeDiscretization().getTimeIndex(time);
		if(timeIndexStart < 0) {
			timeIndexStart = -timeIndexStart-1;	// Get timeIndex corresponding to next point
		}

		int timeIndexEnd =volatilityModel.getTimeDiscretization().getTimeIndex(maturity);
		if(timeIndexEnd < 0) {
			timeIndexEnd = -timeIndexEnd-2;	// Get timeIndex corresponding to previous point
		}

		RandomVariable integral = new Scalar(0.0);
		double timePrev = time;
		double timeNext;
		RandomVariable expMRTimePrev = getMRTime(timePrev,maturity).mult(-1).exp();
		for(int timeIndex=timeIndexStart+1; timeIndex<=timeIndexEnd; timeIndex++) {
			timeNext = volatilityModel.getTimeDiscretization().getTime(timeIndex);
			RandomVariable meanReversion = volatilityModel.getMeanReversion(timeIndex-1);
			RandomVariable volatility = volatilityModel.getVolatility(timeIndex-1);
			RandomVariable volatilityPerMeanReversionSquared = volatility.squared().div(meanReversion.squared());
			RandomVariable expMRTimeNext = getMRTime(timeNext,maturity).mult(-1).exp();
			integral = integral.add(volatilityPerMeanReversionSquared.mult(
					expMRTimeNext.sub(expMRTimePrev).add(
							expMRTimeNext.squared().sub(expMRTimePrev.squared()).div(-2.0)
							) ));
			timePrev = timeNext;
			expMRTimePrev = expMRTimeNext;
		}
		timeNext = maturity;
		RandomVariable meanReversion = volatilityModel.getMeanReversion(timeIndexEnd);
		RandomVariable volatility = volatilityModel.getVolatility(timeIndexEnd);
		RandomVariable volatilityPerMeanReversionSquared = volatility.squared().div(meanReversion.squared());
		RandomVariable expMRTimeNext = getMRTime(timeNext,maturity).mult(-1).exp();
		integral = integral.add(volatilityPerMeanReversionSquared.mult(
				expMRTimeNext.sub(expMRTimePrev).add(
						expMRTimeNext.squared().sub(expMRTimePrev.squared()).div(-2.0)
						) ));

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
	public RandomVariable getShortRateConditionalVariance(double time, double maturity) {
		int timeIndexStart = volatilityModel.getTimeDiscretization().getTimeIndex(time);
		if(timeIndexStart < 0) {
			timeIndexStart = -timeIndexStart-1;	// Get timeIndex corresponding to next point
		}

		int timeIndexEnd =volatilityModel.getTimeDiscretization().getTimeIndex(maturity);
		if(timeIndexEnd < 0) {
			timeIndexEnd = -timeIndexEnd-2;	// Get timeIndex corresponding to previous point
		}

		RandomVariable integral = new Scalar(0.0);
		double timePrev = time;
		double timeNext;
		RandomVariable expMRTimePrev = getMRTime(timePrev,maturity).mult(-2).exp();
		for(int timeIndex=timeIndexStart+1; timeIndex<=timeIndexEnd; timeIndex++) {
			timeNext = volatilityModel.getTimeDiscretization().getTime(timeIndex);
			RandomVariable meanReversion = volatilityModel.getMeanReversion(timeIndex-1);
			RandomVariable volatility = volatilityModel.getVolatility(timeIndex-1);
			RandomVariable volatilitySquaredPerMeanReversion = volatility.squared().div(meanReversion);
			RandomVariable expMRTimeNext = getMRTime(timeNext,maturity).mult(-2).exp();
			integral = integral.add(volatilitySquaredPerMeanReversion.mult(expMRTimeNext.sub(expMRTimePrev).div(2))
					);
			timePrev = timeNext;
			expMRTimePrev = expMRTimeNext;
		}
		timeNext = maturity;
		RandomVariable meanReversion = volatilityModel.getMeanReversion(timeIndexEnd);
		RandomVariable volatility = volatilityModel.getVolatility(timeIndexEnd);
		RandomVariable volatilitySquaredPerMeanReversion = volatility.squared().div(meanReversion);
		RandomVariable expMRTimeNext = getMRTime(timeNext,maturity).mult(-2).exp();
		integral = integral.add(volatilitySquaredPerMeanReversion.mult(expMRTimeNext.sub(expMRTimePrev).div(2))
				);

		return integral;
	}

	public RandomVariable getIntegratedBondSquaredVolatility(double time, double maturity) {
		return getShortRateConditionalVariance(0, time).mult(getB(time,maturity).squared());
	}

	@Override
	public HullWhiteModel getCloneWithModifiedVolatilityModel(ShortRateVolatilityModel volatilityModel) {
		return new HullWhiteModel(randomVariableFactory, liborPeriodDiscretization, curveModel, forwardRateCurve, discountCurve, volatilityModel, properties);
	}

	@Override
	public ShortRateVolatilityModel getVolatilityModel() {
		return volatilityModel;
	}

	@Override
	public Map<String, RandomVariable> getModelParameters() {
		Map<String, RandomVariable> modelParameters = new TreeMap<>();

		// Add initial values
		for(int timeIndex=0; timeIndex<getTimeDiscretization().getNumberOfTimes(); timeIndex++) {
			modelParameters.put("FORWARDCURVEDISCOUNTFACTOR("+ getTime(timeIndex) + ")", getDiscountFactorFromForwarCurve(timeIndex));
		}

		// Add volatilities
		if(volatilityModel instanceof ShortRateVolatilityModelParametric) {
			RandomVariable[] parameters = ((ShortRateVolatilityModelParametric) volatilityModel).getParameter();

			for(int volatilityModelParameterIndex=0; volatilityModelParameterIndex<parameters.length; volatilityModelParameterIndex++) {
				modelParameters.put("VOLATILITYMODELPARAMETER("+ volatilityModelParameterIndex + ")", parameters[volatilityModelParameterIndex]);
			}
		}

		// Add numeraire adjustments
		for(int timeIndex=0; timeIndex<getTimeDiscretization().getNumberOfTimes(); timeIndex++) {
			modelParameters.put("DISCOUNTFACTOR("+ getTime(timeIndex) + ")", getDiscountFactor(timeIndex));
		}

		return modelParameters;
	}

	private RandomVariable getDiscountFactor(int timeIndex) {
		synchronized(discountFactorCache) {
			if(discountFactorCache.size() <= timeIndex+1) {
				// Initialize cache
				for(int i=discountFactorCache.size(); i<= timeIndex; i++) {
					double df = discountCurve.getDiscountFactor(curveModel, getTime(i));
					RandomVariable dfAsRandomVariable = new Scalar(df);//randomVariableFactory.createRandomVariable(df);
					discountFactorCache.add(dfAsRandomVariable);
				}
			}

		}

		return discountFactorCache.get(timeIndex);
	}

	private RandomVariable getDiscountFactorFromForwarCurve(double time) {
		int timeIndex = getProcess().getTimeIndex(time);
		if(timeIndex >= 0) return getDiscountFactorFromForwarCurve(timeIndex);
		else throw new UnsupportedOperationException("Interpolation of forward curve currently not supported.");
	}

	private RandomVariable getDiscountFactorFromForwarCurve(int timeIndex) {
		synchronized(discountFactorForForwardCurveCache) {
			if(discountFactorForForwardCurveCache.size() <= timeIndex+1) {
				// Initialize cache
				for(int i=discountFactorForForwardCurveCache.size(); i<=timeIndex; i++) {
					double df = discountCurve.getDiscountFactor(curveModel, getTime(i));
					RandomVariable dfAsRandomVariable = new Scalar(df);//randomVariableFactory.createRandomVariable(df);
					discountFactorForForwardCurveCache.add(dfAsRandomVariable);
				}
			}

		}

		return discountFactorForForwardCurveCache.get(timeIndex);
	}
}
