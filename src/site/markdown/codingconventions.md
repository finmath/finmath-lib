# Coding Conventions


## Design Principles

### Implementation against interfaces

The library is developed to allow users to focus on its interfaces. Interfaces should come with names describing the core concept (like `BrownianMotion`  or `RandomVariable` or `Curve`). Implementation pick up the name of the main interface followed by a specifictation of an implementation aspect (like `RandomVariableFromDoubleArray`) 


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



## Code Style

We follow loosely the Eclipse coding conventions, which are a minimal modification of the original Java coding conventions. See https://wiki.eclipse.org/Coding_Conventions

We deviate in some places:

-   We allow for long code lines. Some coding conventions limit the length of a line to something like 80 characters (like FORTRAN did in the 70'ies). Given widescreen monitors we believe that line wrapping makes code much harder to read than code with long(er) lines.

-   We usually do not make a space after statements like `íf`, `for`. We interpret `íf` and `for` as functions and for functions and methods we do not have a space between the name and the argument list either. That is, we write

    if(condition) {
      // code
    }

The project is using a checkstyle profile, which currently only a very limited set 
of style issues. See [finmath-checkstyle.xml](https://github.com/finmath/finmath-lib/blob/master/finmath-checkstyle.xml).

