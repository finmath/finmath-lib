package net.finmath.climate.models.dice.submodels;

import java.util.function.DoubleUnaryOperator;

/**
 * The function \( T \mapsto \Omega(T) \) with \( T \) being the temperature above baseline, i.e., \( Omega(0) = 0 \).
 *
 * The function is a second order polynomial.
 *
 * @author Christian Fries
 */
public class DamageFromTemperature implements DoubleUnaryOperator {

	private final double tempToDamage0;
	private final double tempToDamage1;
	private final double tempToDamage2;

	/**
	 * Create the damage function
	 * \( T \mapsto \Omega(T) = a_{0} + a_{1} T + a_{2} T^{2} \), with
	 * \( T \) being temperature above pre-industrial.
	 *
	 * @param tempToDamage0 The constant term.
	 * @param tempToDamage1 The coefficient of the linear term.
	 * @param tempToDamage2 The coefficient of the quadratic term.
	 */
	public DamageFromTemperature(double tempToDamage0, double tempToDamage1, double tempToDamage2) {
		super();
		this.tempToDamage0 = tempToDamage0;
		this.tempToDamage1 = tempToDamage1;
		this.tempToDamage2 = tempToDamage2;
	}

	/**
	 * Create the damage function \( T \mapsto (a_{0} + a_{1} T + a_{2} T^{2}) \), with \( T \) being temperature above pre-industrial,
	 * using the default DICE (2016) parameters.
	 */
	public DamageFromTemperature() {
		// Parameters from original model
		this(0.0, 0.0, 0.00236);
	}

	/**
	 * Get the relative damage the GDP at a given temperature above pre-industrial.
	 * @param temperature The above pre-industrial in °K.
	 */
	@Override
	public double applyAsDouble(double temperature) {
		final double damage = tempToDamage0 + tempToDamage1 * temperature + tempToDamage2 * temperature * temperature;

		return damage;
	}

}
