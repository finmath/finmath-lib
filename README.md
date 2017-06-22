# finmath-lib automatic differentiation extentions

This project provides and interface (<code>RandomVariableDifferentiableInterface</code>)
for random variables which provide automatic differentiation. The interfaces
extends RandomVariableInterface and hence allows to use auto-diff in all
Monte-Carlo contexts (via a replacement of the corresponding parameters / factories).

The project provides an implementation of the backward (a.k.a. adjoint)
method via RandomVariableDifferentiableAADFactory.

The interface RandomVariableDifferentiableInterface will introduce
two additional methods:

	Long getID();	
	Map<Long, RandomVariableInterface> getGradient();

The method <code>getGradient</code> will return a map providing the first order
differentiation of the given random variable (this) with respect to
*all* its input <code>RandomVariableDifferentiableInterface</code>s (leave nodes). To get the differentiation with respect to a specific object use

	Map gradient = X.getGradient();
	RandomVariableInterface derivative = X.get(Y.getID());
	
	
