/*
 * Copyright 2026 Becoming Machinic Inc.
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

import io.machinic.stream.BasePipeline;
import io.machinic.stream.StreamException;
import io.machinic.stream.StreamInterruptedException;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/**
 * A spliterator that partitions elements from a previous spliterator into multiple output spliterators based on
 * a partitioning function. This implementation uses a fan-out pattern where elements are distributed across
 * multiple secondary spliterators.
 *
 * @param <T> the type of elements provided by this spliterator
 */
public class FanOutPartitionedSpliterator<T> extends AbstractChainedSpliterator<T, T> {
	
	private final CopyOnWriteArrayList<FanOutSecondarySpliterator> partitions = new CopyOnWriteArrayList<>();
	private final int bufferSize;
	private final Supplier<ToIntFunction<? super T>> supplier;
	private final ToIntFunction<? super T> toIntFunction;
	private volatile boolean done = false;
	
	public FanOutPartitionedSpliterator(BasePipeline<?,T> pipeline, MxSpliterator<T> previousSpliterator, int bufferSize, Supplier<ToIntFunction<? super T>> supplier) {
		super(pipeline, previousSpliterator);
		this.bufferSize = bufferSize;
		this.supplier = supplier;
		this.toIntFunction = supplier.get();
	}
	
	protected boolean isDone() {
		return this.done;
	}
	
	@Override
	public boolean tryAdvance(Consumer<? super T> action) {
		boolean advance = true;
		do {
			advance = this.getPreviousSpliterator().tryAdvance(value -> {
				try {
					int partitionIndex = toIntFunction.applyAsInt(value) % partitions.size();
					Wrapper wrapper = new Wrapper(value);
					do {
						if (partitions.get(partitionIndex).offer(wrapper, 100, TimeUnit.MILLISECONDS)) {
							break;
						}
					} while (!done);
				} catch (InterruptedException e) {
					this.close();
				}
			});
		} while (!done && advance);
		this.done = true;
		return false;
	}
	
	@Override
	protected MxSpliterator<T> split(MxSpliterator<T> spliterator) {
		// not used for this implementation
		throw new UnsupportedOperationException();
	}
	
	@Override
	public MxSpliterator<T> trySplit() {
		FanOutSecondarySpliterator secondarySpliterator = new FanOutSecondarySpliterator(this);
		this.partitions.add(secondarySpliterator);
		return secondarySpliterator;
	}
	
	private class Wrapper {
		private final T value;
		
		public Wrapper(T value) {
			this.value = value;
		}
		
		public T getValue() {
			return value;
		}
	}
	
	@Override
	public void close() {
		super.close();
		this.done = true;
	}
	
	public class FanOutSecondarySpliterator implements MxSpliterator<T> {
		
		private final FanOutPartitionedSpliterator<T> parent;
		private final BlockingQueue<Wrapper> queue;
		private long pollIntervalMillis = 100;
		
		public FanOutSecondarySpliterator(FanOutPartitionedSpliterator<T> parent) {
			this.parent = parent;
			this.queue = new ArrayBlockingQueue<>(bufferSize);
		}
		
		private boolean offer(Wrapper wrapper, long timeout, TimeUnit unit) throws InterruptedException {
			return this.queue.offer(wrapper, timeout, unit);
		}
		
		@Override
		public boolean tryAdvance(Consumer<? super T> action) {
			Wrapper wrapper = null;
			try {
				do {
					wrapper = queue.poll(pollIntervalMillis, TimeUnit.MILLISECONDS);
					if (wrapper != null) {
						action.accept(wrapper.getValue());
					}
				} while (wrapper != null);
				return !this.parent.isDone();
			} catch (StreamException e) {
				throw e;
			} catch (RuntimeException e) {
				parent.getPipeline().exceptionHandler().onException(e, (wrapper != null ? wrapper.getValue() : null));
			} catch (InterruptedException e) {
				throw new StreamInterruptedException("FanOutPartitionedSpliterator was interrupted");
			}
			return false;
		}
		
		@Override
		public MxSpliterator<T> trySplit() {
			return parent.trySplit();
		}
		
		@Override
		public void onStart() {
			this.pollIntervalMillis = parent.getSource().getPollIntervalMillis();
		}
		
		@Override
		public void close() {
			parent.close();
			this.queue.clear();
		}
	}
}
