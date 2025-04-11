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

import io.machinic.stream.spliterator.AbstractChainedSpliterator;
import io.machinic.stream.spliterator.BlockingQueueReaderSpliterator;
import io.machinic.stream.spliterator.CancellableSpliterator;
import io.machinic.stream.util.BufferedReaderIterator;

import java.io.BufferedReader;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public abstract class PipelineSource<IN> extends BasePipeline<IN, IN> implements Source<IN> {
	
	protected final AtomicBoolean closedReference = new AtomicBoolean(false);
	private final boolean parallel;
	private final int parallelism;
	private final ExecutorService executorService;
	private final CancellableSpliterator<IN> spliterator;
	private MxStreamExceptionHandler exceptionHandler = new MxStreamExceptionHandler.DefaultMxStreamExceptionHandler();
	private volatile StreamException streamException = null;
	
	public PipelineSource(Spliterator<IN> spliterator, boolean parallel) {
		this(spliterator, parallel, ForkJoinPool.getCommonPoolParallelism(), ForkJoinPool.commonPool());
	}
	
	public PipelineSource(Spliterator<IN> spliterator, boolean parallel, int parallelism, ExecutorService executorService) {
		this.spliterator = new CancellableSpliterator<>(this, spliterator);
		this.parallel = parallel;
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
	protected AbstractChainedSpliterator<IN, IN> getSpliterator() {
		return this.spliterator;
	}
	
	public ExecutorService getExecutorService() {
		return this.executorService;
	}
	
	@Override
	public boolean isClosed() {
		return closedReference.get();
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
	
	public StreamException getException() {
		return this.streamException;
	}
	
	protected void setStream(StreamException streamException) {
		this.streamException = streamException;
	}
	
	public MxStreamExceptionHandler getExceptionHandler() {
		return exceptionHandler;
	}
	
	public static class StreamSource<IN> extends PipelineSource<IN> {
		
		private final Stream<IN> stream;
		
		public StreamSource(Stream<IN> stream) {
			super(stream.spliterator(), stream.isParallel());
			this.stream = stream;
		}
		
		public StreamSource(Stream<IN> stream, int parallelism, ExecutorService executorService) {
			super(stream.spliterator(), stream.isParallel(), parallelism, executorService);
			this.stream = stream;
		}
		
		@Override
		public void close() throws Exception {
			if (!closedReference.getAndSet(true)) {
				stream.close();
			}
		}
	}
	
	public static class IteratorSource<IN> extends PipelineSource<IN> {
		
		public IteratorSource(Iterator<IN> iterator) {
			super(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
		}
		
		public IteratorSource(Spliterator<IN> spliterator, boolean parallel) {
			super(spliterator, parallel);
		}
		
		@Override
		public void close() throws Exception {
			closedReference.getAndSet(true);
			// Nothing to close
		}
	}
	
	public static class BufferedReaderStream extends IteratorSource<String> {
		private final BufferedReader bufferedReader;
		
		public BufferedReaderStream(BufferedReader bufferedReader) {
			super(new BufferedReaderIterator(bufferedReader));
			this.bufferedReader = bufferedReader;
		}
		
		@Override
		public void close() throws Exception {
			if (!closedReference.getAndSet(true)) {
				bufferedReader.close();
			}
		}
	}
	
	public static class TapSource<IN> extends PipelineSource<IN> {
		
		private final BasePipeline<?, IN> parentPipeline;
		
		public TapSource(BasePipeline<?, IN> parentPipeline, BlockingQueue<BlockingQueueReaderSpliterator.QueueWrapper<IN>> queue, boolean parallel, int parallelism, ExecutorService executorService) {
			super(new BlockingQueueReaderSpliterator<>(parentPipeline, parallel, queue), parallel, parallelism, executorService);
			this.parentPipeline = parentPipeline;
		}
		
		@Override
		protected PipelineSource<?> getSource() {
			return parentPipeline.getSource();
		}
		
		@Override
		protected BasePipeline<?, IN> getPrevious() {
			//noinspection unchecked
			return (BasePipeline<?, IN>) parentPipeline.getPrevious();
		}
		
		@Override
		public boolean isClosed() {
			return this.parentPipeline.isClosed();
		}
		
		@Override
		public void close() throws Exception {
			parentPipeline.close();
		}
	}
	
}
