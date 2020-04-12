# Coding Conventions


Below you find some high level design principles. With respect to the low-level code style, see the checkstyle file.


## Design Principles


### Implementation against Interfaces

The library is developed to allow users to focus on its interfaces. Code should implement against an interface (e.g., the left hand side of an assignment should be an interface).


### Naming, Code is Documentation

Class names, member names and variable names are as descriptive as possible (feasible).
Names are formed using camel notation from the general to the special property. Implementation
details are appended. Examples:

  - `periodStart`
  - `periodEnd`
  - `RandomVariableFromDoubleArray`
  - `DiscountCurveInterpolation`

A reassignment of a reference that alters the meaning should be avoided. A temporary assignment with a different meaning should be avoided. Example:

Wrong:

```
	var discountFactor = (1 + forwardRate * periodLength);
	discountFactor = 1.0/discountFactor;
```

Correct:

```
	var forwardBond = (1 + forwardRate * periodLength);
	var discountFactor = 1.0/forwardBond;
```


#### Naming of Interfaces

Interfaces should come with names describing the core concept (like `BrownianMotion`  or `RandomVariable` or `Curve`). Implementations pick up the name of the main interface followed by a specification of an implementation aspect (like `RandomVariableFromDoubleArray`).


#### Naming of Collections

Collections are named as the plural of their items. Example:

```
	Period period = periods.get(periodIndex);
```

In cases where it adds clarity the collection type can be used as a suffix instead of the plural. Example:

```
	RandomVariable sensitivity = sensitivityMap.get(riskFactor);
```

#### Naming of Arguments

If constructor arguments or setter (builder) arguments are used to set fields, the field and the argument have the same name, i.e., we use

```
	private final double value;
	
	Skalar(double value) {
		this.value = value;
	}
```


### Naming of Unit Tests

Unit test names are end with the word `Test` and formed as XxxYyyTest where Xxx is the name of the primary class being under test (it may be that a tests is testing a broader aspect of the class) and Yyy optionally specifies an aspect. Example: `LIBORMarketModelCalibrationTest`. The methods of the unit test start with the word `test`.



### Thread Safety

The library is developed with thread safety in mind.

-   Classes prefer a design resulting in *effectively immutable object*.
    -   Method calls are idempotent.
    -   Method calls are free of side effects.
-   Members are final whenever possible.



### Lazy Initialization

The library utilized lazy initialization for time consuming task. Object construction is expected to require minimal resources.



### Serialization

The library is developed with object serialization in mind.

-   Classes should implement the `Serializable` interface.


## Language

### Language Extensions
 
The code is written in Java. That is, it avoids language extensions like *Lombok*.


### Java Modules

Starting with version 5.x the library will export Java 9 modules.


## Language

### Language Extensions
 
The code is written in Java. That is, it avoids language extensions like *Lombok*.


### Java Modules

Starting with version 5.x the library will export Java 9 modules.


## Other Principles

- Code in `src/main` should conform to DRY (don't repeat yourself). Unit tests in `src/test` should be self-contained and code duplication (e.g. create a model for a specific purpose) is fine.


## Documentation

### JavaDoc with MathJax

Every public interface, class, method, enum comes with a complete JavaDoc. If there are mathematical formulas, we use MathJax. (MathJax is available due to a configuration in the Maven pom.xml).


## Code Style

We follow loosely the Eclipse coding conventions, which are a minimal modification of the original Java coding conventions. See https://wiki.eclipse.org/Coding_Conventions

See also the Spring coding conventions, see https://github.com/spring-projects/spring-framework/wiki/Code-Style

We repeat some of the aspects:

-   Indentation uses a single TAB character (as for the Spring coding convention and the Eclipse coding convention).

-   Files are UTF-8 encoded with Unit line endings.

-	Lines do not have trailing whiltespaces.



We deviate in some places:

-   We allow for long code lines. Some coding conventions limit the length of a line to something like 80 characters (like FORTRAN did in the 70'ies). Given widescreen monitors we believe that line wrapping makes code much harder to read than code with long(er) lines.

-   We usually do not make a space after statements like `íf`, `for`. We interpret `íf` and `for` as functions and for functions and methods we do not have a space between the name and the argument list either. That is, we write

```
    if(condition) {
      // code
    }
```

The project is using a checkstyle profile, which currently only a very limited set 
of style issues. See [finmath-checkstyle.xml](https://github.com/finmath/finmath-lib/blob/master/finmath-checkstyle.xml).

