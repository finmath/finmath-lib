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

### Test coverage

The optional `coverage` profile records test coverage with JaCoCo and adds a coverage report to the Maven site's **Project Reports** menu:

    mvn clean test site -Pcoverage

Open `target/site/index.html` and follow **Project Reports → JaCoCo**, or open `target/site/jacoco/index.html` directly. The report includes package, class, method, line and branch coverage, with highlighted source code.

To generate only the coverage report, without the rest of the site:

    mvn clean test jacoco:report -Pcoverage

For Java 8 sources, use `-Pcoverage,java-8,-java-11`. Use a clean build when switching source profiles or collecting fresh coverage. JaCoCo combines results from the separate test JVMs, so repeated runs without cleaning accumulate coverage data.

Run the complete test suite for a release report. A selection such as `-Dtest=CSVSwaptionParserTest` is useful for local checks, but its report only reflects those selected tests. Skipping tests does not collect coverage.

The site-publishing script `maven-release-site.sh` enables coverage and runs the tests before publishing. Normal builds without `-Pcoverage` do not collect coverage.
