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

public abstract class AbstractChainedSpliterator<IN, OUT> implements MxSpliterator<OUT> {
	
	protected final MxStream<IN> stream;
	protected final Spliterator<IN> previousSpliterator;

	
	public AbstractChainedSpliterator(MxStream<IN> stream, Spliterator<IN> previousSpliterator) {
		this.stream = stream;
		this.previousSpliterator = previousSpliterator;

	}
	
	protected MxStream<IN> getStream() {
		return stream;
	}
	
	protected boolean isParallel() {
		return this.stream.isParallel();
	}
	
	protected abstract Spliterator<OUT> split(Spliterator<IN> spliterator);
	
	@Override
	public Spliterator<OUT> trySplit() {
		if (this.isParallel()) {
			Spliterator<IN> spliterator = this.previousSpliterator.trySplit();
			if (spliterator != null) {
				return split(spliterator);
			}
		}
		return null;
	}
	
	@Override
	public long estimateSize() {
		return Long.MAX_VALUE;
	}
	
	@Override
	public int characteristics() {
		if (this.isParallel()) {
			return this.previousSpliterator.characteristics() | (Spliterator.CONCURRENT);
		}
		return this.previousSpliterator.characteristics();
	}
	
	@Override
	public void close() {	}
}
