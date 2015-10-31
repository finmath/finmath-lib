finmath lib Release Notes
==========

****************************************

# Release Notes



## 1.3.6-SNAPSHOT

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

### Other

-	Constructor for trapezoidal rule using equi-distant grid.

## 1.3.5

-    Added trapezoidal rule to net.finmath.integration and corresponding unit test.


## 1.3.4 and earlier

Please check the subversion or git log messages.