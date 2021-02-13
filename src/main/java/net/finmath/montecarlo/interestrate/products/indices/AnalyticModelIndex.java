/*
 * Created on 25.10.2014
 *
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 */
package net.finmath.montecarlo.interestrate.products.indices;

import java.util.HashSet;
import java.util.Set;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.curves.Curve;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;

/**
 * An index which is given by a name referencing a curve of an analytic model.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class AnalyticModelIndex extends AbstractIndex {

	private static final long serialVersionUID = 1L;

	private final String curveName;
	private final double fixingOffet;


	/**
	 * Creates an analytic model index using a given fixing offset (in days / 365).
	 *
	 * @param name The name of an index. Used to map an index on a curve.
	 * @param curveName The name of the curve used to infer the forward of this index.
	 * @param fixingOffset An offset added to the fixing to define the period start. Measured in days / 365.
	 */
	public AnalyticModelIndex(final String name, final String curveName, final double fixingOffset) {
		super(name);
		this.curveName = curveName;
		fixingOffet = fixingOffset;
	}

	@Override
	public RandomVariable getValue(final double evaluationTime, final TermStructureMonteCarloSimulationModel model) throws CalculationException {

		final AnalyticModel analyticModel = model.getModel().getAnalyticModel();
		if(analyticModel == null) {
			throw new IllegalArgumentException("Provided model does not carry an associated analytic model.");
		}

		final Curve curve = analyticModel.getCurve(curveName);
		if(curve == null) {
			throw new IllegalArgumentException("Associated analytic model does not carry a curve of the name " +  curveName + ".");
		}

		final double index = curve.getValue(analyticModel, evaluationTime + fixingOffet);

		return model.getRandomVariableForConstant(index);
	}


	/**
	 * Returns the fixingOffet as an act/365 day count.
	 *
	 * @return the fixingOffet
	 */
	public double getPeriodStartOffset() {
		return fixingOffet;
	}

	@Override
	public Set<String> queryUnderlyings() {
		final Set<String> underlyingNames = new HashSet<>();
		underlyingNames.add(getName());
		return underlyingNames;
	}

	@Override
	public String toString() {
		return "AnalyticModelIndex [curveName=" + curveName + ", fixingOffet=" + fixingOffet + "]";
	}
}
