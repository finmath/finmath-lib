# Maven Build

## Source Repository

Source code is available from the [source repository](source-repository.html).

## Building finmath lib

### Maven build

On JDK 11 or newer, Maven activates the `java-11` profile by default. The `java-8` profile builds the Java 8 classifier from the backported source tree.

#### Java 11

To build finmath lib with a JDK 11 or newer, run

    mvn clean package

This uses the sources in `src/main/java` and the tests in `src/test/java`.

#### Java 8

To build the Java 8 classifier with the same JDK, select `java-8` and explicitly disable `java-11`:

    mvn clean package -Pjava-8,-java-11

This uses the sources in `src/main/java8` and the tests in `src/test/java8`, and produces the `-java8.jar` artifact.

