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
	 * Begin gracefully stopping this stream. This will be done by shutting down the source and allowing events in the stream to complete processing.
	 */
	void stop();
	
	/**
	 * Closes the stream and handles any necessary cleanup to release resources. Processing will be aborted which can result in a non-deterministic state.
	 *
	 * Throws an Exception if an error occurs during the closure process.
	 */
	void close() throws Exception;
	
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
	 * Limits the number of elements returned by the stream to the specified value.
	 *
	 * @param n the maximum number of elements to return
	 * @return a new MxStream containing at most n occurrences of type T
	 */
	MxStream<T> limit(long n);
	
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
	 * Sorts items in stream using a sliding window to prevent loading all items into memory at one time.
	 * Sorting parallel streams will produce approximate results only due to the non-deterministic nature of parallel streams.
	 *
	 * @param windowSize The size of the window to consider when sorting elements.
	 * @param comparator A comparator to use for sorting elements. Must be a supertype of {@link Comparator}.
	 *
	 * @return A new MxStream instance with sorted elements.
	 */
	MxStream<T> sorted(int windowSize, Comparator<? super T> comparator);
	
	/**
	 * Sorts items in stream using a sliding window to prevent loading all items into memory at one time.
	 * Sorting parallel streams will produce approximate results only due to the non-deterministic nature of parallel streams.
	 *
	 * @param windowSize The size of the sliding window used for sorting. Must be greater than 0.
	 * @param supplier A supplier function that returns a comparator instance, which is used to define the sort order.
	 * @return A new MxStream instance containing elements in sorted order.
	 */
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
	 * @param supplier the action supplier to be performed for each element
	 * @throws StreamException if an error occurs during stream processing
	 * @throws StreamSourceException if the stream source fails during processing
	 * @throws StreamInterruptedException if a stream thread is interrupted during stream processing
	 */
	void forEach(Supplier<Consumer<? super T>> supplier);
	
	<R, A> R collect(Collector<? super T, A, R> collector);
	
	/**
	 * Terminate stream with a list of all elements in this stream.
	 *
	 * @return a new stream containing the elements from this stream, wrapped in an ArrayList.
	 */
	List<T> toList();
	
	/**
	 * Terminate stream with a set of all elements in this stream.
	 *
	 * @return a new stream containing the elements from this stream, wrapped in a HashSet.
	 */
	Set<T> toSet();
	
	/**
	 * Terminate stream with the number of elements in this stream.
	 *
	 * @return the number of elements in this stream.
	 */
	long count();
	
	/**
	 * Terminate stream a new standard Java stream containing the elements from this stream.
	 *
	 * @return a new stream containing the elements from this stream.
	 */
	Stream<T> toStream();
	
	/**
	 * Creates a new instance of the MxStream class from the given input {@link java.util.stream.Stream}.
	 *
	 * @param <T> The type of elements in the Stream.
	 * @param stream The Stream object to create the MxStream from.
	 * @return A new MxStream instance, wrapping the original Stream object.
	 */
	static <T> MxStream<T> of(Stream<T> stream) {
		return new PipelineSource.StreamSource<>(stream);
	}
	
	/**
	 * Creates a new instance of the MxStream class from the given input {@link java.util.stream.Stream}.
	 *
	 * @param <T> The type of elements in the input stream.
	 *
	 * @param stream        The input stream to read data from. This stream is wrapped by the MxStream instance and can be closed when no longer needed.
	 * @param parallelism   The degree of parallelism to use for processing elements from the stream. Higher values result in more simultaneous processing, but may consume more resources
	 * .
	 * @param executorService The ExecutorService to use for executing tasks. This service is responsible for managing threads that process elements from the stream.
	 *
	 * @return A new instance of the MxStream class wrapping the given input stream and configured with the specified parallelism and executor service.
	 */
	static <T> MxStream<T> of(Stream<T> stream, int parallelism, ExecutorService executorService) {
		return new PipelineSource.StreamSource<>(stream, parallelism, executorService);
	}
	
	/**
	 * Creates a new pipeline source from the given iterable.
	 *
	 * @param <T> the type of elements in the stream
	 * @param iterator an iterable to create a pipeline source from
	 * @return a new MxStream containing the elements from the stream
	 */
	static <T> MxStream<T> of(Iterator<T> iterator) {
		return new PipelineSource.IteratorSource<>(iterator);
	}
	
	/**
	 * Creates an MxStream from a {@link java.io.BufferedReader}. The BufferedReader will be read line by line and each line will be passed into the stream.
	 *
	 * @param bufferedReader The buffered reader to read from.
	 *
	 * @return A pipeline source that reads from the specified buffered reader.*/
	static MxStream<String> of(BufferedReader bufferedReader) {
		return new PipelineSource.BufferedReaderStream(bufferedReader);
	}
	
	/**
	 * Creates a new pipeline source from the given BufferedReader.
	 *
	 * @param <T> the type of elements in the stream
	 * @return a new MxStream containing the elements from the stream
	 */
	static <T> MxStream<T> of(Iterable<T> iterable) {
		return new PipelineSource.IteratorSource<>(iterable.spliterator(), false);
	}
	
}
