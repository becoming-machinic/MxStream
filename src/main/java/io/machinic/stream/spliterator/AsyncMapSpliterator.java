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

public class AsyncMapSpliterator<IN, OUT> extends AbstractSpliterator<IN, OUT> {
	
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
		this.queue = new ArrayDeque<>(parallelism + 1);
	}
	
	private AsyncMapSpliterator(MxStream<IN> stream, Spliterator<IN> previousSpliterator, int parallelism, ExecutorService executorService, Supplier<Function<? super IN, ? extends OUT>> supplier, Queue<Future<OUT>> queue) {
		super(stream, previousSpliterator);
		this.parallelism = parallelism;
		this.supplier = supplier;
		this.mapper = supplier.get();
		this.executorService = executorService;
		this.queue = queue;
	}
	
	private AsyncTask createTask(IN input) {
		return new AsyncTask(input);
	}
	
	protected void safeEnqueue(AsyncTask asyncTask) {
		if (this.isParallel()) {
			synchronized (queue) {
				enqueue(asyncTask);
			}
		} else {
			enqueue(asyncTask);
		}
	}
	
	private void enqueue(AsyncTask asyncTask) {
		queue.add(this.executorService.submit(asyncTask));
	}
	
	/**
	 * Call nonBlockingDequeue with synchronized if needed.
	 *
	 * @return Future<OUT>
	 */
	protected Future<OUT> safeNonBlockingDequeue() {
		if (this.isParallel()) {
			synchronized (queue) {
				return nonBlockingDequeue();
			}
		}
		return nonBlockingDequeue();
	}
	
	private Future<OUT> nonBlockingDequeue() {
		Future<OUT> future = queue.peek();
		if (future != null && future.isDone()) {
			return queue.poll();
		}
		return null;
	}
	
	protected Future<OUT> safeBlockingDequeue() {
		if (this.isParallel()) {
			synchronized (queue) {
				return blockingDequeue();
			}
		}
		return blockingDequeue();
	}
	
	private Future<OUT> blockingDequeue() {
		Future<OUT> future = queue.poll();
		if (future != null) {
			try {
				future.get();
				return future;
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
		}
		return null;
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
		// finish completed tasks
		Future<OUT> future;
		while ((future = safeNonBlockingDequeue()) != null) {
			try {
				action.accept(future.get());
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
		}
		
		if (this.getQueueSize() >= parallelism) {
			Future<OUT> blockingFuture = safeBlockingDequeue();
			if (blockingFuture != null) {
				try {
					action.accept(blockingFuture.get());
				} catch (InterruptedException | ExecutionException e) {
					throw new RuntimeException(e);
				}
			}
		}
		boolean canAdvance = this.previousSpliterator.tryAdvance(value ->
		{
			this.safeEnqueue(this.createTask(value));
		});
		
		return canAdvance || this.getQueueSize() != 0;
	}
	
	@Override
	public AbstractSpliterator<IN, OUT> split(Spliterator<IN> spliterator) {
		return new AsyncMapSpliterator<>(stream, spliterator, parallelism, executorService, supplier, queue);
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
				throw new RuntimeException(e);
			}
		}
	}
}
