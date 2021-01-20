package net.finmath.montecarlo.automaticdifferentiation.backward.alternative;

import net.finmath.stochastic.RandomVariable;

/**
 * @author Stefan Sedlmair
 *
 */

public class RandomVariableUniqueVariableTest {

	/**
	 * @param args
	 */
	public static void main(final String[] args) {

		final RandomVariableUniqueVariable a = new RandomVariableUniqueVariable(5.0 , new double[]{1.0, 2.0, 3.0 ,4.0, 10.0});
		System.out.println(a);

		final RandomVariableUniqueVariable b = new RandomVariableUniqueVariable(7.0, new double[]{1.0, 2.0, 3.0, 5.0, 6.0});
		System.out.println(b);

		final RandomVariableUniqueVariable c = (RandomVariableUniqueVariable) a.add(b);
		System.out.println(c);

		final RandomVariableUniqueVariable d = (RandomVariableUniqueVariable) c.mult(b);
		System.out.println(d);

		final RandomVariableUniqueVariable e = (RandomVariableUniqueVariable) a.div(d);
		System.out.println(e);

		final RandomVariableUniqueVariable f = (RandomVariableUniqueVariable) e.exp();
		System.out.println(f);

		final RandomVariable[] g =  f.getGradient();
		for(int i = 0; i < g.length; i++){
			System.out.println(g[i]);
		}

	}


}
