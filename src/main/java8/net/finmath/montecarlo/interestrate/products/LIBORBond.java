/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 10.02.2004
 */
package net.finmath.montecarlo.interestrate.products;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;

/**
 * This class implements the valuation of a zero (forward) bond on the models forward rate curve.
 *
 * The value returned by getValue(t) is \( F_{t} \) measurable, since the valuation
 * uses getLIBOR(t,t,T) only.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class LIBORBond extends AbstractLIBORMonteCarloProduct {
	private final double maturity;

	/**
	 * @param maturity The maturity given as double.
	 */
	public LIBORBond(final double maturity) {
		super();
		this.maturity = maturity;
	}

	/**
	 * This method returns the value random variable of the product within the specified model, evaluated at a given evalutationTime.
	 * Note: For a lattice this is often the value conditional to evalutationTime, for a Monte-Carlo simulation this is the (sum of) value discounted to evaluation time.
	 * Cashflows prior evaluationTime are not considered.
	 *
	 * @param evaluationTime The time on which this products value should be observed.
	 * @param model The model used to price the product.
	 * @return The random variable representing the value of the product discounted to evaluation time
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	@Override
	public RandomVariable getValue(final double evaluationTime, final TermStructureMonteCarloSimulationModel model) throws CalculationException {
		if(evaluationTime > maturity) {
			return new Scalar(0);
		}

		return model.getForwardRate(evaluationTime, evaluationTime, maturity).mult(maturity - evaluationTime).add(1.0).invert();
	}

	/**
	 * @return Returns the maturity.
	 */
	public double getMaturity() {
		return maturity;
	}

	@Override
	public String toString() {
		return super.toString() + "\n" + "maturity: " + maturity;
	}
}
