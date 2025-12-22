Developer's Guide
=================

Quick introduction to basic development activities.

Build
-----

1. Clone and build Cassandra Analytics 0.2.0:
   ```shell
   git clone https://github.com/apache/cassandra-analytics
   cd cassandra-analytics
   git checkout cassandra-analytics-0.2.0
   
   CASSANDRA_USE_JDK11=true ./scripts/build-dependencies.sh
   ./gradlew clean build -x check
   ```

2. Update _cassandra-analytics-spark-four-zero-converter/build.gradle_ to publish the artefact:
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

3. Similarly, in _cassandra-five-zero-bridge/build.gradle_, _cassandra-four-zero-types/build.gradle_ and
   _cassandra-four-zero-avro-converter/build.gradle_ insert on top:
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
   ./gradlew cassandra-four-zero-types:publishToMavenLocal -PartifactType=common
   ./gradlew cassandra-four-zero-avro-converter:publishToMavenLocal -PartifactType=common

   SCALA_VERSION=2.13 ./gradlew cassandra-analytics-core:publishToMavenLocal -PartifactType=spark
   SCALA_VERSION=2.13 ./gradlew cassandra-analytics-cdc:publishToMavenLocal -PartifactType=spark
   SCALA_VERSION=2.13 ./gradlew cassandra-analytics-integration-framework:publishToMavenLocal -PartifactType=spark
   SCALA_VERSION=2.13 ./gradlew cassandra-analytics-spark-converter:publishToMavenLocal -PartifactType=spark
   SCALA_VERSION=2.13 ./gradlew cassandra-analytics-spark-four-zero-converter:publishToMavenLocal -PartifactType=spark
   SCALA_VERSION=2.13 ./gradlew cassandra-avro-converter:publishToMavenLocal -PartifactType=spark
   SCALA_VERSION=2.13 ./gradlew cassandra-bridge:publishToMavenLocal -PartifactType=spark
   ```

5. Clone and build DataStax Cassandra distribution:
   ```shell
   git clone git@github.com:datastax/cassandra.git
   cd cassandra
   git checkout 8f317826cdb0
   ant clean jar mvn-install
   ```

6. Finally, build repackaged Cassandra Analytics library. Uber-JAR which needs to be added to Spark job, will be present in
   _cassandra-analytics-core-ext/build/libs_ directory.
   ```shell
   git clone git@github.com:datastax/cassandra-analytics-ext.git
   cd cassandra-analytics-ext
   ./gradlew clean assemble -x check
   ```

Integration Tests
-----------------

1. Build Cassandra DTest JAR:
   ```shell
   CASSANDRA_USE_JDK11=true ./scripts/build-dependencies.sh
   ```

2. Run tests:
   ```shell
   ./gradlew clean test
   ```

Artefact Release
----------------

Publish uber-JAR and _pom.xml_ to local Maven repository:

```shell
./gradlew :cassandra-analytics-core-ext:publishToMavenLocal
```