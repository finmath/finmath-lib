# finmath-lib automatic differentiation extentions
- - - -
**Enabling finmath lib to utilize automatic differentiation algorithms (e.g. AAD).**
- - - -
This project provides an interface (<code>RandomVariableDifferentiableInterface</code>)
for random variables which provide automatic differentiation.
The interfaces extends <code>RandomVariableInterface</code> and
hence allows to use auto-diff in all Monte-Carlo contexts
(via a replacement of the corresponding parameters / factories).

The project also provides implementations of this interface, e.g. utilizing
the backward (a.k.a. adjoint) method via <code>RandomVariableDifferentiableAADFactory</code>.

The interface <code>RandomVariableDifferentiableInterface</code> will introduce
two additional methods:

	Long getID();	
	Map<Long, RandomVariableInterface> getGradient();

The method <code>getGradient</code> will return a map providing the
first order differentiation of the given random variable (<code>this</code>)
with respect to *all* its input <code>RandomVariableDifferentiableInterface</code>s (leave nodes). To get the differentiation with respect to a specific object use

	/* Get the gradient of X with respect to all its leave nodes: /*
	Map gradientOfX = X.getGradient();

	/* Get the derivative of X with respect to Y: */
	RandomVariableInterface derivative = gradientOfX.get(Y.getID());



	
