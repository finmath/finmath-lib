/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2026
 */
package net.finmath.montecarlo.interestrate.products;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;

/**
 * A diagnostic hedge instrument representing a discretely rolled unit bond.
 *
 * The product pays
 *
 * \[
 *     1 + (T_{i+1}-T_{i}) L(T_{i},T_{i+1};T_{i})
 * \]
 *
 * at payment time \( T_{i+1} \). Hence, in a single-curve model, the value at
 * fixing time \( T_{i} \) is one. This is the instrument obtained if the cash
 * from a matured \(T_{i}\)-bond is rolled from \( T_{i} \) to
 * {\( T_{i+1} \) at the fixing Libor, rather than through the model's
 * short-rate money-market numeraire.
 *
 * <p>
 * The class is mainly meant as a diagnostic product for comparing LMM and
 * short-rate/Hull-White roll-over conventions. For {@code t < T_i} its value is
 * the adapted model-implied value of a {@code T_i}-bond, because the payoff is
 * constructed to have value one at {@code T_i}. For {@code T_i <= t < T_{i+1}}
 * the payoff amount is known and is valued as that amount times the adapted
 * payment bond. At {@code T_{i+1}} the payoff amount is returned.
 * </p>
 *
 * @author Christian Fries
 */
public class DiscreteTenorRollOver extends AbstractTermStructureMonteCarloProduct {

	private final double fixingTime;
	private final double paymentTime;
	private final double periodLength;

	/**
	 * Creates a discretely rolled unit bond.
	 *
	 * @param fixingTime The fixing time \( T_{i} \).
	 * @param paymentTime The payment time \( T_{i+1} \).
	 * @param periodLength The period length \( T_{i+1}-T_{i} \).
	 */
	public DiscreteTenorRollOver(
			final double fixingTime,
			final double paymentTime,
			final double periodLength) {
		super();
		if(paymentTime <= fixingTime) {
			throw new IllegalArgumentException("paymentTime must be greater than fixingTime.");
		}
		if(periodLength <= 0.0) {
			throw new IllegalArgumentException("periodLength must be positive.");
		}
		this.fixingTime = fixingTime;
		this.paymentTime = paymentTime;
		this.periodLength = periodLength;
	}

	@Override
	public RandomVariable getValue(
			final double evaluationTime,
			final TermStructureMonteCarloSimulationModel model) throws CalculationException {

		final double tolerance = 1E-12;

		if(evaluationTime < fixingTime - tolerance) {
			return getAdaptedBondValue(evaluationTime, fixingTime, model);
		}

		final RandomVariable payoffAmount = model.getForwardRate(fixingTime, fixingTime, paymentTime).mult(periodLength).add(1.0);

		if(evaluationTime < paymentTime - tolerance) {
			return payoffAmount.mult(getAdaptedBondValue(evaluationTime, paymentTime, model));
		}

		if(evaluationTime <= paymentTime + tolerance) {
			return payoffAmount;
		}

		/*
		 * Safe fallback for evaluation after payment: return the paid amount carried
		 * forward with the same numeraire/weight convention as standard products.
		 */
		return payoffAmount
				.div(model.getNumeraire(paymentTime))
				.mult(model.getMonteCarloWeights(paymentTime))
				.mult(model.getNumeraire(evaluationTime))
				.div(model.getMonteCarloWeights(evaluationTime));
	}
	private RandomVariable getAdaptedBondValue(
			final double evaluationTime,
			final double maturity,
			final TermStructureMonteCarloSimulationModel model) throws CalculationException {

		final double tolerance = 1E-12;
		if(maturity <= evaluationTime + tolerance) {
			if(maturity >= evaluationTime - tolerance) {
				return model.getRandomVariableForConstant(1.0);
			}
			return model.getRandomVariableForConstant(1.0)
					.div(model.getNumeraire(maturity))
					.mult(model.getMonteCarloWeights(maturity))
					.mult(model.getNumeraire(evaluationTime))
					.div(model.getMonteCarloWeights(evaluationTime));
		}

		return model.getModel().getForwardDiscountBond(
				model.getProcess(),
				evaluationTime,
				maturity);
	}

	public double getFixingTime() {
		return fixingTime;
	}

	public double getPaymentTime() {
		return paymentTime;
	}

	public double getPeriodLength() {
		return periodLength;
	}

	@Override
	public String toString() {
		return super.toString()
				+ "\nfixingTime: " + fixingTime
				+ "\npaymentTime: " + paymentTime
				+ "\nperiodLength: " + periodLength;
	}
}
