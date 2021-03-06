/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.elasticsearch.spark.rdd.api.java.JavaEsSpark;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.Serializable;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.elasticsearch.hadoop.cfg.ConfigurationOptions.ES_RESOURCE;
import static scala.collection.JavaConversions.propertiesAsScalaMap;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BasicEsSparkTest implements Serializable {

    private static final Properties ES_SPARK_CFG = new Properties();

    static {
        ES_SPARK_CFG.setProperty("es.nodes", "localhost");
        ES_SPARK_CFG.setProperty("es.port", "9200");
    }

    private static final transient SparkConf conf = new SparkConf().setAll(propertiesAsScalaMap(ES_SPARK_CFG)).setMaster(
            "local").setAppName("estest");
    private static transient JavaSparkContext sc = null;

    @BeforeClass
    public static void setup() {
        sc = new JavaSparkContext(conf);
    }

    @AfterClass
    public static void clean() throws Exception {
        if (sc != null) {
            sc.stop();
            // wait for jetty & spark to properly shutdown
            Thread.sleep(TimeUnit.SECONDS.toMillis(2));
        }
    }

    @Test
    public void testBasicExample() throws Exception {
        // write some stuff
        Map<String, ?> doc1 = ImmutableMap.of("one", 1, "two", 2);
        Map<String, ?> doc2 = ImmutableMap.of("OTP", "Otopeni", "SFO", "San Fran");

        String target = "spark-test/poc";
        JavaRDD<Map<String, ?>> javaRDD = sc.parallelize(ImmutableList.of(doc1, doc2));
        JavaEsSpark.saveToEs(javaRDD, target);
        JavaEsSpark.saveToEs(javaRDD, ImmutableMap.of(ES_RESOURCE, target + "1"));

        // get the docs back
        JavaPairRDD<String, Map<String, Object>> esRDD = JavaEsSpark.esRDD(sc, "spark-test/poc");
        // to do a query, use the extra param
        //esRDD = JavaEsSpark.esRDD(sc, "spark-test/poc", "?q=...") or JavaEsSpark.esRDD(sc, "spark-test/poc", "{queryDSL}")


        System.out.println(esRDD.take(2));
    }
}