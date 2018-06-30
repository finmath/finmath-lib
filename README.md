About finmath lib
==========


****************************************

**Mathematical Finance Library: Algorithms and methodologies related to mathematical finance.**

****************************************

**Project home page: http://finmath.net/finmath-lib**

The finmath lib libraries provides (JVM) implementations of methodologies related to mathematical finance, but applicable to other fields. Examples are

- General numerical algorithms like
    - Generation of random numbers
    - Optimization (a Levenberg–Marquardt algorithm is provided)
- Valuation using Fourtier transforms / characteristic functions
	- Black-Scholes model
	- Heston model
	- Bates model
	- Two factor Bates model
- Monte-Carlo simulation of multi-dimensional, multi-factor stochastic differential equations (SDEs)
    - LIBOR Market Model
    - Black Scholes type multi-asset model (multi-factor, multi-dimensional geometric Brownian motion)
    - Equity Hybrid LIBOR Market Model
    - Hull-White Short Rate Model (with time dependent parameters)
    - Merton Model (as Monte-Carlo Simulation)
    - Heston Model (as Monte-Carlo Simulation)
- Estimation of conditional expectations in a Monte-Carlo framework
- Stochastic Automatic Differentiation (AAD) (requires finmath-lib-automaticdifferentiation-extensions https://github.com/finmath/finmath-lib-automaticdifferentiation-extensions )
- Monte-Carlo Simulation on GPGPUs (via Cuda) (requires finmath-lib-cuda-extensions https://github.com/finmath/finmath-lib-cuda-extensions )
- Calibration of market data objects like curves (discount and forward curve) or volatility surfaces
	- Multi-curve interest rate curve calibration (OIS discounting, basis-swaps, cross-currency-swaps).
	- Bond curve calibration using local linear regression (see https://ssrn.com/abstract=3073942 ).
    - Various interpolation methods (linear, cubic spline, harmonic spline, Akima).
    - Various interpolation entities (value, log-value, rate, etc.).
    - Parametric curves like Nelson-Siegel and Nelson-Siegel-Svensson.
- Simulation of interest rate term structure models (LIBOR market model with local and stochastic volatility)
- Calibration of the LIBOR market model
- Valuation of complex derivatives
    - Bermudan options / multi-callables lower bound via regression estimation of the conditional expectation
    - Bermudan options / multi-callables upper bound via dual method
- Hedge Simulation
- Margin Valuation Adjustments (MVA) though forward ISDA SIMM simulation (this is currently a separate project at https://github.com/finmath ).

The libraries have a focus on Monte-Carlo methods, interest rate products and models and hybrid models.

Languages and Build
--------------------------------------

The library is available for Java 8 and Java 6. We are starting to provide examples in Kotlin.

The Maven build file is provide. Import the project as Maven project.

The default Maven profile is Java 8 without Kotlin. To enable Java 6 version select the Maven profile 'java-6'. To enable Kotlin select the Maven profile 'kotlin'.

Distribution
--------------------------------------

finmath lib is distributed through the central maven repository. It's coordinates are:

For the Java 6 version:

	<groupId>net.finmath</groupId>
	<artifactId>finmath-lib</artifactId>
	<version>${project.version}</version>
	<classifier>java6</classifier>

For the Java 8 version:

	<groupId>net.finmath</groupId>
	<artifactId>finmath-lib</artifactId>
	<version>${project.version}</version>
	
You may build the Java 8 version via Maven using

	mvn -P java-8

and the Java 6 version using

	mvn -P java-6
	
Source code
-------------------------------------

The finmath lib Java library comes in two flavors which have a slightly different code base: a Java 8/9 version and a Java 6 version.
We will use Java 8 and Java 9 concepts in the future and try to provide Java 6 compatibility where possible.

For that reason, the source code is duplicated:
-    src/main/java				contains the Java 8 compatible source files
-    src/main/java6				contains the Java 6 compatible source files

Although the two folder share some/many identical source files, we prefer this two folder layout
over one with a third folder like java-common.


### Building finmath lib

-    To build finmath lib for Java 8 use src/main/java
-    To build finmath lib for java 6 use src/main/java-6

These builds may be performed via Maven the profiles "java-8" and "java-6".
The eclipse project file is pre-configured to Java 6.

#### Maven build

The maven pom defaults to the Java 8 build. To build finmath lib for Java 6 use the maven profile "java-6".



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

Although not recommended, the repository contains an Eclipse project and classpath file including all dependencies. We provide this for convenience. We provide <a href="/java/subversion">instructions</a> on how to checkout the code using the Eclipse IDE.
Of course, you may use the IDE of your choice by simply importing the maven pom.


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

The finmath-lib-cuda-extensions implement the RandomVariableInterface via Cuda GPU code. This allows to perform Monte-Carlo simulations on the GPUs with a minimal change: a replacement of the random variable factory.

The finmath-lib-automaticdifferentiation-extensions implement the RandomVariableInterface via an AAD enabled version. This allows to access automatic differentiations with a minimal change: a replacement of the random variable factory.


Coding Conventions
-------------------------------------

We follow losely the Eclipse coding conventions, which are a minmal modification of the original Java coding conventions. See https://wiki.eclipse.org/Coding_Conventions

We deviate in some places:

-   We allow for long code lines. Some coding conventions limit the length of a line to something like 80 characters (like FORTRAN did in the 70'ies). Given widescreen monitors we believe that line wrapping makes code much harder to read than code with long(er) lines.

-	We usually do not make a space after statements like `íf`, `for`. We interpred `íf` and `for` as functions and for functions and methods we do not have a space between the name and the argument list either. That is, we write

    if(condition) {
      // code
    }
