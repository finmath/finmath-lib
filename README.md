# finmath-lib automatic differentiation extentions
- - - -
**Enabling finmath lib to utilize automatic differentiation algorithms (e.g. AAD).**
- - - -
This project implements a [stochastic automatic differentiation](http://ssrn.com/abstract=2995695).

The project provides an interface (<code>RandomVariableDifferentiableInterface</code>)
for random variables which provide automatic differentiation.
The interface extends <code>RandomVariableInterface</code> and
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

### Example

The following sample code calculate delta, vega and rho for an
almost arbitrary product (here an EuropeanOption) using
AAD on the Monte-Carlo valuation

	RandomVariableDifferentiableAADFactory randomVariableFactory = new RandomVariableDifferentiableAADFactory();
	
	// Generate independent variables (quantities w.r.t. to which we like to differentiate)
	RandomVariableDifferentiableInterface initialValue	= randomVariableFactory.createRandomVariable(modelInitialValue);
	RandomVariableDifferentiableInterface riskFreeRate	= randomVariableFactory.createRandomVariable(modelRiskFreeRate);
	RandomVariableDifferentiableInterface volatility	= randomVariableFactory.createRandomVariable(modelVolatility);
	
	// Create a model
	AbstractModel model = new BlackScholesModel(initialValue, riskFreeRate, volatility);
	
	// Create a time discretization
	TimeDiscretizationInterface timeDiscretization = new TimeDiscretization(0.0 /* initial */, numberOfTimeSteps, deltaT);
	
	// Create a corresponding MC process
	AbstractProcess process = new ProcessEulerScheme(new BrownianMotion(timeDiscretization, 1 /* numberOfFactors */, numberOfPaths, seed));
	
	// Using the process (Euler scheme), create an MC simulation of a Black-Scholes model
	AssetModelMonteCarloSimulationInterface monteCarloBlackScholesModel = new MonteCarloAssetModel(model, process);
	
	/*
	 * Value a call option (using the product implementation)
	 */
	EuropeanOption europeanOption = new EuropeanOption(optionMaturity, optionStrike);
	RandomVariableInterface value = (RandomVariableDifferentiableInterface) europeanOption.getValue(0.0, monteCarloBlackScholesModel);
	
	/*
	 * Calculate sensitivities using AAD
	 */
	Map<Long, RandomVariableInterface> derivative = ((RandomVariableDifferentiableInterface)value).getGradient();
		
	double valueMonteCarlo = value.getAverage();
	double deltaAAD = derivative.get(initialValue.getID()).getAverage();
	double rhoAAD = derivative.get(riskFreeRate.getID()).getAverage();
	double vegaAAD = derivative.get(volatility.getID()).getAverage();

