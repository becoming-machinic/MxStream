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

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class WindowedSortSpliterator<T> extends AbstractChainedSpliterator<T, T> {
	
	private final int windowSize;
	private final Supplier<Comparator<? super T>> supplier;
	private final Queue<T> queue;
	
	public WindowedSortSpliterator(MxStream<T> stream, Spliterator<T> previousSpliterator, int windowSize, Supplier<Comparator<? super T>> supplier) {
		super(stream, previousSpliterator);
		this.windowSize = windowSize;
		this.supplier = supplier;
		Comparator<? super T> comparator = supplier.get();
		this.queue = new PriorityQueue<>(windowSize + 1, comparator);
	}
	
	protected void enqueue(T value) {
		this.queue.add(value);
	}
	
	protected T dequeueWhenFull() {
		if (queue.size() >= windowSize) {
			return this.queue.poll();
		}
		return null;
	}
	
	protected T dequeue() {
		return this.queue.poll();
	}
	
	@Override
	public boolean tryAdvance(Consumer<? super T> action) {
		// advance until queue is full or we reach end
		while (true) {
			if (this.previousSpliterator.tryAdvance(value -> {
				// If value is null bypass queue to prevent NPE
				if (value == null) {
					action.accept(null);
				} else {
					enqueue(value);
				}
			})) {
				// advanced
				T value = dequeueWhenFull();
				if (value != null) {
					action.accept(value);
					return true;
				}
			} else {
				// failed to advance
				T value = dequeue();
				if (value != null) {
					action.accept(value);
					return true;
				}
				return false;
			}
		}
	}
	
	@Override
	public Spliterator<T> split(Spliterator<T> spliterator) {
		return new WindowedSortSpliterator<>(stream, spliterator, windowSize, supplier);
	}
	
}
