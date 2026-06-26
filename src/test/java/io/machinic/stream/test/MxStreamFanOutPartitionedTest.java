/*
 * Copyright 2026 Becoming Machinic Inc.
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
import io.machinic.stream.StreamException;
import io.machinic.stream.metrics.RateStreamMetricSupplier;
import io.machinic.stream.test.utils.IntegerGeneratorIterator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.machinic.stream.test.TestData.INTEGER_LIST_A;
import static io.machinic.stream.test.TestData.INTEGER_SET_A;
import static io.machinic.stream.test.TestData.INTEGER_SET_B;
import static io.machinic.stream.test.TestData.NOOP_EXCEPTION_HANDLER;

@Execution(ExecutionMode.CONCURRENT)
public class MxStreamFanOutPartitionedTest {
	
	private static final Logger LOG = LoggerFactory.getLogger(MxStreamFanOutPartitionedTest.class);
	
	@BeforeEach
	void setUp(TestInfo testInfo) {
		LOG.info("test started: {}", testInfo.getDisplayName());
	}
	
	
	@Test
	public void fanOutPartitionedWithEmptyStreamTest() {
		Assertions.assertTrue(MxStream.of(List.<Integer>of())
				.fanOutPartitioned(10, 50, value -> value)
				.toSet()
				.isEmpty());
	}
	
	@Test
	public void fanOutPartitionedWithSingleElementStreamTest() {
		Set<Integer> result = MxStream.of(List.of(42))
				.fanOutPartitioned(10, 50, value -> value)
				.toSet();
		
		Assertions.assertEquals(Set.of(42), result);
	}
	
	@Test
	public void fanOutPartitionedEmptyInputTest() {
		Assertions.assertEquals(List.of(),
				MxStream.of(List.<Integer>of())
						.fanOutPartitioned(2, 100, value -> value)
						.toList());
	}
	
	@Test
	public void fanOutPartitionedSingleElementInputTest() {
		Assertions.assertEquals(List.of(0),
				MxStream.of(List.of(0))
						.fanOutPartitioned(2, 100, value -> value)
						.toList());
	}
	
	@Test
	public void fanOutPartitionedSmallBufferTest() {
		Assertions.assertEquals(INTEGER_SET_A, MxStream.of(INTEGER_LIST_A)
				.fanOutPartitioned(2, 1, value -> value)
				.toSet());
	}
	
	@Test
	public void fanOutPartitionedLargeBufferTest() {
		Assertions.assertEquals(INTEGER_SET_A, MxStream.of(INTEGER_LIST_A)
				.fanOutPartitioned(2, 50, value -> value)
				.toSet());
	}
	
	@Test
	public void fanOutPartitionedWithMorePartitionsThanElementsTest() {
		Assertions.assertEquals(Set.of(0, 1, 2),
				MxStream.of(List.of(0, 1, 2))
						.fanOutPartitioned(10, 50, value -> value)
						.toSet());
	}
	
	@Test
	public void fanOutPartitionedWithConstantPartitionTest() {
		Assertions.assertEquals(INTEGER_SET_A, MxStream.of(INTEGER_LIST_A)
				.fanOutPartitioned(10, 50, value -> 0)
				.toSet());
	}
	
	@Test
	public void fanOutPartitionedWithModuloPartitionTest() {
		int numElements = 100;
		Set<Integer> expected = new HashSet<>();
		for (int i = 0; i < numElements; i++) {
			expected.add(i);
		}
		
		Set<Integer> result = MxStream.of(new IntegerGeneratorIterator(numElements).toStream())
				.fanOutPartitioned(10, 50, value -> value % 10)
				.toSet();
		
		Assertions.assertEquals(expected, result);
	}
	
	@Test
	public void fanOutPartitionedWithSupplierTest() {
		AtomicInteger supplierCount = new AtomicInteger();
		
		Set<Integer> result = MxStream.of(INTEGER_LIST_A)
				.fanOutPartitioned(4, 50, () -> {
					supplierCount.incrementAndGet();
					return value -> value;
				})
				.toSet();
		
		Assertions.assertEquals(INTEGER_SET_A, result);
		Assertions.assertEquals(1, supplierCount.get());
	}
	
	@Test
	public void fanOutPartitionedWithSlowDownstreamTest() {
		Assertions.assertEquals(INTEGER_SET_A, MxStream.of(INTEGER_LIST_A)
				.fanOutPartitioned(10, 50, value -> value)
				.peek(value -> {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				})
				.toSet());
	}
	
	@Test
	public void fanOutPartitionedWithConcurrencyTest() {
		int numElements = 100;
		Set<Integer> expected = new HashSet<>();
		for (int i = 0; i < numElements; i++) {
			expected.add(i);
		}
		
		Set<Integer> result = MxStream.of(new IntegerGeneratorIterator(numElements).toStream())
				.fanOutPartitioned(10, 50, value -> value)
				.peek(value -> {
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				})
				.toSet();
		
		Assertions.assertEquals(expected, result);
	}
	
	@Test
	public void fanOutPartitionedWithParallelUpstreamTest() {
		int numElements = 100;
		Set<Integer> expected = new HashSet<>();
		for (int i = 0; i < numElements; i++) {
			expected.add(i);
		}
		
		Set<Integer> result = MxStream.of(new IntegerGeneratorIterator(numElements).toParallelStream())
				.fanOutPartitioned(10, 50, value -> value)
				.peek(value -> {
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				})
				.toSet();
		
		Assertions.assertEquals(expected, result);
	}
	
	@Test
	public void fanOutPartitionedWithExceptionHandlingTest() {
		Set<Integer> expected = new HashSet<>();
		for (int i = 0; i < 10; i++) {
			if (i % 2 == 0) {
				expected.add(i);
			}
		}
		
		Set<Integer> result = MxStream.of(new IntegerGeneratorIterator(10).toStream())
				.fanOutPartitioned(10, 50, value -> value)
				.asyncMap(10, value -> {
					if (value % 2 != 0) {
						throw new RuntimeException("Odd number encountered");
					}
					return value;
				})
				.exceptionHandler((e, context) -> System.out.println("Exception caught: " + e.getMessage()))
				.toSet();
		
		Assertions.assertEquals(expected, result);
	}
	
	@Test
	public void fanOutPartitionedWithAsyncMapTest() {
		Assertions.assertEquals(
				100L, MxStream.of(new IntegerGeneratorIterator(100).toStream())
						.fanOutPartitioned(10, 50, value -> value)
						.asyncMap(10, value -> {
							try {
								Thread.sleep(10);
							} catch (InterruptedException e) {
								throw new RuntimeException(e);
							}
							return Integer.toString(value);
						})
						.batch(10, 500, TimeUnit.MILLISECONDS)
						.asyncMap(10, batch -> {
							try {
								Thread.sleep(10);
							} catch (InterruptedException e) {
								throw new RuntimeException(e);
							}
							return batch;
						})
						.flatMap(batch -> batch.stream())
						.count());
	}
	
	@Test
	public void fanOutPartitionedMaintainsPerPartitionOrderTest() {
		List<Integer> result = MxStream.of(new IntegerGeneratorIterator(100).toStream())
				.fanOutPartitioned(2, 50, value -> value % 2)
				.toList();
		
		List<Integer> evenValues = result.stream()
				.filter(value -> value % 2 == 0)
				.toList();
		List<Integer> oddValues = result.stream()
				.filter(value -> value % 2 != 0)
				.toList();
		
		Assertions.assertEquals(new IntegerGeneratorIterator(50).toStream()
				.map(value -> value * 2)
				.toList(), evenValues);
		Assertions.assertEquals(new IntegerGeneratorIterator(50).toStream()
				.map(value -> (value * 2) + 1)
				.toList(), oddValues);
	}
	
	@Test
	public void fanOutPartitionedDefaultExceptionHandlerTest() {
		Exception exception = Assertions.assertThrows(StreamException.class, () -> {
			MxStream.of(INTEGER_LIST_A)
					.fanOutPartitioned(2, 5, value -> value)
					.peek(value -> {
						throw new RuntimeException("peek operation exception");
					}).toList();
		});
		Assertions.assertEquals("Stream failed with unhandled exception: peek operation exception", exception.getMessage());
	}
	
	@Test
	public void fanOutPartitionedPeekInterruptException() {
		Exception exception = Assertions.assertThrows(StreamException.class, () -> {
			MxStream.of(INTEGER_LIST_A)
					.fanOutPartitioned(2, 5, value -> value)
					.peek(value -> {
						Thread.currentThread().interrupt();
					}).toList();
		});
		Assertions.assertEquals("FanOutPartitionedSpliterator was interrupted", exception.getMessage());
	}
	
	@Test
	public void fanOutPartitionedPeekParallelCustomExceptionHandler() {
		Assertions.assertEquals(INTEGER_SET_B,
				MxStream.of(INTEGER_SET_B)
						.exceptionHandler(NOOP_EXCEPTION_HANDLER)
						.fanOutPartitioned(2, 5, integer -> integer)
						.peek(integer -> {
							if (integer % 2 != 0) {
								throw new RuntimeException("peek operation exception");
							}
						}).toSet());
	}
	
	@Test
	public void fanOutPartitionedMetrics() {
		RateStreamMetricSupplier metricSupplier = new RateStreamMetricSupplier();
		long count = MxStream.of(new IntegerGeneratorIterator(100))
				.peek(value -> {
					try {
						Thread.sleep(20);
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				})
				.fanOutPartitioned(2, 50, value -> value)
				.metrics(metricSupplier)
				.batch(10, 50, TimeUnit.MILLISECONDS)
				.peek(batch -> {
					// Metrics are captured by metricSupplier.
				})
				.count();
		
		Assertions.assertTrue(count > 10L);
	}
	
	@Test
	public void fanOutPartitionedRejectsParallelismLessThanTwoTest() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> MxStream.of(INTEGER_LIST_A)
				.fanOutPartitioned(1, 50, value -> value)
				.toSet());
	}
	
	@Test
	public void fanOutPartitionedRejectsBufferSizeLessThanOneTest() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> MxStream.of(INTEGER_LIST_A)
				.fanOutPartitioned(2, 0, value -> value)
				.toSet());
	}
	
	@Test
	public void fanOutPartitionedPropagatesPartitionFunctionExceptionTest() {
		Exception exception = Assertions.assertThrows(StreamException.class, () -> MxStream.of(INTEGER_LIST_A)
				.fanOutPartitioned(2, 50, value -> {
					if (value == 3) {
						throw new RuntimeException("partition function exception");
					}
					return value;
				})
				.toSet());
		
		Assertions.assertTrue(exception.getMessage().contains("partition function exception"));
	}
	
}
