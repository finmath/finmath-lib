About finmath lib
==========

****************************************

**Mathematical Finance Library: Algorithms and methodologies related to mathematical finance.**

****************************************

[![Twitter](https://img.shields.io/twitter/url/https/github.com/finmath/finmath-lib.svg?style=social)](https://twitter.com/intent/tweet?text=Wow:&url=https%3A%2F%2Fgithub.com%2Ffinmath%2Ffinmath-lib)
[![GitHub license](https://img.shields.io/github/license/finmath/finmath-lib.svg)](https://github.com/finmath/finmath-lib/blob/master/LICENSE.txt)
[![Latest release](https://img.shields.io/github/release/finmath/finmath-lib.svg)](https://github.com/finmath/finmath-lib/releases/latest)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/net.finmath/finmath-lib/badge.svg)](https://maven-badges.herokuapp.com/maven-central/net.finmath/finmath-lib)
[![Build Status](https://travis-ci.org/finmath/finmath-lib.svg?branch=master)](https://travis-ci.org/finmath/finmath-lib)
[![javadoc](https://javadoc.io/badge2/net.finmath/finmath-lib/javadoc.svg)](https://javadoc.io/doc/net.finmath/finmath-lib)

**Project home page: http://finmath.net/finmath-lib**

The finmath lib libraries provides (JVM) implementations of methodologies related to mathematical finance, but applicable to other fields. Examples are

- Analytic Formulas
    - Distributions: Normal, Gamma, Non-Central Chi-Squared (some functions are delegated to Apache commons-math).
    - Models: Black Scholes, Bachelier, SABR, ZABR, CEV, etc.
- General numerical algorithms like
    - Generation of random numbers
    - Optimization (a Levenberg–Marquardt algorithm is provided)
- Valuation using **Fourier transforms** / **characteristic functions** (contributed by Alessandro Gnoatto, Lorenzo Torricelli and others)
    - Black-Scholes model
    - Heston model
    - Bates model
    - Two factor Bates model
    - Merton model
    - Variance Gamma model (contributed and maintained by Alessandro Gnoatto)
- **Finite difference methods** (contributed by Ralph Rudd and Jörg Kienitz)
    - Numerical schemes using finite differences
        - Theta-scheme
    - Models
        - Black-Scholes model
        - Constant Elasticity of Variance model
    - Products
        - European option
- **Monte-Carlo simulation** of multi-dimensional, multi-factor stochastic differential equations (SDEs)
    - Hull-White Short Rate Model (with time dependent parameters)
    - LIBOR Market Model (Forward Rate Model) (in various forms)
    - Time Homogeneous Forward Rate Model
    - Cross-Currency LIBOR Market Model
    - Black-Scholes type multi-asset model (multi-factor, multi-dimensional geometric Brownian motion)
    - Equity Hybrid LIBOR Market Model
    - Merton Model (as Monte-Carlo Simulation)
    - Heston Model (as Monte-Carlo Simulation)
    - Variance Gamma model (as Monte-Carlo Simulation, contributed and maintained by Alessandro Gnoatto)
- **American Monte-Carlo**: Estimation of conditional expectations in a Monte-Carlo framework
- **Stochastic Automatic Differentiation** (AAD) (part of the package `net.finmath.montecarlo.automaticdifferentiation`)
- **Monte-Carlo Simulation on GPGPUs** (via Cuda) (requires finmath-lib-cuda-extensions https://github.com/finmath/finmath-lib-cuda-extensions )
- Dependency injection on numerical algorithms (Monte-Carlo simulations) with custom return type priorities (see http://ssrn.com/abstract=3246127 ).
- **Dividend model for equity option valuation** (European and American) (contributed by Andreas Grotz)
- **Analytic valuation via curves and surfaces** 
    - Multi-Curve valuation of interest rate products (collateralization and funding) (Swaps, FRA).
    - Bonds valuation using bond curves.
    - CDS valuation.
- **Calibration** of market data objects like curves (discount and forward curve) or volatility surfaces
    - Rate Curves:
        - **Multi-curve interest rate curve calibration** (OIS discounting, basis-swaps, cross-currency-swaps).
        - **Bond curve calibration** using local linear regression (see https://ssrn.com/abstract=3073942 ).
        - Various interpolation methods (linear, cubic spline, harmonic spline, Akima).
        - Various interpolation entities (value, log-value, rate, etc.).
        - Parametric curves like Nelson-Siegel and Nelson-Siegel-Svensson.
    - Volatility Curves and Cubes:
        - **SABR smile** parameterization.
        - Swaption volatility cubes with SABR parameterization.
        - **CMS replication** with various annuity mappings.
- Simulation of **interest rate term structure models** (LIBOR market model with local and stochastic volatility)
    - LIBOR market model with local and stochastic volatility
    - Time-Homogeneous Forward Rate Model (LIBOR market model)
    - Calibration of the **LIBOR market model**
    - Cross-Currency LIBOR Market Model
    - Equity Hybrid LIBOR Market Model
    - Local and stochastic volatility models (SABR, ZABR)
- Valuation of complex derivatives
    - Bermudan options / multi-callables lower bound via regression estimation of the conditional expectation
    - Bermudan options / multi-callables upper bound via dual method
- **Hedge Simulation**
- **Margin Valuation Adjustments** (MVA) though forward ISDA SIMM simulation (this is currently a separate project at https://github.com/finmath ).

Languages and Build
--------------------------------------

The library is available for Java 11 and Java 8. We are starting to provide examples in Kotlin.

The Maven build file is provide. Import the project as Maven project.

The default Maven profile is Java 11 without Kotlin. To enable Java 8 version select the Maven profile 'java-8'. To enable Kotlin select the Maven profile 'kotlin'.


Releases
--------------------------------------

Binary releases can be found at http://finmath.net/finmath-lib .
The version numbering of finmath-lib follows a the *[semantic versioning](https://semver.org)* 
(at least we try to).


Distribution
--------------------------------------

finmath lib is distributed through the central maven repository. It's coordinates are:

For the Java 11 version:

    <groupId>net.finmath</groupId>
    <artifactId>finmath-lib</artifactId>
    <version>${project.version}</version>
    
For the Java 8 version:

	<groupId>net.finmath</groupId>
	<artifactId>finmath-lib</artifactId>
	<version>3.6.3</version>
	<classifier>java8</classifier>

*Note: finmath-lib Version 4 and 5 is currently not available for Java 6. For Java 6 you may try version 3.6.3 with classifier java6.*


You may build the Java 11 version via Maven using

	mvn -P java-11

and the Java 8 version using

	mvn -P java-8
	
Source code
-------------------------------------

The finmath lib Java library comes in two flavors which have a slightly different code base: a Java 11 version and a Java 8 version.
We will use Java 11 concepts in the future and try to provide Java 8 compatibility where possible.

For that reason, the source code is duplicated:
-    src/main/java				contains the Java 11 compatible source files
-    src/main/java8				contains the Java 8 compatible source files

Although the two folder share some/many identical source files, we prefer this two folder layout
over one with a third folder like java-common.


### Building finmath lib

-    To build finmath lib for Java 11 use src/main/java
-    To build finmath lib for Java 8 use src/main/java8

These builds may be performed via Maven the profiles "java-11" and "java-8".
The eclipse project file is pre-configured to Java 11.

#### Maven build

The maven pom defaults to the Java 11 build. To build finmath lib for Java 8 use the maven profile "java-8".


Repositories
-------------------------------------

Source code and demos are provided via Github repository.
			<ul>
				<li>
					<i>Git</i> repositories with Java code:
					<ul>
						<li>
							finmath lib: [https://github.com/finmath/finmath-lib](https://github.com/finmath/finmath-lib)
						</li>
						<li>
							finmath experiments: [https://github.com/finmath/finmath-experiments](https://github.com/finmath/finmath-experiments)
						</li>
						<li>
							finmath spreadsheets: [https://github.com/finmath/finmath-spreadsheets](https://github.com/finmath/finmath-spreadsheets)
						</li>
					</ul>
				</li>
			</ul>

Although not recommended, the repository contains an Eclipse project and classpath file including all dependencies.

Documentation
-------

For documentation please check out

-   [finmath lib Project documentation][]  
    provides the documentation of the library api.
-   [finmath lib API documentation][]  
    provides the documentation of the library api.
-   [finmath.net special topics][]  
    cover some selected topics with demo spreadsheets and uml diagrams.
    Some topics come with additional documentations (technical papers).


License
-------

The code of "finmath lib" and "finmath experiments" (packages
`net.finmath.*`) are distributed under the [Apache License version
2.0][], unless otherwise explicitly stated.
 

  [finmath lib Project documentation]: http://finmath.net/finmath-lib/ 
  [finmath lib API documentation]: http://finmath.net/finmath-lib/apidocs/
  [finmath.net special topics]: http://www.finmath.net/topics
  [Apache License version 2.0]: http://www.apache.org/licenses/LICENSE-2.0.html


Extensions
-------------------------------------

The [finmath-lib-cuda-extensions](http://finmath.net/finmath-lib-cuda-extensions/) implement the interface `RandomVariable` via Cuda GPU code. This allows to perform Monte-Carlo simulations on the GPUs with a minimal change: a replacement of the random variable factory.

The finmath-lib-automaticdifferentiation-extensions implement the RandomVariableInterface via an AAD enabled version. This allows to access automatic differentiations with a minimal change: a replacement of the random variable factory.
Starting with version 3.3.1 the finmath-lib-automaticdifferentiation-extensions is part of finmath-lib.


Coding Conventions
-------------------------------------

We follow losely the Eclipse coding conventions, which are a minimal modification of the original Java coding conventions. See https://wiki.eclipse.org/Coding_Conventions

We deviate in some places:

-   We allow for long code lines. Some coding conventions limit the length of a line to something like 80 characters (like FORTRAN did in the 70'ies). Given widescreen monitors we believe that line wrapping makes code much harder to read than code with long(er) lines.

-	We usually do not make a space after statements like `íf`, `for`. We interpret `íf` and `for` as functions and for functions and methods we do not have a space between the name and the argument list either. That is, we write

    if(condition) {
      // code
    }

