/*
 * Copyright DataStax, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.cassandra.spark.example;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.spark.KryoRegister;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.sql.DataFrameReader;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.SparkSession;
import scala.Tuple2;

// CHECKSTYLE IGNORE: Example code.
public class RowCountJob
{
    private static final Logger log = LoggerFactory.getLogger(RowCountJob.class);

    public static void main(String[] args)
    {
        // System.setProperty("spark.cassandra_analytics.cassandra.version", "5.0.0");
        System.setProperty("cassandra.analytics.bridges.sstable_format", "bti");
        SparkConf sparkConf = new SparkConf()
                .loadFromSystemProperties(true)
                .setAppName("Spark Cassandra Row Count Example")
                .set("spark.master", "local[*]");

        KryoRegister.setup(sparkConf);

        SparkSession.Builder sparkBuilder = SparkSession.builder().config(sparkConf);

        SparkSession spark = sparkBuilder.getOrCreate();
        SparkContext sc = spark.sparkContext();
        SQLContext sql = spark.sqlContext();

        log.info("Spark configuration: {}", sparkConf.toDebugString());
        try
        {
            Map<String, String> options = configure(sparkConf, sc);

            DataFrameReader reader = sql.read()
                    .format("org.apache.cassandra.spark.sparksql.CassandraDataSource")
                    .options(options);
            Dataset<Row> df = reader.load();

            log.info("Count: {}", df.count());
            log.info("Finished Spark job, shutting down...");
        }
        catch (Throwable throwable)
        {
            log.error("Unexpected exception while executing Spark job: " + throwable.getMessage(), throwable);
        }
        finally
        {
            try
            {
                sc.stop();
            }
            catch (Throwable ignored)
            {
            }
        }
    }

    private static Map<String, String> configure(SparkConf sparkConf, SparkContext sparkContext)
    {
        int coresPerExecutor = sparkConf.getInt("spark.executor.cores", 1);
        int executorInstances = sparkConf.getInt("spark.executor.instances", 1);
        int numExecutors = sparkConf.getInt("spark.dynamicAllocation.maxExecutors", executorInstances);
        int numCores = coresPerExecutor * numExecutors;

        Map<String, String> options = new HashMap<>();
        options.put("sidecar_contact_points", "localhost");
        options.put("DC", "datacenter1");
        // options.put("consistencyLevel", "ONE");
        options.put("snapshotName", UUID.randomUUID().toString());
        options.put("createSnapshot", "true");
        options.put("keyspace", sparkConf.get("spark.keyspace"));
        options.put("table", sparkConf.get("spark.table"));

        options.put("defaultParallelism", String.valueOf(sparkContext.defaultParallelism()));
        options.put("numCores", String.valueOf(numCores));
        options.put("sizing", "default");

        Arrays.stream(sparkConf.getAll())
              .collect(Collectors.toMap(t -> t._1().replaceFirst("spark.", ""), Tuple2::_2))
              .forEach(options::putIfAbsent);

        return options;
    }
}
