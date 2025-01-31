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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.function.Function;

import static io.machinic.stream.test.TestData.INTEGER_LIST_A;
import static io.machinic.stream.test.TestData.NOOP_EXCEPTION_HANDLER;
import static io.machinic.stream.test.TestData.STRING_LIST_A;
import static io.machinic.stream.test.TestData.STRING_SET_A;
import static io.machinic.stream.test.TestData.STRING_SET_B;

@Execution(ExecutionMode.CONCURRENT)
public class MxStreamMapTest {
	
	@Test
	public void mapTest() {
		Assertions.assertEquals(STRING_LIST_A, MxStream.of(INTEGER_LIST_A).map(integer -> Integer.toString(integer)).toList());
	}
	
	@Test
	public void mapParallelTest() {
		Assertions.assertEquals(STRING_SET_A, MxStream.parallel(INTEGER_LIST_A).map(integer -> Integer.toString(integer)).toSet());
	}
	
	@Test
	public void mapSupplierTest() {
		CountingSupplier<Function<? super Integer, ? extends String>> supplier = new CountingSupplier<>(integer -> Integer.toString(integer));
		Assertions.assertEquals(STRING_LIST_A, MxStream.of(INTEGER_LIST_A).map(supplier).toList());
		// Supplier should only be called once on a sequential stream
		Assertions.assertEquals(1, supplier.getCount());
	}
	
	@Test
	public void mapParallelSupplierTest() {
		CountingSupplier<Function<? super Integer, ? extends String>> supplier = new CountingSupplier<>(integer -> Integer.toString(integer));
		Assertions.assertEquals(STRING_SET_A, MxStream.parallel(INTEGER_LIST_A, 3).map(supplier).toSet());
		// Supplier should be called once by the main thread and once for each additional thread
		Assertions.assertEquals(4, supplier.getCount());
	}
	
	@Test
	public void mapDefaultExceptionHandler() {
		Exception exception = Assertions.assertThrows(StreamException.class, () -> {
			MxStream.of(INTEGER_LIST_A).map(value -> {
				throw new RuntimeException("map operation exception");
			}).toList();
		});
		Assertions.assertEquals("Stream failed with unhandled exception: map operation exception", exception.getMessage());
	}
	
	@Test
	public void mapParallelDefaultExceptionHandler() {
		Exception exception = Assertions.assertThrows(StreamException.class, () -> {
			MxStream.parallel(INTEGER_LIST_A).map(value -> {
				throw new RuntimeException("map operation exception");
			}).toList();
		});
		Assertions.assertEquals("Stream failed with unhandled exception: map operation exception", exception.getMessage());
	}
	
	@Test
	public void mapCustomExceptionHandler() {
		Assertions.assertEquals(STRING_SET_B,
				MxStream.of(INTEGER_LIST_A)
						.exceptionHandler(NOOP_EXCEPTION_HANDLER)
						.map(integer -> {
							if (integer % 2 != 0) {
								throw new RuntimeException("map operation exception");
							}
							return Integer.toString(integer);
						}).toSet());
	}
	
	@Test
	public void mapParallelCustomExceptionHandler() {
		Assertions.assertEquals(STRING_SET_B,
				MxStream.parallel(INTEGER_LIST_A, 4)
						.exceptionHandler(NOOP_EXCEPTION_HANDLER)
						.map(integer -> {
							if (integer % 2 != 0) {
								throw new RuntimeException("map operation exception");
							}
							return Integer.toString(integer);
						}).toSet());
	}
}
