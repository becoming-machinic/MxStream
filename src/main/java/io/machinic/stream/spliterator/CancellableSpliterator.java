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

import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class CancellableSpliterator<T> extends AbstractChainedSpliterator<T, T> {
	
	private final AtomicBoolean cancelled;
	
	public CancellableSpliterator(MxStream<T> stream, Spliterator<T> previousSpliterator) {
		super(stream, previousSpliterator);
		this.cancelled = new AtomicBoolean(false);
	}
	
	// Also set cancelled reference for split
	private CancellableSpliterator(MxStream<T> stream, Spliterator<T> previousSpliterator, AtomicBoolean cancelled) {
		super(stream, previousSpliterator);
		this.cancelled = cancelled;
	}
	
	@Override
	public boolean tryAdvance(Consumer<? super T> action) {
		return !cancelled.get() && previousSpliterator.tryAdvance(action);
	}
	
	@Override
	public Spliterator<T> split(Spliterator<T> spliterator) {
		return new CancellableSpliterator<>(this.stream, spliterator, cancelled);
	}
	
	@Override
	public int characteristics() {
		// MxStreams are not sized
		return this.previousSpliterator.characteristics() & (Spliterator.IMMUTABLE | Spliterator.DISTINCT | Spliterator.SORTED | Spliterator.CONCURRENT | Spliterator.ORDERED | Spliterator.NONNULL);
	}
	
	public void cancel() {
		cancelled.set(true);
	}
	
	public boolean isCancelled() {
		return cancelled.get();
	}
}
