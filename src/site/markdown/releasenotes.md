finmath lib Release Notes
==========

****************************************

# Release Notes

## 2.0.4-SNAPSHOT

- Added implementation of a Monte-Carlo simulation of the Hull-White model.
- Added LIBOR Market Model (LMM) local volatiltiy model to generate Hull-White model dynamic in an LMM

## 2.0.3

- Some internals may be configured via Java system properties.
- A bug introduce in 2.0.0 in DayCountConvention_30U_360 in the Java 6 branch has been fixed

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