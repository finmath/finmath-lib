About finmath lib
==========


****************************************

**Mathematical Finance Library: Algorithms and methodologies related to mathematical finance.**

****************************************
	
The finmath lib libraries provides implementations of methodologies related to mathematical finance, but applicable to other fields. Examples are

- General numerical algorithms like
- - Generation of random numbers
- - Optimization (a Levenberg-Marquard algorithm is provided)
- Monte-Carlo simulation of multi-dimensional, multi-factor stochastic differential equations (SDEs)
- Estimation of conditional expectations in a Monte-Carlo framework
- Calibration of market data object like curve (discount and forward curve) or volatility surfaces
- Simulation of interest rate term structure models
- Valuation of complex derivatives (e.g. Bermudan/multi-callables)

The libraries have a focus on Monte-Carlo methods, interest rate products and models and hybrid models.

**finmath lib is now on Java 8 (since February 2nd, 2014), but a Java 6 version is provided too.**

*Note: for convenience the provided Eclipse project is configured for Java 6. The maven pom defaults to Java 6. To build the Java 8 version use the profile "java-8", i.e. the maven command line option "-P java-8"*


Distribution
--------------------------------------

Starting with version 1.2.19 finmath lib is distributed through the central maven repository. It's coordinates are:

	<groupId>net.finmath</groupId>
	<artifactId>finmath-lib</artifactId>
	<version>1.2.27</version>
	


License
-------

The code of "finmath lib" and "finmath experiments" (packages
`net.finmath.*`) are distributed under the [Apache License version
2.0][], unless otherwise explicitly stated.
 

  [finmath lib Project documentation]: http://www.finmath.net/java/finmath-lib 
  [finmath lib API documentation]: http://www.finmath.net/java/finmath-lib/doc/
  [finmath.net special topics]: http://www.finmath.net/topics
  [Apache License version 2.0]: http://www.apache.org/licenses/LICENSE-2.0.html
