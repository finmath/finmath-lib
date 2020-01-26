/*
 * (c) Copyright finmath.net, Germany. Contact: info@finmath.net.
 *
 * Created on 01.06.2018
 */
package net.finmath.marketdata.model.bond;

import java.time.LocalDate;

import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.curves.AbstractCurve;
import net.finmath.marketdata.model.curves.Curve;
import net.finmath.marketdata.model.curves.CurveBuilder;

/**
 * Implements the bond curve as a curve object, see {@link net.finmath.marketdata.model.curves.Curve}.
 *
 * The bond curve is built as a product of a given reference discount curve and spread curve.
 * Since it is not clear in general if the reference curve or the spread curve are given in terms of
 * discount factors or zero rates, this class distinguishes between all possible cases of types.
 *
 * For the curve types provided see {@link net.finmath.marketdata.model.bond.BondCurve.Type}.
 *
 * @author Moritz Scherrmann
 * @version 1.0
 */
public class BondCurve extends AbstractCurve  {

	private static final long serialVersionUID = -7832169179168188306L;

	/**
	 * Possible curve types, where the first term stands for the reference discount curve and the
	 * second term stands for the spread curve.
	 *
	 * Example:
	 * "DISCOUNTFACTOR_ZERORATE" means that the "getValue" method of the reference discount curve returns a value expressed as
	 * discount factor and the "getValue" method of the spread curve returns a value expressed as
	 * zero rate.
	 *
	 * @author Moritz Scherrmann
	 */
	public enum Type {
		DISCOUNTFACTOR_DISCOUNTFACTOR,
		ZERORATE_DISCOUNTFACTOR,
		DISCOUNTFACTOR_ZERORATE,
		ZERORATE_ZERORATE,
	}

	private String name;
	private LocalDate referenceDate;
	private final Curve referenceCurve;
	private final Curve spreadCurve;
	private final Type type;

	/**
	 * Creates a bond curve.
	 *
	 * @param name Name of the curve.
	 * @param referenceDate The reference date for this curve, i.e., the date which defined t=0.
	 * @param referenceCurve The reference discount curve.
	 * @param spreadCurve The given spread curve.
	 * @param type The types of the given curves "referenceCurve" and "spreadCurve" (discount factor or zero rate)
	 */
	public BondCurve(
			final String name,
			final LocalDate referenceDate,
			final Curve referenceCurve,
			final Curve spreadCurve,
			final Type type
			) {
		super(name,referenceDate);
		this.referenceCurve=referenceCurve;
		this.spreadCurve=spreadCurve;
		this.type=type;
	}

	@Override
	public double getValue(final double time) {
		return getValue(null,time);
	}

	@Override
	public double getValue(final AnalyticModel model, final double time) {
		switch(type) {
		case DISCOUNTFACTOR_DISCOUNTFACTOR:
			return referenceCurve.getValue(model, time)*spreadCurve.getValue(model, time);
		case ZERORATE_DISCOUNTFACTOR:
			return Math.exp(-time*referenceCurve.getValue(time))*spreadCurve.getValue(model, time);
		case DISCOUNTFACTOR_ZERORATE:
			return referenceCurve.getValue(model, time)*Math.exp(-time*spreadCurve.getValue(time));
		case ZERORATE_ZERORATE:
		default:
			return Math.exp(-time*referenceCurve.getValue(time))*Math.exp(-time*spreadCurve.getValue(time));
		}
	}

	public double getDiscountFactor(final double time) {
		return getValue(time);
	}

	public double getDiscountFactor(final AnalyticModel model, final double time) {
		return getValue(model, time);
	}

	/**
	 * Returns the zero rate for a given maturity, i.e., -ln(df(T)) / T where T is the given maturity and df(T) is
	 * the discount factor at time $T$.
	 *
	 * @param maturity The given maturity.
	 * @return The zero rate.
	 */
	public double getZeroRate(final double maturity)
	{
		if(maturity == 0) {
			return this.getZeroRate(1.0E-14);
		}

		return -Math.log(getDiscountFactor(null, maturity))/maturity;
	}

	@Override
	public CurveBuilder getCloneBuilder() throws CloneNotSupportedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public LocalDate getReferenceDate() {
		return referenceDate;
	}

	public Curve getReferenceCurve() {
		return referenceCurve;
	}

	public Curve getSpreadCurve() {
		return spreadCurve;
	}

	public String getType() {
		return type.toString();
	}

	@Override
	public double[] getParameter() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setParameter(final double[] parameter) {
		// TODO Auto-generated method stub

	}




}
