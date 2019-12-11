/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.flokkr.demo;

import java.util.Properties;

import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.functions.FoldFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.timestamps.AscendingTimestampExtractor;
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer010;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer010;
import org.apache.flink.streaming.util.serialization.SimpleStringSchema;

/**
 * Skeleton for a Flink Streaming Job.
 *
 * <p>For a tutorial how to write a Flink streaming application, check the
 * tutorials and examples on the <a href="http://flink.apache.org/docs/stable/">Flink Website</a>.
 *
 * <p>To package your application into a JAR file for execution, run
 * 'mvn clean package' on the command line.
 *
 * <p>If you change the name of the main class (with the public static void main(String[] args))
 * method, change the respective entry in the POM.xml file (simply search for 'mainClass').
 */
public class StreamingJob {

	public static void main(String[] args) throws Exception {

		// Set parameters
		ParameterTool parameters = ParameterTool.fromArgs (args);
		String inputTopic = parameters.get ("inputTopic", "transactions");
		String outputTopic = parameters.get ("outputTopic", "fraud");
		String kafka_host = parameters.get ("kafka_host", "kafka-broker-0.kafka-broker:9092");

		// create execution environment
		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment ();
		env.setStreamTimeCharacteristic (TimeCharacteristic.EventTime);


		Properties properties = new Properties ();
		properties.setProperty ("bootstrap.servers", kafka_host);
		properties.setProperty ("group.id", "flink_consumer");


		DataStream<Transaction> transactionsStream = env
				.addSource (new FlinkKafkaConsumer010<String>(inputTopic, new SimpleStringSchema(), properties))
				// Map from String from Kafka Stream to Transaction.
				.map (new MapFunction<String, Transaction>() {
					@Override
					public Transaction map(String value) throws Exception {
						return new Transaction (value);
					}
				});

		transactionsStream.print ();

		// Extract timestamp information to support 'event-time' processing
		SingleOutputStreamOperator<Transaction> timestampedStream = transactionsStream.assignTimestampsAndWatermarks (
				new AscendingTimestampExtractor<Transaction>() {
					@Override
					public long extractAscendingTimestamp(Transaction element) {
						return element.getTimestamp ();
					}
				});
		timestampedStream.print ();

		DataStream<TransactionAggregate> rate_count = timestampedStream
				.keyBy ("origin", "target")
				// Sum over ten minute
				.window (SlidingEventTimeWindows.of (Time.minutes (10), Time.minutes (2)  ))
				// Fold into Aggregate.
				.fold (new TransactionAggregate (), new FoldFunction<Transaction,TransactionAggregate>() {
					@Override
					public TransactionAggregate fold( TransactionAggregate transactionAggregate, Transaction transaction) {
						transactionAggregate.transactionVector.add (transaction);
						transactionAggregate.amount += transaction.getAmount ();
						transactionAggregate.startTimestamp = Math.min(transactionAggregate.startTimestamp, transaction.getTimestamp ());
						transactionAggregate.endTimestamp = Math.max (transactionAggregate.endTimestamp, transaction.getTimestamp ());
						return transactionAggregate;
					}
				});

		DataStream<String> kafka_output = rate_count
				.filter (new FilterFunction<TransactionAggregate>() {
					@Override
					public boolean filter(TransactionAggregate transaction) throws Exception {
						// Output if summed amount greater than 10000
						return (transaction.amount > 10000) ;
					}})
				.map (new MapFunction<TransactionAggregate, String> () {
					@Override
					public String map(TransactionAggregate transactionAggregate) throws Exception {
						return transactionAggregate.toString ();
					}
				});

		rate_count.print ();


		kafka_output.addSink (new FlinkKafkaProducer010<String>(outputTopic, new SimpleStringSchema (), properties));

		env.execute ();
	}
}
