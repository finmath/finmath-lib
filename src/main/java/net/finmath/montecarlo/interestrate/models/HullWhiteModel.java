/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2004
 */
package net.finmath.montecarlo.interestrate.models;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.DiscountCurveFromForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFromArrayFactory;
import net.finmath.montecarlo.interestrate.CalibrationProduct;
import net.finmath.montecarlo.interestrate.LIBORModel;
import net.finmath.montecarlo.interestrate.ShortRateModel;
import net.finmath.montecarlo.interestrate.models.covariance.ShortRateVolatilityModel;
import net.finmath.montecarlo.interestrate.models.covariance.ShortRateVolatilityModelCalibrateable;
import net.finmath.montecarlo.interestrate.models.covariance.ShortRateVolatilityModelParametric;
import net.finmath.montecarlo.model.AbstractProcessModel;
import net.finmath.montecarlo.process.MonteCarloProcess;
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
 * This implementation supports different method for the interpolation of the curves.
 * The property <code>"isInterpolateDiscountFactorsOnLiborPeriodDiscretization"</code> is a boolean. If true, the
 * given curves are used only at the discretization points given by <code>liborPeriodDiscretization</code>.
 * This implies that the model reports only a limited set of risk factors in the methods {@link HullWhiteModel#getModelParameters()}.
 *
 * @see net.finmath.montecarlo.interestrate.models.covariance.ShortRateVolatilityModel
 * @see <a href="http://ssrn.com/abstract=2737091">ssrn.com/abstract=2737091</a>
 *
 * @author Christian Fries
 * @version 1.4
 */
public class HullWhiteModel extends AbstractProcessModel implements ShortRateModel, LIBORModel, Serializable {

	private static final long serialVersionUID = 8677410149401310062L;

	private static final Logger logger = Logger.getLogger("net.finmath");

	private final TimeDiscretization		liborPeriodDiscretization;

	private String					forwardCurveName;
	private final AnalyticModel			analyticModel;

	private final ForwardCurve			forwardRateCurve;
	private final DiscountCurve			discountCurve;
	private final DiscountCurve			discountCurveFromForwardCurve;

	private final RandomVariableFactory	randomVariableFactory;

	private final ShortRateVolatilityModel volatilityModel;

	private final Map<String, Object>	properties;

	private final boolean isInterpolateDiscountFactorsOnLiborPeriodDiscretization;

	/*
	 * Cache
	 */
	private transient List<RandomVariable> numeraireDiscountFactors = new ArrayList<>();
	private transient List<RandomVariable> numeraireDiscountFactorForwardRates = new ArrayList<>();
	private transient List<RandomVariable> discountFactorFromForwardCurveCache = new ArrayList<>();
	private transient List<RandomVariable> forwardRateCache = new ArrayList<>();

	/**
	 * Creates a Hull-White model which implements <code>LIBORMarketModel</code>.
	 *
	 * @param randomVariableFactory The factory to be used to construct random variables.
	 * @param liborPeriodDiscretization The forward rate discretization to be used in the <code>getLIBOR</code> method.
	 * @param analyticModel The analytic model to be used (currently not used, may be null).
	 * @param forwardRateCurve The forward curve to be used (currently not used, - the model uses disocuntCurve only.
	 * @param discountCurve The disocuntCurve (currently also used to determine the forward curve).
	 * @param volatilityModel The volatility model specifying mean reversion and instantaneous volatility of the short rate.
	 * @param properties A map specifying model properties.
	 */
	public HullWhiteModel(
			final RandomVariableFactory		randomVariableFactory,
			final TimeDiscretization			liborPeriodDiscretization,
			final AnalyticModel				analyticModel,
			final ForwardCurve				forwardRateCurve,
			final DiscountCurve				discountCurve,
			final ShortRateVolatilityModel	volatilityModel,
			final Map<String, Object>			properties
			) {

		this.randomVariableFactory		= randomVariableFactory;
		this.liborPeriodDiscretization	= liborPeriodDiscretization;
		this.analyticModel					= analyticModel;
		this.forwardRateCurve	= forwardRateCurve;
		this.discountCurve		= discountCurve;
		this.volatilityModel	= volatilityModel;

		this.properties = new HashMap<>();
		if(properties != null) {
			for(final Map.Entry<String,?> property : properties.entrySet()) {
				if(Serializable.class.isAssignableFrom(property.getValue().getClass())) {
					properties.put(property.getKey(), property.getValue());
				}
				else {
					logger.warning("Ignored non serializable property under the key " + property.getKey() + ":" + property.getValue());
				}
			}
		}

		isInterpolateDiscountFactorsOnLiborPeriodDiscretization = (Boolean) this.properties.getOrDefault("isInterpolateDiscountFactorsOnLiborPeriodDiscretization", Boolean.valueOf(true));
		discountCurveFromForwardCurve = new DiscountCurveFromForwardCurve(forwardRateCurve);
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
			final TimeDiscretization			liborPeriodDiscretization,
			final AnalyticModel				analyticModel,
			final ForwardCurve				forwardRateCurve,
			final DiscountCurve				discountCurve,
			final ShortRateVolatilityModel	volatilityModel,
			final Map<String, Object>			properties
			) {
		this(new RandomVariableFromArrayFactory(), liborPeriodDiscretization, analyticModel, forwardRateCurve, discountCurve, volatilityModel, properties);
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
			final RandomVariableFactory		randomVariableFactory,
			final TimeDiscretization			liborPeriodDiscretization,
			final AnalyticModel				analyticModel,
			final ForwardCurve				forwardRateCurve,
			final DiscountCurve				discountCurve,
			final ShortRateVolatilityModel	volatilityModel,
			final CalibrationProduct[]					calibrationProducts,
			final Map<String, Object>					properties
			) throws CalculationException {

		final HullWhiteModel model = new HullWhiteModel(randomVariableFactory, liborPeriodDiscretization, analyticModel, forwardRateCurve, discountCurve, volatilityModel, properties);

		// Perform calibration, if data is given
		if(calibrationProducts != null && calibrationProducts.length > 0) {
			ShortRateVolatilityModelCalibrateable volatilityModelParametric = null;
			try {
				volatilityModelParametric = (ShortRateVolatilityModelCalibrateable)volatilityModel;
			}
			catch(final Exception e) {
				throw new ClassCastException("Calibration restricted to covariance models implementing HullWhiteModelCalibrateable.");
			}

			Map<String,Object> calibrationParameters = null;
			if(properties != null && properties.containsKey("calibrationParameters")) {
				calibrationParameters	= (Map<String,Object>)properties.get("calibrationParameters");
			}

			final ShortRateVolatilityModelCalibrateable volatilityModelCalibrated = volatilityModelParametric.getCloneCalibrated(model, calibrationProducts, calibrationParameters);

			final HullWhiteModel modelCalibrated = model.getCloneWithModifiedVolatilityModel(volatilityModelCalibrated);

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
	public int getNumberOfFactors()
	{
		return 1;
	}

	@Override
	public RandomVariable applyStateSpaceTransform(MonteCarloProcess process, int timeIndex, final int componentIndex, final RandomVariable randomVariable) {
		return randomVariable;
	}

	@Override
	public RandomVariable applyStateSpaceTransformInverse(MonteCarloProcess process, int timeIndex, final int componentIndex, final RandomVariable randomVariable) {
		return randomVariable;
	}

	@Override
	public RandomVariable[] getInitialState(MonteCarloProcess process) {
		// Initial value is zero - BrownianMotion serves as a factory here.
		final RandomVariable zero = getRandomVariableForConstant(0.0);
		return new RandomVariable[] { zero, zero };
	}

	@Override
	public RandomVariable getNumeraire(MonteCarloProcess process, final double time) throws CalculationException {
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
			final double nextTime = process.getTime(previousTimeIndex+1);

			// Log-linear interpolation
			return getNumeraire(process, previousTime).log().mult(nextTime-time)
					.add(getNumeraire(process, nextTime).log().mult(time-previousTime))
					.div(nextTime-previousTime).exp();
		}

		final RandomVariable logNum = process.getProcessValue(timeIndex, 1).add(getV(0,time).mult(0.5));

		RandomVariable numeraireNormalized = logNum.exp();

		// Control variate on zero bond
		numeraireNormalized = numeraireNormalized.mult(numeraireNormalized.invert().getAverage());

		// Apply discount factor scaling
		RandomVariable discountFactor;
		if(discountCurve != null) {
			discountFactor =  getDiscountFactor(process, time).div(getDiscountFactorFromForwardCurve(process, time).getAverage()).mult(getDiscountFactorFromForwardCurve(process, time));
			//			discountFactor =  getDiscountFactor(time);
		}
		else {
			discountFactor =  getDiscountFactorFromForwardCurve(process, time);
		}
		final RandomVariable numeraire = numeraireNormalized.div(discountFactor);

		return numeraire;
	}

	@Override
	public RandomVariable getForwardDiscountBond(final MonteCarloProcess process, final double time, final double maturity) throws CalculationException {
		final RandomVariable inverseForwardBondAsOfTime = getForwardRate(process, time, time, maturity).mult(maturity-time).add(1.0);
		final RandomVariable inverseForwardBondAsOfZero = getForwardRate(process, 0.0, time, maturity).mult(maturity-time).add(1.0);
		final RandomVariable forwardDiscountBondAsOfZero = getDiscountFactor(process, maturity).div(getDiscountFactor(process, time));
		return forwardDiscountBondAsOfZero.mult(inverseForwardBondAsOfZero).div(inverseForwardBondAsOfTime);
	}

	@Override
	public RandomVariable[] getDrift(final MonteCarloProcess process, final int timeIndex, final RandomVariable[] realizationAtTimeIndex, final RandomVariable[] realizationPredictor) {

		final double time = process.getTime(timeIndex);
		final double timeNext = process.getTime(timeIndex+1);

		if(timeNext == time) {
			return new RandomVariable[] { null, null };
		}

		int timeIndexVolatility = volatilityModel.getTimeDiscretization().getTimeIndex(time);
		if(timeIndexVolatility < 0) {
			timeIndexVolatility = -timeIndexVolatility-2;
		}
		final RandomVariable meanReversion = volatilityModel.getMeanReversion(timeIndexVolatility);

		final RandomVariable driftShortRate		= realizationAtTimeIndex[0].mult(meanReversion.mult(getB(time,timeNext).div(-1*(timeNext-time))));
		final RandomVariable driftLogNumeraire	= realizationAtTimeIndex[0].mult(getB(time,timeNext).div(timeNext-time));

		return new RandomVariable[] { driftShortRate, driftLogNumeraire };
	}

	@Override
	public RandomVariable[] getFactorLoading(final MonteCarloProcess process, final int timeIndex, final int componentIndex, final RandomVariable[] realizationAtTimeIndex) {
		final double time = process.getTime(timeIndex);
		final double timeNext = process.getTime(timeIndex+1);

		int timeIndexVolatility = volatilityModel.getTimeDiscretization().getTimeIndex(time);
		if(timeIndexVolatility < 0) {
			timeIndexVolatility = -timeIndexVolatility-2;
		}

		final RandomVariable meanReversion = volatilityModel.getMeanReversion(timeIndexVolatility);
		final RandomVariable meanReversionTimesTime = meanReversion.mult(-2.0 * (timeNext-time));
		// double scaling = Math.sqrt((1.0-Math.exp(-2.0 * meanReversion * (timeNext-time)))/(2.0 * meanReversion * (timeNext-time)));
		final RandomVariable scaling = meanReversionTimesTime.exp().sub(1.0).div(meanReversionTimesTime).sqrt();

		final RandomVariable volatilityEffective = scaling.mult(volatilityModel.getVolatility(timeIndexVolatility));

		RandomVariable factorLoading1, factorLoading2;
		if(componentIndex == 0) {
			// Factor loadings for the short rate driver.
			factorLoading1 = volatilityEffective;
			factorLoading2 = new Scalar(0.0);
		}
		else if(componentIndex == 1) {
			// Factor loadings for the numeraire driver.
			final RandomVariable volatilityLogNumeraire = getV(time,timeNext).div(timeNext-time).sqrt();
			final RandomVariable rho = getDV(time,timeNext).div(timeNext-time).div(volatilityEffective.mult(volatilityLogNumeraire));
			factorLoading1 = volatilityLogNumeraire.mult(rho);
			factorLoading2 = volatilityLogNumeraire.mult(rho.squared().sub(1).mult(-1).sqrt());
		}
		else {
			throw new IllegalArgumentException();
		}

		return new RandomVariable[] { factorLoading1, factorLoading2 };
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
	public LIBORModel getCloneWithModifiedData(final Map<String, Object> dataModified) {
		if(dataModified == null) {
			return new HullWhiteModel(randomVariableFactory, liborPeriodDiscretization, analyticModel, forwardRateCurve, discountCurve, volatilityModel, properties);
		}

		final RandomVariableFactory	newRandomVariableFactory	= (RandomVariableFactory) dataModified.getOrDefault("randomVariableFactory", randomVariableFactory);
		final ShortRateVolatilityModel		newVolatilityModel			= (ShortRateVolatilityModel) dataModified.getOrDefault("volatilityModel", volatilityModel);

		return new HullWhiteModel(newRandomVariableFactory, liborPeriodDiscretization, analyticModel, forwardRateCurve, discountCurve, newVolatilityModel, properties);
	}

	/**
	 * Returns the "short rate" from timeIndex to timeIndex+1.
	 *
	 * @param timeIndex The time index (corresponding to {@link MonteCarloProcess#getTime(int)}).
	 * @return The "short rate" from timeIndex to timeIndex+1.
	 * @throws CalculationException Thrown if simulation failed.
	 */
	private RandomVariable getShortRate(final MonteCarloProcess process, final int timeIndex) throws CalculationException {
		final double time = process.getTime(timeIndex);
		final double timePrev = timeIndex > 0 ? process.getTime(timeIndex-1) : time;
		final double timeNext = process.getTime(timeIndex+1);

		final RandomVariable zeroRate = getZeroRateFromForwardCurve(process, time); //getDiscountFactorFromForwardCurve(time).div(getDiscountFactorFromForwardCurve(timeNext)).log().div(timeNext-time);

		final RandomVariable alpha = getDV(0, time);

		RandomVariable value = process.getProcessValue(timeIndex, 0);
		value = value.add(alpha);
		//		value = value.sub(Math.log(value.exp().getAverage()));

		value = value.add(zeroRate);

		return value;
	}

	private RandomVariable getZeroCouponBond(final MonteCarloProcess process, final double time, final double maturity) throws CalculationException {
		final int timeIndex = process.getTimeIndex(time);
		if(timeIndex < 0) {
			final int timeIndexLo = -timeIndex-1-1;
			final double timeLo = process.getTime(timeIndexLo);
			return getZeroCouponBond(process, timeLo, maturity).div(getZeroCouponBond(process, timeLo, time));
		}
		final RandomVariable shortRate = getShortRate(process, timeIndex);
		final RandomVariable A = getA(process, time, maturity);
		final RandomVariable B = getB(time, maturity);
		return shortRate.mult(B.mult(-1)).exp().mult(A);
	}

	/**
	 * This is the shift alpha of the process, which essentially represents
	 * the integrated drift of the short rate (without the interest rate curve related part).
	 *
	 * @param timeIndex Time index associated with the time discretization obtained from <code>getProcess</code>
	 * @return The integrated drift (integrating from 0 to getTime(timeIndex)).
	 */
	private RandomVariable getIntegratedDriftAdjustment(final MonteCarloProcess process, final int timeIndex) {
		RandomVariable integratedDriftAdjustment = new Scalar(0.0);
		for(int i=1; i<=timeIndex; i++) {
			final double t = process.getTime(i-1);
			final double t2 = process.getTime(i);

			int timeIndexVolatilityModel = volatilityModel.getTimeDiscretization().getTimeIndex(t);
			if(timeIndexVolatilityModel < 0) {
				timeIndexVolatilityModel = -timeIndexVolatilityModel-2;	// Get timeIndex corresponding to previous point
			}
			final RandomVariable meanReversion = volatilityModel.getMeanReversion(timeIndexVolatilityModel);

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
	private RandomVariable getA(final MonteCarloProcess process, final double time, final double maturity) {
		final int timeIndex = process.getTimeIndex(time);
		final double timeStep = process.getTimeDiscretization().getTimeStep(timeIndex);

		final RandomVariable zeroRate = getZeroRateFromForwardCurve(process, time); //getDiscountFactorFromForwardCurve(time).div(getDiscountFactorFromForwardCurve(timeNext)).log().div(timeNext-time);

		final RandomVariable forwardBond = getDiscountFactorFromForwardCurve(process, maturity).div(getDiscountFactorFromForwardCurve(process, time)).log();

		final RandomVariable B = getB(time,maturity);

		final RandomVariable lnA = B.mult(zeroRate).sub(B.squared().mult(getShortRateConditionalVariance(0,time).div(2))).add(forwardBond);

		return lnA.exp();
	}

	/**
	 * Calculates \( \int_{t}^{T} a(s) \mathrm{d}s \), where \( a \) is the mean reversion parameter.
	 *
	 * @param time The parameter t.
	 * @param maturity The parameter T.
	 * @return The value of \( \int_{t}^{T} a(s) \mathrm{d}s \).
	 */
	private RandomVariable getMRTime(final double time, final double maturity) {
		int timeIndexStart = volatilityModel.getTimeDiscretization().getTimeIndex(time);
		if(timeIndexStart < 0) {
			timeIndexStart = -timeIndexStart-2;	// Get timeIndex corresponding to previous point
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
			final RandomVariable meanReversion = volatilityModel.getMeanReversion(timeIndex-1);
			integral = integral.add(meanReversion.mult(timeNext-timePrev));
			timePrev = timeNext;
		}
		timeNext = maturity;
		final RandomVariable meanReversion = volatilityModel.getMeanReversion(timeIndexEnd);
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
	private RandomVariable getB(final double time, final double maturity) {
		int timeIndexStart = volatilityModel.getTimeDiscretization().getTimeIndex(time);
		if(timeIndexStart < 0) {
			timeIndexStart = -timeIndexStart-2;	// Get timeIndex corresponding to previous point
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
			final RandomVariable meanReversion = volatilityModel.getMeanReversion(timeIndex-1);
			integral = integral.add(
					getMRTime(timeNext,maturity).mult(-1.0).exp().sub(
							getMRTime(timePrev,maturity).mult(-1.0).exp()).div(meanReversion));
			timePrev = timeNext;
		}
		final RandomVariable meanReversion = volatilityModel.getMeanReversion(timeIndexEnd);
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
	private RandomVariable getV(final double time, final double maturity) {
		if(time ==  maturity) {
			return new Scalar(0.0);
		}
		int timeIndexStart = volatilityModel.getTimeDiscretization().getTimeIndex(time);
		if(timeIndexStart < 0) {
			timeIndexStart = -timeIndexStart-2;	// Get timeIndex corresponding to previous point
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
			final RandomVariable meanReversion = volatilityModel.getMeanReversion(timeIndex-1);
			final RandomVariable volatility = volatilityModel.getVolatility(timeIndex-1);
			final RandomVariable volatilityPerMeanReversionSquared = volatility.squared().div(meanReversion.squared());
			final RandomVariable expMRTimeNext = getMRTime(timeNext,maturity).mult(-1).exp();
			integral = integral.add(volatilityPerMeanReversionSquared.mult(
					expMRTimeNext.sub(expMRTimePrev).mult(-2).div(meanReversion)
					.add( expMRTimeNext.squared().sub(expMRTimePrev.squared()).div(meanReversion).div(2.0))
					.add(timeNext-timePrev)
					));
			timePrev = timeNext;
			expMRTimePrev = expMRTimeNext;
		}
		timeNext = maturity;
		final RandomVariable meanReversion = volatilityModel.getMeanReversion(timeIndexEnd);
		final RandomVariable volatility = volatilityModel.getVolatility(timeIndexEnd);
		final RandomVariable volatilityPerMeanReversionSquared = volatility.squared().div(meanReversion.squared());
		final RandomVariable expMRTimeNext = getMRTime(timeNext,maturity).mult(-1).exp();
		integral = integral.add(volatilityPerMeanReversionSquared.mult(
				expMRTimeNext.sub(expMRTimePrev).mult(-2).div(meanReversion)
				.add( expMRTimeNext.squared().sub(expMRTimePrev.squared()).div(meanReversion).div(2.0))
				.add(timeNext-timePrev)
				));

		return integral;
	}

	private RandomVariable getDV(final double time, final double maturity) {
		if(time==maturity) {
			return new Scalar(0.0);
		}
		int timeIndexStart = volatilityModel.getTimeDiscretization().getTimeIndex(time);
		if(timeIndexStart < 0) {
			timeIndexStart = -timeIndexStart-2;	// Get timeIndex corresponding to next point
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
			final RandomVariable meanReversion = volatilityModel.getMeanReversion(timeIndex-1);
			final RandomVariable volatility = volatilityModel.getVolatility(timeIndex-1);
			final RandomVariable volatilityPerMeanReversionSquared = volatility.squared().div(meanReversion.squared());
			final RandomVariable expMRTimeNext = getMRTime(timeNext,maturity).mult(-1).exp();
			integral = integral.add(volatilityPerMeanReversionSquared.mult(
					expMRTimeNext.sub(expMRTimePrev).add(
							expMRTimeNext.squared().sub(expMRTimePrev.squared()).div(-2.0)
							) ));
			timePrev = timeNext;
			expMRTimePrev = expMRTimeNext;
		}
		timeNext = maturity;
		final RandomVariable meanReversion = volatilityModel.getMeanReversion(timeIndexEnd);
		final RandomVariable volatility = volatilityModel.getVolatility(timeIndexEnd);
		final RandomVariable volatilityPerMeanReversionSquared = volatility.squared().div(meanReversion.squared());
		final RandomVariable expMRTimeNext = getMRTime(timeNext,maturity).mult(-1).exp();
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
	public RandomVariable getShortRateConditionalVariance(final double time, final double maturity) {
		int timeIndexStart = volatilityModel.getTimeDiscretization().getTimeIndex(time);
		if(timeIndexStart < 0) {
			timeIndexStart = -timeIndexStart-2;	// Get timeIndex corresponding to next point
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
			final RandomVariable meanReversion = volatilityModel.getMeanReversion(timeIndex-1);
			final RandomVariable volatility = volatilityModel.getVolatility(timeIndex-1);
			final RandomVariable volatilitySquaredPerMeanReversion = volatility.squared().div(meanReversion);
			final RandomVariable expMRTimeNext = getMRTime(timeNext,maturity).mult(-2).exp();
			integral = integral.add(volatilitySquaredPerMeanReversion.mult(expMRTimeNext.sub(expMRTimePrev).div(2))
					);
			timePrev = timeNext;
			expMRTimePrev = expMRTimeNext;
		}
		timeNext = maturity;
		final RandomVariable meanReversion = volatilityModel.getMeanReversion(timeIndexEnd);
		final RandomVariable volatility = volatilityModel.getVolatility(timeIndexEnd);
		final RandomVariable volatilitySquaredPerMeanReversion = volatility.squared().div(meanReversion);
		final RandomVariable expMRTimeNext = getMRTime(timeNext,maturity).mult(-2).exp();
		integral = integral.add(volatilitySquaredPerMeanReversion.mult(expMRTimeNext.sub(expMRTimePrev).div(2))
				);

		return integral;
	}

	public RandomVariable getIntegratedBondSquaredVolatility(final double time, final double maturity) {
		return getShortRateConditionalVariance(0, time).mult(getB(time,maturity).squared());
	}

	@Override
	public HullWhiteModel getCloneWithModifiedVolatilityModel(final ShortRateVolatilityModel volatilityModel) {
		return new HullWhiteModel(randomVariableFactory, liborPeriodDiscretization, analyticModel, forwardRateCurve, discountCurve, volatilityModel, properties);
	}

	@Override
	public ShortRateVolatilityModel getVolatilityModel() {
		return volatilityModel;
	}

	@Override
	public Map<String, RandomVariable> getModelParameters() {
		// TODO Will remember last used process as a chache.
		final MonteCarloProcess process = null;

		final Map<String, RandomVariable> modelParameters = new TreeMap<>();

		final TimeDiscretization timeDiscretizationForCurves = isInterpolateDiscountFactorsOnLiborPeriodDiscretization ? liborPeriodDiscretization : process.getTimeDiscretization();

		// Add initial values
		for(int timeIndex=0; timeIndex<timeDiscretizationForCurves.getNumberOfTimes()-1; timeIndex++) {
			modelParameters.put("FORWARDRATE("+ timeDiscretizationForCurves.getTime(timeIndex) + ")", getForwardRateInitialValue(process, timeIndex));
		}

		// Add volatilities
		if(volatilityModel instanceof ShortRateVolatilityModelParametric) {
			final RandomVariable[] parameters = ((ShortRateVolatilityModelParametric) volatilityModel).getParameter();

			for(int volatilityModelParameterIndex=0; volatilityModelParameterIndex<parameters.length; volatilityModelParameterIndex++) {
				modelParameters.put("VOLATILITYMODELPARAMETER("+ volatilityModelParameterIndex + ")", parameters[volatilityModelParameterIndex]);
			}
		}

		// Add numeraire adjustments
		// TODO Trigger lazy init
		// Add initial values
		for(int timeIndex=0; timeIndex<timeDiscretizationForCurves.getNumberOfTimes()-1; timeIndex++) {
			modelParameters.put("NUMERAIREADJUSTMENTFORWARD("+ timeDiscretizationForCurves.getTime(timeIndex) + ")", numeraireDiscountFactorForwardRates.get(timeIndex));
		}

		return modelParameters;
	}

	private RandomVariable getDiscountFactor(final MonteCarloProcess process, final double time) {
		final TimeDiscretization timeDiscretizationForCurves = isInterpolateDiscountFactorsOnLiborPeriodDiscretization ? liborPeriodDiscretization : process.getTimeDiscretization();
		final int timeIndex = timeDiscretizationForCurves.getTimeIndex(time);
		if(timeIndex >= 0) {
			return getDiscountFactor(process, timeIndex);
		}
		else {
			// Interpolation
			final int timeIndexPrev = Math.min(-timeIndex-2, getLiborPeriodDiscretization().getNumberOfTimes()-2);
			final int timeIndexNext = timeIndexPrev+1;
			final double timePrev = timeDiscretizationForCurves.getTime(timeIndexPrev);
			final double timeNext = timeDiscretizationForCurves.getTime(timeIndexNext);
			final RandomVariable discountFactorPrev = getDiscountFactor(process, timeIndexPrev);
			final RandomVariable discountFactorNext = getDiscountFactor(process, timeIndexNext);
			return discountFactorPrev.mult(discountFactorNext.div(discountFactorPrev).pow((time-timePrev)/(timeNext-timePrev)));
		}
	}

	private RandomVariable getDiscountFactor(final MonteCarloProcess process, final int timeIndex) {
		final TimeDiscretization timeDiscretizationForCurves = isInterpolateDiscountFactorsOnLiborPeriodDiscretization ? liborPeriodDiscretization : process.getTimeDiscretization();
		final double time = timeDiscretizationForCurves.getTime(timeIndex);

		synchronized(numeraireDiscountFactorForwardRates) {
			if(numeraireDiscountFactors.size() == 0) {
				final double dfInitial = discountCurve.getDiscountFactor(analyticModel, timeDiscretizationForCurves.getTime(0));
				RandomVariable deterministicNumeraireAdjustment
				= randomVariableFactory.createRandomVariable(dfInitial);
				numeraireDiscountFactors.add(0, deterministicNumeraireAdjustment);

				for(int i=0; i<timeDiscretizationForCurves.getNumberOfTimeSteps(); i++) {
					final double dfPrev = discountCurve.getDiscountFactor(analyticModel, timeDiscretizationForCurves.getTime(i));
					final double dfNext = discountCurve.getDiscountFactor(analyticModel, timeDiscretizationForCurves.getTime(i+1));
					final double timeStep = timeDiscretizationForCurves.getTimeStep(i);
					final double timeNext = timeDiscretizationForCurves.getTime(i+1);
					final RandomVariable forwardRate = randomVariableFactory.createRandomVariable((dfPrev / dfNext - 1.0) / timeStep);
					numeraireDiscountFactorForwardRates.add(i, forwardRate);
					deterministicNumeraireAdjustment = deterministicNumeraireAdjustment.discount(forwardRate, timeStep);
					numeraireDiscountFactors.add(i+1, deterministicNumeraireAdjustment);
				}
			}
			final RandomVariable deterministicNumeraireAdjustment = numeraireDiscountFactors.get(timeIndex);
			return deterministicNumeraireAdjustment;
		}
	}

	private RandomVariable getZeroRateFromForwardCurve(final MonteCarloProcess process, final double time) {
		final TimeDiscretization timeDiscretizationForCurves = isInterpolateDiscountFactorsOnLiborPeriodDiscretization ? liborPeriodDiscretization : process.getTimeDiscretization();

		int timeIndex = timeDiscretizationForCurves.getTimeIndex(time);
		if(timeIndex < 0) {
			timeIndex = Math.min(-timeIndex-2, getLiborPeriodDiscretization().getNumberOfTimes()-2);
		}

		final double timeStep = timeDiscretizationForCurves.getTimeStep(timeIndex);
		return getDiscountFactorFromForwardCurve(process, timeIndex).div(getDiscountFactorFromForwardCurve(process, timeIndex)).log().div(timeStep);
	}

	private RandomVariable getDiscountFactorFromForwardCurve(final MonteCarloProcess process, final double time) {
		final TimeDiscretization timeDiscretizationForCurves = isInterpolateDiscountFactorsOnLiborPeriodDiscretization ? liborPeriodDiscretization : process.getTimeDiscretization();
		final int timeIndex = timeDiscretizationForCurves.getTimeIndex(time);
		if(timeIndex >= 0) {
			return getDiscountFactorFromForwardCurve(process, timeIndex);
		}
		else {
			final int timeIndexPrev = Math.min(-timeIndex-2, getLiborPeriodDiscretization().getNumberOfTimes()-2);
			final int timeIndexNext = timeIndexPrev+1;
			final double timePrev = timeDiscretizationForCurves.getTime(timeIndexPrev);
			final double timeNext = timeDiscretizationForCurves.getTime(timeIndexNext);
			final RandomVariable discountFactorPrev = getDiscountFactorFromForwardCurve(process, timeIndexPrev);
			final RandomVariable discountFactorNext = getDiscountFactorFromForwardCurve(process, timeIndexNext);
			return discountFactorPrev.mult(discountFactorNext.div(discountFactorPrev).pow((time-timePrev)/(timeNext-timePrev)));
		}
	}

	private RandomVariable getDiscountFactorFromForwardCurve(final MonteCarloProcess process, final int timeIndex) {
		synchronized(discountFactorFromForwardCurveCache) {
			if(discountFactorFromForwardCurveCache.size() <= timeIndex) {
				// Initialize cache
				final TimeDiscretization timeDiscretizationForCurves = isInterpolateDiscountFactorsOnLiborPeriodDiscretization ? liborPeriodDiscretization : process.getTimeDiscretization();
				for(int i=discountFactorFromForwardCurveCache.size(); i<=timeIndex; i++) {
					RandomVariable dfAsRandomVariable;
					if(i == 0) {
						final double df = discountCurveFromForwardCurve.getDiscountFactor(analyticModel, timeDiscretizationForCurves.getTime(i));
						dfAsRandomVariable = randomVariableFactory.createRandomVariable(df);
					}
					else {
						final RandomVariable dfPrevious = discountFactorFromForwardCurveCache.get(i-1);
						final RandomVariable forwardRate = getForwardRateInitialValue(process, i-1);
						dfAsRandomVariable = dfPrevious.div(forwardRate.mult(timeDiscretizationForCurves.getTimeStep(i-1)).add(1.0));
					}
					discountFactorFromForwardCurveCache.add(dfAsRandomVariable);
				}
			}
		}

		return discountFactorFromForwardCurveCache.get(timeIndex);
	}

	private RandomVariable getForwardRateInitialValue(final MonteCarloProcess process, final int timeIndex) {
		synchronized(forwardRateCache) {
			if(forwardRateCache.size() <= timeIndex) {
				// Initialize cache
				final TimeDiscretization timeDiscretizationForCurves = isInterpolateDiscountFactorsOnLiborPeriodDiscretization ? liborPeriodDiscretization : process.getTimeDiscretization();
				for(int i=forwardRateCache.size(); i<=timeIndex; i++) {
					final double dfPrev = discountCurveFromForwardCurve.getDiscountFactor(analyticModel, timeDiscretizationForCurves.getTime(i));
					final double dfNext = discountCurveFromForwardCurve.getDiscountFactor(analyticModel, timeDiscretizationForCurves.getTime(i+1));
					final RandomVariable forwardRate = randomVariableFactory.createRandomVariable((dfPrev / dfNext - 1.0) / timeDiscretizationForCurves.getTimeStep(i));
					forwardRateCache.add(forwardRate);
				}
			}
		}
		return forwardRateCache.get(timeIndex);
	}

	private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();

		/*
		 * Init transient fields
		 */
		numeraireDiscountFactors = new ArrayList<>();
		numeraireDiscountFactorForwardRates = new ArrayList<>();
		discountFactorFromForwardCurveCache = new ArrayList<>();
		forwardRateCache = new ArrayList<>();
	}

	@Override
	public String toString() {
		return "HullWhiteModel [liborPeriodDiscretization=" + liborPeriodDiscretization + ", forwardCurveName="
				+ forwardCurveName + ", analyticModel=" + analyticModel + ", forwardRateCurve=" + forwardRateCurve
				+ ", discountCurve=" + discountCurve + ", discountCurveFromForwardCurve="
				+ discountCurveFromForwardCurve + ", randomVariableFactory=" + randomVariableFactory
				+ ", volatilityModel=" + volatilityModel + ", properties=" + properties
				+ ", isInterpolateDiscountFactorsOnLiborPeriodDiscretization="
				+ isInterpolateDiscountFactorsOnLiborPeriodDiscretization + "]";
	}
}
