Developer's Guide
=================

Quick introduction to basic development activities.

Build
-----

1. Clone and build Cassandra Analytics 0.2.0:
   ```shell
   git clone https://github.com/apache/cassandra-analytics
   cd cassandra-analytics
   # check Cassandra Analytics version in gradle.properties
   git checkout cassandra-analytics-0.4.0
   
   CASSANDRA_USE_JDK11=true ./scripts/build-dependencies.sh
   ./gradlew clean build -x check
   ```

2. Update below Gradle build descriptors to publish the artefacts:
   - _cassandra-analytics-spark-four-zero-converter/build.gradle_
   - _cassandra-analytics-spark-five-zero-converter/build.gradle_

   ```groovy
   plugins {
       id('java-library')
       id('maven-publish')
   }
   
   if (propertyWithDefault("artifactType", null) == "spark")
   {
       apply from: "$rootDir/gradle/common/publishing.gradle"
   }
   
   // leave the rest unchanged
   ```

3. Similarly, in below files insert on top:
   - _cassandra-five-zero-bridge/build.gradle_
   - _cassandra-five-zero-types/build.gradle_
   - _cassandra-five-zero-avro-converter/build.gradle_
   - _cassandra-four-zero-types/build.gradle_
   - _cassandra-four-zero-avro-converter/build.gradle_

  ```groovy
  plugins {
      id('java-library')
      id('maven-publish')
  }
   
  if (propertyWithDefault("artifactType", null) == "common")
  {
      apply from: "$rootDir/gradle/common/publishing.gradle"
  }
   
  // leave the rest unchanged
  ```

4. Publish Cassandra Analytics artefacts to local Maven repository:
   ```shell
   ./gradlew cassandra-analytics-common:publishToMavenLocal -PartifactType=common
   ./gradlew cassandra-analytics-sidecar-client:publishToMavenLocal -PartifactType=common
   ./gradlew cassandra-five-zero-bridge:publishToMavenLocal -PartifactType=common
   ./gradlew cassandra-five-zero-types:publishToMavenLocal -PartifactType=common
   ./gradlew cassandra-five-zero-avro-converter:publishToMavenLocal -PartifactType=common
   ./gradlew cassandra-four-zero-types:publishToMavenLocal -PartifactType=common
   ./gradlew cassandra-four-zero-avro-converter:publishToMavenLocal -PartifactType=common

   # repeat below for scala 2.13 if needed
   SCALA_VERSION=2.12 ./gradlew cassandra-analytics-core:publishToMavenLocal -PartifactType=spark
   SCALA_VERSION=2.12 ./gradlew cassandra-analytics-cdc:publishToMavenLocal -PartifactType=spark
   SCALA_VERSION=2.12 ./gradlew cassandra-analytics-integration-framework:publishToMavenLocal -PartifactType=spark
   SCALA_VERSION=2.12 ./gradlew cassandra-analytics-spark-converter:publishToMavenLocal -PartifactType=spark
   SCALA_VERSION=2.12 ./gradlew cassandra-analytics-spark-four-zero-converter:publishToMavenLocal -PartifactType=spark
   SCALA_VERSION=2.12 ./gradlew cassandra-analytics-spark-five-zero-converter:publishToMavenLocal -PartifactType=spark
   SCALA_VERSION=2.12 ./gradlew cassandra-avro-converter:publishToMavenLocal -PartifactType=spark
   SCALA_VERSION=2.12 ./gradlew cassandra-bridge:publishToMavenLocal -PartifactType=spark
   ```

5. Clone and build DataStax Cassandra 4 and 5 distributions:
   ```shell
   git clone git@github.com:datastax/cassandra.git
   cd cassandra
   git checkout 8f317826cdb0
   ant clean jar mvn-install
   git checkout deebade59f4b
   ant clean jar mvn-install
   ```

6. Finally, build repackaged Cassandra Analytics library. Uber-JAR which needs to be added to Spark job, will be present in
   _cassandra-analytics-core-ext/build/libs_ directory.
   ```shell
   git clone git@github.com:datastax/cassandra-analytics-ext.git
   cd cassandra-analytics-ext
   ./gradlew clean :cassandra-analytics-core-ext:assemble -x check
   ```

Integration Tests
-----------------

1. Build Cassandra DTest JAR:
   ```shell
   CASSANDRA_USE_JDK11=true ./scripts/build-dependencies.sh
   ```

2. Run tests (all unit tests and integration for version 4.0):
   ```shell
   ./gradlew clean codeCheckTasks test
   ```

3. Run integration tests for each Cassandra version:
   ```shell
   DTEST_JAR="dtest-4.0.11.0.jar" CASSANDRA_VERSION="4.0" ./gradlew clean :cassandra-analytics-integration-tests:test
   DTEST_JAR="dtest-5.0.4.0.jar" CASSANDRA_VERSION="5.0" ./gradlew clean :cassandra-analytics-integration-tests:test
   ```

Artefact Release
----------------

Publish uber-JAR and _pom.xml_ to local Maven repository:

```shell
./gradlew -PscalaVersion=2.12 :cassandra-analytics-core-ext:publishToMavenLocal
```