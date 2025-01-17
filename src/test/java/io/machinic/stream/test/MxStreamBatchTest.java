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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static io.machinic.stream.test.TestData.INTEGER_LIST_A;
import static io.machinic.stream.test.TestData.INTEGER_LIST_D;
import static io.machinic.stream.test.TestData.INTEGER_SET_D;

@Execution(ExecutionMode.CONCURRENT)
public class MxStreamBatchTest {
	
	@Test
	public void batchTest() {
		Assertions.assertEquals(INTEGER_LIST_D,
				MxStream.of(INTEGER_LIST_A)
						.batch(5)
						.toList());
	}
	
	@Test
	public void batchParallelTest() {
		Assertions.assertEquals(INTEGER_SET_D,
				MxStream.parallel(INTEGER_LIST_A, 1)
						.batch(5)
						.toSet());
	}
}
