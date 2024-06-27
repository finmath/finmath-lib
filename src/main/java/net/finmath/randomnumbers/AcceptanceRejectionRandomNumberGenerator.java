/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christianfries.com.
 *
 * Created on 21 May 2018
 */
package net.finmath.randomnumbers;

import java.util.function.DoubleUnaryOperator;

import org.apache.commons.lang3.Validate;

/**
 * Class implementing <code>RandomNumberGenerator1D</code> by the acceptance rejection method.
 *
 * Note that the acceptance rejection methods requires a two dimensional uniform random number sequence with independent components.
 *
 * @author Christian Fries
 * @version 1.1
 */
public class AcceptanceRejectionRandomNumberGenerator implements RandomNumberGenerator1D {

	private static final long serialVersionUID = -9060003224133337426L;

	private final RandomNumberGenerator uniformRandomNumberGenerator;
	private final DoubleUnaryOperator targetDensity;
	private final DoubleUnaryOperator referenceDensity;
	private final DoubleUnaryOperator referenceDistributionICDF;
	private final double acceptanceLevel;

	/**
	 * Create a random number sequence using the given input sequence and acceptance-rejection sampling.
	 * 
	 * @param uniformRandomNumberGenerator A two dimension (at least) uniform number generator (with i.i.d components). The first two components will be used.
	 * @param targetDensity The density f of the target distribution F.
	 * @param referenceDensity The density g.
	 * @param referenceDistributionICDF The ICDF, i.e. the inverse of G where G' = g.
	 * @param acceptanceLevel The constant C such that f &le; C g.
	 */
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
	public double nextDouble() {
		boolean rejected = true;
		double y = Double.NaN;
		while(rejected) {
			final double[] uniform = uniformRandomNumberGenerator.getNext();		// Tuple (u,v) of two uniforms
			final double u = uniform[0];											// u that samples acceptance/rejection
			y = referenceDistributionICDF.applyAsDouble(uniform[1]);				// y = ICDF method applied to v (candidate)
			rejected = targetDensity.applyAsDouble(y) < u * acceptanceLevel * referenceDensity.applyAsDouble(y);
		}
		return y;
	}
}
