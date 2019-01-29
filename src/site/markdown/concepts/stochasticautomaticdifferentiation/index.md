finmath-lib stochastic automatic differentiation
==========

****************************************

**Enabling finmath lib to utilize automatic differentiation algorithms (e.g. AAD).**

****************************************

This project implements a [stochastic automatic differentiation](http://ssrn.com/abstract=2995695).

The implementation is fast, memory efficient and thread safe. It handles automatic differentiation of the conditional expectation (American Monte-Carlo), see http://ssrn.com/abstract=3000822.

The project extends [finmath lib](http://finmath.net/finmath-lib) by
providing an interface <code>RandomVariableDifferentiable</code>
for random variables which provide automatic differentiation.
The interface extends <code>RandomVariable</code> and
hence allows to use auto-diff in all Monte-Carlo contexts
(via a replacement of the corresponding parameters / factories).

The project also provides implementations of this interface, e.g. utilizing
the backward (a.k.a. adjoint) method via <code>RandomVariableDifferentiableAADFactory</code>.
This factory creates a random variable <code>RandomVariableDifferentiableAAD</code> which implements <code>RandomVariableDifferentiable</code>.

All the backward automatic differentiation code is contained in
<code>RandomVariableDifferentiableAAD</code>.

The interface <code>RandomVariable</code> is provided by [finmath-lib](http://finmath.net/finmath-lib) and specifies the arithmetic operations which may be performed on random variables, e.g.,

    RandomVariableDifferentiable add(RandomVariableDifferentiable randomVariable);    
    RandomVariableDifferentiable mult(RandomVariableDifferentiable randomVariable);
    RandomVariableDifferentiable exp();
    
    // ...  

The interface <code>RandomVariableDifferentiable</code> will introduce
two additional methods:

    Long getID();   
    Map<Long, RandomVariable> getGradient();

The method <code>getGradient</code> will return a map providing the
first order differentiation of the given random variable (<code>this</code>)
with respect to *all* its input <code>RandomVariableDifferentiable</code>s (leaf nodes). To get the differentiation with respect to a specific object use

    /* Get the gradient of X with respect to all its leaf nodes: /*
    Map gradientOfX = X.getGradient();

    /* Get the derivative of X with respect to Y: */
    RandomVariable derivative = gradientOfX.get(Y.getID());

### AAD on Cuda GPUs

It is possible to combine the automatic-differentiation-extensions with the cuda-extensions. 
See https://github.com/finmath/finmath-lib-cuda-extensions 

Using

    AbstractRandomVariableFactory randomVariableFactory = new RandomVariableDifferentiableAADFactory();

will create a standard (CPU) random variable with automatic differentiation. Instead, using

    AbstractRandomVariableFactory randomVariableFactory = new RandomVariableDifferentiableAADFactory(new RandomVariableCudaFactory());

will create a Cuda GPU random variable with automatic differentiation.

### Example

The following sample code calculates valuation, delta, vega and rho for an
almost arbitrary product (here an EuropeanOption) using
AAD on the Monte-Carlo valuation

    RandomVariableDifferentiableAADFactory randomVariableFactory = new RandomVariableDifferentiableAADFactory();
    
    // Generate independent variables (quantities w.r.t. to which we like to differentiate)
    RandomVariableDifferentiable initialValue  = randomVariableFactory.createRandomVariable(modelInitialValue);
    RandomVariableDifferentiable riskFreeRate  = randomVariableFactory.createRandomVariable(modelRiskFreeRate);
    RandomVariableDifferentiable volatility    = randomVariableFactory.createRandomVariable(modelVolatility);
    
    // Create a model
    AbstractModel model = new BlackScholesModel(initialValue, riskFreeRate, volatility);
    
    // Create a time discretization
    TimeDiscretizationInterface timeDiscretization = new TimeDiscretization(0.0 /* initial */, numberOfTimeSteps, deltaT);
    
    // Create a corresponding MC process
    AbstractProcess process = new EulerSchemeFromProcessModel(new BrownianMotion(timeDiscretization, 1 /* numberOfFactors */, numberOfPaths, seed));
    
    // Using the process (Euler scheme), create an MC simulation of a Black-Scholes model
    AssetModelMonteCarloSimulationInterface monteCarloBlackScholesModel = new MonteCarloAssetModel(model, process);
    
    /*
     * Value a call option (using the product implementation)
     */
    EuropeanOption europeanOption = new EuropeanOption(optionMaturity, optionStrike);
    RandomVariable value = (RandomVariableDifferentiable) europeanOption.getValue(0.0, monteCarloBlackScholesModel);
    
    /*
     * Calculate sensitivities using AAD
     */
    Map<Long, RandomVariable> derivative = ((RandomVariableDifferentiable)value).getGradient();
        
    double valueMonteCarlo = value.getAverage();
    double deltaAAD = derivative.get(initialValue.getID()).getAverage();
    double rhoAAD = derivative.get(riskFreeRate.getID()).getAverage();
    double vegaAAD = derivative.get(volatility.getID()).getAverage();

