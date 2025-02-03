///*
// * Copyright 2025 Becoming Machinic Inc.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package io.machinic.test;
//
//import io.machinic.stream.MxStream;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.parallel.Execution;
//import org.junit.jupiter.api.parallel.ExecutionMode;
//
//import java.util.ArrayList;
//import java.util.Comparator;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Set;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.stream.Collectors;
//
//@Execution(ExecutionMode.CONCURRENT)
//public class MxStreamTests {
//	// TODO finish migrating these tests into stream.test
//	@Test
//	public void batchTest() {
//		List<String> array = List.of("1", "2", "3", "4", "5", "6", "7", "8");
//
//		Assertions.assertEquals(
//				List.of(List.of("1", "2", "3"), List.of("4", "5", "6"), List.of("7", "8")),
//				MxStream.of(array)
//						.batch(3)
//						.collect(Collectors.toList()));
//
//		Assertions.assertEquals(
//				List.of(List.of("1"), List.of("2"), List.of("3"), List.of("4"), List.of("5"), List.of("6"), List.of("7"), List.of("8")),
//				MxStream.of(array)
//						.batch(1)
//						.toList());
//
//		Assertions.assertEquals(
//				List.of(List.of("1", "2", "3", "4", "5", "6", "7", "8")),
//				MxStream.of(array)
//						.batch(10)
//						.toList());
//
//		{
//			List<String> peekList = new ArrayList<>();
//			List<String> forEachList = new ArrayList<>();
//			MxStream.of(array)
//					.peek(peekList::add)
//					.forEach(forEachList::add);
//			Assertions.assertEquals(peekList, forEachList);
//		}
//
//		Assertions.assertEquals(
//				List.of("1", "2", "3", "4", "5", "6", "7", "8"),
//				MxStream.of(array)
//						.batch(10)
//						.flatMap(List::stream)
//						.toList());
//
//		Assertions.assertEquals(
//				List.of("2", "3", "4", "5", "6", "7", "8"),
//				MxStream.of(array)
//						.skip(1)
//						.toList());
//
//		Assertions.assertEquals(
//				List.of("3", "4", "5", "6", "7", "8"),
//				MxStream.of(array)
//						.batch(2)
//						.skip(1)
//						.flatMap(List::stream)
//						.toList());
//	}
//
//	@Test
//	public void batchParallelTest() {
//		List<String> array = List.of("1", "2", "3", "4", "5", "6", "7", "8");
//
//		Assertions.assertEquals(
//				Set.of(List.of("1", "2"), List.of("3", "4"), List.of("5", "6"), List.of("7", "8")),
//				MxStream.parallel(array, 2)
//						.batch(2)
//						.collect(Collectors.toSet())
//		);
//
//		Assertions.assertEquals(
//				Set.of(List.of("1"), List.of("2"), List.of("3"), List.of("4"), List.of("5"), List.of("6"), List.of("7"), List.of("8")),
//				MxStream.parallel(array)
//						.batch(1)
//						.toSet());
//
//		{
//			Set<String> peekSet = ConcurrentHashMap.newKeySet();
//			Set<String> forEachSet = ConcurrentHashMap.newKeySet();
//			MxStream.parallel(array)
//					.peek(peekSet::add)
//					.forEach(forEachSet::add);
//			Assertions.assertEquals(peekSet, forEachSet);
//		}
//
//		Assertions.assertEquals(
//				new HashSet<>(array),
//				MxStream.parallel(array)
//						.batch(4)
//						.flatMap(List::stream)
//						.toSet());
//
//	}
//
//	@Test
//	public void windowedSortTest() {
//		List<String> list = List.of("1", "2", "3", "4", "5", "6", "7", "8");
//
//		Assertions.assertEquals(
//				list,
//				MxStream.of(list)
//						.sorted(10, Comparator.naturalOrder())
//						.toList());
//
//
//		{
//			List<Integer> array = new ArrayList<>();
//			for (int i = 1; i < 50; i++) {
//				array.add(i);
//			}
//
//			Assertions.assertEquals(
//					array.stream().sorted(Comparator.naturalOrder()).map(Object::toString).collect(Collectors.toList()),
//					MxStream.of(array)
//							.sorted(30, Comparator.naturalOrder())
//							.map(Object::toString)
//							.toList());
//
//			Assertions.assertEquals(
//					array.stream().map(Object::toString).collect(Collectors.toSet()),
//					MxStream.parallel(array, 1)
//							.sorted(30, Comparator.naturalOrder())
//							.map(Object::toString)
//							.toSet());
//
//		}
//
//	}
//
//	@Test
//	public void fanOutTest() {
//		List<Integer> array = List.of(1, 2, 3, 4, 5, 6, 7, 8);
//
//		Assertions.assertEquals(
//				Set.of("3", "4", "5", "6", "7", "8"),
//				MxStream.of(array)
//						.skip(2)
//						.fanOut(4)
//						.map(Object::toString)
//						.toSet());
//	}
//
//	@Test
//	public void fanOutParallelTest() {
//		MxStream<Integer> stream = MxStream.parallel(List.of(1, 2, 3, 4, 5, 6, 7, 8));
//		// If stream is a parallel stream fanOut does nothing
//		Assertions.assertSame(stream, stream.fanOut(4));
//	}
//
//}
