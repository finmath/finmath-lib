/**
 * 
 */
package net.finmath.montecarlo;

import net.finmath.stochastic.RandomVariableInterface;

/**
 * @author Stefan Sedlmair
 *
 */

public class RandomVariableUniqueVariableTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		RandomVariableUniqueVariable a = new RandomVariableUniqueVariable(5.0 , new double[]{1.0, 2.0, 3.0 ,4.0, 10.0});
		System.out.println(a);
		
		RandomVariableUniqueVariable b = new RandomVariableUniqueVariable(7.0, new double[]{1.0, 2.0, 3.0, 5.0, 6.0});
		System.out.println(b);
		
		RandomVariableUniqueVariable c = (RandomVariableUniqueVariable) a.add(b);
		System.out.println(c);

		RandomVariableUniqueVariable d = (RandomVariableUniqueVariable) c.mult(b);
		System.out.println(d);
		
		RandomVariableUniqueVariable e = (RandomVariableUniqueVariable) a.div(d);
		System.out.println(e);		
		
		RandomVariableUniqueVariable f = (RandomVariableUniqueVariable) e.exp();
		System.out.println(f);
		
		RandomVariableInterface[] g =  f.getGradient();
		for(int i = 0; i < g.length; i++){
			System.out.println((RandomVariable)g[i]);
		}
		
	}
	

}
