package net.finmath.stochastic.operators;

import java.util.ArrayList;
import java.util.List;

import net.finmath.stochastic.RandomVariable;

/**
 * Small utility class to generate a list of monomials of a random variable.
 * This is handy if basis functions are needed that are monomials of a random variable.
 */
public class Monomials {

	private final int powerStartInclusive;
	private final int powerEndExclusive;

	/**
	 * Create a generator for monimials \( x^{k} \) with k from powerStartInclusive (inclusive) and powerEndExclusive (exclusive).
	 * If {@code powerStartInclusive > powerEndExclusive+1} an empty list is returned.
	 * 
	 * @param powerStartInclusive Smallest power.
	 * @param powerEndExclusive Largest power plus one.
	 */
	public Monomials(int powerStartInclusive, int powerEndExclusive) {
		super();
		this.powerStartInclusive = powerStartInclusive;
		this.powerEndExclusive = powerEndExclusive;
	}
	
	/**
	 * Create a list of monimials \( x^{k} \) with k from powerStartInclusive (inclusive) and powerEndExclusive (exclusive).
	 * If {@code powerStartInclusive > powerEndExclusive+1} an empty list is returned.
	 * 
	 * @param randomVariable The random variable x.
	 * @param powerStartInclusive Smallest power.
	 * @param powerEndExclusive Largest power plus one.
	 * @return The list of monomials.
	 */
	public static List<RandomVariable> of(RandomVariable randomVariable, int powerStartInclusive, int powerEndExclusive) {
		List<RandomVariable> monomials = new ArrayList<>();
		if(powerStartInclusive > powerEndExclusive+1) return monomials;
		
		int power = powerStartInclusive;
		RandomVariable monomial = randomVariable.pow(power);
		monomials.add(monomial);
		while(power++ < powerEndExclusive) {
			monomial = monomial.mult(randomVariable);
			monomials.add(monomial);
		}
		return monomials;
	}
	
	/**
	 * Create a list of monimials \( x^{k} \) with k from powerStartInclusive (inclusive) and powerEndExclusive (exclusive).
	 * 
	 * @param randomVariable The random variable x.
	 * @return The list of monomials.
	 */
	public List<RandomVariable> of(RandomVariable randomVariable) {
		return of(randomVariable, powerStartInclusive, powerEndExclusive);
	}
}
