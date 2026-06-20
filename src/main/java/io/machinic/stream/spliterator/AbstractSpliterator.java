/*
 * Copyright 2026 Becoming Machinic Inc.
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

import io.machinic.stream.BasePipeline;
import io.machinic.stream.PipelineSource;

public abstract class AbstractSpliterator<IN, OUT> implements MxSpliterator<OUT> {
	
	protected final BasePipeline<?,IN> pipeline;
	public final boolean parallel;
	
	public AbstractSpliterator(BasePipeline<?,IN> pipeline, boolean parallel) {
		this.pipeline = pipeline;
		this.parallel = parallel;
	}
	
	protected PipelineSource<?> getSource() {
		return pipeline.getSource();
	}
	
	protected BasePipeline<?,IN> getPipeline() {
		return pipeline;
	}
	
	protected boolean isParallel() {
		return this.parallel;
	}
	
	protected abstract MxSpliterator<OUT> split();
	
	@Override
	public MxSpliterator<OUT> trySplit() {
		if (this.isParallel()) {
			return this.split();
		}
		return null;
	}
	
	@Override
	public void onStart() {
	
	}
	
	@Override
	public void close() {
	}
}
