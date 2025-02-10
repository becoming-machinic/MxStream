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

package io.machinic.stream.sink;

import io.machinic.stream.MxCollector;
import io.machinic.stream.MxStream;

import java.util.Spliterator;

public class CollectorSink<T, A, R> extends AbstractSink<T> {
	
	private final MxCollector<? super T, A, R> collector;
	private final A container;
	
	public CollectorSink(MxStream<T> stream, Spliterator<T> previousSpliterator, MxCollector<? super T, A, R> collector) {
		super(stream, previousSpliterator);
		this.collector = collector;
		this.container = collector.get();
	}
	
	@Override
	protected AbstractSink<T> split(Spliterator<T> spliterator) {
		return new CollectorSink<>(stream, spliterator, collector);
	}
	
	@Override
	public void forEachRemaining() {
		//noinspection StatementWithEmptyBody
		do {
			// NOOP
		} while (previousSpliterator.tryAdvance(
				value -> collector.accumulator().accept(container, value)
		));
	}
	
}
