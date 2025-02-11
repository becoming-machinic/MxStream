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
import java.util.function.Consumer;

public class PassThoughSpliterator<T> extends AbstractChainedSpliterator<T, T> {
	
	public PassThoughSpliterator(MxStream<T> stream, Spliterator<T> previousSpliterator) {
		super(stream, previousSpliterator);
	}
	
	@Override
	public boolean tryAdvance(Consumer<? super T> action) {
		return previousSpliterator.tryAdvance(action);
	}
	
	@Override
	public Spliterator<T> split(Spliterator<T> spliterator) {
		return new PassThoughSpliterator<>(this.stream, spliterator);
	}
	
	@Override
	public int characteristics() {
		if (this.isParallel()) {
			return this.previousSpliterator.characteristics() | (Spliterator.CONCURRENT);
		}
		return this.previousSpliterator.characteristics();
	}
	
}
