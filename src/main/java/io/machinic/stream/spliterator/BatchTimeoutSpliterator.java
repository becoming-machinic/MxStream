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

import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class BatchTimeoutSpliterator<T> extends AbstractSpliterator<T, List<T>> {
	
	private final int batchSize;
	private final AtomicReference<Batch> batchReference;
	private final long timeout;
	private final TimeUnit unit;
	
	public BatchTimeoutSpliterator(MxStream<T> stream, Spliterator<T> previousSpliterator, int batchSize, long timeout, TimeUnit unit) {
		super(stream, previousSpliterator);
		this.batchSize = batchSize;
		this.timeout = timeout;
		this.unit = unit;
		batchReference = new AtomicReference<>(new Batch());
	}
	
	@Override
	public boolean tryAdvance(Consumer<? super List<T>> action) {
		Batch batch = batchReference.getPlain();
		if (this.previousSpliterator.tryAdvance(batch::add)) {
			if (batch.getBatch().size() >= batchSize || batch.isExpired()) {
				action.accept(batch.getBatch());
				batchReference.setPlain(new Batch());
			}
			return true;
		} else {
			if (!batch.getBatch().isEmpty()) {
				action.accept(batch.getBatch());
				batchReference.setPlain(new Batch());
			}
			return false;
		}
	}
	
	@Override
	public Spliterator<List<T>> split(Spliterator<T> spliterator) {
		return new BatchTimeoutSpliterator<>(this.stream, spliterator, batchSize, timeout, unit);
	}
	
	private class Batch {
		private final List<T> batch;
		private final long endTime;
		
		private Batch() {
			this.batch = new ArrayList<>(batchSize);
			this.endTime = System.currentTimeMillis() + unit.toMillis(timeout);
		}
		
		public List<T> getBatch() {
			return batch;
		}
		
		public boolean isExpired() {
			return endTime <= System.currentTimeMillis();
		}
		
		public void add(T element) {
			batch.add(element);
		}
	}
}
