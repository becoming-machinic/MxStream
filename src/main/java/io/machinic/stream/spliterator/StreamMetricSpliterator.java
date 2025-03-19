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
import io.machinic.stream.metrics.StreamMetric;
import io.machinic.stream.metrics.StreamMetricSupplier;

import java.util.Spliterator;
import java.util.function.Consumer;

public class StreamMetricSpliterator<T> extends AbstractChainedSpliterator<T, T> {
	
	private final StreamMetricSupplier metricSupplier;
	private final StreamMetric streamMetric;
	private boolean started = false;
	
	public StreamMetricSpliterator(MxStream<T> stream, Spliterator<T> previousSpliterator, StreamMetricSupplier metricSupplier) {
		super(stream, previousSpliterator);
		this.metricSupplier = metricSupplier;
		this.streamMetric = metricSupplier.get();
	}
	
	@Override
	public boolean tryAdvance(Consumer<? super T> action) {
		if (!started) {
			streamMetric.onStart();
			started = true;
		}
		
		return this.previousSpliterator.tryAdvance(value -> {
			try {
				this.streamMetric.onEvent();
				action.accept(value);
			} catch (StreamException e) {
				throw e;
			} catch (Exception e) {
				stream.exceptionHandler().onException(e, value);
				action.accept(value);
			}
		});
	}
	
	@Override
	public Spliterator<T> split(Spliterator<T> spliterator) {
		return new StreamMetricSpliterator<>(this.stream, spliterator, metricSupplier);
	}
	
	@Override
	public void close() {
		streamMetric.onStop();
	}
}
