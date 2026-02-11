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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.bridge.HcdVersion;
import org.apache.cassandra.spark.KryoRegister;
import org.apache.cassandra.spark.bulkwriter.BulkSparkConf;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.sql.DataFrameWriter;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.StructType;

import static org.apache.spark.sql.types.DataTypes.IntegerType;

// CHECKSTYLE IGNORE: Example code.
public class DataImportJob
{
    private static final Logger log = LoggerFactory.getLogger(DataImportJob.class);

    public static void start(String[] args)
    {
        System.setProperty("cassandra.analytics.bridges.sstable_format", "bti");
        SparkConf sparkConf = new SparkConf()
                .loadFromSystemProperties(true)
                .setAppName("Spark Cassandra Row Count Example")
                .set("spark.master", "local[*]")
                .set(BulkSparkConf.CASSANDRA_VERSION, HcdVersion.HCD_2_0.getCassandraVersion());

        KryoRegister.setup(sparkConf);

        SparkSession.Builder sparkBuilder = SparkSession.builder().config(sparkConf);

        SparkSession spark = sparkBuilder.getOrCreate();
        SparkContext sc = spark.sparkContext();
        SQLContext sql = spark.sqlContext();

        log.info("Spark configuration: {}", sparkConf.toDebugString());
        try
        {
            Map<String, String> options = configure(sparkConf);

            StructType schema = new StructType()
                                .add("x", IntegerType, false)
                                .add("y", IntegerType, false);

            JavaSparkContext javaSparkContext = JavaSparkContext.fromSparkContext(sc);
            int parallelism = sc.defaultParallelism();
            int rowCount = 100;
            log.info("Importing {} rows with parallelism {}", rowCount, parallelism);
            JavaRDD<Row> rows = genDataset(javaSparkContext, rowCount, parallelism);
            Dataset<Row> df = sql.createDataFrame(rows, schema);

            DataFrameWriter<Row> writer = df.write().format("org.apache.cassandra.spark.sparksql.HcdDataSink");
            writer.options(options);
            writer.mode("append").save();

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

    private static Map<String, String> configure(SparkConf sparkConf)
    {
        Map<String, String> options = new HashMap<>();
        options.put("sidecar_contact_points", "localhost");
        options.put("local_dc", "datacenter1");
        options.put("bulk_writer_cl", "ONE");
        options.put("number_splits", "-1");
        options.put("data_transport", "DIRECT");
        options.put("keyspace", sparkConf.get("spark.keyspace"));
        options.put("table", sparkConf.get("spark.table"));
        // Optionally provide SSL certificates for mTLS authentication, authorization
        // and transport encryption
        // options.put("KEYSTORE_PATH", "/path/to/keystore.p12");
        // options.put("KEYSTORE_PASSWORD", "password");
        // options.put("TRUSTSTORE_PATH", "/path/to/truststore.jks");
        // options.put("TRUSTSTORE_PASSWORD", "password");
        return options;
    }

    private static JavaRDD<Row> genDataset(JavaSparkContext sc, int records, Integer parallelism)
    {
        int recordsPerPartition = records / parallelism;
        int remainder = recordsPerPartition + (records - (recordsPerPartition * parallelism));
        List<Integer> seq = IntStream.range(0, parallelism).boxed().collect(Collectors.toList());
        return sc.parallelize(seq, parallelism).mapPartitionsWithIndex(
        (Function2<Integer, Iterator<Integer>, Iterator<Row>>) (index, integerIterator) -> {
            int firstRecordNumber = index * recordsPerPartition;
            int recordsToGenerate = (index == (parallelism - 1)) ? remainder : recordsPerPartition;
            return IntStream.range(0, recordsToGenerate).mapToObj((offset) -> {
                int i = firstRecordNumber + offset;
                return RowFactory.create(i, i);
            }).iterator();
        }, false);
    }
}
