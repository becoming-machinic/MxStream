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

package io.machinic.stream.spliterator;

import io.machinic.stream.MxStream;
import io.machinic.stream.StreamEventException;
import io.machinic.stream.StreamException;
import io.machinic.stream.metrics.AsyncMapMetric;
import io.machinic.stream.metrics.AsyncMapMetricSupplier;
import io.machinic.stream.util.Opp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Spliterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class AsyncMapSpliterator<IN, OUT> extends AbstractChainedSpliterator<IN, OUT> {
	
	private static final Logger logger = LoggerFactory.getLogger(MxStream.class);
	
	private final Supplier<Function<? super IN, ? extends OUT>> supplier;
	private final Function<? super IN, ? extends OUT> mapper;
	private final int parallelism;
	private final ExecutorService executorService;
	private final AsyncMapMetricSupplier metricSupplier;
	private final AsyncMapMetric metric;
	private final Queue<Future<TaskResult>> queue;
	private boolean started;
	
	public AsyncMapSpliterator(MxStream<IN> stream, Spliterator<IN> previousSpliterator, int parallelism, ExecutorService executorService, AsyncMapMetricSupplier metricSupplier, Supplier<Function<? super IN, ? extends OUT>> supplier) {
		super(stream, previousSpliterator);
		this.supplier = supplier;
		this.mapper = supplier.get();
		this.parallelism = parallelism;
		this.executorService = executorService;
		this.metricSupplier = metricSupplier;
		this.metric = Opp.applyOrNull(metricSupplier, AsyncMapMetricSupplier::get);
		this.queue = new ArrayDeque<>(parallelism + 2);
	}
	
	private TaskCallable createTask(IN input) {
		return new TaskCallable(input);
	}
	
	private void enqueue(TaskCallable taskCallable) {
		queue.add(this.executorService.submit(taskCallable));
	}
	
	private int getQueueSize() {
		return queue.size();
	}
	
	private boolean peekNext() {
		Future<TaskResult> future = queue.peek();
		return future != null && future.isDone();
	}
	
	private TaskResult getNext() throws ExecutionException, InterruptedException {
		Future<TaskResult> nextFuture = queue.poll();
		if (nextFuture != null) {
			if (metric != null) {
				long startTimestamp = System.nanoTime();
				try {
					return nextFuture.get();
				} finally {
					metric.onWait(System.nanoTime() - startTimestamp);
				}
			} else {
				return nextFuture.get();
			}
		}
		return null;
	}
	
	private void dequeue(Consumer<? super OUT> action, boolean drain) {
		if (peekNext() || this.getQueueSize() >= parallelism || drain) {
			try {
				TaskResult result = getNext();
				if (result != null) {
					if (metric != null) {
						metric.onEvent(result.duration);
					}
					action.accept(result.output);
				}
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			} catch (ExecutionException e) {
				if (e.getCause() != null && e.getCause() instanceof StreamEventException) {
					logger.debug("asyncMap operation skipping message due to caught exception {}", e.getCause().getMessage());
				} else if (e.getCause() != null && e.getCause() instanceof StreamException) {
					throw (StreamException) e.getCause();
				} else {
					throw new StreamException(String.format("mapAsync failed. Caused by %s", e.getMessage()), e);
				}
			} catch (Exception e) {
				throw new StreamException(String.format("mapAsync failed. Caused by %s", e.getMessage()), e);
			}
		}
	}
	
	@Override
	public boolean tryAdvance(Consumer<? super OUT> action) {
		if (!started && this.metric != null) {
			this.metric.onStart();
			started = true;
		}
		
		boolean canAdvance;
		do {
			canAdvance = this.previousSpliterator.tryAdvance(value ->
			{
				this.enqueue(this.createTask(value));
				dequeue(action, false);
			});
		} while (canAdvance && this.getQueueSize() <= parallelism);
		
		if (!canAdvance) {
			do {
				dequeue(action, true);
			} while (this.getQueueSize() != 0);
		}
		
		return canAdvance;
	}
	
	@Override
	public AbstractChainedSpliterator<IN, OUT> split(Spliterator<IN> spliterator) {
		return new AsyncMapSpliterator<>(stream, spliterator, parallelism, executorService, metricSupplier, supplier);
	}
	
	@Override
	public void close() {
		if (this.metric != null) {
			this.metric.onStop();
		}
	}
	
	private class TaskCallable implements Callable<TaskResult> {
		private final IN input;
		
		private TaskCallable(IN input) {
			this.input = input;
		}
		
		@Override
		public TaskResult call() throws Exception {
			long startTimestamp = System.currentTimeMillis();
			try {
				return new TaskResult(mapper.apply(input), startTimestamp);
			} catch (Exception e) {
				getStream().exceptionHandler().onException(e, input);
				throw new StreamEventException(input, String.format("Event %s failed. Caused by %s", input, e.getMessage()), e);
			}
		}
	}
	
	private class TaskResult {
		
		private final OUT output;
		private final long duration;
		
		private TaskResult(OUT output, long startTimestamp) {
			this.output = output;
			this.duration = System.currentTimeMillis() - startTimestamp;
		}
	}
}
