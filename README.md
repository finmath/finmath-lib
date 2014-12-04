Changes by KIC

added a generic reusable LevenbergMarquardt optimizer -> LevenbergMarquardtGeneric

finmath lib
===========

**Mathematical Finance Library: Algorithms and methodologies related to mathematical finance.**

*********************************************************

About finmath lib
--------

The finmath lib libraries provides implementations of methodologies related to mathematical finance, but applicable to other fields, e.g., the Monte-Carlo simulation of SDEs and the estimation of conditional expectations in Monte-Carlo.

The libraries have a focus on Monte-Carlo methods, interest rate products and models and hybrid models.

**finmath lib is now on Java 8 (since February 2nd, 2014), but a Java 6 version is provided too.**

*Note: for convenience the provided Eclipse project is configured for Java 6. The maven pom defaults to Java 6. To build the Java 8 version use the profile "java-8", i.e. the maven command line option "-P java-8"*


Distribution
--------------------------------------

Starting with version 1.2.19 finmath lib is distributed through the central maven repository. It's coordinates are:

	<groupId>net.finmath</groupId>
	<artifactId>finmath-lib</artifactId>
	<version>1.2.20</version>
	


Source code
-------------------------------------

The finmath lib Java library comes in two flavors which have a slightly different code base: a Java 8 version and a Java 6 version.
We will use Java 8 concepts in the future and try to provide Java 6 compatibility where possible.

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

The maven pom defaults to the Java 6 build. To build finmath lib for Java 8 use the maven profile "java-8".



Repositories
-------------------------------------

Source code and demos are provided via Github and a subversion repository.
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
					</ul>
				</li>
				<li>
					<i>Subversion</i> repositories with Java code:
					<ul>
						<li>
							finmath lib: [http://svn.finmath.net/finmath lib](http://svn.finmath.net/finmath%20lib)
						</li>
						<li>
							finmath experiments: [http://svn.finmath.net/finmath experiments](http://svn.finmath.net/finmath%20experiments)
						</li>
					</ul>
				</li>
			</ul>

Although not recommeded, the repository contains an Eclipse procject and classpath file including all dependencies. We provide this for convenience. We provide <a href="/java/subversion">instructions</a> on how to checkout the code using the Eclipse IDE.
Of course, you may use the IDE of your choice by simply importing the maven pom.


Documentation
-------------

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
 

  [finmath lib Project documentation]: http://www.finmath.net/java/finmath-lib 
  [finmath lib API documentation]: http://www.finmath.net/java/finmath-lib/doc/
  [finmath.net special topics]: http://www.finmath.net/topics
  [Apache License version 2.0]: http://www.apache.org/licenses/LICENSE-2.0.html
