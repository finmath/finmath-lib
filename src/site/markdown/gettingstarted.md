# Getting Started

Below you find some instructions if you just like to "play" around with finmath-lib

## Getting started if you are new to Java

If you are new to software development the first thing you need is an IDE (integrated development environment). You may have a look at [Eclipse](http://www.eclipse.org/downloads/), [IntelliJ IDEA](https://www.jetbrains.com/idea/) and [NetBeans](https://netbeans.org).

## Checking out the finmath lib source code form its repository.

The finmath lib source code is available from GitHub. Visit [finmath-lib on GitHub](https://github.com/finmath/finmath-lib).

###  Importing the finmath lib source code into Eclipse

To import the source code into you Eclipse workspace:
* File -> Import… -> Git -> Projects from Git -> Clone URI
* Enter the URI https://github.com/finmath/finmath-lib.git
* Select "Next".
* Select "master".
* "Next" and "Finish".

## Inspecting the source code / running some tests.

The main library code is available in the folder src/main/java.

There some unit test available under the folder src/test/java. In Eclipse right click on any class file in src/test/java and select
* Run As -> JUnit Test
to run the test.

## Creating your onw porject

To create your own projects which uses finmath-lib:

In Eclipse
- File -> New -> Java Project
- Enter a name for the project
- Right click on the new project and select "Properties".
- Select "Java Build Path -> Projects" and add the (imported) finmath lib project.

By this, your project "knows" finmath lib and you may use all the classes from finmath lib.
