/*
 * Created on 12.10.2014
 *
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 */
package net.finmath.montecarlo.interestrate.products.indices;

import java.util.Set;

import net.finmath.marketdata.model.curves.ForwardCurveInterface;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * A fixed coupon index paying coupon calculated from a forward curve.
 * 
 * @see net.finmath.marketdata.model.curves.ForwardCurveInterface
 * @author Christian Fries
 */
public class FowardCurveIndex extends AbstractIndex {

	private static final long serialVersionUID = 5375406324063846793L;
	
	private final ForwardCurveInterface fowardCurve;
	
	/**
	 * Creates a forward curve index.
	 * 
	 * @param fowardCurve The forward curve.
	 */
	public FowardCurveIndex(ForwardCurveInterface fowardCurve) {
		super();
		this.fowardCurve = fowardCurve;
	}

	@Override
	public RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) {
		return new RandomVariable(fowardCurve.getForward(null,  evaluationTime));
	}

	@Override
	public Set<String> queryUnderlyings() {
		return null;
	}

	@Override
	public String toString() {
		return "FowardCurveIndex [fowardCurve=" + fowardCurve + "]";
	}
}
