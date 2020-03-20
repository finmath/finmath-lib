# Automatic Tracking of Measurability

## Introduction

The _automatic tracking of measurability_ has been part of [finmath-lib](http://finmath.net/finmath-lib) since its earliest version (&approx; 2004). It adds a method to random variables to inspect their measurability. Its concept and implementation is similar to a forward mode automatic differentiation, in the sense that each operator is augmented with a corresponding operation on measurability and certain random variables (constants, Brownian increments) are initialized with specific values.

### Definition

For a random variable X we define a map T : X &rarr; [-&infin; , &infin;) such that for for t &ge; T(X) it is guaranteed that X is F<sub>t</sub>-measurable. It is not guaranteed that T(X) is the smallest such number, although in most cases the implementation can provide the smallest time.

### Applications

Originally the concept was introduces as a safe-guard in least-square-estimates of the conditional expectation (American Monte-Carlo), to ensure that the regression basis functions are F<sub>t</sub>-measurable if used in an F<sub>t</sub>-conditional expectation.
However, the concept allows for important optimizations in the context of _stochastic automatic differentiation_: it may be used to detect cases where the computationally expensive conditional expectation operator can be avoided. It is:
<center>E(X | F<sub>t</sub>) = X if t &ge; T(X).</center>
See [ssrn.com/abstract=3000822](https://ssrn.com/abstract=3000822) for the interaction with an AD/AAD algorithm.

## Implementation

Then random variable interface <code>net.finmath.stochastic.RandomVariable</code> provides a method <code>getFiltrationTime()</code> implementing T(X) such that for a random variable X represented by the object <code>X</code> and t calculated by t = <code>X.getFiltrationTime()</code> it is guaranteed that X is F<sub>t</sub>-measurable (note again: it is not guaranteed that t is the smallest such number, although in most cases the implementation can provide the smallest time).

The implementation is similar to a forward mode automatic differentiation, where the operator on random variables is augmented by additional operations on the filtration time:

Let T(X) denote the filtration time of X, i.e., T(X) = <code>X.getFiltrationTime()</code>. We apply the following definitions:

* For constant (deterministic) random variables C we set T(C) = -&infin;
* For Brownian increments W(t+&Delta;t)-W(t) we set T(W(t+&Delta;t)-W(t)) = t+&Delta;
* For operators f on random variables X<sub>1</sub>,...,X<sub>n</sub> with Z = f(X<sub>1</sub>,...,X<sub>n</sub>) we set T(Z) = max(T(X<sub>1</sub>),...,T(X<sub>n</sub>))

This definition of T already fulfills the requirement that t = T(Z) is a time guaranteeing that Z is F<sub>t</sub>-measurable, however it may not give the smallest such filtration time. There are a few optimizations or special cases which can be implemented, e.g. for Z = 0 * X we have T(Z) = T(0) (instead of T(Z) = max(T(0),T(X)) = T(X)).

Note: This definition already covers the generation of the correct filtration times for an Euler-scheme of an Ito process.

An implementation may achieve this by augmenting (extending) the type X by (X,t) with t = T(X) and overloading operators on X.

### Example

The method is implemented in <code>RandomVariableFromDoubleArray</code> implementing the interface <code>RandomVariable</code> in finmath-lib. The interface is given as (extract)

	public interface RandomVariable {
		double getFiltrationTime();
		public RandomVariable add(RandomVariable randomVariable);
		
		// ... (declaration of other methods)
		
	}

and the implementation is given as (extract) 

	public class RandomVariableFromDoubleArray implements RandomVariable {
	
		private final double time; // Time (filtration)
	
		@Override
		public double getFiltrationTime() {
			return time;
		}
		
		// ... (implementation of other methods)
		
		@Override
		public RandomVariable add(RandomVariable randomVariable) {
			// Set time of this random variable to maximum of time with respect to which measurability is known.
			double newTime = Math.max(time, randomVariable.getFiltrationTime());
			
			// ... (calculate newRealizations as sum of this and randomVariable)
			
			return new RandomVariable(newTime, newRealizations);
		}
	}


## References

For an application to AAD for American Monte-Carlo simulation see 

Fries, Christian P., Automatic Backward Differentiation for American Monte-Carlo Algorithms - ADD for Conditional Expectations and Indicator Functions (June 27, 2017). Available at SSRN: https://ssrn.com/abstract=3000822
