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

public abstract class AbstractSpliterator<IN, OUT> implements MxSpliterator<OUT> {
	
	protected final MxStream<IN> stream;
	protected final int characteristics;
	public final boolean parallel;
	
	public AbstractSpliterator(MxStream<IN> stream, boolean parallel) {
		this.stream = stream;
		this.characteristics = stream.getCharacteristics();
		this.parallel = parallel;
	}
	
	protected MxStream<IN> getStream() {
		return stream;
	}
	
	protected boolean isParallel() {
		return this.parallel;
	}
	
	protected abstract Spliterator<OUT> split();
	
	@Override
	public Spliterator<OUT> trySplit() {
		if (this.isParallel()) {
			return this.split();
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
			return this.characteristics | (Spliterator.CONCURRENT);
		}
		return this.characteristics;
	}
	
	@Override
	public void close() {
	}
}
