# This script performs a release of finmath-lib. The release is pushed to Maven central.
#
#
# While a replease can be performed with just the two lines
#
#   mvn release:prepare
#   mvn release:perform
#
# the script performs some additional task in order to:
# - deploy other profiles with Maven classifiers (java8)
# - run the clover report, which can be run for java-8 only.
#
#
# Before running this script you should check the profiles via
# 	mvn clean javadoc:javadoc checkstyle:check test -P java-8.
# and
# 	mvn javadoc:javadoc checkstyle:check test
#
# To create a new release you need to run
# 	mvn release:prepare
# before running this script and specify the version and check if this is successful.
#
# To re-release a given tag / version you need to run
# 	git checkout finmath-lib-<verion>
# before running this script.
#
# If this script is run on a newly setup system, the following things need to be setup:
# - .m2/settings.xml - has to contain the passwords for sonatype, github and finmath.net site
# - .keyring has to contain the gpg keys
# - a gpg app has be installed (referenced in the .m2/settings.mxl)
# - you have to run mvn site:deploy manually once to get prompted for adding the RSA fingerprint
#
#
# The code coverage report from clover currently only works for the Java 8 branch.
# Hence there is an additional run of mvn clover:instrument with -P java-8
# To prevent the deletion of the report(s), this run must not use mvn clean.
# Since we switch the SDK, we have to do a recompilation via
# -Dmaven.compiler.useIncrementalCompilation=false
#
#

set -o verbose

#
# Run mvn release:prepare manually to check if the none of the tests fail
#

mvn release:prepare

#

# relase the default profile
echo ###################
echo # RELEASE:PERFORM #
echo ###################

mvn release:perform

# deploy the other profiles (we do this skipping tests) (the clean is important here!)
cd target/checkout/
mvn clean verify javadoc:jar source:jar gpg:sign deploy:deploy -P java-8 -D skipTests

# re-deploy the java-11 profile to deploy the right source and javadoc files to Maven central
mvn clean verify javadoc:jar source:jar gpg:sign deploy:deploy -P java-11 -D skipTests

# deploy site (clover:instrument takes a long time)
mvn compile clover:instrument -P java-8 -Dmaven.compiler.useIncrementalCompilation=false
mvn install site site:stage site-deploy -Dmaven.compiler.useIncrementalCompilation=false
