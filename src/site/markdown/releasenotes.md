finmath lib Release Notes
==========

****************************************

# Release Notes

## 2.3.4

### Added valuation of Resettable Cross Currency Swap (aka Mark-to-Market Cross Currency Swap).

## 2.3.0

### Dependencies

- Adding jblas 1.2.4 for LinearAlgebra.

Since the implementation of commons-math matrix solver is sometimes noticable slower,
we switched to jblas. Note that jblas is used by default, but you may use
commons-math instead by setting the property net.finmath.functions.LinearAlgebra.isUseApacheCommonsMath
to true.

Note that jblas is currently not an OSGi bundle. I try to work on that.


## 2.2.6

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