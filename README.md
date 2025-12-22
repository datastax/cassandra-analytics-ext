Cassandra Analytics for HCD and Astra
=====================================

This library allows integration between DataStax Astra / HCD and Apache Spark, allowing users to run arbitrary
analytical workloads. It has been designed as a drop-in replacement for [Cassandra Analytics](https://github.com/apache/cassandra-analytics),
but enabling consumption of proprietary sstable versions.

> Library targets only DataStax Astra and HCD distributions. For open-source Cassandra 4.x / 5.x deployments,
> leverage standard [Cassandra Analytics](https://github.com/apache/cassandra-analytics) version.

For sample hands-on project see the [HCD example](cassandra-analytics-example-hcd/README.md).

Supported Versions
------------------

| Library Version | DataStax HCD |
|-----------------|--------------|
| 0.1             | 1.x          |

Implementation Note
-------------------

At the moment of writing, DataStax Astra and HCD are based on Cassandra 4.x with BTI support. Shipped BTI format
is prior to the one donated to open-source Cassandra 5.x. Therefore, set of custom _four nine_ bridge modules
had to be implemented allowing consumption of proprietary sstable version. Cassandra Analytics does not support
dynamic registration of new bridge modules. The output uber-JAR of `cassandra-analytics-core-ext` **replaces**
Cassandra 4.0 bridges with DataStax 4.9 modules.