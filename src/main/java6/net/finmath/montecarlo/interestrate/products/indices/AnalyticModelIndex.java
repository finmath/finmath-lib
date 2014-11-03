/*
 * Created on 25.10.2014
 *
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 */
package net.finmath.montecarlo.interestrate.products.indices;

import java.util.HashSet;
import java.util.Set;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.marketdata.model.curves.CurveInterface;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * An index which is given by a name referencing a curve of an analytic model.
 * 
 * @author Christian Fries
 */
public class AnalyticModelIndex extends AbstractIndex {

	private static final long serialVersionUID = 1L;

	private final String curveName;
	private final double fixingOffet;
    

	/**
	 * Creates a forward rate index for a given period start offset (offset from fixing) and period length.
	 * 
	 * @param name The name of an index. Used to map an index on a curve.
	 * @param curveName The name of the curve used to infer the forward of this index.
	 * @param fixingOffset An offset added to the fixing to define the period start.
	 */
	public AnalyticModelIndex(String name, String curveName, double fixingOffset) {
		super(name);
		this.curveName = curveName;
		this.fixingOffet = fixingOffset;
	}

	@Override
	public RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {

		AnalyticModelInterface analyticModel = model.getModel().getAnalyticModel();
		if(analyticModel == null) throw new IllegalArgumentException("Provided model does not carry an associated analytic model.");

		CurveInterface curve = analyticModel.getCurve(curveName);
		if(curve == null) throw new IllegalArgumentException("Associated analytic model does not carry a curve of the name " +  curveName + ".");
		
		double index = curve.getValue(analyticModel, evaluationTime + fixingOffet);
		
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
		Set<String> underlyingNames = new HashSet<String>();
		underlyingNames.add(getName());
		return underlyingNames;
	}

	@Override
	public String toString() {
		return "AnalyticModelIndex [curveName=" + curveName + ", fixingOffet=" + fixingOffet + "]";
	}
}