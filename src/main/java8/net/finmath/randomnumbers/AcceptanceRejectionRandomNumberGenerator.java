/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christianfries.com.
 *
 * Created on 21 May 2018
 */
package net.finmath.randomnumbers;

import java.util.function.DoubleUnaryOperator;

import org.apache.commons.lang3.Validate;

/**
 * Class implementing <code>RandomNumberGenerator</code> by the acceptance rejection method.
 *
 * Note that the acceptance rejection methods requires a two dimensional uniform random number sequence with independent components.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class AcceptanceRejectionRandomNumberGenerator implements RandomNumberGenerator {

	private static final long serialVersionUID = -9060003224133337426L;

	private final RandomNumberGenerator uniformRandomNumberGenerator;
	private final DoubleUnaryOperator targetDensity;
	private final DoubleUnaryOperator referenceDensity;
	private final DoubleUnaryOperator referenceDistributionICDF;
	private final double acceptanceLevel;

	public AcceptanceRejectionRandomNumberGenerator(final RandomNumberGenerator uniformRandomNumberGenerator,
			final DoubleUnaryOperator targetDensity,
			final DoubleUnaryOperator referenceDensity,
			final DoubleUnaryOperator referenceDistributionICDF,
			final double acceptanceLevel) {
		Validate.inclusiveBetween(2, Integer.MAX_VALUE, uniformRandomNumberGenerator.getDimension(), "The acceptance rejection method requires a uniform distributed random number generator with at least dimension 2.");

		this.uniformRandomNumberGenerator = uniformRandomNumberGenerator;
		this.targetDensity = targetDensity;
		this.referenceDensity = referenceDensity;
		this.referenceDistributionICDF = referenceDistributionICDF;
		this.acceptanceLevel = acceptanceLevel;
	}

	@Override
	public double[] getNext() {
		boolean rejected = true;
		double y = Double.NaN;
		while(rejected) {
			final double[] uniform = uniformRandomNumberGenerator.getNext();
			final double u = uniform[0];
			y = referenceDistributionICDF.applyAsDouble(uniform[1]);
			rejected = targetDensity.applyAsDouble(y) < u * acceptanceLevel * referenceDensity.applyAsDouble(y);
		}
		return new double[] { y };
	}

	@Override
	public int getDimension() {
		return 1;
	}
}
