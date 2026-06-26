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

public abstract class AbstractChainedSpliterator<IN, OUT> implements MxSpliterator<OUT> {
	
	private final BasePipeline<?,IN> pipeline;
	private final MxSpliterator<IN> previousSpliterator;
	
	public AbstractChainedSpliterator(BasePipeline<?,IN> pipeline, MxSpliterator<IN> previousSpliterator) {
		this.pipeline = pipeline;
		this.previousSpliterator = previousSpliterator;
		
	}
	
	protected final PipelineSource<?> getSource() {
		return pipeline.getSource();
	}
	
	protected final BasePipeline<?,IN> getPipeline() {
		return pipeline;
	}
	
	protected final MxSpliterator<IN> getPreviousSpliterator() {
		return previousSpliterator;
	}
	
	protected boolean isParallel() {
		return this.pipeline.isParallel();
	}
	
	protected abstract MxSpliterator<OUT> split(MxSpliterator<IN> spliterator);
	
	@Override
	public MxSpliterator<OUT> trySplit() {
		if (this.isParallel()) {
			MxSpliterator<IN> spliterator = this.previousSpliterator.trySplit();
			if (spliterator != null) {
				return split(spliterator);
			}
		}
		return null;
	}
	
	@Override
	public void onStart() {
		this.previousSpliterator.onStart();
	}
	
	@Override
	public void close() {
		this.previousSpliterator.close();
	}
}
