/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 25.08.2017
 */
package net.finmath.marketdata.model.curves;

import java.io.IOException;
import java.io.Serializable;

import org.threeten.bp.LocalDate;

import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.time.FloatingpointDate;

/**
 * A discount curve \( t \mapsto df(t) \) with property \( df(t_{0}) = 1 \) for a given
 * \( t_{0} \) derived from a base discount curve by a constant skaling.
 *
 * @author Christian Fries
 */
public class DiscountCurveRenormalized implements DiscountCurveInterface, Serializable {

	private static final long serialVersionUID = -7603795467908495733L;

	private final String name;
	private final LocalDate referenceDate;
	private final LocalDate spotDate;
	private final String baseCurveName;

	private transient double spotOffset;

	public DiscountCurveRenormalized(String name, LocalDate referenceDate, LocalDate spotDate, String baseCurveName) {
		super();
		this.name = name;
		this.referenceDate = referenceDate;
		this.spotDate = spotDate;
		this.baseCurveName = baseCurveName;
		this.spotOffset = FloatingpointDate.getFloatingPointDateFromDate(referenceDate, spotDate);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public LocalDate getReferenceDate() {
		return referenceDate;
	}

	@Override
	public double getValue(double time) {
		return getDiscountFactor(time);
	}

	@Override
	public double getValue(AnalyticModelInterface model, double time) {
		return getDiscountFactor(model, time);
	}

	@Override
	public CurveBuilderInterface getCloneBuilder() throws CloneNotSupportedException {
		throw new UnsupportedOperationException("Method not supported.");
	}

	@Override
	public CurveInterface getCloneForParameter(double[] value) throws CloneNotSupportedException {
		throw new UnsupportedOperationException("Method not supported.");
	}

	@Override
	public double[] getParameter() {
		throw new UnsupportedOperationException("Method not supported.");
	}

	@Override
	public void setParameter(double[] parameter) {
		throw new UnsupportedOperationException("Method not supported.");
	}

	@Override
	public double getDiscountFactor(double maturity) {
		return getDiscountFactor(null, maturity);
	}

	@Override
	public double getDiscountFactor(AnalyticModelInterface model, double maturity) {
		return model.getDiscountCurve(baseCurveName).getDiscountFactor(model, maturity)
				/ model.getDiscountCurve(baseCurveName).getDiscountFactor(model, spotOffset);
	}

	@Override
	public DiscountCurveRenormalized clone() throws CloneNotSupportedException {
		return new DiscountCurveRenormalized(getName(), getReferenceDate(), spotDate, baseCurveName);
	}

	private void readObject(java.io.ObjectInputStream in) throws ClassNotFoundException, IOException {
		in.defaultReadObject();
		// initialization of transients
		this.spotOffset = FloatingpointDate.getFloatingPointDateFromDate(referenceDate, spotDate);
	}
}

