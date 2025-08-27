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

import io.machinic.stream.metrics.AsyncMapMetricSupplier;
import io.machinic.stream.metrics.StreamMetricSupplier;

import java.io.BufferedReader;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

/**
 * The MxStream interface represents a custom stream with additional functionalities beyond the standard Java streams. It includes methods for parallel processing, asynchronous mapping, batching, filtering, sorting and more. The interface also supports
 * exception handling, and conversions from standard Java streams.
 * <ul>
 *   <li>Operations accepting suppliers will call supplier.get for each partition when running as a parallel stream.</li>
 *   <li>Operations are performed per partition unless otherwise noted.</li>
 * </ul>
 * @param <T>
 */
public interface MxStream<T> {
	
	/**
	 * Indicates if this is a parallel stream.
	 * @return true if the stream is parallel, false otherwise
	 */
	boolean isParallel();
	
	/**
	 * Returns the parallelism level of the stream.
	 * @return the parallelism level
	 */
	int getParallelism();
	
	/**
	 * Indicates if the stream is closed.
	 * @return true if the stream is closed, false otherwise
	 */
	boolean isClosed();
	
	/**
	 * returns the characteristics of the stream.
	 * @return the characteristics of the stream
	 */
	int getCharacteristics();
	
	/**
	 * Returns the exception encountered during stream processing, if any.
	 * @return the exception encountered, or else null
	 */
	StreamException getException();
	
	/**
	 * Sets the exception handler for the stream.
	 * @param exceptionHandler the exception handler
	 * @return a new stream with the mapped elements
	 */
	MxStream<T> exceptionHandler(MxStreamExceptionHandler exceptionHandler);
	
	/**
	 * Returns the exception handler for the stream.
	 * @return the exception handler
	 */
	MxStreamExceptionHandler exceptionHandler();
	
	/**
	 * Begin gracefully stopping this stream.
	 */
	void stop();
	
	/**
	 * Filters the elements of the stream using the given predicate.
	 * @param predicate the predicate to apply to each element
	 * @return a new stream with the mapped elements
	 */
	MxStream<T> filter(Predicate<? super T> predicate);
	
	/**
	 * Filters the elements of the stream using the predicate provided by the supplier.
	 * @param supplier the supplier providing the predicate
	 * @return a new stream with the mapped elements
	 */
	MxStream<T> filter(Supplier<Predicate<? super T>> supplier);
	
	/**
	 * Skips the fist n elements of teh stream.
	 * @param n the number of elements to skip
	 * @return a new stream with the mapped elements
	 */
	MxStream<T> skip(long n);
	
	/**
	 * Capture metrics from this point in stream
	 * @param streamMetricSupplier the metrics supplier that will be used to collect stream metrics
	 * @return a new stream with the mapped elements
	 */
	MxStream<T> metrics(StreamMetricSupplier streamMetricSupplier);
	
	/**
	 * Maps the elements of the stream using the given function.
	 * @param mapper the function to apply to each element
	 * @param <R> the type of the result elements
	 * @return a new stream with the mapped elements
	 */
	<R> MxStream<R> map(Function<? super T, ? extends R> mapper);
	
	/**
	 * Maps the elements of the stream using the function provided by teh supplier.
	 * @param supplier the supplier providing the function
	 * @param <R> the type of the result elements
	 * @return a new stream with the mapped elements
	 */
	<R> MxStream<R> map(Supplier<Function<? super T, ? extends R>> supplier);
	
	/**
	 * Flat maps the elements of the stream using the given function.
	 * @param mapper the function to apply to each element
	 * @param <R> the type of the result elements
	 * @return a new stream with the flat mapped elements
	 */
	<R> MxStream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper);
	
	/**
	 * Flat maps the elements of the stream using the function provided by the supplier.
	 * @param supplier the supplier providing the function
	 * @param <R> the type of the result elements
	 * @return a new stream with the flat mapped elements
	 */
	<R> MxStream<R> flatMap(Supplier<Function<? super T, ? extends Stream<? extends R>>> supplier);
	
	/**
	 * Runs the map operation asynchronously using the function provided and the default ExecutorService. The asyncMap operation will maintain stream order.
	 * @param parallelism number of tasks to process at one time. This applies back pressure to limit the number of items that are loaded into memory at one time.
	 * @param mapper the function to apply to each element
	 * @param <R> the type of the result elements
	 * @return a new stream with the mapped elements
	 */
	<R> MxStream<R> asyncMap(int parallelism, Function<? super T, ? extends R> mapper);
	
	/**
	 * Runs the map operation asynchronously using the function provided by the supplier and the default ExecutorService. The asyncMap operation will maintain stream order.
	 * @param parallelism the number of tasks to process at one time. This applies back pressure to limit the number of items that are loaded into memory at one time.
	 * @param supplier the supplier providing the function
	 * @param <R> the type of the result elements
	 * @return a new stream with the mapped elements
	 */
	<R> MxStream<R> asyncMap(int parallelism, Supplier<Function<? super T, ? extends R>> supplier);
	
	/**
	 * Runs the map operation asynchronously using the function and ExecutorService provided. The asyncMap operation will maintain stream order.
	 * @param parallelism number of tasks to process at one time. This applies back pressure to limit the number of items that are loaded into memory at one time.
	 * @param mapper the function to apply to each element
	 * @param <R> the type of the result elements
	 * @param executorService the executor service to submit tasks to
	 * @return a new stream with the mapped elements
	 */
	<R> MxStream<R> asyncMap(int parallelism, ExecutorService executorService, Function<? super T, ? extends R> mapper);
	
	/**
	 * Runs the map operation asynchronously using the function and ExecutorService provided. The asyncMap operation will maintain stream order.
	 * @param parallelism number of tasks to process at one time. This applies back pressure to limit the number of items that are loaded into memory at one time.
	 * @param mapper the function to apply to each element
	 * @param <R> the type of the result elements
	 * @param executorService the executor service to submit tasks to
	 * @param metricSupplier the metricSupplier will store captured metrics
	 * @return a new stream with the mapped elements
	 */
	<R> MxStream<R> asyncMap(int parallelism, ExecutorService executorService, AsyncMapMetricSupplier metricSupplier, Function<? super T, ? extends R> mapper);
	
	/**
	 * Runs the map operation asynchronously using the function and ExecutorService provided. The asyncMap operation will maintain stream order.
	 * @param parallelism number of tasks to process at one time. This applies back pressure to limit the number of items that are loaded into memory at one time.
	 * @param supplier the supplier providing the function
	 * @param <R> the type of the result elements
	 * @param executorService the executor service to submit tasks to
	 * @return a new stream with the mapped elements
	 */
	<R> MxStream<R> asyncMap(int parallelism, ExecutorService executorService, Supplier<Function<? super T, ? extends R>> supplier);
	
	/**
	 * Runs the map operation asynchronously using the function and ExecutorService provided. The asyncMap operation will maintain stream order.
	 * @param parallelism number of tasks to process at one time. This applies back pressure to limit the number of items that are loaded into memory at one time.
	 * @param supplier the supplier providing the function
	 * @param <R> the type of the result elements
	 * @param executorService the executor service to submit tasks to
	 * @param metricSupplier the metricSupplier will store captured metrics
	 * @return a new stream with the mapped elements
	 */
	<R> MxStream<R> asyncMap(int parallelism, ExecutorService executorService, AsyncMapMetricSupplier metricSupplier, Supplier<Function<? super T, ? extends R>> supplier);
	
	/**
	 * Batches the elements of the stream into lists of the given size. This operation is the logical opposite of flatMap.
	 * @param batchSize the size of each batch
	 * @return a new stream with the batched elements
	 */
	MxStream<List<T>> batch(int batchSize);
	
	/**
	 * Batches the elements of the stream into lists of the given size or time. Whichever comes first. This operation is the logical opposite of flatMap.
	 * @param batchSize the size of each batch
	 * @return a new stream with the batched elements
	 */
	MxStream<List<T>> batch(int batchSize, long timeout, TimeUnit timeUnit);
	
	/**
	 * Performs an action for each element of the stream.
	 * @param action the action to perform
	 * @return a new stream with the peek operation
	 */
	MxStream<T> peek(Consumer<? super T> action);
	
	/**
	 * Performs an action for each element of the stream using the action provided by teh supplier.
	 * @param supplier the supplier providing the action
	 * @return a new stream with the peek operation
	 */
	MxStream<T> peek(Supplier<Consumer<? super T>> supplier);
	
	/**
	 * Sorts items in stream using a sliding window to prevent loading all items into memory at one time. Sorting parallel streams will produce approximate results only due to the non-deterministic nature of parallel streams.
	 * @param windowSize maximum number of items that will be compared until pushing the lowest item forward.
	 * @param comparator the comparator that is used to compare items.
	 */
	MxStream<T> sorted(int windowSize, Comparator<? super T> comparator);
	
	MxStream<T> sorted(int windowSize, Supplier<Comparator<? super T>> supplier);
	
	/**
	 * FanOut converts a single threaded stream to a parallel stream at this point in the stream. If the stream is already parallel this does nothing. Parallel streams will process items in a non-deterministic order.
	 * @param parallelism the number of additional threads that will be used to process stream.
	 * @param bufferSize size of the buffer between main thread and additional threads
	 */
	MxStream<T> fanOut(int parallelism, int bufferSize);
	
	/**
	 * FanOut converts a single threaded stream to a parallel stream at this point in the stream. If the stream is already parallel this does nothing. Parallel streams will process items in a non-deterministic order.
	 * @param parallelism the number of additional threads that will be used to process stream.
	 * @param bufferSize size of the buffer between main thread and additional threads
	 * @param executorService executorService that parallel tasks are submitted to
	 */
	MxStream<T> fanOut(int parallelism, int bufferSize, ExecutorService executorService);
	
	MxStream<T> tap(TapBuilder<T> tapBuilder);
	
	/**
	 * Performs the given action for each element of the stream.
	 * This method is a terminal operation and will process all elements.
	 *
	 * @param action the action to be performed for each element
	 * @throws StreamException if an error occurs during stream processing
	 * @throws StreamSourceException if the stream source fails during processing
	 * @throws StreamInterruptedException if a stream thread is interrupted during stream processing
	 */
	void forEach(Consumer<? super T> action) throws StreamException;
	
	/**
	 * Performs the given action for each element of the stream.
	 * This method is a terminal operation and will process all elements.
	 *
	 * @param action the action to be performed for each element
	 * @throws StreamException if an error occurs during stream processing
	 * @throws StreamSourceException if the stream source fails during processing
	 * @throws StreamInterruptedException if a stream thread is interrupted during stream processing
	 */
	void forEach(Supplier<Consumer<? super T>> supplier);
	
	<R, A> R collect(Collector<? super T, A, R> collector);
	
	List<T> toList();
	
	Set<T> toSet();
	
	long count();
	
	Stream<T> toStream();
	
	static <T> MxStream<T> of(Stream<T> stream) {
		return new PipelineSource.StreamSource<>(stream);
	}
	
	static <T> MxStream<T> of(Stream<T> stream, int parallelism, ExecutorService executorService) {
		return new PipelineSource.StreamSource<>(stream, parallelism, executorService);
	}
	
	static <T> MxStream<T> of(Iterator<T> iterator) {
		return new PipelineSource.IteratorSource<>(iterator);
	}
	
	static MxStream<String> of(BufferedReader bufferedReader) {
		return new PipelineSource.BufferedReaderStream(bufferedReader);
	}
	
	static <T> MxStream<T> of(Iterable<T> iterable) {
		return new PipelineSource.IteratorSource<>(iterable.spliterator(), false);
	}
	
}
