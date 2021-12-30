finmath lib Release Notes
==========

****************************************

# Release Notes

## 6.0.0 (30.12.2021)

### General

 In version 6.0.x several small refactorings will be done. This may take place over multiple patch versions x.
 
 - Refactor rename: AbstractLIBORMonteCarloProduct to AbstractTermStructureMonteCarloProduct
 

## 5.1.3 (12.03.2021)

### RandomVariableFactory

 - Fixed issue where default value is used if value is of type RandomVariable. (The bug was not relevant in the current implementation of the models, since models used the method with type Double).


## 5.1.2 (11.03.2021)

### General

 - Performance improvements for the LIBOR Market Model / Discrete Term Structure Model.
 - Added log messages to Levenberg Marquardt algorithm.


## 5.1.1 (24.01.2021)

### Monte-Carlo Interest Rate Models

 - Added a property to the `LIBORMarketModelFromCovarianceModel` to choose the interpolation method of simulation time: either rounding down or rounding to the nearest time. The "rounding down" preserves the propery, that the rate is (at least) F_t-measurable. The "rounding nearest" improves the interpolation of options with respect to option  maturity.


## 5.1.0 (24.01.2021)

### Monte-Carlo Interest Rate Models

#### Refactorings

 - The `TermStructureMonteCarloProduct` getValue method requires only a `TermStructureMonteCarloSimulationModel` as model. The stronger interface `LIBORModelMonteCarloSimulationModel` is rarely needed. Cases where it is used should be checked in future versions.
 - The name of the method `getLIBOR` has been replaced by `getForwardRate`. A default implementation provides `getLIBOR` for backward compatibility.
 - Interest rate Monte-Carlo simulations implement the slightly weaker interface `TermStructureMonteCarloSimulationModel`. The class `TermStructureMonteCarloSimulationFromTermStructureModel` implements the weaker interface `TermStructureMonteCarloSimulationFromTermStructureModel`. This is used in valuation of interest rate products. This interface does not provide the methods related to forward rate period discretizations - `getLIBORPeriodDiscretization`. In case it is needed, there is still a `LIBORMonteCarloSimulationFromTermStructureModel` implementing `LIBORModelMonteCarloSimulationModel` for backward compatibility (however, here the corresponding methods thrown an exception). (This was the case in the previous version). As before, the class `LIBORMonteCarloSimulationFromLIBORModel` implements `LIBORModelMonteCarloSimulationModel` and provides a getLIBORPeriodDiscretization(). 
 - Refactor rename TermStructureFactorLoadingsModelInterface to TermStructureFactorLoadingsModel
 - Refactor rename TermStructureFactorLoadingsModelParametricnterface to TermStructureFactorLoadingsModelParametric
 - Refactor rename TermStructureCovarianceModelInterface to TermStructureCovarianceModel
 - Refactor rename TermStructureTenorTimeScalingInterface to TermStructureTenorTimeScaling


## 5.0.10 (24.01-2021)

### Analytic Formulas

 - Some minor additions to the analytic formulas.

### Monte-Carlo Interest Rate Models

 - Fixed a bug in the volatility return type (ValueUnit) of the Caplet.


## 5.0.9 (26.12-2020)

### Analytic Formulas

 - Some minor additions to the analytic formulas.

### Bond curves and Bond valuation

 - Bonds can be valued on a single discount curve
 (instead a set of three curves consisting of (separate risk free curve, basis curve, survival prob. curve)).
 
### Algorithmic Differentiation / Automatic Differentiation

 - Added unit tests.

### Java 8

 - Backporting fixes from 5.0.6 to the Java 8 branch.


## 5.0.6 (03.10-2020)

### General

 - Improved exceptions.
 
### Algorithmic Differentiation / Automatic Differentiation

 - Improvement to AAD for indicators.
 - Fixed an issue where (in a very special non-default setting), an algorithmic differentiation of an indicator function did not carry out the inner derivative.
 
### Monte-Carlo Single Asset Model

 - Monte-Carlo Merton model supports use of RandomVariableFactory. (Hence:)
 - Monte-Carlo Merton model supports AAD.
 

## 5.0.5 (16.08-2020)

### Equity European and American Options with Local Volatility (SVI) and Dividents (Bühler 2007)

 - Under the package `net.finmath.equities` the valuation of European and American options with Local Volatility (SVI) and Dividents (Bühler 2007) has been contributed by Andreas Grotz.

### Monte-Carlo Single Asset Model

 - Fixed a bug in the implementation of `DigitalOptionDeltaLikelihood` related to weighted Monte-Carlo simulation (weights were ignored).
 - Added some getters.


## 5.0.4 (28.07.2020)

### General

 - Removed Kotlin dependency - there is a separate project now: finmath-lib-kotlin.

### Monte-Carlo Single Asset Model

 - Fixes to getCloneWithModifiedData to support more parameters.

### Monte-Carlo Multi-Asset Black-Scholes Model

 - Multi-asset Black-Scholes model allows construction from factor loadings and volatility-and-correlation.
 - Added unit tests.
 

## 5.0.3 (05.07.2020)

### General

 - Added some convenient methods in AnalyticFomulas consuming RandomVariable parameters.
 - Interface RandomVariable contains convenient default methods for variance and covariance.
 - TimeDiscretization can be used as DoubleStream.

### Monte-Carlo Single Asset Model

 - MonteCarloBlackScholesModel extends MonteCarloAssetModel
 

## 5.0.2 (12.06.2020)

### General

 - Minor improvements (additional c'tors).

### Random number generators

 - Added some quasi random number generators.


## 5.0.1 (10.05.2020)

### Numerical Integration

 - Exact integration of functions of piecewise constant functions (useful for intensity models). See `net.finmath.integrationnet.finmath.integration.PiecewiseContantDoubleUnaryOperator`.
 
### Analytic valuation models

 - CDS valuation (contributed by Matthias Föhr).


## 5.0.0 (12.04.2020)

 - Refactoring and cleanup (deprecation) from 4.1.8. See [migration/4.1.x-to-5.0.0](migration/4.1.x-to-5.0.0).

### Monte-Carlo Simulation Models

 - The state space transform is time dependent, i.e., has timeIndex as addition argument. This allows more accurate numerical schemes.
 - The `ProcessModel` has to be passed as the first argument to the methods `getInitalState`, `getDrift`, `getFactorLoading`, `applyStateSpaceTransfrom`.
 

## 4.1.8 (12.04.2020)

### General

 - RandomOperator is a synonym for Function<RandomVariable,RandomVariable>
 - RandomVariables come with apply(RandomOperator).
 
### Analytic Formulas

 - More convenient variants of Bachelier formula (classic, homogeneous, inhomogeneous).

### Interest Rate Models (Monte-Carlo)

 - Generic Cross-Currency LIBOR Market Model (with generic quanto adjustment).
 - LIBOR Market Model with SABR and ZABR stochastic volatility (unit test).
 - Improve analytic calibration of normal LIBOR Market Model.

### Equity Models (Monte-Carlo)

 - Reworked Monte-Carlo implementation of Displaced Lognormal Models.


## 4.1.7 (22.03.2020)

### General

 - Prepearation of the refactoring introduces in 5.0. The `ProcessModel` and `MonteCarloProcess` model do not hold a reference to the process anymore.
   Instead, a reference to the `ProcessModel` has to be passed as the first argument of methods like `getInitalState`, `getDrift`, `getFactorLoading`.
   Starting with version 4.1.4 of the library default implementations of these methods are introduces while the old methods are marked as deprecated.
   For a guideline of migrating any implementation see [migration/4.1.x-to-5.0.0](migration/4.1.x-to-5.0.0).

### Random Number Generators

 - An interface `net.finmath.randomnumbers.RandomNumberGenerator1DRandomNumberGenerator1D` has been added, using default implementation to extend `net.finmath.randomnumbers.RandomNumberGenerator1DRandomNumberGenerator`.

### Fourier Methods

 - Variance Gamma model (contributed and maintained by Alessandro Gnoatto)


## 4.1.2

### General

 - Moved site to GitHub pages.
 - Improved JavaDoc.
 - Improved Documentation.


## 4.1.1

### General

 - Improved toString() for nicer output in jshell

### Refactorings

 - Removed deprecated methods (setXxx) in AnalyticModel - the class is now immutable

 - Refactoring rename RandomVariableDifferentiableFactory: AbstractRandomVariableDifferentiableFactory extract interface RandomVariableDifferentiableFactory

## 4.1.0 (26.01.2020)

### General

 - Clean up. Arguments of methods are final (when possible).

### Refactorings

 - Refactoring rename RandomVariableFactory: RandomVariableFactory to RandomVariableFromArrayFactory
 - Refactoring rename RandomVariableFactory: AbstractRandomVariableFactory extract interface RandomVariableFactory

 - Refactoring rename Notional: Notional to NotionalFromConstant
 - Refactoring rename Notional: AbstractNotional to Notional

 - Refactoring rename SwaptionMarketData: SwaptionMarketData to SwaptionATMMarketDataFromArray
 - Refactoring rename SwaptionMarketData: AbstractSwaptionMarketData to SwaptionMarketData
 
### Documentation

 - Added documentation of Model and Product interfaces.
 - Added script (Eclipse) for refactorings 4.0.x to 4.1.0
 - Added documentation for refactorings 4.0.x to 4.1.0
 
### Interest Rate Models (Analytic)

 - Added code for caplet bootstrapping and caplet volatility tenor conversion.


## 4.0.15 (26.01.2020)

### General

 - Minor clean up.
 
### Market Data

 - Discount Curves carry a reference date.
 
### Monte-Carlo Simulation / AAD

 - Added PREDICTOR_CORRECTOR_FUNCTIONAL scheme, being compatible with AAD.

### Interest Rate Models - Monte-Carlo Simulation

 - Changed scaling of numeraire if default curve is given for measures where N(0) is not equal to 1.


## 4.0.13 (05.10.2019)

### General

 - Added some constructors for convenience.
 
### Monte-Carlo Simulation / AAD

 - Improved compatibility with AAD valuations - EULER_FUNCTIONAL is default in Euler scheme.


## 4.0.12 (02.06.2019)

### Fourier Methods (contributed and maintained by Prof. A. Gnoatto)

 -  Variance Gamma Model:  Fourier and Monte-Carlo implementations for the Variance Gamma Model.

### Interest Rate Models - Analytic and Semi-Analytic (this part is still under development / refactoring) - see `net.finmath.singleswaprate`

 - SABR Volatility Cube Calibration (contributed by Roland Bachl)
 - Cash-Settled Swaption and CMS Valuation via Replication using different Annuity Mappings (contributed by Roland Bachl)
 
### Interest Rate Models - Monte-Carlo Simulation

 - Improved implementation of the Bermudan Swaption. See `BermudanSwaptionFromSwapSchedules`.


## 4.0.11 (22.04.2019)

### General

 - The `net.finmath.time.TimeDiscretizationFromArray` comes with an additional (optional) argument to allow duplicate simulation time discretization points (i.e., time step sizes of 0) (this is useful to ensure path-consistent Monte-Carlo simulations).

### Market Data / Curves

 - Allowing access to interpolation points for interpolated curve.

### Fourier Methods (contributed and maintained by Prof. A. Gnoatto)

 - Implementation of the Merton Model. See `net.finmath.fouriermethod.models.MertonModel`.
 
### Monte-Carlo Simulation

 - The LIBOR Market Model () and the Hull White Model () may be used with simulation time discretizations having duplicate entries (this is useful to ensure path-consistent Monte-Carlo simulations).
 
 - Introduced `MonteCarloConditionalExpectationRegressionFactory`.

### Single Asset (Equity/FX) Models - Monte-Carlo Simulation

 - Implementation of the Variance Gamma Process (contributed and maintained by Prof. A. Gnoatto). See `net.finmath.montecarlo.VarianceGammaProcess`.

### Interest Rate Models - Monte-Carlo Simulation

 - Improved implementation of the Bermudan Swaption allowing arbitrary swap schedules (consistent with AnalyticModel) and injection of custom method for estimation of conditional expectation. See `BermudanSwaptionFromSwapSchedules`.


## 4.0.9 (07.04.2019)

### General

 - Introduced the interface `net.finmath.montecarlo.process.ProcessTimeDiscretizationProvider` which can be implemented by products to give a hint on a preferred time discretization of a model.
 
 - Stricter checkstyle rules: checking for visibility of filed. Fields have to be private.

### Market Data

 - Improved serialization for ScheduleDescriptors.

 - Improved serialization for Swaps.
 
### Interest Rate Models

 - Added `net.finmath.montecarlo.interestrate.products.BermudanSwaptionFromSwapSchedules` - an implementation of a Monte-Carlo valuation of a Bermudan swaption using exact swap schedules.
 
 - `SwaptionFromSwapSchedules` implements `ProcessTimeDiscretizationProvider`.

 - `BermudanSwaptionFromSwapSchedules` implements `ProcessTimeDiscretizationProvider`.

 
## 4.0.8 (31.03.2019)

### Interest Rate Models

 - Added `net.finmath.montecarlo.interestrate.products.SwaptionFromSwapSchedules` - an implementation of a Monte-Carlo valuation of a swaption based on swap schedules, being compatible with AAD.


## 4.0.7 (24.03.2019)

### General

 - The `getCloneWithModifiedData` allows to create a copy of models with modified random variable factory. This allow to create AAD enabled models from model without AAD support. The typical use is to perform a model calibration without AAD random variables, then inject the AAD dependency and calculate sensitivities.
 
 - Minor updates to documentation.

### Interest Rate Models

 - Added support for different interest rate curve interpolation methods.


## 4.0.5 (27.01.2019)

### General

 - Some minor refactorings:
    - `CurveFromInterpolationPoints` renamed to `CurveInterpolation`.
    - Fixed typo in class name `AnalyticModelFromCurvesAndVols`.
    
### Algorithmic Differentiation / Interest Rate Models

 - The Hull White Model (`HullWhiteModel`) and the Short Rate Volatility Models (implementing `ShortRateVolatilityParametricModel` support
   objects implementing `RandomVariable` as parameters.
   This enables the model to use Algorithmic Differentiation and Adjoint Algorithmic Differentiation.

### Interest Rate Models

 - A unit test for the calibration of the Hull-White model has been added.
 

## 4.0.1 (20.01.2019)

### General

 - *Refactoring of names*. Interfaces come with plain names like `RandomVariable` 
   while implementation come with implementation details as in `RandomVariableFromDoubleArray`.
   Details can be found in docs/refactoring-3.6.x-to-4.0.0.

 - *Refactoring of `RandomVariable`*. Removed deprecated methods: The method `barrier` has been removed from RandomVariable. 
   Use `choose` instead.

 - *Refactoring: Interest rate model plugins (covariance models)*. Refactoring of methods: the method `getParameter` returns a
   `RandomVariable[]` to allow for generic parameters, possibly enabling AAD. A method getParameterAsDoubleArray is provided
   for convenience in case the random variable parameter is deterministic.

 - The packages `net.finmath.montecarlo.interestrate.models.modelplugins` and `net.finmath.montecarlo.interestrate.models.covariancemodels` 
   have been merged. The resulting package has been renamed to `net.finmath.montecarlo.interestrate.models.covariance`.
 
 - Refactoring: Package hierarchy. 1) A package `net.finmath.montecarlo.interestrate.models` is introduced.
   The classes implementing `ProcessModel` (or extending `AbstractProcessModel`) for the `HullWhiteModel` and LMM are moved into this package (from `net.finmath.montecarlo.interestrate`). The packages covariancemodels, modelplugins, factordrift are moved into this package. 2) A package `net.finmath.montecarlo.assetderivativevaluation.models` is introduced. The classes implementing ProcessModel (or extending `AbstractProcessModel`) for the `BlackScholes`, `Bacheler`, `Merton`, `Bates`, etc. are moved into this package (from `net.finmath.montecarlo.assetderivativevaluation`).

### Linear Algebra

 - Added *solve linear equation* with *Tikhonov regularization*.
    

## 3.6.3 (15.01.2019)

### General

 - Some interfaces have default methods to allow compatibility with the 4.0.0 classes


### Hull-White Model

 - Added generic calibration of Hull-White model (adaption of the LMM calibration)
   (adaption contributed by Ruben).


## 3.6.2 (29.12.2018)

### General

 - Added interface for models which generically report the independent model parameters.
   
### Automatic Differentiation

 - Added unit test showing the generic calculation of sensitivities (implicit function 
   theorem).


## 3.6.1 (02.12.2018)

### Automatic Differentiation

- Added unit test for AAD delta in LIBOR Market Model.

- The behavior of the differentiation of discontinuities can be controlled by the keys
  `diracDeltaApproximationMethod`, `diracDeltaApproximationWidthPerStdDev`, `diracDeltaApproximationDensityRegressionWidthPerStdDev`;

- The key `barrierDiracWidth` is deprecated and replaced by `diracDeltaApproximationWidthPerStdDev`.


## 3.6.0 (25.11.2018)

### General

- The `choose` method has been added to the RandomVariableInterface.
- The `barrier` method has been marked deprecated in RandomVariableInterface.

### Automatic Differentiation

- Setting the property `barrierDiracWidth` to `Double.POSITIVE_INFINITY` results 
  in a replacement of the derivative of the indicator function by 1. This option will 
  be useful in an accurate estimation of the derivative of the indicator function.
- Implementation of the density regression for the derivative of indicator functions, 
  see https://ssrn.com/abstract=3282667


## 3.5.0 (04.11.2018)

### LIBOR Market Model

- The LIBORMarletModel provides the numeraire adjustments through a method. This method  
  is required for - e.g. - a sensitivity based calculation of an MVA.


## 3.4.4 (02.11.2018)

### General

- Minor bug fixes (NPE in toString() method in AnalyticModel if referenceDate is missing).
- Merged with development branch of product descriptors: added initial version of FPML parser.

### LIBOR Market Model

- Improved performance and consistency of numeraire interpolation.
- Ensured that numeraire cache is thread safe (only relevant when using automatic differentiation)


## 3.4.0 (23.09.2018)

### Adjoint Algorithmic Differentiation

- Added Automatic Type Tracking / Operator Result Type Priorities to RandomVariableInterface implementations. For details on the concet of "Operator Result Type Priorities" see https://ssrn.com/abstract=3246127
- Improved test coverage: adding forward mode differentiation to some parametrized unit test.


## 3.3.4 (07.09.2018)

### Adjoint Algorithmic Differentiation

- Fixed bug in the implementation of floor operator.
- Improved unit test for vega test. 
- Preparations for Operator Result Type Priorities.


## 3.3.3 (02.09.2018)

### General

- Fixed typos.

### Monte-Carlo Simulation

- Added the interface RegressionBasisFunctionProvider for American Monte-Carlo algorithms.
- Added the ability to use products implementing AbstractMonteCarloProduct as regression basis functions.

### Monte-Carlo Interest Rate Models

- The Option component allows specification of a RegressionBasisFunctionsProvider.
 
### LIBOR Market Model

- The LIBORIndex supports automatic adjustment of a tenor basis spread if the index name relates to a curve in the LIBOR market model analytic model.
- Added a unit test for the LIBORIndex testing the value of a FRA on different forward curves.


## 3.3.2 (29.07.2018)

### General

- Minor improvements.
- Fixed an incompatibility in the LIBOR Market Model spreadsheet.


## 3.3.1 (22.07.2018)

### General

- Integrated finmath-lib-automaticdifferentiation-extensions (1.1.0) into finmath-lib.

## 3.2.17 (19.07.2018)

### Interest Rate Products

- Added additional features to Bermudan Swaption, Swaption and Swap to support initial 
  margin project. The Bermudan swaption reports the stochastic exerciseTime in the result map.


## 3.2.14 (16.07.2018)

### General

- Requiring some checkstyle checks to be passed.
- Improving API documentation (JavaDoc).
- Improvements to TimeDiscretizationInterface and TimeDiscretization (see pull request #60).
- More unit tests.

### LIBOR Market Model

- Small internal change: Introduced interface LIBORCovarianceModelCalibrateable and changed argument calibrationTargetValues 
  to RandomVariableInterface[]. This step was just to ease the integration of the AAD 
  calibration, where the getParameters method of the covariance models results in a 
  RandomVariableInterface[].
  

## 3.2.11 (07.07.2018)

### General

- Clean up and improvements to JavaDoc.

### Monte-Carlo Simulation

- Added algorithm implementing acceptance rejection method.

## 3.2.10 (30.06.2018)

### Modelling / Interfaces

- Clean up of the model hierarchy.

### Model and Product Interfaces

- Partially reverting the refactoring from 3.2.0 (removing type parameter from model 
  and product interfaces). Adding dedicated interfaces `DecribedProduct<T extends ProductDescriptor>` and `DecribedModel<T extends ModelDescriptor>`
  for model and products allowing construction from product descriptors. For details 
  see [concepts "separation of product and model"](concepts/separationofproductandmodel).
- The interface `net.finmath.modelling.Model<T extends ModelDescriptor>` has been renamed/refactored to `net.finmath.modelling.ModelInterface`.
- The interface `net.finmath.modelling.Product<T extends ProductDescriptor>` has been renamed/refactored to `net.finmath.modelling.ProductInterface`.
- Adding (extracting) additional interfaces `DescribedModel<T extends ModelDescriptor>` 
  and `DescribedProduct<T extends ProductDescriptor>`.

### Finite Difference Methods

- Merged initial version of the finite difference methods.
- Added a unit test illustrating the use of model descriptors and product descriptors 
  for a Black-Scholes model and a European option on a Monte-Carlo-, Fourier-Transform-, and 
  Finite-Difference-Method-Factory.

### Documentation

- Model and product interfaces are described in [concepts "separation of product and model"](concepts/separationofproductandmodel).
  
## 3.2.5 (09.06.2018)

### Market Data: Bond Curves

- Added implementation for Bond curve calibration via Local Linear Regression (see https://ssrn.com/abstract=3073942 ). See package net.finmath.marketdata.model.bond

### LIBOR Market Model

- Fixed an inconsistency in the LMM curve interpolation. The LMM curve interpolation is now 100% consistent with the interpolation of the provided forward/discount curve, given that the forward curves allows for an interpolation on short periods (Note: not all forward curves allow this, positive example are forward curves from discout curves and NSS curves).

## 3.2.3 (10.05.2018)

### LIBOR Market Model

- LIBOR Volatility and Correlation models are immutable.

The method `setParameter` has been replaced by
`getCloneWithModifiedParameter`.

Instead of

	newModel = model.clone();
	newModel.setParameter(parameter);

call

	newModel = model.getCloneWithModifiedParameter(parameter);


## 3.2.2 (05.05.2018)

### Monte-Carlo Simulation

- API Change: The implementation of the method getQuantile of RandomVariableInterface has been changed to comply with its JavaDoc. In case backward compatibbility is requied replace getQuantile(x) by getQuantile(1.0-x)
- Performance improvement by caching of SVD solver. This has some relevance for the valuation of forward sensitivities or MVA, see https://ssrn.com/abstract=3018165
- Small improvement to exception message.

### LIBOR Market Model

- Fixed bug which could lead to race condition when performing a multi-threadded calibration of the correlation.
- LMM covariance model is serializable using Java standard serializer.

### General Improvements

- Improved JavaDoc.
- Updated maven plug-ins.

## 3.2.0 (18.02.2018)

### General Refactoring - Interfaces (see Concepts / Separation of Model and Products)

- The interface `net.finmath.modelling.ModelInterface` has been renamed/refactored to `net.finmath.modelling.Model<T extends ModelDescriptor>`. This is related to the introduction of ModelDescriptors and ProductDescriptors. The interface was previously a marker interface, but now comes with two methods.
- The interface `net.finmath.modelling.ProductInterface` has been renamed/refactored to `net.finmath.modelling.Product<T extends ProductDescriptor>`. This is related to the introduction of ModelDescriptors and ProductDescriptors. The interface was previously a marker interface, but now comes with two methods.
- Classes implementing the interface `Product<T>` require a type parameterization, example: `AbstractMonteCarloProduct<T extends ProductDescriptor> implements Product<T>`
- Heston model may be constructed by a model descriptor. See `net.finmath.modelling.descriptor.HestonModelDescriptorTest` for an example.

## 3.1.5 (05.11.2017)

### Monte-Carlo models

- Small performance improvement for Monte-Carlo models. RandomVariable implementation avoids use of apply.

## 3.1.3 (28.10.2017)

### Analytic models - Curves

- Added serializability. For example for AnalytModel and curves. The AnalyticModel may be serialized to disk and loaded from disk. This is useful in curve calibration spreadsheets.

## 3.1.0 (30.09.2017)

### Monte-Carlo models

- RandomVariableInterface gets an additional method doubleValue() which will return the value of random variable if it is deterministic. So instead of getAverage() you may call average().doubleValue(). This addition is to allow the consistent use of
deterministic random variables in analytic model. The advantage of this approach is the possibility of dependency injection (using stochastic automatic differentiation) and the possiblity to use stochastic quantities in analytic models ("stochastic curves").

### Analytic models - Curves

- Introduction of "stochastic curves". The package net.finmath.analytic is a port of net.finmath.marketdata where all curve object operate on RandomVariableInterface. This allows AAD calibration and "stochastic curves".

### Optimizer

- Introduction of the stochastic Levenberg Marquardt algorithm.

## 3.0.14 (20.08.2017)

### Monte-Carlo models

- AbstractModelInterface requires the inverse of the state-space-transform. This enables ProcessEulerScheme to construct a sequential dependency structure (this is useful for AAD).
- ProcessEulerScheme offers the scheme EULER_FUNCTiONAL (constructs a sequential dependency structure (this is useful for AAD))

### Valuation using Fourier transforms

-- Added discount rate as optional parameter to Heston and Bates model.

## 3.0.12 (19.08.2017)

### Valuation using Fourier transforms

- Added characteristic function of Heston model - allows semi-analytic valuation of option prices under Heston model.
- Added characteristic function of Bates model - allows semi-analytic valuation of option prices under Bates model.

### Support for finmath-lib automatic differentiation extensions: Interface chances to RandomVariableInterface and Monte-Carlo models

- Minor changes to RandomVariableInterface to support the finmath-lib automatic differentiation extensions.
- Minor changes to RandomVariableFactories to support the finmath-lib automatic differentiation extensions.
- Monte-Carlo models may now take their own factory for RandomVariableInterface objects. This allows injection of AAD capable random varaibles.
- Minor changes to BermdanOption valuation to support the finmath-lib automatic differentiation extensions.

### Improvements

- Fixed a scaling issue in the conditional expectation estimations, improving the result of products using the Option component.

### Other

- Java 8 version is the default in the Eclipse .classpath file.

## 3.0.0 (27.05.2017)

### Java 6 version of finmath-lib switched from joda-time to threeten-backport

- The Java 6 version of finmath-lib switches from jado-time to threeten-backport for the implementaton of LocalDate. This reduces the difference between the Java 6 code base and the Java 8 code base.
- The Java 8 version is not affected.
- The 2.4.x version using the joda-time version of LocalDate is maintained on the branch named "2.4.x"

### Introducing a dedicated class for the conversion between LocalDate and Double.

- Finmath lib allows to use Double to specify dates/times and date/time-intervals. The motivation of this is to allow a simplified "textbook-like" definition of models, which is often sufficient for research purposes.. With respect to industry/practical applications it is often necessary to consider exact date-based measurment of time, e.g. using date-rolling and daycount conventions. To achive a clear 1:1 mapping, the library fixes a conversion given by a) a reference date and b) a daycount convention (ACT/365).
- The conversion from LocalDate to Double is centralized in the class FloatingpointDate.
- The motivation for ACT/365 is performance (easy conversion). Apart from this there is no specific reason except the need to make some choice.

## 2.4.4 (20.05.2017)

### Introducing a dedicated class for the conversion between LocalDate and Double.

- Finmath lib allows to use Double to specify dates/times and date/time-intervals. The motivation of this is to allow a simplified "textbook-like" definition of models, which is often sufficient for research purposes.. With respect to industry/practical applications it is often necessary to consider exact date-based measurment of time, e.g. using date-rolling and daycount conventions. To achive a clear 1:1 mapping, the library fixes a conversion given by a) a reference date and b) a daycount convention (ACT/365).
- The conversion from LocalDate to Double is centralized in the class FloatingpointDate.
- The motivation for ACT/365 is performance (easy conversion). Apart from this there is no specific reason except the need to make some choice.

## 2.4.2 (05.02.2017)

### Bug fixes and improvements

- Invalidating numeraire caches if model is used with a different (new) process.
- Fixed a failure of date lookup in CapletVolatilityParametric (rare).

## 2.4.1

### Monte-Carlo Interest Rate Model: Term Structure Model with Time Dependent Tenor Discretization

- Added sample implementation of a term structure model with time dependent tenor discretization (see http://ssrn.com/abstract=2884699 ).
- Added calibration of term structure model

### Monte-Carlo Interest Rate Model: LIBOR Market Model

- Refactoring: The objects implementing AbstractLIBORCovarianceModelParametric have become immutable. The method setParameter has been removed and replaced by a method getCloneWithModifiedParameter. This allows a performance increase, since in a calibration it is now safe to reuse parts of the model, given that parts of a parameter (e.g. correlation) has not changed.
- Refactoring: The method getLIBOR(double, double, double) performing the interpoation on the tenor structure has been moved from LIBORModelMonteCarloSimualtionInterface to LIBORModelInterface. This allows to have different models implement different interpolation methods (e.g. LMM versus Hull-White). This chance is also motivated by the introduction of LIBOR models with time dependent tenor discretizations.

### Optimizer

- Levenberg-Marquardt algorithm will stop if improvement is smaller than given accurarcy. Previously the solver was running more interations than required.

## 2.4.0

### Market Data / Schedule Generation

- Added (optional) end-of-month schedule generation to the ScheduleGenerator.

Note: This includes a small change in the schedule generation when schedules are
generated as rolling up/down from a 29th, 30th or 31st.

## 2.3.6

### SABR Model

- Added analytic formulas for SABR skew and SABR curvature.

## 2.3.4

### Market Data / Curve Calibration

- Added valuation of Resettable Cross Currency Swap (aka Mark-to-Market Cross Currency Swap).

## 2.3.0

### Dependencies

- Adding jblas 1.2.4 for LinearAlgebra.

Since the implementation of commons-math matrix solver is sometimes noticable slower,
we switched to jblas. Note that jblas is used by default, but you may use
commons-math instead by setting the property net.finmath.functions.LinearAlgebra.isUseApacheCommonsMath
to true.

Note that jblas is currently not an OSGi bundle. I try to work on that.


## 2.2.6

- Cleanup

## 2.2.5

### Random Number Generators

- Addes Poisson distribution for jump-diffusion processes.
- Added class IndependentIncrements allowing to create mixed process increments (Brownian increments and jump process increments). See Merton model for a demo.

### Market Data / Interest Rates Curve / Multi-Curve

- Added Deposit (may be used as calibration product in multi-curve calibration)
- Added FRA (may be used as calibration product in multi-curve calibration)

### Equity / Single Asset Models

- Added Merton model (Monte-Carlo simulation)
- Added Heston model (Monte-Carlo simulation)
- Added hedge simulation based on mean-variance hedging (using American Monte-Carlo / regression).

### Other

- Some demo spreadsheets have been added at finmath-spreadsheets.

## 2.2.2

### General

- Added OSGi MANIFEST file

## 2.2.0

### Dependencies

- Replaced colt-1.2.0 by apache commons-math-3.6.1.

Since the implementation of MersenneTwister in commons-math differs from that in colt, this
change will lead to small changes in Monte-Carlo valuations.
Since the implementation of linear equation solver in commons-math differes from that in colt, this
change will lead to (very) small changes in calibration parameters.

The switch from colt-1.2.0 to commons-math was necessary to obtain OSGi compliant setup.

### Analytic Formulas

- Added analytic conversion from lognormal to normal (ATM) volatility.

## 2.1.1

### Monte-Carlo Simulation of Interest Rate Models

#### Hull-White Model

- Improved implementation and unit testing. Implementation now uses Browninan motion as a factory for RandomVariableInterface objects.

## 2.1.0

### Monte-Carlo Simulation of Interest Rate Models

#### Interfaces

- From the interface `LIBORMarketModelInterface` the narrower interface  `LIBORModelInterface` has been extracted,
leaving the methods related to the covariance model to `LIBORMarketModelInterface` only. The method `getModel()`
of `LIBORModelMonteCarloSimulationInterface` now returns a `LIBORModelInterface` interface only. In case your
code requires access to the covariance model, you have to check for `LIBORMarketModelInterface` (see the code
of `getValue` in `SwaptionAnalyticApproximation` for an example).

#### Hull-White Model

- Added implementation of a Monte-Carlo simulation of the Hull-White model.

#### LIBOR Market Model

- Added LIBOR Market Model (LMM) local volatiltiy model to generate Hull-White model dynamic in an LMM

## 2.0.3

- Some internals may be configured via Java system properties.
- A bug introduce in 2.0.0 in `DayCountConvention_30U_360` in the Java 6 branch has been fixed

## 2.0.2

- The CalibrationItem of CalibratedCurves may now carry a symbol to create a shifted model (for calculation of sensitivities using finite differences).

## 2.0.0

- API Change: The type java.util.Calendar has been replaced by LocalDate:
    - For Java 6 sources (src/main/java6): Replaced Calendar and Date by org.joda.time.LocalDate.
    - For Java 8 sources (src/main/java): Replaced Calendar and Date by java.time.LocalDate, contributed by William Wong.

- API Change: The DateIndex in net.finmath.montecarlo.interestrate.products.indices now returns the month according to java.time.Month, i.e., January = 1, February = 2, etc.

## 1.3.6

### Swaps

-	Additional constructor for Swap (using SwapLegs).

### Monte Carlo

-	BrownianMotion allows to use a custom AbstractRandomVariableFactory. Useful to switch to single precision floating point numbers (to save memory).
-	ProcessEulerScheme has an addition constructor (to directly construct a predictor corrector scheme).
-	Swaption is now compatible multi-curve LMM (using collateral curve).

### Exposure

-	Exposure has been renamed to ExposureEstimator (since it is just an estimator, see the corresponding unit test for an application).
-	Improved the unit test ExposureTest.

### Volatility

-	Added SABR analytic approximations to net.finmath.functions.AnalyticFormulas
-	BrownianMotion allows to use a custom AbstractRandomVariableFactory. Useful to switch to single precision floating point numbers (to save memory).

### Other

-	Constructor for trapezoidal rule using equi-distant grid.

## 1.3.5

-    Added trapezoidal rule to net.finmath.integration and corresponding unit test.


## 1.3.4 and earlier

Please check the subversion or git log messages.