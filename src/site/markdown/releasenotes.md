finmath lib Release Notes
==========

****************************************

# Release Notes

## 3.2.8 (30.06.2018)

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