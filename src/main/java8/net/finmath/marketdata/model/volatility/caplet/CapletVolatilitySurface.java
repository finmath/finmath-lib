package net.finmath.marketdata.model.volatility.caplet;

import java.time.LocalDate;

import net.finmath.functions.AnalyticFormulas;
import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.model.volatilities.VolatilitySurface;
import net.finmath.marketdata.model.volatility.caplet.smile.LinearSmileInterpolater;
import net.finmath.time.daycount.DayCountConvention;

/**
 * This class implements a caplet volatility surface.
 *
 * @author Daniel Willhalm
 * @author Christian Fries (review and fixes)
 */
public class CapletVolatilitySurface implements VolatilitySurface {

	private final String name;
	private final LocalDate referenceDate;
	private final QuotingConvention volatilityConvention;
	private final double[][] volatilityMatrix;
	private final double[] maturityVector;
	private final double[] strikeVector;
	private final ForwardCurve forwardCurve;
	private final DiscountCurve discountCurve;
	private final DayCountConvention daycountConvention;

	/**
	 * The constructor of the caplet volatility surface class.
	 * A volatility matrix is used to create the surface
	 *
	 * @param name The name of the surface.
	 * @param referenceDate The reference date of the surface.
	 * @param volatilityMatrix The matrix with caplet volatilities as entries.
	 * @param maturityVector The maturities of the rows of the volatility matrix.
	 * @param strikeVector The caplet strikes.
	 * @param forwardCurve The forward curve.
	 * @param volatilityConvention The volatility convention.
	 * @param discountCurve The discount curve.
	 */
	public CapletVolatilitySurface(final String name, final LocalDate referenceDate, final double[][] volatilityMatrix, final double[] maturityVector, final double[] strikeVector, final ForwardCurve forwardCurve, final QuotingConvention volatilityConvention, final DiscountCurve discountCurve) {
		this.name = name;
		this.referenceDate = referenceDate;
		this.forwardCurve = forwardCurve;
		this.discountCurve = discountCurve;
		this.volatilityMatrix = volatilityMatrix;
		this.maturityVector = maturityVector;
		this.strikeVector = strikeVector;
		this.volatilityConvention = volatilityConvention;
		daycountConvention = null;
	}

	/**
	 * The constructor of the caplet volatility surface class.
	 * A single volatility is used and all matrix entries will be that value.
	 * This constructor is used if we want to price a cap given a cap volatility.
	 *
	 * @param name The name of the surface.
	 * @param referenceDate The reference date of the surface.
	 * @param volatility The volatility.
	 * @param maturityVector The maturities of the rows of the volatility matrix.
	 * @param strikeVector The caplet strikes.
	 * @param forwardCurve The forward curve.
	 * @param volatilityConvention The volatility convention.
	 * @param discountCurve The discount curve.
	 */
	public CapletVolatilitySurface(final String name, final LocalDate referenceDate, final double volatility, final double[] maturityVector, final double[] strikeVector, final ForwardCurve forwardCurve, final QuotingConvention volatilityConvention, final DiscountCurve discountCurve) {
		this.name = name;
		this.referenceDate = referenceDate;
		this.forwardCurve = forwardCurve;
		this.discountCurve = discountCurve;
		volatilityMatrix = new double[maturityVector.length][strikeVector.length];
		for (int i = 0; i < maturityVector.length; i++) {
			for (int j = 0; j < strikeVector.length; j++) {
				volatilityMatrix[i][j] = volatility;
			}
		}
		this.maturityVector = maturityVector;
		this.strikeVector = strikeVector;
		this.volatilityConvention = volatilityConvention;
		daycountConvention = null;
	}

	/**
	 * Method that returns the volatility value.
	 *
	 * @param maturity The desired maturity.
	 * @param strike The desired strike
	 * @param quotingConvention The desired quoting convention.
	 * @return the volatility value for the given maturity, strike and quoting convention.
	 */
	@Override
	public double getValue(final double maturity, final double strike, final QuotingConvention quotingConvention) {
		double distance = Math.abs(maturityVector[0] - maturity);
		int closestIndex = 0;
		for(int i = 1; i < maturityVector.length; i++){
			if(Math.abs(maturityVector[i] - maturity) < distance){
				closestIndex = i;
				distance = Math.abs(maturityVector[i] - maturity);
			}
		}

		// TODO The inter and extra polater should be made flexible at some point. Right now only one exists
		final LinearSmileInterpolater linearSmileInterpolater = new LinearSmileInterpolater(volatilityMatrix, strikeVector);
		return linearSmileInterpolater.calculateInterpolatedExtrapolatedSmileVolatility(strike, closestIndex);
	}

	@Override
	public double getValue(final AnalyticModel model, final double maturity, final double strike, final QuotingConvention quotingConvention) {
		if(maturity == 0) {
			return 0;
		}

		double value;
		double distance = Math.abs(maturityVector[0] - maturity);
		int closestIndex = 0;
		for(int i = 1; i < maturityVector.length; i++){
			if(Math.abs(maturityVector[i] - maturity) < distance){
				closestIndex = i;
				distance = Math.abs(maturityVector[i] - maturity);
			}
		}
		if(distance == 0) {
			final LinearSmileInterpolater linearSmileInterpolater = new LinearSmileInterpolater(volatilityMatrix, strikeVector);
			value			= linearSmileInterpolater.calculateInterpolatedExtrapolatedSmileVolatility(strike, closestIndex);
		}
		else {
			int maturityGreaterEqualIndex = closestIndex;
			if(maturityGreaterEqualIndex < 0) {
				maturityGreaterEqualIndex = -maturityGreaterEqualIndex-1;
			}
			if(maturityGreaterEqualIndex > maturityVector.length-1) {
				maturityGreaterEqualIndex = maturityVector.length-1;
			}

			final double adjustedStrike = this.getForwardCurve().getValue(model, maturityVector[maturityGreaterEqualIndex]) + (strike - this.getForwardCurve().getValue(model, maturity));

			final LinearSmileInterpolater linearSmileInterpolater = new LinearSmileInterpolater(volatilityMatrix, strikeVector);
			value			= linearSmileInterpolater.calculateInterpolatedExtrapolatedSmileVolatility(adjustedStrike, maturityGreaterEqualIndex);
		}

		return convertFromTo(model, maturity, strike, value, this.getQuotingConvention(), quotingConvention);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public QuotingConvention getQuotingConvention() {
		return volatilityConvention;
	}

	@Override
	public LocalDate getReferenceDate() {
		return referenceDate;
	}

	public ForwardCurve getForwardCurve() {
		return forwardCurve;
	}

	public DiscountCurve getDiscountCurve() {
		return discountCurve;
	}

	//from abstract class AbstractVolatilitySurface from finmath library
	/**
	 * Convert the value of a caplet from one quoting convention to another quoting convention.
	 *
	 * @param model An analytic model providing the context when fetching required market date.
	 * @param optionMaturity Option maturity of the caplet.
	 * @param optionStrike Option strike of the caplet.
	 * @param value Value of the caplet given in the form of <code>fromQuotingConvention</code>.
	 * @param fromQuotingConvention The quoting convention of the given value.
	 * @param toQuotingConvention The quoting convention requested.
	 * @return Value of the caplet given in the form of <code>toQuotingConvention</code>.
	 */
	public double convertFromTo(final AnalyticModel model, final double optionMaturity, final double optionStrike, final double value, final QuotingConvention fromQuotingConvention, final QuotingConvention toQuotingConvention) {

		if(fromQuotingConvention.equals(toQuotingConvention)) {
			return value;
		}

		if(discountCurve == null) {
			throw new IllegalArgumentException("Missing discount curve. Conversion of QuotingConvention requires forward curve and discount curve to be set.");
		}
		if(forwardCurve == null) {
			throw new IllegalArgumentException("Missing forward curve. Conversion of QuotingConvention requires forward curve and discount curve to be set.");
		}

		final double periodStart = optionMaturity;
		final double periodEnd = periodStart + forwardCurve.getPaymentOffset(periodStart);

		final double forward = forwardCurve.getForward(model, periodStart);

		double daycountFraction;
		if(daycountConvention != null) {
			final LocalDate startDate = referenceDate.plusDays((int)Math.round(periodStart*365));
			final LocalDate endDate   = referenceDate.plusDays((int)Math.round(periodEnd*365));
			daycountFraction = daycountConvention.getDaycountFraction(startDate, endDate);
		}
		else {
			daycountFraction = forwardCurve.getPaymentOffset(periodStart);
		}

		final double payoffUnit = discountCurve.getDiscountFactor(optionMaturity+forwardCurve.getPaymentOffset(optionMaturity)) * daycountFraction;

		if(toQuotingConvention.equals(QuotingConvention.PRICE) && fromQuotingConvention.equals(QuotingConvention.VOLATILITYLOGNORMAL)) {
			return AnalyticFormulas.blackScholesGeneralizedOptionValue(forward, value, optionMaturity, optionStrike, payoffUnit);
		}
		else if(toQuotingConvention.equals(QuotingConvention.PRICE) && fromQuotingConvention.equals(QuotingConvention.VOLATILITYNORMAL)) {
			return AnalyticFormulas.bachelierOptionValue(forward, value, optionMaturity, optionStrike, payoffUnit);
		}
		else if(toQuotingConvention.equals(QuotingConvention.VOLATILITYLOGNORMAL) && fromQuotingConvention.equals(QuotingConvention.PRICE)) {
			return AnalyticFormulas.blackScholesOptionImpliedVolatility(forward, optionMaturity, optionStrike, payoffUnit, value);
		}
		else if(toQuotingConvention.equals(QuotingConvention.VOLATILITYNORMAL) && fromQuotingConvention.equals(QuotingConvention.PRICE)) {
			return AnalyticFormulas.bachelierOptionImpliedVolatility(forward, optionMaturity, optionStrike, payoffUnit, value);
		}
		else {
			return convertFromTo(model, optionMaturity, optionStrike, convertFromTo(model, optionMaturity, optionStrike, value, fromQuotingConvention, QuotingConvention.PRICE), QuotingConvention.PRICE, toQuotingConvention);
		}
	}

	/**
	 * Convert the value of a caplet from one quoting convention to another quoting convention.
	 *
	 * @param optionMaturity Option maturity of the caplet.
	 * @param optionStrike Option strike of the caplet.
	 * @param value Value of the caplet given in the form of <code>fromQuotingConvention</code>.
	 * @param fromQuotingConvention The quoting convention of the given value.
	 * @param toQuotingConvention The quoting convention requested.
	 * @return Value of the caplet given in the form of <code>toQuotingConvention</code>.
	 */
	public double convertFromTo(final double optionMaturity, final double optionStrike, final double value, final QuotingConvention fromQuotingConvention, final QuotingConvention toQuotingConvention) {
		return convertFromTo(null, optionMaturity, optionStrike, value, fromQuotingConvention, toQuotingConvention);
	}
}
