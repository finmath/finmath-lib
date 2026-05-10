package net.finmath.modelling;

import java.time.LocalDate;

import net.finmath.time.FloatingpointDate;

/**
 * European exercise: exercise only at maturity.
 *
 * @author Alessandro Gnoatto
 */
public class EuropeanExercise extends AbstractExercise {

	/**
	 * Creates a European exercise from a maturity in running time.
	 *
	 * @param maturity The maturity.
	 */
	public EuropeanExercise(final double maturity) {
		super(maturity, new double[] {maturity });
	}

	/**
	 * Creates a European exercise from dates.
	 *
	 * @param referenceDate The reference date.
	 * @param maturityDate The maturity date.
	 */
	public EuropeanExercise(final LocalDate referenceDate, final LocalDate maturityDate) {
		this(FloatingpointDate.getFloatingPointDateFromDate(referenceDate, maturityDate));
	}

	@Override
	public boolean isContinuousExercise() {
		return false;
	}

	@Override
	public boolean isExerciseAllowed(final double time) {
		return Math.abs(time - getMaturity()) < TIME_TOLERANCE;
	}

	@Override
	public boolean isEuropean() {
		return true;
	}

	@Override
	public boolean isAmerican() {
		return false;
	}

	@Override
	public boolean isBermudan() {
		return false;
	}
}
