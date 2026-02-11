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
| 0.2             | 1.x, 2.0     |

Implementation Note
-------------------

At the moment of writing, DataStax Astra and HCD are based on Cassandra 4.x with BTI support, and Cassandra 5.x.
Skipped sstable format is different to the one from open-source Cassandra distribution. Therefore, set of custom
_four nine_ and _five zero_ bridge modules had to be implemented allowing consumption of proprietary sstable version.
Cassandra Analytics does not support dynamic registration of new bridge modules. The output uber-JAR of
`cassandra-analytics-core-ext` **replaces** Cassandra 4.0 and 5.0 bridges with DataStax 4.9 and 5.0 modules.