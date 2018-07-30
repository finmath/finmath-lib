# Maven Build

## Source Repository

Source code is available from the [source repository](source-repository.html).

## Building finmath lib

### Maven build

The maven pom defaults to the Java 8 build. To build finmath lib for Java 6 use the maven profile "java-6".

#### Java 8

To build finmath lib via maven (on the command line) run

    mvn clean package

This will use the code base in ``src/main/java``

#### Java 6

To build finmath lib for Java 6 via maven (on the command line) run

    mvn clean package -P java-6

This will use the code base in ``src/main/java-6`

These builds may be performed via Maven the profiles "java-8" and "java6".

