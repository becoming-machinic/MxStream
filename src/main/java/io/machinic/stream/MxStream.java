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

package io.machinic.stream;

import io.machinic.stream.source.PipelineSource;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

public interface MxStream<T> {
	
	/**
	 * indicates if this is a parallel stream
	 *
	 * @return
	 */
	boolean isParallel();
	
	int getParallelism();
	
	MxStream<T> parallelStream(int parallelism);
	
	MxStream<T> parallelStream(int parallelism, ExecutorService executorService);
	
	MxStream<T> exceptionHandler(MxStreamExceptionHandler exceptionHandler);
	
	MxStreamExceptionHandler exceptionHandler();
	
	MxStream<T> filter(Predicate<? super T> predicate);
	
	MxStream<T> filter(Supplier<Predicate<? super T>> supplier);
	
	MxStream<T> skip(long n);
	
	<R> MxStream<R> map(Function<? super T, ? extends R> mapper);
	
	<R> MxStream<R> map(Supplier<Function<? super T, ? extends R>> supplier);
	
	<R> MxStream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper);
	
	<R> MxStream<R> flatMap(Supplier<Function<? super T, ? extends Stream<? extends R>>> supplier);
	
	/**
	 * Runs map operation asynchronously using Java's default VirtualThreadPool
	 *
	 * @param parallelism
	 * 		number of tasks to process at one time. This applies back pressure to limit the number of items that are loaded into memory at one time.
	 * @param mapper
	 * 		the map function
	 */
	<R> MxStream<R> asyncMap(int parallelism, Function<? super T, ? extends R> mapper);
	
	<R> MxStream<R> asyncMap(int parallelism, Supplier<Function<? super T, ? extends R>> supplier);
	
	/**
	 * Runs map operation asynchronously using the provided executorService
	 *
	 * @param parallelism
	 * 		number of tasks to process at one time. This applies back pressure to limit the number of items that are loaded into memory at one time.
	 * @param executorService
	 * 		the executorService that tasks will be summited to
	 * @param mapper
	 * 		the map function
	 */
	<R> MxStream<R> asyncMap(int parallelism, ExecutorService executorService, Function<? super T, ? extends R> mapper);
	
	<R> MxStream<R> asyncMap(int parallelism, ExecutorService executorService, Supplier<Function<? super T, ? extends R>> supplier);
	
	MxStream<List<T>> batch(int batchSize);
	
	MxStream<List<T>> batch(int batchSize, long timeout, TimeUnit timeUnit);
	
	MxStream<T> peek(Consumer<? super T> action);
	
	MxStream<T> peek(Supplier<Consumer<? super T>> supplier);
	
	/**
	 * Sorts items in stream using a sliding window to prevent loading all items into memory at one time. Sorting parallel streams will produce approximate results only due to the non-deterministic nature of parallel streams.
	 *
	 * @param windowSize
	 * 		maximum number of items that will be compared until pushing the lowest item forward.
	 * @param comparator
	 * 		the comparator that is used to compare items.
	 */
	MxStream<T> sorted(int windowSize, Comparator<? super T> comparator);
	
	MxStream<T> sorted(int windowSize, Supplier<Comparator<? super T>> supplier);
	
	/**
	 * FanOut converts a single threaded stream to a parallel stream at this point in the stream. If the stream is already parallel this does nothing.
	 *
	 * @param bufferSize
	 * 		size of the buffer that parallel threads read from
	 */
	MxStream<T> fanOut(int bufferSize);
	
	/**
	 * FanOut converts a single threaded stream to a parallel stream at this point in the stream. If the stream is already parallel this does nothing.
	 *
	 * @param bufferSize
	 * 		size of the buffer that parallel threads read from
	 * @param executorService
	 * 		executorService that parallel tasks are submitted to
	 */
	MxStream<T> fanOut(int bufferSize, ExecutorService executorService);
	
	void forEach(Consumer<? super T> action);
	
	void forEach(Supplier<Consumer<? super T>> supplier);
	
	<R, A> R collect(Collector<? super T, A, R> collector);
	
	List<T> toList();
	
	Set<T> toSet();
	
	Stream<T> toStream();
	
	static <T> MxStream<T> of(Stream<T> stream) {
		return new PipelineSource<>(stream);
	}
	
	static <T> MxStream<T> of(Spliterator<T> spliterator, boolean parallel) {
		return new PipelineSource<>(spliterator, parallel);
	}
	
	static <T> MxStream<T> parallel(Spliterator<T> spliterator) {
		return new PipelineSource<>(spliterator, true);
	}
	
	static <T> io.machinic.stream.MxStream<T> of(Iterator<T> iterator) {
		return new PipelineSource<>(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
	}
	
	static <T> io.machinic.stream.MxStream<T> of(Iterable<T> iterable) {
		return new PipelineSource<>(iterable.spliterator(), false);
	}
	
	/**
	 * Create a parallel MxStream from the provided source. Parallel streams will process items in a non-deterministic order.
	 *
	 * @param iterable
	 * 		stream source
	 */
	static <T> io.machinic.stream.MxStream<T> parallel(Iterable<T> iterable) {
		return new PipelineSource<>(iterable.spliterator(), true);
	}
	
	/**
	 * Create a parallel MxStream from the provided source. Parallel streams will process items in a non-deterministic order.
	 *
	 * @param iterable
	 * 		stream source
	 * @param parallelism
	 * 		number of threads that will be used to process stream
	 */
	static <T> io.machinic.stream.MxStream<T> parallel(Iterable<T> iterable, int parallelism) {
		return new PipelineSource<>(iterable.spliterator(), true, parallelism, null);
	}
	
	/**
	 * @param iterable
	 * 		stream source
	 * @param parallelism
	 * 		number of threads that will be used to process stream
	 * @param executorService
	 * 		the ExecutorService that stream will use
	 */
	static <T> io.machinic.stream.MxStream<T> parallel(Iterable<T> iterable, int parallelism, ExecutorService executorService) {
		return new PipelineSource<>(iterable.spliterator(), true, parallelism, executorService);
	}
}
