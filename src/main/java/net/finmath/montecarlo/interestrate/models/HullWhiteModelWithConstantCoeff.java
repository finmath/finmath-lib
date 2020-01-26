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
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.interestrate.LIBORMarketModel;
import net.finmath.montecarlo.interestrate.LIBORModel;
import net.finmath.montecarlo.model.AbstractProcessModel;
import net.finmath.montecarlo.process.MonteCarloProcess;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;

/**
 * Implements a Hull-White model with constant coefficients.
 *
 * <i>
 * A more general implementation of the Hull-White model can be found in {@link net.finmath.montecarlo.interestrate.models.HullWhiteModel}.
 * For details and documentation please see {@link net.finmath.montecarlo.interestrate.models.HullWhiteModel} for real applications.
 * </i>
 *
 * @author Christian Fries
 * @version 1.0
 */
public class HullWhiteModelWithConstantCoeff extends AbstractProcessModel implements LIBORModel {

	private final TimeDiscretization		liborPeriodDiscretization;

	private String							forwardCurveName;
	private final AnalyticModel			curveModel;

	private final ForwardCurve			forwardRateCurve;
	private final DiscountCurve			discountCurve;
	private final DiscountCurve			discountCurveFromForwardCurve;

	private final double meanReversion;
	private final double volatility;

	// Cache for the numeraires, needs to be invalidated if process changes
	private final ConcurrentHashMap<Integer, RandomVariable>	numeraires;
	private MonteCarloProcess									numerairesProcess = null;


	// Initialized lazily using process time discretization
	private RandomVariable[] initialState;

	/**
	 * Creates a Hull-White model which implements <code>LIBORMarketModel</code>.
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
			final TimeDiscretization			liborPeriodDiscretization,
			final AnalyticModel				analyticModel,
			final ForwardCurve				forwardRateCurve,
			final DiscountCurve				discountCurve,
			final double 								meanReversion,
			final double								volatility,
			final Map<String, ?>						properties
			) {

		this.liborPeriodDiscretization	= liborPeriodDiscretization;
		curveModel					= analyticModel;
		this.forwardRateCurve	= forwardRateCurve;
		this.discountCurve		= discountCurve;
		this.meanReversion		= meanReversion;
		this.volatility			= volatility;

		discountCurveFromForwardCurve = new DiscountCurveFromForwardCurve(forwardRateCurve);

		numeraires = new ConcurrentHashMap<>();
	}

	@Override
	public int getNumberOfComponents() {
		return 1;
	}

	@Override
	public RandomVariable applyStateSpaceTransform(final int componentIndex, final RandomVariable randomVariable) {
		return randomVariable;
	}

	@Override
	public RandomVariable applyStateSpaceTransformInverse(final int componentIndex, final RandomVariable randomVariable) {
		return randomVariable;
	}

	@Override
	public RandomVariable[] getInitialState() {
		if(initialState == null) {
			final double dt = getProcess().getTimeDiscretization().getTimeStep(0);
			//liborPeriodDiscretization.getTimeStep(0);
			initialState = new RandomVariable[] { new RandomVariableFromDoubleArray(Math.log(discountCurveFromForwardCurve.getDiscountFactor(0.0)/discountCurveFromForwardCurve.getDiscountFactor(dt))/dt) };
		}

		return initialState;
	}

	@Override
	public RandomVariable getNumeraire(final double time) throws CalculationException {
		if(time == getTime(0)) {
			return new RandomVariableFromDoubleArray(1.0);
		}

		final int timeIndex = getProcess().getTimeIndex(time);
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
			final double previousTime = getProcess().getTime(previousTimeIndex);

			// Get value of short rate for period from previousTime to time.
			final RandomVariable value = getShortRate(previousTimeIndex);

			// Piecewise constant rate for the increment
			final RandomVariable integratedRate = value.mult(time-previousTime);

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
		RandomVariable numeraire = numeraires.get(timeIndex);
		if(numeraire == null) {
			/*
			 * Calculate the numeraire for timeIndex
			 */
			final RandomVariable zero = getProcess().getStochasticDriver().getRandomVariableForConstant(0.0);
			RandomVariable integratedRate = zero;
			// Add r(t_{i}) (t_{i+1}-t_{i}) for i = 0 to previousTimeIndex-1
			for(int i=0; i<timeIndex; i++) {
				final RandomVariable rate = getShortRate(i);
				final double dt = getProcess().getTimeDiscretization().getTimeStep(i);
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
			final double deterministicNumeraireAdjustment = numeraire.invert().getAverage() / discountCurve.getDiscountFactor(curveModel, time);
			numeraire = numeraire.mult(deterministicNumeraireAdjustment);
		}

		return numeraire;
	}

	@Override
	public RandomVariable[] getDrift(final int timeIndex, final RandomVariable[] realizationAtTimeIndex, final RandomVariable[] realizationPredictor) {

		final double time = getProcess().getTime(timeIndex);
		final double timeNext = getProcess().getTime(timeIndex+1);

		final double t0 = time;
		final double t1 = timeNext;
		final double t2 = timeIndex< getProcess().getTimeDiscretization().getNumberOfTimes()-2 ? getProcess().getTime(timeIndex+2) : t1 + getProcess().getTimeDiscretization().getTimeStep(timeIndex);

		final double df0 = discountCurveFromForwardCurve.getDiscountFactor(t0);
		final double df1 = discountCurveFromForwardCurve.getDiscountFactor(t1);
		final double df2 = discountCurveFromForwardCurve.getDiscountFactor(t2);

		final double forward = time > 0 ? - Math.log(df1/df0) / (t1-t0) : getInitialState()[0].get(0);
		final double forwardNext = - Math.log(df2/df1) / (t2-t1);
		final double forwardChange = (forwardNext-forward) / ((t1-t0));

		final double meanReversionEffective = meanReversion*getB(time,timeNext)/(timeNext-time);

		final double shortRateVariance = getShortRateConditionalVariance(0, time);

		/*
		 * The +meanReversionEffective * forwardPrev removes the previous forward from the mean-reversion part.
		 * The +forwardChange updates the forward to the next period.
		 */
		final double theta = forwardChange + meanReversionEffective * forward + shortRateVariance*getB(time,t1)/(t1-time);

		return new RandomVariable[] { realizationAtTimeIndex[0].mult(-meanReversionEffective).add(theta) };
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.model.ProcessModel#getRandomVariableForConstant(double)
	 */
	@Override
	public RandomVariable getRandomVariableForConstant(final double value) {
		return getProcess().getStochasticDriver().getRandomVariableForConstant(value);
	}

	@Override
	public RandomVariable[] getFactorLoading(final int timeIndex, final int componentIndex, final RandomVariable[] realizationAtTimeIndex) {
		final double time = getProcess().getTime(timeIndex);
		final double timeNext = getProcess().getTime(timeIndex+1);

		final double scaling = Math.sqrt((1.0-Math.exp(-2.0 * meanReversion * (timeNext-time)))/(2.0 * meanReversion * (timeNext-time)));
		final double volatilityEffective = scaling*volatility;

		return new RandomVariable[] { new RandomVariableFromDoubleArray(volatilityEffective) };
	}

	@Override
	public RandomVariable getLIBOR(final double time, final double periodStart, final double periodEnd) throws CalculationException
	{
		return getZeroCouponBond(time, periodStart).div(getZeroCouponBond(time, periodEnd)).sub(1.0).div(periodEnd-periodStart);
	}

	@Override
	public RandomVariable getLIBOR(final int timeIndex, final int liborIndex) throws CalculationException {
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
	public double getLiborPeriod(final int timeIndex) {
		return liborPeriodDiscretization.getTime(timeIndex);
	}

	@Override
	public int getLiborPeriodIndex(final double time) {
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
	public LIBORMarketModel getCloneWithModifiedData(final Map<String, Object> dataModified) {
		throw new UnsupportedOperationException();
	}

	private RandomVariable getShortRate(final int timeIndex) throws CalculationException {
		final RandomVariable value = getProcess().getProcessValue(timeIndex, 0);
		return value;
	}

	private RandomVariable getZeroCouponBond(final double time, final double maturity) throws CalculationException {
		final RandomVariable shortRate = getShortRate(getProcess().getTimeIndex(time));
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
	private double getA(final double time, final double maturity) {
		final int timeIndex = getProcess().getTimeIndex(time);
		final double timeStep = getProcess().getTimeDiscretization().getTimeStep(timeIndex);

		final double dt = timeStep;
		final double zeroRate = -Math.log(discountCurveFromForwardCurve.getDiscountFactor(time+dt)/discountCurveFromForwardCurve.getDiscountFactor(time)) / dt;

		final double B = getB(time,maturity);

		final double lnA = Math.log(discountCurveFromForwardCurve.getDiscountFactor(maturity)/discountCurveFromForwardCurve.getDiscountFactor(time))
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
	private double getB(final double time, final double maturity) {
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
	public double getShortRateConditionalVariance(final double time, final double maturity) {
		return volatility*volatility * (1 - Math.exp(-2*meanReversion*(maturity-time))) / (2*meanReversion);

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

