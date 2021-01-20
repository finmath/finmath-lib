package net.finmath.montecarlo.interestrate.products.components;

import java.time.LocalDateTime;
import java.util.Set;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;

/**
 * Base class for a period. A period has references to the index (coupon) and the notional.
 * It provides the fixing date for the index, the period length, and the payment date.
 *
 * @author Christian Fries
 * @version 1.1
 */
public abstract class AbstractPeriod extends AbstractProductComponent {

	private static final long serialVersionUID = 8035860121112226049L;

	private final LocalDateTime referenceDate;

	private final double periodStart;
	private final double periodEnd;
	private final double fixingDate;
	private final double paymentDate;

	private final Notional				notional;
	private final AbstractProductComponent		index;
	private final double						daycountFraction;

	@Override
	public abstract RandomVariable getValue(double evaluationTime, TermStructureMonteCarloSimulationModel model) throws CalculationException;

	public abstract RandomVariable getCoupon(double evaluationTime, TermStructureMonteCarloSimulationModel model) throws CalculationException;

	/**
	 * Initialize basic properties of the period.
	 *
	 * @param referenceDate The date corresponding to time \( t = 0 \).
	 * @param periodStart The period start.
	 * @param periodEnd The period end.
	 * @param fixingDate The fixing date (as double).
	 * @param paymentDate The payment date (as double).
	 * @param notional The notional object relevant for this period.
	 * @param index The index (used for coupon calculation) associated with this period.
	 * @param daycountFraction The daycount fraction (<code>coupon = index(fixingDate) * daycountFraction</code>).
	 */
	public AbstractPeriod(final LocalDateTime referenceDate, final double periodStart, final double periodEnd,
			final double fixingDate, final double paymentDate, final Notional notional,
			final AbstractProductComponent index, final double daycountFraction) {
		super();
		this.referenceDate = referenceDate;
		this.periodStart = periodStart;
		this.periodEnd = periodEnd;
		this.fixingDate = fixingDate;
		this.paymentDate = paymentDate;
		this.notional = notional;
		this.index = index;
		this.daycountFraction = daycountFraction;
	}

	/**
	 * Initialize basic properties of the period.
	 *
	 * @param periodStart The period start.
	 * @param periodEnd The period end.
	 * @param fixingDate The fixing date (as double).
	 * @param paymentDate The payment date (as double).
	 * @param notional The notional object relevant for this period.
	 * @param index The index (used for coupon calculation) associated with this period.
	 * @param daycountFraction The daycount fraction (<code>coupon = index(fixingDate) * daycountFraction</code>).
	 */
	public AbstractPeriod(final double periodStart, final double periodEnd,
			final double fixingDate, final double paymentDate, final Notional notional,
			final AbstractProductComponent index, final double daycountFraction) {
		this(null, periodStart, periodEnd, fixingDate, paymentDate, notional, index, daycountFraction);
	}

	/**
	 * Initialize basic properties of the period using the idealized
	 * daycount faction <code>periodEnd-periodStart</code>.
	 *
	 * @param periodStart The period start.
	 * @param periodEnd The period end.
	 * @param fixingDate The fixing date (as double).
	 * @param paymentDate The payment date (as double).
	 * @param notional The notional object relevant for this period.
	 * @param index The index (coupon) associated with this period.
	 */
	public AbstractPeriod(final double periodStart, final double periodEnd,
			final double fixingDate, final double paymentDate, final Notional notional,
			final AbstractProductComponent index) {
		this(periodStart, periodEnd, fixingDate, paymentDate, notional, index, periodEnd - periodStart);
	}

	@Override
	public String getCurrency() {
		return notional.getCurrency();
	}

	/**
	 * @return the reference date
	 */
	public LocalDateTime getReferenceDate() {
		return referenceDate;
	}

	/**
	 * @return the period start
	 */
	public double getPeriodStart() {
		return periodStart;
	}

	/**
	 * @return the period end
	 */
	public double getPeriodEnd() {
		return periodEnd;
	}

	/**
	 * @return the fixing date
	 */
	public double getFixingDate() {
		return fixingDate;
	}

	/**
	 * @return the payment date
	 */
	public double getPaymentDate() {
		return paymentDate;
	}

	/**
	 * @return the notional associated with this period.
	 */
	public Notional getNotional() {
		return notional;
	}

	/**
	 * @return the index associated with this period.
	 */
	public AbstractProductComponent getIndex() {
		return index;
	}

	/**
	 * @return the daycount fraction used in this period.
	 */
	public double getDaycountFraction() {
		return daycountFraction;
	}

	@Override
	public Set<String> queryUnderlyings() {
		return getIndex().queryUnderlyings();
	}

	@Override
	public String toString() {
		return "AbstractPeriod [periodStart=" + periodStart + ", periodEnd="
				+ periodEnd + ", fixingDate=" + fixingDate + ", paymentDate="
				+ paymentDate + ", notional=" + notional + ", index=" + index
				+ ", daycountFraction=" + daycountFraction + "]";
	}
}
