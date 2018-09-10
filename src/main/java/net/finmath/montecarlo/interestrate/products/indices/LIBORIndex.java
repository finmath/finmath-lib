/*
 * Created on 15.09.2006
 *
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 */
package net.finmath.montecarlo.interestrate.products.indices;

import java.util.HashSet;
import java.util.Set;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.marketdata.model.curves.ForwardCurveInterface;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * A (floating) forward rate index for a given period start offset (offset from fixing) and period length.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class LIBORIndex extends AbstractIndex {

	private static final long serialVersionUID = 1L;

	private final double periodStartOffset;
	private final double periodLength;


	/**
	 * Creates a forward rate index for a given period start offset (offset from fixing) and period length.
	 *
	 * @param name The name of an index. Used to map an index on a curve.
	 * @param periodStartOffset An offset added to the fixing to define the period start.
	 * @param periodLength The period length
	 */
	public LIBORIndex(String name, double periodStartOffset, double periodLength) {
		super(name);
		this.periodStartOffset = periodStartOffset;
		this.periodLength = periodLength;
	}

	/**
	 * Creates a forward rate index for a given period start offset (offset from fixing) and period length.
	 *
	 * @param periodStartOffset An offset added to the fixing to define the period start.
	 * @param periodLength The period length
	 */
	public LIBORIndex(double periodStartOffset, double periodLength) {
		this(null, periodStartOffset, periodLength);
	}

	@Override
	public RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {

		// Check if model provides this index
		if(getName() != null && model.getModel().getForwardRateCurve().getName() != null) {
			// Check if analytic adjustment would be possible
			if(!model.getModel().getForwardRateCurve().getName().equals(getName()) && (model.getModel().getAnalyticModel() != null && model.getModel().getAnalyticModel().getForwardCurve(getName()) == null)) {
				throw new IllegalArgumentException("No curve for index " + getName() + " found in model.");
			}
		}

		// If evaluationTime < 0 take fixing from curve (note: this is a fall-back, fixing should be provided by product, if possible).
		if(evaluationTime < 0) {
			return model.getRandomVariableForConstant(model.getModel().getForwardRateCurve().getForward(model.getModel().getAnalyticModel(), evaluationTime+periodStartOffset));
		}

		RandomVariableInterface forwardRate = model.getLIBOR(evaluationTime, evaluationTime+periodStartOffset, evaluationTime+periodStartOffset+periodLength);

		if(getName() != null && !model.getModel().getForwardRateCurve().getName().equals(getName())) {
			// Perform a multiplicative adjustment on the forward bonds
			AnalyticModelInterface analyticModel = model.getModel().getAnalyticModel();
			ForwardCurveInterface indexForwardCurve = analyticModel.getForwardCurve(getName());
			ForwardCurveInterface modelForwardCurve = model.getModel().getForwardRateCurve();
			double adjustment = (1.0 + indexForwardCurve.getForward(analyticModel, evaluationTime+periodStartOffset, periodLength) * periodLength) / (1.0 + modelForwardCurve.getForward(analyticModel, evaluationTime+periodStartOffset, periodLength) * periodLength);
			forwardRate = forwardRate.mult(periodLength).add(1.0).mult(adjustment).sub(1.0).div(periodLength);
		}

		return forwardRate;
	}

	/**
	 * Returns the periodStartOffset as an act/365 daycount.
	 *
	 * @return the periodStartOffset
	 */
	public double getPeriodStartOffset() {
		return periodStartOffset;
	}

	/**
	 * Returns the tenor encoded as an pseudo act/365 daycount fraction.
	 *
	 * @return the periodLength The tenor as an act/365 daycount fraction.
	 */
	public double getPeriodLength() {
		return periodLength;
	}

	@Override
	public Set<String> queryUnderlyings() {
		Set<String> underlyingNames = new HashSet<>();
		underlyingNames.add(getName());
		return underlyingNames;
	}

	@Override
	public String toString() {
		return "LIBORIndex [periodStartOffset=" + periodStartOffset
				+ ", periodLength=" + periodLength + ", toString()="
				+ super.toString() + "]";
	}
}
