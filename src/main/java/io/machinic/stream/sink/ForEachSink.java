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

import io.machinic.stream.MxStream;

import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ForEachSink<T> extends AbstractSink<T> {
	
	private final Supplier<Consumer<? super T>> supplier;
	private final Consumer<? super T> consumer;
	
	public ForEachSink(MxStream<T> stream, Spliterator<T> previousSpliterator, Supplier<Consumer<? super T>> supplier) {
		super(stream, previousSpliterator);
		this.supplier = supplier;
		this.consumer = supplier.get();
	}
	
	@Override
	protected AbstractSink<T> split(Spliterator<T> spliterator) {
		return new ForEachSink<>(stream, spliterator, supplier);
	}
	
	@Override
	public void forEachRemaining() {
		do {
			// NOOP
		} while (previousSpliterator.tryAdvance(value ->
		{
			try {
				consumer.accept(value);
			} catch (Exception e) {
				stream.exceptionHandler().onException(e, value);
			}
		}));
	}
	
}
