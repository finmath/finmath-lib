#!/usr/bin/env bash

# This script performs a release of finmath-lib and publishes one Maven Central
# deployment containing the Java 11 main artifact, Java 11 sources/javadocs,
# and the Java 8 classifier artifact.
#
# The important change compared to the old OSSRH workflow is that we no longer
# run deploy multiple times. The Central Portal deploy plugin creates one
# deployment/bundle per deploy run, so all artifacts must be attached before the
# single final deploy.
#
# Before running this script you should check the profiles via
# 	mvn clean javadoc:javadoc checkstyle:check test -P=java-8,-java-11
# and
# 	mvn clean javadoc:javadoc checkstyle:check test -P=java-11,-java-8
#
# If this script is run on a newly setup system, the following things need to be setup:
# - .m2/settings.xml has to contain the passwords/tokens for central, github and the site
# - .keyring has to contain the gpg keys
# - a gpg app has to be installed and configured
# - you may have to run mvn site:deploy manually once to get prompted for adding the RSA fingerprint
#
# This script assumes that the final deploy is run with a Java 11+ JDK. The
# Java 8 classifier artifact is first built separately, copied outside target,
# and then attached to the final Java 11 deploy.

set -euo pipefail
set -o verbose

# Run mvn release:prepare explicitly so that you can check that release
# preparation is successful before the final Central deployment.
mvn release:prepare

# Let release:perform check out the release tag into target/checkout and verify
# it, but do not deploy from release:perform. The final deploy below attaches all
# artifacts first and then publishes once.
echo "###################"
echo "# RELEASE:PERFORM #"
echo "###################"

mvn release:perform \
	-Dgoals=verify \
	-Darguments="-DskipTests -Dgpg.skip=true -Dmaven.javadoc.skip=true -Dmaven.source.skip=true"

cd target/checkout/

VERSION="$(mvn -q -DforceStdout help:evaluate -Dexpression=project.version)"
ATTACH_DIR="$(pwd)/../release-artifacts"
JAVA8_ARTIFACT="${ATTACH_DIR}/finmath-lib-${VERSION}-java8.jar"

rm -rf "${ATTACH_DIR}"
mkdir -p "${ATTACH_DIR}"

# Build the Java 8 classifier jar, but do not deploy it. Sources and javadocs
# are intentionally skipped here because they should come from the Java 11 build.
mvn clean package \
	-P=java-8,-java-11 \
	-DskipTests \
	-Dmaven.javadoc.skip=true \
	-Dmaven.source.skip=true

if [ ! -f "target/finmath-lib-${VERSION}-java8.jar" ]; then
	echo "ERROR: Expected Java 8 artifact target/finmath-lib-${VERSION}-java8.jar was not created." >&2
	exit 1
fi

cp "target/finmath-lib-${VERSION}-java8.jar" "${JAVA8_ARTIFACT}"

# Final deploy: build the Java 11 main artifact, attach Java 11 sources/javadocs
# and the pre-built Java 8 classifier jar, sign everything, and create exactly
# one Central Portal deployment.
mvn clean deploy \
	-P=java-11,attach-java8-artifact,-java-8 \
	-Djava8.artifact="${JAVA8_ARTIFACT}" \
	-DskipTests

echo "Turn to https://central.sonatype.com/publishing/deployments to publish/release the deployment."
echo "Then release the new site."
