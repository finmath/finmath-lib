package net.finmath.equities.models;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;

import net.finmath.equities.marketdata.VolatilityPoint;
import net.finmath.interpolation.RationalFunctionInterpolation;
import net.finmath.optimizer.LevenbergMarquardt;
import net.finmath.optimizer.SolverException;
import net.finmath.time.daycount.DayCountConvention;

/**
 * Class that implements an SVI volatility surface according to Gatheral's 2013 paper.
 * The smile-specific parts (i.e. for strikes at a single option expiry) are implemented
 * in a separate class SviVolatilitySmile.
 * The surface supports sticky-strike as well as sticky-moneyness of volatility.
 * The calibration is currently done smile by smile, and does not guarantee absence of arbitrage.
 *
 * @author Andreas Grotz
 */

public class SviVolatilitySurface implements VolatilitySurface, ShiftedVolatilitySurface {

	private final DayCountConvention dayCounter;
	private final boolean useStickyStrike;
	private LocalDate valuationDate;
	private EquityForwardStructure forwardStructure;
	private SviVolatilitySmile[] smiles = new SviVolatilitySmile[0];
	private double[] smileTimes = new double[0];
	private boolean isCalibrated = false;
	private final double volShift;

	public SviVolatilitySurface(DayCountConvention dayCounter, boolean useStickyStrike)
	{
		this.dayCounter = dayCounter;
		this.useStickyStrike = useStickyStrike;
		this.volShift = 0.0;
	}

	public SviVolatilitySurface(
			LocalDate valuationDate,
			DayCountConvention dayCounter,
			EquityForwardStructure forwardStructure,
			SviVolatilitySmile[] smiles,
			boolean useStickyStrike)
	{
		this(valuationDate,
				dayCounter,
				forwardStructure,
				smiles,
				useStickyStrike,
				0.0);
	}

	private SviVolatilitySurface(
			LocalDate valuationDate,
			DayCountConvention dayCounter,
			EquityForwardStructure forwardStructure,
			SviVolatilitySmile[] smiles,
			boolean useStickyStrike,
			double volShift)
	{
		this.dayCounter = dayCounter;
		setForwardStructure(forwardStructure);
		this.smiles = smiles;
		this.useStickyStrike = useStickyStrike;
		this.volShift = volShift;

		final var sortedSmiles = Arrays.asList(smiles);
		sortedSmiles.sort(Comparator.comparing(pt -> pt.getSmileDate()));
		smileTimes = new double[sortedSmiles.size() + 1];
		smileTimes[0] = 0.0;
		for (int i = 0; i < sortedSmiles.size(); i++) {
			smileTimes[i+1] = dayCounter.getDaycountFraction(valuationDate, sortedSmiles.get(i).getSmileDate());
		}

		isCalibrated = true;
	}

	@Override
	public SviVolatilitySurface getShiftedSurface(double shift)
	{
		assert volShift == 0.0 : "Surface is already shifted";
		return new SviVolatilitySurface(
				this.valuationDate,
				this.dayCounter,
				this.forwardStructure,
				this.smiles,
				this.useStickyStrike,
				this.volShift);
	}

	@Override
	public double getShift()
	{
		return volShift;
	}

	public SviVolatilitySmile[] getSmiles()
	{
		return smiles;
	}

	private void setForwardStructure(EquityForwardStructure forwardStructure)
	{
		this.forwardStructure = forwardStructure;
		valuationDate = forwardStructure.getValuationDate();
	}

	@Override
	public double getVolatility(
			double strike,
			LocalDate expiryDate,
			EquityForwardStructure currentForwardStructure)
	{
		final var timeToMaturity = dayCounter.getDaycountFraction(valuationDate, expiryDate);
		return 	getVolatility(strike, timeToMaturity, currentForwardStructure);
	}

	@Override
	public double getVolatility(
			double strike,
			double timeToMaturity,
			EquityForwardStructure currentForwardStructure)
	{
		// sticky moneyness
		assert isCalibrated : "Surface is not calibrated yet";
	double logStrike;
	if(useStickyStrike) {
		logStrike = forwardStructure.getLogMoneyness(strike, timeToMaturity);
	} else {
		logStrike = currentForwardStructure.getLogMoneyness(strike, timeToMaturity);
	}
	return interpolateVolatility(logStrike, timeToMaturity);
	}

	@Override
	public double getLocalVolatility(
			double strike,
			LocalDate expiryDate,
			EquityForwardStructure currentForwardStructure,
			double strikeShift,
			double timeShift)
	{
		assert isCalibrated : "Surface is not calibrated yet";
	final var logStrike = currentForwardStructure.getLogMoneyness(strike, expiryDate);
	final var timeToMaturity = dayCounter.getDaycountFraction(valuationDate, expiryDate);
	return getLocalVolatility(logStrike, timeToMaturity, currentForwardStructure, strikeShift, timeShift);
	}

	@Override
	public double getLocalVolatility(
			double logStrike,
			double timeToMaturity,
			EquityForwardStructure currentForwardStructure,
			double strikeShift,
			double timeShift)
	{
		assert isCalibrated : "Surface is not calibrated yet";
	// Log-strike is provided w.r.t. current forward structure.
	// When using sticky strike, we need to transform
	// to log-strike w.r.t. forward structure prevailing during surface calbration
	if (useStickyStrike)
	{
		final var expiryTimeAsofCalib = timeToMaturity + dayCounter.getDaycountFraction(
				valuationDate, currentForwardStructure.getValuationDate());
		logStrike += Math.log(currentForwardStructure.getForward(timeToMaturity)
				/ forwardStructure.getForward(expiryTimeAsofCalib));
	}

	if (timeToMaturity >= 1e-16)
	{
		final var f = interpolateTotalVariance(logStrike, timeToMaturity);
		var f_t = interpolateTotalVariance(logStrike, timeToMaturity + timeShift);
		f_t = (f_t - f) / timeShift;
		final var f_plu = interpolateTotalVariance(logStrike + strikeShift, timeToMaturity);
		final var f_min = interpolateTotalVariance(logStrike - strikeShift, timeToMaturity);
		final var f_x = 0.5 * (f_plu - f_min) / strikeShift;
		final var f_xx = (f_plu + f_min - 2 * f) / strikeShift / strikeShift;
		var lv = 0.5 * f_x * logStrike / f - 1.0;
		lv *= lv;
		lv += 0.5 * f_xx - 0.25 * (0.25 + 1.0 / f) * f_x * f_x;
		return Math.sqrt(f_t / lv);
	}
	else if (timeToMaturity >= 0.0) {
		return getLocalVolatility(logStrike, 1e-16, currentForwardStructure, strikeShift, timeShift);
	} else {
		return 0.0;
	}
	}

	private double interpolateVolatility(double logStrike, double timeToMaturity)
	{
		if (timeToMaturity >= 1e-16) {
			return Math.sqrt(interpolateTotalVariance(logStrike, timeToMaturity) / timeToMaturity);
		} else if (timeToMaturity >= 0.0) {
			return interpolateVolatility(logStrike, 1e-16);
		} else {
			return 0.0;
		}
	}

	private double interpolateTotalVariance(double logStrike, double timeToMaturity)
	{
		final var len = smileTimes.length;
		final var totalVariances = new double[len];
		totalVariances[0] = 0.0;
		for (int i = 1; i< len; i++) {
			totalVariances[i] = smiles[i-1].getTotalVariance(logStrike);
		}

		final RationalFunctionInterpolation interpolator = new RationalFunctionInterpolation(
				smileTimes,
				totalVariances,
				RationalFunctionInterpolation.InterpolationMethod.LINEAR,
				RationalFunctionInterpolation.ExtrapolationMethod.LINEAR);
		final var totalVariance = interpolator.getValue(timeToMaturity);

		if (volShift == 0.0) {
			return totalVariance;
		} else {
			return totalVariance + volShift * (2 * Math.sqrt(totalVariance * timeToMaturity) + volShift * timeToMaturity);
		}
	}


	@Override
	public void calibrate(
			EquityForwardStructure forwardStructure,
			ArrayList<VolatilityPoint> volaPoints)
	{
		/*TODO The current calibration is smile by smile. It does not ensure absence of arbitrage.
		 * An improved calibration would use optimization constraints obtained from
		 * the density formula from Gatheral's 2013 paper to remove butterfly arbitrage,
		 * as well as monotonicity of total variances to remove calendar arbitrage.
		 * A natural initial guess would then be a globally calibrated SSVI surface as described
		 * in Gatheral's 2013 paper.*/

		assert volShift == 0.0 : "A shifted SVI surface cannot be calibrated";
		setForwardStructure(forwardStructure);

		final var groupedPoints =
				volaPoints.stream().collect(Collectors.groupingBy(VolatilityPoint::getDate));
		final var sortedSmileDates = Arrays.asList(groupedPoints.keySet().toArray(new LocalDate[0]));
		sortedSmileDates.sort(Comparator.comparing(pt -> pt));
		smileTimes = new double[sortedSmileDates.size() + 1];
		smileTimes[0] = 0.0;
		smiles = new SviVolatilitySmile[sortedSmileDates.size()];
		for (int i = 0; i < sortedSmileDates.size(); i++)
		{
			final var date = sortedSmileDates.get(i);
			final var thisPoints = groupedPoints.get(date);
			thisPoints.sort(Comparator.comparing(pt -> pt.getStrike()));
			final var forward = forwardStructure.getDividendAdjustedStrike(forwardStructure.getForward(date), date) ;
			final var ttm = dayCounter.getDaycountFraction(valuationDate, date);
			final var logStrikes = new ArrayList<Double>();
			final var totalVariances = new ArrayList<Double>();
			for (final var pt : thisPoints)
			{
				totalVariances.add(ttm * pt.getVolatility() * pt.getVolatility());
				logStrikes.add(Math.log(forwardStructure.getDividendAdjustedStrike(pt.getStrike(), date) / forward));
			}

			double[] thisSviParams;
			try
			{
				thisSviParams = calibrateSviSmile(ttm, logStrikes, totalVariances);
			}
			catch (final SolverException se)
			{
				continue;
			}
			smileTimes[i+1] = ttm;
			smiles[i] = new SviVolatilitySmile(date, thisSviParams[0], thisSviParams[1], thisSviParams[2], thisSviParams[3], thisSviParams[4]);

		}
		isCalibrated = true;
	}

	private static double[] calibrateSviSmile(double ttm, ArrayList<Double> logStrikes, ArrayList<Double> totalVariances) throws SolverException
	{
		final LevenbergMarquardt optimizer = new LevenbergMarquardt() {
			private static final long serialVersionUID = -2542034123359128169L;

			@Override
			public void setValues(final double[] parameters, final double[] values) {
				for(int i = 0; i < logStrikes.size(); i++)
				{
					values[i] = SviVolatilitySmile.sviTotalVariance(
							logStrikes.get(i),
							parameters[0],
							parameters[1],
							parameters[2],
							parameters[3],
							parameters[4]);
				}
			}
		};
		final var initialGuess = SviVolatilitySmile.sviInitialGuess(logStrikes, totalVariances);
		final var weights = new double[logStrikes.size()];
		final var targetValues = new double[logStrikes.size()];
		for (int i = 0; i < logStrikes.size(); i++)
		{
			weights[i] = 1.0;
			targetValues[i] = totalVariances.get(i);
		}
		optimizer.setInitialParameters(initialGuess);
		optimizer.setWeights(weights);
		optimizer.setMaxIteration(100);
		optimizer.setTargetValues(targetValues);
		optimizer.run();

		return optimizer.getBestFitParameters();
	}
}
