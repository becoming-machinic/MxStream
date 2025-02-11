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
import io.machinic.stream.test.utils.IntegerGeneratorIterator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ForkJoinPool;
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
		String joined = MxStream.of(new IntegerGeneratorIterator(500))
				.asyncMap(100, ForkJoinPool.commonPool(),
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
	
	@Test
	public void bulkTest() {
		long count = MxStream.of(new IntegerGeneratorIterator(5000000))
				.asyncMap(100, ForkJoinPool.commonPool(),
						integer -> Integer.toString(integer))
				.collect(Collectors.counting());
		Assertions.assertEquals(5000000, count);
	}
	
}
