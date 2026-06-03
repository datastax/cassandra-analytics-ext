#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

set -xe

SCRIPT_DIR=$( dirname -- "$( readlink -f -- "$0"; )"; )

CASSANDRA_ANALYTICS_VERSION=$(cat "${SCRIPT_DIR}/../gradle.properties" | grep 'analyticsVersion' | awk '{print $3}')
DATASTAX_CASSANDRA_BRANCHES=(
  # HCD 1.x version
  "main:8f317826cdb0747124929b768a67a9c23cc45f5b"
  # HCD 2.x version
  "main-5.0:deebade59f4bbfb9e126432e73870a0c4f2ebe11"
)

DTEST_JAR_DIR="$(dirname "${SCRIPT_DIR}/")/dependencies"
DTEST_JAR_DIR=${CASSANDRA_DEP_DIR:-$DTEST_JAR_DIR}
BUILD_DIR="${DTEST_JAR_DIR}/cassandra-analytics-build"

SED=sed
case "$(uname -s)" in
    Darwin*) SED=gsed;;
esac

if ! which $SED >/dev/null; then
    echo "GNU sed executable not found. On MacOS install using 'brew install gnu-sed'"
    exit 1
fi

build_oss_analytics() {
  # checkout Cassandra Analytics
  git clone --depth 1 --single-branch --branch "cassandra-analytics-${CASSANDRA_ANALYTICS_VERSION}" "https://github.com/apache/cassandra-analytics.git" cassandra-analytics
  cd cassandra-analytics

  # build Cassandra Analytics dependencies
  CASSANDRA_USE_JDK11=true ./scripts/build-dependencies.sh

  # build Cassandra Analytics
  ./gradlew clean build -x check

  # update Spark artefacts to enable publishing to Maven repository
  for build_file in 'cassandra-analytics-spark-four-zero-converter' 'cassandra-analytics-spark-five-zero-converter'; do
    $SED -i "s|id('java-library')|id('java-library')\n    id('maven-publish')\n}\n\nif (propertyWithDefault(\"artifactType\", null) == \"spark\") {\n    apply from: \"\$rootDir/gradle/common/publishing.gradle\"|" ${build_file}/build.gradle
  done

  # update non-Spark artefacts to enable publishing to Maven repository
  for build_file in 'cassandra-five-zero-bridge' 'cassandra-five-zero-types' 'cassandra-five-zero-avro-converter' 'cassandra-four-zero-types' 'cassandra-four-zero-avro-converter'; do
    $SED -i "s|id('java-library')|id('java-library')\n    id('maven-publish')\n}\n\nif (propertyWithDefault(\"artifactType\", null) == \"common\") {\n    apply from: \"\$rootDir/gradle/common/publishing.gradle\"|" ${build_file}/build.gradle
  done

  ./gradlew cassandra-analytics-common:publishToMavenLocal -PartifactType=common
  ./gradlew cassandra-analytics-sidecar-client:publishToMavenLocal -PartifactType=common
  ./gradlew cassandra-five-zero-bridge:publishToMavenLocal -PartifactType=common
  ./gradlew cassandra-five-zero-types:publishToMavenLocal -PartifactType=common
  ./gradlew cassandra-five-zero-avro-converter:publishToMavenLocal -PartifactType=common
  ./gradlew cassandra-four-zero-types:publishToMavenLocal -PartifactType=common
  ./gradlew cassandra-four-zero-avro-converter:publishToMavenLocal -PartifactType=common

  for scala_version in '2.12' '2.13'; do
    SCALA_VERSION=${scala_version} ./gradlew cassandra-analytics-core:publishToMavenLocal -PartifactType=spark
    SCALA_VERSION=${scala_version} ./gradlew cassandra-analytics-cdc:publishToMavenLocal -PartifactType=spark
    SCALA_VERSION=${scala_version} ./gradlew cassandra-analytics-integration-framework:publishToMavenLocal -PartifactType=spark
    SCALA_VERSION=${scala_version} ./gradlew cassandra-analytics-spark-converter:publishToMavenLocal -PartifactType=spark
    SCALA_VERSION=${scala_version} ./gradlew cassandra-analytics-spark-four-zero-converter:publishToMavenLocal -PartifactType=spark
    SCALA_VERSION=${scala_version} ./gradlew cassandra-analytics-spark-five-zero-converter:publishToMavenLocal -PartifactType=spark
    SCALA_VERSION=${scala_version} ./gradlew cassandra-avro-converter:publishToMavenLocal -PartifactType=spark
    SCALA_VERSION=${scala_version} ./gradlew cassandra-bridge:publishToMavenLocal -PartifactType=spark
  done

  cd ..
  rm -rf cassandra-analytics
}

build_datastax_cassandra() {
  for index in "${!DATASTAX_CASSANDRA_BRANCHES[@]}"; do
    branchSha=(${DATASTAX_CASSANDRA_BRANCHES[$index]//:/ })
    branch=${branchSha[0]}
    sha=${branchSha[1]}

    mkdir -p "${branch}"
    cd "${branch}"
    git init
    git remote add upstream "https://github.com/datastax/cassandra.git"
    git fetch --depth=1 upstream "${sha}"
    git reset --hard FETCH_HEAD
    ant clean jar mvn-install
    cd ..
    rm -rf "${branch}"
  done
}

mkdir -p "${BUILD_DIR}"
cd "${BUILD_DIR}"
build_oss_analytics
build_datastax_cassandra

cd "${SCRIPT_DIR}/.."
rm -rf "${BUILD_DIR}"

# Fix linux-riscv64 not downloaded by Gradle
NETTY_EPOLL_VERSIONS=("4.1.118.Final" "4.1.130.Final")
NETTY_EPOLL_CLASSIFIERS=("linux-aarch_64" "linux-x86_64" "linux-riscv64")
for ver in ${NETTY_EPOLL_VERSIONS[@]}; do
  mvn org.apache.maven.plugins:maven-dependency-plugin:3.1.1:get \
    -Dartifact=io.netty:netty-transport-native-unix-common:${ver} \
    -DremoteRepositories=https://repository.jboss.org/nexus/repository/staging
  for class in ${NETTY_EPOLL_CLASSIFIERS[@]}; do
    mvn org.apache.maven.plugins:maven-dependency-plugin:3.1.1:get \
      -Dtransitive=false \
      -Dartifact=io.netty:netty-transport-native-epoll:${ver}:jar:${class} \
      -DremoteRepositories=https://repository.jboss.org/nexus/repository/staging
  done
done
mvn org.apache.maven.plugins:maven-dependency-plugin:3.1.1:get \
  -Dtransitive=false \
  -Dartifact=com.github.jnr:jffi:1.3.10:jar:native

./gradlew clean :cassandra-analytics-core-ext:assemble -x check
