package net.finmath.singleswaprate.model.curves;

import java.time.LocalDate;

import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.curves.AbstractCurve;
import net.finmath.marketdata.model.curves.CurveBuilder;

/**
 * A curve, which models exponential decay of correlation from one point in time to another, according to
 * \[
 * 		\max\{e^{c(t-T)}, 1\} \, .
 * \]
 * Any point after the given termination time will have correlation of one.
 * Any point before will have decaying correlation according to the parameter.
 *
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public class ExponentialCorrelationCurve extends AbstractCurve implements Cloneable {

	/**
	 *
	 */
	private static final long serialVersionUID = -2781643232961198556L;
	private final double termination;
	private final double correlationDecay;

	/**
	 * Create the curve.
	 *
	 * @param name The name of the curve
	 * @param referenceDate The reference date of the curve
	 * @param termination The date as double, from which the correlation is measured
	 * @param correlationDecay The rate at which the correlation decays
	 */
	public ExponentialCorrelationCurve(final String name, final LocalDate referenceDate, final double termination, final double correlationDecay) {
		super(name, referenceDate);

		this.termination = termination;
		this.correlationDecay = correlationDecay;
	}

	@Override
	public double getValue(final AnalyticModel model, final double time) {
		if(time > termination) {
			return 1.0;
		} else {
			return Math.exp(correlationDecay *(time -termination));
		}
	}

	@Override
	public CurveBuilder getCloneBuilder() {
		throw new UnsupportedOperationException("This class does not allow to add points.");
	}

	@Override
	public double[] getParameter() {
		return new double[]{termination, correlationDecay};
	}

	@Override
	public void setParameter(final double[] parameter) {
		throw new UnsupportedOperationException("This class is immutable.");
	}

}
