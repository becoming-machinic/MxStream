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
import io.machinic.stream.StreamException;

import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class FlatMapSpliterator<IN, OUT> extends AbstractSpliterator<IN, OUT> {
	
	private final Supplier<Function<? super IN, ? extends Stream<? extends OUT>>> supplier;
	private final Function<? super IN, ? extends Stream<? extends OUT>> mapper;
	
	public FlatMapSpliterator(MxStream<IN> stream, Spliterator<IN> previousSpliterator, Supplier<Function<? super IN, ? extends Stream<? extends OUT>>> supplier) {
		super(stream, previousSpliterator);
		this.supplier = supplier;
		this.mapper = supplier.get();
	}
	
	@Override
	public boolean tryAdvance(Consumer<? super OUT> action) {
		return this.previousSpliterator.tryAdvance(value -> {
			try {
				mapper.apply(value).forEachOrdered(action);
			} catch (StreamException e) {
				throw e;
			} catch (Exception e) {
				stream.exceptionHandler().onException(e, value);
			}
		});
	}
	
	@Override
	public AbstractSpliterator<IN, OUT> split(Spliterator<IN> spliterator) {
		return new FlatMapSpliterator<>(this.stream, spliterator, supplier);
	}
	
}
