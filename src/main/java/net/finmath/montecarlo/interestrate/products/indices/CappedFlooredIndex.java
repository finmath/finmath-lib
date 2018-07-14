/*
 * Created on 06.12.2009
 *
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 */
package net.finmath.montecarlo.interestrate.products.indices;

import java.util.Set;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * An capped and floored index paying min(max(index(t),floor(t)),cap(t)), where index, floor and cap are indices,
 * i.e., objects implementing <code>AbstractIndex</code>.
 *
 * @author Christian Fries
 */
public class CappedFlooredIndex extends AbstractIndex {

	private static final long serialVersionUID = 7835825574794506180L;

	private final AbstractIndex index;
	private final AbstractIndex cap;
	private final AbstractIndex floor;

	/**
	 * Create an capped and floored index paying min(max(index(t),floor(t)),cap(t)).
	 *
	 * @param index An index.
	 * @param cap An index.
	 * @param floor An index.
	 */
	public CappedFlooredIndex(AbstractIndex index, AbstractIndex cap, AbstractIndex floor) {
		super();
		this.index	= index;
		this.cap	= cap;
		this.floor	= floor;
	}

	@Override
	public RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {
		RandomVariableInterface indexValues = index.getValue(evaluationTime, model);

		if(floor != null) {
			indexValues = indexValues.floor(floor.getValue(evaluationTime, model));
		}
		if(cap != null) {
			indexValues = indexValues.cap(cap.getValue(evaluationTime, model));
		}

		return indexValues;
	}

	@Override
	public Set<String> queryUnderlyings() {
		Set<String> underlyingNames			= index != null ? index.queryUnderlyings() : null;
		Set<String>	underlyingNamesCap		= cap != null ? cap.queryUnderlyings() : null;
		if(underlyingNamesCap != null) {
			if(underlyingNames != null) {
				underlyingNames.addAll(underlyingNamesCap);
			} else {
				underlyingNames = underlyingNamesCap;
			}
		}
		Set<String>	underlyingNamesFloor	= floor != null ? floor.queryUnderlyings() : null;
		if(underlyingNamesFloor != null) {
			if(underlyingNames != null) {
				underlyingNames.addAll(underlyingNamesFloor);
			} else {
				underlyingNames = underlyingNamesFloor;
			}
		}
		return underlyingNames;
	}

	@Override
	public String toString() {
		return "CappedFlooredIndex [index=" + index + ", cap=" + cap
				+ ", floor=" + floor + ", toString()=" + super.toString() + "]";
	}
}
