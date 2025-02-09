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
import io.machinic.stream.StreamException;
import io.machinic.stream.test.utils.CountingSupplier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import static io.machinic.stream.test.TestData.INTEGER_LIST_A;
import static io.machinic.stream.test.TestData.INTEGER_LIST_B;
import static io.machinic.stream.test.TestData.INTEGER_LIST_C;
import static io.machinic.stream.test.TestData.INTEGER_SET_A;
import static io.machinic.stream.test.TestData.INTEGER_SET_B;
import static io.machinic.stream.test.TestData.NOOP_EXCEPTION_HANDLER;

@Execution(ExecutionMode.SAME_THREAD)
public class MxStreamFlatMapTest {
	
	private static final Logger LOG = LoggerFactory.getLogger(MxStreamFlatMapTest.class);
	
	@BeforeEach
	void setUp(TestInfo testInfo) {
		LOG.info("test started: {}", testInfo.getDisplayName());
	}
	
	@Test
	public void flatMapTest() {
		AtomicInteger index = new AtomicInteger(0);
		Assertions.assertEquals(INTEGER_LIST_A, MxStream.of(INTEGER_LIST_C).flatMap(Collection::stream).peek(integer -> {
			Assertions.assertEquals(INTEGER_LIST_A.get(index.getAndIncrement()), integer);
		}).toList());
	}
	
	@Test
	public void flatMapParallelTest() {
		AtomicInteger counter = new AtomicInteger(0);
		Assertions.assertEquals(INTEGER_SET_A, MxStream.parallel(INTEGER_LIST_C).flatMap(Collection::stream).peek(integer -> {
			counter.getAndIncrement();
		}).toSet());
		Assertions.assertEquals(INTEGER_LIST_A.size(), counter.get());
	}
	
	@Test
	public void flatMapSupplierTest() {
		CountingSupplier<Function<? super List<Integer>, ? extends Stream<? extends Integer>>> supplier = new CountingSupplier<>(Collection::stream);
		Assertions.assertEquals(INTEGER_LIST_A, MxStream.of(INTEGER_LIST_C).flatMap(supplier).toList());
		// Supplier should only be called once on a sequential stream
		Assertions.assertEquals(1, supplier.getCount());
	}
	
	@Test
	public void flatMapParallelSupplierTest() {
		CountingSupplier<Function<? super List<Integer>, ? extends Stream<? extends Integer>>> supplier = new CountingSupplier<>(Collection::stream);
		Assertions.assertEquals(INTEGER_SET_A, MxStream.parallel(INTEGER_LIST_C, 3).flatMap(supplier).toSet());
		// Supplier should be called once by the main thread and once for each additional thread
		Assertions.assertEquals(4, supplier.getCount());
	}
	
	@Test
	public void flatMapDefaultExceptionHandler() {
		Exception exception = Assertions.assertThrows(StreamException.class, () -> {
			MxStream.of(INTEGER_LIST_C).flatMap(value -> {
				throw new RuntimeException("flatMap operation exception");
			}).toList();
		});
		Assertions.assertEquals("Stream failed with unhandled exception: flatMap operation exception", exception.getMessage());
	}
	
	@Test
	public void flatMapParallelDefaultExceptionHandler() {
		Exception exception = Assertions.assertThrows(StreamException.class, () -> {
			MxStream.parallel(INTEGER_LIST_C).flatMap(value -> {
				throw new RuntimeException("flatMap operation exception");
			}).toList();
		});
		Assertions.assertEquals("Stream failed with unhandled exception: flatMap operation exception", exception.getMessage());
	}
	
	@Test
	public void flatMapCustomExceptionHandler() {
		Assertions.assertEquals(INTEGER_LIST_B, MxStream.of(INTEGER_LIST_C).exceptionHandler(NOOP_EXCEPTION_HANDLER).flatMap(list -> {
			if (list.get(0) % 2 != 0) {
				throw new RuntimeException("flatMap operation exception");
			}
			return list.stream();
		}).toList());
	}
	
	@Test
	public void flatMapParallelCustomExceptionHandler() {
		Assertions.assertEquals(INTEGER_SET_B, MxStream.parallel(INTEGER_LIST_C, 4).exceptionHandler(NOOP_EXCEPTION_HANDLER).flatMap(list -> {
			if (list.get(0) % 2 != 0) {
				throw new RuntimeException("flatMap operation exception");
			}
			return list.stream();
		}).toSet());
	}
}
