DataStax HCD Example
====================

## Requirements

- Java 11
- Apache Ant and Maven
- A running DataStax HCD cluster
- A running Cassandra Sidecar service

### Step 1: Configure and Run Cassandra Sidecar

In this step, we will clone and configure the Cassandra Sidecar project. Finally, we will run Sidecar which will be
connecting to our local single-node Cassandra cluster.

```shell
git clone git@github.com:apache/cassandra-sidecar.git
cd cassandra-sidecar
CASSANDRA_USE_JDK11=true ./scripts/build-dtest-jars.sh
```

Copy and edit example configuration:

```shell
cp examples/sidecar-ccm/conf/sidecar-ccm.yaml examples/sidecar-hcd.yaml
```

Configure the `examples/sidecar-hcd.yaml` file for your local environment. You will most likely need to
update only the `cassandra_instances` section pointing to your local Cassandra data directories.
Below snippet presents how my `cassandra_instances` configuration looks like for this tutorial (running
only single-node HCD cluster). Whenever necessary, specify correct local data center name in
`driver_parameters.local_dc` parameter.

```yaml
cassandra_instances:
  - id: 1
    host: localhost
    port: 9042
    data_dirs:
      - <your_ccm_parent_path>/.ccm/test/node1/data0
    staging_dir: <your_ccm_parent_path>/.ccm/test/node1/sstable-staging
    cdc_dir: <your_ccm_parent_path>/.ccm/test/node1/cdc_raw
    commitlog_dir: /<your_ccm_parent_path>/.ccm/test/node1/commitlog
    hints_dir: <your_ccm_parent_path>/.ccm/test/node1/hints
    saved_caches_dir: <your_ccm_parent_path>/.ccm/test/node1/saved_caches
    jmx_host: 127.0.0.1
    jmx_port: 7100
    jmx_ssl_enabled: false

# ...

driver_parameters:
  contact_points:
    - "127.0.0.1:9042"
  username: cassandra
  password: cassandra
  ssl:
    enabled:  false
    keystore:
      type: PKCS12
      path: path/to/keystore.p12
      password: password
    truststore:
      type: PKCS12
      path: path/to/keystore.p12
      password: password
  num_connections: 6
  local_dc: datacenter1
```

Update of version regexp pattern may be needed in case of HCD 2.0 cluster:

```diff
diff --git a/server/src/main/java/org/apache/cassandra/sidecar/utils/SimpleCassandraVersion.java b/server/src/main/java/org/apache/cassandra/sidecar/utils/SimpleCassandraVersion.java
index 637865a..67e1dee 100644
--- a/server/src/main/java/org/apache/cassandra/sidecar/utils/SimpleCassandraVersion.java
+++ b/server/src/main/java/org/apache/cassandra/sidecar/utils/SimpleCassandraVersion.java
@@ -45,7 +45,7 @@ public class SimpleCassandraVersion implements Comparable<SimpleCassandraVersion
      * note: 3rd group matches to words but only allows number and checked after regexp test.
      * this is because 3rd and the last can be identical.
      **/
-    private static final String VERSION_REGEXP = "(\\d+)\\.(\\d+)(?:\\.(\\w+))?(\\-[.\\w]+)?([.+][.\\w]+)?";
+    private static final String VERSION_REGEXP = "(\\d+)\\.(\\d+)(?:\\.(\\w+))(?:\\.(\\w+))?(\\-[.\\w]+)?([.+][.\\w]+)?";
 
     private static final Pattern PATTERN = Pattern.compile(VERSION_REGEXP);
     private static final String SNAPSHOT = "-SNAPSHOT";
```

Finally, run the Sidecar:

```shell
$ ./gradlew run -Dsidecar.config=file:///$PWD/examples/sidecar-hcd.yaml
```

### Step 2: Insert Data

Connect to the HCD, create example table and insert some data.

```shell
ccm node1 cqlsh
```

```cassandraql
CREATE KEYSPACE spark_test WITH replication = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };

CREATE TABLE spark_test.test
(
    x INT PRIMARY KEY,
    y INT
);

INSERT INTO spark_test.test(x, y) VALUES(1, 1);
INSERT INTO spark_test.test(x, y) VALUES(2, 2);
```

### Step 3: Run the Sample Job

Navigate to `cassandra-analytics-ext` directory and execute:

```shell
$ ./gradlew :cassandra-analytics-example-hcd:run \
    -Dspark.keyspace="spark_test" \
    -Dspark.table="test" \
    --args="count"

$ ./gradlew :cassandra-analytics-example-hcd:run \
    -Dspark.keyspace="spark_test" \
    -Dspark.table="test" \
    --args="write"
```