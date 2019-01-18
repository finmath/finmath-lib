/*
 * Created on 12.10.2014
 *
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 */
package net.finmath.montecarlo.interestrate.products.indices;

import java.util.Set;

import net.finmath.marketdata.model.curves.ForwardCurveInterface;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariable;

/**
 * A fixed coupon index paying coupon calculated from a forward curve.
 *
 * @see net.finmath.marketdata.model.curves.ForwardCurveInterface
 * @author Christian Fries
 * @version 1.0
 */
public class ForwardCurveIndex extends AbstractIndex {

	private static final long serialVersionUID = 5375406324063846793L;

	private final ForwardCurveInterface forwardCurve;

	/**
	 * Creates a forward curve index.
	 *
	 * @param forwardCurve The forward curve.
	 */
	public ForwardCurveIndex(ForwardCurveInterface forwardCurve) {
		super();
		this.forwardCurve = forwardCurve;
	}

	@Override
	public RandomVariable getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) {
		return new RandomVariableFromDoubleArray(forwardCurve.getForward(null,  evaluationTime));
	}

	@Override
	public Set<String> queryUnderlyings() {
		return null;
	}

	@Override
	public String toString() {
		return "ForwardCurveIndex [forwardCurve=" + forwardCurve + "]";
	}
}

