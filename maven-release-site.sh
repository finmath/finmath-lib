#!/bin/sh
set -e

# Run tests with coverage before publishing the site. Clean removes stale coverage
# data; do not use -DskipTests when generating a coverage report for a release.

# need to limit buffer size to avoid https issues because our pages have become big
GIT_CONFIG_COUNT=2 \
GIT_CONFIG_KEY_0=http.https://github.com/.version \
GIT_CONFIG_VALUE_0=HTTP/1.1 \
GIT_CONFIG_KEY_1=http.https://github.com/.postBuffer \
GIT_CONFIG_VALUE_1=52428800 \
mvn clean install site site:stage site-deploy -Pcoverage "$@"
