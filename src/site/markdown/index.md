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

Source code is available from the [source repository](source-repository.html).

### Java 6 and Java 8 source files

The finmath lib Java library comes in two flavors which have a slightly different code base: a Java 8 version and a Java 6 version. We will use Java 8 concepts in the future and try to provide Java 6 compatibility where possible.

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



License
-------

The code of "finmath lib" and "finmath experiments" (packages
`net.finmath.*`) are distributed under the [Apache License version
2.0][], unless otherwise explicitly stated.
 

  [finmath lib Project documentation]: http://www.finmath.net/java/finmath-lib 
  [finmath lib API documentation]: http://www.finmath.net/java/finmath-lib/doc/
  [finmath.net special topics]: http://www.finmath.net/topics
  [Apache License version 2.0]: http://www.apache.org/licenses/LICENSE-2.0.html
