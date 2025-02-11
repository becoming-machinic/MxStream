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
	private final Queue<Future<OUT>> queue;
	
	public AsyncMapSpliterator(MxStream<IN> stream, Spliterator<IN> previousSpliterator, int parallelism, ExecutorService executorService, Supplier<Function<? super IN, ? extends OUT>> supplier) {
		super(stream, previousSpliterator);
		this.supplier = supplier;
		this.mapper = supplier.get();
		this.parallelism = parallelism;
		this.executorService = executorService;
		this.queue = new ArrayDeque<>(parallelism + 2);
	}
	
	private AsyncTask createTask(IN input) {
		return new AsyncTask(input);
	}
	
	private void enqueue(AsyncTask asyncTask) {
		queue.add(this.executorService.submit(asyncTask));
	}
	
	private Future<OUT> nonBlockingDequeue() {
		Future<OUT> future = queue.peek();
		if (future != null && future.isDone()) {
			return queue.poll();
		}
		return null;
	}
	
	private Future<OUT> dequeue() {
		return queue.poll();
	}
	
	protected int getQueueSize() {
		if (this.isParallel()) {
			synchronized (queue) {
				return queue.size();
			}
		}
		return queue.size();
	}
	
	@Override
	public boolean tryAdvance(Consumer<? super OUT> action) {
		
		// process completed futures
		Future<OUT> future = nonBlockingDequeue();
		if (future == null && this.getQueueSize() >= parallelism) {
			future = dequeue();
		}
		
		if (future != null) {
			try {
				action.accept(future.get());
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
		
		boolean canAdvance;
		do {
			canAdvance = this.previousSpliterator.tryAdvance(value ->
			{
				this.enqueue(this.createTask(value));
			});
		} while (canAdvance && this.getQueueSize() <= parallelism);
		
		return canAdvance
				|| this.getQueueSize() != 0;
	}
	
	@Override
	public AbstractChainedSpliterator<IN, OUT> split(Spliterator<IN> spliterator) {
		return new AsyncMapSpliterator<>(stream, spliterator, parallelism, executorService, supplier);
	}
	
	public class AsyncTask implements Callable<OUT> {
		private final IN input;
		
		private AsyncTask(IN input) {
			this.input = input;
		}
		
		@Override
		public OUT call() throws Exception {
			try {
				return mapper.apply(input);
			} catch (Exception e) {
				getStream().exceptionHandler().onException(e, input);
				throw new StreamEventException(input, String.format("Event %s failed. Caused by %s", input, e.getMessage()), e);
			}
		}
	}
}
