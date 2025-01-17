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

package io.machinic.stream;

import io.machinic.stream.source.PipelineSource;
import io.machinic.stream.spliterator.AbstractSpliterator;

public class Pipeline<IN, OUT> extends BasePipeline<IN, OUT> {
	
	private final PipelineSource<?> source;
	private final BasePipeline<?, IN> previous;
	private final AbstractSpliterator<IN, OUT> spliterator;
	
	public Pipeline(PipelineSource<?> source, BasePipeline<?, IN> previous, AbstractSpliterator<IN, OUT> spliterator) {
		this.source = source;
		this.previous = previous;
		this.spliterator = spliterator;
	}
	
	@Override
	protected PipelineSource<?> getSource() {
		return source;
	}
	
	@Override
	protected BasePipeline<?, IN> getPrevious() {
		return previous;
	}
	
	@Override
	protected AbstractSpliterator<IN, OUT> getSpliterator() {
		return spliterator;
	}
	
}
