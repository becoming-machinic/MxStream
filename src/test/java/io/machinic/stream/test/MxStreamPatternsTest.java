/*
 * Copyright 2025 Becoming Machinic Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.machinic.stream.test;

import io.machinic.stream.MxStream;
import io.machinic.stream.TapBuilder;
import io.machinic.stream.metrics.RateAsyncMapMetricSupplier;
import io.machinic.stream.test.utils.IntegerGeneratorIterator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Execution(ExecutionMode.SAME_THREAD)
public class MxStreamPatternsTest {
	private static final Logger LOG = LoggerFactory.getLogger(MxStreamPatternsTest.class);
	
	@BeforeEach
	void setUp(TestInfo testInfo) {
		LOG.info("test started: {}", testInfo.getDisplayName());
	}
	
	@Test
	public void asyncMapTest() {
		RateAsyncMapMetricSupplier metricSupplier = new RateAsyncMapMetricSupplier();
		String joined = MxStream.of(new IntegerGeneratorIterator(500))
				.asyncMap(100, ForkJoinPool.commonPool(),
						metricSupplier,
						integer -> {
							try {
								// slow task
								Thread.sleep(10);
							} catch (InterruptedException e) {
								throw new RuntimeException(e);
							}
							return Integer.toString(integer);
						})
				.collect(Collectors.joining(","));
		System.out.println(joined);
		System.out.println("Duration: " + metricSupplier.getDuration());
		System.out.println("Average task duration: " + metricSupplier.getAverageDuration());
		System.out.println("Total task duration: " + metricSupplier.getTotalDuration());
		System.out.println("Average task rate: " + metricSupplier.getAverageRate());
	}
	
	@Test
	public void chainedAsyncMapTest() {
		RateAsyncMapMetricSupplier metricSupplier = new RateAsyncMapMetricSupplier();
		long count = MxStream.of(new IntegerGeneratorIterator(500))
				.asyncMap(100, ForkJoinPool.commonPool(),
						metricSupplier,
						integer -> {
							try {
								// slow task
								Thread.sleep(10);
							} catch (InterruptedException e) {
								throw new RuntimeException(e);
							}
							return integer;
						})
				.asyncMap(50, ForkJoinPool.commonPool(),
						integer -> {
							try {
								// slow task
								Thread.sleep(ThreadLocalRandom.current().nextInt(10));
							} catch (InterruptedException e) {
								throw new RuntimeException(e);
							}
							return integer;
						})
				.batch(100, 20, TimeUnit.MILLISECONDS)
				.asyncMap(5, ForkJoinPool.commonPool(),
						batch -> batch)
				.flatMap(Collection::stream)
				.count();
		Assertions.assertEquals(500, count);
		System.out.println("Duration: " + metricSupplier.getDuration());
		System.out.println("Average task duration: " + metricSupplier.getAverageDuration());
		System.out.println("Total task duration: " + metricSupplier.getTotalDuration());
		System.out.println("Average task rate: " + metricSupplier.getAverageRate());
	}
	
	@Test
	public void streamTapTest() throws InterruptedException {
		TapBuilder<String> tapBuilder = new TapBuilder<>(5);
		
		AtomicLong counter = new AtomicLong(0);
		Thread streamThread = new Thread(() -> {
			MxStream.of(new IntegerGeneratorIterator(500))
					.map(integer -> Integer.toString(integer))
					.tap(tapBuilder)
					.forEach(value -> counter.incrementAndGet());
		});
		streamThread.setName("stream-thread");
		streamThread.start();
		
		long tapCount = tapBuilder.awaitBuild()
				.collect(Collectors.counting());
		
		Assertions.assertEquals(500, counter.get());
		Assertions.assertEquals(500, tapCount);
	}
	
}
