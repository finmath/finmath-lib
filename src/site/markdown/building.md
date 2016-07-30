# Building finmath lib from Source Files


## Build

- The build process of finmath lib is based on a [maven bulid](maven.html).

- For convenience we provide a pre-configures [Eclipse project file](eclipseproject.html).

## Java 6 and Java 8 source files

The finmath lib Java library comes in two flavors which have a slightly different code base: a Java 8 version and a Java 6 version. We will use Java 8 concepts in the future and try to provide Java 6 compatibility where possible.

For that reason, the source code is duplicated:
-    src/main/java				contains the Java 8 compatible source files
-    src/main/java6				contains the Java 6 compatible source files

Although the two folder share some/many identical source files, we prefer this two folder layout over one with a third folder like java-common.

## Repositories

Source code and demos are provided via Github repositories:
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
			</ul>



