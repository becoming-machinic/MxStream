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

package io.machinic.stream.source;

import io.machinic.stream.BasePipeline;
import io.machinic.stream.MxStreamExceptionHandler;
import io.machinic.stream.spliterator.AbstractSpliterator;
import io.machinic.stream.spliterator.CancellableSpliterator;

import java.util.Spliterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Stream;

public class PipelineSource<IN> extends BasePipeline<IN, IN> implements Source<IN> {
	
	private final boolean parallel;
	private final int parallelism;
	private final ExecutorService executorService;
	private final CancellableSpliterator<IN> spliterator;
	private MxStreamExceptionHandler exceptionHandler = new MxStreamExceptionHandler.DefaultMxStreamExceptionHandler();
	
	public PipelineSource(Stream<IN> stream) {
		this(stream.spliterator(), stream.isParallel());
	}
	
	public PipelineSource(Spliterator<IN> spliterator, boolean parallel) {
		this(spliterator, parallel, ForkJoinPool.getCommonPoolParallelism(), ForkJoinPool.commonPool());
	}
	
	public PipelineSource(Spliterator<IN> spliterator, boolean parallel, int parallelism, ExecutorService executorService) {
		this.parallel = parallel;
		this.spliterator = new CancellableSpliterator<>(this, spliterator);
		this.parallelism = parallelism;
		this.executorService = (executorService != null ? executorService : ForkJoinPool.commonPool());
	}
	
	@Override
	protected PipelineSource<?> getSource() {
		return this;
	}
	
	@Override
	protected BasePipeline<?, IN> getPrevious() {
		return this;
	}
	
	@Override
	protected AbstractSpliterator<IN, IN> getSpliterator() {
		return this.spliterator;
	}
	
	public ExecutorService getExecutorService() {
		return this.executorService;
	}
	
	@Override
	public boolean isParallel() {
		return parallel;
	}
	
	public int getParallelism() {
		return this.parallelism;
	}
	
	public PipelineSource<IN> exceptionHandler(MxStreamExceptionHandler exceptionHandler) {
		this.exceptionHandler = exceptionHandler;
		return this;
	}
	
	public void close() throws Exception {
		this.spliterator.cancel();
	}
	
	public MxStreamExceptionHandler getExceptionHandler() {
		return exceptionHandler;
	}
	
}
