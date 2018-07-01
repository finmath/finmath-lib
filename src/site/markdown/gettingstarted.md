# Getting Started

Below you find some instructions if you just like to "play" around with finmath-lib

### Getting started if you are new to Java

If you are new to software development the first thing you need is an IDE (integrated development environment).
You may have a look at [Eclipse](http://www.eclipse.org/downloads/), [IntelliJ IDEA](https://www.jetbrains.com/idea/) and [NetBeans](https://netbeans.org).

### Checking out the finmath lib source code from its repository.

The finmath lib source code is available from GitHub.
Visit [finmath-lib on GitHub](https://github.com/finmath/finmath-lib).
You may directly import the code into your IDE from Git.
	
####  Importing the finmath lib source code into Eclipse

To import the source code into your Eclipse workspace:

The recommended way is to import the project as a maven project. To do so:

* File -> Import… -> Maven -> Checkout Projects from SCM -> SCM: URL
* Enter the URI https://github.com/finmath/finmath-lib.git
* "Next" and "Finish".

Alternatively you can import the project as "Project from Git" (this works, because 
an Eclipse .project file is part of the repository):

* File -> Import… -> Git -> Projects from Git -> Clone URI
* Enter the URI https://github.com/finmath/finmath-lib.git
* Select "Next".
* Select "master".
* "Next" and "Finish".

####  Importing the finmath lib source code into IntelliJ IDEA

To import the source code into IntelliJ IDEA:

* File -> New -> Project from Version Control
* Enter the URL https://github.com/finmath/finmath-lib.git
* Select "Clone".
* After the project has been imported: Select "Add as Maven Project", e.g. by
  * context menu (right click) on pom.xml or
  * select "+" in the Maven projects tab.

### Inspecting the source code / running some tests.

The main library code is available in the folder `src/main/java`.

There are some unit test available under the folder `src/test/java`. Inspecting theses 
tests may serve as a good entry point.

#### Running the unit test in Eclipse:

In Eclipse right click on any class file in src/test/java and select

* Run As -> JUnit Test

to run the test.

#### Running the unit test in IntelliJ:

In IntelliJ right click on the project and select

* Run "All Tests"

### Creating your own project

#### Creating your own project using finmath-lib in Ecipse

To create your own projects which uses finmath-lib:

In Eclipse

- File -> New -> Java Project
- Enter a name for the project
- Right click on the new project and select "Properties".
- Select "Java Build Path -> Projects" and add the (imported) finmath lib project.

By this, your project "knows" finmath lib and you may use all the classes from finmath lib.
