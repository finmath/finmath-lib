package net.finmath.marketdata.model.volatilities;

import java.time.LocalDate;

import net.finmath.marketdata.model.volatilities.VolatilitySurface.QuotingConvention;

/**
 * An Equity option quote is a function of strike and maturity. The quote can be represented in terms of prices or volatilities.
 * Concerning the strike: being a double, one might decide to store there a moneyness instead of a price, i.e. a relative strike where ATM = 0.
 *
 * @author Alessandro Gnoatto
 */
public class OptionData {

	private final String underlying;
	private final LocalDate referenceDate;
	private final double strike;
	private final double maturity;
	private final double value;
	private final QuotingConvention convention;

	public OptionData(final String underlying, final LocalDate referenceDate, final double strike, final double maturity, final double value, final QuotingConvention convention) {
		super();
		this.underlying = underlying;
		this.referenceDate = referenceDate;
		this.strike = strike;
		this.maturity = maturity;
		this.value = value;
		this.convention = convention;
	}

	public String getUnderlying() {
		return underlying;
	}

	public LocalDate getReferenceDate() {
		return referenceDate;
	}

	public double getStrike() {
		return strike;
	}

	public double getMaturity() {
		return maturity;
	}

	public double getValue() {
		return value;
	}

	public QuotingConvention getConvention() {
		return convention;
	}

	@Override
	public String toString() {
		return "EquityOptionQuote [underlying=" + underlying + ", referenceDate=" + referenceDate + ", strike="
				+ strike + ", maturity=" + maturity + ", value=" + value + ", convention=" + convention + "]";
	}
}
