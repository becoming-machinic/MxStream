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

import io.machinic.stream.sink.AbstractSink;
import io.machinic.stream.sink.CollectorSink;
import io.machinic.stream.sink.ForEachSink;
import io.machinic.stream.source.PipelineSource;
import io.machinic.stream.spliterator.AbstractSpliterator;
import io.machinic.stream.spliterator.AsyncMapSpliterator;
import io.machinic.stream.spliterator.BatchSpliterator;
import io.machinic.stream.spliterator.FanOutSpliterator;
import io.machinic.stream.spliterator.FilteringSpliterator;
import io.machinic.stream.spliterator.FlatMapSpliterator;
import io.machinic.stream.spliterator.MapSpliterator;
import io.machinic.stream.spliterator.PassThoughSpliterator;
import io.machinic.stream.spliterator.PeekSpliterator;
import io.machinic.stream.spliterator.WindowedSortSpliterator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public abstract class BasePipeline<IN, OUT> implements MxStream<OUT> {
	
	protected abstract PipelineSource<?> getSource();
	
	protected abstract BasePipeline<?, IN> getPrevious();
	
	protected abstract AbstractSpliterator<IN, OUT> getSpliterator();
	
	protected ExecutorService getExecutorService() {
		return getPrevious().getExecutorService();
	}
	
	public boolean isParallel() {
		return getPrevious().isParallel();
	}
	
	public int getParallelism() {
		return getPrevious().getParallelism();
	}
	
	@Override
	public MxStream<OUT> parallelStream(int parallelism) {
		return parallelStream(parallelism, this.getExecutorService());
	}
	
	@Override
	public MxStream<OUT> parallelStream(int parallelism, ExecutorService executorService) {
		if (!this.isParallel()) {
			if (parallelism < 0) {
				throw new IllegalArgumentException(Long.toString(parallelism));
			}
			return new PipelineParallel<OUT, OUT>(this.getSource(), this, parallelism, executorService, new PassThoughSpliterator<>(this, this.getSpliterator()));
		}
		return this;
	}
	
	@Override
	public MxStream<OUT> exceptionHandler(MxStreamExceptionHandler exceptionHandler) {
		this.getSource().exceptionHandler(exceptionHandler);
		return this;
	}
	
	@Override
	public MxStreamExceptionHandler exceptionHandler() {
		return this.getSource().getExceptionHandler();
	}
	
	@Override
	public MxStream<OUT> filter(Predicate<? super OUT> predicate) {
		return this.filter(() -> predicate);
	}
	
	@Override
	public MxStream<OUT> filter(Supplier<Predicate<? super OUT>> supplier) {
		return new Pipeline<OUT, OUT>(this.getSource(), this, new FilteringSpliterator<>(this, this.getSpliterator(), supplier));
	}
	
	@Override
	public MxStream<OUT> skip(final long n) {
		if (n < 0)
			throw new IllegalArgumentException(Long.toString(n));
		if (n == 0) {
			return this;
		} else {
			AtomicLong count = new AtomicLong();
			return filter(value -> count.getAndIncrement() >= n);
		}
	}
	
	@Override
	public <R> MxStream<R> map(Function<? super OUT, ? extends R> mapper) {
		return this.map(() -> mapper);
	}
	
	@Override
	public <R> MxStream<R> map(Supplier<Function<? super OUT, ? extends R>> supplier) {
		return new Pipeline<OUT, R>(this.getSource(), this, new MapSpliterator<>(this, this.getSpliterator(), supplier));
	}
	
	@Override
	public <R> MxStream<R> flatMap(Function<? super OUT, ? extends Stream<? extends R>> mapper) {
		return this.flatMap(() -> mapper);
	}
	
	@Override
	public <R> MxStream<R> flatMap(Supplier<Function<? super OUT, ? extends Stream<? extends R>>> supplier) {
		return new Pipeline<OUT, R>(this.getSource(), this, new FlatMapSpliterator<>(this, this.getSpliterator(), supplier));
	}
	
	@Override
	public <R> MxStream<R> asyncMap(int parallelism, Function<? super OUT, ? extends R> mapper) {
		return this.asyncMap(parallelism, Executors.newVirtualThreadPerTaskExecutor(), mapper);
	}
	
	@Override
	public <R> MxStream<R> asyncMap(int parallelism, Supplier<Function<? super OUT, ? extends R>> supplier) {
		return this.asyncMap(parallelism, Executors.newVirtualThreadPerTaskExecutor(), supplier);
	}
	
	@Override
	public <R> MxStream<R> asyncMap(int parallelism, ExecutorService executorService, Function<? super OUT, ? extends R> mapper) {
		return this.asyncMap(parallelism, executorService, () -> mapper);
	}
	
	@Override
	public <R> MxStream<R> asyncMap(int parallelism, ExecutorService executorService, Supplier<Function<? super OUT, ? extends R>> supplier) {
		return new Pipeline<OUT, R>(this.getSource(), this, new AsyncMapSpliterator<>(this, this.getSpliterator(), parallelism, executorService, supplier));
	}
	
	@Override
	public MxStream<List<OUT>> batch(int batchSize) {
		if (batchSize < 0) {
			throw new IllegalArgumentException(Long.toString(batchSize));
		}
		return new Pipeline<OUT, List<OUT>>(this.getSource(), this, new BatchSpliterator<>(this, this.getSpliterator(), batchSize));
	}
	
	@Override
	public MxStream<OUT> peek(Consumer<? super OUT> action) {
		return peek(() -> action);
	}
	
	@Override
	public MxStream<OUT> peek(Supplier<Consumer<? super OUT>> supplier) {
		return new Pipeline<OUT, OUT>(this.getSource(), this, new PeekSpliterator<>(this, this.getSpliterator(), supplier));
	}
	
	@Override
	public MxStream<OUT> sorted(int windowSize, Comparator<? super OUT> comparator) {
		return sorted(windowSize, () -> comparator);
	}
	
	@Override
	public MxStream<OUT> sorted(int windowSize, Supplier<Comparator<? super OUT>> supplier) {
		return new Pipeline<OUT, OUT>(this.getSource(), this, new WindowedSortSpliterator<>(this, this.getSpliterator(), windowSize, supplier));
	}
	
	@Override
	public MxStream<OUT> fanOut(int bufferSize) {
		return this.fanOut(bufferSize, this.getExecutorService());
	}
	
	@Override
	public MxStream<OUT> fanOut(int bufferSize, ExecutorService executorService) {
		if (!this.isParallel()) {
			if (bufferSize < 0) {
				throw new IllegalArgumentException(Long.toString(bufferSize));
			}
			return new PipelineParallel<OUT, OUT>(this.getSource(), this, this.getParallelism(), executorService, new FanOutSpliterator<>(this, this.getSpliterator(), bufferSize));
		}
		return this;
	}
	
	@Override
	public void forEach(Consumer<? super OUT> action) {
		this.forEach(() -> action);
	}
	
	@Override
	public void forEach(Supplier<Consumer<? super OUT>> supplier) {
		processSink(new ForEachSink<>(this, getSpliterator(), supplier));
	}
	
	@Override
	public <R, A> R collect(Collector<? super OUT, A, R> collector) {
		MxCollector<? super OUT, A, R> mxCollector = new MxCollector<>(collector);
		this.processSink(new CollectorSink<>(this, getSpliterator(), mxCollector));
		return mxCollector.finish();
	}
	
	@Override
	public List<OUT> toList() {
		return collect(Collectors.toList());
	}
	
	@Override
	public Set<OUT> toSet() {
		return collect(Collectors.toSet());
	}
	
	@Override
	public Stream<OUT> toStream() {
		return StreamSupport.stream(this.getSpliterator(), this.isParallel());
	}
	
	private void processSink(AbstractSink<OUT> sink) {
		if (isParallel()) {
			int parallelism = this.getParallelism();
			ExecutorService executorService = this.getExecutorService();
			
			List<Runnable> tasks = new ArrayList<>();
			for (int i = 0; i < parallelism; i++) {
				AbstractSink<OUT> split = sink.trySplit();
				if (split != null) {
					tasks.add(split::forEachRemaining);
				}
			}
			
			List<? extends Future<?>> futures = tasks.stream().map(executorService::submit).toList();
			sink.forEachRemaining();
			
			for (Future<?> future : futures) {
				try {
					future.get();
				} catch (InterruptedException | ExecutionException e) {
					throw new RuntimeException(e);
				}
			}
		} else {
			sink.forEachRemaining();
		}
		try {
			this.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public void close() throws Exception {
		this.getSource().close();
	}
	
}
